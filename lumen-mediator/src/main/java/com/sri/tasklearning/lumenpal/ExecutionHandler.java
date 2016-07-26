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

// $Id: ExecutionHandler.java 7750 2016-07-26 16:53:01Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.core.CoreUtil;
import com.sri.ai.lumen.mediator.MediatorException;
import com.sri.ai.lumen.mediator.TaskExecutor;
import com.sri.ai.lumen.runtime.LumenExtras;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.mediators.TypeFetcher;
import com.sri.tasklearning.mediators.WithLockedTypes;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.*;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.spine.util.ReplyWatcher;
import com.sri.tasklearning.spine.util.TypeUtil;
import com.sri.tasklearning.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * This class handles incoming {@link ExecuteRequest}s from other Spine clients.
 * The requests are dispatched to the appropriate {@link TaskExecutor} instance,
 * which presumably causes Lumen to execute the procedure.
 *
 * @author chris
 */
public class ExecutionHandler
        implements MessageHandler {
    public static final String NAMESPACE = "lumen";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final RunOnce runOnce;
    private final LockingActionModel actionModel;
    private final TypeFetcher typeFetcher;
    private final Spine spine;
    private final Map<SimpleTypeName, TaskExecutor> lumenExecutors;
    private final Map<TransactionUID, LumenTaskResultListener> resultListeners;
    private final ExecutorService threadPool;
    private final ReplyWatcher<SerialNumberResponse> serialNumberGetter;
    private final ErrorFactory errorFactory;
    private final WithLockedTypes withLockedTypes;
    private final ProcedureDependencyFinder finder;

    public ExecutionHandler(RunOnce runOnce,
                            LockingActionModel actionModel,
                            TypeFetcher typeFetcher,
                            ReplyWatcher<SerialNumberResponse> serialGetter,
                            Spine spine,
                            ProcedureDependencyFinder procDepFinder) {
        this.runOnce = runOnce;
        this.actionModel = actionModel;
        this.typeFetcher = typeFetcher;
        this.spine = spine;
        lumenExecutors = new HashMap<SimpleTypeName, TaskExecutor>();
        resultListeners = new HashMap<TransactionUID, LumenTaskResultListener>();
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newCachedThreadPool(tf);
        serialNumberGetter = serialGetter;
        errorFactory = new ErrorFactory(spine.getClientId());
        withLockedTypes = new WithLockedTypes(actionModel);
        finder = procDepFinder;
    }

    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (!(message instanceof ExecuteRequest)) {
            log.warn("Unexpected message ({}): {}", message.getClass(), message);
            return;
        }
        ExecuteRequest execMsg = (ExecuteRequest) message;              

        SimpleTypeName actionName = execMsg.getActionName();
        if (!canExecuteLocally(actionName)) {
            log.debug("{} is not my responsibility; ignoring", actionName);
            // Report back that Lumen is not handling this request
            threadPool.execute(new InnerIgnoreExecutionHandler(execMsg));
            return;
        }

        threadPool.execute(new InnerExecutionHandler(execMsg));
    }

    private boolean canExecuteLocally(SimpleTypeName actionName) {
        String namespace = actionName.getNamespace();
        return NAMESPACE.equals(namespace);
    }

    /**
     * Actually starts Lumen executing the given procedure. Also causes the
     * {@link StartExecutionStatus} to be sent.
     *
     * @param actionName
     * @param uid
     * @param parentUid
     * @param inParams
     * @throws SpineException
     * @throws MediatorException
     */
    protected void startExecution(SimpleTypeName actionName,
                                  TransactionUID uid,
                                  TransactionUID parentUid,
                                  List<Object> inParams,
                                  Runnable cleanup,
                                  boolean stepped)
            throws SpineException,
            MediatorException,
            InterruptedException {
        ATRActionDeclaration action = (ATRActionDeclaration) actionModel
                .getInherited(actionName);
        String source = ATRSyntax.toSource(action);
        ATRSig sig = action.getSignature();
        TaskExecutor executor = lumenExecutors.get(actionName);

        // Get a serial number for this action, because it might appear in a
        // demonstration.
        TransactionUID serialUid = spine.getNextUid();
        Message msg = new SerialNumberRequest(spine.getClientId(), serialUid);
        SerialNumberResponse serialMsg = serialNumberGetter.sendAndGetReply(msg);
        long serial = serialMsg.getSerialNumber();

        LumenTaskResultListener statusListener = new LumenTaskResultListener(
                actionName, uid, parentUid, serial, inParams, cleanup, spine,
                this, actionModel, serialNumberGetter);
        resultListeners.put(uid, statusListener);
        LumenTaskResultListener parentListener = resultListeners.get(parentUid);
        List<Object> args = new ArrayList<Object>();
        List<? extends ATRParameter> params = sig.getElements();
        for (int i = 0; i < params.size(); i++) {
            ATRParameter atrParam = params.get(i);
            Object paramValue;
            if (atrParam.getMode() == Modality.OUTPUT) {
                paramValue = LumenExtras.EXECUTOR_OUTPUT_MARKER;
            } else {
                paramValue = inParams.get(i);
                if (paramValue == null) {
                    paramValue = CoreUtil.NULL_VALUE;
                }
            }
            args.add(paramValue);
        }
        Map<String, Object> optInfo = null;
        statusListener.sendStartMessage();
        log.debug("Starting procedure {}, source: {}, uid {}, stepped {}",
                new Object[] { actionName, source, uid, stepped });
        executor.startTask(statusListener, uid.toString(), actionName
                .getFullName(), args, optInfo, parentListener, stepped);
    }

    private void sendFailureMessage(SimpleTypeName actionName,
                                    TransactionUID uid,
                                    TransactionUID parentUid,
                                    List<Object> inParams,
                                    ErrorInfo error) {
        try {
            /*
             * First send a start message, so this execution is on everybody's
             * radar map.
             */
            StartExecutionStatus startMsg = new StartExecutionStatus(
                    spine.getClientId(), uid, parentUid, actionName, -1,
                    inParams);
            spine.send(startMsg);

            /* Now send the error. */
            ErrorExecutionStatus failMsg = new ErrorExecutionStatus(
                    spine.getClientId(), uid, parentUid, error);
            log.debug("Sending failure message {}", failMsg);
            spine.send(failMsg);
        } catch (Exception e) {
            log.warn("Procedure " + uid
                    + " failed, then failed to send failure message " + uid, e);
        }
    }

    /**
     * Sends an ignore notification back to the requester
     */
    private class InnerIgnoreExecutionHandler
            implements Runnable {
        private ExecuteRequest execMsg;

        public InnerIgnoreExecutionHandler(ExecuteRequest execMsg) {
            this.execMsg = execMsg;
        }

        @Override
        public void run() {
            try {
                spine.send(new RequestIgnored(spine.getClientId(), execMsg.getUid(), execMsg.getParentUid()));
            } catch (SpineException e) {
                log.warn("Unable to send ignore message", e);
            }
        }
    }

    private class InnerExecutionHandler
            implements Runnable {
        private final SimpleTypeName actionName;
        private final TransactionUID parentUid;
        private final TransactionUID uid;
        private final List<Object> inParams;
        private final boolean stepped;
        private final ExecuteRequest message;

        public InnerExecutionHandler(ExecuteRequest execMsg) {
            // Read the request parameters.
            message = execMsg;
            uid = execMsg.getUid();
            parentUid = execMsg.getParentUid();
            actionName = execMsg.getActionName();
            inParams = execMsg.getInParams();
            stepped = execMsg.isStepped();
            log.debug("Received request for {} with UID {}, called by UID {}",
                    new Object[] { actionName, uid, parentUid });
        }

        @Override
        public void run() {
            try {
                runOnce.runOnce();
            } catch (Exception e) {
                log.warn("Lumen preload failed", e);
                ErrorInfo error = errorFactory.error(ErrorType.LUMEN, e);
                sendFailureMessage(actionName, uid, parentUid, inParams, error);
                return;
            }

            ErrorInfo error = null;
            try {
                ATRActionDeclaration proc = (ATRActionDeclaration) typeFetcher
                        .getType(actionName);
                StartExecAction action = new StartExecAction(actionName, uid,
                        parentUid, inParams, stepped);
                withLockedTypes.lockedAction(action, proc, finder);
                error = action.getError();
            } catch(Exception e) {
                log.warn("Start exec failed for " + message.getActionName(), e);
                error = errorFactory.error(ErrorType.PROC_START, actionName, e);
            }

            if(error != null) {
                sendFailureMessage(actionName, uid, parentUid, inParams, error);
            }
        }
    }

    private class StartExecAction
            extends WithLockedTypes.Action<Void, ATRActionDeclaration> {
        private final SimpleTypeName actionName;
        private final TransactionUID uid;
        private final TransactionUID parentUid;
        private final List<Object> inParams;
        private final boolean isStepped;
        private ErrorInfo error;

        public StartExecAction(SimpleTypeName actionName,
                               TransactionUID uid,
                               TransactionUID parentUid,
                               List<Object> inParams,
                               boolean isStepped) {
            this.actionName = actionName;
            this.uid = uid;
            this.parentUid = parentUid;
            this.inParams = inParams;
            this.isStepped = isStepped;
        }

        @Override
        public void run(ATRActionDeclaration proc,
                        List<ATRDecl> requiredTypes,
                        Runnable cleanup) {
            // Retrieve and lock any required actions, then start the execution.
            try {
                // Verify that executors exist for all required actions.
                for(ATRDecl type : requiredTypes) {
                    if (TypeUtil.isAction(type)) {
                        SimpleTypeName actName = TypeUtil.getName(type);
                        if (!typeFetcher.hasExecutor(actName)) {
                            throw new NoSuchElementException(
                                    "No executor for action " + actName);
                        }
                    }
                }

                // Start the execution.
                startExecution(actionName, uid, parentUid, inParams, cleanup,
                        isStepped);
                /*
                 * The readlock is released in
                 * LumenTaskResultListener.cleanup(), which is called when the
                 * task succeeds or fails. The exception handling below is for
                 * when the task can't be started.
                 */
            } catch (NoSuchElementException e) {
                error = errorFactory.error(ErrorType.EXEC_MISSING, e);
                sendFailureMessage(actionName, uid, parentUid, inParams, error);
                cleanup.run();
            } catch (Exception e) {
                log.warn("Failed to start " + actionName, e);
                error = errorFactory.error(ErrorType.LUMEN, e);
                sendFailureMessage(actionName, uid, parentUid, inParams, error);
                cleanup.run();
            }
        }

        @Override
        public ErrorInfo getError() {
            return error;
        }

        @Override
        public Void result() {
            return null;
        }
    }

    /**
     * Registers a Lumen executor which will handle the given task. Until this
     * method is called, execute requests for that task will not be handled.
     *
     * @param taskExecutor
     * @param taskName
     */
    public void addExecutor(TaskExecutor taskExecutor,
                            String taskName) {
        SimpleTypeName actionName = (SimpleTypeName) TypeNameFactory
                .makeName(taskName);
        lumenExecutors.put(actionName, taskExecutor);
    }

    /**
     * Unregisters an executor for the given task.
     *
     * @param taskName
     */
    public void removeExecutor(String taskName) {
        SimpleTypeName actionName = (SimpleTypeName) TypeNameFactory
                .makeName(taskName);
        TaskExecutor removed = lumenExecutors.remove(actionName);
        if (removed == null) {
            log.warn("Nothing to remove for {}", taskName);
        }
    }

    /**
     * When a Lumen procedure finishes executing, this method removes the
     * corresponding {@link LumenTaskResultListener}. That object was receiving
     * procedure status updates from Lumen and relaying them out via the Spine.
     *
     * @param uid
     */
    public void removeResultListener(TransactionUID uid) {
        resultListeners.remove(uid);
    }

    public void shutdown() {
        threadPool.shutdown();
    }
}
