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

import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.PalUiException;
import com.sri.tasklearning.ui.core.step.LoopModel;
import com.sri.tasklearning.ui.core.step.LoopView;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;

/**
 * A panel for a DialogStepView that knows how to configure a loop. Loop 
 * configuration currently involves choosing a collection variable to iterate
 * over. 
 * 
 * TODO: Eventually we'd like to support the user configuring inline 
 * (hard-coded) collections for iteration. 
 */
public class LoopConfigDialogPanel extends DialogContentPanel {
    private boolean isEditing = false;
    private final LoopView loopView;
    private final LoopModel loopModel;
    private VBox vbox = new VBox(30);
    private final ParameterModel newInputListArg;
    private final ParameterModel newCollectArg;
    private final ProcedureEditController control;

    public LoopConfigDialogPanel(LoopView loop, boolean argIsEditing) 
            throws PalUiException {
        super(loop);
        
        this.loopView = loop;
        this.loopModel = (LoopModel)loop.getStepModel();
        this.control = (ProcedureEditController) loopView.getView().getController();
        
        newInputListArg = loopModel.getInputCollection().copy();
        newCollectArg = loopModel.getCollect().copy(); 
        
        isEditing = argIsEditing;

        title = "Set the inputs for \u201C" + stepModel.getName() + "\u201D";

        prefHeightProperty().bind(vbox.prefHeightProperty());
        
        List<VariableModel> variables = varManager.getValidInputs(loop.getStepModel(), newInputListArg, null);
        List<FunctionModel> functions = control.getSuggestedFunctionCalls(loop.getStepModel(), newInputListArg);
        
        if (!argIsEditing && variables.size() == 0 && functions.size() == 0)
            throw new PalUiException(
                    "You can't add a repeat step here because there are no " +
                    "collections to repeat over.");
        
        InputArgumentChooser iac = new InputArgumentChooser(
                newInputListArg, loopView, true, false, false, false, variables, functions);
        vbox.getChildren().add(iac);       
        
        if (isEditing) {
            List<VariableModel> opts = new ArrayList<VariableModel>();
            for (StepModel step : loopModel.getSteps())                
                for (ParameterModel out : step.getResults())
                    opts.add((VariableModel)out.getTerm());
            
            if (opts.size() > 0) {
                InputArgumentChooser iac2 = new InputArgumentChooser(
                        newCollectArg, loopView, true, false, false, true, opts, null);
                vbox.getChildren().add(iac2);
            }
        }
        
        getChildren().add(vbox);
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
        
    }
    
    @Override
    public void abandonChanges() {
    }

    @Override
    public IUndoable writeChanges() {
        
        if (isEditing) {
            final TermModel oldInputList = loopModel.getLoopList();
            final TermModel oldCollect = loopModel.getCollectTerm();
            final TermModel oldIterand = loopModel.getLoopTerm();
            final TermModel oldInto = loopModel.getIntoTerm();
            final TermModel newCollect = newCollectArg.getTerm();
            final TermModel newInputList = newInputListArg.getTerm();
            
            final boolean collectionChanged;
            final boolean collectChanged;
            
            if (!oldInputList.equals(newInputList))
                collectionChanged = true;
            else
                collectionChanged = false;
                        
            if ((oldCollect == null ^ newCollect == null) ||
                (oldCollect != null && !oldCollect.equals(newCollect)))
                collectChanged = true; 
            else
                collectChanged = false;
            
            if (collectionChanged || collectChanged) {

                if (collectionChanged)
                    loopModel.configInputCollection(newInputList, control.getVariableManager());
                
                final TermModel newIterand = loopModel.getLoopTerm();
                
                
                if (collectChanged)
                    loopModel.configCollect(newCollect, control.getVariableManager());
                
                final TermModel newInto = loopModel.getIntoTerm(); 
                
                final Runnable refresh = new Runnable() {                    
                    public void run() {
                        control.manageVariables();
                        control.validate(true);
                    }
                };
                
                refresh.run();
                    
                IUndoable undo = new IUndoable() {
                    public boolean undo() {
                        if (collectionChanged) {
                            loopModel.getInputCollection().setTerm(oldInputList);
                            loopModel.getIterand().setTerm(oldIterand);
                        }
                        if (collectChanged) {
                            loopModel.getCollect().setTerm(oldCollect);
                            loopModel.getInto().setTerm(oldInto);
                        }
                        refresh.run();
                        return true;
                    }
                    public boolean redo() {
                        if (collectionChanged) {
                            loopModel.getInputCollection().setTerm(newInputList);
                            loopModel.getIterand().setTerm(newIterand);
                        }
                        if (collectChanged) {
                            loopModel.getCollect().setTerm(newCollect);
                            loopModel.getInto().setTerm(newInto);
                        }
                        refresh.run();
                        return true;
                    }
                    public String getDescription() {
                        return "Configure the loop in step " + (loopModel.getIndex() + 1);
                    }
                };
                
                return undo;      
            }            
        } else {
            loopModel.configInputCollection(newInputListArg.getTerm(), control.getVariableManager());
            
            if (loopModel.getLoopTerm() instanceof ProcedureInputVariableModel) {
                VariableModel var = (VariableModel)loopModel.getLoopTerm();                
                VariableModel exist = varManager.getVariableByName(var.getVariableName());
                
                final IUndoable nestedUndo;
                if (exist != null)
                    nestedUndo = control.renameReplaceVariable_impl(var, var.getVariableName());
                else
                    nestedUndo = null;
                
                IUndoable undo = new IUndoable() {
                    public boolean undo() {
                        if (nestedUndo != null)
                            nestedUndo.undo();
                        
                        varManager.unmanageVariable(((ProcedureInputVariableModel)loopModel.getLoopTerm()));
                        return true;
                    }
                    
                    public boolean redo() {
                        if (nestedUndo != null)
                            nestedUndo.redo();
                        
                        varManager.manageVariable(((ProcedureInputVariableModel)loopModel.getLoopTerm()));
                        return true;
                    }
                    
                    public String getDescription() {
                        return null; // This undo should be nested within another
                    }
                };
                
                return undo; 
            }
        }
        loopModel.refreshVariableBindStatus();
        return null;
    }    
}
