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

// $Id: Equivalence_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.List;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class Equivalence_FuncTest
        extends PALBridgeTestCase {
    private static final String ACTION_MODEL = ActionModels.EQUIVALENCE;
    private static final String NAMESPACE = "eqv";

    private TypeDef typeA;
    private TypeDef typeB;
    private ActionDef makeADef;
    private ActionDef takeBDef;
    private ActionDef takeCDef;
    private TypeDef typeD;
    private ActionDef takeDDef;
    private ActionDef makeBDef;
    private TypeDef typeE;
    private ActionDef takeEDef;
    private SimpleTypeName makeAName;
    private SimpleTypeName makeBName;
    private SimpleTypeName takeBName;
    private SimpleTypeName takeCName;
    private SimpleTypeName takeDName;
    private SimpleTypeName takeEName;
    private TypeDef typeF;
    private SimpleTypeName makeFName;
    private ActionDef makeFDef;
    private TypeDef typeG;
    private SimpleTypeName takeGName;
    private ActionDef takeGDef;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTION_MODEL), NAMESPACE);
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    @BeforeMethod
    public void getTypes()
            throws Exception {
        SimpleTypeName typeAName = (SimpleTypeName) TypeNameFactory.makeName(
                "typeA", "1.0", NAMESPACE);
        typeA = (TypeDef) actionModel.getType(typeAName);
        makeAName = (SimpleTypeName) TypeNameFactory.makeName("makeA", "1.0",
                NAMESPACE);
        makeADef = (ActionDef) actionModel.getType(makeAName);
        actionModel.registerExecutor(makeAName, callbackHandler);

        SimpleTypeName typeBName = (SimpleTypeName) TypeNameFactory.makeName(
                "typeB", "1.0", NAMESPACE);
        typeB = (TypeDef) actionModel.getType(typeBName);
        makeBName = (SimpleTypeName) TypeNameFactory.makeName("makeB", "1.0",
                NAMESPACE);
        makeBDef = (ActionDef) actionModel.getType(makeBName);
        actionModel.registerExecutor(makeBName, callbackHandler);
        takeBName = (SimpleTypeName) TypeNameFactory.makeName("takeB", "1.0",
                NAMESPACE);
        takeBDef = (ActionDef) actionModel.getType(takeBName);
        actionModel.registerExecutor(takeBName, callbackHandler);

        takeCName = (SimpleTypeName) TypeNameFactory.makeName("takeC", "1.0",
                NAMESPACE);
        takeCDef = (ActionDef) actionModel.getType(takeCName);
        actionModel.registerExecutor(takeCName, callbackHandler);

        SimpleTypeName typeDName = (SimpleTypeName) TypeNameFactory.makeName(
                "typeD", "1.0", NAMESPACE);
        typeD = (TypeDef) actionModel.getType(typeDName);
        takeDName = (SimpleTypeName) TypeNameFactory.makeName("takeD", "1.0",
                NAMESPACE);
        takeDDef = (ActionDef) actionModel.getType(takeDName);
        actionModel.registerExecutor(takeDName, callbackHandler);

        SimpleTypeName typeEName = (SimpleTypeName) TypeNameFactory.makeName(
                "typeE", "1.0", NAMESPACE);
        typeE = (TypeDef) actionModel.getType(typeEName);
        takeEName = (SimpleTypeName) TypeNameFactory.makeName("takeE", "1.0",
                NAMESPACE);
        takeEDef = (ActionDef) actionModel.getType(takeEName);
        actionModel.registerExecutor(takeEName, callbackHandler);

        SimpleTypeName typeFName = (SimpleTypeName) TypeNameFactory.makeName(
                "typeF", "1.0", NAMESPACE);
        typeF = (TypeDef) actionModel.getType(typeFName);
        makeFName = (SimpleTypeName) TypeNameFactory.makeName("makeF", "1.0",
                NAMESPACE);
        makeFDef = (ActionDef) actionModel.getType(makeFName);
        actionModel.registerExecutor(makeFName, callbackHandler);

        SimpleTypeName typeGName = (SimpleTypeName) TypeNameFactory.makeName(
                "typeG", "1.0", NAMESPACE);
        typeG = (TypeDef) actionModel.getType(typeGName);
        takeGName = (SimpleTypeName) TypeNameFactory.makeName("takeG", "1.0",
                NAMESPACE);
        takeGDef = (ActionDef) actionModel.getType(takeGName);
        actionModel.registerExecutor(takeGName, callbackHandler);
    }

    @AfterMethod
    public void unregisterTypes() {
        actionModel.unregisterExecutor(makeAName);
        actionModel.unregisterExecutor(makeBName);
        actionModel.unregisterExecutor(takeBName);
        actionModel.unregisterExecutor(takeCName);
        actionModel.unregisterExecutor(takeDName);
        actionModel.unregisterExecutor(takeEName);
        actionModel.unregisterExecutor(makeFName);
        actionModel.unregisterExecutor(takeGName);
    }

    @BeforeMethod
    public void resetHandler() {
        callbackHandler.reset();
    }

    /**
     * Type A is equivalent to Type B. Learn a procedure which uses values a1
     * and b1, which are instances of A and B respectively, where a1 = b1. The
     * resulting procedure should unify a1 with b1.
     */
    @Test
    public void simpleEquiv()
            throws Exception {
        String value = "foo";

        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();

        ActionInvocation action1 = makeADef.invoke(null);
        action1.setValue(0, value);
        actions.add(action1);

        ActionInvocation action2 = takeBDef.invoke(null);
        action2.setValue(0, value);
        actions.add(action2);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(actions,
                "simpleEquiv");

        // Should have 1 parameter.
        Assert.assertEquals(1, proc.size());
        // Parameter should be either of type A or B.
        TypeDef type = proc.getParamType(0);
        Assert.assertTrue(type.equals(typeA) || type.equals(typeB), type
                .getName().getFullName());
    }

    /**
     * Types F and G are mutually equivalent to each other. This setup can
     * trigger infinite recursion in code that tries to expand equivalences and
     * dependencies. Learn and execute a simple procedure using these types.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void mutualEquiv()
            throws Exception {
        String value = "foo";

        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();

        ActionInvocation action1 = makeFDef.invoke(null);
        action1.setValue(0, value);
        actions.add(action1);

        ActionInvocation action2 = takeGDef.invoke(null);
        action2.setValue(0, value);
        actions.add(action2);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(actions,
                "mutualEquiv");

        // Should have 1 parameter.
        Assert.assertEquals(1, proc.size());
        // Parameter should be either of type A or B.
        TypeDef type = proc.getParamType(0);
        Assert.assertTrue(type.equals(typeF) || type.equals(typeG), type
                .getName().getFullName());
    }

    /**
     * Type A is equivalent to C, but they use different representation types.
     * The system should reject any procedure referencing both types.
     */
    @Test
    public void differentRepresentationTypes()
            throws Exception {
        Double valueDouble = 1.234;
        String valueString = "" + valueDouble;

        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();

        ActionInvocation action1 = makeADef.invoke(null);
        action1.setValue(0, valueString);
        actions.add(action1);

        ActionInvocation action2 = takeCDef.invoke(null);
        action2.setValue(0, valueDouble);
        actions.add(action2);

        try {
            ProcedureLearner.learnProcedure(actions, "diffReprTypes");
        } catch (PALException e) {
            return;
        }
        Assert.fail();
    }

    /**
     * A is equivalent to B. D is also equivalent to B. We should be able to
     * learn a procedure that uses only values of types A and D, and they should
     * unify.
     */
    @Test
    public void transitiveEquiv()
            throws Exception {
        String value = "foo";

        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();

        ActionInvocation action1 = makeADef.invoke(null);
        action1.setValue(0, value);
        actions.add(action1);

        ActionInvocation action2 = takeDDef.invoke(null);
        action2.setValue(0, value);
        actions.add(action2);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(actions,
                "transitiveEquiv");

        // Should have 1 parameter.
        Assert.assertEquals(1, proc.size());
        // Parameter should be either of type A or D.
        TypeDef type = proc.getParamType(0);
        Assert.assertTrue(type.equals(typeA) || type.equals(typeD));
    }

    /**
     * A is equivalent to B, and A is equivalent to E. The demonstration
     * contains only B and E, not A. We should send LAPDOG enough information
     * that it can deduce that B and E are transitively equivalent to each
     * other.
     */
    @Test
    public void transitive2()
            throws Exception {
        String value = "foo";

        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();

        ActionInvocation action1 = makeBDef.invoke(null);
        action1.setValue(0, value);
        actions.add(action1);

        ActionInvocation action2 = takeEDef.invoke(null);
        action2.setValue(0, value);
        actions.add(action2);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(actions,
                "transitive2");

        // Should have 1 parameter.
        Assert.assertEquals(1, proc.size());
        // Parameter should be either of type B or E.
        TypeDef type = proc.getParamType(0);
        Assert.assertTrue(type.equals(typeB) || type.equals(typeE));
    }
}
