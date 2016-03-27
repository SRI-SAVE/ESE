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

// $Id: Agave_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Agave_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = "agavetest";

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.ACTIONS);
        setup(url, NAMESPACE);
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    /*
     * TODO This never got finished; it's a placeholder. Probably just want to
     * do a very simple test of Agave inserting completer actions in a
     * demonstration, since it should be more fully tested in LAPDOG itself. We
     * just want to show that it works in the Bridge's environment.
     */
    @Test(timeOut = 10000, enabled = false)
    public void procWithAgaveActions()
            throws Exception {
        List<ActionInvocation> events = new ArrayList<ActionInvocation>();

        SimpleTypeName act1Name = (SimpleTypeName) TypeNameFactory.makeName(
                "act1", "1.0", NAMESPACE);
        ActionDef act1Def = (ActionDef) actionModel.getType(act1Name);
        ActionInvocation act1 = act1Def.invoke(null);
        events.add(act1);

        SimpleTypeName act2Name = (SimpleTypeName) TypeNameFactory.makeName(
                "act2", "1.0", ActionModel.AGAVE_NAMESPACE);
        ActionDef act2Def = (ActionDef) actionModel.getType(act2Name);
        ActionInvocation act2 = act2Def.invoke(null);
        events.add(act2);

        ProcedureLearner.learnAndInvokeProcedure(events, "agaveproc");
    }
}
