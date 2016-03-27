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

// $Id: DeeplyNestedProcs_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.util.LogUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DeeplyNestedProcs_FuncTest {
    private static final Logger log = LoggerFactory
            .getLogger(DeeplyNestedProcs_FuncTest.class);

    private static Bridge palBridge;
    private static ActionModel actionModel;
    private static VerifiableCallbackHandler callbackHandler;
    private static Learner learningBridge;

    @BeforeClass
    public static void initialize()
            throws Exception {
        LogUtil.configureLogging(PALTestCase.LOG_CONFIG_QUIET,
                PALTestCase.class);
        PALBridgeTestCase.setup();
        palBridge = PALBridgeTestCase.palBridge;
        actionModel = PALBridgeTestCase.actionModel;
        callbackHandler = PALBridgeTestCase.callbackHandler;
        learningBridge = PALBridgeTestCase.learningBridge;
    }

    @AfterClass
    public static void shutdown()
            throws Exception {
        palBridge.shutdown();
    }

    /**
     * Build a very deeply nested procedure, and run it.
     */
    @Test
    public void deeplyNested()
            throws Exception {
        int nestingLevel = 100; // Reduce this if the test slows too much.
        int reps = 1000;

        long beforeLearn = System.currentTimeMillis();

        // Build the bottom-level procedure first.
        SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName(
                "action144", "1.0", ProcedureLearner.NAMESPACE);
        actionModel.registerExecutor(name, callbackHandler);
        ActionDef action = (ActionDef) actionModel.getType(name);
        ActionInvocation invoc = action.invoke(null, "arg0");
        invoc.setValue(1, "arg1");
        ProcedureDef proc0 = learningBridge.learn("proc0", null, invoc);

        // Now build the stack of nested procedures.
        ProcedureDef lowerProc = proc0;
        for (int procNum = 1; procNum <= nestingLevel; procNum++) {
            ProcedureInvocation lowerInvoc = lowerProc.invoke(null);
            for (int argNum = 0; argNum < lowerProc.size(); argNum++) {
                lowerInvoc.setValue(argNum, "arg" + argNum);
            }
            ProcedureDef newProc = learningBridge.learn("proc" + procNum, null,
                    lowerInvoc);
            lowerProc = newProc;
        }
        ProcedureDef topProc = proc0;

        long afterLearn = System.currentTimeMillis();
        log.info("Learning took {} ms.", afterLearn - beforeLearn);

        // Repeatedly run the highest-level procedure.
        ProcedureInvocation invocs[] = new ProcedureInvocation[reps];
        for (int rep = 0; rep < reps; rep++) {
            invocs[rep] = topProc.invoke(null);
            invocs[rep].start();
        }
        for (ProcedureInvocation procInvoc : invocs) {
            procInvoc.waitUntilFinished();
            Assert.assertEquals(Status.ENDED, procInvoc.getStatus());
        }

        long afterExec = System.currentTimeMillis();
        log.info("Executing took {} ms.", afterExec - afterLearn);
    }
}
