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

// $Id: LearningPrefs_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
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

public class LearningPrefs_FuncTest
        extends PALBridgeTestCase {
    private static final String ACTION_MODEL = ActionModels.LEARNING_PREFS;
    private static final String NS = "lpft";
    private static final String VERS = "1.0";

    private static ActionDef act6Def;
    private static ActionDef act7Def;
    private static ActionDef act8Def;
    private static ActionDef act9Def;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTION_MODEL), NS);

        SimpleTypeName act6Name = (SimpleTypeName) TypeNameFactory.makeName(
                "action6", VERS, NS);
        act6Def = (ActionDef) actionModel.getType(act6Name);
        actionModel.registerExecutor(act6Name, callbackHandler);

        SimpleTypeName act7Name = (SimpleTypeName) TypeNameFactory
                .makeName("action7", VERS, NS);
        act7Def = (ActionDef) actionModel.getType(act7Name);
        actionModel.registerExecutor(act7Name, callbackHandler);

        SimpleTypeName act8Name = (SimpleTypeName) TypeNameFactory.makeName(
                "action8", VERS, NS);
        act8Def = (ActionDef) actionModel.getType(act8Name);
        actionModel.registerExecutor(act8Name, callbackHandler);

        SimpleTypeName act9Name = (SimpleTypeName) TypeNameFactory.makeName(
                "action9", VERS, NS);
        act9Def = (ActionDef) actionModel.getType(act9Name);
        actionModel.registerExecutor(act9Name, callbackHandler);
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
     * Test <generalizeSingleton method="first">.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void generalizeSingletonFirst()
            throws Exception {
        String item1 = "item1";
        String item2 = "item2";
        String item3 = "item3";
        String item4 = "item4";
        List<String> list1 = new ArrayList<String>();
        list1.add(item1);

        List<ActionStreamEvent> demo = new ArrayList<ActionStreamEvent>();
        ActionInvocation event1 = act6Def.invoke(null);
        event1.setValue(0, list1);
        demo.add(event1);

        ActionInvocation event2 = act7Def.invoke(null);
        event2.setValue(0, item1);
        demo.add(event2);

        List<ActionInvocation> playback = new ArrayList<ActionInvocation>();
        ActionInvocation playback1 = act6Def.invoke(null);
        List<String> playbackList = new ArrayList<String>();
        playbackList.add(item2);
        playbackList.add(item3);
        playbackList.add(item4);
        playback1.setValue(0, playbackList);
        playback.add(playback1);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(demo,
                "generalizeSingletonFirst", playback);

        Assert.assertTrue(proc.getAtrStr().contains("first("));
        List<ActionStreamEvent> seen = callbackHandler.getSeenActions();
        Assert.assertEquals(2, seen.size());
        ActionInvocation seen1 = (ActionInvocation) seen.get(0);
        Assert.assertEquals(act6Def, seen1.getDefinition());
        Assert.assertEquals(playbackList, seen1.getValue(0));
        ActionInvocation seen2 = (ActionInvocation) seen.get(1);
        Assert.assertEquals(act7Def, seen2.getDefinition());
        Assert.assertEquals(item2, seen2.getValue(0));
    }

    /**
     * Test <generalizeSingleton method="last">.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void generalizeSingletonLast()
            throws Exception {
        String item1 = "item1";
        String item2 = "item2";
        String item3 = "item3";
        String item4 = "item4";
        List<String> list1 = new ArrayList<String>();
        list1.add(item1);

        List<ActionStreamEvent> demo = new ArrayList<ActionStreamEvent>();
        ActionInvocation event1 = act8Def.invoke(null);
        event1.setValue(0, list1);
        demo.add(event1);

        ActionInvocation event2 = act7Def.invoke(null);
        event2.setValue(0, item1);
        demo.add(event2);

        List<ActionInvocation> playback = new ArrayList<ActionInvocation>();
        ActionInvocation playback1 = act8Def.invoke(null);
        List<String> playbackList = new ArrayList<String>();
        playbackList.add(item2);
        playbackList.add(item3);
        playbackList.add(item4);
        playback1.setValue(0, playbackList);
        playback.add(playback1);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(demo,
                "generalizeSingletonLast", playback);

        Assert.assertTrue(proc.getAtrStr().contains("last("));
        List<ActionStreamEvent> seen = callbackHandler.getSeenActions();
        Assert.assertEquals(2, seen.size());
        ActionInvocation seen1 = (ActionInvocation) seen.get(0);
        Assert.assertEquals(act8Def, seen1.getDefinition());
        Assert.assertEquals(playbackList, seen1.getValue(0));
        ActionInvocation seen2 = (ActionInvocation) seen.get(1);
        Assert.assertEquals(act7Def, seen2.getDefinition());
        Assert.assertEquals(item4, seen2.getValue(0));
    }

    /**
     * An action input parameter designated as local is one that is generalized
     * normally if it's supported by another input or output parameter, but
     * replaced with null if not.
     * <p>
     * To test this, we demonstrate two instances of an action with a local
     * input. They're preceded by an action that supports one of them, but not
     * the other.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void localInput()
            throws Exception {
        String suppInput = "string #1";
        String unsuppInput = "string #2";

        List<ActionInvocation> demo = new ArrayList<ActionInvocation>();
        ActionInvocation action0 = act7Def.invoke(null);
        action0.setValue(0, suppInput);
        demo.add(action0);

        ActionInvocation action1 = act9Def.invoke(null);
        action1.setValue(0, suppInput);
        demo.add(action1);

        ActionInvocation action2 = act9Def.invoke(null);
        action2.setValue(0, unsuppInput);
        demo.add(action2);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(demo,
                "localInput", demo);

        Assert.assertEquals(1, proc.numInputParams());
    }
}
