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

// $Id: Task_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.bridge;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.rmi.RemoteException;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.Bridge;
import com.sri.pal.Bridge2_FuncTest;
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

public class Task_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory.getLogger("TestSourceLogger");

    private static final String TEST_DIR = "../test/data/serialized";
    private static final String ACTIONS_FILE = ActionModels.ACTIONS;
    private static final String NAMESPACE = Bridge2_FuncTest.NAMESPACE;

    @BeforeClass
    public static void setup() throws RemoteException, PALException {
        setup(ActionModels.class.getResource(ACTIONS_FILE), NAMESPACE);

        SimpleTypeName name1 = (SimpleTypeName) TypeNameFactory.makeName(
                "createObject", "1.0", NAMESPACE);
        actionModel.registerExecutor(name1, callbackHandler);
        SimpleTypeName name2 = (SimpleTypeName) TypeNameFactory.makeName(
                "getClass", "1.0", NAMESPACE);
        actionModel.registerExecutor(name2, callbackHandler);
        SimpleTypeName name3 = (SimpleTypeName) TypeNameFactory.makeName(
                "destroyObject", "1.0", NAMESPACE);
        actionModel.registerExecutor(name3, callbackHandler);
        SimpleTypeName name4 = (SimpleTypeName) TypeNameFactory.makeName(
                "copyObject", "1.0", NAMESPACE);
        actionModel.registerExecutor(name4, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test(timeOut = 30000)
    public void checkSerializedForms()
            throws Exception {
        Bridge palBridge = ProcedureLearner.palBridge;
        ProcedureExecutor sparkExec = palBridge.getPALExecutor();

        File testDir = new File(TEST_DIR);
        File[] files = testDir.listFiles();
        for (File file : files) {
            if (!file.getName().startsWith("task")) {
                log.warn("Ignoring file: " + file);
            } else {
                log.info("Checking file: " + file);
                String taskStr = ProcedureLearner.readWholeFile(file);
                ProcedureDef task = sparkExec.load(taskStr);
                log.debug("taskStr = " + taskStr);
                assertNotNull("Unable to create initial task", task);
                log.info("Successfully reconstructed task object from file");
                String taskStr2 = task.getSource();
                log.debug("taskStr2 = " + taskStr2);
                ProcedureDef task2 = sparkExec.load(taskStr2);
                assertEquals("Serialized tasks are not equal", task, task2);

                ProcedureInvocation proc1 = task2.invoke(null);
                proc1.start();
                proc1.waitUntilFinished();
                /*
                 * Give it a chance to deliver the "done" message to the
                 * callback handler.
                 */
                Thread.sleep(1000);
                VerifiableCallbackHandler vch = ProcedureLearner.callbackHandler;
                assertTrue("Task1 execution didn't end", vch.isTaskEnded());
                assertTrue("Task1 execution failed", vch.wasTaskSuccessful());

                vch.reset();
                ProcedureInvocation proc2 = task2.invoke(null);
                proc2.start();
                proc2.waitUntilFinished();
                Thread.sleep(1000);
                assertTrue("Task2 execution didn't end", vch.isTaskEnded());
                assertTrue("Task2 execution failed", vch.wasTaskSuccessful());
            }
        }
    }

    @Test(timeOut = 30000)
    public void serializeStrangeChars()
            throws Exception {
        String strangeString = "foo &(bar)";
        SimpleTypeName actId = (SimpleTypeName) TypeNameFactory.makeName(
                "copyObject", "1.0", NAMESPACE);
        ActionInvocation action = ((ActionDef) actionModel.getType(actId))
                .bindAll(null, strangeString, strangeString);
        ProcedureDef task1 = ProcedureLearner.learnAndInvokeProcedure(action,
                "serializeStrange1");

        ProcedureLearner.invokeTask(task1);
        String serializedTask = task1.getSource();
        ProcedureExecutor sparkExec = palBridge.getPALExecutor();
        ProcedureDef task2 = sparkExec.load(serializedTask);
        ProcedureLearner.invokeTask(task2);
    }
}
