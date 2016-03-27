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

package com.sri.tasklearning.lumenpal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sri.ai.lumen.core.CoreUtil;
import com.sri.ai.lumen.core.Failure;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.errors.RunErrAborted;
import com.sri.ai.lumen.mediator.TaskExecutionListener;
import com.sri.ai.lumen.runtime.LumenStackTraceElement;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.ErrorInfo.PALStackFrame;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.ExecutionStatus;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SerialNumberRequest;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.StartExecutionStatus;
import com.sri.tasklearning.spine.messages.SuccessExecutionStatus;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.spine.util.ReplyWatcher;
import com.sri.tasklearning.spine.util.TypeUtil;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class tracks the results of a procedure call or gesture which Lumen is
 * handling and houses the report of success or failure of the same. This is
 * responsible for sending {@link ExecutionStatus} messages to the other Spine
 * clients in response to status updates received from Lumen.
 * <p>
 * The {@link ExecutionHandler} will construct this class and pass it to Lumen
 * as part of the execution call. This class is also used by
 * {@link ServerConnectionImpl#createSubTaskExecutionListener}.
 */
public class LumenTaskResultListener
        implements TaskExecutionListener {
    private static final Logger log = LoggerFactory
            .getLogger(LumenTaskResultListener.class);

    private static final ThreadFactory threadFactory = new NamedThreadFactory(
            LumenTaskResultListener.class);
    private static final ExecutorService threadPool = Executors
            .newFixedThreadPool(1, threadFactory);

    private final SimpleTypeName actionName;
    private final TransactionUID uid;
    private final TransactionUID parentUid;
    private final long serial;
    private final List<Object> inParams;
    private final Runnable cleanupTask;
    private final LockingActionModel actionModel;
    private final Spine spine;
    private final ExecutionHandler execHandler;
    private final ErrorFactory errorFactory;
    private final ReplyWatcher<SerialNumberResponse> serialGetter;

    public LumenTaskResultListener(SimpleTypeName actionName,
                                   TransactionUID uid,
                                   TransactionUID parentUid,
                                   long serialNumber,
                                   List<Object> inParams,
                                   Runnable cleanup,
                                   Spine spineFacade,
                                   ExecutionHandler execHandler,
                                   LockingActionModel actionModel,
                                   ReplyWatcher<SerialNumberResponse> serialGetter) {
        this.actionName = actionName;
        this.uid = uid;
        this.parentUid = parentUid;
        serial = serialNumber;
        this.inParams = inParams;
        cleanupTask = cleanup;
        this.actionModel = actionModel;
        spine = spineFacade;
        this.execHandler = execHandler;
        errorFactory = new ErrorFactory(spine.getClientId());
        this.serialGetter = serialGetter;
    }

    /**
     * Called if the task succeeds
     *
     * @param taskid
     *            the id of the task
     * @param resultList
     *            the output values
     * @return true
     */
    @Override
    public boolean taskSucceeded(String taskid,
                                 List<Object> resultList) {
        TaskSucceeded taskSucceeded = new TaskSucceeded(taskid, resultList);
        threadPool.execute(taskSucceeded);
        return true;
    }

    /**
     * Called if the task failed
     *
     * @param taskid
     *            the id of the task
     * @param reason
     *            reason for failure
     * @return false
     */
    @Override
    public boolean taskFailed(final String taskid,
                              Object reason,
                              List<LumenStackTraceElement> stack) {
        if (reason instanceof Throwable) {
            log.debug("taskFailed", (Throwable) reason);
        } else if (reason != null) {
            log.debug("taskFailed: {} ({})", reason, reason.getClass());
        }
        ErrorInfo error;
        if (reason == null) {
            log.warn("Received null reason for taskFailed({}, null, {})",
                    taskid, stack);
            error = errorFactory.error(ErrorType.UNKNOWN);
        } else if (reason instanceof ErrorInfo) {
            error = (ErrorInfo) reason;
            List<PALStackFrame> palStack = TypeConverter.fromLumenStack(stack);
            error = new ErrorInfo(error.getSource(), error.getErrorId(),
                    error.getTerseMessage(), error.getDetailedMessage(),
                    palStack);
        } else if (reason instanceof Failure) {
            Failure lumenError = (Failure) reason;
            List<PALStackFrame> palStack = TypeConverter.fromLumenStack(stack);
            error = new ErrorInfo("lumen", lumenError.getErrorId(),
                    lumenError.getShortMessage(), lumenError.getMessage(),
                    palStack);
        } else {
            error = errorFactory.error(ErrorType.LUMEN, reason);
        }
        if (error.getErrorId() == RunErrAborted.ID) {
            error = errorFactory.error(ErrorType.CANCEL, error.getStackInfo(),
                    actionName.getFullName());
        }
        final ErrorExecutionStatus statusMsg = new ErrorExecutionStatus(
                spine.getClientId(), uid, parentUid, error);

        // Send this message in the same thread pool as the TaskSucceeded
        // messages, to make sure they go in order.
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    spine.send(statusMsg);
                } catch (SpineException e) {
                    log.warn("Couldn't send procedure error message for "
                            + taskid, e);
                } finally {
                    cleanup();
                }
            }
        };
        threadPool.execute(r);

        return false;
    }

    /**
     * Get the task message id
     *
     * @return the id
     */
    public String getTaskMessageUID() {
        return uid.toString();
    }

    /**
     * Release locks, try to remove actions from Lumen's action model, and
     * remove this listener.
     */
    private void cleanup() {
        cleanupTask.run();
        execHandler.removeResultListener(uid);
    }

    /**
     * Sends a {@link StartExecutionStatus} for this execution.
     *
     * @throws SpineException
     */
    public void sendStartMessage()
            throws SpineException {
        StartExecutionStatus statusMsg = new StartExecutionStatus(spine
                .getClientId(), uid, parentUid, actionName, serial, inParams);
        spine.send(statusMsg);
    }

    private class TaskSucceeded
            implements Runnable {
        private final String taskid;
        private final List<Object> resultList;

        public TaskSucceeded(String taskid,
                             List<Object> resultList) {
            this.taskid = taskid;
            this.resultList = resultList;
        }

        @Override
        public void run() {
            try {
                log.debug("taskSucceeded: taskId: {} Task result arg list: {}",
                        taskid, resultList);
                List<Object> outParams = new ArrayList<Object>();
                boolean isGestureStart = false;
                if (actionName == null) {
                    isGestureStart = true;
                } else if(actionName.equals(TypeUtil.GESTURE_END_NAME)) {
                    /* Do nothing. */
                } else {
                    /*
                     * It has a name, so it's either a sub-procedure or an idiom
                     * gesture. If it's a sub-procedure, it has output
                     * parameters that we need to fill in values for. But if
                     * it's an idiom, Lumen won't have sent us any output
                     * parameters.
                     */
                    ATRActionDeclaration action = (ATRActionDeclaration) actionModel
                            .getInherited(actionName);
                    if (action == null || resultList == null) {
                        isGestureStart = true;
                    } else {
                        List<? extends ATRParameter> atrParams = action
                                .getSignature().getElements();
                        for (int i = 0; i < atrParams.size(); i++) {
                            ATRParameter atrParam = atrParams.get(i);
                            if (atrParam.getMode() == Modality.INPUT) {
                                continue;
                            }
                            try {
                                Object paramValue = resultList.get(i);
                                if (CoreUtil.isNull(paramValue)) {
                                    paramValue = null;
                                }
                                outParams.add(paramValue);
                            } catch (Exception e) {
                                log.warn(
                                        "Couldn't assign param " + i + " of "
                                                + actionName + ": "
                                                + ATRSyntax.toSource(action), e);
                            }
                        }
                    }
                }
                SuccessExecutionStatus statusMsg = new SuccessExecutionStatus(
                        spine.getClientId(), uid, parentUid, inParams,
                        outParams);
                spine.send(statusMsg);

                /*
                 * If it's a gesture (or idiom gesture), we need to create a
                 * mock "gesture end" event to mark the end of the gesture, then
                 * have it send its own completion message also.
                 */
                if (isGestureStart) {
                    /* First get a serial number for the gesture end. */
                    TransactionUID serialUid = spine.getNextUid();
                    Message msg = new SerialNumberRequest(spine.getClientId(),
                            serialUid);
                    SerialNumberResponse serialMsg = serialGetter
                            .sendAndGetReply(msg);
                    long endSerial = serialMsg.getSerialNumber();

                    TransactionUID endUid = spine.getNextUid();
                    LumenTaskResultListener end = new LumenTaskResultListener(
                            TypeUtil.GESTURE_END_NAME, endUid, parentUid,
                            endSerial, Collections.emptyList(), cleanupTask,
                            spine, execHandler, actionModel, serialGetter);
                    end.sendStartMessage();
                    end.new TaskSucceeded(taskid, null).run();
                }
            } catch (SpineException e) {
                log.warn("Couldn't send task completion message for " + taskid,
                        e);
            } finally {
                cleanup();
            }
        }
    }
}
