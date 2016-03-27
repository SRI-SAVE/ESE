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

// $Id: CancelReceiver.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;

import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.messages.CancelRequest;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives cancel messages and sends them on to {@link ActionExecutor}s.
 */
class CancelReceiver
        implements MessageHandler {
    private static final Logger log = LoggerFactory
            .getLogger(CancelReceiver.class);

    /**
     * When we receive a cancel request, we try to retrieve the event in
     * question so we can cancel it. How long to we wait for the event to
     * appear? It may not have arrived yet, or it may have already died and
     * disappear.
     */
    private static final long TIMEOUT = 10 * 1000;

    private final Bridge bridge;
    private final Executor threadPool;

    CancelReceiver(Bridge bridge) {
        this.bridge = bridge;
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newCachedThreadPool(tf);
    }

    private InvocationCache getInvocCache() {
        return bridge.getInvocationCache();
    }

    private ActionModel getActionModel() {
        return bridge.getActionModel();
    }

    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (!(message instanceof CancelRequest)) {
            log.warn("Unexpected message ({}): {}", message.getClass(), message);
            return;
        }
        CancelRequest cancelMsg = (CancelRequest) message;

        CancelTask task = new CancelTask(cancelMsg);
        threadPool.execute(task);
    }

    private class CancelTask
            implements Runnable {
        private final CancelRequest cancelMsg;

        CancelTask(CancelRequest req) {
            cancelMsg = req;
        }

        @Override
        public void run() {
            TransactionUID uid = cancelMsg.getUid();
            ActionStreamEvent event;
            try {
                event = getInvocCache().get(uid, TIMEOUT);
            } catch (TimeoutException e) {
                log.warn(
                        "Couldn't get event {} after {}ms; it's probably dead and gone",
                        uid, TIMEOUT);
                return;
            }
            for (ActionExecutor executor : getActionModel().getExecutors()) {
                executor.cancel(event);
            }
        }
    }
}
