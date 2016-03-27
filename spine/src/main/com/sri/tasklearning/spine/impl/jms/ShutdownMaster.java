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

import com.sri.tasklearning.spine.messages.JmsSpineClosing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a helper thread that sends a shutdown notice to the REMOTE spines when
 * the LOCAL/MASTER spine has been asked to shut down. It then waits to see if the
 * REMOTE spines close gracefully. If they do, the shutdown method is called for
 * this spine. If they do not, a warning is printed and we shutdown anyway.
 */
class ShutdownMaster implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ShutdownMaster.class);
    private JmsSpine spine;

    public ShutdownMaster(JmsSpine spine) {
        this.spine= spine;
    }

    @Override
    public void run() {
        try {
            spine.sendSystemMessage(new JmsSpineClosing(spine.getClientId(), spine.getClientType(), spine.getNextUid()));
            int patience = 100;
            int duration = 0;
            int spineInstanceCount = spine.getSpineInstanceCount();
            while (spineInstanceCount > 1 && duration < patience) {
                Thread.sleep(100);
                duration += 1;
                spineInstanceCount = spine.getSpineInstanceCount();
            }
            if (spineInstanceCount > 1) {
                log.warn("The REMOTE spines did not close in a timely manner, MASTER shutting down anyway.");
            }
            spine.shutdown(false);
        } catch (Exception e) {
            log.warn("Unable to shutdown " + spine.getClientId() + " properly:", e);
        }
    }
}
