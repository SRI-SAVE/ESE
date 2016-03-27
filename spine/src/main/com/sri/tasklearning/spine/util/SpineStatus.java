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
package com.sri.tasklearning.spine.util;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.JmsClient;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;
import com.sri.tasklearning.spine.messages.Heartbeat;
import com.sri.tasklearning.spine.messages.JmsSpineClosing;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SystemMessageType;

/**
 * This class replaces the logic that used to be in JmsSpine.isMasterRunning. Previously,
 * a new Test Spine was created each time the Master Spines status was to be checked.
 * Although the state was cached for 1 second before tearing down and starting up a new
 * Test Spine, this proved to be too burdensome for the Garbage Collector to keep up with.
 *
 * This class creates one instance of the Test Spine and then listens for Heartbeat
 * messages coming from the Master Spine to update status continually.
 */
public class SpineStatus {

    private static final Logger log = LoggerFactory.getLogger(SpineStatus.class);
    public static final String TEST_MASTER_STATUS = "testMasterStatus";
    private Spine spine;
    private boolean running = false;
    private CallbackHandler callbackHandler;
    private long lastHeartbeat;
    private final Object semaphore = new Object();
    private boolean notified = false;
    private long patience = 600000;
    private boolean everRunning = false;
    private boolean everHeartbeat = false;

    public SpineStatus() {
        connectSpine();
    }

    /**
     * This method attempts to start a new Test Spine to first test if the Master Spine is up
     * and then to intercept heartbeat messages that indicate the Master Spine remains up.
     */
    private void connectSpine() {
        UUID uuid = UUID.randomUUID();
        try {
            if (!everRunning) {
                spine = new JmsSpine(JmsClient.TEST, TEST_MASTER_STATUS
                        + uuid.toString());
            } else {
                spine = new JmsSpine(JmsClient.DURABLE_TEST, TEST_MASTER_STATUS
                        + uuid.toString());
            }
            log.trace("Initial check shows PAL is running");
            running = true;
            // Show that an initial connection succeeded
            everRunning = true;
        } catch (Exception e) {
            log.info("Initial check shows PAL is not running");
            running = false;
        }

        if (running) {
            callbackHandler = new CallbackHandler();
            try {
                //Subscribe to the heartbeat for continuing status and the SpineClosing for end of life event
                spine.subscribe(callbackHandler, SystemMessageType.HEARTBEAT, SystemMessageType.SPINE_CLOSING);
            } catch (SpineException e) {
                log.warn("Failed to subscribe PING Spine to Master Spine", e);
                running = false;
            }
        }
    }

    /**
     * This method uses various strategies to determine if the Master Spine is up an running
     * @return up status of Master Spine
     */
    public boolean isMasterRunning() {
        // If no connection was ever successful, try again
        if (!everRunning) {
            connectSpine();
        }
        // Previous attempts to connect or waiting for heartbeat failed, report the Master Spine is down
        if (!running) {
            return running;
        }
        long now = System.currentTimeMillis();
        long lengthOfSilence = now - lastHeartbeat;
        synchronized (semaphore) {
            // If the status is up, and we received at least 1 heartbeat, but not for at least 10 seconds
            // the Master Spine may be very busy - lets give it some time to resolve.
            if (running && everHeartbeat && lengthOfSilence > 10000) {
                log.info("No heartbeat for 10 seconds, Master Spine may be down. Waiting up to 10 minutes for delayed heartbeat");
                long start = System.currentTimeMillis();
                long currentTime = System.currentTimeMillis();
                // This 'while' is necessary to prevent phantom notify() calls to the Object.wait()
                // check out the javadoc for that method for more details.
                while (((currentTime - start) < patience) && !notified) {
                    try {
                        semaphore.wait(patience);
                    } catch (InterruptedException e) {
                        log.warn("Unable to wait for heartbeat due to synchronization failure");
                    }
                    currentTime = System.currentTimeMillis();
                }
                // Notified will be true if a heartbeat arrived
                if (!notified) {
                    running = false;
                    log.info("No heartbeat for {} seconds", lengthOfSilence / 1000 + "");
                }
            }
        }
        return running;
    }

    /**
     * This method is called by the Message Handler when a Heartbeat or Spine Closing message
     * is received. If the thread was waiting for a heartbeat message it is notified to wake up.
     * @param heartbeat did we receive a heartbeat message?
     */
    private void signal(boolean heartbeat) {
        synchronized (semaphore) {
            // Out of paranoia, set the boolean before notifying the semaphore
            notified = heartbeat;
            semaphore.notify();
        }
    }

    /**
     * ner class that handles messages sent from the Master Spine.
     */
    private class CallbackHandler implements MessageHandler {

        public void handleMessage(Message message) throws MessageHandlerException {
            boolean heartbeatReceived = true;
            // A heart beat is expectedevery 1 second when the system is not being taxed
            if (Heartbeat.class.isInstance(message)) {
                log.trace("Heartbeat received in {}", spine.getClientId());
                lastHeartbeat = System.currentTimeMillis();
                running = true;
                // Shows we received at least 1 heartbeat
                everHeartbeat = true;
                // Wake up sleeping thread and indicate a heartbeat was received
                signal(heartbeatReceived);
            } else if (JmsSpineClosing.class.isInstance(message)) {
                JmsClient spineType = ((JmsSpineClosing)message).getSpineType();
                // Check if it is the Master Spine that is shutting down
                if (spineType.equals(JmsClient.LOCAL)) {
                    log.debug("Master/Local SpineClosing received in {}", spine.getClientId());
                    running = false;
                    // Wake up sleeping thread and indicate a heartbeat was not received
                    signal(!heartbeatReceived);
                }
				else if (spineType.equals(JmsClient.REMOTE)) {
                    log.debug("Remote SpineClosing received in {}", spine.getClientId());
					try {
						spine.shutdown(true);
				    } 
					catch(Exception e) {
						log.debug("Problem shutting down status spine {}", e);
					}
				}
            }
        }

    }
}
