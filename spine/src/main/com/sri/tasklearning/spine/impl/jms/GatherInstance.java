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

import java.util.ArrayList;

import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.contents.UID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An instance that performs a gather request on behalf of the Spine Client. It runs
 * in a thread of its own to allow multiple simultaneous gathers and also to free up
 * the Spine Client to do other things.
 */
public class GatherInstance implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(GatherInstance.class);
    private static final long DEFAULT_PATIENCE = 60000; // One minute
    private final Object semaphore = new Object();
    private boolean notified = false;
    private long patience;
    private UID gatherIdentifier;
    private GatherCallback callback;
    private ArrayList<Message> accumulatedResults;
    private int expectedResponses;

    /**
     * Constructs a new gather instance with the default time out.
     * @param expectedResponses The number of responses that should be received
     * @param gatherIdentifier The UID that uniquely identifies responses this instance requires
     * @param callback The class that is invoked when the results have been gathered or an error occurs
     */
    public GatherInstance(int expectedResponses, UID gatherIdentifier, GatherCallback callback) {
        this.expectedResponses = expectedResponses;
        this.gatherIdentifier = gatherIdentifier;
        this.callback = callback;
        this.patience = DEFAULT_PATIENCE;
        accumulatedResults = new ArrayList<Message>();
    }

    /**
     * Constructs a new gather instance with a specific time out.
     * @param expectedResponses The number of responses that should be received
     * @param patience The timeout to be used when waiting for responses
     * @param gatherIdentifier The UID that uniquely identifies responses this instance requires
     * @param callback The class that is invoked when the results have been gathered or an error occurs
     */
    public GatherInstance(int expectedResponses, long patience, UID gatherIdentifier, GatherCallback callback) {
        this.expectedResponses = expectedResponses;
        this.gatherIdentifier = gatherIdentifier;
        this.callback = callback;
        this.patience = patience;
        accumulatedResults = new ArrayList<Message>();
    }

    /**
     * Called by the StateManager when a 'gatherable' message arrives. The message will be
     * checked to see if it is intended for this gather instance or not.
     * @param message The received message
     * @return true if this is that last response being waited on
     */
    public boolean signal(Message message) {
        synchronized (semaphore) {
            if (message.getUid().equals(gatherIdentifier)) {
                accumulatedResults.add(message);
                if (accumulatedResults.size() >= expectedResponses) {
                    notified = true;
                    semaphore.notify();
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Start the timer and wait to be notified when the responses have arrived.
     */
    public void run() {
        synchronized (semaphore) {
            // Keep track of how long the wait was
            long start = System.currentTimeMillis();
            long currentTime = System.currentTimeMillis();
            // This 'while' is necessary to prevent phantom notify() calls to the Object.wait()
            // check out the javadoc for that method for more details.
            while (((currentTime - start) < patience) && !notified) {
                try {
                    semaphore.wait(patience);
                } catch (InterruptedException e) {
                    log.warn("Gather wait thread was interrupted by an exception");
                }
                currentTime = System.currentTimeMillis();
            }
            log.trace("GatherInstance wait complete, waited for: " + (currentTime - start) +" ms");
            callback.result(accumulatedResults.toArray(new Message[accumulatedResults.size()]));
        }
        if (!notified) {
            log.warn("The GatherInstance {} did not receive the expected responses.", gatherIdentifier);
        }
    }


}
