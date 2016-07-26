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

package com.sri.pal;

import com.sri.ai.tasklearning.lapdog.LapdogConfiguration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.util.PALBridgeTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Try to fully mimic the usage patterns for the BAD AC project. Procedures can be learned with either the specific or
 * general actions. The general action calls out to a Lumen procedure which is preloaded; that procedure dispatches to
 * a specific action at execution time, based on static properties on the specific actions and corresponding values in
 * a constraints object. The constraints object is passed to the procedure.
 *
 * @see LumenPreload_FuncTest
 */
public class GeneralizedActions_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory.getLogger(GeneralizedActions_FuncTest.class);

    private static final String NAMESPACE = "BAD_AC";
    private static final String VERS = "2.0";
    private static final String BAD_AC_DIR = "../doc/applications/badac";
    private static ActionDef detectRunwayDef;
    private static ActionDef importMsiDef;
    private static DetectorScorer detectorScorer;

    @BeforeClass
    public static void setup()
            throws Exception {
        // Check our data dir.
        File badAcDir = new File(BAD_AC_DIR);
        if (!badAcDir.isDirectory()) {
            badAcDir = new File("../" + BAD_AC_DIR);
            if (!badAcDir.isDirectory()) {
                throw new RuntimeException("Can't find data dir at " + BAD_AC_DIR);
            }
        }
        File amFile = new File(badAcDir, "BAD_AC_AF.xml");
        File preloadFile = new File(badAcDir, "detectorSelection.lumen");

        // Generate name objects that refer to action definitions we care about.
        SimpleTypeName importMsiName = new SimpleTypeName("importMSI", VERS, NAMESPACE);
        SimpleTypeName detectRunwayName = new SimpleTypeName("detectRunway", VERS, NAMESPACE);
        SimpleTypeName detectRunwayAltName = new SimpleTypeName("detectRunwayAlt", VERS, NAMESPACE);
        SimpleTypeName detectorScoreName = new SimpleTypeName("detectorScore", VERS, NAMESPACE);

        // These actions are prerequisites for the Lumen preload script. They're all actions that might be called by
        // DETECT_ENTITY/0, or the detectorScore function itself.
        List<String> prereqs = new ArrayList<>();
        prereqs.add(importMsiName.getFullName());
        prereqs.add(detectRunwayName.getFullName());
        prereqs.add(detectRunwayAltName.getFullName());
        prereqs.add(detectorScoreName.getFullName());

        // Tell Lumen to load the preload file, and that it depends on the prerequisites above.
        System.setProperty("PAL.lumen-preload-file", preloadFile.getCanonicalPath());
        System.setProperty("PAL.lumen-preload-prerequisites", String.join(",", prereqs));

        // Use the test harness to initialize the task learning system. A production application would use other calls.
        setup(amFile.toURI().toURL(), NAMESPACE);

        // Use the mock executor to handle these three actions. A production application would define its own callback
        // handler.
        actionModel.registerExecutor(importMsiName, callbackHandler);
        actionModel.registerExecutor(detectRunwayName, callbackHandler);
        actionModel.registerExecutor(detectRunwayAltName, callbackHandler);

        // This special callback handler decides how well an action fits the constraints.
        detectorScorer = new DetectorScorer();
        actionModel.registerExecutor(detectorScoreName, detectorScorer);

        importMsiDef = (ActionDef) actionModel.getType(importMsiName);
        detectRunwayDef = (ActionDef) actionModel.getType(detectRunwayName);
    }

    @AfterClass
    public static void teardown()
            throws Exception {
        palBridge.shutdown();
    }

    @Test
    public void generalProcedure()
            throws Exception {
        // These actions will be used as the demonstration, to learn a new procedure. The demonstration includes both
        // input and output parameters.
        List<ActionStreamEvent> actions = new ArrayList<>();

        // Action 1 imports the MSI file.
        String imageName = "test_image.msi";
        ActionInvocation action1 = importMsiDef.invoke(null, imageName);
        String imageId = "23";
        action1.setValue("image", imageId);
        actions.add(action1);

        // Action 2 looks for runways in the image.
        String classifier = "Alpha";
        ActionInvocation action2 = detectRunwayDef.invoke(null, imageId, classifier);
        List<String> roads = new ArrayList<>();
        roads.add("road1");
        roads.add("road2");
        action2.setValue("roads", roads);
        actions.add(action2);

        // Set learn properties to request the general procedure be learned, not the specific one.
        Properties learnProps = new Properties();
        learnProps.put("lapdog.idiom.learning", LapdogConfiguration.IdiomLearning.ABSTRACT.name());
        learnProps.put("lapdog.idiom.group-single-actions", "true");

        // Learn the procedure now.
        ProcedureDef proc = learningBridge.learn("general", learnProps, null, actions.toArray(new ActionStreamEvent[0]));
        log.info("Learned procedure: {}", proc.getSource());

        // Ensure it learned the general procedure.
        Assert.assertTrue(proc.getSource().contains("DETECT_ENTITY/0"), proc.getSource());

        // Execute the procedure we just learned. It should call our DetectorScorer to decide which detector action to
        // use.
        ProcedureInvocation invocation = proc.invoke(null);
        invocation.start();
        invocation.waitUntilFinished();

        // Was the procedure execution successful?
        Assert.assertEquals(invocation.getStatus(), ActionStreamEvent.Status.ENDED);

        // Make sure the detector scorer was actually called.
        Assert.assertTrue(detectorScorer.wasCalled());
    }

    private static class DetectorScorer
            implements ActionExecutor {
        private boolean wasCalled = false;

        @Override
        public void cancel(ActionStreamEvent event) {
            // Can be ignored, since the action runs synchronously.
        }

        @Override
        public void continueStepping(ActionInvocation invocation,
                                     ActionInvocation.StepCommand command,
                                     List<Object> actionArgs)
                throws PALException {
            // Can be ignored, since we don't support stepped execution.
        }

        @Override
        public void executeStepped(ActionInvocation invocation)
                throws PALException {
            execute(invocation);
        }

        @Override
        public void execute(ActionInvocation invocation)
                throws PALException {
            log.info("{} was called", invocation);

            wasCalled = true;

            // Retrieve the parameters for this invocation.
            String falseAlarmStr = (String) invocation.getValue("falseAlarmRate");
            String requiresMsiStr = (String) invocation.getValue("requiresMSIData");
            String viewpointStr = (String) invocation.getValue("viewpoint");
            String minSamplesStr = (String) invocation.getValue("minSamples");

            // For this test, we only care about the false alarm rate.
            Double falseAlarm = Double.parseDouble(falseAlarmStr);
            double score = 1 / falseAlarm;

            // Set the output parameter.
            invocation.setValue("score", score);

            // Tell the system that this execution is complete.
            invocation.setStatus(ActionStreamEvent.Status.ENDED);
        }

        boolean wasCalled() {
            return wasCalled;
        }
    }
}
