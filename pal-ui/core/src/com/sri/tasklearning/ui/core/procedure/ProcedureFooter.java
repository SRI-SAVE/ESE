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

import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.IVariableWatcher;
import com.sri.tasklearning.ui.core.control.TermSplitMenuButton;
import com.sri.tasklearning.ui.core.popup.PublishResultsDialog;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.VariableView;

/**
 * Visualizes the footer of a procedure, which includes the current published 
 * results of the procedure and a link for configuring them. 
 */
public final class ProcedureFooter extends Pane implements IVariableWatcher {
    public static final double PREF_HEIGHT = 200;
    
    private ProcedureView procedureView;
    private double contentWidth = 400;
    
    public ProcedureFooter(final ProcedureView argProcView) {
        procedureView = argProcView;
        
        getStyleClass().add("procedure-footer");
        
        setPrefHeight(PREF_HEIGHT);
        setMaxHeight(Region.USE_PREF_SIZE);
        setMinHeight(Region.USE_PREF_SIZE);

        setMinWidth(700);
        
        refreshResults();
        
        link.visibleProperty().bind(procedureView.readOnlyProperty().not());
        resultsView.disableProperty().bind(procedureView.readOnlyProperty());       
        
        this.getChildren().addAll(instructions, resultsView, link);
    }
    
    @Override
    protected void layoutChildren() {
        instructions.setWrappingWidth((int)contentWidth + 1);
        
        double contentIndent = (getWidth() / 2.0) - (contentWidth / 2.0); 
        
        double y = 16;
        instructions.relocate(contentIndent - 1, y);
        y += instructions.getLayoutBounds().getHeight() + 9;

        resultsView.setPrefWidth(contentWidth);
        
        // Resizes resultsView among other things
        super.layoutChildren();        
        
        resultsView.relocate(contentIndent, y);
        y += resultsView.getHeight() + 12;        
        link.relocate(contentIndent - 1, y);
    }
        
    // *********************** IVariableWatcher *******************************
    public void onAddedVariable (VariableModel newVar) {
        // If user undoes a delete, this translates to an add that may
        // reintroduce some published results
        refreshResults();
    }

    public void onRemovedVariable(VariableModel removedVar) {
        refreshResults(); 
    }
    
    public void onRenamedVariable(VariableModel vm) {
        
    }
    
    public void setContentWidth(double contentWidth) { 
        this.contentWidth = contentWidth;
    }
    
    public void refreshResults() {
        List<VariableModel> results = null;
        
        resultsView.getChildren().clear();
        
        if (procedureView.getModel().getSignature() != null)
            results = procedureView.getModel().getSignature().getResults();
        
        if (results != null && results.size() > 0) {         
            // Create the list of procedure results. 
            for (VariableModel result : results) {
                VariableView vv = new VariableView(result, null, null, procedureView);
                vv.setReadOnly(true);
                ((TermSplitMenuButton) vv.getNode()).setTooltip(new Tooltip(
                        "A value published after this procedure has executed \n"
                                + "("
                                + TypeUtilities.getAName(result.getTypeDef())
                                + ")"));
                resultsView.getChildren().add(vv.getNode());
            }
        }
        else {
            Text msg = new Text("No result values are published.");
            msg.setFont(Fonts.DIALOG_ITALICIZED);
            msg.setFill(Colors.DisabledText);
            resultsView.getChildren().add(msg);            
        }
        resultsView.requestLayout();         
    }
       
    // ********************** UI Components ***********************************
    
    private final Text instructions = new Text(); {
        instructions.setFill(Colors.DisabledText);
        instructions.setStroke(null);
        instructions.setTextOrigin(VPos.BASELINE);
        instructions.setFont(Fonts.DIALOG_TEXT);
        instructions.setText("Results from this procedure can be published, " + 
                                "making them available to other procedures. " +
                                "The following values will be published:");            
    }
    
    private final Hyperlink link = new Hyperlink("Select which values to publish\u2026"); {
        link.setFont(Fonts.DIALOG_TEXT);
        link.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                PublishResultsDialog.showDialog(procedureView,
                        ProcedureFooter.this.getScene(), ProcedureFooter.this);
            }
        });
    }
    
    private final FlowPane resultsView = new FlowPane(); {
        resultsView.setMinWidth(Region.USE_PREF_SIZE);
        resultsView.setMaxWidth(Region.USE_PREF_SIZE);
        resultsView.setHgap(10);
        resultsView.setVgap(15);
    }
}
