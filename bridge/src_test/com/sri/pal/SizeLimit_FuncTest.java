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

// $Id: SizeLimit_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.fail;

import java.net.URL;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SizeLimit_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = "testApp";
    private static final int LIMIT = 10 * 1024;
    private static String bigString;
    private static ActionDef createActDef;
    private static ActionDef destroyActDef;

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.ACTIONS);
        setup(url, NAMESPACE);
        Bridge.setMaxStringSize(LIMIT);
        bigString = makeBigString(LIMIT + 1);

        SimpleTypeName createId = (SimpleTypeName) TypeNameFactory.makeName(
                "createObject", "1.0", NAMESPACE);
        createActDef = (ActionDef) actionModel.getType(createId);
        actionModel.registerExecutor(createId, callbackHandler);
        SimpleTypeName destroyId = (SimpleTypeName) TypeNameFactory.makeName(
                "destroyObject", "1.0", NAMESPACE);
        destroyActDef = (ActionDef) actionModel.getType(destroyId);
        actionModel.registerExecutor(destroyId, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void bigDemonstration()
            throws Exception {
        Learner learner = palBridge.getLearner();

        ActionInvocation action = createActDef.invoke(null);
        action.setValue(0, bigString);

        try {
            learner.learn("bigDemo", null, action);
        } catch (PALSizeException e) {
            // It worked.
            return;
        }

        fail();
    }

    @Test
    public void bigProcArg()
            throws Exception {
        Learner learner = palBridge.getLearner();

        ActionInvocation action = destroyActDef.invoke(null);
        action.setValue(0, "foo");
        ProcedureDef procDef = learner.learn("bigProcArg", null, action);
        assertEquals(1, procDef.numInputParams());

        ProcedureInvocation invoc = procDef.invoke(null, bigString);
        try {
            invoc.start();
        } catch (PALSizeException e) {
            // It worked.
            return;
        }

        fail();
    }

    @Test
    public void bigActionResult()
            throws Exception {
        Learner learner = palBridge.getLearner();

        ActionInvocation demoAction = createActDef.invoke(null);
        demoAction.setValue(0, "foo");
        demoAction.setValue(1, "bar");
        ProcedureDef procDef = learner.learn("bigActionResult", null,
                demoAction);
        assertEquals(2, procDef.size());
        assertEquals(1, procDef.numInputParams());

        VerifiableCallbackHandler vch = callbackHandler;
        ActionInvocation replayAction = createActDef.invoke(null);
        replayAction.setValue(0, "foo");
        replayAction.setValue(1, bigString);
        vch.addFutureEvent(replayAction);

        ProcedureInvocation invoc = procDef.invoke(null);
        invoc.start();
        invoc.waitUntilFinished();

        assertFalse(vch.wasTaskSuccessful());
    }

    private static String makeBigString(int size) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < size; i++) {
            buffer.append("A");
        }
        return buffer.toString();
    }
}
