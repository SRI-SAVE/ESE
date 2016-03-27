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

package com.sri.tasklearning.ui.core.step.dialog;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.VBox;

import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.PalUiException;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;
import com.sri.tasklearning.ui.core.ProcedureEditController;

/**
 * A DialogStepView panel that allows the user to configure the inputs
 * to a newly added step. Creates an instance of {@link InputArgumentChooser}
 * for each input to the step. 
 */
public class DialogInputsPanel extends DialogContentPanel {
    private final VBox vbox = new VBox(30);   
    
    public DialogInputsPanel(final StepView step) throws PalUiException {
        super(step);
        
        title = "Set the inputs for \u201C" + stepModel.getName() + "\u201D";        
        
        for (ParameterModel input : stepModel.getInputs()) {
      
        	TypeDef type = input.getTypeDef();
            boolean exist = TypeUtilities.allowExistingValue(type);
            boolean fixed = TypeUtilities.allowFixedValue(type);
            boolean ask   = TypeUtilities.allowAskUser(type);
            
            List<VariableModel> variables = null;
            List<FunctionModel> functions = null;
            
            if (exist) {
            	
            	if (varManager != null)
            		variables = varManager.getValidInputs(step.getStepModel(), input, null);
            	
                functions = control.getSuggestedFunctionCalls(step.getStepModel(), input);
                
            }
            
            if (!fixed && !ask && (!exist || (variables != null && variables.size() == 0 && functions != null && functions.size() == 0)))
                throw new PalUiException(
                        "You can't add this step here because " +
                        "there are no inputs for '" + input.getName() + "'");
            
            // TODO explicit empty option should be driven off of explicitly
            // nullable types when we have them. 
                        
            InputArgumentChooser iac = new InputArgumentChooser(
                    input, step, exist, fixed, ask, false, variables, functions);
            
            iac.prefWidthProperty().bind(prefWidthProperty());
            vbox.getChildren().add(iac);
        }
        
        getChildren().add(vbox);
        layout(); 
    }
    
    @Override
    protected void layoutChildren() {
        vbox.setPrefWidth(prefWidth(0));
        super.layoutChildren(); 
    }
        
    @Override
    protected double computePrefHeight(double width) {
        return vbox.prefHeight(width);
    }

    @Override
    public void activated(boolean isActive) {
        // Do nothing
    }

    @Override
    public IUndoable writeChanges() {
        final List<VariableModel> affectedTerms = new ArrayList<VariableModel>();
        final List<IUndoable> replacements = new ArrayList<IUndoable>();
        for (int i = 0; i < stepModel.getInputs().size(); i++) { 
            TermModel t = stepModel.getInputs().get(i).getTerm();
            
            if (t instanceof ProcedureInputVariableModel) {            
                ProcedureInputVariableModel var = (ProcedureInputVariableModel)t;
                VariableModel exist = null;
                
                if ((exist = varManager.getVariableByName(var.getVariableName())) == null) {
                    affectedTerms.add(var);
                    varManager.manageVariable(var);
                } else {
                    if (exist instanceof ProcedureInputVariableModel) {
                        stepModel.getInputs().get(i).setTerm(exist);
                    } else {
                        IUndoable undo = ((ProcedureEditController) control).renameReplaceVariable_impl(var, var.getVariableName());
                        replacements.add(undo);
                    }
                }                
            }            
        }
         
        IUndoable undo = new IUndoable() {
            public boolean undo() {
                for (VariableModel var : affectedTerms)
                    varManager.unmanageVariable(var);
                
                for (IUndoable replace : replacements)
                    replace.undo();
                
                return true;
            }
            
            public boolean redo() {
                for (VariableModel var : affectedTerms)
                    varManager.manageVariable(var);
                
                for (IUndoable replace : replacements)
                    replace.redo();
                
                return true;
            }
            
            public String getDescription() {
                return null; 
            }
        };
        
        return undo;
    }
    
    @Override
    public void abandonChanges() {
        
    }
}
