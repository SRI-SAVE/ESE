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

// $Id: MemLeak_PerfTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.events;

import java.util.ArrayList;
import java.util.List;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionModel;
import com.sri.pal.Bridge;
import com.sri.pal.Learner;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MemLeak_PerfTest
        extends PALTestCase {
    private static Bridge palBridge;
    private static Learner learningBridge;
    private int numRuns = 0;
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");


    @BeforeClass
    public static void init()
            throws Exception {
        ProcedureLearner.buildBridge();
        learningBridge = ProcedureLearner.learningBridge;
        palBridge = ProcedureLearner.palBridge;
    }

    /**
     * This long-running test assists in looking for memory leaks. Disabled by
     * default because it's designed to work in combination with a debugger, and
     * it runs for a long time.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test(enabled = false)
    public void testRepeatProcedure()
            throws Exception {
        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();
        SimpleTypeName actionId = (SimpleTypeName) TypeNameFactory.makeName(
                "action202", "1.0", "TESTNS");
        ActionModel actionModel = palBridge.getActionModel();
        ActionDef actDef = (ActionDef) actionModel.getType(actionId);
        for(int i = 0; i < 1; i++) {
            actions.add(actDef.invoke(null, "entityName", "attribute",
                "newValue" + i));
        }
        ProcedureDef task = learningBridge.learn("testRepeatProcedure", null,
                actions.toArray(new ActionInvocation[0]));

        Thread thread = new Thread() {
            public void run() {
                int currentRuns = 0, prevRuns;
                while(true) {
                    prevRuns = currentRuns;
                    currentRuns = numRuns;
                    log.info("STATS: " + currentRuns + "," + (currentRuns - prevRuns));

                    try {
                        Thread.sleep(5000);
                    } catch(InterruptedException e) { }
                }
            }
        };
        thread.setDaemon(true);
        thread.start();

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 1000 * 60 * 40;
        while (System.currentTimeMillis() < endTime) {
            ProcedureInvocation proc = task.invoke(null);
            proc.start();
            proc.waitUntilFinished();
            numRuns++;
        }
    }
}
