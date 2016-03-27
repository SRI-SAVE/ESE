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

// $Id: PermutableList_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.events;

import static org.testng.AssertJUnit.assertTrue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Issues associated with learning loops over non-ordered demonstrations. <br>
 * https://jira.esd.sri.com/browse/CPAL-96
 *
 * @author chris
 */
public class PermutableList_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = "CPAL96";
    private static final String ACTION_MODEL = ActionModels.PERMUTABLE_LISTS;

    private static ActionDef defMakePList;
    private static ActionDef defUsePList;
    private static ActionDef defUseString;

    @BeforeClass
    public static void setup()
            throws RemoteException,
            PALException {
        setup(ActionModels.class.getResource(ACTION_MODEL), NAMESPACE);
        SimpleTypeName makePListName = (SimpleTypeName) TypeNameFactory
                .makeName("makePList", "1.0", NAMESPACE);
        actionModel.registerExecutor(makePListName, callbackHandler);
        defMakePList = (ActionDef) actionModel.getType(makePListName);

        SimpleTypeName usePListName = (SimpleTypeName) TypeNameFactory
                .makeName("usePList", "1.0", NAMESPACE);
        actionModel.registerExecutor(usePListName, callbackHandler);
        defUsePList = (ActionDef) actionModel.getType(usePListName);

        SimpleTypeName useStringName = (SimpleTypeName) TypeNameFactory
                .makeName("useString", "1.0", NAMESPACE);
        actionModel.registerExecutor(useStringName, callbackHandler);
        defUseString = (ActionDef) actionModel.getType(useStringName);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * Simple test to make and then consume a permutable list.
     *
     * @throws Exception
     */
    @Test
    public void makePList()
            throws Exception {
        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();
        ActionInvocation action;

        List<String> pList = new ArrayList<String>();
        pList.add("foo");
        pList.add("bar");
        action = defMakePList.bindAll(null, pList);
        actions.add(action);

        action = defUsePList.bindAll(null, pList);
        actions.add(action);

        ProcedureLearner.learnAndInvokeProcedure(actions, "makePList");
    }

    /**
     * Learn a loop from a permutable list, traversed in order.
     */
    @Test
    public void orderedLoop()
            throws Exception {
        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();
        ActionInvocation action;

        List<String> pList = new ArrayList<String>();
        pList.add("item1");
        pList.add("item2");
        action = defMakePList.bindAll(null, pList);
        actions.add(action);

        action = defUseString.bindAll(null, "item1");
        actions.add(action);

        action = defUseString.bindAll(null, "item2");
        actions.add(action);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(actions,
                "orderedLoop");
        String procSource = proc.getSource();
        assertTrue(procSource.contains("forall"));
    }

    /**
     * Learn a loop from a permutable list, traversed out of order.
     */
    @Test
    public void unorderedLoop()
            throws Exception {
        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();
        ActionInvocation action;

        List<String> pList = new ArrayList<String>();
        pList.add("item1");
        pList.add("item2");
        action = defMakePList.bindAll(null, pList);
        actions.add(action);

        action = defUseString.bindAll(null, "item2");
        actions.add(action);

        action = defUseString.bindAll(null, "item1");
        actions.add(action);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(actions,
                "unorderedLoop");
        String procSource = proc.getSource();
        assertTrue(procSource.contains("forall"));
    }
}
