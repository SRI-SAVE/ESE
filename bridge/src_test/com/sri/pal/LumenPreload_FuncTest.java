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

import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.util.PALBridgeTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Tests Lumen's ability to preload a .lumen file and some actions before the first procedure is run.
 *
 * @see GeneralizedActions_FuncTest
 */
public class LumenPreload_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = "lpft";
    private static final String procSrc = "action 'lumen^0.4^callPreload'()\n"+
            " argtypes:[]\n"+
            " execute:{\n"+
            "  do 'preloadedProc'();}\n"+
            " properties:{category:\"effector\", constraints:`(), demonstrated_variable_bindings:{}, description:\"\", isTransient:\"true\", needsPreload:\"true\"}";
    private static final String preloadSrc = "action preloadedProc()\n" +
            " execute:{\n" +
            "  do 'lpft^1.0^noop'();};\n";

    @BeforeClass
    public static void setup()
            throws Exception {
        File loadFile = new File("preload.lumen");
        try (FileWriter fw = new FileWriter(loadFile);
             PrintWriter out = new PrintWriter(fw)) {
            out.print(preloadSrc);
        }

        SimpleTypeName noopName = new SimpleTypeName("noop", "1.0", NAMESPACE);

        System.setProperty("PAL.lumen-preload-file", loadFile.getCanonicalPath());
        System.setProperty("PAL.lumen-preload-prerequisites", noopName.getFullName());
        setup(ActionModels.class.getResource(ActionModels.SIMPLE), NAMESPACE);
        actionModel.registerExecutor(noopName, callbackHandler);
    }

    @AfterClass
    public static void teardown()
            throws Exception {
        palBridge.shutdown();
    }

    @Test
    public void preload()
            throws Exception {
        ATRActionDeclaration atrProc = LumenProcedureDef.sourceToProc(procSrc);
        LumenProcedureExecutor exec = (LumenProcedureExecutor) palBridge.getPALExecutor();
        LumenProcedureDef proc = exec.load(atrProc);
        ProcedureInvocation invoc = proc.invoke(null);
        invoc.start();
        invoc.waitUntilFinished();
        Assert.assertEquals(invoc.getStatus(), ActionStreamEvent.Status.ENDED);
    }
}
