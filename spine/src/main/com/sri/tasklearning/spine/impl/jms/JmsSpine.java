/*
 * Copyright 2016 SRI International
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sri.tasklearning.spine.impl.jms;

import static com.sri.tasklearning.spine.messages.DurableMessageType.REGISTER;
import static com.sri.tasklearning.spine.messages.SystemMessageType.UNSUBSCRIBE;

import java.net.URI;
import java.util.*;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.InvalidClientIDException;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.*;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

import com.sri.tasklearning.spine.util.SpineStatus;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is one implementation of the Spine MiddleWare. The Spine
 * is the messaging center for all components of the PALCPOF system.
 * Currently, all messages are broadcast (posted to a topic, in JMS speak).
 * Clients use this module by subscribing with interest in particular
 * message types (see the subscribe method). When a message of that
 * topic arrives in the spine, the message is sent to the handler that
 * the client(s) used to subscribe for that message type.
 * When a spine client attempts to send a message on a topic and there are
 * no subscribers, or the only subscriber is the sending client, the send
 * request will be rejected. Thus, a message must have a valid destination
 * before it can be sent and client are not permitted to send messages to
 * themselves.
 * When a Client sends a message into the Spine, the message type is
 * examined by using the isInstance method (see the send method below).
 * Depending on the type of the message, it is sent on a particular topic
 * or rejected it if is not recognised.
 * The JmsSpine has three operation modes, REMOTE, LOCAL and TEST. The LOCAL
 * mode creates and hosts the Jms Message Broker, so there can only be
 * one instance on a machine (it uses localhost) that can be running
 * in LOCAL mode. Attempts to create a second LOCAL instance will generate
 * a SpineException.
 * The REMOTE mode is used to allow clients in the same or other JVM to
 * communicate and use the JmsMessageBroker managed by the LOCAL spine.
 * The LOCAL instance MUST be started before the REMOTE instance or it
 * will throw a SpineException
 * The TEST mode is used to test the upstate of the LOCAL spine.
 */
public class JmsSpine implements Spine {
    private static final Logger log = LoggerFactory.getLogger(JmsSpine.class);
    public static final String JMS_MESSAGE_BROKER_PORT = "PAL.JmsMessageBrokerPort";
    public static final String MAX_RECONNECT_ATTEMPTS = "PAL.MaxReconnectAttempts";
    public static final int DEFAULT_JMS_MESSAGE_BROKER_PORT = 61616;
    public static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 20;
    protected Session session;
    protected Connection connection;
    private HashMap<MessageType, Topic> messageTypeToTopicMap;
    private HashMap<MessageType, MessageProducer> messageTypeToProducerMap;
    private HashMap<DurableMessageType, TopicSubscriber> messageTypeToDurableTopicMap;
    protected JmsClient clientType;
    protected String clientId;
    protected BrokerService broker;
    protected int transactionUidIndex;
    protected int internalTransactionUidIndex;
    private boolean spineReady = false;
    private final Object internalIdLock = new Object();
    private final Object sendLock = new Object();
    private final Object consumerLock = new Object();
    private final Object shutdownLock = new Object();
    protected StateManager stateManager;
    protected SubscriptionManager subscriptionManager;
    private Timer heartbeatTimer;
    private SpineStatus spineStatus;
    // These member variables need to be protected from multithreaded access
    volatile private Map<MessageType, MessageConsumer> messageTypeToConsumersMap = new HashMap<MessageType, MessageConsumer>();
    private volatile boolean shutdown = false;

    /**
     * Create the JMS Spine. This constructor creates the Message Broker for the LOCAL
     * Spine instance and allows the REMOTE Spine instances to use it. It also employs,
     * the system properties to setup the connection port, and connection semantics.
     * @throws SpineException if there is a problem
     * @param clientType whether this JMS instance should REMOTE or LOCAL
     * @param clientId The id of the client who is using this Jms Spine instance
     */
    public JmsSpine(JmsClient clientType, String clientId) throws SpineException {
        // Get configurable broker port
        int port = getBrokerPort();
        // Get the Number of Reconnect Attempts to use
        int maxReconnectAttempts = getMaxReconnectAttempts();

        // REMOTE or LOCAL
        this.clientType = clientType;
        // The Id of the client (Lumen, Lapdog etc)
        this.clientId = clientId;
        // The index used to create unique UID's for this client. These count up.
        transactionUidIndex = 0;
        // The index used to create unique internal UID's for this client. These count down instead of up.
        internalTransactionUidIndex = -1;

        // Create a ConnectionFactory
        ActiveMQConnectionFactory connectionFactory;
        try {
            if (clientType.equals(JmsClient.TEST)) {
                log.trace("Starting test Jms Server for client '{}'", clientId);
                // This is a stripped down Spine instance. It is intended for
                // testing the upstate of the LOCAL Spine instance.
                connectionFactory = new ActiveMQConnectionFactory("tcp://127.0.0.1:" + port);
            }
            else if (clientType.equals(JmsClient.DURABLE_TEST)) {
                log.trace("Starting durable test Jms Server for client '{}'", clientId);
                // This is a stripped down Spine instance. It is intended for
                // testing the upstate of the LOCAL Spine instance. It does have failover.
                connectionFactory = new ActiveMQConnectionFactory("failover:(tcp://127.0.0.1:" + port +
                        ")?initialReconnectDelay=1000&useExponentialBackOff=false&startupMaxReconnectAttempts=5&maxReconnectAttempts=" + maxReconnectAttempts);
            }
            else if (clientType.equals(JmsClient.REMOTE)) {
                log.debug("Starting remote Jms Server for client '{}'", clientId);
                // Any JmsSpine instances on the same machine will be able to reach
                // the embedded vm broker using the tcp protocol
                connectionFactory = new ActiveMQConnectionFactory("failover:(tcp://127.0.0.1:" + port +
                        ")?initialReconnectDelay=1000&useExponentialBackOff=false&startupMaxReconnectAttempts=5&maxReconnectAttempts=" + maxReconnectAttempts);
            }
            // Must be JmsClient.LOCAL
            else {
                log.debug("Starting local Jms Server for client '{}'", clientId);
                // configure the embedded broker in the PAL JVM, there can only be one instance
                // of this type of JmsSpine
                broker = BrokerFactory.createBroker(
                        new URI("broker:(tcp://127.0.0.1:" + port + ")?" +
                              "persistent=false&" +
                              "useJmx=false&" +
                              "maximumConnections=10&keepAlive=true"));
                broker.start();
                // For high speed comms, the in vm transport protocol is used for
                // messages sent inside the this JVM
                connectionFactory = new ActiveMQConnectionFactory(broker.getVmConnectorURI());
            }
            // Create a Connection
            connection = connectionFactory.createConnection();
            if (clientType.equals(JmsClient.TEST) || clientType.equals(JmsClient.DURABLE_TEST)) {
                // if the spine type is a test client - to check the upstate of the local spine,
                // be forgiving of client id clashes when they occur when used by different JVMs
                boolean idAccepted = false;
                while (!idAccepted) {
                    try {
                        connection.setClientID(clientId);
                        idAccepted = true;
                    }
                    catch (InvalidClientIDException e) {
                        clientId = clientId + new Random(System.currentTimeMillis()).nextInt();
                        this.clientId = clientId;
                    }
                    catch (Exception e) {
                        log.warn("Unable to create an TEST Spine connection, exiting", e);
                        throw new SpineException(e);
                    }
                }
            }
            else {
                // If the spine type is a REMOTE or LOCAL spine type, there can be no duplicates
                connection.setClientID(clientId);
            }
            connection.start();
            connection.setExceptionListener(new JmsExceptionListener(this));

            // Create a Session
            boolean transacted = false;
            session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);

            // Create the topics that the system messages will pass through
            createTopics();

            // The subscription manager is used to manage subscription information for the spines
            // in this cloud. It keeps track of what spines are listening on what topic and informs
            // other spines when this spine subscribes to a topic.
            subscriptionManager = new SubscriptionManager(this);
            subscriptionManager.synchronizeWithPeers();

            // The state manager is used to track the current state of the spine for composite
            // functions such as the gather method.
            stateManager = new StateManager(this);

            // The LOCAL Spine registers on this durable topic which will free up the REMOTE spine
            // threads which are waiting to register with this LOCAL spine thread
            if (clientType.equals(JmsClient.LOCAL)) {
                // Start sending Heartbeat messages for the SpineStatus instances to listen to
                startHeartBeat();
                subscribe(REGISTER);
            }
            // WARNING: Do not perform any other start up actions for the LOCAL spine after this
            // point as the subscription above to the REGISTER message will open the DURABLE QUEUE
            // that the REMOTE Spines are trying to register on. Once this occurs, a flood of
            // new Registration messages may arrive and cause a resource deadlock if the LOCAL
            // Spine is not yet fully initialized.

            // This flag indicates that this instance of the spine is up and running and
            // ready to receive and send messages - we can be confident of this as the methods
            // above will block until the connections are established properly.
            spineReady = true;
        }
        catch (JMSException e) {
            // This exception is encountered when the LOCAL JmsSpine is not up and running and
            // a REMOTE instance tries to connect. The exception is being suppressed in favor
            // of an informative message as this is expected behavior at times - there is a loop
            // in the PALBridge code that tries to connect until it succeeds.
            log.debug("Unable to connect to Jms Spine, please try again");
            throw new SpineException(e);
        } catch (Exception e) {
            log.error("Exception thrown when configuring embedded broker", e);
            throw new SpineException(e);
        }
        // This suppresses uninteresting logging generated by the test spine instances which are used to
        // check the upstate of the master spine.
        if (!isATestSpine()) {
            log.debug("'{}' Spine start up sequence complete.", clientId);
        }
    }

    /**
     * This method will create a timer that fires every 1 second. On firing it will send a heartbeat
     * message broadcast out into the system. This informs the Spines and other inerested parties that
     * the Local Spine is operational
     */
    private void startHeartBeat() {
        int delay = 0;
        int period = 1000;
        heartbeatTimer = new Timer();

        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    sendSystemMessage(new Heartbeat(clientId, getNextInternalUid()));
                } catch (SpineException e) {
                    log.warn("LOCAL/MASTER Spine unable to send Heartbeat");
                }
            }
        }, delay, period);
    }

    /**
     * Used to support an over-riding Functional Test, it is not used in practise.
     */
    protected JmsSpine() {
    }

    /**
     * Explicitly shutdown the JMS queue connection and session. The LOCAL instance
     * is the only instance that is allowed to stop the broker. This is called by the
     * client when it wants the JmsSpine instance to go away. When in Watching mode,
     * if a client exits without sending a RequestStopWatching message, this method
     * will inform the LOCAL JmsSpine so that it can StopWatching.
     * @throws Exception if something goes wrong
     * @param loud if true try to send shutdown notification messages
     */
    public void shutdown(boolean loud) throws Exception {
        // Occasionally, two shutdown calls would be made, one by the ShutDown handler when the LOCAL
        // Spine issues a shutdown notice, and one from the Spine client when it encountered the same
        // ShutDown message from the LOCAL Spine. This was resulting in a deadlock between two and
        // sometimes even 3 threads. This synch block prevents this.
        synchronized (shutdownLock) {
            if (shutdown) {
                // Ensure that we do not attempt to call shutdown more than once as it can cause exceptions
                // in the JMS code as the connections start winding themselves up after the first request.
                return;
            }
            shutdown = true;
        }
        // This suppresses uninteresting logging generated by the test spine instances which are used to
        // check the upstate of the master spine.
        if (!isATestSpine()) {
            log.debug("Shutting down the {} Jms Spine", clientId);
        }
        // Inform LOCAL JmsSpine that the client that requested START_WATCHING
        // is now exiting - this will be interpreted as a STOP_WATCHING message
        if (loud) {
            try {
                // If creating a session throws an exception, the Message Broker has already been shut down.
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                sendSystemMessage(new JmsSpineClosing(clientId, clientType, getNextUid()));
                log.trace("JmsSpineClosing message successfully sent from spine: {}", clientId);
            }
            catch(Exception e) {
                log.trace("Spine '{}', the shutdown method was unable to deliver the SpineClosing method in time", clientId);
            }
        }
        try {
            connection.close();
            if (clientType.equals(JmsClient.LOCAL)) {
                // LOCAL Spine is shutting down, stop sending Heartbeat messages.
                heartbeatTimer.cancel();
                if (broker.isStarted()) {
                    broker.stop();
                    // This ensures the broker has cleaned up correctly before returning
                    broker.waitUntilStopped();
                }
            }
        } catch (JMSException e) {
            log.debug("Exception thrown when shutting down the " + clientId + "Jms Spine", e);
            throw(e);
        } finally {
            spineReady = false;
        }
    }

    /**
     * This method is used by the REMOTE spine instances to Shutdown the MASTER/LOCAL
     * spine. When the MASTER/LOCAL spine is told to shut down, it will in turn inform
     * the other REMOTE spines that they should wrap things up as they will no longer
     * be able to comm with each other.
     * @throws Exception if something goes wrong
     */
    public void shutdownMaster() throws Exception {
        // If this instance is the master spine, directly call its shut down method
        if (clientType.equals(JmsClient.LOCAL)) {
            try {
                ShutdownMaster shutdownThread = new ShutdownMaster(this);
                Thread thread = new Thread(shutdownThread);
                thread.start();
            } catch (Exception e) {
                log.warn("Unable to shutdown " + clientId + " properly:", e);
            }
        }
        else {
            sendSystemMessage(new com.sri.tasklearning.spine.messages.ShutdownMaster(clientId, getNextUid()));
        }
    }

    /**
     * Used to deconstruct the JMS queue connection and session when exiting. The LOCAL
     * instance is the only instance that is allowed to stop the broker. This is called
     * by the garbage collector when all references are removed. When in Watching mode,
     * if a client exits without sending a RequestStopWatching message, this method
     * will inform the LOCAL JmsSpine so that it can StopWatching.
     * @throws Throwable if something goes wrong
     */
    @Override
    protected void finalize() throws Throwable {
        // This suppresses uninteresting logging generated by the test spine instances which are used to
        // check the upstate of the master spine.
        if (!isATestSpine()) {
            log.debug("Finalizing the {} Jms Spine", clientId);
        }
        super.finalize();
        // If possible, Inform LOCAL JmsSpine that the client that requested START_WATCHING
        // is now exiting - this will be interpreted as a STOP_WATCHING message
        if (clientType.equals(JmsClient.REMOTE) || clientType.equals(JmsClient.TEST) ||
            clientType.equals(JmsClient.DURABLE_TEST)) {
            try {
                // If creating a session throws an exception, the Message Broker has already been shut down.
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                sendSystemMessage(new JmsSpineClosing(clientId, clientType, getNextUid()));
                log.trace("JmsSpineClosing message successfully sent from spine: {}", clientId);
            }
            catch(Exception e) {
                log.trace("Spine '{}', the finalize method was unable to deliver the SpineClosing method in time", clientId);
            }
        }
        try {
            spineReady = false;
            connection.close();
            if (clientType.equals(JmsClient.LOCAL)) {
                // LOCAL Spine is shutting down, stop sending Heartbeat messages.
                heartbeatTimer.cancel();
                if (broker.isStarted()) {
                    broker.stop();
                    // This ensures the broker has cleaned up correctly before returning
                    broker.waitUntilStopped();
                }
            }
        } catch (JMSException e) {
            log.warn("Exception thrown when finalizing the " + clientId + "Jms Spine", e);
            throw(e);
        }
    }

    /**
     * Get the client id for this instance of the JmsSpine
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Get the client type for this instance of the JmsSpine
     * @return the client type
     */
    public JmsClient getClientType() {
        return clientType;
    }

    /**
     * Get the number of active spine instances
     * @return the number of active spine instances
     */
    public int getSpineInstanceCount() {
        return stateManager.getSpineInstanceCount();
    }

    /**
     * Informs the Spine Client of this Spine's readiness to send and receive messages.
     * @return true if the Spine is ready
     */
    public boolean isRunning() {
        return spineReady;
    }

    /**
     * Informs the Spine Client of the LOCAL/MASTER Spines up-state
     */
    @Override
    public boolean isMasterRunning() {
        if (clientType.equals(JmsClient.LOCAL)) {
            return spineReady;
        }
        else {
            if (spineStatus == null) {
                spineStatus = new SpineStatus();
            }
            boolean running = spineStatus.isMasterRunning();
            if (running) {
                log.trace("Master is running");
            }
            else {
                log.debug("Master is not running");
            }
            return running;
        }
    }

    /**
     * This method is responsible for creating TransactionUID's for each of the
     * system clients. Depending on what message the TransactionUID's are being
     * embedded in, the client may cast the TransactionUID to a UID, the base
     * class of TransactionUID- these are for messages that do not involve
     * a transaction like exchange of messages with the same unique TransactionUID.
     * These ids start at 0 and ++
     * @return the next TransactionUID
     */
    synchronized public TransactionUID getNextUid() {
        return new TransactionUID(clientId, transactionUidIndex++);
    }

    /**
     * This method creates internal TransactionUID's that are used to identify
     * internal message that are sent between the Spine instances. They are identical
     * to the regular UID's except they decrement in index rather than increment. These
     * ids start at -1 and --
     * @return the next internal TransactionUID
     */
    TransactionUID getNextInternalUid() {
        synchronized (internalIdLock) {
            return new TransactionUID(clientId, internalTransactionUidIndex--);
        }
    }

    /**
     * Called by one of the system clients such as Lumen, Lapdog or the Bridge in order
     * to subscribe themselves as alive and interested in receiving the types of messages
     * listed in the messageType(s) parameter. These are the publicly available message
     * types that any client can send. There are also restricted system messages that only
     * the spine instances themselves are allowed to send.
     * @param handler The class that will be called when a message is received
     * @param messageTypes The message types that the client wants to receive
     * @throws SpineException if something goes wrong
     */
    public void subscribe(final MessageHandler handler, UserMessageType... messageTypes) throws SpineException {
        subscriptionManager.subscribe(handler, messageTypes);
    }

        /**
     * Called by the Spine instances of the Spine clients in order to subscribe themselves as
     * alive and interested in receiving the types of system messages listed in the
     * SystemMessageTypes parameter. These are publicly available to subscribe to but only
     * the Spine instances are permitted to send them. These messages are generally for
     * synchronization (NEW_SUBSCRIPTION) between the spines.
     * @param handler The class that will be called when a message is received
     * @param messageTypes The message types that the client wants to receive
     * @throws SpineException if something goes wrong
     */
    public void subscribe(final MessageHandler handler, SystemMessageType... messageTypes) throws SpineException {
        subscriptionManager.subscribe(handler, messageTypes);
    }

    /**
     * Called by the Spine clients to request subscription to Privileged messages. These are
     * special messages that can only be subscribed to by specific clients or only by a specific
     * number of clients. If the subscription is successful, true is returned, false otherwise.
     * Since the LOCAL Jms Spine controls access to this subscription, simultaneous subscription
     * by two racing Spines is impossible.
     * @param handler The handler that should be called when a message of this type arrives (if subscription is successful)
     * @param messageType The Type of message the client wants to subscribe to
     * @return true if subscription was permitted
     * @throws SpineException if something goes wrong
     */
    public boolean subscribe(final MessageHandler handler, PrivilegedMessageType messageType) throws SpineException {
        return subscriptionManager.subscribe(handler, messageType);
    }

    /**
     * Allows a spine to un-subscribe from a topic type
     * @param messageType the topic the spine wishes to de-register from
     * @return true if successful
     * @throws SpineException if something goes wrong
     */
    public boolean unsubscribe(MessageType messageType) throws SpineException {
        synchronized (consumerLock) {
            if (!messageTypeToConsumersMap.containsKey(messageType)) {
                log.trace("Unable to un-subscribe from topic that we are not subscribed to.");
                return false;
            }
            try {
                messageTypeToConsumersMap.get(messageType).close();
            } catch (JMSException e) {
                throw new SpineException(e);
            }
            messageTypeToConsumersMap.remove(messageType);

            subscriptionManager.unsubscribe(messageType);

            return true;
        }
    }

    /**
     * Called internally by the LOCAL Spine instance to create a durable topic to receive
     * messages from the REMOTE Spine instances on. The durable nature of the topic is to
     * guarantee message delivery. If the LOCAL instance is not yet subscribed on the
     * topic when the REMOTE Spines send a message, they will be held in memory until the LOCAL
     * Spine starts up and subscribes to the topic. Durable messages are used for critical
     * communication such as synchronization.
     * @param messageTypes The durable message types that the client wants to receive
     * @throws SpineException if something goes wrong
     */
    private void subscribe(DurableMessageType... messageTypes) throws SpineException {
        try {
            for (final Enum<DurableMessageType> messageType : messageTypes) {
                if (messageType == null) {
                    throw new SpineException("messageType was null");
                }
                log.debug("Subscribing '{}' to the '{}' topic", clientId, messageType);

                TopicSubscriber topicSubscriber = messageTypeToDurableTopicMap.get(messageType);
                topicSubscriber.setMessageListener(stateManager);
            }
        }
        catch (JMSException e) {
            log.warn("Exception thrown when registering '" + clientId + "' for durable messages on the Jms Spine", e);
            throw new SpineException(e);
        }
    }

    /**
     * This method adds a handler, provided by the client, to a listener that will be
     * notified when a message of this type is received from its topic. All MessageTypes that
     * clients are permitted to register a subscription on are available through this method.
     * If the client tries to subscribe to a bogus MessageType an exception will be thrown
     * @param handler the handler to be called when messages arrive
     * @param messageType the type of message the client wants to subscribe to
     * @throws JMSException if there is a problem
     */
    void subscribeToTopic(final MessageHandler handler, final MessageType messageType) throws JMSException {
        // Get the topic end point on which messages of this type will arrive
        Topic destination = messageTypeToTopicMap.get(messageType);
        if (destination == null) {
            log.warn("Attempting to Subscribe to unsupported topic type, there is no " +
                    "destination channel for '{}' message type", messageType);
            throw new JMSException("Attempting to Subscribe to unsupported topic type, there is no " +
                    "destination channel for '" + messageType + "' message type");
        }
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(new MessageListener() {
            public void onMessage(javax.jms.Message message) {
                Message spineMessage;
                try {
                    // this is called when a message of this type is posted to the spine. The client
                    // must be able to filter the Message instance to determine which type of message
                    // was caught - ExecutionRequest for instance.
                    spineMessage = (Message)((ObjectMessage)message).getObject();
                    log.trace("SpineInstance '{}', Sending message '{}'", clientId, messageType);
                    handler.handleMessage(spineMessage);
                    log.trace("Returned from SpineInstance '{}' handler, freeing delivery thread", clientId);
                }
                catch (Exception e) {
                    log.warn("Exception while handling message for '"
                            + clientId + "', messageType: '" + messageType + "'", e);
                }
            }
        });
        synchronized(consumerLock) {
            messageTypeToConsumersMap.put(messageType, consumer);
        }
    }

    private boolean isATestSpine() {
        return clientType.equals(JmsClient.TEST) || clientType.equals(JmsClient.DURABLE_TEST);
    }

    private boolean isARequestFromATestSpine(Message message) {
        return message.getUid().getOriginator()
                .startsWith(SpineStatus.TEST_MASTER_STATUS);
    }

    /**
     * This method sends the message into the correct topic channel. The topic is chosen
     * depending on the MessageType.
     * @param message The message to be sent in the payload
     * @param messageType The type of message that is being send
     * @return true if there exists a subscriber (excluding this spine) in the network of JmsSpine instances
     * @throws SpineException if something goes wrong
     */
    synchronized private boolean sendMessage(Message message, MessageType messageType) throws SpineException {
        // This suppresses uninteresting logging generated by the test spine instances which are used to
        // check the upstate of the master spine.
        if (!isATestSpine() && !isARequestFromATestSpine(message)) {
            log.trace("sending '{}'", message);
        }
        // Check to see if the MessageType has any subscribers, or if it is a system, or durable message
        // which do not require subscribers.
        if (subscriptionManager.calculateExpectedResponderCount(messageType) == 0 && !SystemMessageType.class.isInstance(messageType) &&
            !DurableMessageType.class.isInstance(messageType)) {
            log.warn("Send {} message failed - no subscribers found.", messageType);
            return false;
        }
        try {
            // Get the topic end point for sending messages into.
            MessageProducer producer = messageTypeToProducerMap.get(messageType);
            producer.send(session.createObjectMessage(message));
        }
        catch (JMSException e) {
            if (messageType != UNSUBSCRIBE) {
                // Don't log the warning if we're trying to shut down.
                log.warn("Exception thrown when sending message of type '" + messageType + "'", e);
            }
            throw new SpineException(e);
        }
        if (log.isDebugEnabled()) {
            String listOfTopics = "";
            List<MessageType> topics = subscriptionManager.getSubscriptionsForClient();
            for (MessageType topic : topics) {
                listOfTopics += topic + ", ";
            }
            // During initialization of the Subscription Manager the spines will not be subscribed to any topic
            if (listOfTopics.length() > 0 && !clientType.equals(JmsClient.TEST) &&
                !clientType.equals(JmsClient.DURABLE_TEST)) {
                log.trace("Client: '{}' is registered for '{}'", clientId, listOfTopics.substring(0, listOfTopics.length() - 2));
            }
        }
        log.trace("'{}' just posted into Spine", messageType);
        return true;
    }

    /**
     * Called by one of the system clients to request a message be sent through the spine
     * to other subscribed clients. The class of the Message determines which type of
     * message is being sent and on which topic it should be posted. This method will
     * allow only public messages to be sent, ie, those listed in the MessageTypes and
     * the PrivilegedMessageTypes enumerations. Attempts to send System messages orDurable
     * messages will result in an exception.
     * @param message The type of message to be sent through the spine
     * @return true if there is at least one subscriber (excluding the sender) on this topic
     * @throws SpineException if something goes wrong
     */
    public boolean send(Message message) throws SpineException {
        synchronized (sendLock) {
            MessageType messageType = message.getMessageType();
            if (SystemMessageType.class.isInstance(messageType) ||
                    DurableMessageType.class.isInstance(messageType)) {
                log.warn("Attempt to send unauthorized messageType: '{}'", messageType);
                throw new SpineException("Attempt to send unauthorized MessageType: " + message);
            }
            if (messageTypeToProducerMap.get(messageType) == null) {
                log.warn("Attempt to send unsupported MessageType: {}", messageType);
                throw new SpineException("Attempt to send unsupported MessageType: " + message);
            }
            else {
                return sendMessage(message, messageType);
            }
        }
    }

    /**
     * Called by the Spine instances (note the protected access) to send system or durable
     * level messages to other Spine instances and sometimes, Spine Clients. Only the
     * message types that have been configured will be allowed to be sent.
     * @param message The type of message to be sent through the spine
     * @throws SpineException if something goes wrong
     */
    void sendSystemMessage(Message message) throws SpineException {
        synchronized (sendLock) {
            MessageType messageType = message.getMessageType();

            if (!SystemMessageType.class.isInstance(messageType) &&
                    !DurableMessageType.class.isInstance(messageType)) {
                log.warn("Attempt to send unsupported Durable or System MessageType: {}", message);
                throw new SpineException("Attempt to send unsupported Durable or SystemMessageType: " + message);
            }
            if (messageTypeToProducerMap.get(messageType) == null) {
                log.warn("Attempt to send unsupported MessageType: {}", message);
                throw new SpineException("Attempt to send unsupported MessageType: " + message);
            }
            else {
                sendMessage(message, messageType);
            }
        }
    }

    /**
     * Return the number of spines (excluding this one) that are registered to receive
     * messages of this type
     * @param messageType that we want a subscriber count for
     * @return The number of subscribers (excluding this one)
     * @throws SpineException if something goes wrong
     */
    public int calculateExpectedResponderCount(MessageType messageType) throws SpineException {
        return subscriptionManager.calculateExpectedResponderCount(messageType);
    }

    /**
     * This method allows the calling Spine Client to issue a request to every (excluding itself)
     * spine in the system. This is a synchronous call that will block until the results have been gathered.
     * @param message the instance of the message to be broadcast
     * @param timeout the duration the gather call will wait for replies before giving up
     * @return the array of result messages
     */
    public Message[] gather(Message message, long timeout) throws SpineException {
        return stateManager.gather(message, timeout);
    }

    /**
     * This method is an asynchronous version of the gather method. It will return after
     * being called an the caller can perform other work while the gather request is being
     * worked on.
     * @param message The gather request message
     * @param timeout The timeout that should be used before giving up the gather request
     * @param callback The class to be called when the gather completes or fails.
     */
    public void gatherAsynchronous(Message message, long timeout, GatherCallback callback) throws SpineException {
        stateManager.gatherAsynchronous(message, timeout, callback);
    }


    /**
     * Get the Port to use for the Message Broker - the default may be overridden by
     * a system property
     * @return the Message Broker port to use
     */
    protected final int getBrokerPort() {
        int port = DEFAULT_JMS_MESSAGE_BROKER_PORT;
        String customPort = System.getProperty(JMS_MESSAGE_BROKER_PORT);
        if (customPort != null) {
            port = Integer.parseInt(customPort);
            log.debug("Starting JmsBroker on custom port: {}", port);
        }
        return port;
    }

    /**
     * Get the Max Number of times to attempt reconnect when the LOCAL Spine is down - the default may be
     * overridden by a system property
     * @return the Max Reconnect to use
     */
    protected final int getMaxReconnectAttempts() {
        int reconnectAttempts = DEFAULT_MAX_RECONNECT_ATTEMPTS;
        String customReconnectAttempts = System.getProperty(MAX_RECONNECT_ATTEMPTS);
        if (customReconnectAttempts != null) {
            reconnectAttempts = Integer.parseInt(customReconnectAttempts);
            log.debug("Starting JmsBroker with custom reconnect Attempts: {}", reconnectAttempts);
        }
        return reconnectAttempts;
    }

    /**
     * Create topics for messages to be sent on. Some topics are special and are there
     * to allow the JmsSpine Instances to communicate with each other, such as the
     * subscription synchronization messages. Topics are created and then added to two
     * maps for lookup keyed on the MessageTypes provided in the send call. The first
     * map manages the topic end points. The second map manages the topic entry points
     * @throws JMSException if something goes wrong
     */
    protected final void createTopics() throws JMSException {
        // Create map of Topics, one for each Message Type, both standard and
        // system. These will be used to register listeners on so that when a message
        // arrives on that topic, the listener will be called
        messageTypeToTopicMap = new HashMap<MessageType, Topic>();

        // This is a specialized message to topic map as it contains durable topics.
        // These are topics that will never disappear from a queue until the intended
        // receiver picks them up.
        messageTypeToDurableTopicMap = new HashMap<DurableMessageType, TopicSubscriber>();

        // Create map of Producers, one for each Message Type, both standard and
        // system. These will be used to send messages on so that the message will be
        // routed onto the correct topic.
        messageTypeToProducerMap = new HashMap<MessageType, MessageProducer>();


        // First add the topic end points for the standard messages
        UserMessageType[] messageTypes = UserMessageType.values();
        for (UserMessageType messageType : messageTypes) {
            Topic destination = session.createTopic(messageType.name());
            messageTypeToTopicMap.put(messageType, destination);

            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            messageTypeToProducerMap.put(messageType, producer);
        }

        // Next add the topic end points for the system messages
        SystemMessageType[] systemMessageTypes = SystemMessageType.values();
        for (SystemMessageType messageType : systemMessageTypes) {
            Topic destination = session.createTopic(messageType.name());
            messageTypeToTopicMap.put(messageType, destination);

            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            messageTypeToProducerMap.put(messageType, producer);
        }

        // Next add the topic end points for the privileged messages
        PrivilegedMessageType[] privilegedMessageTypes = PrivilegedMessageType.values();
        for (PrivilegedMessageType messageType : privilegedMessageTypes) {
            Topic destination = session.createTopic(messageType.name());
            messageTypeToTopicMap.put(messageType, destination);

            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            messageTypeToProducerMap.put(messageType, producer);
        }

        // Finally add the durable topic (a durable topic is one which holds the messages
        // until the consuming client is active and able to receive them). This is the
        // first message sent from the REMOTE Spine instances to the LOCAL Spine instance
        // in order to advertise their existence. This allows the LOCAL Spine instance,
        // which manages the secondary REMOTE Spine instances, to ensure that each
        // instance has a unique id, to get an accurate head count, and to signal the
        // REMOTE instances when the LOCAL Spine instance is ready (on occasion, the
        // REMOTE instances were sending messages before the LOCAL instance had a chance
        // to register on them, these messages were lost as they were not durable)
        Topic topic = session.createTopic(DurableMessageType.REGISTER.name());
        if (clientType.equals(JmsClient.LOCAL)) {
            TopicSubscriber topicSubscriber = session.createDurableSubscriber(topic, DurableMessageType.REGISTER.name());
            messageTypeToDurableTopicMap.put(DurableMessageType.REGISTER, topicSubscriber);
        }

        // Even though the topic is special, as it is durable, sending messages into the
        // topic is the same as with any other topic, hence it does not need to be treated
        // differently.
        MessageProducer producer = session.createProducer(topic);
        // This message has a persistent delivery mode as the REMOTE instance will hang
        // if it is lost in transit.
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        messageTypeToProducerMap.put(DurableMessageType.REGISTER, producer);
    }

}
