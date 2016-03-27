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

package com.sri.tasklearning.ui.core;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.pal.ActionDef;
import com.sri.pal.ActionModelDef;
import com.sri.pal.PALException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.TypeStorage;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.util.CoreUITestUtilities;
import com.sri.tasklearning.ui.core.validation.EditorIssue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class Models_Test extends CoreUITestWithStartup {
    private static final Logger log = LoggerFactory
        .getLogger(Models_Test.class);    
    /**
     * Loads CTR-S for all procedures contained within testStorage/procedures, 
     * instantiates each procedure using core UI models/factory, serializes
     * models back out to CTR-S and asserts equivalence to original CTR-S. 
     * 
     * @throws IOException
     *             if we fail to read the file for this procedure
     * @throws PALException
     *             if we fail to load the type for this procedure
     * @throws JAXBException
     *             if XML loading fails
     */
    @Test(groups = "GUI")
    public void roundTripCannedProcedures() throws IOException, PALException,
            JAXBException {

        for (File proc : CoreUITestUtilities.PROC_SRC_DIR.listFiles()) {
            if (!proc.getName().endsWith(".procedure"))
                continue;
            
            log.info("Round-tripping " + proc.getName());
            final String source = CoreUITestUtilities.ctrsFromFile(proc, true);
            final ProcedureModel model = BackendFacade.getInstance()
                    .instantiateProcedureFromSource(source);

            assertEquals("Round trip: " + proc.getName() + ":\n", source,
                    ATRSyntax.toSource(model));
            
            assertNoLogErrors(true);
        }
    }
    
    /**
     * Insures that the canned procedures do not contain any validation errors.
     * This is really a functional test of the lumen static validator code but
     * it doesn't hurt to have it here and run it on the UI canned procs
     * 
     * @throws PALException
     */
    @Test(groups = "GUI")
    public void validateCannedProcedures() throws PALException {
        Set<SimpleTypeName> procs = bridge.getActionModel().listTypes(TypeStorage.Subset.PROCEDURE);
        
        for (SimpleTypeName procName : procs) {
            ProcedureModel pm = BackendFacade.getInstance().instantiateProcedure(procName);
            new ProcedureEditController(pm);
            log.info("Validating " + pm.getName());
            List<EditorIssue> issues = BackendFacade.getInstance().validateProcedure(pm);
            
            for (EditorIssue issue : issues)
                log.info(issue.getIssue().getMessage());
            assertEquals("Canned procedure threw validation errors", 0, issues.size());
        }
    }
    
    @Test(groups = "GUI")
    public void executeCannedProcedures() throws PALException, IOException, JAXBException {
        Set<ActionModelDef> types = bridge.getActionModel().getTypes();
        TestExecutor exec = new TestExecutor(bridge);
        for (ActionModelDef type : types) {
            if (type instanceof ActionDef && 
                !(type instanceof ProcedureDef) &&
                ((SimpleTypeName)type.getName()).getNamespace().equals("arda")) {
                bridge.getActionModel().registerExecutor((SimpleTypeName)type.getName(), exec);
            }
        }

        for (File proc : CoreUITestUtilities.PROC_SRC_DIR.listFiles()) {
            if (!proc.getName().endsWith(".procedure") ||
                proc.getName().equals("2.5UnknownCustom.procedure"))
                continue;
            
            exec.setOutput("");
            
            log.info("Executing " + proc.getName());
            final String source = CoreUITestUtilities.ctrsFromFile(proc, true);
            final ProcedureModel model = BackendFacade.getInstance()
                    .instantiateProcedureFromSource(source);
            
            BackendFacade.getInstance().runProcedure(model, true, null, false, null);
            assertNoLogErrors(true);
            String expectedOutput = model.getMetadata().getString("expected_output");
            String output = exec.getOutput();
            
//          model.getMetadata().set("expected_output",
//          new ConstantValueModel(output));
//          bridge.getPALExecutor().load(
//          LumenProcedureDef.wrapXml(ATRSyntax.toSource(model)));
//          System.out.println(LumenProcedureDef.wrapXml(ATRSyntax.toSource(model)));
            
            if ((expectedOutput == null || expectedOutput.length() == 0) &&
                (output == null || output.length() == 0))
                continue;

            // Set.toString() is non-deterministic.
            String name = proc.getName();
            if (name.equals("3.2Sets.procedure")
                    || name.equals("4.2Loops.procedure")) {
                continue;
            }

            assertEquals(proc.getName(), expectedOutput, output);
        }
    }
}
