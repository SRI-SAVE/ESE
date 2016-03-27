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

import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.util.repair.RepairOperation;
import com.sri.ai.lumen.util.repair.RepairSuggestion;
import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.layout.TextFlowLayout;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.validation.EditorIssue;
import com.sri.tasklearning.ui.core.validation.EditorRepairOperation;
import com.sri.tasklearning.ui.core.validation.EditorRepairUI;

/**
 * A panel for a DialogStepView that knows how to display repair suggestions
 * for a validation error and allows the user to choose an appropriate
 * repair suggestion. 
 */
public class ErrorCorrectionPanel extends DialogContentPanel {
    private EditorIssue issue;
    private static final double X_INDENT = 22;
    private VBox vbox = new VBox(10);
    private RepairSuggestion selectedSuggestion = null;
    
    private static final Logger log = LoggerFactory
        .getLogger(ErrorCorrectionPanel.class);  
    
    public ErrorCorrectionPanel(StepView step, EditorIssue argIssue) {
        super(step);
        
        canContinue.setValue(false);
        issue = argIssue; 
        
        title = (issue.isError() ? "Error" : "Warning") + " in step " + 
                (stepModel.getIndex() + 1) + ": \u201C" + stepModel.getName() + "\u201D";        

        TextFlowLayout tfl = new TextFlowLayout(issue.getFancyMessage(false));
        tfl.setFont(Fonts.DIALOG_TEXT);
        tfl.setTextColor(Colors.DefaultText);
        tfl.prefWrapLengthProperty().bind(widthProperty());
        tfl.setHgap(4);
        tfl.setEffect(null);
        tfl.setDisable(true);       
        
        vbox.getChildren().add(tfl); 
        
        ToggleGroup tg = new ToggleGroup();
        for (RepairSuggestion suggestion : issue.getSuggestions()) {
            Node result = makeSuggestionRow(suggestion, tg);  
           
           if (result != null)
               vbox.getChildren().add(result);
        }       
        this.getChildren().add(vbox); 
    }
    
    @Override 
    public double computePrefHeight(double width) {
        return vbox.prefHeight(width);
    }    

    @Override
    public void activated (boolean isActive) {
        // nothing here (yet)
    }

    @Override
    public IUndoable writeChanges() {
        if (selectedSuggestion != null) {
            List<RepairOperation> ops = selectedSuggestion.getOperations();
            int i = 1;
            for (RepairOperation op : ops) {
                if (((EditorRepairOperation)op).applyOperation() == false) 
                    log.error("Failed to apply operation {} ({} of {})", new Object[] {op, i, ops.size()});                
                i++;
            }
        }
        return null;
    }

    private Node makeSuggestionRow(final RepairSuggestion suggestion, final ToggleGroup toggleGroup) {

        final RadioButton rb = new RadioButton(" ");
        rb.setLayoutX(2);
        rb.setToggleGroup(toggleGroup);
        rb.setFocusTraversable(false);
        rb.selectedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> value,
                    Boolean newVal, Boolean oldVal) {
                    if (value.getValue()) {
                        // It's selected
                        canContinue.setValue(true);
                        selectedSuggestion = suggestion; 
                    }
                }
            });        

//        // overlay to allow any click to set the radio button
//        final Rectangle overlayRect = new Rectangle();
//        overlayRect.setFill(Color.TRANSPARENT);
//        overlayRect.setStroke(null);
//        overlayRect.setOnMousePressed(new EventHandler<MouseEvent>() {
//                @Override
//                public void handle(MouseEvent event) {
//                    rb.setSelected(true);
//                    // rb.requestFocus();
//                }
//            });
        
        // make a panel listing each operation for this suggestion
        final Pane opPane = new Pane() {
            protected void layoutChildren() {
                super.layoutChildren(); // I cannot stress how important it is to remember this!
                double widest = 10;
                double y = 0;
                
                for (Node child : getChildren()) {
                    if (child instanceof EditorRepairUI) {
                        EditorRepairUI opUi = (EditorRepairUI)child;
                        opUi.relocate(0, y);
                        y += opUi.prefHeight(0);
                        widest = Math.max(widest, opUi.prefWidth(0));
                  }
              }
              setPrefWidth(widest);
              setPrefHeight(y);
              
//              overlayRect.setWidth(widest + X_INDENT);
//              overlayRect.setHeight(y);                                      
            }
        };
        opPane.setLayoutX(X_INDENT);
        
        if (suggestion.getOperations().size() > 0) {
            for (RepairOperation op : suggestion.getOperations()) {
                EditorRepairUI ui = ((EditorRepairOperation)op).getSuggestionUI(issue);
                if (ui != null)
                    opPane.getChildren().add(ui);
            }
        } else {
            // if there are no operations, just print the suggestion
            // note this is a weird case, and shouldn't happen
            Label lbl = new Label(suggestion.toString());
            lbl.setFont(Fonts.DIALOG_TEXT);
            lbl.setTextFill(Colors.DefaultText);
            lbl.setWrapText(false);
            opPane.getChildren().add(lbl);
        }

        if (suggestion.getOperations().size() > 0 &&  opPane.getChildren().size() == 0) {
            // handle the case where there were operations, but none of them are usable
            // e.g., it suggested we change parameters, but there are no valid replacements
            return null;
        } else {
            Pane group = new Pane();
            group.getChildren().addAll(rb, opPane/*, overlayRect*/);
            return group;
        }
    }
}
