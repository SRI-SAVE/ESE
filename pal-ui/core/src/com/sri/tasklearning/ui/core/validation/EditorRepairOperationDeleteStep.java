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

import com.sri.ai.lumen.util.repair.RepairOperation;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;

/**
 * A repair operation that knows how to delete a step.
 */
public class EditorRepairOperationDeleteStep extends EditorRepairOperation {
    private static final long serialVersionUID = 1L;
    
    public EditorRepairOperationDeleteStep(RepairOperation operation,
            ProcedureModel procModel) {
        super(operation, procModel);
    }

    @Override
    public boolean applyOperation() {
        controller.deleteStep(getStep());
        return true; 
    }

    @Override
    public EditorRepairUI getSuggestionUI(EditorIssue issue) {
        String messageText = "";
        if (issue.getStep().equals(getStep())) {
            messageText = "Delete this step";
        } else {
            messageText = "Delete step " + getStep().getIndex();
        }
        return new EditorRepairUI(this, messageText);
    }
}
