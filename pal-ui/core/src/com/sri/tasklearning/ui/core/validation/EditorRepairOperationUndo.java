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

import com.sri.tasklearning.ui.core.procedure.ProcedureModel;

/**
 * A repair operation that knows how to undo the user's last action.
 * This repair operation is unknown to Lumen and is therefore never
 * instantiated by Lumen. Is instead added manually in {@link EditorIssue}.
 */
public class EditorRepairOperationUndo extends EditorRepairOperation {
    private static final long serialVersionUID = 1L;
    
    public static final String MESSAGE = "Undo your last edit";
    
    public EditorRepairOperationUndo(ProcedureModel procModel) {
        super(procModel);
    }

    @Override
    public boolean applyOperation() {
        controller.getUndoManager().undo();
        return true; 
    }

    @Override
    public EditorRepairUI getSuggestionUI(EditorIssue issue) {
        return new EditorRepairUI(this, MESSAGE);
    }
}

