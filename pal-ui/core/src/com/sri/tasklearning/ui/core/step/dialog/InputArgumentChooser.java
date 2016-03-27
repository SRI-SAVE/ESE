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
import java.util.LinkedList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Callback;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.VariableManager;
import com.sri.tasklearning.ui.core.control.constant.ConstantEditor;
import com.sri.tasklearning.ui.core.popup.RenameVariableDialog;
import com.sri.tasklearning.ui.core.popup.ReplaceVariableDialog;
import com.sri.tasklearning.ui.core.popup.ReplaceVariableDialog.ITermChosenCallback;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ParameterView;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TermView;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.VariableView;
import com.sri.tasklearning.ui.core.term.function.AccessorFunctionView;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;

/**
 * Visualizes the configuration options for a single input to a step, including
 * providing a constant value, referencing an existing variable or creating a
 * new procedure input. Helper class for {@link DialogInputsPanel} and 
 * {@link LoopConfigDialogPanel}. 
 */
public class InputArgumentChooser extends VBox {   
    private final ParameterModel input;
    private final StepView step;
    
    private List<VariableModel> potentialExistingVars = new LinkedList<VariableModel>(); 
    private List<FunctionModel> potentialFunctionCalls = new LinkedList<FunctionModel>();
    
    private ParameterModel existingValueParameterModel; 
    private ParameterView existingValueParameterView;
    
    private final ParameterModel userInputModel;   
    private final VariableView userInputView;
    
    private ConstantEditor constantEditor;
    
    private boolean allowConstantInput;
    private final boolean allowProcedureInput;
    private final boolean explicitEmptyOption;
    private final boolean allowExistingValue; 

    private final VariableManager varMgr;
    
    public InputArgumentChooser(
            final ParameterModel input, 
            final StepView step,
            final boolean argAllowExistingValue,
            final boolean argAllowConstantInput, 
            final boolean argAllowProcedureInput,
            final boolean explicitEmptyOption,
            final List<VariableModel> existingVariables,
            final List<FunctionModel> functionCalls) {
        
        super(10);

        this.input = input;
        this.step = step;
        this.allowExistingValue = argAllowExistingValue; 
        this.allowConstantInput = argAllowConstantInput;
        this.allowProcedureInput = argAllowProcedureInput;
        this.explicitEmptyOption = explicitEmptyOption;
        
        varMgr = step.getView().getController().getVariableManager();

        setMaxHeight(Region.USE_PREF_SIZE);
        setPrefHeight(Region.USE_COMPUTED_SIZE);
        setMinHeight(Region.USE_PREF_SIZE);
        
        if (allowExistingValue) {            
        	if (existingVariables != null) potentialExistingVars = existingVariables;
            if (functionCalls != null) potentialFunctionCalls = functionCalls; 
            
            existingValueParameterModel = input.copy(); 
            existingValueParameterView = new ParameterView(
                    existingValueParameterModel, step, step.getView(),
                    true, true, null);           
                
            // set the "existing input" line
            if (input.getTerm() instanceof VariableModel ||
                (input.getTerm() instanceof FunctionModel && 
                 (FunctionModel.isAccessorFunction((FunctionModel)input.getTerm())))) {
                existingValueParameterModel.setTerm(input.getTerm());
            } else {
                if (potentialExistingVars.size() > 0) 
                    existingValueParameterModel.setTerm(potentialExistingVars.get(0));
                else if (potentialFunctionCalls.size() > 0)
                    existingValueParameterModel.setTerm(potentialFunctionCalls.get(0));           
            }   
        }
        
        if (allowConstantInput) {
            // Constant value stuff
            constantEditor = ConstantEditor.create(input.getTypeDef(), step, step.getView());
            
            // If the type is unknown, the constant editor will be disabled
            // and we shouldn't allow them to choose fixed value
            if (constantEditor.getPane().isDisable()) {
                this.allowConstantInput = false;
            } else {
                constantEditor.getPane().setPrefWidth(280);
                constantEditor.setDoLiveUpdates(true);                               
        
                Callback<Object, Void> cb = new Callback<Object, Void>() {
                    public Void call(Object val) {
                        input.setTerm(constantEditor.getATRValue());
                        return null; 
                    }
                };
                constantEditor.setOnConfirmed(cb);
                constantEditor.setOnChanged(cb);
            }
        }    
        
        // Wait until now to add radio buttons, in case we determined that we
        // can't support fixed values for this input
        initializeUIComponents();
            
        if (allowProcedureInput) {
            // set the procedure input option
        	
            String name = varMgr != null ? varMgr.createValidName(input.getName()) : input.getName(); 
            
            final ProcedureInputVariableModel pivm = new ProcedureInputVariableModel(name, null);
            pivm.setTypeDef(input.getTypeDef());
            
            userInputModel = new ParameterModel(null, null, input.getTypeDef(), pivm, null, false, null);
            userInputView = new VariableView(pivm, null , step, step.getView());
            userInputView.setGenerateMenuItems(false);
            userInputView.setReadOnly(true);
            
            final Callback<String, Boolean> onOkay = new Callback<String, Boolean>() {
                public Boolean call(String str) {
                    pivm.setVariableName(str);
                    return true;
                }
            };
            
            MenuItem mi = new MenuItem("Rename...");
            mi.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    List<String> forbiddens = new ArrayList<String>();
                    
                    for (ParameterModel pm : step.getStepModel().getInputs())
                        if (pm.getTerm() instanceof ProcedureInputVariableModel)
                            if (!pm.getTerm().equals(pivm) && !pm.getTerm().getTypeDef().equals(pivm.getTypeDef()))
                                forbiddens.add(((ProcedureInputVariableModel)pm.getTerm()).getVariableName());
                    
                    RenameVariableDialog.showDialog(
                            step.getView(),
                            pivm,
                            getScene(), 
                            InputArgumentChooser.this,
                            onOkay,
                            forbiddens);
                }
            });
            userInputView.getNode().getItems().add(mi); 
        } else {
            userInputModel = null;
            userInputView = null; 
        }
        
        if (explicitEmptyOption && 
            (input.getTerm() == null || input.getTerm() instanceof NullValueModel))
            rbExplicitEmpty.setSelected(true); 
        else if (allowExistingValue &&
            ( potentialExistingVars.size() > 0 || 
              potentialFunctionCalls.size() > 0))
            rbExistingValue.setSelected(true);
        else if (allowConstantInput)
            rbConstantValue.setSelected(true);
        else if (allowProcedureInput)
            rbProcedureInput.setSelected(true);
    }  
    
    private void initializeUIComponents() {
        String instruct = input.getName();
        if (input.getTypeDef() != null)
            instruct += 
                " (" + TypeUtilities.getAName(input.getTypeDef()) + ")";        
        instruct += ": " + input.getDescription();
        instructions.setText(instruct);
        getChildren().add(instructions);

        if (explicitEmptyOption) {
            getChildren().add(
                    makeRow(rbExplicitEmpty,
                            "No value", "No value"));
        }
        if (allowExistingValue && 
        		( potentialExistingVars.size() > 0 || 
        		  potentialFunctionCalls.size() > 0)) {
            getChildren().add(
                    makeRow(rbExistingValue, "Use this existing value:",
                            "Use an existing value"));            
            generateExistingValueMenu();
            ((Pane)rbExistingValue.getParent()).getChildren().add(existingValueParameterView);
        }

        if (allowProcedureInput) {
            getChildren().add(
                    makeRow(rbProcedureInput, "Ask the user for "
                            + TypeUtilities.getAName(input.getTypeDef())
                            + ", call it: ", "Ask the user for "
                            + TypeUtilities.getAName(input.getTypeDef())));
        }

        if (allowConstantInput) {
            getChildren().add(
                    makeRow(rbConstantValue,
                            "Always use "
                            + TypeUtilities.getThisName(input.getTypeDef()), 
                            "Always use the same "
                            + TypeUtilities.getName(input.getTypeDef())));
        }
    }
    
    private Pane makeRow(
            final RadioButton rb, 
            final String labelSelected, 
            final String labelDeselected) {

        final FlowPane box = new FlowPane(10, 10);
        rb.setText(labelDeselected);
        rb.setFont(Fonts.DIALOG_TEXT);
        box.getChildren().addAll(rb);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefHeight(Region.USE_COMPUTED_SIZE);
        box.setMinWidth(Region.USE_PREF_SIZE);
        box.setMaxWidth(Region.USE_PREF_SIZE);
        
        final Rectangle rectangle = new Rectangle();
        rectangle.widthProperty().bind(this.widthProperty());
        rectangle.heightProperty().bind(box.heightProperty().add(5));
        rectangle.setFill(fillColor);
        rectangle.setLayoutY(-2.0);
        rectangle.setLayoutX(-4.0);
        rectangle.setStroke(strokeColor);
        rectangle.setStrokeWidth(1.0);
        rectangle.setArcHeight(8.0);
        rectangle.setArcWidth(8.0);
        rectangle.setOpacity(0.0);        
        
        final Pane row = new Pane() {
            @Override
            protected void layoutChildren() {
                box.setPrefWidth(InputArgumentChooser.this.getWidth());
                super.layoutChildren();                
            } 
            
            @Override
            protected double computePrefHeight(double width) {
                return Math.max(20, box.getHeight());
            }            
        };

        rb.selectedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed (ObservableValue<? extends Boolean> value, Boolean oldVal, Boolean newVal) {
                rectangle.setOpacity(newVal ? 0.7 : 0.0);
                if (newVal)
                    rb.setText(labelSelected);
                else
                    rb.setText(labelDeselected);
                
                InputArgumentChooser.this.layout();
            }            
        });
        
        row.getChildren().addAll(rectangle, box);
 
        return row; 
    }
    
    
    private void generateExistingValueMenu() {
        TermView currentView = existingValueParameterView.getTermView();
        currentView.setGenerateMenuItems(false);
        
        // Disable sub-terms in function calls. This is hacky.  
        if (currentView instanceof AccessorFunctionView)
            for (Node n : ((AccessorFunctionView)currentView).getNode().getChildren())
                if (n instanceof HBox)
                    n.setDisable(true);

        List<MenuItem> items = new ArrayList<MenuItem>();
        for (int i = 0; i < Math.min(potentialExistingVars.size(), 4); i++) {
            final VariableModel variable = potentialExistingVars.get(i);
            EventHandler<ActionEvent> action = new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    existingValueParameterModel.setTerm(variable);
                    input.setTerm(variable); 
                    generateExistingValueMenu();
                };
            };
            items.add(VariableView.makeVariableMenuItem(variable,
                    existingValueParameterModel.getTerm(), action));
        }

        if ( ( potentialExistingVars.size() > 4) || 
             ( potentialFunctionCalls.size() > 0 ) ) {
            final MenuItem moreValuesButton = new MenuItem("More...");
            moreValuesButton.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    ITermChosenCallback cb = new ITermChosenCallback() {
                        public void callback(TermModel term) {
                            existingValueParameterModel.setTerm(term);
                            input.setTerm(term); 
                            generateExistingValueMenu();
                        }
                    };
                    ReplaceVariableDialog.showDialog(
                            potentialExistingVars,
                            potentialFunctionCalls, input,
                            InputArgumentChooser.this.getScene(), step.getView(), cb);
                }
            });
            items.add(new SeparatorMenuItem());
            items.add(moreValuesButton);
        }
        currentView.setSpecialMenuItems(items);  
    }       
   
    //******************************** UI Components ***************************
    
    private final Text instructions = new Text(); {
        instructions.wrappingWidthProperty().bind(prefWidthProperty());
        instructions.setFont(Fonts.DIALOG_EMPHASIZED);    
    }

    private final ToggleGroup tg = new ToggleGroup(); 
    
    private final RadioButton rbExistingValue = new RadioButton(" "); {
        rbExistingValue.setToggleGroup(tg);
        rbExistingValue.selectedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> value, Boolean oldVal, Boolean newVal) {
                existingValueParameterView.setVisible(newVal);
                
                if (newVal) 
                    input.setTerm(existingValueParameterModel.getTerm());
                
                step.layout();
            }
        });
    }
    
    private final RadioButton rbProcedureInput = new RadioButton(" "); {
        rbProcedureInput.setToggleGroup(tg);
        rbProcedureInput.selectedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> value, Boolean oldVal, Boolean newVal) {
                userInputView.getNode().setVisible(newVal);
                
                if (newVal) {
                    input.setTerm(userInputModel.getTerm());
                    
                    if (rbProcedureInput.getParent() != userInputView.getNode().getParent())
                        ((Pane)rbProcedureInput.getParent()).getChildren().add(userInputView.getNode());
                }
                
                step.layout();
            }
        });
    }
    
    private final RadioButton rbExplicitEmpty = new RadioButton(" "); {
        rbExplicitEmpty.setToggleGroup(tg);
        rbExplicitEmpty.selectedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(
                    final ObservableValue<? extends Boolean> value, 
                    final Boolean oldVal, 
                    final Boolean newVal) {

                if (newVal)
                    input.setTerm(null);
                
                step.layout();
            }
        });
    }
    
    private final RadioButton rbConstantValue = new RadioButton(" "); {
        rbConstantValue.setToggleGroup(tg);
        rbConstantValue.selectedProperty().addListener(
            new ChangeListener<Boolean>() {
                public void changed(
                        final ObservableValue<? extends Boolean> value,
                        final Boolean oldVal, 
                        final Boolean newVal) {

                    Node ed = constantEditor.getPane();
                    if (newVal) {
                        input.setTerm(constantEditor.getATRValue());
                        
                        if (input.getTerm() == null)
                            input.setTerm(NullValueModel.NULL);

                        if (rbConstantValue.getParent() != ed.getParent())
                            ((Pane) rbConstantValue.getParent())
                                    .getChildren().add(ed);
                    } else
                        ((Pane)rbConstantValue.getParent()).getChildren().remove(ed);
                    
                    step.layout();
                }
            });
    }   

    private final LinearGradient strokeColor = new LinearGradient(0.0, 0.0,
            1.75, 0.0, true, null, new Stop[] {
                    new Stop(0.25, Colors.SelectedStepBorder),
                    new Stop(1.0, Colors.SelectedStepBackground) });
    private final LinearGradient fillColor = new LinearGradient(0.0, 0.0, 2.0,
            0.0, true, null, new Stop[] {
                    new Stop(0.1, Colors.SelectedStepBackground),
                    new Stop(1.0, Colors.SelectedStepLightBackground) });
}
