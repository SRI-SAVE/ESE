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

import static com.sri.tasklearning.spine.messages.SystemMessageType.EXISTING_SUBSCRIPTIONS;
import static com.sri.tasklearning.spine.messages.SystemMessageType.EXISTING_SUBSCRIPTIONS_REQUEST;
import static com.sri.tasklearning.spine.messages.SystemMessageType.NEW_SUBSCRIPTION;
import static com.sri.tasklearning.spine.messages.SystemMessageType.PRIVILEGED_SUBSCRIPION_REQUEST;
import static com.sri.tasklearning.spine.messages.SystemMessageType.PRIVILEGED_SUBSCRIPION_RESPONSE;
import static com.sri.tasklearning.spine.messages.SystemMessageType.SHUTDOWN_MASTER;
import static com.sri.tasklearning.spine.messages.SystemMessageType.SPINE_CLOSING;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSException;

import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ExistingSubscriptions;
import com.sri.tasklearning.spine.messages.ExistingSubscriptionsRequest;
import com.sri.tasklearning.spine.messages.JmsSpineClosing;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.MessageType;
import com.sri.tasklearning.spine.messages.NewSubscription;
import com.sri.tasklearning.spine.messages.PrivilegedMessageType;
import com.sri.tasklearning.spine.messages.PrivilegedSubscriptionRequest;
import com.sri.tasklearning.spine.messages.PrivilegedSubscriptionResponse;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.messages.Unsubscribe;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SubscriptionManager is responsible for keeping track of what spines are
 * registered to receive what messages. When this spine registers or de-registers
 * for a topic, a message blast is sent to all other spines to let them know. When
 * other spines register or de-register for a message topic, this class intercepts
 * the notification and records the fact.
 * This information allows the spine to determine when a message would not be
 * received by anyone an also how many
 * responses to expect when sending a gather request.
 */
public class SubscriptionManager implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionManager.class);
    private JmsSpine spine;
    private JmsClient clientType;
    private String clientId;
    protected BlockingMessageSemaphore subscriptionsUpdated;
    final Object subscriberSemaphore = new Object();
    volatile protected Map<MessageType, HashSet<String>> subscriberCountMap;
    final Object typeStoreLock = new Object();
    private final Object subscribeLock = new Object();
    volatile private boolean typeStoreDeclared = false;
    BlockingMessageSemaphore privilegedSubscription;

    public SubscriptionManager(JmsSpine parentSpine) {
        this.spine = parentSpine;
        this.clientType = spine.getClientType();
        this.clientId = spine.getClientId();
        // Used to coordinate existing subscriptions between LOCAL and REMOTE
        subscriptionsUpdated = new BlockingMessageSemaphore(spine);
        // Keeps track of the set of subscribed topics for each of the other Spines
        subscriberCountMap = new HashMap<MessageType, HashSet<String>>();
        // Used to ask and wait for permission to subscribe to a privileged subscription
        privilegedSubscription = new BlockingMessageSemaphore(spine);
    }

    /**
     * Synchronizes with the other Spines in the system during startup. The REMOTE or
     * TEST spines will ask the LOCAL Spine for information on existing subscriptions
     * in the system and the LOCAL Spine will start listening on the subscription
     * channels for requests from the REMOTE/TEST Spines.
     * @throws com.sri.tasklearning.spine.SpineException if something goes wrong
     */
    public void synchronizeWithPeers() throws SpineException {
        if (clientType.equals(JmsClient.REMOTE) || clientType.equals(JmsClient.TEST) ||
            clientType.equals(JmsClient.DURABLE_TEST)) {
            // Subscribe to the subscription notification messages.
            subscribe(this, NEW_SUBSCRIPTION, EXISTING_SUBSCRIPTIONS,
                    PRIVILEGED_SUBSCRIPION_RESPONSE, SPINE_CLOSING);

            log.trace("Request for existing subscriptions sent for client '{}'", clientId);
            try {
                // Send the existing subscription request to the LOCAL Spine and wait for a response
                Message message = new ExistingSubscriptionsRequest(clientId, spine.getNextInternalUid());
                subscriptionsUpdated.await(message);
            } catch (InterruptedException e) {
                log.warn("Spine await was interrupted while waiting for the ExistingSubscriptionsRequest to be " +
                        "fulfilled");
            }
            log.trace("Existing Subscriptions received for client '{}'", clientId);
        }
        // Must be JmsClient.LOCAL
        else {
            // Subscribe to the subscription channels.
            // Note: SPINE_CLOSING is a critical message that can have far reaching consequences to the system
            // depending on which spine is shutting down and what their commitments to the system
            // are. As such, this message is subscribed to by both the StateManager and
            // SubscriptionManager for individual special handling.
            subscribe(this, NEW_SUBSCRIPTION, EXISTING_SUBSCRIPTIONS_REQUEST,
                    SPINE_CLOSING, PRIVILEGED_SUBSCRIPION_REQUEST,
                    SHUTDOWN_MASTER);
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
        synchronized (subscribeLock) {
            try {
                for (UserMessageType messageType : messageTypes) {
                    if (messageType == null) {
                        throw new SpineException("messageType was null");
                    }
                    log.trace("Subscribing '{}' to the '{}' topic", clientId, messageType);
                    // Tell the other JmsSpine instances that at least 1 client will consume this message type
                    notifyOtherSpinesOfTopicSubscription(messageType);
                    // Create a listener that will call the handler when a message of that type arrives
                    spine.subscribeToTopic(handler, messageType);
                }
            }
            catch (JMSException e) {
                log.warn("Exception thrown when registering '" + clientId + "' for messages on the Jms Spine", e);
                throw new SpineException(e);
            }
        }
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
        synchronized (subscribeLock) {
            try {
                for (SystemMessageType messageType : messageTypes) {
                    if (messageType == null) {
                        throw new SpineException("messageType was null");
                    }
                    log.trace("Subscribing '{}' to the '{}' topic", clientId, messageType);
                    // Create a listener that will call the handler when a message of that type arrives
                    spine.subscribeToTopic(handler, messageType);
                }
            }
            catch (JMSException e) {
                log.warn("Exception thrown when registering '" + clientId
                        + "' for messages on the Jms Spine", e);
                throw new SpineException(e);
            }
        }
    }

    public boolean subscribe(final MessageHandler handler, PrivilegedMessageType messageType) throws SpineException {
        synchronized (subscribeLock) {
            try {
                if (messageType == null) {
                    throw new SpineException("messageType was null");
                }
                // Currently there is only one Privileged message type so we do not need to check the message type
                // to decided what to do with it. As we add new privileged messages we will need to react differently
                // to each one.
                if (!typeStoreDeclared) {
                    if (clientType == com.sri.tasklearning.spine.impl.jms.JmsClient.LOCAL) {
                        setTypeStoreDeclared();
                        log.trace("Subscribing '{}' to the '{}' topic", clientId, messageType);
                        // Create a listener that will call the handler when a message of that type arrives
                        spine.subscribeToTopic(handler, messageType);
                        notifyOtherSpinesOfTopicSubscription(PrivilegedMessageType.TYPE_STORE_REQUEST);
                        return true;
                    }
                    // Must be JmsClient.REMOTE
                    else {
                        // Since the LOCAL Spine instance manages the access to the privileged messages, we
                        // need to ask permission to subscribe to this message type.
                        PrivilegedSubscriptionRequest message = new PrivilegedSubscriptionRequest(clientId, spine.getNextInternalUid(), messageType);
                        // The await method will send the message and block, waiting for an answer
                        privilegedSubscription.await(message);
                        // We got a response, now lets see what the answer was
                        if (typeStoreDeclared) {
                            // Success, this spine instance now owns that subscription
                            // Create a listener that will call the handler when a message of that type arrives
                            spine.subscribeToTopic(handler, messageType);
                            notifyOtherSpinesOfTopicSubscription(PrivilegedMessageType.TYPE_STORE_REQUEST);
                            return typeStoreDeclared;
                        }
                        else {
                            // Failure, this spine was not timely enough to successfully subscribe
                            // This will prevent unnecessary subsequent subscription calls
                            typeStoreDeclared = true;
                            return false;
                        }
                    }
                }
                else {
                    log.trace("Unable to Subscribe '{}' to the '{}' privileged topic, another " +
                            "client has already registered as the TypeStore", clientId, messageType);
                    return false;
                }
            }
            catch (JMSException e) {
                log.warn("Exception thrown when registering " + clientId + " for messages on the Jms Spine", e);
                throw new SpineException(e);
            }
            catch (InterruptedException e) {
                log.warn("Exception thrown when registering " + clientId + " for privileged messages on the Jms Spine", e);
                throw new SpineException(e);
            }
        }
    }

    /**
     * Sets the TypeStoreDeclared flag to true - since there can only be one, this flag will lock
     * out subsequent requests for TypeStore ownership.
     */
    void setTypeStoreDeclared() {
        synchronized (typeStoreLock) {
            typeStoreDeclared = true;
        }
    }

    /**
     * Checks to see if the TypeStore has already been declared by another spine.
     * @return true if TypeStore ownership is no longer available.
     */
    boolean isTypeStoreDeclared() {
        synchronized (typeStoreLock) {
            return typeStoreDeclared;
        }
    }

    /**
     * Sends a message to all instances of the JMS Spine to inform them that the messageType
     * has a new subscriber. This allows the other Jms Spine instances to know if they are
     * sending a message that no one has registered to receive.
     * @param messageType The type of message that a client of this Spine just subscribed to
     * @throws com.sri.tasklearning.spine.SpineException if there is a problem
     */
    public void notifyOtherSpinesOfTopicSubscription(MessageType messageType) throws SpineException {
        synchronized (subscriberSemaphore) {
            if (!subscriberCountMap.containsKey(messageType)) {
                // Inialize the set for this message type
                subscriberCountMap.put(messageType, new HashSet<String>());
            }
            // Add this Spine as a known subscriber
            subscriberCountMap.get(messageType).add(clientId);
        }
        // Send a message to the other spines so that they know about this spines interest in this topic
        spine.sendSystemMessage(new NewSubscription(clientId, spine.getNextInternalUid(), messageType));
    }

    /**
     * Sends a message to all instances of the JMS Spine to inform them that the messageType
     * has lost a subscriber. This allows the other Jms Spine instances to know if they are
     * sending a message that no one has registered to receive.
     * @param messageType The type of message that a client of this Spine just un-subscribed from
     * @throws SpineException if something goes wrong
     */
    public void unsubscribe(MessageType messageType) throws SpineException {
        if (SystemMessageType.class.isInstance(messageType)) {
            // No need to notify other spines as system messages do not warrant notification
            return;
        }
        synchronized (subscriberSemaphore) {
            // Remove this Spine as a known subscriber
            subscriberCountMap.get(messageType).remove(clientId);
        }
        // Send a message to the other spines so that they know about this spines elapsed interest in this topic
        spine.sendSystemMessage(new Unsubscribe(clientId, spine.getNextInternalUid(), messageType));
    }

    /**
     * Inspects the subscription records to see how many Spine Clients should be expected to
     * respond to this gather query.
     * @param messageType The message to be submitted to the gather function
     * @return the number of expected responses
     * @throws SpineException if something goes wrong
     */
    public int calculateExpectedResponderCount(MessageType messageType) throws SpineException {
        int expectedResponders;
        Set<String> list;
        synchronized (subscriberSemaphore) {
            if (!subscriberCountMap.containsKey(messageType)) {
                return 0;
            }
            expectedResponders = subscriberCountMap.get(messageType).size();
            // The list of known subscribers to this message
            list = subscriberCountMap.get(messageType);
        }

        for (Object o : list) {
            if (o.equals(clientId)) {
                // If this spine is listed as a subscriber drop this number as we will not answer ourselves.
                expectedResponders--;
            }
            else {
                log.trace("Subscriber: {}", o);
            }
        }
        log.trace("There are '{}' Subscribers to the '{}' message type", expectedResponders, messageType);
        return expectedResponders;
    }

    /**
     * Get the message types that this client is subscribed to.
     * @return The message types this client is subscribed to
     */
    public List<MessageType> getSubscriptionsForClient() {
        List<MessageType> listOfTopics = new ArrayList<MessageType>();
        synchronized (subscriberSemaphore) {
            for (MessageType messageTypeString : subscriberCountMap.keySet()) {
                if (subscriberCountMap.get(messageTypeString).contains(clientId)) {
                    listOfTopics.add(messageTypeString);
                }
            }
        }
        return listOfTopics;
    }

    /**
     * The SubscriptionManager implements the MessageHandler interface. This requires that
     * it be able to handle messages of the registered type when they arrive. These messages
     * are then handled based on their type.
     * @param message the message this class registered to receive
     * @throws MessageHandlerException if something goes wrong
     */
    @Override
    public void handleMessage(Message message) throws MessageHandlerException {

        // This message is sent by the LOCAL Spine with details of all existing
        // subscriptions to the REMOTE Spines in response to an
        // ExistingSubscriptionsRequest.
        if (ExistingSubscriptions.class.isInstance(message)) {
            ExistingSubscriptions existingSubscriptions = (ExistingSubscriptions) message;
            Map<MessageType, HashSet<String>> messageTypes = existingSubscriptions.getSubscriptionTypes();
            log.trace("Client '{}' Received Existing Subscriptions notification: '{}'", clientId, messageTypes);
            for (MessageType messageType : messageTypes.keySet()) {
                HashSet<String> subscriberSetFromMaster = messageTypes.get(messageType);
                subscriberSetFromMaster.remove(clientId);
                synchronized (subscriberSemaphore) {
                    if (!subscriberCountMap.containsKey(messageType)) {
                        subscriberCountMap.put(messageType, new HashSet<String>());
                    }
                    HashSet<String> currentSubscriberSet = subscriberCountMap.get(messageType);
                    currentSubscriberSet.addAll(subscriberSetFromMaster);
                    subscriberCountMap.put(messageType, currentSubscriberSet);
                }
            }
            // This signal method will alert the Spine instance that the existing subscriptions
            // have arrived and construction can continue.
            subscriptionsUpdated.signal();
        }

        // This message can be sent by any Spine instance to indicate it is subscribed
        // to a MessageType
        else if (NewSubscription.class.isInstance(message)) {
            NewSubscription newSubscription = (NewSubscription) message;
            // Don't process messages from self, subscription was already added
            if (newSubscription.getSubscriber().equals(clientId)) {
                return;
            }
            MessageType messageType = newSubscription.getSubscriptionType();
            log.trace("Spine {} Received NewSubscription subscription notification from: '{}'", clientId, message);
            synchronized (subscriberSemaphore) {
                if (!subscriberCountMap.containsKey(messageType)) {
                    subscriberCountMap.put(messageType, new HashSet<String>());
                }
                subscriberCountMap.get(messageType).add(newSubscription.getSubscriber());
            }
        }

        // This message is sent by the REMOTE Spines during startup to the LOCAL Spine
        // in order to synchronize their knowledge of which messages have subscribers.
        else if (ExistingSubscriptionsRequest.class.isInstance(message)) {
            try {
                ExistingSubscriptionsRequest existingSubsMessage = (ExistingSubscriptionsRequest) message;
                log.trace("Got request for existing subscriptions from '{}'", existingSubsMessage.getSender());
                spine.sendSystemMessage(new ExistingSubscriptions(clientId, existingSubsMessage.getUid(),
                        subscriberCountMap, existingSubsMessage.getSender()));
            }
            catch (Exception e) {
                log.warn("Exception while sending Subscription Notification", e);
            }
        }

        // This message is sent from one of the REMOTE spines to request that the LOCAL
        // Spine wind up operations and shut down. The shutdown master thread will then
        // take the necessary action.
        else if (com.sri.tasklearning.spine.messages.ShutdownMaster.class.isInstance(message)) {
            // Only the master spine should receive and process this message
            if (clientType.equals(JmsClient.LOCAL)) {
                String sender = message.getSender();
                log.info("Spine instance {} just requested that the Master/Local Spine {} shut down", sender, clientId);
                try {
                    ShutdownMaster shutdownThread = new ShutdownMaster(spine);
                    Thread thread = new Thread(shutdownThread);
                    thread.start();
                } catch (Exception e) {
                    log.warn("Unable to shutdown " + clientId + " properly:", e);
                }
            }
        }

        // This message is sent to the Spine instances from a REMOTE Spine instance when
        // it is about to shutdown. This block initiates the shutdown of this spine if the
        // sender is the LOCAL spine (since REMOTE spines cannot operate without it). The
        // subscription information is also updated to show that spine will no longer be
        // receiving messages
        else if (JmsSpineClosing.class.isInstance(message) ) {
            // Remove the closing spine subscriptions from the log so we don't depend on it
            // for responses during gather.
            String closingSpine = message.getSender();
            JmsClient spineType = ((JmsSpineClosing)message).getSpineType();
            if (spineType.equals(JmsClient.LOCAL) && !clientType.equals(JmsClient.LOCAL)) {
                log.info("The master Spine {} is about to shut down, this spine: {} will exit now...", closingSpine, clientId);
                ShutdownSlave shutdownThread = new ShutdownSlave(spine);
                Thread thread = new Thread(shutdownThread);
                thread.start();
            }

            synchronized (subscriberSemaphore) {
                for (MessageType messageType : subscriberCountMap.keySet()) {
                    if (log.isTraceEnabled()) {
                        log.trace(
                                "Spine '{}' removing '{}' from subscriber count for message '{}'",
                                new Object[] { clientId, closingSpine,
                                        messageType });
                    }
                    subscriberCountMap.get(messageType).remove(closingSpine);
                }
            }
        }

        // This message is sent by a REMOTE Spine instance to ask the LOCAL Spine
        // instance for permission to subscribe to a Privileged Message type
        else if (PrivilegedSubscriptionRequest.class.isInstance(message)) {
            try {
                synchronized (this) {
                    TransactionUID uid = ((PrivilegedSubscriptionRequest)message).getUid();
                    boolean subscriptionAvailable = true;
                    if (isTypeStoreDeclared()) {
                        // Tell client that no, the TYPE_STORE_REQUEST has already been subscribed to
                        spine.sendSystemMessage(new PrivilegedSubscriptionResponse(clientId, uid, !subscriptionAvailable));
                    }
                    else {
                        // Tell client that yes, it is the only subscriber to TYPE_STORE_REQUEST
                        spine.sendSystemMessage(new PrivilegedSubscriptionResponse(clientId, uid, subscriptionAvailable));
                        // Now prevent further subscriptions
                        setTypeStoreDeclared();
                    }
                }
            } catch (Exception e) {
                log.warn("Exception while sending SerialNumberResponse Message", e);
            }
        }

        // This message is sent by a REMOTE Spine instance to ask the LOCAL Spine
        // instance for permission to subscribe to a Privileged Message type
        else if (PrivilegedSubscriptionResponse.class.isInstance(message)) {
            try {
                PrivilegedSubscriptionResponse responseMessage = ((PrivilegedSubscriptionResponse)message);
                // Check if this was a request this Spine instance sent
                if (responseMessage.getUid().getOriginator().equals(clientId)) {
                    boolean permission = responseMessage.getPermission();
                    if (permission) {
                        setTypeStoreDeclared();
                    }
                    privilegedSubscription.signal();
                }
            } catch (Exception e) {
                log.warn("Exception while sending SerialNumberResponse Message", e);
            }
        }

        else {
            log.error("Unrecognized Internal message: {}", message);
        }
    }

}
