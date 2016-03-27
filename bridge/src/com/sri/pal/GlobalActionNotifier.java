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

// $Id: GlobalActionNotifier.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for sending events to the GlobalActionListeners.
 *
 * @author chris
 */
class GlobalActionNotifier
        implements Runnable {
    private static final Logger log = LoggerFactory
            .getLogger(GlobalActionNotifier.class);

    private final List<ActionStreamEvent> queue;
    private final Set<GlobalActionListener> listeners;

    GlobalActionNotifier() {
        queue = new LinkedList<ActionStreamEvent>();
        listeners = new CopyOnWriteArraySet<GlobalActionListener>();
    }

    public void newInvocation(ActionStreamEvent invocation) {
        log.debug("Adding invocation of {}", invocation.getDefinition());
        synchronized (this) {
            queue.add(invocation);
            notifyAll();
        }
    }

    @Override
    public void run() {
        try {
            innerRun();
        } catch (Exception t) {
            log.error("Global notifier died", t);
        }
    }

    private void innerRun() {
        while (true) {
            ActionStreamEvent invocation;
            synchronized (this) {
                while (queue.isEmpty()) {
                    try {
                        log.debug("Sleeping");
                        wait();
                    } catch (InterruptedException e) {
                        log.warn("Interrupted", e);
                    }
                    log.debug("Woke up");
                }
                invocation = queue.remove(0);
            }
            log.debug("Notifying for {}", invocation.getDefinition());
            for (GlobalActionListener listener : listeners) {
                try {
                    listener.actionStarted(invocation);
                } catch (Exception e) {
                    log.info("GlobalActionListener " + listener
                            + " threw exception", e);
                }
            }
            invocation.finishedStartNotify();
        }
    }

    public void addListener(GlobalActionListener listener) {
        listeners.add(listener);
    }
}
