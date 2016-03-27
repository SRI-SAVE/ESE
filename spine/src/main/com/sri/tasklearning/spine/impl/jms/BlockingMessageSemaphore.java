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

import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.messages.DurableMessageType;
import com.sri.tasklearning.spine.SpineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to coordinate messages that require blocking. For example,
 * the requesting of existing subscriptions from the LOCAL JmsSpine and handling
 * the resulting incoming response. Control is not handed back to the constructing
 * client to prevent the client from sending messages on topics that have not yet
 * been processed as known to have a subscriber on them.
 */
public class BlockingMessageSemaphore {

    private static final Logger log = LoggerFactory.getLogger(BlockingMessageSemaphore.class);
    private static final long DEFAULT_PATIENCE = 60000; // One minute
    private final Object semaphore = new Object();
    private boolean notified = false;
    private long patience;
    private JmsSpine jmsSpine;

    /**
     * Creates a BlockingMessageSemaphore that attempts to deliver a message.
     * If the required delivery and release mechanism (semphore signal) is
     * not triggered inside of the default delay, the wait will release and
     * the situation will be handled as deemed appropriate
     * @param jmsSpine the Spine requesting the blocking behavior
     */
    public BlockingMessageSemaphore(JmsSpine jmsSpine) {
        this.patience = DEFAULT_PATIENCE;
        this.jmsSpine = jmsSpine;
    }

    /**
     * Creates a BlockingMessageSemaphore that attempts to deliver a message.
     * If the required delivery and release mechanism (semphore signal) is
     * not triggered inside of the specified delay, the wait will release and
     * the situation will be handled as deemed appropriate
     * @param patience how long to wait between attempts
     * @param jmsSpine the Spine requesting the blocking behavior
     */
    public BlockingMessageSemaphore(long patience, JmsSpine jmsSpine) {
        this.patience = patience;
        this.jmsSpine = jmsSpine;
    }

    /**
     * Returns control to the JmsSpine which will return to the client from the
     * constructor call
     */
    public void signal() {
        synchronized (semaphore) {
            // Out of paranoia, set the boolean before notifying the semaphore
            notified = true;
            semaphore.notify();
        }
    }

    /**
     * This method locks the semaphore and then submits the request provided
     * @param message the instance of the message that needs to be sent
     * @throws InterruptedException if the lock is interrupted
     * @throws com.sri.tasklearning.spine.SpineException if the send fails
     */
    public void await(Message message) throws SpineException, InterruptedException {
        synchronized (semaphore) {
            // Deliver the message
            sendMessage(message);
            // Keep track of how long the wait was
            long start = System.currentTimeMillis();
            long currentTime = System.currentTimeMillis();
            // This 'while' is necessary to prevent phantom notify() calls to the Object.wait()
            // check out the javadoc for that method for more details.
            while (((currentTime - start) < patience) && !notified) {
                semaphore.wait(patience);
                currentTime = System.currentTimeMillis();
            }
            log.trace("BlockingMessageSemaphore wait complete, waited for: " + (currentTime - start) +" ms");
        }
        if (!notified) {
            log.warn("The '{}' JmsSpine did not receive the expected call(s) to unblock " +
                    "semaphore for message '{}'.", jmsSpine.getClientId(), message);
        }
    }

    /**
     * Handles the logic of deciding what type of message is being sent and forwarding
     * it to the correct send method in the Spine
     * @param message the message to be sent
     * @throws SpineException if there is a problem sending the message
     */
    private void sendMessage(Message message) throws SpineException {
        if (SystemMessageType.class.isInstance(message.getMessageType()) ||
                DurableMessageType.class.isInstance(message.getMessageType())) {
            jmsSpine.sendSystemMessage(message);
        }
        else {
            jmsSpine.send(message);
        }
    }
}
