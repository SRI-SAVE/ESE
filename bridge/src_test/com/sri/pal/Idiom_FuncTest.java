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

// $Id: Idiom_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.List;

import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.ai.lumen.atr.learning.impl.ATRDemonstrationImpl;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test functionality relating to idiom recognition.
 */
public class Idiom_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory
            .getLogger(Idiom_FuncTest.class);
    private static final String ACTION_MODEL = ActionModels.IDIOMS;
    private static final String NS = "gft";
    private static final String VERS = "1.0";

    private static ActionDef add1Def;
    private static ActionDef remove1Def;
    private static SimpleTypeName moveName;
    private static SimpleTypeName moveAName;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTION_MODEL), NS);

        SimpleTypeName add1Name = (SimpleTypeName) TypeNameFactory.makeName(
                "addThingToContainer", VERS, NS);
        add1Def = (ActionDef) actionModel.getType(add1Name);
        actionModel.registerExecutor(add1Name, callbackHandler);

        SimpleTypeName remove1Name = (SimpleTypeName) TypeNameFactory.makeName(
                "removeThingFromContainer", VERS, NS);
        remove1Def = (ActionDef) actionModel.getType(remove1Name);
        actionModel.registerExecutor(remove1Name, callbackHandler);

        SimpleTypeName addRemoveName = (SimpleTypeName) TypeNameFactory
                .makeName("addSomeThingsAndRemoveOthers", VERS, NS);
        actionModel.registerExecutor(addRemoveName, callbackHandler);

        moveName = (SimpleTypeName) TypeNameFactory.makeName("move", VERS, NS);
        moveAName = moveName.addIdiomTemplateName("A");
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
     * Try recognizing a simple idiom.
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    public void simpleIdiom()
            throws Exception {
        List<ActionStreamEvent> events = new ArrayList<ActionStreamEvent>();
        GestureStart gStart = GestureStart.newInstance(palBridge, null, null, null);
        events.add(gStart);

        ActionInvocation add = add1Def.invoke(null);
        add.setValue(0, "beer");
        add.setValue(1, "stein");
        events.add(add);

        ActionInvocation remove = remove1Def.invoke(null);
        remove.setValue(0, "beer");
        remove.setValue(1, "pitcher");
        events.add(remove);

        GestureEnd gEnd = GestureEnd.newInstance(gStart);
        events.add(gEnd);

        List<ActionStreamEvent> idiom = learningBridge.recognizeIdiom(events
                .toArray(new ActionStreamEvent[0]));
        log.info("Idiom: {}", idiom);

        Assert.assertEquals(4, idiom.size());
        ActionStreamEvent event0 = idiom.get(0);
        Assert.assertTrue(event0 instanceof GestureStart);
        GestureStart start0 = (GestureStart) event0;
        IdiomDef def0 = (IdiomDef) event0.getDefinition();
        Assert.assertNotNull(def0);
        Assert.assertEquals(moveName, def0.getName());

        /* Verify idiom parameter names. */
        Assert.assertEquals("payload", def0.getParamName(0));
        Assert.assertEquals("source", def0.getParamName(1));
        Assert.assertEquals("dest", def0.getParamName(2));

        /* Verify idiom parameter types. */
        PrimitiveTypeDef stringDef = PrimitiveTypeDef.getPrimitive(
                PrimitiveTypeDef.Predefined.STRING, palBridge);
        Assert.assertEquals(stringDef, start0.getParamType(0));
        Assert.assertEquals(stringDef, start0.getParamType(1));
        Assert.assertEquals(stringDef, start0.getParamType(2));

        /* Verify idiom parameter values. */
        Assert.assertEquals("beer", start0.getValue(0));
        Assert.assertEquals("pitcher", start0.getValue(1));
        Assert.assertEquals("stein", start0.getValue(2));
    }

    /**
     * Reconstitute an idiom in Bridge objects from ATR. The idiom is preceded
     * by an action, and followed by an action. Ensure all serial numbers are
     * assigned sequentially.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void idiomOrder()
            throws Exception {
        String idiomStr = "$demonstration {"
                + "  'gft^1.0^addThingToContainer' \"water\", \"cup\";"
                + "  $idiom 'gft^1.0^move/A'(\"beer\", \"pitcher\", \"stein\")"
                + "   argtypes:[\"string\", \"string\", \"string\"]"
                + "   body:{"
                + "    'gft^1.0^addThingToContainer' \"beer\", \"stein\";"
                + "    'gft^1.0^removeThingFromContainer' \"beer\", \"pitcher\";};"
                + "  'gft^1.0^removeThingFromContainer' \"water\", \"cup\";}"
                + " properties:{}";
        ATRDemonstration idiomAtr = ATRDemonstrationImpl.parse(idiomStr);
        List<ActionStreamEvent> actions = learningBridge.demoFromAtr(idiomAtr
                .getActions());

        /* First make sure we have the right definitions in the right order. */
        Assert.assertEquals(actions.get(0).getDefinition(), add1Def);
        Assert.assertEquals(actions.get(1).getDefinition().getName(), moveName);
        Assert.assertEquals(actions.get(2).getDefinition(), add1Def);
        Assert.assertEquals(actions.get(3).getDefinition(), remove1Def);
        Assert.assertTrue(actions.get(4) instanceof GestureEnd);
        Assert.assertEquals(actions.get(5).getDefinition(), remove1Def);

        /* Check that serial numbers are sequential. */
        long serial0 = actions.get(0).getSerialNumber();
        long serial1 = actions.get(1).getSerialNumber();
        long serial2 = actions.get(2).getSerialNumber();
        long serial3 = actions.get(3).getSerialNumber();
        long serial4 = actions.get(4).getSerialNumber();
        long serial5 = actions.get(5).getSerialNumber();
        Assert.assertTrue(serial0 < serial1, serial0 + ", " + serial1);
        Assert.assertTrue(serial1 < serial2, serial1 + ", " + serial2);
        Assert.assertTrue(serial2 < serial3, serial2 + ", " + serial3);
        Assert.assertTrue(serial3 < serial4, serial3 + ", " + serial4);
        Assert.assertTrue(serial4 < serial5, serial4 + ", " + serial5);
    }

    /**
     * Try learning a simple procedure which should get recognized as an idiom.
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    public void simpleLearn()
            throws Exception {
        List<ActionStreamEvent> events = new ArrayList<ActionStreamEvent>();
        GestureStart gStart = GestureStart.newInstance(palBridge, null, null, null);
        events.add(gStart);

        ActionInvocation add = add1Def.invoke(null);
        add.setValue(0, "beer");
        add.setValue(1, "stein");
        events.add(add);

        ActionInvocation remove = remove1Def.invoke(null);
        remove.setValue(0, "beer");
        remove.setValue(1, "pitcher");
        events.add(remove);

        GestureEnd gEnd = GestureEnd.newInstance(gStart);
        events.add(gEnd);

        Assert.assertFalse(callbackHandler.sawAction(moveAName));

        ProcedureLearner.learnAndInvokeProcedure(events, "simpleLearn");

        Assert.assertEquals(1, callbackHandler.getNumGesturesSeen());
        Assert.assertTrue(callbackHandler.sawAction(moveName));
        callbackHandler.getSeenActions();
    }
}
