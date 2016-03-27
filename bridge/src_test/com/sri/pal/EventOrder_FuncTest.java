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

// $Id: EventOrder_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.net.URL;

import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.actionmodels.DebugExecutor;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class EventOrder_FuncTest
        extends PALTestCase {
    private static final String NAMESPACE = "ns";

    private static Bridge palBridge;
    private static ActionModel actionModel;
    private static ActionDef logDef;
    private static ActionDef sleepDef;
    private static DebugExecutor executor;
    private static Learner learner;

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.DEBUG);
        Bridge.startPAL();
        palBridge = Bridge.newInstance("eoft");
        ProcedureLearner.setStorage(EventOrder_FuncTest.class, palBridge);
        actionModel = palBridge.getActionModel();
        executor = new DebugExecutor();
        actionModel.load(url, NAMESPACE);
        learner = palBridge.getLearner();

        SimpleTypeName logName = (SimpleTypeName) TypeNameFactory.makeName(
                "log", "1.0", NAMESPACE);
        SimpleTypeName sleepName = (SimpleTypeName) TypeNameFactory.makeName(
                "sleep", "1.0", NAMESPACE);
        actionModel.registerExecutor(logName, executor);
        actionModel.registerExecutor(sleepName, executor);
        logDef = (ActionDef) actionModel.getType(logName);
        sleepDef = (ActionDef) actionModel.getType(sleepName);
    }

    @AfterClass
    public static void shutdown() throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * https://jira.esd.sri.com/browse/CPAL-76 Events are being delivered too
     * asynchronously: A long-running action is not allowed to complete before
     * the next action is called.
     */
    @Test
    public void cpal76()
            throws Exception {
        ActionInvocation[] demo = new ActionInvocation[3];
        demo[0] = logDef.invoke(null, "before sleep");
        demo[1] = sleepDef.invoke(null, 3000l);
        demo[2] = logDef.invoke(null, "after sleep");
        ProcedureDef proc = learner.learn("cpal76", null, demo);

        ProcedureInvocation invoc = proc.invoke(null);
        invoc.start();
        invoc.waitUntilFinished();

        assertEquals(Status.ENDED, invoc.getStatus());
        assertEquals(3, executor.numExecuted());
        assertEquals(logDef, executor.getExecuted(0).getDefinition());
        assertEquals(sleepDef, executor.getExecuted(1).getDefinition());
        assertEquals(logDef, executor.getExecuted(2).getDefinition());
        assertTrue(executor.getStart(1) >= executor.getEnd(0));
        assertTrue(executor.getStart(2) >= executor.getEnd(1));
    }
}
