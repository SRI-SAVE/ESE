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

package com.sri.pal.events;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureExecutor;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.VerifiableCallbackHandler;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Valerie Wagner
 *         Date: Nov 5, 2008
 */
public class LoopLearning_FuncTest extends PALBridgeTestCase {
    private static final String TEST_FILE = "tuple_test.xml";
    protected static final String NAMESPACE = "testApp";
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    private static SimpleTypeName produceListName;
    private static SimpleTypeName bodyAction;
    private static SimpleTypeName takeGiveName;
    private static SimpleTypeName takeBName;
    private static SimpleTypeName consumeListName;

    @BeforeClass
    public static void setup() throws RemoteException, PALException {
        setup(ActionModels.class.getResource(TEST_FILE), NAMESPACE);

        produceListName = (SimpleTypeName) TypeNameFactory.makeName(
                "produceList", "1.0", NAMESPACE);
        consumeListName = (SimpleTypeName) TypeNameFactory.makeName(
                "consumeList", "1.0", NAMESPACE);
        bodyAction = (SimpleTypeName) TypeNameFactory.makeName("takesOne",
                "1.0", NAMESPACE);
        takeGiveName = (SimpleTypeName) TypeNameFactory.makeName(
                "takesOneGivesOne", "1.0", NAMESPACE);
        takeBName = (SimpleTypeName) TypeNameFactory.makeName("takesOneBType",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(produceListName, callbackHandler);
      actionModel.registerExecutor(bodyAction, callbackHandler);
        actionModel.registerExecutor(takeGiveName, callbackHandler);
        actionModel.registerExecutor(takeBName, callbackHandler);
        actionModel.registerExecutor(consumeListName, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    // MTRS-106
    @Test(timeOut = 30000)
    public void learnLoopAndSerialize()
            throws Exception {
        ProcedureDef proc = loopTest1(null);
        assertTrue(proc.getSource().contains("forall"));
    }

    public static ProcedureDef loopTest1(Properties learnProps)
            throws Exception {
        List<ActionInvocation> actions = new Vector<ActionInvocation>();
        List<String> iterList = new Vector<String>();
        iterList.add("a");
        iterList.add("b");
        iterList.add("c");

        ActionInvocation action = ((ActionDef) actionModel.getType(produceListName)).bindAll(null, iterList);
        actions.add(action);

        for (String listItem : iterList) {
            action = ((ActionDef) actionModel.getType(bodyAction)).bindAll(null, listItem);
            actions.add(action);
        }

        ProcedureDef task1 = ProcedureLearner.learnAndInvokeProcedure(actions, "learnLoopAndSerialize", learnProps,
                null);
        String taskStr = task1.getSource();
        ProcedureExecutor sparkExec = palBridge.getPALExecutor();
        ProcedureDef task2 = sparkExec.load(taskStr);
        ProcedureInvocation proc = task2.invoke(null);
        proc.start();
        proc.waitUntilFinished();
        assertTrue("Task execution failed", ProcedureLearner.callbackHandler.wasTaskSuccessful());
        return task2;
    }

    /**
     * This demonstrates a loop where a list [a,b,c] is produced outside the
     * loop, then [a], [b], and [c] are consumed inside the loop. This is
     * different from consuming a, b, and c inside the loop.
     *
     * @throws Exception
     */
    @Test
    public void loopWithListInputs()
            throws Exception {
        List<ActionInvocation> actions = new Vector<ActionInvocation>();
        List<String> iterList = new ArrayList<String>();
        iterList.add("a");
        iterList.add("b");
        iterList.add("c");

        ActionInvocation action = ((ActionDef) actionModel.getType(produceListName))
                .bindAll(null, iterList);
        actions.add(action);

        for (String listItem : iterList) {
            List<String> singletonList = new ArrayList<String>();
            singletonList.add(listItem);
            action = ((ActionDef) actionModel.getType(consumeListName)).bindAll(
                    null, singletonList);
            actions.add(action);
        }

        ProcedureDef task1 = ProcedureLearner.learnAndInvokeProcedure(actions,
                "loopWithListInputs");
        assertTrue("Task execution failed", ProcedureLearner.callbackHandler
                .wasTaskSuccessful());
        assertTrue(task1.getSource().contains("forall"));
    }

    @Test
    public void learnALoopWithOutputs() throws PALException, RemoteException {
        List<String> myList = new ArrayList<String>();
        myList.add("d");
        myList.add("e");
        myList.add("f");

        List<String> myOtherList = new ArrayList<String>();
        myOtherList.add("x");
        myOtherList.add("y");
        myOtherList.add("z");

        List<ActionInvocation> actions = new Vector<ActionInvocation>();
        actions.add(((ActionDef) actionModel.getType(produceListName)).bindAll(
                null, myList));
        actions.add(((ActionDef) actionModel.getType(takeGiveName)).bindAll(
                null, myList.get(0), myOtherList.get(0)));
        actions.add(((ActionDef) actionModel.getType(takeGiveName)).bindAll(
                null, myList.get(1), myOtherList.get(1)));
        actions.add(((ActionDef) actionModel.getType(takeGiveName)).bindAll(
                null, myList.get(2), myOtherList.get(2)));
        actions.add(((ActionDef) actionModel.getType(takeBName)).bindAll(null,
                myOtherList.get(0)));

        ProcedureDef task = ProcedureLearner.learnAndInvokeProcedure(actions, "testLearnALoop");
        log.info("task = " + task);

        assertNotNull("Learned task is null", task);
        assertTrue("Learned task does not contain 'forall' loop", task.getSource().contains("forall"));
    }

    @Test
    public void loopInnerProcedure() throws PALException, RemoteException {
        List<String> myList = new ArrayList<String>();
        myList.add("d");
        myList.add("e");
        myList.add("f");

        List<String> myOtherList = new ArrayList<String>();
        myOtherList.add("x");
        myOtherList.add("y");
        myOtherList.add("z");

        ActionInvocation subAction = ((ActionDef) actionModel
                .getType(takeGiveName)).bindAll(null, myList.get(0),
                myOtherList.get(0));
        ProcedureDef subTask = ProcedureLearner.learnProcedure(subAction, "LoopInnerProcSub");

        List<ActionInvocation> actions = new Vector<ActionInvocation>();
        actions.add(((ActionDef) actionModel.getType(produceListName)).bindAll(
                null, myList));
        actions.add(subTask.bindAll(null, myList.get(0), myOtherList.get(0)));
        actions.add(subTask.bindAll(null, myList.get(1), myOtherList.get(1)));
        actions.add(subTask.bindAll(null, myList.get(2), myOtherList.get(2)));
        actions.add(((ActionDef) actionModel.getType(takeBName)).bindAll(null,
                myOtherList.get(0)));

        ProcedureDef task = ProcedureLearner.learnProcedure(actions, "LoopOuterProc");
        log.info("task = " + task);

        assertNotNull("Learned task is null", task);
        assertTrue("Learned task does not contain 'forall' loop:" + task.getSource(), task.getSource().contains("forall"));

        VerifiableCallbackHandler callbackHandler = ProcedureLearner.callbackHandler;
        callbackHandler.reset();
        ProcedureInvocation proc = task.invoke(null);
        proc.start();
        proc.waitUntilFinished();

        assertTrue("Task wasn't successful", callbackHandler.wasTaskSuccessful());
        assertFalse("A task encountered an error", callbackHandler.wasError());
        // ProcedureLearner synthesizes a 2-item list instead of the 3-item list
        // we demonstrated. So we'll only get 2 sub-procs called.
        assertEquals("Wrong number of sub-procedures", 2, callbackHandler.subProceduresCalled());

        // It would be nice to do this test also, but there's no easy way to
        // make ITL be synchronous enough to avoid race conditions for procedure
        // termination.
        //        assertEquals("Wrong number of tasks ended", 3, callbackHandler.numTasksEnded());
    }
}
