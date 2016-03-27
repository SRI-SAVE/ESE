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

import static com.sri.tasklearning.spine.messages.SystemMessageType.REGISTER_CONFIRMATION;
import static com.sri.tasklearning.spine.messages.SystemMessageType.SPINE_CLOSING;
import static com.sri.tasklearning.spine.messages.UserMessageType.EXECUTOR_LIST_QUERY;
import static com.sri.tasklearning.spine.messages.UserMessageType.EXECUTOR_LIST_RESULT;
import static com.sri.tasklearning.spine.messages.UserMessageType.REQUEST_START_WATCHING;
import static com.sri.tasklearning.spine.messages.UserMessageType.REQUEST_STOP_WATCHING;
import static com.sri.tasklearning.spine.messages.UserMessageType.SERIAL_NUMBER_REQUEST;
import static com.sri.tasklearning.spine.messages.UserMessageType.TYPE_LIST_QUERY;
import static com.sri.tasklearning.spine.messages.UserMessageType.TYPE_LIST_RESULT;
import static com.sri.tasklearning.spine.messages.UserMessageType.TYPE_QUERY;
import static com.sri.tasklearning.spine.messages.UserMessageType.TYPE_RESULT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.DurableMessageType;
import com.sri.tasklearning.spine.messages.ExecutorListResult;
import com.sri.tasklearning.spine.messages.GatherIssues;
import com.sri.tasklearning.spine.messages.JmsSpineClosing;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.MessageType;
import com.sri.tasklearning.spine.messages.Register;
import com.sri.tasklearning.spine.messages.RegisterConfirmation;
import com.sri.tasklearning.spine.messages.RequestStartWatching;
import com.sri.tasklearning.spine.messages.RequestStopWatching;
import com.sri.tasklearning.spine.messages.SerialNumberRequest;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.StartWatching;
import com.sri.tasklearning.spine.messages.StopWatching;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.messages.TypeListResult;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.messages.contents.UID;
import com.sri.tasklearning.spine.util.SpineStatus;

/**
 * The StateManager is responsible for managing multi-step spine calls such as the
 * the gather command and the registration handshake with the LOCAL spine.
 */
public class StateManager implements MessageHandler, MessageListener {

    private static final Logger log = LoggerFactory.getLogger(StateManager.class);
    private JmsSpine spine;
    private JmsClient clientType;
    private String clientId;
    private ArrayList<GatherInstance> gatheringThreads;
    protected BlockingMessageSemaphore registrationSemaphore;
    private boolean registered;
    private boolean registrationPermissionMessageReceived;
    volatile private Set<String> watchingSpines;
    private boolean watching = false;
    private int serialNumber = 0;
    protected Set<String> spineInstanceList;
    private final Object gatherLock = new Object();
    // These are used to support the synchronous gather method
    private final Message[][] responseMessages = {new Message[0]};
    private final Object semaphore = new Object();
    private boolean notified = false;

    public StateManager(JmsSpine parentSpine) throws SpineException {
        this.spine = parentSpine;
        this.clientType = spine.getClientType();
        this.clientId = spine.getClientId();
        gatheringThreads = new ArrayList<GatherInstance>();

        // Used to coordinate the registration between the remote and local spines.
        registrationSemaphore = new BlockingMessageSemaphore(5000, spine);

        if (clientType.equals(JmsClient.REMOTE) || clientType.equals(JmsClient.TEST) ||
            clientType.equals(JmsClient.DURABLE_TEST)) {
            // Subscribe to the RegisterConfirmation message first, this message will indicate the
            // REMOTE Spine has permission to  continue construction - no other subscriptions will
            // be allowed until that time.
            spine.subscribe(this, REGISTER_CONFIRMATION);

            try {
                // This sends the register message to the LOCAL spine and then waits for an
                // answer. If the id is not already allocated to another spine it should give
                // permission to continue.
                int tries = 0;
                for (int i = 0; i < 120; i++) {
                    Message message = new Register(clientId, spine.getNextInternalUid());
                    tries++;
                    registrationSemaphore.await(message);
                    if (registrationPermissionMessageReceived) {
                        break;
                    }
                }
                if (tries > 1) {
                    log.info("Initial Registration Message was lost for Spine " + clientId + ". Resend attempts: " + tries);
                }
                if (!registered) {
                    if (registrationPermissionMessageReceived) {
                        log.error("Unable to start Spine with Client Id '{}', this ID is " +
                                                 "already allocated to another instance", clientId);
                        throw new SpineException("Unable to start Spine with Client Id '" + clientId
                                               + "' this ID is already allocated to another instance");
                    } else {
                        log.error("The {} Spine tried to get permission to start but did not get an answer.", clientId);
                        throw new SpineException("Unable to start Spine with Client Id '" + clientId
                                               + "', requested permission to start but did not get an answer.");
                    }
                }
                log.trace("Spine Client '{}' registered successfully", clientId);
            } catch (InterruptedException e) {
                log.warn("Spine await was interrupted while waiting for the Register request to be " +
                        "fulfilled");
            }

            // Subscribe to the messages that may be returned to the gather() method call
            spine.subscribe(this, TYPE_LIST_RESULT, TYPE_RESULT, EXECUTOR_LIST_RESULT);
        }
        // Must be JmsClient.LOCAL
        else {
            // Subscribe the ExecutionAcceptanceWatcher to the execution request and execution
            // status messages. This class makes sure that execution requests are matched with
            // an executor. If no executor accepts the request, a notification is sent to the
            // requesting Spine Client. This is the role of the LOCAL Spine only.
            spine.subscribe(new ExecutionAcceptanceWatcher(spine),
                    UserMessageType.EXECUTE_REQUEST, UserMessageType.EXECUTION_STATUS, UserMessageType.REQUEST_IGNORED);
            // Subscribe to the Specialized messages that only the LOCAL Spine deals with, plus the
            // messages that may be returned to the gather() method call
            spine.subscribe(this, REQUEST_START_WATCHING, REQUEST_STOP_WATCHING,
                                            SERIAL_NUMBER_REQUEST, TYPE_LIST_RESULT,
                                            TYPE_RESULT, EXECUTOR_LIST_RESULT);
            // This is a critical message that can have far reaching consequences to the system
            // depending on which spine is shutting down and what their commitments to the system
            // are. As such, this message is subscribed to by both the StateManager and
            // SubscriptionManager for individual special handling.
            spine.subscribe(this, SPINE_CLOSING);
        }
    }

    /**
     * This gather method allows the spine client to issue fire and forget(kind of)
     * gather requests that will work while the spine client does other work and or
     * issues other simultaneous gather requests. Once the answers have been gathered, or
     * the time limit has expired, the callback implemetation is called to inform the
     * caller that the gather request is complete.
     * NOTE: How to make message 'Gatherable'.
     * 1) Add the message type to the exception conditional in the method below.
     * 2) When a message arrives, call informGatheringThreads() so they can decide if it is for them.
     * @param message the request message to be issued, the responses will be gathered
     * @param timeout the maximum time the gather effort should wait for all responses before giving up
     * @param callback called when the gather call completes or fails
     * @throws SpineException if something goes wrong
     */
    public void gatherAsynchronous(Message message, long timeout, GatherCallback callback) throws SpineException {
        synchronized(gatherLock) {
            UID gatherIdentifier = message.getUid();

            MessageType messageType = message.getMessageType();
            // It would be trivial to make all message types gatherable but it does not make sense as there are
            // many message that cannot and/or should not be used in a gather call, such as response messages
            // themselves.
            if (messageType != TYPE_LIST_QUERY && messageType != TYPE_QUERY && messageType != EXECUTOR_LIST_QUERY) {
                log.info("The '{}' message type is not currently supported by the gather command", messageType);
                callback.warning(GatherIssues.NOT_GATHERABLE);
                return;
            }

            int expectedResponses = spine.calculateExpectedResponderCount(messageType);
            if (expectedResponses == 0) {
                log.debug("No subscribers to send the gather request to. The other spines may have gone down or " +
                        "have not subscribed to this message type yet.");
                callback.warning(GatherIssues.NO_SUBSCRIBERS);
                return;
            }

            // Create the gather instance - this new thread will keep track of the incoming messages
            GatherInstance gatherInstance = new GatherInstance(expectedResponses, timeout, gatherIdentifier, callback);
            gatheringThreads.add(gatherInstance);
            Thread thread = new Thread(gatherInstance);
            thread.start();

            // Send the request message that will ask all the other spines to respond
            if (SystemMessageType.class.isInstance(messageType) ||
                    DurableMessageType.class.isInstance(messageType)) {
                spine.sendSystemMessage(message);
            }
            else {
                spine.send(message);
            }
        }
    }

    /**
     * Blocking gather request. When called this method does not return to the caller until
     * all the expected responses are received or the timeout is reached. This gather request
     * calls the Asynchronous gather and blocks until it is complete. It will likely be removed
     * in the future but will be kept here for now to support the expected behavior of the
     * gather mechanism. If this method is ever removed, the sanity check for the TypeQueryResults
     * below should be moved to the calling spine client.
     * @param message The gather request message
     * @param timeout The patience of the gather requester
     * @return The responses
     * @throws SpineException if something goes wrong
     */
    public Message[] gather(Message message, long timeout) throws SpineException {
        GatherCallbackImpl callback = new GatherCallbackImpl();
        synchronized (semaphore) {
            notified = false;

            // Call gather asynchronously
            gatherAsynchronous(message, timeout, callback);

            // And then wait for a response.
            while (!notified) {
                try {
                    semaphore.wait(100);
                } catch (InterruptedException e) {
                    log.warn("Synchronous Gather wait thread was interrupted by an exception");
                }
            }
        }
        // Sanity check the TypeQueryResults. Make sure all positive
        // responses are the same.
        if (message.getMessageType() == TYPE_QUERY) {
            String type = null;
            for (Message replyMsg : responseMessages[0]) {
                if (replyMsg != null && replyMsg instanceof TypeResult) {
                    String thisType = ((TypeResult) replyMsg).getTypeStr();
                    if (thisType == null) {
                        continue;
                    }
                    if (type == null) {
                        type = thisType;
                    }
                    if (!type.equals(thisType)) {
                        log.warn("Action models are out of sync! Query was {}, responses are {}",
                                message, responseMessages[0]);
                        break;
                    }
                }
            }
        }
        return responseMessages[0];
    }

    /**
     * This class is invoked when the Asynchronous gather request completes when called
     * by the synchronous gather below. Eventually the synchronous gather request may
     * be removed but currently it is needed for backward compatibility with how the
     * spine clients expect the gather mechanism to work (blocking, not asynchronous)
     */
    private class GatherCallbackImpl implements GatherCallback {

        /**
         * Called when the gather request gets the appropriate number of responses
         * @param messages
         */
        @Override
        public void result(Message[] messages) {
            synchronized (semaphore) {
                responseMessages[0] = messages;
                notified = true;
                semaphore.notifyAll();
            }
        }

        /**
         * Called when the gather request times out and does not get enough responses
         * @param warningType
         */
        @Override
        public void warning(com.sri.tasklearning.spine.messages.GatherIssues warningType) {
            synchronized (semaphore) {
                notified = true;
                semaphore.notifyAll();
            }
        }

    }

    public void onMessage(javax.jms.Message message) {
        // This message is sent from the REMOTE Spine instances to the LOCAL Spine
        // instance to register themselves and ask for permission to transmit
        // messages
        try {
            Serializable objectMessage =  ((ObjectMessage)message).getObject();
            if (Register.class.isInstance(objectMessage)) {
                Register registerMsg = (Register) objectMessage;

                String sender = registerMsg.getSender();
                if (spineInstanceList == null) {
                    spineInstanceList = new HashSet<String>();
                    // Add LOCAL id to the list to ensure no other Spine tries to use it.
                    log.trace("Adding first registered Spine with id: {}", clientId);
                    if (!sender.startsWith(SpineStatus.TEST_MASTER_STATUS)) {
                        spineInstanceList.add(clientId);
                    }
                }
                log.trace("Client '{}' Received Registration request from '{}'", clientId, sender);
                TransactionUID uid = registerMsg.getUid();
                boolean permission = true;
                try {
                    if (spineInstanceList.contains(sender)) {
                        log.info("Spine Client id: '{}' has already been used, declining registration.", sender);
                        spine.sendSystemMessage(new RegisterConfirmation(clientId, uid, !permission));
                    }
                    else {
                        spine.sendSystemMessage(new RegisterConfirmation(clientId, uid, permission));
                        log.trace("Adding client '{}' to the authorized Spine List", sender);
                        if (!sender.startsWith(SpineStatus.TEST_MASTER_STATUS)) {
                            spineInstanceList.add(sender);
                        }
                    }
                } catch (SpineException e) {
                    log.warn("Exception while sending RegisterConfirmation Message", e);
                }
            }
            else {
                log.warn("Attempt to subscribe to Unsupported Durable Message: '{}'", message);
            }
        } catch (JMSException e) {
            log.warn("Exception while handling Durable Message", e);
        }
    }

    /**
     * The method that is called when a message arrives on a topic that this class
     * has been subscribed as a listener to.
     * @param message The message that just arrived on a topic
     * @throws com.sri.tasklearning.spine.MessageHandlerException if something goes wrong
     */
    @Override
    public void handleMessage(Message message) throws MessageHandlerException {

        // This message is sent by the LOCAL Spine instance to the REMOTE Spine instances
        // when they ask to be registered. If they are the first instance with that id to
        // request registration they will be given permission to start transmitting.
        if (RegisterConfirmation.class.isInstance(message)) {
            RegisterConfirmation registerConf = (RegisterConfirmation) message;
            TransactionUID uid = registerConf.getUid();
            // Is this a response to my request?
            if (uid.getOriginator().equals(clientId)) {
                registrationPermissionMessageReceived = true;
                boolean permission = registerConf.getPermission();
                log.trace("'{}' Received Registration Confirmation, permission: '{}'", clientId, permission);
                if (permission) {
                    registered = true;
                    registrationSemaphore.signal();
                }
                else {
                    registered = false;
                    registrationSemaphore.signal();
                }
            }
        }

        // This message is sent by a Spine Client to request that the START_WATCHING
        // command be issued.
        else if (RequestStartWatching.class.isInstance(message)) {
            try {
                if (!watching) {
                    TransactionUID uid = ((RequestStartWatching)message).getUid();
                    spine.sendSystemMessage(new StartWatching(clientId, uid));
                    watching = true;
                    watchingSpines = new HashSet<String>();
                    watchingSpines.add(message.getSender());
                }
                else {
                    watchingSpines.add(message.getSender());
                    log.trace("Watching has already been initiated by another client");
                }
            } catch (Exception e) {
                log.warn("Exception while sending Start Watching Message", e);
            }
        }

        // This message is sent by a Spine Client to request that the STOP_WATCHING
        // command be issued.
        else if (RequestStopWatching.class.isInstance(message)) {
            try {
                if (watchingSpines != null && watchingSpines.remove(message.getSender())) {
                    if (watchingSpines.size() == 0) {
                        TransactionUID uid = ((RequestStopWatching)message).getUid();
                        spine.sendSystemMessage(new StopWatching(clientId, uid));
                        watching = false;
                    }
                }
            } catch (Exception e) {
                log.warn("Exception while sending Stop Watching Message", e);
            }
        }

        // This message is sent by a Spine Client to request a Serial Number. This
        // function is managed by the LOCAL Spine instance
        else if (SerialNumberRequest.class.isInstance(message)) {
            try {
                TransactionUID uid = ((SerialNumberRequest)message).getUid();
                spine.sendSystemMessage(new SerialNumberResponse(clientId, uid, serialNumber++));
            } catch (Exception e) {
                log.warn("Exception while sending SerialNumberResponse Message", e);
            }
        }

        // This message is sent from the Spines to the 'gather call issuing Spine' to allow
        // it to assemble the Types into one structure for return to the Spine Client
        else if (TypeListResult.class.isInstance(message)) {
            informGatheringThreads(message);

        }

        // This message is sent from the Spines to the 'gather call issuing Spine' to allow
        // it to assemble the Types into one structure for return to the Spine Client
        else if (TypeResult.class.isInstance(message)) {
            informGatheringThreads(message);
        }

        /*
         * This message is sent from the Spines to the 'gather call issuing
         * Spine' to allow it to assemble the results into one structure for
         * return to the Spine client.
         */
        else if (ExecutorListResult.class.isInstance(message)) {
            informGatheringThreads(message);
        }

        // This message is sent to the Spine instances from a REMOTE Spine instance when
        // it is about to shutdown. If that Spine instance had previously requested
        // START_WATCHING it will be treated as a STOP_WATCHING request.
        else if (JmsSpineClosing.class.isInstance(message) ) {
            // The Local instance needs to do extra work with this message
            if (clientType.equals(JmsClient.LOCAL)) {
                String closingSpine = message.getSender();
                log.trace("Spine closing received from: {}", closingSpine);
                spineInstanceList.remove(closingSpine);
                if (watchingSpines != null && watchingSpines.remove(message.getSender())) {
                    if (watchingSpines.size() == 0) {
                        TransactionUID uid =  ((JmsSpineClosing)message).getUid();
                        try {
                            spine.sendSystemMessage(new StopWatching(clientId, uid));
                        } catch (Exception e) {
                            log.warn("Exception while sending Stop Watching Message", e);
                        }
                        watching = false;
                    }
                }
            }
        }

        else {
            log.error("Unrecognized Internal message: {}", message);
        }
    }

    /**
     * When a 'gatherable' response message arrives the message handler passes the message
     * to this method which alerts each of the gathering threads.
     * @param message The message received
     */
    private void informGatheringThreads(Message message) {
        synchronized (gatherLock) {
            // For each of the gathering threads, show them the message and let them decide if they
            // want it. The signal method will return true if that is the last message it was waiting
            // on so remove it from the list of active gather threads.
            for (int i = 0; i < gatheringThreads.size(); i++) {
                if (gatheringThreads.get(i).signal(message)) {
                    gatheringThreads.remove(i);
                }
            }
        }
    }

    /**
     * Get the number of spines active in the system
     * @return the number of active spines
     */
    public int getSpineInstanceCount() {
        if (spineInstanceList == null) {
            return 0;
        }
        return spineInstanceList.size();
    }
}
