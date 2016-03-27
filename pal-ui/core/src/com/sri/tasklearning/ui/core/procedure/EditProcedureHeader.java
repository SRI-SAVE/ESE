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

package com.sri.tasklearning.ui.core.procedure;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import com.sri.ai.lumen.atr.ATR;
import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.VariableManager;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.control.Alert.AlertResult;
import com.sri.tasklearning.ui.core.control.ToolTippedImageView;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.constant.ConstantEditor;
import com.sri.tasklearning.ui.core.control.constant.StringConstantEditor;
import com.sri.tasklearning.ui.core.procedure.ProcedureInputsPanel.ProcedureInputRow;
import com.sri.tasklearning.ui.core.term.CompositeTermModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * This concrete extension of ProcedureHeader is for when you want to provide
 * the user with mechanisms to reorder inputs, change default values for inputs,
 * etc. It is not for editing runtime values or executing procedures, although
 * it will directly execute a procedure with no inputs.   
 */
public final class EditProcedureHeader extends ProcedureHeader {
    private InvalidationListener listen;
    
    public EditProcedureHeader(final ProcedureView view) {
        super(view);
    }
    
    private void handleRunRequest(final boolean debug) {
        if (!controller.isUnsavedChanges()) {
            runProcedureUI(procedureView, debug);
            return;
        }

        Callback<AlertResult, Void> call = new Callback<AlertResult, Void>() {
            public Void call(AlertResult result) {
                if (result == AlertResult.YES) {
                    Callback<CommonModel, Void> onSuccessfulSave = 
                        new Callback<CommonModel, Void>() {
                        public Void call(CommonModel pm) {
                            runProcedureUI(procedureView, debug);
                            return null;
                        }
                    };
                    controller.attemptSave(true, false, onSuccessfulSave,
                            getScene());
                }
                return null;
            }

        };
        Alert.show("Save changes?",
                "Running this procedure will save your pending "
                        + "edits. Do you wish to continue?",
                AlertConfig.YES_NO, call);
    }
    
    @Override
    protected void configureRunButton() {
        // Clicking the Run button in an edit header causes a popup to
        // open that contains an instance of an exec header
        final EventHandler<ActionEvent> runHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                runButton.setText("Run");
                runButton.setOnAction(this);
                handleRunRequest(false);
            }
        };
        final EventHandler<ActionEvent> debugHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                runButton.setText("Debug");
                runButton.setOnAction(this);
                handleRunRequest(true);
            }
        };
        runButton.setOnAction(runHandler);
        runButton.setText("Run");
        
        run.setOnAction(runHandler);
        debug.setOnAction(debugHandler);
        
        // Can't run a procedure unless it's error-free, has steps and there
        // isn't already a procedure running
        listen = new InvalidationListener() {
            public void invalidated(Observable val) {
                if (procedureView.isReadOnly() ||
                    controller.numStepsWithErrorsProperty().getValue() > 0 ||
                    procedureView.getModel().hasStepsProperty().getValue() == false ||
                    backend.runningProcedureProperty().getValue() != null)
                    runButton.setDisable(true);
                else
                    runButton.setDisable(false);                   
            }
        };
        
        listen.invalidated(null);
        
        procedureView.readOnlyProperty().addListener(listen);
        controller.numStepsWithErrorsProperty().addListener(listen);
        procedureView.getModel().hasStepsProperty().addListener(listen);
        backend.runningProcedureProperty().addListener(listen);       
    }
    
    @Override
    public void prepareToClose() {
        super.prepareToClose();
        
        if (listen != null) {
            procedureView.readOnlyProperty().removeListener(listen);
            controller.numStepsWithErrorsProperty().removeListener(listen);
            procedureView.getModel().hasStepsProperty().removeListener(listen);
            backend.runningProcedureProperty().removeListener(listen);
        }
    }
    
    
    @Override
    protected void addChildren() {        
        getChildren().addAll(titleBarBox, gears, boxBorder, topHighlight,
                leftHighlight, bottomShadow, rightShadow, title, desc, inputs,
                exp, dflt, renameLink, instructions, runButton);
    }
    
    @Override
    protected double getBottomOffset() {
        return 0;
    }
    
    @Override
    protected ProcedureInputsPanel getProcedureInputsPanel() {        
        return new ProcedureInputsPanel(this, procedureView, false);
    }
    
    @Override
    public ConstantEditor configureProcedureInputRow(
            final ProcedureInputRow row, 
            final ConstantEditor argEditor,
            final boolean forceEditor) {
        
        final ProcedureInputVariableModel input = row.getInput();
        final ConstantEditor editor; 
        // Set the editor's value to the current value, if there is one in
        // the procedure. If not, just use the editor's default value.
        if (input.getDefaultValue() != null) {
            editor = argEditor;
            editor.setAtrValue(input.getDefaultValue());
        } else if (!forceEditor) {
            StringConstantEditor placebo = new StringConstantEditor(null);
            placebo.getTextField().setDisable(true);
            placebo.setValue("[Ask the user]");
            editor = placebo;
        } else
            editor = argEditor;
        
        editor.setTooltip(new IToolTipCallback() {
            public String getToolTipText() {
                return "Value to use for this input when procedure is executed.\nClick to modify.";
            }
        });

        editor.setOnCanceled(new Runnable() {
            public void run() {
                row.refreshEditor(false);    
            }
        });

        editor.setOnConfirmed(new Callback<Object, Void>() {
            public Void call(final Object value) {
                ATR atrValue = editor.getATRValue();
                if ((atrValue != null && !atrValue.equals(input.getDefaultValue())) ||
                    atrValue == null && input.getDefaultValue() != null) {
                    editor.getOnChanged().call(value);
                }
                return null; 
            }
        });

        editor.setOnChanged(new Callback<Object, Void>() {
            public Void call(final Object value) {
                if (value == null) {
                    controller.clearDefault(input, row);
                    return null; 
                } 
                
                TermModel newValue;

                if (value instanceof TermModel)
                    newValue = (TermModel)value;
                else
                    newValue = editor.getATRValue();
                
                if (newValue instanceof NullValueModel)
                    newValue = null;
                
                final TermModel oldValue = input.getDefaultValue();
                final TermModel fNewValue = newValue;
                
                if ((oldValue == null && newValue == null) ||
                    (oldValue != null && oldValue.equals(newValue)))
                    return null;

                editor.setAtrValue(newValue);
                input.setDefaultValue(newValue);

                final IUndoable undo = new IUndoable() {
                    public String getDescription() {
                        String desc =  "Change the default value of \""
                                + input.getVariableName() + "\"";
                        
                        if (!(fNewValue instanceof CompositeTermModel))
                            desc += " to \"" + fNewValue + "\"";
                        
                        return desc;
                    }

                    public boolean undo() {
                        input.setDefaultValue(oldValue);
                        EditProcedureHeader.this.inputs.findProcedureInputRow(
                                input).refreshEditor(false);
                        return true;
                    }

                    public boolean redo() {
                        input.setDefaultValue(fNewValue);
                        EditProcedureHeader.this.inputs.findProcedureInputRow(
                                input).refreshEditor(false);
                        return true;
                    }
                };

                controller.getUndoManager().pushUndo(undo);
                
                return null; 
            }            
        });
        
        final ToolTippedImageView errorButton = row.getErrorButton();
        
        errorButton.setToolTipCallback(new IToolTipCallback() {
            public String getToolTipText() {
                if (input.isReferenced())
                    return "";
                else
                    return "The value \"" + input.getVariableName()
                            + "\" is not used by any steps. \n"
                            + "Click here to delete.";
            }
        });
        errorButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                final VariableManager mgr = procedureModel.getVariableManager();
                if (errorButton.isVisible()
                        && !input.isReferenced()
                        && !mgr.isVariableReferenced(procedureModel
                                .getSignature().getOwningActionDecl(), input)) {
                    
                    Callback<AlertResult, Void> call = 
                        new Callback<AlertResult, Void>() {
                        public Void call(final AlertResult result) {
                            if (result == AlertResult.YES) {
                                inputs.removeRow(input);
                                mgr.unmanageVariable(input);
            
                                final IUndoable undo = new IUndoable() {
                                    public String getDescription() {
                                        return "Remove the procedure input \""
                                                + input.getVariableName() + "\"";
                                    }
            
                                    public boolean undo() {
                                        mgr.manageVariable(input);
                                        return true;
                                    }
            
                                    public boolean redo() {
                                        mgr.unmanageVariable(input);
                                        return true;
                                    }
                                };
                                controller.getUndoManager().pushUndo(undo);
                            }
                            return null; 
                        }
                    };

                    Alert.show("Warning: Unused Value", 
                            "The procedure input \"" + input.getVariableName() +
                            "\" is not used by any step. " + 
                            "Would you like to delete it?", 
                            AlertConfig.YES_NO, call);                        
                }
            }
        });
        
        return editor;
    }
}
