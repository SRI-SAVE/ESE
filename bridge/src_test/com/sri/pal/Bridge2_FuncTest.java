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

// $Id: Bridge2_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.net.URL;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This is like Bridge_FuncTest, but it doesn't use a mock executor.
 * @author chris
 */
public class Bridge2_FuncTest
        extends PALBridgeTestCase {
    public static final String NAMESPACE = "testApp";
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");
    private static SimpleTypeName createId;
    private static SimpleTypeName getClassId;
    private static SimpleTypeName destroyObjectId;
    private static SimpleTypeName mapPointId;
    private static SimpleTypeName copyObjectId;
    private static SimpleTypeName moveId;

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.ACTIONS);
        setup(url, NAMESPACE);
        createId = (SimpleTypeName) TypeNameFactory.makeName("createObject",
                "1.0", NAMESPACE);
        getClassId = (SimpleTypeName) TypeNameFactory.makeName("getClass",
                "1.0", NAMESPACE);
        destroyObjectId = (SimpleTypeName) TypeNameFactory.makeName(
                "destroyObject", "1.0", NAMESPACE);
        mapPointId = (SimpleTypeName) TypeNameFactory.makeName("mapPoint",
                "1.0", NAMESPACE);
        copyObjectId = (SimpleTypeName) TypeNameFactory.makeName("copyObject",
                "1.0", NAMESPACE);
        moveId = (SimpleTypeName) TypeNameFactory.makeName("move", 
                "1.0", NAMESPACE);
        actionModel.registerExecutor(createId, callbackHandler);
        actionModel.registerExecutor(getClassId, callbackHandler);
        actionModel.registerExecutor(destroyObjectId, callbackHandler);
        actionModel.registerExecutor(mapPointId, callbackHandler);
        actionModel.registerExecutor(copyObjectId, callbackHandler);
        actionModel.registerExecutor(moveId, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void recordAndPlayback() throws Exception {
        Learner lBridge = palBridge.getLearner();
        ActionInvocation[] actions = new ActionInvocation[3];

        ActionDef createActDef = (ActionDef) actionModel.getType(createId);
        ActionInvocation action0 = createActDef.invoke(null);
        action0.setValue(0, "7");
        action0.setValue(1, "foo");
        actions[0] = action0;

        ActionDef getClassActDef = (ActionDef) actionModel.getType(getClassId);
        ActionInvocation action1 = getClassActDef.invoke(null);
        action1.setValue(0, "7");
        StructDef structDef = (StructDef) actionModel
                .getType(TypeNameFactory.makeName("friendlyObject", "1.0",
                        NAMESPACE));
        Struct tupleValue = structDef.newInstance();
        tupleValue.setValue(0, "7");
        tupleValue.setValue(1, "friendlyobject arg");
        action1.setValue("method", tupleValue);
        actions[1] = action1;

        ActionDef destroyActDef = (ActionDef) actionModel.getType(destroyObjectId);
        assertNotNull("actionDef failed", destroyActDef);
        ActionInvocation action2 = destroyActDef.invoke(null);
        assertNotNull("action2 failed", action2);
        action2.setValue("object", "7");
        actions[2] = action2;

        ProcedureDef task = lBridge.learn("bridgeRecordAndPlayback", null, actions);
        assertNotNull("task failed", task);
        log.info("Task string: " + task.getSource());

        ProcedureInvocation procedure = task.invoke(null);
        procedure.start();
        procedure.waitUntilFinished();
        Thread.sleep(1000);
        VerifiableCallbackHandler callbackHandler = ProcedureLearner.getCallbackHandler();
        assertTrue("Create was never called", callbackHandler.sawAction(createId));
        assertTrue("Destroy was never called", callbackHandler.sawAction(getClassActDef.getName()));
        assertTrue("Start was never called", callbackHandler.wasStartCalled());
        assertTrue("End was never called", callbackHandler.isTaskEnded());

        // Make sure output parameters of the whole procedure are being set.
        ActionDef def = procedure.getDefinition();
        assertEquals("Wrong number of output parameters", 2, def.size() - def.numInputParams());
        for(int i = def.numInputParams(); i < def.size(); i++) {
            Object paramObj = procedure.getValue(i);
            assertNotNull("Output param " + i + " was null", paramObj);
        }
    }

    @Test
    public void asyncLearn()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(copyObjectId);
        ActionInvocation action = actDef.invoke(null, "foo");
        action.setValue(1, "bar");
        Learner learner = palBridge.getLearner();
        SynchronousCallbackHandler<ProcedureDef> cb = new SynchronousCallbackHandler<ProcedureDef>();
        learner.learn(cb, "asyncLearn", null, null, action);
        ProcedureDef proc = cb.waitForResult();
        ErrorInfo err = cb.getError();
        Assert.assertNull(err);
        Assert.assertNotNull(proc);
    }

    @Test
    public void testCasting() throws Exception {
        Learner lBridge = palBridge.getLearner();

        float x = 7.0F;
        ActionDef actDef = (ActionDef) actionModel.getType(mapPointId);
        ActionInvocation action = actDef.invoke(null);
        action.setValue(0, x);

        ProcedureDef task = lBridge.learn("testCasting", null, action);
        assertNotNull("task failed", task);

        Object thing = task.getDefaultValue(0);

        assertNotNull("Default value of param is null", thing);
        assertEquals("Returned object should be a float and is not", Float.class, thing.getClass());
    }

    @Test
    public void testBenign()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(moveId);
        Assert.assertTrue(actDef.isBenign());
    }
}
