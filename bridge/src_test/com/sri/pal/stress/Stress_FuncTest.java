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

// $Id: Stress_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.stress;

import java.net.URL;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionModel;
import com.sri.pal.Bridge;
import com.sri.pal.Learner;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * We're trying to reproduce CPAL-124. Under heavy load, the PAL JVM memory
 * footprint appears to increase, indicating a possible memory leak. The example
 * fires off a new procedure execution every time a particular action is called.
 *
 * @author chris
 */
public class Stress_FuncTest
        extends PALTestCase {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    private static final String NAMESPACE = "ns";
    private static final int MIN_RUNS = 1000;

    private static Bridge bridge;

    @BeforeClass
    public static void start()
            throws Exception {
        Bridge.startPAL();
        bridge = Bridge.newInstance("sft");
    }

    @AfterClass
    public static void shutdown() throws PALRemoteException {
        bridge.shutdown();
    }

    @Test
    public void stress()
            throws Exception {
        int parallelism = 50;
        int reps = 10; // Change to 600 for a longer, better test.

        ProcedureLearner.setStorage(getClass(), bridge);
        ActionModel actionModel = bridge.getActionModel();
        URL url = ActionModels.class.getResource(ActionModels.STRESS);
        ExecutionCounter counter = new ExecutionCounter();
        bridge.addActionListener(counter);
        RecursiveExecutor exec = new RecursiveExecutor();
        actionModel.load(url, NAMESPACE);

        Learner learner = bridge.getLearner();
        SimpleTypeName actName = (SimpleTypeName) TypeNameFactory.makeName(
                "action1", "1.0", NAMESPACE);
        actionModel.registerExecutor(actName, exec);
        ActionDef ad = (ActionDef) actionModel.getType(actName);
        ActionInvocation action = ad.invoke(null, "a");
        ProcedureDef procDef = learner.learn("stress", null, action);

        log.info("Learned procedure: {}", procDef.getSource());
        exec.setProcedure(procDef);

        for (int i = 0; i < parallelism; i++) {
            ProcedureInvocation invoc = procDef.invoke(null, "" + i);
            invoc.start();
        }

        Runtime runtime = Runtime.getRuntime();
        for (int i = 0; i < reps; i++) {
            long memUsed = runtime.totalMemory() - runtime.freeMemory();
            int procs = counter.getNumRunning();
            String msg = "mem: " + memUsed + "\trunning: " + procs
                    + "\tstarted: " + counter.getTotalStarted();
            log.info(msg);

            Thread.sleep(1000);
        }

        exec.stop();
        log.info("stop");

        for (int i = 0; i < (reps / 10) + 10; i++) {
            long memUsed = runtime.totalMemory() - runtime.freeMemory();
            int procs = counter.getNumRunning();
            String msg = "mem: " + memUsed + "\trunning: " + procs
                    + "\tstarted: " + counter.getTotalStarted();
            log.info(msg);

            Thread.sleep(1000);
        }

        log.info("still running: {}", counter.runningSet());
        Assert.assertEquals(counter.getNumRunning(), 0);
        int totalRuns = counter.getTotalStarted();
        Assert.assertTrue(totalRuns > MIN_RUNS, "only ran " + totalRuns);
        Assert.assertEquals(counter.getNumErrors(), 0);
    }
}
