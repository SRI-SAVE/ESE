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

// $Id: DebugExecutor.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.actionmodels;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionExecutor;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionInvocation.StepCommand;
import com.sri.pal.ActionInvocationStatusListener;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.PALException;
import com.sri.pal.common.ErrorInfo;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.JmsClient;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ActionExecutor for the debug.xml action model.
 *
 * @author chris
 */
public class DebugExecutor
        implements ActionExecutor {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    private boolean asynchronous = true;
    private int numActions = 0;
    private final Map<Integer, ActionInvocation> actions;
    private final Map<Integer, Long> startTimes;
    private final Map<Integer, Long> endTimes;
    private final Set<ActionStreamEvent> canceled;
    private JmsSpine spine;

    public DebugExecutor() throws SpineException {
        actions = new HashMap<Integer, ActionInvocation>();
        startTimes = new HashMap<Integer, Long>();
        endTimes = new HashMap<Integer, Long>();
        canceled = new CopyOnWriteArraySet<ActionStreamEvent>();
        spine = new JmsSpine(JmsClient.REMOTE, "debugexecutor");
        spine.subscribe(new DebugMessageHandler(), UserMessageType.CANCEL);
    }

    public void setAsynchronous(boolean flag) {
        asynchronous = flag;
    }

    @Override
    public void cancel(ActionStreamEvent invocation) {
        canceled.add(invocation);
        UncancelWatcher watcher = new UncancelWatcher(invocation);
        invocation.addListener(watcher);
        synchronized (this) {
            notifyAll();
        }
        /*
         * TODO This method shouldn't return until all called actions have been
         * marked FAILED.
         */
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore.
        }
    }

    /**
     * Consider canceling an invocation. If it's (directly or indirectly) called
     * by an invocation known to be canceled, then we mark it failed.
     *
     * @param invocation
     *            the invocation to consider canceling
     * @return {@code true} if the invocation was canceled
     */
    private boolean maybeCancel(ActionInvocation invocation) {
        for (ActionStreamEvent caller = invocation; caller != null; caller = caller
                .getCaller()) {
            if(canceled.contains(caller)) {
                log.info("Canceling {} called by {}", invocation, caller);
                ErrorInfo err = ErrorFactory.error(getClass().getSimpleName(),
                        ErrorType.CANCEL, invocation.getDefinition().getName()
                                .getFullName());
                invocation.error(err);
                return true;
            }
        }
        return false;
    }

    @Override
    public void execute(final ActionInvocation invocation)
            throws PALException {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    innerExecute(invocation);
                } catch (Exception e) {
                    log.warn("Unable to execute " + invocation, e);
                    ErrorInfo error = new ErrorInfo("test", 1, e.toString(),
                            e.toString(), null);
                    invocation.error(error);
                }
            }
        };
        t.setDaemon(true);
        if (asynchronous) {
            t.start();
        } else {
            t.run();
        }
    }

    private void innerExecute(ActionInvocation invocation)
            throws PALException {
        log.info("Starting {}", invocation);
        invocation.setStatus(Status.RUNNING);
        Status status = Status.ENDED;
        int num = numActions++;
        actions.put(num, invocation);
        startTimes.put(num, System.currentTimeMillis());
        ActionDef actDef = invocation.getDefinition();
        String name = actDef.getName().getSimpleName();
        if(maybeCancel(invocation)) {
            // Do nothing; maybeCancel has a side effect.
        } else if (name.equals("log")) {
            Object arg = invocation.getValue(0);
            log.info("Log action: {}", arg);
        } else if (name.equals("sleep")) {
            Object arg = invocation.getValue(0);
            int duration = (Integer) arg;
            try {
                long startSleep = System.currentTimeMillis();
                long endSleep = startSleep + duration;
                for (long now = startSleep; now < endSleep; now = System
                        .currentTimeMillis()) {
                    synchronized (this) {
                        this.wait(endSleep - now);
                    }
                    if (maybeCancel(invocation)) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            }
        } else if (name.equals("concat")) {
            Object arg0 = invocation.getValue(0);
            Object arg1 = invocation.getValue(1);
            String result = arg0.toString() + arg1.toString();
            invocation.setValue(2, result);
        } else if (name.equals("makeString")) {
            String result = "foo";
            invocation.setValue(0, result);
        } else if (name.equals("fail")) {
            Object argObj = invocation.getValue(0);
            if ("succeed".equals(argObj)) {
                log.debug("Succeeding");
            } else if ("fail".equals(argObj)) {
                log.debug("Failing");
                status = Status.FAILED;
            } else if ("error".equals(argObj)) {
                log.debug("Sending an error");
                invocation.error(new ErrorInfo("test", 1, "err", "error", null));
                status = Status.FAILED;
            } else {
                log.warn("Unexpected arg {} for fail action", argObj);
            }
        } else {
            throw new PALException("Unknown action type " + actDef);
        }
        endTimes.put(num, System.currentTimeMillis());

        // If it's been canceled, don't say it's finished now.
        if (invocation.getStatus() == Status.RUNNING) {
            invocation.setStatus(status);
        }
    }

    @Override
    public void executeStepped(ActionInvocation invocation)
            throws PALException {
        execute(invocation);
    }

    @Override
    public void continueStepping(ActionInvocation invocation,
                                 StepCommand command,
                                 List<Object> actionArgs)
            throws PALException {
        throw new PALException("Not implemented.");
    }

    public int numExecuted() {
        return numActions;
    }

    public ActionInvocation getExecuted(int i) {
        return actions.get(i);
    }

    public long getStart(int i) {
        return startTimes.get(i);
    }

    public long getEnd(int i) {
        return endTimes.get(i);
    }

    /**
     * Wait until the event completes, and remove it from the set of those we've
     * marked as canceled.
     */
    private class UncancelWatcher
            implements ActionInvocationStatusListener {
        private final ActionStreamEvent event;

        public UncancelWatcher(ActionStreamEvent invocation) {
            event = invocation;
        }

        @Override
        public void error(ErrorInfo error) {
            // Do nothing.
        }

        @Override
        public void newStatus(Status newStatus) {
            if (newStatus == Status.ENDED || newStatus == Status.FAILED) {
                canceled.remove(event);
            }
        }
    }

    private class DebugMessageHandler implements MessageHandler {

        @Override
        public void handleMessage(Message message) throws MessageHandlerException {
        }
    }
}
