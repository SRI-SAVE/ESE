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

// $Id: PALInstrumentationControl.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.RequestStartWatching;
import com.sri.tasklearning.spine.messages.RequestStopWatching;
import com.sri.tasklearning.spine.messages.StartWatching;
import com.sri.tasklearning.spine.messages.StopWatching;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for an application to change the current state of event
 * reporting. This is relevant to cross-application learning, as described in
 * {@link InstrumentationControl}.
 */
public class PALInstrumentationControl
        implements InstrumentationControl, MessageHandler {
    private static final Logger log = LoggerFactory
            .getLogger(PALInstrumentationControl.class);

    private final Set<InstrumentationControl> instrSet;
    private final Spine spine;
    private final Executor threadPool;

    PALInstrumentationControl(Spine spine) {
        this.spine = spine;
        instrSet = new CopyOnWriteArraySet<InstrumentationControl>();
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newFixedThreadPool(1, tf);
    }

    @Override
    public void startWatching() {
        TransactionUID uid = spine.getNextUid();
        RequestStartWatching request = new RequestStartWatching(
                spine.getClientId(), uid);
        try {
            spine.send(request);
        } catch (SpineException e) {
            log.warn("Unable to send message " + request, e);
        }
    }

    @Override
    public void stopWatching() {
        TransactionUID uid = spine.getNextUid();
        RequestStopWatching request = new RequestStopWatching(
                spine.getClientId(), uid);
        try {
            spine.send(request);
        } catch (SpineException e) {
            log.warn("Unable to send message " + request, e);
        }
    }

    void addApplicationInstrumentation(InstrumentationControl instr) {
        instrSet.add(instr);
    }

    void removeApplicationInstrumentation(InstrumentationControl instr) {
        instrSet.remove(instr);
    }

    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (!(message instanceof StopWatching)
                && !(message instanceof StartWatching)) {
            log.warn("Unexpected message type {}: {}", message.getClass(),
                    message);
            return;
        }

        Runnable task;
        if (message instanceof StartWatching) {
            task = new StartTask();
        } else {
            task = new StopTask();
        }
        threadPool.execute(task);
    }

    private class StartTask
            implements Runnable {
        @Override
        public void run() {
            for (InstrumentationControl instr : instrSet) {
                instr.startWatching();
            }
        }
    }

    private class StopTask
            implements Runnable {
        @Override
        public void run() {
            for (InstrumentationControl instr : instrSet) {
                instr.stopWatching();
            }
        }
    }
}
