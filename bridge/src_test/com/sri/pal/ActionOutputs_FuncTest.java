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

// $Id: ActionOutputs_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.actionmodels.DebugExecutor;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ActionOutputs_FuncTest
        extends PALTestCase {
    private static final String NAMESPACE = "ns";

    private static Bridge palBridge;
    private static ActionModel actionModel;
    private static ActionDef logDef;
    private static ActionDef concatDef;
    private static DebugExecutor executor;
    private static Learner learner;
    private static ActionDef makeStringDef;

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.DEBUG);
        Bridge.startPAL();
        palBridge = Bridge.newInstance("aoft");
        ProcedureLearner.setStorage(ActionOutputs_FuncTest.class, palBridge);
        actionModel = palBridge.getActionModel();
        executor = new DebugExecutor();
        actionModel.load(url, NAMESPACE);
        learner = palBridge.getLearner();

        SimpleTypeName logName = (SimpleTypeName) TypeNameFactory.makeName(
                "log", "1.0", NAMESPACE);
        SimpleTypeName concatName = (SimpleTypeName) TypeNameFactory.makeName(
                "concat", "1.0", NAMESPACE);
        SimpleTypeName makeStringName = (SimpleTypeName) TypeNameFactory
                .makeName("makeString", "1.0", NAMESPACE);
        actionModel.registerExecutor(logName, executor);
        actionModel.registerExecutor(concatName, executor);
        actionModel.registerExecutor(makeStringName, executor);
        logDef = (ActionDef) actionModel.getType(logName);
        concatDef = (ActionDef) actionModel.getType(concatName);
        makeStringDef = (ActionDef) actionModel.getType(makeStringName);
    }

    @AfterClass
    public static void shutdown() throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * https://jira.esd.sri.com/browse/CPAL-77 Actions in a procedure which
     * produce output cause the procedure to fail, if the output actions aren't
     * last in the procedure.
     */
    @Test
    public void cpal77()
            throws Exception {
        List<ActionInvocation> demo = new ArrayList<ActionInvocation>();
        demo.add(logDef.invoke(null, "before makeString"));
        ActionInvocation action = makeStringDef.invoke(null);
        action.setValue(0, "foo");
        demo.add(action);
        demo.add(logDef.invoke(null, "before concat"));
        action = concatDef.invoke(null, "foo", "bar");
        action.setValue(2, "foobar");
        demo.add(action);
        demo.add(logDef.invoke(null, "after concat"));
        ProcedureDef proc = learner.learn("cpal77", null,
                demo.toArray(new ActionInvocation[0]));

        ProcedureInvocation invoc = proc.invoke(null);
        invoc.start();
        invoc.waitUntilFinished();

        assertEquals(Status.ENDED, invoc.getStatus());
        assertEquals(demo.size(), executor.numExecuted());
        assertEquals(logDef, executor.getExecuted(0).getDefinition());
        assertEquals(makeStringDef, executor.getExecuted(1).getDefinition());
        assertEquals(logDef, executor.getExecuted(2).getDefinition());
        assertEquals(concatDef, executor.getExecuted(3).getDefinition());
        assertEquals(logDef, executor.getExecuted(4).getDefinition());
    }
}
