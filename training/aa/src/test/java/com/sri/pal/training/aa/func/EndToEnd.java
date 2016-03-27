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
package com.sri.pal.training.aa.func;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionExecutor;
import com.sri.pal.ActionModel;
import com.sri.pal.ActionModelDef;
import com.sri.pal.Bridge;
import com.sri.pal.TypeStorage;
import com.sri.pal.training.aa.Assessor;
import com.sri.pal.training.aa.SymbolManager;
import com.sri.pal.training.core.exercise.Exercise;
import com.sri.pal.training.core.response.Response;
import com.sri.pal.training.core.storage.ExerciseFactory;
import com.sri.pal.upgrader.MemoryTypeStorage;

/**
 * This is the (JUnit) point of entry for all the end-to-end functional tests of
 * automated assessment plus the pattern matcher. It reads test cases from a
 * properties file, where each test case contains inputs and expected outputs.
 */
public class EndToEnd {
    private static final String TESTCASES = "testcases.properties";
    private static final String TEST = "test";
    private static final String ACTION_MODEL = "actionModel";
    private static final String NAMESPACE = "namespace";
    private static final String EXERCISE = "exercise";
    private static final String RESPONSE = "response";
    private static final String APPLICATION = "application";
    private static final String COST = "cost";
    private static final String ERROR = "error";
    private static final String MAX_EXPANSIONS = "maxExpansions";
    private static final String CSV_FILE = "EndToEnd.csv";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static Bridge bridge;
    private static ActionModel am;
    private static Assessor assessor;
    private static List<Integer> expansions = new ArrayList<Integer>();

    @BeforeClass
    public static void setup()
            throws Exception {
        Bridge.startPAL();
        bridge = Bridge.newInstance(EndToEnd.class.getSimpleName());
        TypeStorage testStorage = new MemoryTypeStorage();
        bridge.setTypeStorage(testStorage);
        am = bridge.getActionModel();
        assessor = new Assessor(bridge);
    }

    @AfterClass
    public static void shutdown()
            throws Exception {
        bridge.shutdown();
    }

    /**
     * Read all the test cases and execute them.
     *
     * @throws Exception
     *             if something goes wrong
     */
    @DataProvider(name = "test cases")
    public Object[][] loadTestCases()
            throws Exception {
        List<Object[]> result = new ArrayList<Object[]>();

        Properties props = new Properties();
        InputStream in = getClass().getResourceAsStream(TESTCASES);
        try {
            props.load(in);
        } finally {       
            in.close();
        }
        for (int tn = 0;; tn++) {
            try {
                String testName = TEST + "." + tn + ".";
                String amStr = props.getProperty(testName + ACTION_MODEL);
                if (amStr == null) {
                    break;
                }
                URL amUrl = getClass().getResource(amStr);
                if (amUrl == null) {
                    Assert.fail("Couldn't load " + amStr + " from "
                            + System.getProperty("java.class.path"));
                }

                String namespace = props.getProperty(testName + NAMESPACE);

                String exeStr = props.getProperty(testName + EXERCISE);
                URL exeUrl = getClass().getResource(exeStr);
                /* Cheap null check: */
                exeUrl.toURI();

                String respStr = props.getProperty(testName + RESPONSE);
                URL respUrl = getClass().getResource(respStr);
                /* Cheap null check: */
                respUrl.toURI();

                String applStr = props.getProperty(testName + APPLICATION);
                ActionExecutor appl = null;
                if (applStr != null && !applStr.equals("")) {
                    Class<?> rawApplClazz = Class.forName(applStr);
                    Class<? extends ActionExecutor> applClazz = rawApplClazz
                            .asSubclass(ActionExecutor.class);
                    Constructor<? extends ActionExecutor> constr = applClazz
                            .getConstructor(Bridge.class);
                    appl = constr.newInstance(bridge);
                }

                double cost = -1;
                String costStr = props.getProperty(testName + COST);
                if (costStr != null && !costStr.equals("")) {
                    cost = Double.parseDouble(costStr);
                }

                String errStr = props.getProperty(testName + ERROR);
                Class<?> error = null;
                if (errStr != null && !errStr.equals("")) {
                    error = Class.forName(errStr);
                }

                int maxExpansions = 0;
                String maxExpStr = props.getProperty(testName + MAX_EXPANSIONS);
                if (maxExpStr != null && !maxExpStr.equals("")) {
                    maxExpansions = Integer.parseInt(maxExpStr);
                }

                result.add(new Object[] { amUrl, namespace, exeUrl, respUrl,
                        appl, cost, error, maxExpansions });
            } catch (Throwable e) {
                log.error("test case " + tn, e);
                throw new RuntimeException("Working on test case " + tn, e);
            }
        }

        return result.toArray(new Object[0][]);
    }

    @AfterClass
    public static void writeCSV()
            throws IOException {
        /* Write out results to a CSV file. */
        File csv = new File(CSV_FILE);
        PrintWriter out = new PrintWriter(new FileWriter(csv));
        try {
            for (int i = 0; i < expansions.size(); i++) {
                out.print(i);
                if (i < expansions.size() - 1) {
                    out.print(",");
                }
            }
            out.println("");
            for (int i = 0; i < expansions.size(); i++) {
                int nx = expansions.get(i);
                out.print(nx);
                if (i < expansions.size() - 1) {
                    out.print(",");
                }
            }
            out.println("");
        } finally {
            out.close();
        }
    }

    /**
     * Run the indicated test case.
     */
    @Test(dataProvider = "test cases")
    public void oneTest(URL actionModelUrl,
                        String namespace,
                        URL exerciseUrl,
                        URL responseUrl,
                        ActionExecutor application,
                        double costExpected,
                        Class<?> errExpected,
                        int maxExpansions)
            throws Exception {
        /* Load the exercise. */
        Unmarshaller um = ExerciseFactory.getUnmarshaller();
        @SuppressWarnings("unchecked")
        JAXBElement<Exercise> exeEle = (JAXBElement<Exercise>) um
                .unmarshal(new File(exerciseUrl.toURI()));
        Exercise exercise = exeEle.getValue();
        /* Load the response. */
        @SuppressWarnings("unchecked")
        JAXBElement<Response> respEle = (JAXBElement<Response>) um
                .unmarshal(new File(responseUrl.toURI()));
        Response response = respEle.getValue();

        /* Load the action model. */
        Set<ActionModelDef> loaded = am.load(actionModelUrl, namespace);

        /*
         * Register the application as executor for all actions. We really only
         * care about potential predicates, but it shouldn't hurt to register
         * for everything.
         */
        for (ActionModelDef amDef : loaded) {
            if (amDef instanceof ActionDef) {
                ActionDef actDef = (ActionDef) amDef;
                if (am.getExecutor(actDef.getName()) == null) {
                    am.registerExecutor(actDef.getName(), application);
                }
            }
        }

        /* Perform the assessment. */
        Throwable errObserved = null;
        try {
            // TODO should iterate through tasks/task responses
            assessor.assessTask(
                    exercise.getProblem().getTasks().get(0),
                    exercise.getSolution().getTaskSolutions().get(0),
                    response.getTaskResponses().get(0),
                    new SymbolManager(bridge));

            int numExpansions = assessor.lastExecInfo().numExpansions();
            expansions.add(numExpansions);

            if (maxExpansions != 0) {
                Assert.assertTrue(numExpansions <= maxExpansions, numExpansions
                        + " !< " + maxExpansions);
            }
        } catch (Exception e) {
            errObserved = e;
        }

        /* Check for expected result or error. */
        if (errObserved != null && errExpected != null) {
            if (!errExpected.isInstance(errObserved)) {
                Assert.fail(namespace + ": Expected " + errExpected + ", got "
                        + errObserved);
            }
        } else if(errObserved == null && errExpected != null) {
            Assert.fail("Expected error (" + errExpected + "), got none");
        } else if(errObserved != null && errExpected == null) {
            throw new RuntimeException("Unexpected error", errObserved);
        } else if (costExpected != -1) {
            double costObserved = assessor.lastMatch().cost();
            Assert.assertEquals(costObserved, costExpected, 0.1, namespace);
        }
    }
}
