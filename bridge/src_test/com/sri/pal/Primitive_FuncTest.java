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

// $Id: Primitive_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.ICalDateTime;
import com.sri.pal.common.ICalDuration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test that we can handle our predefined primitive types when we demonstrate
 * actions that use them.
 */
public class Primitive_FuncTest
        extends PALBridgeTestCase {
    private static final String ACTION_MODEL = ActionModels.PRIMITIVES;
    private static final String NS = "pft";
    private static final String VERS = "1.0";

    private static ActionDef takeIntegerDef;
    private static ActionDef takeRealDef;
    private static ActionDef takeStringDef;
    private static ActionDef takeBooleanDef;
    private static ActionDef takeTimestampDef;
    private static ActionDef takeDurationDef;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTION_MODEL), NS);

        SimpleTypeName takeIntegerName = (SimpleTypeName) TypeNameFactory
                .makeName("takeInteger", VERS, NS);
        takeIntegerDef = (ActionDef) actionModel.getType(takeIntegerName);
        actionModel.registerExecutor(takeIntegerName, callbackHandler);

        SimpleTypeName takeRealName = (SimpleTypeName) TypeNameFactory
                .makeName("takeReal", VERS, NS);
        takeRealDef = (ActionDef) actionModel.getType(takeRealName);
        actionModel.registerExecutor(takeRealName, callbackHandler);

        SimpleTypeName takeStringName = (SimpleTypeName) TypeNameFactory
                .makeName("takeString", VERS, NS);
        takeStringDef = (ActionDef) actionModel.getType(takeStringName);
        actionModel.registerExecutor(takeStringName, callbackHandler);

        SimpleTypeName takeBooleanName = (SimpleTypeName) TypeNameFactory
                .makeName("takeBoolean", VERS, NS);
        takeBooleanDef = (ActionDef) actionModel.getType(takeBooleanName);
        actionModel.registerExecutor(takeBooleanName, callbackHandler);

        SimpleTypeName takeTimestampName = (SimpleTypeName) TypeNameFactory
                .makeName("takeTimestamp", VERS, NS);
        takeTimestampDef = (ActionDef) actionModel.getType(takeTimestampName);
        actionModel.registerExecutor(takeTimestampName, callbackHandler);

        SimpleTypeName takeDurationName = (SimpleTypeName) TypeNameFactory
                .makeName("takeDuration", VERS, NS);
        takeDurationDef = (ActionDef) actionModel.getType(takeDurationName);
        actionModel.registerExecutor(takeDurationName, callbackHandler);
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    @BeforeMethod
    public void reset()
            throws Exception {
        callbackHandler.reset();
    }

    @Test
    public void tryInteger()
            throws Exception {
        ActionInvocation demo = takeIntegerDef.invoke(null);
        demo.setValue(0, 3l);
        ProcedureLearner.learnAndInvokeProcedure(demo, "tryInteger");
    }

    @Test
    public void tryReal()
            throws Exception {
        ActionInvocation demo = takeRealDef.invoke(null);
        demo.setValue(0, 3.14d);
        ProcedureLearner.learnAndInvokeProcedure(demo, "tryReal");
    }

    @Test
    public void tryString()
            throws Exception {
        ActionInvocation demo = takeStringDef.invoke(null);
        demo.setValue(0, "foo");
        ProcedureLearner.learnAndInvokeProcedure(demo, "tryString");
    }

    @Test
    public void tryBoolean()
            throws Exception {
        ActionInvocation demo = takeBooleanDef.invoke(null);
        demo.setValue(0, Boolean.TRUE);
        ProcedureLearner.learnAndInvokeProcedure(demo, "tryBoolean");
    }

    @Test
    public void tryTimestamp()
            throws Exception {
        ActionInvocation demo = takeTimestampDef.invoke(null);
        demo.setValue(0, new ICalDateTime());
        ProcedureLearner.learnAndInvokeProcedure(demo, "tryTimestamp");
    }

    @Test
    public void tryDuration()
            throws Exception {
        ActionInvocation demo = takeDurationDef.invoke(null);
        demo.setValue(0, new ICalDuration(30 * 60 * 1000));
        ProcedureLearner.learnAndInvokeProcedure(demo, "tryDuration");
    }
}
