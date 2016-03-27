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

// $Id: ActionException_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.bridge;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.net.URL;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.Bridge;
import com.sri.pal.Learner;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.actionmodels.DebugExecutor;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ActionException_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = "foo";

    private static DebugExecutor executor;
    private static Learner learner;
    private static ProcedureDef proc;

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.DEBUG);
        Bridge.startPAL();
        palBridge = Bridge.newInstance("aeft");
        ProcedureLearner.setStorage(ActionException_FuncTest.class, palBridge);
        actionModel = palBridge.getActionModel();
        executor = new DebugExecutor();
        executor.setAsynchronous(false);
        actionModel.load(url, NAMESPACE);
        learner = palBridge.getLearner();

        SimpleTypeName failName = (SimpleTypeName) TypeNameFactory.makeName(
                "fail", "1.0", NAMESPACE);
        actionModel.registerExecutor(failName, executor);
        ActionDef failDef = (ActionDef) actionModel.getType(failName);
        ActionInvocation action = failDef.invoke(null, "succeed");
        proc = learner.learn("throwException", null, action);
    }

    @AfterClass
    public static void shutdown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test(timeOut = 20000)
    public void throwException()
            throws Exception {
        ProcedureInvocation procInvoc = proc.invoke(null, "error");
        procInvoc.start();
        procInvoc.waitUntilFinished();
        assertEquals(Status.FAILED, procInvoc.getStatus());
        assertNotNull(procInvoc.getError());
    }

    @Test(timeOut = 180000)
    public void exceptionDeadlock()
            throws Exception {
        for (int i = 0; i < 1000; i++) {
            throwException();
        }
    }
}
