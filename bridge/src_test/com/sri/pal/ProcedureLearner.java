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

/* $Id: ProcedureLearner.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $ */
package com.sri.pal;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.upgrader.MemoryTypeStorage;
import com.sri.tasklearning.spine.Spine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Utilities for testing
 *
 * @author Valerie Wagner Date: Sep 6, 2006
 */
public class ProcedureLearner {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    public static final String NAMESPACE = "TESTNS";
    public static final String ACTION_MODEL = "./events/testns_action_model.xml";

    public static Learner learningBridge;
    public static ActionModel actionModel;
    private static List<? extends ActionStreamEvent> lastEventList;
    public static Bridge palBridge;
    public static VerifiableCallbackHandler callbackHandler;
    public static MemoryTypeStorage typeStorage;

    public static void buildBridge() throws RemoteException, PALException {
        URL url = ProcedureLearner.class.getResource(ACTION_MODEL);
        buildBridge(url, NAMESPACE);
    }

    public static void loadActionModel()
            throws PALException {
        URL url = ProcedureLearner.class.getResource(ACTION_MODEL);
        loadActionModel(url);
    }

    public static void loadActionModel(URL url)
            throws PALException {
        actionModel.load(url, NAMESPACE);
    }

    public static void buildBridge(String aModel, String nSpace) throws RemoteException, PALException {
        URL url = ProcedureLearner.class.getResource(aModel);
        buildBridge(url, nSpace);
    }

    public static void buildBridge(URL aModel, String nSpace) throws RemoteException, PALException {
        if (learningBridge == null) {
            Bridge.startPAL();
            palBridge = Bridge.newInstance("bridge-test");
            typeStorage = new MemoryTypeStorage();
            Assert.assertTrue(palBridge.setTypeStorage(typeStorage));
            actionModel = palBridge.getActionModel();
            callbackHandler = new VerifiableCallbackHandler();
            palBridge.addActionListener(callbackHandler);
            actionModel.load(aModel, nSpace);
            learningBridge = palBridge.getLearner();
        }
    }

    public static void buildBridgeNoStorage()
            throws PALException {
        if (learningBridge == null) {
            Bridge.startPAL();
            palBridge = Bridge.newInstance("bridge-test");
            actionModel = palBridge.getActionModel();
            callbackHandler = new VerifiableCallbackHandler();
            palBridge.addActionListener(callbackHandler);
            learningBridge = palBridge.getLearner();
        }
    }

    public static void setStorage(Class<?> clazz,
                                  Bridge bridge)
            throws PALException {
        String dirName = clazz.getSimpleName();
        File storageDir = new File(dirName);
        Spine spine = bridge.getSpine();
        TypeStorage storage = new FileTypeStorage(storageDir,
                spine.getClientId());
        if (!bridge.setTypeStorage(storage)) {
            throw new RuntimeException("Couldn't set storage");
        }
        removeDirs(storageDir);
        storageDir.mkdirs();
    }

    private static void removeDirs(File file) {
        if (file.isDirectory()) {
            for (File sub : file.listFiles()) {
                removeDirs(sub);
            }
        }
        file.delete();
    }

    public static void procedureNotLearned(ActionInvocation event, String name) throws RemoteException {
        boolean errorSeen = false;
        try {
            learnProcedure(event, name, false);
        } catch (PALException e) {
            errorSeen = true;
        }
        assertTrue("PALException should have been thrown and was not", errorSeen);
    }

    public static ProcedureDef learnAndInvokeProcedure(ActionStreamEvent event,
                                                       String procedureName)
            throws PALException,
            RemoteException {
        Vector<ActionStreamEvent> events = new Vector<ActionStreamEvent>();
        events.add(event);
        ProcedureDef task = learnAndInvokeProcedure(events, procedureName);
        return (task);
    }

    public static ProcedureDef learnAndInvokeProcedure(List<? extends ActionStreamEvent> events,
                                                       String procedureName)
            throws PALException,
            RemoteException {
        return (learnAndInvokeProcedure(events, procedureName, null));
    }

    public static ProcedureDef learnAndInvokeProcedure(List<? extends ActionStreamEvent> events,
                                                       String procedureName,
                                                       List<ActionInvocation> eventQueue)
            throws PALException,
            RemoteException {
        return learnAndInvokeProcedure(events, procedureName, null, eventQueue);
    }

    public static ProcedureDef learnAndInvokeProcedure(List<? extends ActionStreamEvent> events,
                                                       String procedureName,
                                                       Properties learnProps,
                                                       List<ActionInvocation> eventQueue)
            throws PALException,
            RemoteException {
        long beforeLearn = System.currentTimeMillis();
        ProcedureDef task = learnProcedure(events, procedureName, learnProps, true);
        long afterLearn = System.currentTimeMillis();

        long beforeInvoke = System.currentTimeMillis();
        invokeTask(task, eventQueue, true);
        long afterInvoke = System.currentTimeMillis();

        float learnTime = (float) (afterLearn - beforeLearn) / 1000F;
        float invokeTime = (float) (afterInvoke - beforeInvoke) / 1000F;
        log.info("Time to learn: " + learnTime);
        log.info("Time to invoke: " + invokeTime);

        return (task);
    }

    public static ProcedureDef learnProcedure(ActionStreamEvent event,
                                              String name)
            throws PALException,
            RemoteException {
        return learnProcedure(event, name, true);
    }

    public static ProcedureDef learnProcedure(ActionStreamEvent event,
                                              String name,
                                              boolean successExpected)
            throws PALException,
            RemoteException {
        List<ActionStreamEvent> list = new ArrayList<ActionStreamEvent>();
        list.add(event);
        return learnProcedure(list, name, successExpected);
    }

    public static ProcedureDef learnProcedure(List<? extends ActionStreamEvent> events,
                                              String name)
            throws PALException,
            RemoteException {
        return learnProcedure(events, name, true);
    }

    public static ProcedureDef learnProcedure(List<? extends ActionStreamEvent> events,
                                              String name,
                                              boolean successExpected)
            throws PALException,
            RemoteException {
        return learnProcedure(events, name, null, successExpected);
    }

    public static ProcedureDef learnProcedure(List<? extends ActionStreamEvent> events,
                                              String name,
                                              Properties learnProps,
                                              boolean successExpected)
            throws PALException,
            RemoteException {
        buildBridge();
        assertNotNull("LearningBridge is null", learningBridge);
        System.out.println("Testing action: " + name);
        System.out.println("============================================");

        log.info("Learning procedure from " + events.size() + " events");

        lastEventList = events;

        long beforeLearn = System.currentTimeMillis();
        // Ensure unique task name
        String tempTaskName = "test-" + name + "-" + System.currentTimeMillis();
        ProcedureDef task = learningBridge.learn(tempTaskName, learnProps, null,
                events.toArray(new ActionStreamEvent[0]));
        String src = task.getSource();
        if(src.length() > 1000) {
            src = src.substring(0, 1000) + "...";
        }
        log.info("Task learned: " + src);
        long afterLearn = System.currentTimeMillis();

        log.info("Learning for " + events.size() + " events took " + (afterLearn - beforeLearn) + " ms.");

        if (successExpected) {
            assertNotNull("Procedure not learned", task);

            for(int i = 0; i < task.numInputParams(); i++) {
                String defVal = task.getDefaultValue(i).toString();
                if (defVal.length() > 100) {
                    defVal = defVal.substring(0, 100) + "...";
                }
                log.info("Procedure input type [" + task.getParamType(i)
                        + "] default value [" + defVal + "]");
            }
            for (int i = task.numInputParams(); i < task.size(); i++) {
                log.info("Procedure output type [" + task.getParamType(i) + "]");
            }

            // All inputs should have display name and default values
            for(int i = 0; i < task.numInputParams(); i++) {
                assertNotNull("Display name is null for param #" + i, task.getParamDescription(i));
                assertNotNull("Default value is null for param #" + i, task.getDefaultValue(i));
            }
        } else {
            assertNull("Procedure learning should have failed and did not", task);
        }

        return task;
    }

    public static void invokeTask(ProcedureDef task) throws PALException, RemoteException {
        invokeTask(task, null, true);
    }

    public static void invokeTask(ProcedureDef task, List<ActionInvocation> eventQueue, boolean provideInputs) throws PALException,
            RemoteException {
        if (eventQueue != null) {
            for (ActionInvocation event : eventQueue) {
                callbackHandler.addFutureEvent(event);
            }
        }
        List<Object> taskInputs = new Vector<Object>();
        if (provideInputs) {
            log.info("Invoking {} with {} inputs", task.getName(), task.numInputParams());
            for(int i = 0; i < task.numInputParams(); i++) {
                TypeDef paramType = task.getParamType(i);
                Object value = VerifiableCallbackHandler.makeBogusValue(paramType);
                taskInputs.add(value);
            }
        }
        log.debug("Invoking with inputs: " + taskInputs);
        ProcedureInvocation proc = task.invoke(null, taskInputs.toArray());
        assertNotNull("Task invocation failed to produce a context for task " + task.getName(), proc);
        proc.start();

        // Wait for callbacks
        int secondsToWait = 20;
        if (eventQueue != null) {
            secondsToWait += eventQueue.size();
        }

        try {
            for (int i = 0; i < secondsToWait * 10
                    && (proc.getStatus().equals(Status.RUNNING) || proc
                            .getStatus().equals(Status.CREATED)); i++) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            fail("Thread interrupted while waiting for action callbacks");
        }
        assertFalse("Task invocation did not complete in the time alloted (" + secondsToWait + " s)",
                proc.getStatus().equals(Status.RUNNING));
        if (proc.getStatus() == Status.ENDED && !callbackHandler.wasTaskSuccessful()) {
            log.error("Synchronization Bug Detected - Please tell Daragh - the procedure is ended but is being reported as unsuccessful");
        }
        assertTrue("Task execution failed with status " + proc.getStatus(), callbackHandler.wasTaskSuccessful());
        assertTrue("Did not see task Ended callback", callbackHandler.isTaskEnded());
        assertFalse("Task was successful, but also had an error", callbackHandler.wasError());
        if (lastEventList != null) {
            for (ActionStreamEvent event : lastEventList) {
                if (event.getClass().equals(ActionInvocation.class)) {
                    assertTrue("Did not see callback for action " + event.getDefinition().getName(),
                            callbackHandler.sawAction(event.getDefinition().getName()));
                }
            }
        }
    }

    public static VerifiableCallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public static String readWholeFile(File file) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuffer sb = new StringBuffer();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            sb.append(line);
            sb.append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String readWholeFile(URL resource) throws Exception {
        File file = new File(resource.toURI());
        return readWholeFile(file);
    }
}
