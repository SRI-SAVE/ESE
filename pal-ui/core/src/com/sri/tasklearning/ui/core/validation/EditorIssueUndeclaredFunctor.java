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
import com.sri.ai.lumen.util.repair.RepairSuggestion;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.StepModel;

/**
 * Specialization of EditorIssue for when a functor is missing. This can
 * expectedly occur when the user performs renames or deletes in the midst
 * of a series of edits involving the functor in question, and then uses the
 * undo stack. 
 */
public class EditorIssueUndeclaredFunctor extends EditorIssue {
    protected EditorIssueUndeclaredFunctor(            
            final Issue issue, 
            final List<RepairSuggestion> lumenSuggestions, 
            final ProcedureModel procedure) {
        super(issue, lumenSuggestions, procedure);
    }

    @Override
    public List<Object> getFancyMessage(boolean asText) {
        List<Object> ret = new ArrayList<Object>();
        ret.add("The procedure '" + ((StepModel) getIssueLocation()).getName()
                + "' doesn't exist anymore.");
        return ret; 
    }
}
