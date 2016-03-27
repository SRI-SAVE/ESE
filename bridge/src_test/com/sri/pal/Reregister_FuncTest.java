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

// $Id$
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Reregister_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory
            .getLogger(Reregister_FuncTest.class);

    private static final String ACTION_MODEL = ActionModels.SIMPLE;
    private static final String NAMESPACE = "foo";
    private static SimpleTypeName actionName;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTION_MODEL), NAMESPACE);
        actionName = (SimpleTypeName) TypeNameFactory.makeName("action1",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(actionName, callbackHandler);
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * This is CPAL-155. When we remove all references to a ProcedureDef,
     * ActionFinalizer causes it to be removed from ITL's type system. This
     * causes it to be removed from Lumen and LAPDOG, too. (ActionFinalizer only
     * runs its queue when a procedure concludes.)
     * <p>
     * Do reproduce this bug: Learn a procedure, but don't execute it. Allow its
     * references to be cleaned up so ActionFinalizer runs. Now load the
     * procedure from its source, and try to execute it.
     */
    @Test
    public void cpal155()
            throws Exception {
        Learner learner = palBridge.getLearner();
        LumenProcedureExecutor sparkExec = (LumenProcedureExecutor) palBridge
                .getPALExecutor();
        VerifiableCallbackHandler callbackHandler = ProcedureLearner
                .getCallbackHandler();

        ActionDef actionDef = (ActionDef) actionModel.getType(actionName);
        ActionInvocation action = actionDef.invoke(null, "arg");
        ProcedureDef proc1 = learner.learn("proc1", null, action);
        ProcedureDef proc2 = learner.learn("proc2", null, action);
        assertTrue(proc1.isTransient());

        SimpleTypeName proc1Name = proc1.getName();
        String proc1Src = proc1.getSource();
        proc1 = null;

        log.debug("Starting GC");
        System.gc();
        // Give the GC time to complete:
        Thread.sleep(2000);
        // Run a procedure, to wake up ActionFinalizer:
        ProcedureInvocation dummy = proc2.invoke(null);
        dummy.start();
        dummy.waitUntilFinished();
        // Give ActionFinalizer time to complete:
        Thread.sleep(2000);
        log.debug("After GC wait");

        proc1 = sparkExec.load(proc1Src);
        assertNotNull(actionModel.getType(proc1Name));
        ProcedureInvocation invoc1 = proc1.invoke(null);
        invoc1.start();
        invoc1.waitUntilFinished();
        assertTrue(callbackHandler.wasTaskSuccessful());
        assertFalse(callbackHandler.wasError());
    }

    @Test
    public void cpal168()
            throws Exception {
        Learner learner = palBridge.getLearner();
        LumenProcedureExecutor sparkExec = (LumenProcedureExecutor) palBridge
                .getPALExecutor();
        VerifiableCallbackHandler callbackHandler = ProcedureLearner
                .getCallbackHandler();

        ActionDef actionDef = (ActionDef) actionModel.getType(actionName);
        ActionInvocation action = actionDef.invoke(null, "arg");
        ProcedureDef proc1 = learner.learn("cpal168", null, action);
        String proc1Src = proc1.getSource();
        ProcedureDef proc2 = sparkExec.load(proc1Src);
        assertEquals(proc1, proc2);

        // Now see if we can get it to remove proc1 from the action model, thus
        // destroying proc2 as well.
        proc1 = null;
        System.gc();
        Thread.sleep(1000);
        ProcedureInvocation invoc = proc2.invoke(null);
        invoc.start();
        invoc.waitUntilFinished();
        Thread.sleep(1000);

        // Now run proc2 again.
        callbackHandler.reset();
        invoc = proc2.invoke(null);
        invoc.start();
        invoc.waitUntilFinished();
        Thread.sleep(100);
        assertTrue(callbackHandler.wasTaskSuccessful());
        assertFalse(callbackHandler.wasError());
        assertEquals(1, callbackHandler.getSeenActions().size());
    }
}
