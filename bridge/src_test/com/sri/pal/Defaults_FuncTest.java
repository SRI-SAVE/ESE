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

// $Id: Defaults_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * Test our ability to specify default values for actions in the action model
 * XML file.
 */
public class Defaults_FuncTest
        extends PALBridgeTestCase {
    private static final String ACTION_MODEL = ActionModels.DEFAULTS;
    private static final String NS = "dft";
    private static final String VERS = "1.0";

    private static ActionDef getThing1Def;
    private static ActionDef getThingList1Def;
    private static ActionDef getThingList2Def;
    private static ActionDef getThingSet1Def;
    private static ActionDef getThingSet2Def;
    private static StructDef struct1Def;
    private static ActionDef getStructDef;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTION_MODEL), NS);

        SimpleTypeName getThing1Name = (SimpleTypeName) TypeNameFactory
                .makeName("getThing1", VERS, NS);
        getThing1Def = (ActionDef) actionModel.getType(getThing1Name);
        actionModel.registerExecutor(getThing1Name, callbackHandler);

        SimpleTypeName getThingList1Name = (SimpleTypeName) TypeNameFactory
                .makeName("getThingList1", VERS, NS);
        getThingList1Def = (ActionDef) actionModel.getType(getThingList1Name);
        actionModel.registerExecutor(getThingList1Name, callbackHandler);

        SimpleTypeName getThingList2Name = (SimpleTypeName) TypeNameFactory
                .makeName("getThingList2", VERS, NS);
        getThingList2Def = (ActionDef) actionModel.getType(getThingList2Name);
        actionModel.registerExecutor(getThingList2Name, callbackHandler);

        SimpleTypeName getThingSet1Name = (SimpleTypeName) TypeNameFactory
                .makeName("getThingSet1", VERS, NS);
        getThingSet1Def = (ActionDef) actionModel.getType(getThingSet1Name);
        actionModel.registerExecutor(getThingSet1Name, callbackHandler);

        SimpleTypeName getThingSet2Name = (SimpleTypeName) TypeNameFactory
                .makeName("getThingSet2", VERS, NS);
        getThingSet2Def = (ActionDef) actionModel.getType(getThingSet2Name);
        actionModel.registerExecutor(getThingSet2Name, callbackHandler);

        SimpleTypeName struct1Name = (SimpleTypeName) TypeNameFactory.makeName(
                "struct1", VERS, NS);
        struct1Def = (StructDef) actionModel.getType(struct1Name);

        SimpleTypeName getStructName = (SimpleTypeName) TypeNameFactory
                .makeName("getStruct", VERS, NS);
        getStructDef = (ActionDef) actionModel.getType(getStructName);
        actionModel.registerExecutor(getStructName, callbackHandler);
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
    public void simple()
            throws Exception {
        Assert.assertEquals("", getThing1Def.getDefaultValue(0));
        Assert.assertEquals("default for param #2",
                getThing1Def.getDefaultValue(1));
    }

    @Test
    public void emptyList()
            throws Exception {
        Assert.assertEquals(Collections.emptyList(),
                getThingList1Def.getDefaultValue(0));
    }

    @Test
    public void nonEmptyList()
            throws Exception {
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        Assert.assertEquals(list, getThingList2Def.getDefaultValue(0));
    }

    @Test
    public void emptySet()
            throws Exception {
        Assert.assertEquals(Collections.emptySet(),
                getThingSet1Def.getDefaultValue(0));
    }

    @Test
    public void nonEmptySet()
            throws Exception {
        Set<String> set = new HashSet<String>();
        set.add("c");
        set.add("d");
        Assert.assertEquals(set, getThingSet2Def.getDefaultValue(0));
    }

    @Test
    public void struct()
            throws Exception {
        Struct struct = new Struct(struct1Def);
        struct.setValue(0, "e");
        struct.setValue(1, "f");
        Assert.assertEquals(struct, getStructDef.getDefaultValue(0));
    }
}
