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

// $Id: NoDefaults_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.bridge;

import java.rmi.RemoteException;

import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureExecutor;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.BeforeMethod;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class NoDefaults_FuncTest
        extends PALBridgeTestCase {
    private static final String AM_NAME = ActionModels.ACTIONS;
    private static final String NAMESPACE = "nodef";
    private static final String FILE = "NoDefaults.xml";

    private String procSrc;
    private ProcedureExecutor sparkExec;
    private ProcedureDef procDef;

    @BeforeClass
    public static void setup()
            throws RemoteException,
            PALException {
        setup(ActionModels.class.getResource(AM_NAME), NAMESPACE);
        SimpleTypeName actionName = (SimpleTypeName) TypeNameFactory.makeName(
                "getClass", "1.0", NAMESPACE);
        actionModel.registerExecutor(actionName, callbackHandler);
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    @BeforeMethod
    public void setup2()
            throws Exception {
        procSrc = ProcedureLearner.readWholeFile(getClass().getResource(FILE));
        sparkExec = palBridge.getPALExecutor();
        procDef = sparkExec.load(procSrc);
        Assert.assertTrue(procDef.numInputParams() > 0);
    }

    @Test
    public void getDefault()
            throws Exception {
        Object defVal = procDef.getDefaultValue(0);
        Assert.assertEquals(null, defVal);
    }

    @Test
    public void executeWithArgs()
            throws Exception {
        ProcedureInvocation invoc = procDef.invoke(null, "foo");
        invoc.start();
        invoc.waitUntilFinished();
        Assert.assertEquals(Status.ENDED, invoc.getStatus());
    }

    @Test
    public void executeWithoutArgs()
            throws Exception {
        ProcedureInvocation invoc = procDef.invoke(null);
        invoc.start();
        invoc.waitUntilFinished();
        Assert.assertEquals(Status.ENDED, invoc.getStatus());
    }
}
