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

// $Id: Stepping_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.sri.pal.ActionInvocation.StepCommand;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Checks of our ability to run procedures in stepped (aka debug) mode.
 */
public class Stepping_FuncTest
        extends PALBridgeTestCase {
    private static final String NS = "stepping";
    private static final String VERS = "1.0";

    private static SimpleTypeName makeStringId;
    private static SimpleTypeName concatId;
    private static ActionDef makeStringDef;
    private static ActionDef concatDef;
    private static ProcedureDef procDef;
    private static ProcedureDef superProcDef;

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = ActionModels.class.getResource("debug.xml");
        setup(url, NS);

        makeStringId = (SimpleTypeName) TypeNameFactory.makeName("makeString",
                VERS, NS);
        makeStringDef = (ActionDef) actionModel.getType(makeStringId);
        actionModel.registerExecutor(makeStringId, callbackHandler);
        concatId = (SimpleTypeName) TypeNameFactory
                .makeName("concat", VERS, NS);
        concatDef = (ActionDef) actionModel.getType(concatId);
        actionModel.registerExecutor(concatId, callbackHandler);

        // Learn a procedure of a few actions with some dataflow.
        String str1 = "Ham";
        String str2 = "burg";
        List<ActionInvocation> events = new ArrayList<ActionInvocation>();
        ActionInvocation event = makeStringDef.invoke(null);
        event.setValue(0, str1);
        events.add(event);
        event = makeStringDef.invoke(null);
        event.setValue(0, str2);
        events.add(event);
        event = concatDef.invoke(null);
        event.setValue(0, str1);
        event.setValue(1, str2);
        event.setValue(2, str1 + str2);
        events.add(event);
        procDef = ProcedureLearner.learnProcedure(events, "stepping");

        /* Learn a procedure which calls the first. */
        events.clear();
        event = makeStringDef.invoke(null);
        event.setValue(0, "foo");
        events.add(event);
        event = procDef.invoke(null);
        events.add(event);
        event = concatDef.invoke(null);
        event.setValue(0, "be");
        event.setValue(1, "gin");
        event.setValue(2, "begin");
        events.add(event);
        superProcDef = ProcedureLearner.learnProcedure(events, "super");
    }

    @BeforeMethod
    public void reset() {
        callbackHandler.reset();
    }

    /**
     * Simple case of stepping through a short procedure. Make sure it's stopped
     * at all the right spots when we tell it to stop.
     *
     * @throws Exception
     */
    @Test(timeOut = 10000)
    public void stepOver()
            throws Exception {
        ProcedureInvocation invoc = procDef.invoke(null);
        invoc.startStepping();
        waitUntilPaused(invoc);
        Assert.assertEquals(0, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.PAUSED, invoc.getStatus());
        Assert.assertEquals(13, invoc.getLocation());
        Assert.assertEquals(makeStringId, invoc.getSubAction());

        stepAndSync(invoc);
        Assert.assertEquals(1, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.PAUSED, invoc.getStatus());
        Assert.assertEquals(16, invoc.getLocation());
        Assert.assertEquals(makeStringId, invoc.getSubAction());

        stepAndSync(invoc);
        Assert.assertEquals(2, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.PAUSED, invoc.getStatus());
        Assert.assertEquals(19, invoc.getLocation());
        Assert.assertEquals(concatId, invoc.getSubAction());

        invoc.continueStepping(StepCommand.STEP_OVER);
        invoc.waitUntilFinished();
        Assert.assertEquals(3, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.ENDED, invoc.getStatus());
    }

    /**
     * TLEARN-481. Cancel a procedure that's paused.
     *
     * @throws Exception
     */
    @Test(timeOut = 10000)
    public void cancelPaused()
            throws Exception {
        ProcedureInvocation invoc = procDef.invoke(null);
        invoc.startStepping();
        waitUntilPaused(invoc);
        Assert.assertEquals(0, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.PAUSED, invoc.getStatus());
        Assert.assertEquals(13, invoc.getLocation());
        Assert.assertEquals(makeStringId, invoc.getSubAction());

        stepAndSync(invoc);
        Assert.assertEquals(1, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.PAUSED, invoc.getStatus());
        Assert.assertEquals(16, invoc.getLocation());
        Assert.assertEquals(makeStringId, invoc.getSubAction());

        /* Now that we're past the first action in the procedure, cancel it. */
        invoc.cancel();
        invoc.waitUntilFinished();
        Assert.assertEquals(Status.FAILED, invoc.getStatus());
    }

    /**
     * TLEARN-480. Verify that we can step over a nested procedure without
     * stepping into it.
     *
     * @throws Exception
     *             if an error occurs.
     */
    @Test(timeOut = 10000)
    public void stepOverNestedProc()
            throws Exception {
        ProcedureInvocation invoc = superProcDef.invoke(null);
        invoc.startStepping();
        waitUntilPaused(invoc);
        Assert.assertEquals(0, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.PAUSED, invoc.getStatus());
        Assert.assertEquals(29, invoc.getLocation());
        Assert.assertEquals(makeStringId, invoc.getSubAction());

        /* Step over the first action in the super proc. */
        stepAndSync(invoc);
        Assert.assertEquals(1, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.PAUSED, invoc.getStatus());
        Assert.assertEquals(32, invoc.getLocation());
        Assert.assertEquals(procDef.getName(), invoc.getSubAction());

        /* Step over the nested proc. */
        Assert.assertEquals(0, callbackHandler.numTasksEnded());
        stepAndSync(invoc);
        Assert.assertEquals(1, callbackHandler.numTasksEnded());
        Assert.assertEquals(3, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.PAUSED, invoc.getStatus());
        Assert.assertEquals(37, invoc.getLocation());
        Assert.assertEquals(concatId, invoc.getSubAction());

        /* Now step over the last action. */
        stepAndSync(invoc);
        Assert.assertEquals(4, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.ENDED, invoc.getStatus());
    }

    /**
     * Replace the input parameters for a nested action that's being executed by
     * a procedure in stepped mode.
     *
     * @throws Exception
     *             if an error occurs.
     */
    @Test(timeOut = 10000)
    public void replaceArgs()
            throws Exception {
        /*
         * The first part of this test just steps past the actions that have no
         * input parameters.
         */
        ProcedureInvocation invoc = procDef.invoke(null);
        invoc.startStepping();
        waitUntilPaused(invoc);
        Assert.assertEquals(0, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.PAUSED, invoc.getStatus());
        Assert.assertEquals(13, invoc.getLocation());
        Assert.assertEquals(makeStringId, invoc.getSubAction());

        stepAndSync(invoc);
        Assert.assertEquals(1, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.PAUSED, invoc.getStatus());
        Assert.assertEquals(16, invoc.getLocation());
        Assert.assertEquals(makeStringId, invoc.getSubAction());

        stepAndSync(invoc);
        Assert.assertEquals(2, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.PAUSED, invoc.getStatus());
        Assert.assertEquals(19, invoc.getLocation());
        Assert.assertEquals(concatId, invoc.getSubAction());

        /*
         * Getting ready to execute the concat action. Change its input
         * parameters.
         */
        String arg1 = "arg 1";
        String arg2 = "arg 2";
        List<Object> newArgs = new ArrayList<Object>();
        newArgs.add(arg1);
        newArgs.add(arg2);
        invoc.continueStepping(StepCommand.STEP_OVER, newArgs);
        invoc.waitUntilFinished();
        Assert.assertEquals(3, callbackHandler.getSeenActions().size());
        Assert.assertEquals(Status.ENDED, invoc.getStatus());

        /*
         * Verify the correct input parameters were passed. Can't verify the
         * correct output parameter came back, because we're using
         * VerifiableCallbackHandler instead of the DebugExecutor which was
         * written to handle this concat action.
         */
        List<ActionStreamEvent> calledActions = callbackHandler
                .getActionsByType(concatId);
        Assert.assertEquals(1, calledActions.size());
        ActionInvocation calledAction = (ActionInvocation) calledActions.get(0);
        Assert.assertEquals(arg1, calledAction.getValue(0));
        Assert.assertEquals(arg2, calledAction.getValue(1));
    }

    private void stepAndSync(ActionInvocation invoc)
            throws PALException,
            InterruptedException {
        long startTime = System.currentTimeMillis();
        int loc = invoc.getLocation();
        invoc.continueStepping(StepCommand.STEP_OVER);
        while (invoc.getLocation() == loc
                && (System.currentTimeMillis() - startTime) < 10000) {
            Thread.sleep(20);
        }
    }

    private void waitUntilPaused(ActionInvocation invoc)
            throws InterruptedException {
        Listener l = new Listener(invoc);
        invoc.addListener(l);
        l.waitUntilPaused();
    }

    private class Listener
            implements ActionInvocationStatusListener {
        private final ActionInvocation invoc;

        public Listener(ActionInvocation invoc) {
            this.invoc = invoc;
        }

        @Override
        public void error(ErrorInfo error) {
        }

        @Override
        public synchronized void newStatus(Status newStatus) {
            notifyAll();
        }

        public synchronized void waitUntilPaused()
                throws InterruptedException {
            while (invoc.getStatus() != Status.PAUSED) {
                wait();
            }
        }
    }
}
