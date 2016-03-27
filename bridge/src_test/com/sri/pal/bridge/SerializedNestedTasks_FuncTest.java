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

// $Id: SerializedNestedTasks_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.bridge;

import java.io.File;

import com.sri.pal.Bridge;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureExecutor;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.upgrader.MemoryTypeStorage;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SerializedNestedTasks_FuncTest
        extends PALBridgeTestCase {
    private static final String TEST_DIR = "../test/data/nestedTasks";
    private static final String SUB1 = "sub1.xml";
    private static final String SUB3 = "sub3.xml";
    private static final String SUB3_NAME = "lumen^0.4^test-composableSub3";
    private static final String SUB4 = "sub4.xml";
    private static final String SUB4_NAME = "lumen^0.4^test-composableSub4";
    private static final String SUPER2 = "super2.xml";
    private static final String SUPER2_NAME = "lumen^0.4^test-composableSuper2-1257449315671";
    private static MemoryTypeStorage loader;

    @BeforeClass
    public static void initialize()
            throws Exception {
        setupNoStorage();
        loader = new MemoryTypeStorage();
        Assert.assertTrue(palBridge.setTypeStorage(loader));
        ProcedureLearner.loadActionModel();

        SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName(
                "action202", "1.0", "TESTNS");
        actionModel.registerExecutor(name, callbackHandler);
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * Try loading a task that depends on other tasks which haven't been loaded
     * at all. In order to execute the super-task, we'll need to use the
     * ActionLoader.
     */
    @Test
    public void actionLoader()
            throws Exception {
        Bridge palBridge = ProcedureLearner.palBridge;

        File testDir = new File(TEST_DIR);
        File sub1File = new File(testDir, SUB3);
        File sub2File = new File(testDir, SUB4);
        File superFile = new File(testDir, SUPER2);
        SimpleTypeName sub1Name = (SimpleTypeName) TypeNameFactory
                .makeName(SUB3_NAME);
        SimpleTypeName sub2Name = (SimpleTypeName) TypeNameFactory
                .makeName(SUB4_NAME);
        SimpleTypeName superName = (SimpleTypeName) TypeNameFactory
                .makeName(SUPER2_NAME);

        String sub1TaskStr = ProcedureLearner.readWholeFile(sub1File);
        String sub2TaskStr = ProcedureLearner.readWholeFile(sub2File);
        String superTaskStr = ProcedureLearner.readWholeFile(superFile);

        ProcedureExecutor sparkExec = palBridge.getPALExecutor();
        loader.putType(sub1Name, sub1TaskStr);
        loader.putType(sub2Name, sub2TaskStr);
        loader.putType(superName, superTaskStr);

        ProcedureDef superTask = sparkExec.load(superTaskStr);

        ProcedureLearner.invokeTask(superTask);
    }

    /**
     * At one time, CTR-S didn't specify default values in the procedure
     * signature. That's been changed. This test checks our behavior when faced
     * with an old-format procedure.
     */
    @Test
    public void noDefaults()
            throws Exception {
        Bridge palBridge = ProcedureLearner.palBridge;

        File testDir = new File(TEST_DIR);
        File sub1File = new File(testDir, SUB1);
        String sub1TaskStr = ProcedureLearner.readWholeFile(sub1File);

        ProcedureExecutor sparkExec = palBridge.getPALExecutor();
        ProcedureDef procDef = sparkExec.load(sub1TaskStr);
        Assert.assertNull(procDef.getDefaultValue(0));
    }
}
