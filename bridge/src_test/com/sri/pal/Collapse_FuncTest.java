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

// $Id: Collapse_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.List;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class Collapse_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = "cft";
    private static final String ACTION_MODEL = ActionModels.COLLAPSE;
    private static ActionDef actDef1;
    private static ActionDef actDef2;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTION_MODEL), NAMESPACE);
        SimpleTypeName actName1 = (SimpleTypeName) TypeNameFactory.makeName(
                "action1", "1.0", NAMESPACE);
        actDef1 = (ActionDef) actionModel.getType(actName1);
        actionModel.registerExecutor(actName1, callbackHandler);

        SimpleTypeName actName2 = (SimpleTypeName) TypeNameFactory.makeName(
                "action2", "1.0", NAMESPACE);
        actDef2 = (ActionDef) actionModel.getType(actName2);
        actionModel.registerExecutor(actName2, callbackHandler);
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

    /**
     * Demonstrate repetitions of an actions which is collapsible. Verify that
     * the last instance is the only one produced in the procedure.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void simpleCollapse()
            throws Exception {
        List<ActionInvocation> demo = new ArrayList<ActionInvocation>();

        ActionInvocation action1 = actDef1.invoke(null);
        action1.setValue(0, "foo");
        demo.add(action1);

        ActionInvocation action2 = actDef1.invoke(null);
        action2.setValue(0, "bar");
        demo.add(action2);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(demo,
                "simpleCollapse");

        /* Should have seen only 1 executed action. */
        Assert.assertEquals(1, callbackHandler.getSeenActions().size());
        /* Should have only 1 parameter. */
        Assert.assertEquals(1, proc.size());
        /*
         * Default input should be taken from the second observed instance of
         * the action.
         */
        Assert.assertEquals(action2.getValue(0), proc.getDefaultValue(0));
    }

    /**
     * Collapse multiple instances of an action which merges its input values
     * into a combined list.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void mergeCollapse()
            throws Exception {
        List<ActionInvocation> demo = new ArrayList<ActionInvocation>();
        List<String> accum = new ArrayList<String>();

        ActionInvocation action1 = actDef2.invoke(null);
        List<String> list1 = new ArrayList<String>();
        list1.add("A");
        list1.add("B");
        action1.setValue(0, list1);
        demo.add(action1);
        accum.addAll(list1);

        ActionInvocation action2 = actDef2.invoke(null);
        List<String> list2 = new ArrayList<String>();
        action2.setValue(0, list2);
        demo.add(action2);
        accum.addAll(list2);

        ActionInvocation action3 = actDef2.invoke(null);
        List<String> list3 = new ArrayList<String>();
        list3.add("C");
        action3.setValue(0, list3);
        demo.add(action3);
        accum.addAll(list3);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(demo,
                "mergeCollapse");

        /*
         * Procedure should have done only 1 call, and it should have only 1
         * input.
         */
        Assert.assertEquals(1, callbackHandler.getSeenActions().size());
        Assert.assertEquals(1, proc.size());
        Assert.assertEquals(accum, proc.getDefaultValue(0));
    }
}
