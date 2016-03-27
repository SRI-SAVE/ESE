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

/* $Id: Regression_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $ */
package com.sri.pal.events;

import static com.sri.pal.ProcedureLearner.NAMESPACE;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.VerifiableCallbackHandler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.BeforeMethod;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This test class contains regression tests re-producing bugs seen by running
 * the system in TESTNS.
 *
 * @author Valerie Wagner Date: Apr 17, 2007
 */
public class Regression_FuncTest
        extends PALBridgeTestCase {
    private static SimpleTypeName act144Name;
    private static SimpleTypeName act152Name;
    private static SimpleTypeName act170Name;
    private static SimpleTypeName act189Name;
    private static SimpleTypeName act191Name;
    private static SimpleTypeName act202Name;
    private static SimpleTypeName act210Name;
    private static SimpleTypeName act212Name;

    @BeforeClass
    public static void init()
            throws Exception {
        setup();

        act144Name = (SimpleTypeName) TypeNameFactory.makeName("action144",
                "1.0", "TESTNS");
        actionModel.registerExecutor(act144Name, callbackHandler);
        act152Name = (SimpleTypeName) TypeNameFactory.makeName("action152",
                "1.0", "TESTNS");
        actionModel.registerExecutor(act152Name, callbackHandler);
        act170Name = (SimpleTypeName) TypeNameFactory.makeName("action170",
                "1.0", "TESTNS");
        actionModel.registerExecutor(act170Name, callbackHandler);
        act189Name = (SimpleTypeName) TypeNameFactory.makeName("action189",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(act189Name, callbackHandler);
        act191Name = (SimpleTypeName) TypeNameFactory.makeName("action191",
                "1.0", "TESTNS");
        actionModel.registerExecutor(act191Name, callbackHandler);
        act202Name = (SimpleTypeName) TypeNameFactory.makeName("action202",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(act202Name, callbackHandler);
        act210Name = (SimpleTypeName) TypeNameFactory.makeName("action210",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(act210Name, callbackHandler);
        act212Name = (SimpleTypeName) TypeNameFactory.makeName("action212",
                "1.0", "TESTNS");
        actionModel.registerExecutor(act212Name, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @BeforeMethod
    public void reset() {
        callbackHandler.reset();
    }

    /**
     * https://jira.esd.sri.com/browse/MTRS-24
     */
    @Test
    public void testMTRS_24_1()
            throws Exception {
        List<ActionInvocation> demoEvents = new Vector<ActionInvocation>();
        List<ActionInvocation> playbackEvents = new Vector<ActionInvocation>();

        List<String> demoList = new Vector<String>();
        demoList.add("child1");
        demoList.add("child2");
        ActionInvocation demo1 = ((ActionDef) actionModel.getType(act189Name))
                .bindAll(null, "entity", "children", demoList);
        demoEvents.add(demo1);

        List<String> playbackList = new Vector<String>();
        playbackList.add("child3");
        playbackList.add("child4");
        playbackList.add("child5");
        ActionInvocation playback1 = ((ActionDef) actionModel
                .getType(TypeNameFactory.makeName("action189", "1.0", NAMESPACE))).bindAll(
                null, null, null, playbackList);
        playbackEvents.add(playback1);

        ActionInvocation demo2 = ((ActionDef) actionModel.getType(TypeNameFactory.makeName(
                "action170", "1.0", NAMESPACE))).bindAll(null, "entity2",
                "children", demoList);
        demoEvents.add(demo2);

        ProcedureLearner.learnAndInvokeProcedure(demoEvents, "testMTRS_24_1",
                playbackEvents);

        VerifiableCallbackHandler handler = ProcedureLearner
                .getCallbackHandler();
        List<ActionStreamEvent> seenEvents = handler.getActionsByType(demo2
                .getDefinition().getName());
        assertEquals("Wrong number of observed events", 1, seenEvents.size());
        ActionStreamEvent seenEvent = seenEvents.get(0);
        assertEquals("Event has wrong type", demo2.getDefinition(), seenEvent
                .getDefinition());
        assertEquals("Event has wrong class", demo2.getClass(), seenEvent
                .getClass());
        Object seenListObj = seenEvent.getValue("addEntityIDList");
        assertTrue(seenListObj instanceof List<?>);
        List<?> seenList = (List<?>) seenListObj;
        assertTrue(demoList.size() != seenList.size());
    }

    /**
     * https://jira.esd.sri.com/browse/MTRS-24
     */
    @Test
    public void testMTRS_24_2()
            throws Exception {
        Vector<ActionInvocation> demoEvents = new Vector<ActionInvocation>();
        Vector<ActionInvocation> playbackEvents = new Vector<ActionInvocation>();

        // 33821 2007-07-09 09:54:29,576 LearningBridge : INFO - Received event:
        // DeclareUserContextEvent[param151=0x0732666562656662663832343036613237613a353737313466343838383662383363653a666261373931623530643a2d33636466;
        // param189=0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666632;
        // param111=0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666565;
        // param107=0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666566;
        // ]
        ActionInvocation demo1 = ((ActionDef) actionModel.getType(act210Name))
                .bindAll(
                        null,
                        "0x0732666562656662663832343036613237613a353737313466343838383662383363653a666261373931623530643a2d33636466",
                        "0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666632",
                        "0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666565",
                        "0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666566");
        demoEvents.add(demo1);

        // 33899 2007-07-09 09:54:29,654 LearningBridge : INFO - Received event:
        // SetEntitytype115Event[param185=0x080e000001139b7948a10af1045d80f9;
        // param198=pos_z; param143=2695.0; ]
        ActionInvocation demo2 = ((ActionDef) actionModel.getType(act202Name)).bindAll(null,
                "0x080e000001139b7948a10af1045d80f9", "pos_z", "2695.0");
        demoEvents.add(demo2);

        // 33915 2007-07-09 09:54:29,670 LearningBridge : INFO - Received event:
        // Reaction163RelationMembersEvent[param114=0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666566;
        // param157=selected_child;
        // param161=[0x080e00000113ab0d6c400af1045d8061]; ]
        List<String> list1 = new ArrayList<String>();
        list1.add("0x080e00000113ab0d6c400af1045d8061");
        ActionInvocation demo3 = ((ActionDef) actionModel.getType(act152Name))
                .bindAll(
                        null,
                        "0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666566",
                        "selected_child", list1);
        demoEvents.add(demo3);

        // 33915 2007-07-09 09:54:29,670 LearningBridge : INFO - Received event:
        // AddRelationMembersEvent[param114=0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666566;
        // param157=selected_child;
        // param119=[0x080e000001139b7948a10af1045d80f9]; ]
        List<String> list2 = new ArrayList<String>();
        list2.add("0x080e000001139b7948a10af1045d80f9");
        ActionInvocation demo4 = ((ActionDef) actionModel.getType(act170Name))
                .bindAll(
                        null,
                        "0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666566",
                        "selected_child", list2);
        demoEvents.add(demo4);

        // 35820 2007-07-09 09:54:31,575 LearningBridge : INFO - Received event:
        // SelectEntitiesInRelationEvent[param108=0x080e00000113ab0d6c400af1045d81dd;
        // param157=item22;
        // param170=[0x080e00000113ab24f43d0af1045d81ad,
        // 0x080e00000113ab24f43d0af1045d81c2,
        // 0x080e00000113ab24f43d0af1045d81cc]; ]
        List<String> list3 = new ArrayList<String>();
        list3.add("0x080e00000113ab24f43d0af1045d81ad");
        list3.add("0x080e00000113ab24f43d0af1045d81c2");
        list3.add("0x080e00000113ab24f43d0af1045d81cc");
        ActionInvocation demo5 = ((ActionDef) actionModel.getType(act189Name)).bindAll(null,
                "0x080e00000113ab0d6c400af1045d81dd", "item22", list3);
        demoEvents.add(demo5);

        List<String> playbackList = new ArrayList<String>();
        playbackList.add("item1");
        playbackList.add("item2");
        playbackList.add("item3");
        playbackList.add("item4");
        ActionInvocation playback5 = ((ActionDef) actionModel
                .getType(act189Name)).bindAll(
                null, "0x080e00000113ab0d6c400af1045d81dd", "item22",
                playbackList);
        playbackEvents.add(playback5);

        // 36819 2007-07-09 09:54:32,574 LearningBridge : INFO - Received event:
        // Reaction163RelationMembersEvent[param114=0x080e00000113ab0d6c400af1045d81dd;
        // param157=item22;
        // param161=[0x080e00000113ab24f43d0af1045d81ad,
        // 0x080e00000113ab24f43d0af1045d81c2,
        // 0x080e00000113ab24f43d0af1045d81cc]; ]
        List<String> list4 = new ArrayList<String>();
        list4.add("0x080e00000113ab24f43d0af1045d81ad");
        list4.add("0x080e00000113ab24f43d0af1045d81c2");
        list4.add("0x080e00000113ab24f43d0af1045d81cc");
        ActionInvocation demo6 = ((ActionDef) actionModel.getType(act152Name)).bindAll(null,
                "0x080e00000113ab0d6c400af1045d81dd", "item22", list4);
        demoEvents.add(demo6);

        // 36835 2007-07-09 09:54:32,590 LearningBridge : INFO - Received event:
        // AddRelationMembersEvent[param114=0x080e00000113ab0d6c400af1045d807a;
        // param157=item22;
        // param119=[0x080e00000113ab24f43d0af1045d81ad,
        // 0x080e00000113ab24f43d0af1045d81c2,
        // 0x080e00000113ab24f43d0af1045d81cc]; ]
        List<String> list5 = new ArrayList<String>();
        list5.add("0x080e00000113ab24f43d0af1045d81ad");
        list5.add("0x080e00000113ab24f43d0af1045d81c2");
        list5.add("0x080e00000113ab24f43d0af1045d81cc");
        ActionInvocation demo7 = ((ActionDef) actionModel.getType(act170Name)).bindAll(null,
                "0x080e00000113ab0d6c400af1045d807a", "item22", list5);
        demoEvents.add(demo7);

        // 36835 2007-07-09 09:54:32,590 LearningBridge : INFO - Received event:
        // SetEntitytype115Event[param185=0x080e00000113ab0d6c400af1045d8061;
        // param198=pos_z; param143=2697.0; ]
        ActionInvocation demo8 = ((ActionDef) actionModel.getType(act202Name)).bindAll(null,
                "0x080e00000113ab0d6c400af1045d8061", "pos_z", "2697.0");
        demoEvents.add(demo8);

        // 36835 2007-07-09 09:54:32,590 LearningBridge : INFO - Received event:
        // Reaction163RelationMembersEvent[param114=0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666566;
        // param157=selected_child;
        // param161=[0x080e000001139b7948a10af1045d80f9]; ]
        List<String> list6 = new ArrayList<String>();
        list6.add("0x080e000001139b7948a10af1045d80f9");
        ActionInvocation demo9 = ((ActionDef) actionModel.getType(act152Name))
                .bindAll(
                        null,
                        "0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666566",
                        "selected_child", list6);
        demoEvents.add(demo9);

        // 36835 2007-07-09 09:54:32,590 LearningBridge : INFO - Received event:
        // AddRelationMembersEvent[param114=0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666566;
        // param157=selected_child;
        // param119=[0x080e00000113ab0d6c400af1045d8061]; ]
        List<String> list7 = new ArrayList<String>();
        list7.add("0x080e00000113ab0d6c400af1045d8061");
        ActionInvocation demo10 = ((ActionDef) actionModel
                .getType(act170Name))
                .bindAll(
                        null,
                        "0x0733396563393066656530646230646135643a2d336663306463326465333034376465663a666261613662656665353a2d37666566",
                        "selected_child", list7);
        demoEvents.add(demo10);

        // 39645 2007-07-09 09:54:35,400 LearningBridge : INFO - Received event:
        // SetEntitytype115Event[param185=0x080e00000113ab24f43d0af1045d81c2;
        // param198=lat; param143=33.44960468546824; ]
        ActionInvocation demo11 = ((ActionDef) actionModel
                .getType(act202Name)).bindAll(
                null, "0x080e00000113ab24f43d0af1045d81c2", "lat",
                "33.44960468546824");
        demoEvents.add(demo11);

        // 39661 2007-07-09 09:54:35,416 LearningBridge : INFO - Received event:
        // SetEntitytype115Event[param185=0x080e00000113ab24f43d0af1045d81c2;
        // param198=GridCoords; param143=38SMC 41531 01309; ]
        ActionInvocation demo12 = ((ActionDef) actionModel
                .getType(act202Name)).bindAll(
                null, "0x080e00000113ab24f43d0af1045d81c2", "GridCoords",
                "38SMC 41531 01309");
        demoEvents.add(demo12);

        ProcedureDef proc = ProcedureLearner.learnProcedure(demoEvents, "testMTRS_24_2");
        ProcedureLearner.invokeTask(proc, playbackEvents, false);

        VerifiableCallbackHandler handler = ProcedureLearner
                .getCallbackHandler();
        List<ActionStreamEvent> seenEvents = handler.getActionsByType(demo6
                .getDefinition().getName());
        assertEquals("Wrong number of observed events", 3, seenEvents.size());
        ActionStreamEvent event = seenEvents.get(1);
        assertEquals("Event has wrong type", demo6.getClass(), event.getClass());
        List<?> demoList = (List<?>) demo6.getValue("reaction163EntityIDList");
        List<?> execList = (List<?>) event.getValue("reaction163EntityIDList");
        assertTrue(demoList.size() != execList.size());
    }

    // MTRS-25
    @Test
    public void testMTRS_25()
            throws Exception {
        Vector<ActionInvocation> events = new Vector<ActionInvocation>();

        List<String> strings = new ArrayList<String>();

        for (int i = 0; i < 15; i++) {
            strings.add("SomeId" + i);
        }
        events.add(((ActionDef) actionModel.getType(act191Name))
                .bindAll(null, "100001", "some rel", strings));

        strings = new ArrayList<String>();
        for (int i = 0; i < 90; i++) {
            strings.add("AnotherId" + i);
        }
        events.add(((ActionDef) actionModel.getType(act191Name)).bindAll(null,
                "SomeId2", "some other rel", strings));
        events.add(((ActionDef) actionModel.getType(act191Name)).bindAll(null,
                "AnotherId2", "some rel", Arrays.asList("Blah")));
        events.add(((ActionDef) actionModel.getType(act191Name)).bindAll(null,
                "Something", "some rel", Arrays.asList("Somethingelse")));
        events.add(((ActionDef) actionModel.getType(act212Name)).bindAll(null,
                "Somethingelse", "SomeId10"));

        ProcedureLearner.learnAndInvokeProcedure(events, "testMTRS_25", events);
    }

    // MTRS-113
    @Test
    public void testNonAscii()
            throws Exception {
        String inputStr = "�Hello,� she said � and she meant it.";
        String outputStr = "hi";
        ActionInvocation action = ((ActionDef) actionModel.getType(act144Name))
                .bindAll(null, inputStr, outputStr);
        ProcedureLearner.learnAndInvokeProcedure(action, "testNonAscii");
    }
}
