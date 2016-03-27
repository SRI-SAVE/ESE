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

// $Id: Null_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Various tests of null handling. The expected behaviors for these tests will
 * change once we support nullable.
 */
public class Null_FuncTest
        extends PALBridgeTestCase {
    private static final String ACTION_MODEL = ActionModels.ALLTYPES;
    private static final String NS = "nft";
    private static final String VERS = "1.0";

    private static StructDef struct1Def;
    private static ActionDef getThingDef;
    private static ActionDef setThingDef;
    private static ActionDef getThingBagDef;
    private static ActionDef setThingBagDef;
    private static ActionDef getThingListDef;
    private static ActionDef setThingListDef;
    private static ActionDef getThingSetDef;
    private static ActionDef setThingSetDef;
    private static ActionDef getStruct1Def;
    private static ActionDef setStruct1Def;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTION_MODEL), NS);
        NullyExecutor nullyExecutor = new NullyExecutor();

        TypeName struct1Name = TypeNameFactory.makeName("struct1", VERS, NS);
        struct1Def = (StructDef) actionModel.getType(struct1Name);

        SimpleTypeName getThingName = (SimpleTypeName) TypeNameFactory
                .makeName("getThing", VERS, NS);
        actionModel.registerExecutor(getThingName, callbackHandler);
        getThingDef = (ActionDef) actionModel.getType(getThingName);
        SimpleTypeName setThingName = (SimpleTypeName) TypeNameFactory
                .makeName("setThing", VERS, NS);
        actionModel.registerExecutor(setThingName, nullyExecutor);
        setThingDef = (ActionDef) actionModel.getType(setThingName);
        SimpleTypeName getThingBagName = (SimpleTypeName) TypeNameFactory
                .makeName("getThingBag", VERS, NS);
        actionModel.registerExecutor(getThingBagName, callbackHandler);
        getThingBagDef = (ActionDef) actionModel.getType(getThingBagName);
        SimpleTypeName setThingBagName = (SimpleTypeName) TypeNameFactory
                .makeName("setThingBag", VERS, NS);
        actionModel.registerExecutor(setThingBagName, callbackHandler);
        setThingBagDef = (ActionDef) actionModel.getType(setThingBagName);
        SimpleTypeName getThingListName = (SimpleTypeName) TypeNameFactory
                .makeName("getThingList", VERS, NS);
        actionModel.registerExecutor(getThingListName, callbackHandler);
        getThingListDef = (ActionDef) actionModel.getType(getThingListName);
        SimpleTypeName setThingListName = (SimpleTypeName) TypeNameFactory
                .makeName("setThingList", VERS, NS);
        actionModel.registerExecutor(setThingListName, callbackHandler);
        setThingListDef = (ActionDef) actionModel.getType(setThingListName);
        SimpleTypeName getThingSetName = (SimpleTypeName) TypeNameFactory
                .makeName("getThingSet", VERS, NS);
        actionModel.registerExecutor(getThingSetName, callbackHandler);
        getThingSetDef = (ActionDef) actionModel.getType(getThingSetName);
        SimpleTypeName setThingSetName = (SimpleTypeName) TypeNameFactory
                .makeName("setThingSet", VERS, NS);
        actionModel.registerExecutor(setThingSetName, callbackHandler);
        setThingSetDef = (ActionDef) actionModel.getType(setThingSetName);
        SimpleTypeName getStruct1Name = (SimpleTypeName) TypeNameFactory
                .makeName("getStruct1", VERS, NS);
        actionModel.registerExecutor(getStruct1Name, callbackHandler);
        getStruct1Def = (ActionDef) actionModel.getType(getStruct1Name);
        SimpleTypeName setStruct1Name = (SimpleTypeName) TypeNameFactory
                .makeName("setStruct1", VERS, NS);
        actionModel.registerExecutor(setStruct1Name, callbackHandler);
        setStruct1Def = (ActionDef) actionModel.getType(setStruct1Name);
    }

    @AfterClass
    public static void teardown()
            throws Exception {
        palBridge.shutdown();
    }

    /**
     * The guts of the test. Learn and execute a procedure consisting of the two
     * actions.
     *
     * @param procName
     *            the procedure name
     * @param act1
     *            the first action to demonstrate
     * @param arg1
     *            the argument to assign to the first action
     * @param act2
     *            the second action to demonstrate
     * @param arg2
     *            the argument to assign to the second action
     * @param execArg
     *            argument to pass to the procedure when it's executed
     * @throws Exception
     *             if something goes wrong
     */
    private void nullProc(String procName,
                          ActionDef act1,
                          Object arg1,
                          ActionDef act2,
                          Object arg2,
                          Object execArg)
            throws Exception {
        List<ActionStreamEvent> events = new ArrayList<ActionStreamEvent>();
        ActionStreamEvent event1 = act1.invoke(null);
        event1.setValue(0, arg1);
        events.add(event1);
        ActionStreamEvent event2 = act2.invoke(null);
        event2.setValue(0, arg2);
        events.add(event2);
        ProcedureDef proc = ProcedureLearner.learnProcedure(events, procName);
        ProcedureInvocation invoc = proc.invoke(null);
        invoc.setValue(0, execArg);
        invoc.start();
        invoc.waitUntilFinished();
        Assert.assertEquals(Status.ENDED, invoc.getStatus());
    }

    /**
     * CPAL-181, CPAL-185. Pass a null argument to a procedure.
     */
    @Test(timeOut = 10000)
    public void nullSimple()
            throws Exception {
        nullProc("nullSimple", setThingDef, "foo", getThingDef, "bar", null);
    }

    /**
     * CPAL-185. Pass a null bag to a procedure, and have it return a null one.
     */
    @Test(timeOut = 10000)
    public void nullBag()
            throws Exception {
        List<String> bag1 = new ArrayList<String>();
        bag1.add("foo");
        List<String> bag2 = new ArrayList<String>();
        bag2.add("bar");
        nullProc("nullBag", setThingBagDef, bag1, getThingBagDef, bag2, null);
    }

    /**
     * CPAL-185. Pass a null list to a procedure, and have it return a null one.
     */
    @Test(timeOut = 10000)
    public void nullList()
            throws Exception {
        List<String> list1 = new ArrayList<String>();
        list1.add("foo");
        List<String> list2 = new ArrayList<String>();
        list2.add("bar");
        nullProc("nullList", setThingListDef, list1, getThingListDef, list2,
                null);
    }

    /**
     * CPAL-185. Pass a null set to a procedure, and have it return a null one.
     */
    @Test(timeOut = 10000)
    public void nullSet()
            throws Exception {
        Set<String> set1 = new HashSet<String>();
        set1.add("foo");
        Set<String> set2 = new HashSet<String>();
        set2.add("bar");
        nullProc("nullSet", setThingSetDef, set1, getThingSetDef, set2, null);
    }

    /**
     * CPAL-185. Pass a null struct to a procedure, and have it return a null one.
     */
    @Test(timeOut = 10000)
    public void nullStruct()
            throws Exception {
        Struct struct1 = struct1Def.newInstance();
        struct1.setValue(0, "foo1");
        struct1.setValue(1, "foo2");
        Struct struct2 = struct1Def.newInstance();
        struct2.setValue(0, "bar1");
        struct2.setValue(1, "bar2");
        nullProc("nullStruct", setStruct1Def, struct1, getStruct1Def, struct2,
                null);
    }

    /**
     * CPAL-185. Pass a bag with a null element.
     */
    @Test(timeOut = 10000)
    public void nullBagElement()
            throws Exception {
        List<String> bag1 = new ArrayList<String>();
        bag1.add("foo");
        List<String> bag2 = new ArrayList<String>();
        bag2.add("bar");
        List<String> nullyBag = new ArrayList<String>();
        nullyBag.add(null);
        nullyBag.add("baz");
        nullProc("nullBagElement", setThingBagDef, bag1, getThingBagDef, bag2, nullyBag);
    }

    /**
     * CPAL-185. Pass a list with a null element.
     */
    @Test(timeOut = 10000)
    public void nullListElement()
            throws Exception {
        List<String> list1 = new ArrayList<String>();
        list1.add("foo");
        List<String> list2 = new ArrayList<String>();
        list2.add("bar");
        List<String> nullyList = new ArrayList<String>();
        nullyList.add(null);
        nullyList.add("baz");
        nullProc("nullListElement", setThingListDef, list1, getThingListDef,
                list2, nullyList);
    }

    /**
     * CPAL-185. Pass a set with a null element.
     */
    @Test(timeOut = 10000)
    public void nullSetElement()
            throws Exception {
        Set<String> set1 = new HashSet<String>();
        set1.add("foo");
        Set<String> set2 = new HashSet<String>();
        set2.add("bar");
        Set<String> nullySet = new HashSet<String>();
        nullySet.add(null);
        nullySet.add("baz");
        nullProc("nullSetElement", setThingSetDef, set1, getThingSetDef, set2,
                nullySet);
    }

    /**
     * CPAL-185. Pass a struct with a null element.
     */
    @Test(timeOut = 10000)
    public void nullStructElement()
            throws Exception {
        Struct struct1 = struct1Def.newInstance();
        struct1.setValue(0, "foo1");
        struct1.setValue(1, "foo2");
        Struct struct2 = struct1Def.newInstance();
        struct2.setValue(0, "bar1");
        struct2.setValue(1, "bar2");
        Struct nullyStruct = struct1Def.newInstance();
        nullyStruct.setValue(0, null);
        nullyStruct.setValue(1, "baz");
        nullProc("nullStructElement", setStruct1Def, struct1, getStruct1Def,
                struct2, nullyStruct);
    }
}
