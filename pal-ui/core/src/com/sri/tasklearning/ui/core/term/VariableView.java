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

package com.sri.tasklearning.ui.core.term;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.TermSplitMenuButton;
import com.sri.tasklearning.ui.core.popup.RenameVariableDialog;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.step.IStepViewContainer;
import com.sri.tasklearning.ui.core.step.StepView;

/**
 * View for {@link VariableView}. Variable menu buttons are blue. Procedure
 * input variables are a slightly different blue than normal (step output) 
 * variables. Variables have options for replacing with another variable,
 * providing a fixed value, etc. 
 */
public class VariableView extends MenuButtonTermView {    
    private VariableModel varModel;
    
    public VariableView(
            final VariableModel argVarModel,
            final ParameterModel argParamModel,
            final StepView argStepView,
            final CommonView argProcView) {
        super(argVarModel, argParamModel, argStepView, argProcView);
        
        varModel = argVarModel;
        configureButton();
    }
    
  
    @Override
    protected void configureButton() {
        super.configureButton();       

        button.getStyleClass().clear();
        if (varModel instanceof ProcedureInputVariableModel)
            button.getStyleClass().add("input-var");
        else
            button.getStyleClass().add("existing-var");        
    }       
    
    @Override
    public TermSplitMenuButton getNode() {
        return button; 
    }

    public boolean isDisable() {
        return button.isDisable();
    }

    public void setDisable(boolean val) {
        button.setDisable(val);
    }
    
    public BooleanProperty disableProperty() {
        return button.disableProperty();
    }

    public boolean isInline() {
        return inline.getValue();
    }

    public void setInline(boolean val) {
        inline .setValue(val);
    }
    
    public VariableModel getVarModel() {
        return varModel; 
    }
    
    public static final String HIGHLIGHT_MSG = "Highlight all occurrences";
    public static final String UNHIGHLIGHT_MSG = "Unhighlight occurrences";
    @Override
    public List<MenuItem> buildTermOptionsMenu() {
        if (!isGenerateMenuItems())
            return null; 
        
        List<MenuItem> items = super.buildTermOptionsMenu();
              
        if (!isNested() && !BackendFacade.getInstance().isDebuggingProcedure()) {            
            MenuItem rename = new MenuItem("Rename...");
            rename.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    RenameVariableDialog.showDialog(
                            procView,
                            varModel,
                            getNode().getScene(),
                            stepView,
                            null, null);
                }
            });
            
            items.add(rename);
            
            final MenuItem highlight = new MenuItem(varModel.isHighlighted() ? UNHIGHLIGHT_MSG : HIGHLIGHT_MSG);
            highlight.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    if (varModel.isHighlighted()) {
                        controller.unhighlightVariable(varModel);
                        highlight.setText(HIGHLIGHT_MSG);
                    } else {
                        controller.highlightVariable(varModel);
                        highlight.setText(UNHIGHLIGHT_MSG); 
                    }
                }
            });
            
            items.add(highlight);
        }
        
        return items;
    }
    
    // Makes an awesome menu item that displays the view for the given model, and shows a checkmark if that's the current selection
    public static MenuItem makeVariableMenuItem(
            final VariableModel model, 
            final TermModel currentSelection, 
            final EventHandler<ActionEvent> action) {
        
        final SVGPath check = new SVGPath();
        check.setContent("M0,5H2L4,8L8,0H10L5,10H3Z");
        check.setStroke(null);
        check.setFill(Colors.SystemDarkGray);
        check.setVisible(currentSelection != null && model.equals(currentSelection));

        Label lbl = new Label(model.getVariableName());
        
        HBox hbox = new HBox(4);
        if (model instanceof ProcedureInputVariableModel)
            hbox.getStyleClass().add("input-var");
        else
            hbox.getStyleClass().add("existing-var");
        hbox.setStyle("-fx-padding: 2,2,2,2;");
        hbox.getChildren().addAll(lbl, check);
        hbox.setAlignment(Pos.CENTER_LEFT);
        
        CustomMenuItem nmi = new CustomMenuItem(hbox, true);        
        nmi.setOnAction(action);

        return nmi;
    }
}
