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

// $Id: ComposableProcedures_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.events;

import static com.sri.pal.ProcedureLearner.NAMESPACE;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.sri.pal.AbstractActionDef;
import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.LumenProcedureDef;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.VerifiableCallbackHandler;
import com.sri.pal.bridge.SerializedNestedTasks_FuncTest;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ComposableProcedures_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    private static SimpleTypeName actionId;

    @BeforeClass
    public static void initialize() throws Exception {
        setup();

        actionId = (SimpleTypeName) TypeNameFactory.makeName("action202",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(actionId, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * This method creates the source for the three tasks used by
     * {@link SerializedNestedTasks_FuncTest}. As a test, only minimal sanity
     * checking is done here.
     */
    @Test
    public void makeNestedProcs()
            throws Exception {
        List<ActionInvocation> actions = new Vector<ActionInvocation>();
        ActionInvocation action1 = ((ActionDef) actionModel.getType(actionId)).bindAll(null, "entity", "attr",
                                                                    "value");
        actions.add(action1);

        ProcedureDef task1 = ProcedureLearner.learnAndInvokeProcedure(actions,
                "composableSub1");

        ProcedureDef task2 = ProcedureLearner.learnAndInvokeProcedure(actions,
                "composableSub2");

        actions.clear();

        action1 = task1.invoke(null);
        for(int i = 0; i < task1.size(); i++) {
            action1.setValue(i, "bogus value");
        }
        actions.add(action1);

        ActionInvocation action2 = task2.invoke(null);
        for(int i = 0; i < task2.size(); i++) {
            action1.setValue(i, "bogus value");
        }
        actions.add(action2);

        VerifiableCallbackHandler handler = ProcedureLearner
                .getCallbackHandler();
        handler.reset();

        ProcedureDef parentTask = ProcedureLearner.learnAndInvokeProcedure(actions,
                "composableSuper1");

        log.info("Nested task 1: " + task1.getSource());
        log.info("Nested task 2: " + task2.getSource());
        log.info("Parent task: " + parentTask.getSource());
    }

    /**
     * Demonstrate a procedure which calls another procedure. Make sure
     * everything gets called the right number of times when we execute that
     * super-procedure.
     */
    @Test
    public void composableProcedure1()
            throws Exception {
        ActionInvocation action1 = ((ActionDef) actionModel.getType(actionId)).bindAll(null, "entity", "attr", "value");

        ProcedureDef task1 = ProcedureLearner.learnAndInvokeProcedure(action1,
                "composableSub1");

        action1 = task1.invoke(null);
        for(int i = 0; i < task1.size(); i++) {
            action1.setValue(i, "bogus value");
        }

        VerifiableCallbackHandler handler = ProcedureLearner.getCallbackHandler();
        handler.reset();

        ProcedureLearner.learnAndInvokeProcedure(action1, "composableSuper1");

        assertTrue("action callback was not received", handler.sawAction(actionId));
        assertEquals("Sub-procedure was not called", 1, handler.subProceduresCalled());
        assertEquals("Wrong number of tasks ended", 2, handler.numTasksEnded());
        List<ActionStreamEvent> seenActions = handler.getSeenActions();
        for(ActionStreamEvent ai : seenActions) {
            assertEquals(actionId, ai.getDefinition().getName());
        }
        assertEquals(1, seenActions.size());
    }

    @Test
    public void composableProcedure2()
            throws Exception {
        VerifiableCallbackHandler handler = ProcedureLearner.getCallbackHandler();
        handler.reset();

        List<ActionInvocation> actions = new Vector<ActionInvocation>();
        ActionInvocation action1 = ((ActionDef) actionModel.getType(actionId)).bindAll(null, "entity", "attr", "value");
        actions.add(action1);

        ProcedureDef task1 = ProcedureLearner.learnProcedure(actions, "composableSub2");

        List<Object> args = new Vector<Object>();
        for(int i = 0; i < task1.size(); i++) {
            args.add("bogus value for " + i);
        }
        ProcedureInvocation proc = task1.invoke(null, args.toArray(new Object[0]));
        proc.start();
        proc.waitUntilFinished();

        actions.clear();
        actions.add(proc);

        ProcedureLearner.learnAndInvokeProcedure(actions, "composableSuper2");

        assertTrue("action callback was not received", handler.sawAction(actionId));
        assertEquals("Sub-procedure was not called", 1, handler.subProceduresCalled());
        assertEquals("Wrong number of tasks ended", 3, handler.numTasksEnded());
    }

    /**
     * Procedure calls a nested procedure, then executes some other (primitive)
     * actions.
     */
    @Test
    public void nestedProcBeforeAction()
            throws Exception {
        // Demonstrate a primitive action to learn the nested procedure.
        ActionInvocation action1 = ((ActionDef) actionModel.getType(actionId)).bindAll(null, "entity", "attr", "value");

        // Learn the nested procedure.
        ProcedureDef task1 = ProcedureLearner.learnAndInvokeProcedure(action1,
                "nestedProcBeforeAction");

        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();

        // Demonstrate running the nested procedure to learn the super procedure.
        ProcedureInvocation taskInvoc1 = task1.invoke(null);
        for(int i = 0; i < task1.size(); i++) {
            taskInvoc1.setValue(i, "bogus value");
        }
        actions.add(taskInvoc1);

        // Demonstrate the primitive action again as part of the super procedure.
        actions.add(action1);

        // Reset the callback handler so we get accurate info.
        VerifiableCallbackHandler handler = ProcedureLearner.getCallbackHandler();
        handler.reset();

        // Learn the super procedure.
        ProcedureLearner.learnAndInvokeProcedure(actions, "superProcBeforeAction");

        // Make sure everything ran that should have.
        assertTrue("action callback was not received", handler.sawAction(actionId));
        assertEquals("Sub-procedure was not called", 1, handler.subProceduresCalled());
        assertEquals("Wrong number of tasks ended", 2, handler.numTasksEnded());
        List<ActionStreamEvent> seenActions = handler.getSeenActions();
        for(ActionStreamEvent ai : seenActions) {
            assertEquals(actionId, ai.getDefinition().getName());
        }
        assertEquals(2, seenActions.size());
    }

    @Test
    public void nestedTasks()
            throws Exception {
        List<ActionInvocation> actions = new Vector<ActionInvocation>();
        ActionInvocation action = ((ActionDef) actionModel.getType(actionId)).bindAll(null, "entity", "attr",
                                                                   "value");
        actions.add(action);

        ProcedureDef subTask1 = ProcedureLearner.learnProcedure(actions,
                "nestedTasksSub1");

        ProcedureDef subTask2 = ProcedureLearner.learnProcedure(actions,
                "nestedTasksSub2");

        // Now demonstrate a super-proc which calls the above two.
        actions.clear();
        ProcedureInvocation proc1 = subTask1.invoke(null);
        ProcedureInvocation proc2 = subTask2.invoke(null);
        proc1.start();
        proc2.start();
        proc1.waitUntilFinished();
        proc2.waitUntilFinished();
        actions.add(proc1);
        actions.add(proc2);
        LumenProcedureDef superTask = (LumenProcedureDef) ProcedureLearner
                .learnProcedure(actions, "nestedTasksSuper");

        Set<AbstractActionDef> nestedTasks = superTask.getNestedActions();
        assertEquals("Wrong number of nested Tasks", 2, nestedTasks.size());
        assertTrue(nestedTasks.contains(subTask1));
        assertTrue(nestedTasks.contains(subTask2));
    }

    // https://jira.esd.sri.com/browse/CPAL-8
    @Test
    public void doubleInvocation()
            throws Exception {
        List<ActionInvocation> actions = new Vector<ActionInvocation>();
        ActionInvocation action1 = ((ActionDef) actionModel.getType(actionId)).bindAll(null, "entity", "attr",
                                                                    "value");
        actions.add(action1);

        ProcedureDef task1 = ProcedureLearner.learnAndInvokeProcedure(actions,
                "doubleSub");

        actions.clear();
        action1 = task1.invoke(null);
        for(int i = 0; i < task1.size(); i++) {
            action1.setValue(i, "bogus value");
        }
        actions.add(action1);

        ActionInvocation action2 = task1.invoke(null);
        for(int i = 0; i < task1.size(); i++) {
            action2.setValue(i, "other bogus value");
        }
        actions.add(action2);

        VerifiableCallbackHandler handler = ProcedureLearner
                .getCallbackHandler();
        handler.reset();

        ProcedureLearner.learnAndInvokeProcedure(actions, "doubleSuper");

        assertTrue("action callback was not received", handler
                .sawAction(actionId));
        assertEquals("Sub-procedure was not called", 2, handler
                .subProceduresCalled());
        assertEquals("Wrong number of tasks ended", 3, handler.numTasksEnded());
    }
}
