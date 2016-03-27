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

/* $Id: Event_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $ */
package com.sri.pal.events;

import static com.sri.pal.ProcedureLearner.NAMESPACE;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.VerifiableCallbackHandler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Functional tests of registering events with lapdog
 *
 * @author Valerie Wagner Date: Aug 28, 2006
 */
public class Event_FuncTest
        extends PALBridgeTestCase {
    protected static ActionInvocation addRelationMemberEvent;
    protected static ActionInvocation action144Event;
    protected static ActionInvocation reaction163RelationMemberEvent;
    protected static ActionInvocation action143Event;
    protected static ActionInvocation action189Event;
    protected static ActionInvocation action202Event;
    private static SimpleTypeName act182Name;
    private static SimpleTypeName act144Name;

    @BeforeClass
    public static void initialize() throws Exception {
        setup();

        String item7 = "item7-test-1234";
        String sigActType = "IED";
        String item1 = "item1-test-1234";

        // New action model events
        act144Name = (SimpleTypeName) TypeNameFactory.makeName("action144",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(act144Name, callbackHandler);
        ActionDef actDef = (ActionDef) actionModel.getType(act144Name);
        action144Event = actDef.bindAll(null, "item5item2", item1);

        List<String> singleitem2List = new ArrayList<String>();
        singleitem2List.add(item1);
        SimpleTypeName addRelationMemberName = (SimpleTypeName) TypeNameFactory
                .makeName("action170", "1.0", NAMESPACE);
        actionModel.registerExecutor(addRelationMemberName, callbackHandler);
        addRelationMemberEvent = ((ActionDef) actionModel
                .getType(addRelationMemberName)).bindAll(null, item7,
                "members", singleitem2List);

        ArrayList<String> reaction163List = new ArrayList<String>();
        reaction163List.add(item1);
        SimpleTypeName react163Name = (SimpleTypeName) TypeNameFactory
                .makeName("action152", "1.0", NAMESPACE);
        actionModel.registerExecutor(react163Name, callbackHandler);
        reaction163RelationMemberEvent = ((ActionDef) actionModel
                .getType(react163Name)).bindAll(
                null, "table", "member", reaction163List);

        SimpleTypeName act202Name = (SimpleTypeName) TypeNameFactory.makeName(
                "action202", "1.0", NAMESPACE);
        actionModel.registerExecutor(act202Name, callbackHandler);
        action202Event = ((ActionDef) actionModel.getType(act202Name)).bindAll(null, item1, "name",
                "My New item2 Name");

        List<String> entityIDList = new ArrayList<String>();
        entityIDList.add("entity1");
        SimpleTypeName act189Name = (SimpleTypeName) TypeNameFactory.makeName(
                "action189", "1.0", NAMESPACE);
        actionModel.registerExecutor(act189Name, callbackHandler);
        action189Event = ((ActionDef) actionModel.getType(act189Name)).bindAll(
                null, "source", "item44", entityIDList);

        SimpleTypeName act143Name = (SimpleTypeName) TypeNameFactory.makeName(
                "action143", "1.0", NAMESPACE);
        actionModel.registerExecutor(act143Name, callbackHandler);
        action143Event = ((ActionDef) actionModel.getType(act143Name)).bindAll(null,
                "s.309 output table", "sigacts?", "type", sigActType,
                entityIDList);

        act182Name = (SimpleTypeName) TypeNameFactory.makeName("action182",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(act182Name, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void testEmptyProcedure()
            throws Exception {
        Vector<ActionInvocation> noEvents = new Vector<ActionInvocation>();
        ProcedureLearner.learnProcedure(noEvents, "testEmptyProcedure");
    }

    @Test
    public void testNullInputParameter()
            throws Exception {
        ActionInvocation event = ((ActionDef) actionModel.getType(TypeNameFactory.makeName(
                "action144", "1.0", NAMESPACE)))
                .bindAll(null, null, "entityID");
        ProcedureLearner.learnAndInvokeProcedure(event,
                "testNullInputParameter");
    }

    @Test
    public void testNullOutputParameter()
            throws Exception {
        ActionInvocation event = ((ActionDef) actionModel.getType(TypeNameFactory.makeName(
                "action144", "1.0", NAMESPACE))).bindAll(null, "item4ID", null);
        ProcedureLearner.learnAndInvokeProcedure(event,
                "testNullOutputParameter");
    }

    @Test(timeOut = 5000)
    public void testNullTaskInput()
            throws Exception {
        ActionInvocation event = ((ActionDef) actionModel.getType(act182Name))
                .bindAll(null, "entityID", "attr", "value");
        ProcedureDef task = ProcedureLearner.learnProcedure(event, "testNullTaskInput");

        ProcedureInvocation proc = task.invoke(null, (Object) null);
        proc.start();
        proc.waitUntilFinished();
        VerifiableCallbackHandler handler = ProcedureLearner
                .getCallbackHandler();
        assertTrue("Task execution failed", handler.wasTaskSuccessful());
        List<ActionStreamEvent> seenActions = handler.getSeenActions();
        ActionStreamEvent action0 = seenActions.get(0);
        Object value0 = action0.getValue("entityID");
        assertNull("Null value was not passed to callback: " + value0, value0);
    }

    @Test
    public void testEmptyInputParameter()
            throws Exception {
        ActionInvocation event = ((ActionDef) actionModel.getType(act144Name))
                .bindAll(null, "", "entityID");
        ProcedureLearner.learnAndInvokeProcedure(event,
                "testEmptyInputParameter");
    }

    @Test
    public void testEmptyOutputParameter()
            throws Exception {
        ActionInvocation event = ((ActionDef) actionModel.getType(act144Name))
                .bindAll(null, "item4ID", "");
        ProcedureLearner.learnAndInvokeProcedure(event,
                "testEmptyOutputParemeter");
    }
}
