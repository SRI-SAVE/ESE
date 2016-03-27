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

// $Id: ActionExecAdapter.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;

import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.RequestIgnored;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates between Lumen's task execution requests and the Bridge's API for
 * the same thing. In particular, this receives events and makes calls to the
 * <code>ActionExecutor</code>. Contrast with {@link ITLExecutionStatusListener}
 * , which receives notifications of all actions that start or stop, regardless
 * of who's responsible for executing them.
 *
 * @author chris
 */
class ActionExecAdapter
        implements MessageHandler {
    private static final Logger log = LoggerFactory
            .getLogger(ActionExecAdapter.class);

    // TODO This is duplicated in ITLExecutionStatusListener.
    private static final long TIMEOUT = 10 * 1000;

    private final Bridge bridge;
    private final String appName;
    private final ExecutorService threadPool;
    private final ErrorFactory errorFactory;

    public ActionExecAdapter(Bridge bridge) {
        this.bridge = bridge;
        appName = bridge.getSpine().getClientId();
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newCachedThreadPool(tf);
        errorFactory = new ErrorFactory(appName);
    }

    private ActionModel getActionModel() {
        return bridge.getActionModel();
    }

    private InvocationCache getInvocCache() {
        return bridge.getInvocationCache();
    }

    private Spine getSpine() {
        return bridge.getSpine();
    }

    @Override
    public void handleMessage(Message message) {
        if (!(message instanceof ExecuteRequest)) {
            log.warn("Received unexpected message ({}): {}",
                    message.getClass(), message);
            return;
        }

        ExecuteRequest execMsg = (ExecuteRequest) message;

        if (appName.equals(execMsg.getSender())) {
            log.debug("Ignoring execute message from self: {}", message);
            return;
        }

        log.debug("New execution request: {}", execMsg);

        threadPool.execute(new ExecRunner(execMsg));
    }

    private void innerHandleMessage(ExecuteRequest execMsg)
            throws PALException {
        // Get basic task info.
        SimpleTypeName taskName = execMsg.getActionName();
        TransactionUID uid = execMsg.getUid();
        TransactionUID parentUid = execMsg.getParentUid();

        // TODO: There is probably a more reliable way of accomplishing this - disucuss with Chris
        // This prevents a deadlock. When Lumen is asked to execute a procedure by PALBridge, it
        // makes a gather request for Types it does not know about. The other Bridge (mockBridge
        // in the tests) also gets the execution request and in order to determine if it is from
        // Lumen (in which case it needs to act), or from the other Bridge, it examines the exectutor
        // for the action. However in order to do this it must get the type, this causes a gather request
        // for types - this was waiting on lumen for an answer and lumen was waiting on the mock bridge.
        // This should be taken care of by the gather persmission semaphore which prevents more than one
        // gather request being issued in the system at one time, but it still seems like a wasteful call.
        // This would be fine but if 'PALBridge' is not used for starting up the main Bridge this will
        // not work as expected.
        if (uid.getOriginator().equals("PALBridge")) {
            log.debug("Request coming from PALBridge, exiting: " + uid.getOriginator());
            return;
        }

        // Check the action definition
        ActionDef actionDef = (ActionDef) getActionModel().getType(taskName);
        if (actionDef == null) {
            sendIgnore(execMsg);
            log.debug("Ignoring request for unknown action {} from {}",
                    execMsg, appName);
            return;
        }
        if (actionDef.getExecutor() == null) {
            sendIgnore(execMsg);
            log.debug("Ignoring request for null executor {} from client {} ",
                    execMsg, appName);
            return;
        }

        if (actionDef.getExecutor() instanceof LumenProcedureExecutor) {
            log.debug("Ignoring request for Lumen: {} - the client id is: {}", execMsg, appName);
            sendIgnore(execMsg);
            return;
        }

        // Get the parent invocation.
        ActionStreamEvent parent;
        try {
            // Before requesting the invocation for the parent, first make sure that it
            // is not currently being processed by the ITLExecutionListener. If it is, this
            // line will wait until notified
            getInvocCache().getParentInvocationReady(parentUid);
            parent = getInvocCache().get(parentUid, TIMEOUT);
        } catch (TimeoutException e) {
            log.warn("Couldn't find parent with UID " + parentUid
                    + " for child with UID " + uid);
            // We're going to try to run this action anyway.
            parent = null;
        }

        // It only needs a serial number if it's going to be used in a
        // demonstration sent to LAPDOG. That's only going to happen if it's a
        // top-level, meaning user-initiated, action. That's only true if it has
        // no parent.
        int serialNum = 0;
        // Build the invocation object.
        ActionInvocation invocation = new ActionInvocation(actionDef, parent,
                serialNum, uid);
        /* Assign args. */
        try {
            List<Object> inParams = execMsg.getInParams();
            for (int i = 0; i < actionDef.numInputParams(); i++) {
                TypeDef fieldType = actionDef.getParamType(i);
                Object strValue = inParams.get(i);
                Object value = fieldType.unstringify(strValue);
                invocation.setValue(i, value);
            }

            // Invoke it
            getInvocCache().add(invocation);
            log.debug("Starting {} for {}", invocation, execMsg);
            invocation.start();
        } catch (Exception e) {
            log.warn("Unable to start " + invocation, e);
            ErrorInfo error = errorFactory.error(
                    ErrorType.INTERNAL_ACTION_EXEC, execMsg);
            ErrorExecutionStatus errorMsg = new ErrorExecutionStatus(
                    appName, uid, parentUid, error);
            sendError(errorMsg);
        }

        // It will notify interested parties all on its own.
    }

    /**
     * Sent to inform the requesting spine that this executor will not be
     * executing this request
     * @param exeRequest The message that is being ignored
     */
    private void sendIgnore(ExecuteRequest exeRequest) {
        try {
            TransactionUID uid = exeRequest.getUid();
            TransactionUID parentUid = exeRequest.getParentUid();
            getSpine().send(new RequestIgnored(appName, uid, parentUid));
        } catch (SpineException e) {
            log.warn("Exception when sending ignore message", e);
        }
    }

    private void sendError(ErrorExecutionStatus msg) {
        try {
            getSpine().send(msg);
        } catch (SpineException e) {
            log.warn("Couldn't send failure message: " + msg, e);
        }
    }

    private class ExecRunner
            implements Runnable {
        private final ExecuteRequest execMsg;
        private final TransactionUID uid;
        private final TransactionUID parentUid;

        public ExecRunner(ExecuteRequest execMsg) {
            this.execMsg = execMsg;
            this.uid = execMsg.getUid();
            this.parentUid = execMsg.getParentUid();
        }

        @Override
        public void run() {
            try {
                innerHandleMessage(execMsg);
            } catch (Exception e) {
                log.warn("Exception handling execution request " + execMsg, e);
                ErrorInfo error = errorFactory.error(
                        ErrorType.INTERNAL_ACTION_EXEC, execMsg);
                ErrorExecutionStatus errorMsg = new ErrorExecutionStatus(
                        appName, uid, parentUid, error);
                sendError(errorMsg);
            }
        }
    }
}
