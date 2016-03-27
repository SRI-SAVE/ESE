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

// $Id: Inheritance_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.actionmodels;

import static org.testng.AssertJUnit.assertEquals;

import java.net.URL;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests to verify that actions can successfully inherit from other actions.
 *
 * @author chris
 */
public class Inheritance_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = "inh";
    private static final String AM_FILE = "inheritance.xml";

    private static SimpleTypeName abstractInName;
    private static SimpleTypeName abstractOutName;
    private static SimpleTypeName inheritInName;
    private static SimpleTypeName inheritInAddInName;
    private static SimpleTypeName inheritInAddOutName;
    private static SimpleTypeName inheritInAddBothName;
    private static SimpleTypeName inheritOutName;
    private static SimpleTypeName inheritOutAddInName;
    private static SimpleTypeName inheritOutAddOutName;
    private static SimpleTypeName inheritOutAddBothName;
    private static SimpleTypeName inheritBothName;
    private static SimpleTypeName inheritBothAddInName;
    private static SimpleTypeName inheritBothAddOutName;
    private static SimpleTypeName inheritBothAddBothName;

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = Inheritance_FuncTest.class.getResource(AM_FILE);
        setup(url, NAMESPACE);

        abstractInName = (SimpleTypeName) TypeNameFactory.makeName(
                "abstractIn", "1.0", NAMESPACE);
        abstractOutName = (SimpleTypeName) TypeNameFactory.makeName(
                "abstractOut", "1.0", NAMESPACE);
        inheritInName = (SimpleTypeName) TypeNameFactory.makeName("inheritIn",
                "1.0", NAMESPACE);
        inheritInAddInName = (SimpleTypeName) TypeNameFactory.makeName(
                "inheritInAddIn", "1.0", NAMESPACE);
        inheritInAddOutName = (SimpleTypeName) TypeNameFactory.makeName(
                "inheritInAddOut", "1.0", NAMESPACE);
        inheritInAddBothName = (SimpleTypeName) TypeNameFactory.makeName(
                "inheritInAddBoth", "1.0", NAMESPACE);
        inheritOutName = (SimpleTypeName) TypeNameFactory.makeName(
                "inheritOut", "1.0", NAMESPACE);
        inheritOutAddInName = (SimpleTypeName) TypeNameFactory.makeName(
                "inheritOutAddIn", "1.0", NAMESPACE);
        inheritOutAddOutName = (SimpleTypeName) TypeNameFactory.makeName(
                "inheritOutAddOut", "1.0", NAMESPACE);
        inheritOutAddBothName = (SimpleTypeName) TypeNameFactory.makeName(
                "inheritOutAddBoth", "1.0", NAMESPACE);
        inheritBothName = (SimpleTypeName) TypeNameFactory.makeName(
                "inheritBoth", "1.0", NAMESPACE);
        inheritBothAddInName = (SimpleTypeName) TypeNameFactory.makeName(
                "inheritBothAddIn", "1.0", NAMESPACE);
        inheritBothAddOutName = (SimpleTypeName) TypeNameFactory.makeName(
                "inheritBothAddOut", "1.0", NAMESPACE);
        inheritBothAddBothName = (SimpleTypeName) TypeNameFactory.makeName(
                "inheritBothAddBoth", "1.0", NAMESPACE);

        SimpleTypeName actionNames[] = new SimpleTypeName[] { abstractInName,
                abstractOutName, inheritInName, inheritInAddInName,
                inheritInAddOutName, inheritInAddBothName, inheritOutName,
                inheritOutAddInName, inheritOutAddOutName,
                inheritOutAddBothName, inheritBothName, inheritBothAddInName,
                inheritBothAddOutName, inheritBothAddBothName };
        Assert.assertEquals(14, actionNames.length);
        for (SimpleTypeName name : actionNames) {
            actionModel.registerExecutor(name, callbackHandler);
        }
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void abstractIn()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(abstractInName);
        ActionInvocation action = actDef.invoke(null);
        assertEquals(1, actDef.size());
        action.setValue("input1", "1");
        ProcedureLearner.learnAndInvokeProcedure(action, "abstractInProc");
    }

    @Test
    public void abstractOut()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(abstractOutName);
        ActionInvocation action = actDef.invoke(null);
        assertEquals(1, actDef.size());
        action.setValue("output1", "2");
        ProcedureLearner.learnAndInvokeProcedure(action, "abstractOutProc");
    }

    @Test
    public void inheritIn()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(inheritInName);
        ActionInvocation action = actDef.invoke(null);
        assertEquals(1, actDef.size());
        action.setValue("input1", "3");
        ProcedureLearner.learnAndInvokeProcedure(action, "inheritInProc");
    }

    @Test
    public void inheritInAddIn()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(inheritInAddInName);
        ActionInvocation action = actDef.invoke(null);
        assertEquals(2, actDef.size());
        action.setValue("input1", "4");
        action.setValue("input2", "5");
        ProcedureLearner.learnAndInvokeProcedure(action, "inheritInAddInProc");
    }

    @Test
    public void inheritInAddOut()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(inheritInAddOutName);
        ActionInvocation action = actDef.invoke(null);
        assertEquals(2, actDef.size());
        action.setValue("input1", "6");
        action.setValue("output", "7");
        ProcedureLearner.learnAndInvokeProcedure(action, "inheritInAddOutProc");
    }

    @Test
    public void inheritInAddBoth()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(inheritInAddBothName);
        ActionInvocation action = actDef.invoke(null);
        assertEquals(3, actDef.size());
        action.setValue("input1", "8");
        action.setValue("input2", "9");
        action.setValue("output", "10");
        ProcedureLearner.learnAndInvokeProcedure(action, "inheritInAddBothProc");
    }

    @Test
    public void inheritOut()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(inheritOutName);
        ActionInvocation action = actDef.invoke(null);
        assertEquals(1, actDef.size());
        action.setValue("output1", "11");
        ProcedureLearner.learnAndInvokeProcedure(action, "inheritOutProc");
    }

    @Test
    public void inheritOutAddIn()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(inheritOutAddInName);
        ActionInvocation action = actDef.invoke(null);
        assertEquals(2, actDef.size());
        action.setValue("output1", "12");
        action.setValue("input", "13");
        ProcedureLearner.learnAndInvokeProcedure(action, "inheritOutAddInProc");
    }

    @Test
    public void inheritOutAddOut()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(inheritOutAddOutName);
        ActionInvocation action = actDef.invoke(null);
        assertEquals(2, actDef.size());
        action.setValue("output1", "14");
        action.setValue("output2", "15");
        ProcedureLearner.learnAndInvokeProcedure(action, "inheritOutAddOutProc");
    }

    @Test
    public void inheritOutAddBoth()
            throws Exception {
        ActionDef actDef = (ActionDef) actionModel.getType(inheritOutAddBothName);
        ActionInvocation action = actDef.invoke(null);
        assertEquals(3, actDef.size());
        action.setValue("output1", "16");
        action.setValue("input", "17");
        action.setValue("output2", "18");
        ProcedureLearner.learnAndInvokeProcedure(action, "inheritOutAddBothProc");
    }

    // TODO Add some tests of tuple inheritance.
}
