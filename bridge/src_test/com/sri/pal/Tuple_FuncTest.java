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

// $Id: Tuple_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Tuple_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = "ns";

    private static SimpleTypeName act0Name;
    private static ActionDef actDef0;
    private static ActionDef actDef4;
    private static ActionDef actDef5;
    private static StructDef tupleDef5;
    private static ActionDef actDef6;
    private static StructDef tupleDef6;
    private static ActionDef actDef7;
    private static StructDef tupleDef7;
    private static ActionDef actDef8;

    @BeforeClass
    public static void setup()
            throws RemoteException,
            PALException {
        setup(ActionModels.class.getResource(ActionModels.TUPLES), NAMESPACE);
        SimpleTypeName tuple5Name = (SimpleTypeName) TypeNameFactory.makeName(
                "tuple5", "1.0", NAMESPACE);
        SimpleTypeName tuple6Name = (SimpleTypeName) TypeNameFactory.makeName(
                "tuple6", "1.0", NAMESPACE);
        SimpleTypeName tuple7Name = (SimpleTypeName) TypeNameFactory.makeName(
                "tuple7", "1.0", NAMESPACE);
        act0Name = (SimpleTypeName) TypeNameFactory.makeName("action0", "1.0",
                NAMESPACE);
        SimpleTypeName act4Name = (SimpleTypeName) TypeNameFactory.makeName(
                "action4", "1.0", NAMESPACE);
        SimpleTypeName act5Name = (SimpleTypeName) TypeNameFactory.makeName(
                "action5", "1.0", NAMESPACE);
        SimpleTypeName act6Name = (SimpleTypeName) TypeNameFactory.makeName(
                "action6", "1.0", NAMESPACE);
        SimpleTypeName act7Name = (SimpleTypeName) TypeNameFactory.makeName(
                "action7", "1.0", NAMESPACE);
        SimpleTypeName act8Name = (SimpleTypeName) TypeNameFactory.makeName(
                "action8", "1.0", NAMESPACE);
        actionModel.registerExecutor(act0Name, callbackHandler);
        actionModel.registerExecutor(act4Name, callbackHandler);
        actionModel.registerExecutor(act5Name, callbackHandler);
        actionModel.registerExecutor(act6Name, callbackHandler);
        actionModel.registerExecutor(act7Name, callbackHandler);
        actionModel.registerExecutor(act8Name, callbackHandler);
        actDef0 = (ActionDef) actionModel.getType(act0Name);
        actDef4 = (ActionDef) actionModel.getType(act4Name);
        actDef5 = (ActionDef) actionModel.getType(act5Name);
        actDef6 = (ActionDef) actionModel.getType(act6Name);
        actDef7 = (ActionDef) actionModel.getType(act7Name);
        actDef8 = (ActionDef) actionModel.getType(act8Name);
        tupleDef5 = (StructDef) actionModel.getType(tuple5Name);
        tupleDef6 = (StructDef) actionModel.getType(tuple6Name);
        tupleDef7 = (StructDef) actionModel.getType(tuple7Name);
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * Verify that we can access field info and field values by either field
     * name or field number.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void namedField()
            throws Exception {
        SimpleTypeName tupleName = (SimpleTypeName) TypeNameFactory.makeName("tuple4", "1.0", NAMESPACE);
        StructDef structDef = (StructDef) actionModel.getType(tupleName);
        Assert.assertNotNull(structDef);
        Assert.assertEquals(3, structDef.size());
        Assert.assertEquals("field1", structDef.getFieldName(0));
        Assert.assertEquals("foo", structDef.getFieldName(1));
        Assert.assertEquals("bar", structDef.getFieldName(2));
        Assert.assertEquals(0, structDef.getFieldNum("field1"));
        Assert.assertEquals(1, structDef.getFieldNum("foo"));
        Assert.assertEquals(2, structDef.getFieldNum("bar"));

        Struct struct = new Struct(structDef);
        struct.setValue(0, "v1");
        Assert.assertEquals("v1", struct.getValue("field1"));
        struct.setValue("field1", "v2");
        Assert.assertEquals("v2", struct.getValue(0));
    }

    /**
     * Demonstrate and execute a simple procedure that takes a tuple. Verify the
     * tuple values.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void simpleProc()
            throws Exception {
        SimpleTypeName structName = (SimpleTypeName) TypeNameFactory.makeName(
                "tuple4", "1.0", NAMESPACE);
        StructDef structDef = (StructDef) actionModel
                .getType(structName);

        Struct demoStruct = new Struct(structDef);
        demoStruct.setValue(0, "v1");
        demoStruct.setValue(1, "v2");
        demoStruct.setValue(2, "v3");
        ActionInvocation action = actDef4.invoke(null);
        action.setValue(0, demoStruct);

        ProcedureDef procDef = ProcedureLearner.learnAndInvokeProcedure(action,
                "simpleProc");

        ProcedureInvocation invoc = procDef.invoke(null);
        invoc.start();
        invoc.waitUntilFinished();
        Struct seenStruct = (Struct) invoc.getValue(0);
        Assert.assertEquals("v1", seenStruct.getValue("field1"));
        Assert.assertEquals("v2", seenStruct.getValue("foo"));
        Assert.assertEquals("v3", seenStruct.getValue("bar"));

        Struct defStruct = (Struct) procDef.getDefaultValue(0);
        Assert.assertEquals("v1", defStruct.getValue("field1"));
        Assert.assertEquals("v2", defStruct.getValue("foo"));
        Assert.assertEquals("v3", defStruct.getValue("bar"));
    }

    /**
     * Followup to TLEARN-353. Demonstrate an action that creates a tuple as an
     * output, followed by an action that consumes one field of that tuple.
     * Expect a procedure that uses tuple accessors; it should have no inputs.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void tupleOutputField()
            throws Exception {
        String a0 = "value0";
        String a1 = "value1";
        String a2 = "value2";
        String a3 = "value3";
        Struct tupleA = tupleDef5.newInstance();
        tupleA.setValue(0, a0);
        tupleA.setValue(1, a1);
        tupleA.setValue(2, a2);
        tupleA.setValue(3, a3);
        List<ActionInvocation> events = new ArrayList<ActionInvocation>();
        ActionInvocation event1 = actDef5.invoke(null);
        event1.setValue(0, tupleA);
        events.add(event1);

        ActionInvocation event2 = actDef0.invoke(null);
        event2.setValue(0, a2);
        events.add(event2);

        ActionInvocation event3 = actDef0.invoke(null);
        event3.setValue(0, a0);
        events.add(event3);

        ActionInvocation event4 = actDef0.invoke(null);
        event4.setValue(0, a1);
        events.add(event4);

        ActionInvocation event5 = actDef0.invoke(null);
        event5.setValue(0, a3);
        events.add(event5);

        ProcedureDef proc = ProcedureLearner.learnProcedure(events,
                "tupleOutputField");

        Assert.assertEquals(0, proc.numInputParams());

        callbackHandler.reset();
        Struct tupleB = tupleDef5.newInstance();
        String b0 = "b 0";
        String b1 = "b 1";
        String b2 = "b 2";
        String b3 = "b 3";
        tupleB.setValue(0, b0);
        tupleB.setValue(1, b1);
        tupleB.setValue(2, b2);
        tupleB.setValue(3, b3);
        ActionInvocation ev = actDef5.invoke(null);
        ev.setValue(0, tupleB);
        callbackHandler.addFutureEvent(ev);
        ProcedureInvocation invoc = proc.invoke(null);
        invoc.start();
        invoc.waitUntilFinished();
        Assert.assertEquals(Status.ENDED, invoc.getStatus());

        List<ActionStreamEvent> seenActions = callbackHandler
                .getActionsByType(act0Name);
        Assert.assertEquals(b2, seenActions.get(0).getValue(0));
        Assert.assertEquals(b0, seenActions.get(1).getValue(0));
        Assert.assertEquals(b1, seenActions.get(2).getValue(0));
        Assert.assertEquals(b3, seenActions.get(3).getValue(0));
    }

    /**
     * Followup to TLEARN-353. Demonstrate an action that takes a tuple as an
     * input, followed by an action that consumes one field of that tuple.
     * Expect a procedure that uses tuple accessors; it should have one input.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void tupleInputField()
            throws Exception {
        String a0 = "value0";
        String a1 = "value1";
        String a2 = "value2";
        String a3 = "value3";
        Struct tupleA = tupleDef5.newInstance();
        tupleA.setValue(0, a0);
        tupleA.setValue(1, a1);
        tupleA.setValue(2, a2);
        tupleA.setValue(3, a3);
        List<ActionInvocation> events = new ArrayList<ActionInvocation>();
        ActionInvocation event1 = actDef6.invoke(null);
        event1.setValue(0, tupleA);
        events.add(event1);

        ActionInvocation event2 = actDef0.invoke(null);
        event2.setValue(0, a2);
        events.add(event2);

        ActionInvocation event3 = actDef0.invoke(null);
        event3.setValue(0, a0);
        events.add(event3);

        ActionInvocation event4 = actDef0.invoke(null);
        event4.setValue(0, a1);
        events.add(event4);

        ActionInvocation event5 = actDef0.invoke(null);
        event5.setValue(0, a3);
        events.add(event5);

        ProcedureDef proc = ProcedureLearner.learnProcedure(events,
                "tupleOutputField");

        Assert.assertEquals(1, proc.numInputParams());

        callbackHandler.reset();
        Struct tupleB = tupleDef5.newInstance();
        String b0 = "b 0";
        String b1 = "b 1";
        String b2 = "b 2";
        String b3 = "b 3";
        tupleB.setValue(0, b0);
        tupleB.setValue(1, b1);
        tupleB.setValue(2, b2);
        tupleB.setValue(3, b3);
        ProcedureInvocation invoc = proc.invoke(null, tupleB);
        invoc.start();
        invoc.waitUntilFinished();
        Assert.assertEquals(Status.ENDED, invoc.getStatus());

        List<ActionStreamEvent> seenActions = callbackHandler
                .getActionsByType(act0Name);
        Assert.assertEquals(b2, seenActions.get(0).getValue(0));
        Assert.assertEquals(b0, seenActions.get(1).getValue(0));
        Assert.assertEquals(b1, seenActions.get(2).getValue(0));
        Assert.assertEquals(b3, seenActions.get(3).getValue(0));
    }

    /**
     * TLEARN-419 says we have trouble with a struct that contains a null enum
     * field.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void nullTupleEnumField()
            throws Exception {
        Struct demoStruct = new Struct(tupleDef6);
        demoStruct.setValue(0, "value1");
        demoStruct.setValue(1, "foo");
        Struct playbackStruct = new Struct(tupleDef6);
        playbackStruct.setValue(0, null);
        playbackStruct.setValue(1, "bar");

        ActionInvocation action = actDef7.invoke(null);
        action.setValue(0, demoStruct);
        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(action,
                "nullTupleEnumField");

        ProcedureInvocation invoc = proc.invoke(null);
        invoc.setValue(0, playbackStruct);
        invoc.start();
        invoc.waitUntilFinished();
        Assert.assertEquals(Status.ENDED, invoc.getStatus());
    }

    /**
     * Check that we properly handle structs with constant fields. This mostly
     * applies to struct construction, so we start with a struct definition that
     * has a preference=construct. It has three fields, one of which is marked
     * constant. We demonstrate a single action that takes such a tuple as
     * input, and check how many procedure inputs get generated. One input means
     * the whole tuple is taken as an input, which we don't want. Two inputs
     * means it's working correctly, with the two non-constant fields as
     * procedure inputs. Three means all three fields are inputs, which is
     * wrong.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void constantField()
            throws Exception {
        Struct demoStruct = new Struct(tupleDef7);
        demoStruct.setValue(0, "value0");
        demoStruct.setValue(1, "value1");
        demoStruct.setValue(2, "value2");

        ActionInvocation action = actDef8.invoke(null);
        action.setValue(0, demoStruct);
        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(action,
                "constantField");

        Assert.assertEquals(2, proc.size());
        Assert.assertEquals(2, proc.numInputParams());
    }
}
