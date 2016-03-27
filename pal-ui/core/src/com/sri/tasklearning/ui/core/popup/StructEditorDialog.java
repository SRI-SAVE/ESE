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

package com.sri.tasklearning.ui.core.popup;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

import com.sri.pal.StructDef;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.CompositeTermModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;

/**
 * A popup dialog for editing structure values. The appearance/behavior of this
 * dialog changes depending on whether the user is configuring a default
 * value for a procedure input or if they're configuring an 'inline' structure
 * in a procedure. In the former case, the user cannot reference variables so
 * all values are presented inline. In the latter case, variable references are 
 * possible and so struct values are represented as term buttons. 
 */
public class StructEditorDialog extends ComplexEditorDialog {
    private final StructDef structType;
    
    protected StructEditorDialog(
            final Scene scene, 
            final Node sender,
            final StructDef type, 
            final CompositeTermModel argTerm,
            final Callback<Object, Void> argOnConfirmed,
            final StepView step,
            final ProcedureView pv) {
        super(scene, sender, type, argTerm, argOnConfirmed, step, pv);
    
        structType = type;
        
        String title = "Editing the details of " + TypeUtilities.getAName(type);
              
        updateTitleText(title);
        
        refresh();
    }
    
    @Override
    protected void refresh() {
        grid.getChildren().clear();
        
        if (newTerm == null)
            return; 
                
        // Skip the first input because it's the hidden type input
        for (int i = 1; i < newTerm.getInputs().size(); i++) {
            final ParameterModel pm = newTerm.getInputs().get(i);
            
            final Label lbl = new Label(structType.getFieldName(i - 1));
            final Node n = getNodeForParameterModel(pm, step != null);
            GridPane.setConstraints(lbl, 0, i);
            GridPane.setConstraints(n, 1, i);

            grid.getChildren().addAll(lbl, n);            
        }
    }
    
    @Override
    protected boolean saveChanges() {
        boolean save = true;
        
        // TODO: Enforce non-nullable types once we support them
//        for (int i = 0; i < structType.size(); i++)
//            if (newTerm.getInputs().get(i).getVariable() == null) {
//                save = false;
//                break;
//            }
//        
//        if (!save) {
//            Alert.show("Must specify all required values",
//                       "You must specify all required values", 
//                       AlertConfig.OK, null);            
//        }
        return save;
    }
    
    @Override
    public Pane getContent() {
        final Label lbl = new Label("Current values");
        lbl.setLayoutX(PANE_PAD);
        final ScrollPanePlus scroll = new ScrollPanePlus();
        scroll.setContent(grid);
        scroll.setStyle("-fx-background-color: -pal-LoopBackgroundDient;");
        grid.prefWidthProperty().bind(scroll.prefWidthProperty().subtract(15));

        final Pane pane = new Pane() {
            @Override
            protected void layoutChildren() {
                scroll.setPrefWidth(this.prefWidth(-1) - 2 * PANE_PAD);
                scroll.setPrefHeight(this.prefHeight(-1)
                        - (lbl.getBoundsInParent().getMaxY() + 2 * PANE_PAD));

                super.layoutChildren();

                scroll.relocate(PANE_PAD, lbl.getHeight() + PANE_PAD);
            }
        };

        pane.getChildren().addAll(lbl, scroll);        
        return pane;        
    }
    
    // ************************* UI Components ********************************

    private final GridPane grid = new GridPane();
    {
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setHgap(10);
        grid.setVgap(10);
    }       
}
