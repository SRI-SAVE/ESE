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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a helper method for the REMOTE spines that allow the shutdown method
 * to be called from the Message Receiver thread. The shutdown is 'loud' to communicate
 * the fact that the REMOTE spine is going down to the LOCAL/MASTER spine.
 */
class ShutdownSlave implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ShutdownSlave.class);
    private JmsSpine spine;

    public ShutdownSlave(JmsSpine spine) {
        this.spine = spine;
    }

    @Override
    public void run() {
        try {
            // Shut down loudly so that the MASTER spine knows this spine is down.
            spine.shutdown(true);
        } catch (Exception e) {
            log.warn("Unable to shutdown " + spine.getClientId() + " properly:", e);
        }
    }
}
