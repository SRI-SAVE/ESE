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

// $Id: Procedure_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.bridge;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Procedure_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = ProcedureLearner.NAMESPACE;
    private static ProcedureDef task;

    @BeforeClass
    public static void loadAM()
            throws Exception {
        setup();

        SimpleTypeName actionId = (SimpleTypeName) TypeNameFactory.makeName(
                "action144", "1.0", NAMESPACE);
        actionModel.registerExecutor(actionId, callbackHandler);
        ActionInvocation action = ((ActionDef) actionModel.getType(actionId)).invoke(null, "foo");
        action.setValue(1, "bar");
        task = ProcedureLearner.learnProcedure(action, "watchStatus");
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * If I start a procedure, then watch procedure.getStatus(), will it
     * eventually show up as ENDED? It needs to do this regardless of whether I
     * ever call waitForFinish() or similar.
     */
    @Test
    public void watchStatus()
            throws Exception {
        ProcedureInvocation proc = task.invoke(null);
        proc.start();
        boolean fail = true;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            if (proc.getStatus() == ProcedureInvocation.Status.ENDED) {
                fail = false;
                break;
            }
        }
        assertFalse("Procedure never ended", fail);
    }

    /**
     * Similar to {@link #watchStatus}, but instead of polling getStatus(), we
     * wait for {@link PALApplication#procedureEnded} to be called.
     */
    @Test(timeOut = 30000)
    public void waitForEndedCallback()
            throws Exception {
        ProcedureInvocation proc = task.invoke(null);
        proc.start();
        boolean fail = true;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            if (callbackHandler.isTaskEnded()) {
                fail = false;
                break;
            }
        }
        assertFalse("Procedure never ended", fail);
    }

    /**
     * At one time, ITLProcedureImpl.hashCode returned 0 until its start()
     * method was called -- then it would return a real hash code.
     * <p>
     * https://jira.esd.sri.com/browse/CPAL-32
     */
    @Test(timeOut = 30000)
    public void checkHash()
            throws Exception {
        ProcedureInvocation proc = task.invoke(null);
        int hash1 = proc.hashCode();
        proc.start();
        int hash2 = proc.hashCode();
        proc.waitUntilFinished();
        int hash3 = proc.hashCode();
        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }
}
