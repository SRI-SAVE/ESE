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

// $Id: Validator_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.impl.CTRActionDeclaration;
import com.sri.ai.lumen.editorsupport.ProcedureInfo;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Validator_FuncTest
        extends PALBridgeTestCase {
    private static final String procSrc = "action 'lumen^0.4^BrokenSupportRelationship'"
            + " execute:{" + "  do 'vft^1.0^CreateString'('$output 1');"
            + "  do 'vft^1.0^UseString'('$output 2');}"
            + " properties:{isTransient:\"true\"}";
    private static final String NS = "vft";

    private static ATRActionDeclaration proc;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ActionModels.ARDA), NS);

        proc = ATRSyntax.CTR
                .declFromSource(CTRActionDeclaration.class, procSrc);
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void syncValidate()
            throws Exception {
        Validator v = new Validator(palBridge);
        ProcedureInfo pi = v.makeProcedureInfo(proc);
        Assert.assertEquals(pi.getIssues().size(), 1);
    }

    @Test(timeOut = 10000)
    public void asyncValidate()
            throws Exception {
        Validator v = new Validator(palBridge);
        SynchronousCallbackHandler<ProcedureInfo> cbh = new SynchronousCallbackHandler<ProcedureInfo>();
        v.makeProcedureInfo(cbh, proc);
        ProcedureInfo pi = cbh.waitForResult();
        Assert.assertEquals(pi.getIssues().size(), 1);
    }
}
