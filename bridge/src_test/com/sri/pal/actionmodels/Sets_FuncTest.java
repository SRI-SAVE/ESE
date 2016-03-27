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

// $Id: Sets_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.actionmodels;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Sets_FuncTest
        extends PALBridgeTestCase {
    private static final String ACTIONS_FILE = ActionModels.SETS;
    private static final String NAMESPACE = "testApp";

    private static SimpleTypeName actName1;
    private static SimpleTypeName actName2;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTIONS_FILE), NAMESPACE);
        actName1 = (SimpleTypeName) TypeNameFactory.makeName("action1", "1.0",
                NAMESPACE);
        actName2 = (SimpleTypeName) TypeNameFactory.makeName("action2", "1.0",
                NAMESPACE);
        actionModel.registerExecutor(actName1, callbackHandler);
        actionModel.registerExecutor(actName2, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void simpleTask()
            throws Exception {
        ActionDef actDef1 = (ActionDef) actionModel.getType(actName1);
        ActionDef actDef2 = (ActionDef) actionModel.getType(actName2);

        Set<Object> value1 = new HashSet<Object>();
        value1.add("foo");
        value1.add("bar");
        Set<Object> value2 = new HashSet<Object>();
        value2.add(value1);

        ActionInvocation action1 = actDef1.invoke(null, value1);
        ActionInvocation action2 = actDef2.invoke(null, value2);

        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();
        actions.add(action1);
        actions.add(action2);
        ProcedureDef procDef = ProcedureLearner.learnProcedure(actions, "simpleTask");

        ProcedureLearner.invokeTask(procDef, actions, false);
    }
}
