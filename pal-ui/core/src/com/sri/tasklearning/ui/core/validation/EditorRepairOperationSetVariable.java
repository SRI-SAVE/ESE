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

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.util.repair.RepairOperation;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.VariableModel;

/**
 * A repair operation that knows how to set a variable, which apparently
 * means asking for a user input.
 */

public class EditorRepairOperationSetVariable extends EditorRepairOperation {
    private static final long serialVersionUID = 1L;
    
    public EditorRepairOperationSetVariable(RepairOperation op,
            ProcedureModel proc) {
        super(op, proc);
    }

    /**
     * Sets a variable as described by the operation.
     */
    @Override
    public boolean applyOperation() {
        Object argModel = (getLocation() instanceof ParameterModel) ? (getLocation())
                : (getParameterFromVariable((VariableModel) getLocation()));
        return controller.changeArgumentToProcedureInput(
                (ParameterModel) argModel, getStep());
    }

    private ParameterModel getParameterFromVariable(VariableModel variable) {
        for (ParameterModel input : getStep().getInputs()) {
            if (input.getTerm().equals(variable))
                return input;
        }
        return null;
    }

    @Override
    public EditorRepairUI getSuggestionUI(EditorIssue issue) {
        ATR loc = getLocation();        
        
        if (loc instanceof VariableModel) {
            ParameterModel argModel = (getLocation() instanceof ParameterModel) ? ((ParameterModel) getLocation())
                    : (getParameterFromVariable((VariableModel) getLocation()));
            
            if (argModel != null) {
                EditorRepairUI editorrepairui = new EditorRepairUI(this,
                        "Ask the user to provide \"" + argModel.getName()
                                + "\" as a procedure input.");
                return editorrepairui;
            }
        }
        return null;
    }
}
