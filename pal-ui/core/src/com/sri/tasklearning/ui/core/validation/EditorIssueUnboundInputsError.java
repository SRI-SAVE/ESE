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
import java.util.Iterator;
import java.util.List;

import com.sri.ai.lumen.core.Issue;
import com.sri.ai.lumen.errors.ErrShouldBeInputs;
import com.sri.ai.lumen.util.repair.RepairSuggestion;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * Specialization of EditorIssue for the case where a step references an
 * unbound variable. 
 */
public class EditorIssueUnboundInputsError extends EditorIssue {
    private ErrShouldBeInputs.InputParametersNotEvaluableError theError;
 
    protected EditorIssueUnboundInputsError(            
            final Issue issue, 
            final List<RepairSuggestion> lumenSuggestions, 
            final ProcedureModel procedure) {
        super(issue, lumenSuggestions, procedure);
        
        theError = (ErrShouldBeInputs.InputParametersNotEvaluableError)issue;
    }

    private List<TermModel> getUnboundTerms() {
        List<TermModel> ret = new ArrayList<TermModel>();
        StepModel step = getStep();

        Iterator<Integer> it = theError.getNonEvaluableInputParameters().iterator();
        while (it.hasNext())
            ret.add(step.getInputs().get(it.next()).getTerm());
        
        return ret;
    }

    @Override
    public List<Object> getFancyMessage(boolean asText) {
        final List<Object> ret = new ArrayList<Object>();
        ret.add("The Editor cannot find the " + (getUnboundTerms().size() > 1 ? "values " : "value "));       
        
        if (asText) {
            StringBuffer unboundsStr = new StringBuffer();
            for (TermModel arg : getUnboundTerms())
                unboundsStr.append(" \"" + arg.getDisplayString() + "\" ");
            ret.add(unboundsStr.toString().trim());
        } else {
            final ArrayList<Object> unboundsArr = new ArrayList<Object>();
            for (TermModel arg : getUnboundTerms()) {
                ParameterModel pm = new ParameterModel("bogus", null, null,
                        arg, null, true, null);
                unboundsArr.add(pm);
            }
            ret.addAll(unboundsArr);
        }        

        ret.add(" at this point in the procedure.");
        return ret;
    }
}
