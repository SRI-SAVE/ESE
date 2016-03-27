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
import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.util.repair.RepairSuggestion;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;

/**
 * Specialization of EditorIssue for the case where a step references a variable
 * that is bound by a step that has an error of its own. 
 */
public class EditorIssuePreviousUnboundWarning extends EditorIssue {
    protected EditorIssuePreviousUnboundWarning(            
            final Issue issue, 
            final List<RepairSuggestion> lumenSuggestions, 
            final ProcedureModel procedure) {
        super(issue, lumenSuggestions, procedure);
    }

    @Override
    public List<Object> getFancyMessage(boolean asText) {
        Object unbounds;
        ATR location = getIssueLocation(); 
        if (asText)
            unbounds = location.toString();
        else
            unbounds =  location;
        
        List<Object> ret = new ArrayList<Object>();
        ret.add("This step references the value");
        ret.add(unbounds);
        ret.add(", which was marked with an error in an earlier step.");
        return ret; 
    }
}
