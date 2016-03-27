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

package com.sri.tasklearning.ui.core.validation;

import java.util.ArrayList;
import java.util.List;

import com.sri.ai.lumen.core.Issue;
import com.sri.ai.lumen.errors.ErrShouldBeInputs;
import com.sri.ai.lumen.util.repair.RepairSuggestion;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.LoopModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;

/**
 * Specialization of EditorIssue for when there is a problem with how a loop
 * is configured.
 */
public class EditorIssueUnboundLoopError extends EditorIssue {
    private final ErrShouldBeInputs.LoopInputParametersEvaluableError theError;
    private final LoopModel loopModel;
    protected EditorIssueUnboundLoopError(            
            final Issue issue, 
            final List<RepairSuggestion> lumenSuggestions, 
            final ProcedureModel procedure) {
        super(issue, lumenSuggestions, procedure);
        
        theError = (ErrShouldBeInputs.LoopInputParametersEvaluableError)issue;
        loopModel = (LoopModel) BackendFacade.locate(theError.getPreorderIndex(), procedure);        
    }

    @Override
    public List<Object> getFancyMessage(boolean asText) {
        String unboundsStr = "";
        ArrayList<Object> unboundsArr = null;       
                
        if (asText) {
            unboundsStr = "";
            unboundsStr = unboundsStr + " \""
                    + loopModel.getInputCollection().getTerm().getDisplayString()
                    + "\" ";
            unboundsStr = unboundsStr.trim();
        } else {
            unboundsArr = new ArrayList<Object>();       
            ParameterModel pm = new ParameterModel("bogus", null, null, loopModel
                    .getInputCollection().getTerm(), null, true, null);
            unboundsArr.add(pm);
        }
                   
        List<Object> ret = new ArrayList<Object>();
        ret.add("The Editor cannot find the value ");
        if (asText)
            ret.add(unboundsStr);
        else
            ret.addAll(unboundsArr);
        ret.add(" at this point in the procedure.");
        return ret;
    }
}
