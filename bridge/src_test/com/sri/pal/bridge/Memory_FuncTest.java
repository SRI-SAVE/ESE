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

// $Id: Memory_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.bridge;

import static org.testng.AssertJUnit.fail;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionModel;
import com.sri.pal.Bridge;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.VerifiableCallbackHandler;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Memory_FuncTest
        extends PALTestCase {
    private static final String ACTIONS_FILE = ActionModels.ACTIONS;
    private static final String NAMESPACE = "testApp";

    private static SimpleTypeName actionId;
    private static Bridge palBridge;
    private static ActionModel actionModel;
    private static VerifiableCallbackHandler callbackHandler;

    @BeforeClass
    public static void setup()
            throws Exception {
        Bridge.startPAL();
        palBridge = Bridge.newInstance("mft");
        ProcedureLearner.setStorage(Memory_FuncTest.class, palBridge);
        actionModel = palBridge.getActionModel();
        URL url = ActionModels.class.getResource(ACTIONS_FILE);
        callbackHandler = new VerifiableCallbackHandler();
        palBridge.addActionListener(callbackHandler);
        actionModel.load(url, NAMESPACE);

        ProcedureLearner.palBridge = palBridge;
        ProcedureLearner.learningBridge = palBridge.getLearner();
        ProcedureLearner.actionModel = actionModel;
        ProcedureLearner.callbackHandler = callbackHandler;

        actionId = (SimpleTypeName) TypeNameFactory.makeName("createObject",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(actionId, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * The PAL system is supposed to garbage collect unused SparkTask objects.
     * When it does so, it should undefine the associated type and remove the
     * action from LAPDOG and lumen.
     * <p>
     * https://jira.esd.sri.com/browse/TLEARN-14
     *
     * @throws Exception
     */
    @Test
    public void removeOneUnused()
            throws Exception {
        ProcedureDef task = learnTask("removeOneUnused1");
        Reference<ProcedureDef> ref = new WeakReference<ProcedureDef>(task);
        // LearningBridge keeps a reference to the most recently learned task,
        // so learn another.
        task = learnTask("removeOneUnused2");
        for (int i = 0; i < 60; i++) {
            if (ref.get() == null) {
                // Success.
                return;
            }
            System.gc();
            Thread.sleep(1000);
        }
        fail("Unused task was never garbage collected.");
    }

    private ProcedureDef learnTask(String name)
            throws Exception {
        ActionInvocation action = ((ActionDef) actionModel.getType(actionId)).invoke(null);
        ProcedureDef task = ProcedureLearner.learnAndInvokeProcedure(action, name);
        return task;
    }
}
