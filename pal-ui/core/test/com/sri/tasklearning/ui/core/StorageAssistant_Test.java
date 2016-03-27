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

import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.StepModel;

import org.testng.annotations.Test;

public class StorageAssistant_Test extends CoreUITestWithStartup {
    @Test(groups = "GUI")
    public void testCascadingRename() throws Exception {
        // 1.5ProcedureOutput is called by 1.8SubProcedures, thus we will
        // truly test the cascading part of the rename logic. 
        String procName = "1.5ProcedureOutput";
        String tempName = procName + "temp";

        ProcedureModel pm = BackendFacade.getInstance()
                            .instantiateProcedure(procName);
    
        StorageAssistant.renameProcedure(tempName, pm);
        assertEquals(tempName, pm.getName());
        
        ProcedureModel caller = BackendFacade.getInstance()
                .instantiateProcedure("1.8SubProcedures");
        
        boolean foundRef = false;
        for (StepModel step : caller.getSteps()) {
            assertEquals("Rename failed to cascade",
                    false, step.getName().equals(procName));
            if (step.getName().equals(tempName))
                foundRef = true;
        }
                
        assertEquals("Rename failed to cascade", true, foundRef);

        StorageAssistant.renameProcedure(procName, pm);
        assertEquals(procName, pm.getName());
        
        assertNoLogErrors(true);
    }    
}
