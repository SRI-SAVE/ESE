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

import java.util.HashMap;
import java.util.Map;

import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.IgnoredExecutionStatus;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.RequestIgnored;
import com.sri.tasklearning.spine.messages.StartExecutionStatus;
import com.sri.tasklearning.spine.messages.SuccessExecutionStatus;
import com.sri.tasklearning.spine.messages.contents.UID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used by the Master Spine to detect when an execution request
 * is being ignored by all of the potential executors. In this case, an execution
 * ignored status message is delivered to the client.
 */
public class ExecutionAcceptanceWatcher implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ExecutionAcceptanceWatcher.class);
    volatile private Map<UID, Integer> watchedExecutionMap;
    private JmsSpine jmsSpine;

    public ExecutionAcceptanceWatcher(JmsSpine jmsSpine) {
        log.debug("Starting Execution Acceptance Watcher for spine: {}", jmsSpine.getClientId());
        this.jmsSpine = jmsSpine;
        watchedExecutionMap = new HashMap<UID, Integer>();
    }

    /**
     * Adds an execution request to be monitored for acceptance
     * @param executeRequest the execution request
     * @throws SpineException if something goes wrong
     */
    synchronized public void addWatch(ExecuteRequest executeRequest) throws SpineException {
        int expectedResponses = jmsSpine.calculateExpectedResponderCount(executeRequest.getMessageType());
        log.debug("Waiting for '{}' responses to execute request '{}'", expectedResponses, executeRequest.getUid());
        watchedExecutionMap.put(executeRequest.getUid(), expectedResponses);
    }

    /**
     * How many responses are expected for this execution request
     * @return the number of expected responses
     */
    synchronized public int watchCount() {
        return watchedExecutionMap.size();
    }

    /**
     * Handles messages that are pertinent to the execution request and reacts
     * appropriately
     * @param message Message veing sent through spine
     * @throws MessageHandlerException if something goes wrong
     */
    @Override
    synchronized public void handleMessage(Message message) throws MessageHandlerException {
        // New execution request, keep track of it
        if (message instanceof ExecuteRequest) {
            log.debug("ExecutionRequest Issued, starting watch: '{}'", message);
            try {
                addWatch((ExecuteRequest) message);
            } catch (SpineException e) {
                log.warn("Exception when trying to monitor execution request", e);
                throw new MessageHandlerException(e);
            }
        }
        // Execution request was handled by one of the executors, we can stop watching now.
        else if (message instanceof StartExecutionStatus || message instanceof SuccessExecutionStatus ||
            message instanceof ErrorExecutionStatus) {
            UID uid = message.getUid();
            watchedExecutionMap.remove(uid);
        }
        // One of the executors ignored the request, if all of them did we have a problem
        else if (message instanceof RequestIgnored) {
            log.debug("Request {} ignored by {}", message.getUid(), message.getSender());
            RequestIgnored ignoredMessage = (RequestIgnored) message;
            UID uid = ignoredMessage.getUid();
            if (watchedExecutionMap.containsKey(uid)) {
                int count = watchedExecutionMap.get(uid);
                count--;
                if (count < 1) {
                    log.info("Request {} was ignored by all executors, reporting back to requester.", uid);
                    watchedExecutionMap.remove(uid);
                    try {
                        jmsSpine.send(new IgnoredExecutionStatus(ignoredMessage.getSender(), ignoredMessage.getUid(),
                                      ignoredMessage.getParentUid()));
                    } catch (SpineException e) {
                        log.warn("Unable to send RequestIgnoredMessage", e);
                        throw new MessageHandlerException(e);
                    }
                }
                else {
                    // Decrement the count of potential responsers, when this runs out we know
                    // noone is executing the procedure.
                    watchedExecutionMap.put(uid, count);
                }
            }
        }

    }


}
