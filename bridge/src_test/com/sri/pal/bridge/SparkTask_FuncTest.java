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

// $Id: SparkTask_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.bridge;

import java.rmi.RemoteException;

import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureExecutor;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SparkTask_FuncTest
        extends PALBridgeTestCase {
    private static final String AM_NAME = ActionModels.ACTIONS;
    private static final String NAMESPACE = "SparkTest";
    private static final String PROC1NAME = "lumen^0.4^newName";
    private static final String PROC1FILE = "SparkTask_proc1.xml";
    private static final String PROC2FILE = "SparkTask_proc2.xml";

    @BeforeClass
    public static void setup() throws RemoteException, PALException {
        setup(ActionModels.class.getResource(AM_NAME), NAMESPACE);
        SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName(
                "createObject", "1.0", NAMESPACE);
        actionModel.registerExecutor(name, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void renameSerializedProc()
            throws Exception {
        // When we load a serialized SparkTask, we can pass in a new lumen name
        // to be given to the corresponding procedure. In order to test this, we
        // first deserialize a simple procedure and rename it. Then we
        // deserialize a second procedure which calls the first.
        ProcedureExecutor sparkExec = palBridge.getPALExecutor();

        String proc1Src = ProcedureLearner.readWholeFile(getClass().getResource(PROC1FILE));
        String proc2Src = ProcedureLearner.readWholeFile(getClass().getResource(PROC2FILE));

        ProcedureDef task1 = sparkExec.load(proc1Src);
        task1 = task1.copyAndRename((SimpleTypeName) TypeNameFactory
                .makeName(PROC1NAME));
        actionModel.storeType(task1.getName(), task1);
        ProcedureDef task2 = sparkExec.load(proc2Src);

        ProcedureLearner.invokeTask(task1);
        ProcedureLearner.invokeTask(task2);
    }
}
