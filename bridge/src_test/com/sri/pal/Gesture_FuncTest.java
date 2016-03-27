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

// $Id: Gesture_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
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

/**
 * Tests involving gestures.
 */
public class Gesture_FuncTest
        extends PALBridgeTestCase {
    private static final String ACTION_MODEL = ActionModels.IDIOMS;
    private static final String NS = "gft";
    private static final String VERS = "1.0";

    private ActionDef remove1Def;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTION_MODEL), NS);
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    @BeforeMethod
    public void getTypes()
            throws Exception {
        SimpleTypeName remove1Name = (SimpleTypeName) TypeNameFactory.makeName(
                "removeThingFromContainer", VERS, NS);
        remove1Def = (ActionDef) actionModel.getType(remove1Name);
        actionModel.registerExecutor(remove1Name, callbackHandler);
    }

    @BeforeMethod
    public void reset()
            throws Exception {
        callbackHandler.reset();
    }

    /**
     * Try learning and executing a procedure based on a demonstration which has
     * gestures in it, but no idioms.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void unrecognized()
            throws Exception {
        List<ActionStreamEvent> demo = new ArrayList<ActionStreamEvent>();

        ActionInvocation event1 = remove1Def.invoke(null);
        event1.setValue(0, "item #1");
        event1.setValue(1, "container #1");
        demo.add(event1);

        GestureStart event2 = GestureStart.newInstance(palBridge, null, null, null);
        demo.add(event2);

        ActionInvocation event3 = remove1Def.invoke(null);
        event3.setValue(0, "item #2");
        event3.setValue(1, "container #2");
        demo.add(event3);

        GestureEnd event4 = GestureEnd.newInstance(event2);
        demo.add(event4);

        ProcedureLearner.learnAndInvokeProcedure(demo, "unrecognized");

        Assert.assertEquals(callbackHandler.getSeenActions().size(), 4);
        Assert.assertEquals(callbackHandler.getNumGesturesSeen(), 1);
    }
}
