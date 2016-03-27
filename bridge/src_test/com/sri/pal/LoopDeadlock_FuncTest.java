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

// $Id: LoopDeadlock_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class LoopDeadlock_FuncTest
        extends PALBridgeTestCase {
    private static final int NUM_PROCS = 100;
    private static final int LOOP_SIZE = 100;
    private static final String NAMESPACE = "testApp";
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    private static ProcedureDef bigLoopProc;
    private static List<ProcedureDef> littleProcs;
    private static ActionDef produceList;

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = ActionModels.class.getResource("tuple_test.xml");
        setup(url, NAMESPACE);

        TypeStorage loader = ProcedureLearner.typeStorage;

        // This is our demonstration of actions.
        List<ActionInvocation> demo = new ArrayList<ActionInvocation>();

        // This action produces a list of two elements.
        SimpleTypeName produceListName = (SimpleTypeName) TypeNameFactory
                .makeName("produceList", "1.0", NAMESPACE);
        actionModel.registerExecutor(produceListName, callbackHandler);
        produceList = (ActionDef) actionModel.getType(produceListName);
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        ActionInvocation action = produceList.invoke(null);
        action.setValue(0, list);
        demo.add(action);

        // This action consumes the first list element.
        SimpleTypeName takesOneName = (SimpleTypeName) TypeNameFactory
                .makeName("takesOne", "1.0", NAMESPACE);
        actionModel.registerExecutor(takesOneName, callbackHandler);
        ActionDef takesOne = (ActionDef) actionModel.getType(takesOneName);
        action = takesOne.invoke(null, "a");
        demo.add(action);

        // This action consumes the second list element.
        action = takesOne.invoke(null, "b");
        demo.add(action);

        // Now learn the loop procedure from this demonstration.
        bigLoopProc = learningBridge.learn("loopProc", null,
                demo.toArray(new ActionInvocation[0]));
        loader.putType(bigLoopProc.getName(), bigLoopProc.getXml());

        // Learn all of the little procedures.
        action = takesOne.invoke(null, "foo");
        ProcedureDef proc = learningBridge.learn("littleProc", null, action);
        loader.putType(proc.getName(), proc.getXml());
        littleProcs = new ArrayList<ProcedureDef>();
        for (int i = 0; i < NUM_PROCS; i++) {
            ProcedureDef procCopy = proc
                    .copyAndRename((SimpleTypeName) TypeNameFactory.makeName(
                            "proc" + i, LumenProcedureExecutor.getVersion(),
                            LumenProcedureExecutor.getNamespace()));
            littleProcs.add(procCopy);
            loader.putType(procCopy.getName(), procCopy.getXml());
        }
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * CPAL-228. There's a deadlock between the Lumen Mediator and Lumen itself.
     * The deadlock is caused when the Mediator adds a new task to Lumen while
     * Lumen is requesting the Mediator to start a new execution. To recreate
     * this, we demonstrate a procedure with a big loop in it, and start running
     * that. While it's running, we request a bunch of little procedures to
     * start executing.
     */
    @Test(timeOut = 60000)
    public void loopDeadlock()
            throws Exception {
        List<ProcedureInvocation> invocations = new ArrayList<ProcedureInvocation>();

        // The demonstrated loop procedure only had 2 items in its list. When we
        // play it back, we want a bigger list.
        List<String> returnedList = new ArrayList<String>();
        for (int i = 0; i < LOOP_SIZE; i++) {
            returnedList.add("value" + i);
        }

        // Set up the playback action which will produce this list.
        ActionInvocation action = produceList.invoke(null);
        action.setValue(0, returnedList);
        callbackHandler.addFutureEvent(action);

        // Start the big loop.
        ProcedureInvocation loopInvoc = bigLoopProc.invoke(null);
        loopInvoc.start();
        invocations.add(loopInvoc);

        // Start all the little procs.
        for (ProcedureDef littleProc : littleProcs) {
            ProcedureInvocation littleInvoc = littleProc.invoke(null);
            littleInvoc.start();
            invocations.add(littleInvoc);
        }

        loopInvoc.waitUntilFinished();
        log.info("Finished loop procedure");

        for (ProcedureInvocation invoc : invocations) {
            invoc.waitUntilFinished();
        }

        log.info("Finished little procedures");
    }
}
