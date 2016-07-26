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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sri.ai.lumen.core.CoreUtil;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.mediator.MediatorException;
import com.sri.ai.lumen.mediator.ServerConnection;
import com.sri.ai.lumen.mediator.TaskExecutionListener;
import com.sri.ai.lumen.mediator.TaskExecutor;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ReplyWatcher;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the interface that Lumen uses to interact with this moderator
 * code. The instance is passed to the LumenConnection class (the entry point
 * into the Lumen Module) by the LumenClient class. This handle is used by Lumen
 * to register for tasks, issue updates, issue calls to other system clients
 * etc.
 * <p>
 * Every method in this class is called exclusively by Lumen.
 */
public class ServerConnectionImpl
        implements ServerConnection {
    private static final Logger log = LoggerFactory
            .getLogger(ServerConnectionImpl.class);

    private final ExecutionHandler execHandler;
    private final LockingActionModel actionModel;
    private final Spine spine;
    private final ExecutionWatcher execWatcher;
    private final ExecutorService threadPool;
    private final ReplyWatcher<SerialNumberResponse> serialGetter;

    /**
     * Private constructor called by getInstance to control instance count
     */
    public ServerConnectionImpl(ExecutionHandler execHandler,
                                ExecutionWatcher executionWatcher,
                                LockingActionModel actionModel,
                                Spine spineFacade,
                                ReplyWatcher<SerialNumberResponse> serialGetter) {
        log.debug("starting");
        this.execHandler = execHandler;
        this.actionModel = actionModel;
        spine = spineFacade;
        execWatcher = executionWatcher;
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newCachedThreadPool(tf);
        this.serialGetter = serialGetter;
    }

    /**
     * This method allows Lumen to register itself as the task executor for a
     * specific task.
     *
     * @param taskExecutor
     *            The task executor responsible for this task (Lumen)
     * @param taskName
     *            The task that is being registered for
     */
    @Override
    public void registerAsTaskExecutor(TaskExecutor taskExecutor,
                                       String taskName) {
        execHandler.addExecutor(taskExecutor, taskName);
    }

    /**
     * Allows Lumen to unregister itself as the task executor for a task.
     *
     * @param taskExecutor
     *            The current task executor for the task
     * @param taskName
     *            The task that is about to be abandoned
     */
    @Override
    public void unregisterAsTaskExecutor(TaskExecutor taskExecutor,
                                         String taskName) {
        execHandler.removeExecutor(taskName);
    }

    /**
     * Called by Lumen when it starts executing a subtask of a procedure. Lumen
     * will provide the return value of the subtask to the returned
     * {@link TaskExecutionListener} instance.
     *
     * @param subTaskUID
     *            The ID of the task associated with this task. Not used for
     *            this invocation from Lumen to the mediator. Lumen will
     *            retrieve the task ID by calling
     *            {@link TaskExecutionListener#getTaskMessageUID} on our return
     *            value.
     * @param subTaskName
     *            The name associated with this task
     * @param xpsSubtaskArgs
     *            The arguments provided to the task
     * @param parentTask
     *            The listener of the parent task
     * @return the SubTaskExecutionListener for the sub task
     * @throws MediatorException
     *             if something unexpected happens
     */
    @Override
    public TaskExecutionListener createSubTaskExecutionListener(String subTaskUID,
                                                                String subTaskName,
                                                                List<?> xpsSubtaskArgs,
                                                                TaskExecutionListener parentTask)
            throws MediatorException {
        TransactionUID uid = spine.getNextUid();
        String parentTaskUIDStr = parentTask.getTaskMessageUID();
        TransactionUID parentUid = new TransactionUID(parentTaskUIDStr);
        List<Object> inParams = new ArrayList<Object>();
        SimpleTypeName taskName;

        if (subTaskName == null || subTaskName.equals("")) {
            /* We've started a non-idiom gesture. */
            taskName = null;
        } else {
            /* It's either an idiom gesture or a sub-procedure call. */
            taskName = (SimpleTypeName) TypeNameFactory.makeName(subTaskName);
            ATRActionDeclaration action = (ATRActionDeclaration) actionModel
                    .getInherited(taskName);
            if (xpsSubtaskArgs == null) {
                /* It's an idiom gesture. */
            } else {
                /* It's a sub-procedure call. */
                List<? extends ATRParameter> atrParams = action.getSignature()
                        .getElements();
                for (int i = 0; i < atrParams.size(); i++) {
                    ATRParameter atrParam = atrParams.get(i);
                    if (atrParam.getMode() == Modality.OUTPUT) {
                        continue;
                    }
                    Object paramValue = xpsSubtaskArgs.get(i);
                    inParams.add(paramValue);
                }
            }
        }

        Runnable noCleanup = new Runnable() {
            @Override
            public void run() {
            }
        };
        LumenTaskResultListener listener = new LumenTaskResultListener(
                taskName, uid, parentUid, 0, inParams, noCleanup, spine,
                execHandler, actionModel, serialGetter);
        try {
            listener.sendStartMessage();
        } catch (SpineException e) {
            throw new MediatorException(e);
        }
        return listener;
    }

    /**
     * Lumen calls this to invoke the tasks of other agents. The reply messages
     * to the task are delivered to the {@link TaskExecutionListener} argument
     *
     * @param requester
     *            The requesting task execution listener (representing Lumen)
     * @param newTaskId
     *            The id of the new task to be started -- this is not used by
     *            Lumen.
     * @param taskname
     *            The name of the new task to be started
     * @param xpsTaskArgs
     *            The arguments to the task
     * @param optInfo
     *            Additional optional information
     * @param parentTask
     *            The listener for the parent task
     */
    @Override
    public void startTask(final TaskExecutionListener requester,
                          final String newTaskId,
                          String taskname,
                          List<Object> xpsTaskArgs,
                          Map<String, Object> optInfo,
                          TaskExecutionListener parentTask,
                          boolean stepped)
            throws MediatorException {
        TransactionUID uid = spine.getNextUid();
        String parentUidStr = parentTask.getTaskMessageUID();
        TransactionUID parentUid = new TransactionUID(parentUidStr);
        SimpleTypeName actionName = (SimpleTypeName) TypeNameFactory
                .makeName(taskname);

        StartTask startTask = new StartTask(requester, uid, parentUid,
                actionName, xpsTaskArgs, stepped);
        threadPool.execute(startTask);
    }

    /**
     * This allows Lumen to add a new task type into the ITL Server Task
     * Registry. Since the task registry doesn't exist, this method does
     * nothing.
     *
     * @param xpsTaskProps
     *            The task details
     * @return The full type name associated with the task
     */
    @Override
    public Map<String, Object> addTask(Map<String, Object> xpsTaskProps) {
        // Do nothing.
        return null;
    }

    void shutdown() {
        threadPool.shutdown();
    }

    private class StartTask
            implements Runnable {
        private final SimpleTypeName actionName;
        private final TransactionUID uid;
        private final TransactionUID parentUid;
        private final List<Object> taskArgs;
        private final TaskExecutionListener requester;
        private final boolean stepped;

        public StartTask(TaskExecutionListener requester,
                         TransactionUID uid,
                         TransactionUID parentUid,
                         SimpleTypeName actionName,
                         List<Object> taskArgs,
                         boolean stepped) {
            this.requester = requester;
            this.actionName = actionName;
            this.uid = uid;
            this.parentUid = parentUid;
            this.taskArgs = taskArgs;
            this.stepped = stepped;
        }

        @Override
        public void run() {
            List<Object> inParams = null;
            ATRActionDeclaration action = null;
            try {
                action = (ATRActionDeclaration) actionModel
                        .getInherited(actionName);
                // TODO TLEARN-413 fails on this line:
                List<? extends ATRParameter> atrParams = action.getSignature()
                        .getElements();
                inParams = new ArrayList<Object>();
                for (int i = 0; i < atrParams.size(); i++) {
                    ATRParameter atrParam = atrParams.get(i);
                    if (atrParam.getMode() == Modality.OUTPUT) {
                        continue;
                    }
                    Object paramValue = taskArgs.get(i);
                    if (CoreUtil.isNull(paramValue)) {
                        paramValue = null;
                    }
                    inParams.add(paramValue);
                }
            } catch (Exception e) {
                try {
                    requester.taskFailed(uid.toString(), e, null);
                } catch (MediatorException e1) {
                    log.warn("Failed to start task, and failed to send failure message to Lumen for " + actionName, e1);
                }
                return;
            }

            log.debug("Requesting execution of {} with UID {} and args {}",
                    new Object[] { actionName, uid, taskArgs });
            ExecuteRequest execMsg = new ExecuteRequest(spine.getClientId(),
                    uid, parentUid, actionName, inParams, stepped);
            execWatcher.watch(uid, requester, action, taskArgs);
            try {
                spine.send(execMsg);
            } catch (SpineException e) {
                try {
                    requester.taskFailed(uid.toString(), e, null);
                } catch (MediatorException e1) {
                    log.warn("Failed to send task start message, and"
                            + " failed to send failure message to Lumen for "
                            + actionName + " " + uid, e1);
                }
            }
        }
    }
}
