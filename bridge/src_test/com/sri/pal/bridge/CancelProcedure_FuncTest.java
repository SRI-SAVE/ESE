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

package com.sri.pal.bridge;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionInvocationStatusListener;
import com.sri.pal.ActionModel;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.Bridge;
import com.sri.pal.GlobalActionListener;
import com.sri.pal.Learner;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.actionmodels.DebugExecutor;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.ErrorInfo.PALStackFrame;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;
import com.sri.tasklearning.spine.util.ErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CancelProcedure_FuncTest extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory.getLogger(CancelProcedure_FuncTest.class);

    private static final String NAMESPACE = "ns";

    private static Bridge palBridge;
    private static ActionModel actionModel;
    private static ActionDef logDef;
    private static ActionDef sleepDef;
    private static ActionDef failDef;
    private static DebugExecutor executor;
    private static Learner learner;

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.DEBUG);
        Bridge.startPAL();
        palBridge = Bridge.newInstance("cpft");
        ProcedureLearner.setStorage(CancelProcedure_FuncTest.class, palBridge);
        actionModel = palBridge.getActionModel();
        executor = new DebugExecutor();
        actionModel.load(url, NAMESPACE);
        learner = palBridge.getLearner();

        SimpleTypeName logName = (SimpleTypeName) TypeNameFactory.makeName(
                "log", "1.0", NAMESPACE);
        SimpleTypeName sleepName = (SimpleTypeName) TypeNameFactory.makeName(
                "sleep", "1.0", NAMESPACE);
        SimpleTypeName failName = (SimpleTypeName) TypeNameFactory.makeName(
                "fail", "1.0", NAMESPACE);
        actionModel.registerExecutor(logName, executor);
        actionModel.registerExecutor(sleepName, executor);
        actionModel.registerExecutor(failName, executor);
        logDef = (ActionDef) actionModel.getType(logName);
        sleepDef = (ActionDef) actionModel.getType(sleepName);
        failDef = (ActionDef) actionModel.getType(failName);
    }

    @AfterClass
    public static void shutdown() throws PALRemoteException, InterruptedException {
        palBridge.shutdown();
    }

    /**
     * This is a test to show that cancelling a running task in the TaskExecutor works as expected.
     * It causes some exceptions to be thrown when the executor tries to update the result - this
     * may need to be addressed.
     */
    @Test(timeOut = 10000)
    public void tryCancelARunningTask()
            throws Exception {
        ActionInvocation[] demo = new ActionInvocation[2];
        demo[0] = logDef.invoke(null, "before sleep");
        demo[1] = sleepDef.invoke(null, 3000);
        ProcedureDef proc = learner.learn("cpal76", null, demo);

        ProcedureInvocation invoc = proc.invoke(null);
        invoc.start();

        // Let it get started, then cancel it.
        Thread.sleep(1000);
        invoc.cancel();

        invoc.waitUntilFinished();

        // It should be in an error state, but with no exception because it was
        // canceled instead of hitting a real error.
        assertEquals(Status.FAILED, invoc.getStatus());
        ErrorInfo err = invoc.getError();
        /*
         * Depending on who wins the race, the source of the error could be
         * either DebugExecutor or Lumen-Mediator.
         */
        if (!DebugExecutor.class.getSimpleName().equals(err.getSource())
                && !"Lumen-mediator".equals(err.getSource())) {
            Assert.fail(err.getSource());
        }
        Assert.assertEquals(ErrorType.CANCEL.getTerseMsg(),
                err.getTerseMessage());
        Assert.assertEquals(ErrorType.CANCEL.ordinal(), err.getErrorId());
        List<PALStackFrame> stack = err.getStackInfo();
        Assert.assertEquals(2, stack.size());
        PALStackFrame frame0 = stack.get(0);
        Assert.assertEquals(proc.getName(), frame0.getActionName());
        Assert.assertEquals(17, frame0.getLocation());
        PALStackFrame frame1 = stack.get(1);
        Assert.assertEquals(sleepDef.getName(), frame1.getActionName());
    }

    /**
     * Verify that if a procedure invokes an action which fails, the procedure
     * gets marked as failed too.
     * @throws Exception when something goes wrong
     */
    @Test
    public void nestedProcedureFailure()
            throws Exception {
        ActionInvocation demo = failDef.invoke(null, "succeed");
        ProcedureDef proc = learner.learn("cpal179-1", null, demo);

        ProcedureInvocation invoc = proc.invoke(null, "fail");
        Listener listener = new Listener();
        invoc.addListener(listener);
        invoc.start();
        invoc.waitUntilFinished();
        assertEquals(Status.FAILED, listener.getStatus());

        ErrorInfo thr = listener.getError();
        assertNotNull(thr);
    }

    /**
     * Verify that if a procedure invokes an action which causes an error, the
     * procedure gets marked as failed too. Also, the error should get
     * propagated up to the calling procedure.
     */
    @Test
    public void nestedProcedureError()
            throws Exception {
        ActionInvocation demo = failDef.invoke(null, "succeed");
        ProcedureDef proc = learner.learn("cpal179-2", null, demo);

        ProcedureInvocation invoc = proc.invoke(null, "error");
        Listener listener = new Listener();
        invoc.addListener(listener);
        invoc.start();
        invoc.waitUntilFinished();

        assertEquals(Status.FAILED, listener.getStatus());
        assertNotNull(listener.getError());
        ErrorInfo err = listener.getError();
        assertEquals("test", err.getSource());
        assertEquals(1, err.getErrorId());
        assertEquals("err", err.getTerseMessage());
        assertEquals("error", err.getDetailedMessage());
        List<PALStackFrame> stack = err.getStackInfo();
        log.info("Stack: {}", stack);
        assertEquals(2, stack.size());
        PALStackFrame frame0 = stack.get(0);
        assertEquals(9, frame0.getLocation());
        assertEquals(proc.getName(), frame0.getActionName());
        PALStackFrame frame1 = stack.get(1);
        assertEquals(failDef.getName(), frame1.getActionName());
    }

    /**
     * proc2 calls proc1 which calls actionA. While actionA is running, we
     * cancel it. All three should show as FAILED.
     */
    @Test(timeOut = 30000)
    public void cancelRunningAction()
            throws Exception {
        ActionInvocation demo1 = sleepDef.invoke(null, 1000);
        ProcedureDef proc1 = learner.learn("cancelRunningAction1", null, demo1);

        ProcedureInvocation demo2 = proc1.invoke(null, 1000);
        ProcedureDef proc2 = learner.learn("cancelRunningAction2", null, demo2);

        /* Learning is done. Now run it. */
        CancelingListener listener = new CancelingListener(sleepDef.getName(), 100);
        palBridge.addActionListener(listener);
        ProcedureInvocation invoc2 = proc2.invoke(null, 1000);
        invoc2.start();
        invoc2.waitUntilFinished();

        verifyCancels(listener, proc2, proc1, invoc2,
                DebugExecutor.class.getSimpleName(), ErrorType.CANCEL.ordinal(), true);
    }

    /**
     * proc2 calls proc1 which calls actionA. While actionA is running, we
     * cancel proc1. All three should show as FAILED.
     */
    @Test(timeOut = 30000)
    public void cancelRunningProc()
            throws Exception {
        ActionInvocation demo1 = sleepDef.invoke(null, 1000);
        ProcedureDef proc1 = learner.learn("cancelRunningProc1", null, demo1);

        ProcedureInvocation demo2 = proc1.invoke(null, 1000);
        ProcedureDef proc2 = learner.learn("cancelRunningProc2", null, demo2);

        /* Learning is done. Now run it. */
        CancelingListener listener = new CancelingListener(proc1.getName(), 100);
        palBridge.addActionListener(listener);
        ProcedureInvocation invoc2 = proc2.invoke(null, 1000);
        invoc2.start();
        invoc2.waitUntilFinished();

        verifyCancels(listener, proc2, proc1, invoc2,
                DebugExecutor.class.getSimpleName(), 0, true);
    }

    /**
     * proc2 calls proc1 which calls actionA. While actionA is running, we
     * cancel proc2. All three should show as FAILED.
     */
    @Test(timeOut = 30000)
    public void cancelRunningTopLevel()
            throws Exception {
        ActionInvocation demo1 = sleepDef.invoke(null, 1000);
        ProcedureDef proc1 = learner.learn("cancelRunningProc1", null, demo1);

        ProcedureInvocation demo2 = proc1.invoke(null, 1000);
        ProcedureDef proc2 = learner.learn("cancelRunningProc2", null, demo2);

        /* Learning is done. Now run it. */
        CancelingListener listener = new CancelingListener(proc2.getName(), 100);
        palBridge.addActionListener(listener);
        ProcedureInvocation invoc2 = proc2.invoke(null, 1000);
        invoc2.start();
        invoc2.waitUntilFinished();

        verifyCancels(listener, proc2, proc1, invoc2, null,
                ErrorType.CANCEL.ordinal(), false);
        List<ActionStreamEvent> actionsSeen = listener.getSeenActions();
        /* Verify error for action */
        ActionStreamEvent invoc0 = actionsSeen.get(2);
        Assert.assertEquals(sleepDef, invoc0.getDefinition());
        Assert.assertNotNull(invoc0.getCaller());
        Assert.assertEquals(invoc2, invoc0.getCaller().getCaller());

        /*
         * We send out a cancel message for the top-level procedure, and all
         * executors receive it. Lumen will cancel the procedure. Also
         * DebugExecutor will cancel the sleep action because it's ultimately
         * called by that procedure. If Lumen responds faster than
         * DebugExecutor, we'll see the procedure fail before the action does.
         * To handle that possibility, we wait for the action to complete also.
         */
        invoc0.waitUntilFinished();

        Assert.assertEquals(Status.FAILED, invoc0.getStatus());
        ErrorInfo err0 = invoc0.getError();
        Assert.assertNotNull(err0);
        Assert.assertEquals(DebugExecutor.class.getSimpleName(),
                err0.getSource());
        Assert.assertEquals(0, err0.getErrorId());
        List<PALStackFrame> stack0 = err0.getStackInfo();
        Assert.assertEquals(0, stack0.size());
    }

    private void verifyCancels(CancelingListener listener,
                               ProcedureDef proc2,
                               ProcedureDef proc1,
                               ProcedureInvocation invoc2,
                               String errorSource,
                               int errorNum,
                               boolean verifyAction) {
        /* Did the listener actually call cancel? */
        Assert.assertTrue(listener.didCancel());
        List<ActionStreamEvent> actionsSeen = listener.getSeenActions();
        Assert.assertEquals(3, actionsSeen.size());

        /* Verify error for proc2 */
        Assert.assertEquals(invoc2, actionsSeen.get(0));
        Assert.assertEquals(Status.FAILED, invoc2.getStatus());
        ErrorInfo err2 = invoc2.getError();
        Assert.assertNotNull(err2);
        if (errorSource != null) {
            Assert.assertEquals(errorSource, err2.getSource());
        }
        Assert.assertEquals(errorNum, err2.getErrorId());
        List<PALStackFrame> stack2 = err2.getStackInfo();
        Assert.assertEquals(3, stack2.size());
        PALStackFrame frame0 = stack2.get(0);
        Assert.assertEquals(9, frame0.getLocation());
        Assert.assertEquals(proc2.getName(), frame0.getActionName());
        PALStackFrame frame1 = stack2.get(1);
        Assert.assertEquals(9, frame1.getLocation());
        Assert.assertEquals(proc1.getName(), frame1.getActionName());
        PALStackFrame frame2 = stack2.get(2);
        Assert.assertEquals(sleepDef.getName(), frame2.getActionName());

        /* Verify error for proc1 */
        ActionStreamEvent invoc1 = actionsSeen.get(1);
        Assert.assertEquals(proc1, invoc1.getDefinition());
        Assert.assertEquals(Status.FAILED, invoc1.getStatus());
        ErrorInfo err1 = invoc1.getError();
        Assert.assertNotNull(err1);
        if (errorSource != null) {
            Assert.assertEquals(errorSource, err1.getSource());
        }
        Assert.assertEquals(errorNum, err1.getErrorId());
        List<PALStackFrame> stack1 = err1.getStackInfo();
        Assert.assertEquals(3, stack1.size());
        Assert.assertEquals(frame0, stack1.get(0));
        Assert.assertEquals(frame1, stack1.get(1));
        Assert.assertEquals(frame2, stack1.get(2));

        if (verifyAction) {
            /* Verify error for action */
            ActionStreamEvent invoc0 = actionsSeen.get(2);
            Assert.assertEquals(sleepDef, invoc0.getDefinition());
            Assert.assertEquals(Status.FAILED, invoc0.getStatus());
            ErrorInfo err0 = invoc0.getError();
            Assert.assertNotNull(err0);
            Assert.assertEquals(errorSource, err0.getSource());
            Assert.assertEquals(errorNum, err0.getErrorId());
            List<PALStackFrame> stack0 = err0.getStackInfo();
            Assert.assertEquals(0, stack0.size());
        }
    }

    private class Listener
            implements ActionInvocationStatusListener {
        private ErrorInfo error;
        private Status status;

        @Override
        public void error(ErrorInfo error) {
            log.info("Called: {}", error);
            this.error = error;
        }

        public ErrorInfo getError() {
            return error;
        }

        public Status getStatus() {
            return status;
        }

        @Override
        public void newStatus(Status newStatus) {
            status = newStatus;
        }
    }

    private class CancelingListener
            implements GlobalActionListener {
        private final SimpleTypeName targetAction;
        private final List<ActionStreamEvent> actions;
        private final int delay;
        private boolean didCancel;

        public CancelingListener(SimpleTypeName name,
                                 int delay) {
            targetAction = name;
            actions = Collections
                    .synchronizedList(new ArrayList<ActionStreamEvent>());
            this.delay = delay;
            didCancel = false;
        }

        public List<ActionStreamEvent> getSeenActions() {
            return Collections.unmodifiableList(actions);
        }

        public boolean didCancel() {
            return didCancel;
        }

        @Override
        public void actionStarted(final ActionStreamEvent action) {
            log.debug("Got action: {}", action);
            actions.add(action);
            if (!didCancel
                    && action.getDefinition().getName().equals(targetAction)) {
                log.debug("Will sleep {} and cancel", delay);
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            log.warn("interrupted", e);
                        }
                        if (action instanceof ActionInvocation) {
                            didCancel = true;
                            ((ActionInvocation) action).cancel();
                            log.debug("Canceled");
                        } else {
                            log.error("Not cancelable: {}", action);
                        }
                    }
                };
                t.start();
            }
        }
    }
}
