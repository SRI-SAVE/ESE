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

// $Id: ITLExecutionStatusListener.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;

import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.BreakpointNotify;
import com.sri.tasklearning.spine.messages.BreakpointResponse;
import com.sri.tasklearning.spine.messages.BreakpointResponse.Command;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.ExecutionStatus;
import com.sri.tasklearning.spine.messages.IgnoredExecutionStatus;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.StartExecutionStatus;
import com.sri.tasklearning.spine.messages.SuccessExecutionStatus;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives notification from the Spine of actions which start and stop. Sends
 * those notifications on to PAL components. Contrast with
 * {@link ActionExecAdapter}, which receives requests to execute actions.
 *
 * @author chris
 */
class ITLExecutionStatusListener
        implements MessageHandler {
    private static final Logger log = LoggerFactory
            .getLogger(ITLExecutionStatusListener.class);

    /**
     * How long do we wait to retrieve an invocation before giving up?
     */
    // TODO This is duplicated in ActionExecAdapter.
    private static final long TIMEOUT = 10 * 1000;

    private final Bridge bridge;
    private final String mySenderId;
    private final Executor threadPool;
    private GestureStart lastGestureStart = null;

    ITLExecutionStatusListener(Bridge bridge) {
        this.bridge = bridge;
        mySenderId = bridge.getSpine().getClientId();
        log.debug("My ID: {}", mySenderId);
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newFixedThreadPool(1, tf);
    }

    private InvocationCache getInvocCache() {
        return bridge.getInvocationCache();
    }

    @Override
    public void handleMessage(Message message) {
        if (!(message instanceof ExecutionStatus)) {
            log.warn("Unexpected message ({}) ignored: {}", message.getClass(),
                    message);
            return;
        }
        ExecutionStatus execMsg = (ExecutionStatus) message;

        // Ignore messages from self:
        if (mySenderId.equals(execMsg.getSender())) {
            log.debug("Discarding message from self: {}", execMsg);
            return;
        } else {
            log.debug("Received {}", execMsg);
        }

        // This is used to coordinate with the ActionExecAdapter. While the UpdateThread is
        // running and adding the invocation of this uid, the ActionExecAdapter may receive
        // an execution request for a new subtask with this uid set as its parent. The watcher
        // in the InvocationCache will make the ActionExecAdapter wait until the invocation
        // for this uid is added before continuing.
        if (execMsg instanceof StartExecutionStatus) {
            log.debug("Telling the cache to watch for uid: {}", execMsg.getUid());
            getInvocCache().watchFor(execMsg.getUid());
        }

        threadPool.execute(new UpdateThread(execMsg));
    }

    private class UpdateThread
            implements Runnable {
        private final ExecutionStatus execMsg;
        private final TransactionUID uid;
        private final TransactionUID parentUid;

        public UpdateThread(ExecutionStatus execMsg) {
            this.execMsg = execMsg;
            uid = execMsg.getUid();
            if (uid == null) {
                log.error("Null UID received for {}", execMsg);
            }
            parentUid = execMsg.getParentUid();
            log.debug("The UID is: {}, the parentUid is: {}", uid, parentUid);
        }

        @Override
        public void run() {
            if (execMsg instanceof StartExecutionStatus) {
                StartExecutionStatus startMsg = (StartExecutionStatus) execMsg;
                handleStart(startMsg);
            } else if (execMsg instanceof SuccessExecutionStatus) {
                SuccessExecutionStatus successMsg = (SuccessExecutionStatus) execMsg;
                try {
                    handleSuccess(successMsg);
                } finally {
                    log.debug("Telling InvocationCache to stop watching for uid: {}", uid);
                    getInvocCache().endWatch(uid);
                }
            } else if (execMsg instanceof ErrorExecutionStatus) {
                ErrorExecutionStatus errorMsg = (ErrorExecutionStatus) execMsg;
                try {
                    handleError(errorMsg);
                } finally {
                    log.debug("Telling InvocationCache to stop watching for uid: {}", uid);
                    getInvocCache().endWatch(uid);
                }
            } else if (execMsg instanceof IgnoredExecutionStatus) {
                IgnoredExecutionStatus ignoreMsg = (IgnoredExecutionStatus) execMsg;
                try {
                    handleIgnored(ignoreMsg);
                } finally {
                    log.debug("Telling InvocationCache to stop watching for uid: {}", uid);
                    getInvocCache().endWatch(uid);
                }
            } else if (execMsg instanceof BreakpointNotify) {
                BreakpointNotify stepMsg = (BreakpointNotify) execMsg;
                handleBreakpoint(stepMsg);
            } else {
                log.warn("Unknown execution status message: {}", execMsg);
            }
        }

        private void handleStart(StartExecutionStatus startMsg) {
            ActionStreamEvent invocation;
            try {
                invocation = getInvocCache().get(uid, 0);
            } catch (TimeoutException e) {
                invocation = null;
            }
            if (invocation == null) {
                // This should only happen when the execution was initiated
                // by another Spine client. Create a local ActionInvocation
                // to represent it.
                ActionStreamEvent parent;
                if (parentUid == null) {
                    parent = null;
                } else {
                    try {
                        parent = getInvocCache().get(parentUid, TIMEOUT);
                    } catch (TimeoutException e) {
                        log.warn("Never saw parent UID {} for child UID {}"
                                + " (received {})", new Object[] {
                                parentUid, uid, startMsg });
                        parent = null;
                    }
                }
                long serial = startMsg.getSerialNumber();
                SimpleTypeName taskName = startMsg.getActionName();
                if (taskName == null) {
                    /* This is a non-idiom gesture. */
                    invocation = new GestureStart(null, bridge, parent, null,
                            null, serial, uid);
                    lastGestureStart = (GestureStart) invocation;
                } else {
                    /* This is an ActionInvocation or idiom gesture. */
                    ActionModelDef amDef;
                    try {
                        amDef = bridge.getActionModel().getType(taskName);
                        /* This is just a null check: */
                        amDef.getActionModel();
                    } catch (Exception e) {
                        log.warn("Couldn't load " + taskName, e);
                        return;
                    }
                    if (amDef instanceof IdiomDef) {
                        IdiomDef idiomDef = (IdiomDef) amDef;
                        invocation = new GestureStart(idiomDef, bridge, parent,
                                null, taskName.getIdiomTemplateName(), serial,
                                uid);
                        lastGestureStart = (GestureStart) invocation;
                    } else if (amDef instanceof GestureEndDef) {
                        try {
                            invocation = new GestureEnd(lastGestureStart,
                                    serial, uid);
                        } catch (PALException e) {
                            log.warn("Impossible exception", e);
                        }
                        lastGestureStart = null;
                    } else {
                        ActionDef actionDef = (ActionDef) amDef;
                        try {
                            invocation = actionDef.invoked(parent, uid, serial);
                        } catch (PALException e) {
                            log.warn("Couldn't create invocation for "
                                    + startMsg, e);
                            return;
                        }
                        List<Object> inParams = startMsg.getInParams();
                        for (int i = 0; i < actionDef.numInputParams(); i++) {
                            Object strValue = inParams.get(i);
                            TypeDef type = actionDef.getParamType(i);
                            Object value = type.unstringify(strValue);
                            invocation.setValue(i, value);
                        }
                    }
                }
                getInvocCache().add(invocation);
            }
            try {
                invocation.updateStatus(Status.RUNNING);
            } catch (IllegalStateException e) {
                /*
                 * This should never happen.
                 */
                log.warn("Backwards state transition probably from "
                        + "out-of-order messages", e);
            }
            log.debug("Notified of starting {}", invocation);
        }

        private void handleSuccess(SuccessExecutionStatus successMsg) {
            ActionStreamEvent invocation;
            try {
                invocation = getInvocCache().get(uid, TIMEOUT * 2);
            } catch (TimeoutException e) {
                log.warn("Couldn't fetch ending invocation for UID {} -- "
                        + "memory leak is likely", uid);
                return;
            }
            if (invocation instanceof ActionInvocation) {
                AbstractActionDef def = invocation.getDefinition();
                List<Object> outParams = successMsg.getOutParams();
                try {
                    for (int i = def.numInputParams(); i < def.size(); i++) {
                        Object strValue = outParams.get(i
                                - def.numInputParams());
                        TypeDef type = invocation.getParamType(i);
                        Object value = type.unstringify(strValue);
                        invocation.setValue(i, value);
                    }
                } catch (PALException e) {
                    log.warn("Couldn't assign args to " + invocation + " from "
                            + successMsg, e);
                    /*
                     * We can't do this, because there is another JVM with a
                     * different idea of the status of this invocation. The
                     * other JVM thinks it's fine, so we can't have this one
                     * think it's got an error.
                     */
                    // invocation.error(e);
                }
            }
            invocation.updateStatus(Status.ENDED);
        }

        private void handleError(ErrorExecutionStatus errorMsg) {
            ActionStreamEvent invocation;
            try {
                invocation = getInvocCache().get(uid, TIMEOUT * 2);
            } catch (TimeoutException e) {
                log.warn("Couldn't fetch failing invocation for UID {} -- "
                        + "memory leak is likely", uid);
                return;
            }
            ErrorInfo error = errorMsg.getError();
            invocation.handleError(error);
        }

        private void handleIgnored(IgnoredExecutionStatus ignoreMsg) {
            ActionStreamEvent invocation;
            try {
                invocation = getInvocCache().get(uid, TIMEOUT * 2);
            } catch (TimeoutException e) {
                log.warn("Couldn't fetch failing invocation for UID {} -- "
                        + "memory leak is likely", uid);
                return;
            }
            log.warn("Execution request was ignored by all executors'{}'", ignoreMsg);
            invocation.updateStatus(Status.ENDED);
        }

        private void handleBreakpoint(BreakpointNotify breakMsg) {
            boolean justGo = false;
            ActionStreamEvent event = null;
            try {
                event = getInvocCache().get(uid, 0);
            } catch (TimeoutException e) {
                /*
                 * If the UID is unrecognized, we should tell Lumen to just
                 * step. It's probably for some Lumen internal function.
                 */
                justGo = true;
            }
            if (event instanceof GestureStart) {
                /* TODO We don't yet support stepping inside gestures. */
                justGo = true;
            }
            if (justGo) {
                log.info("Got breakpoint for {} with unrecognized uid {}",
                        breakMsg.getActionName(), breakMsg.getUid());
                BreakpointResponse reply = new BreakpointResponse(mySenderId,
                        Command.STEP_OVER, uid, null);
                try {
                    bridge.getSpine().send(reply);
                } catch (SpineException e1) {
                    log.warn("Unable to send " + reply, e1);
                }
            } else {
                ActionInvocation actInvoc = (ActionInvocation) event;
                actInvoc.setLocation(breakMsg.getPreorderIndex());
                actInvoc.setSubAction(breakMsg.getSubActionName());
                actInvoc.updateStatus(Status.PAUSED);
            }
        }
    }
}
