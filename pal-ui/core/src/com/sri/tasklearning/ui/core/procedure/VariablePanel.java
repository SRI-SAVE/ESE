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

import java.util.Collections;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.EditSessionManager;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.IVariableWatcher;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.PanelHeader;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.control.TermSplitMenuButton;
import com.sri.tasklearning.ui.core.popup.RenameVariableDialog;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.VariableView;

/**
 * Pane that displays all variables known to a procedure. This is used on the
 * right side of the Procedure Editor. Provides "highlight all occurrences"
 * and "show first occurrence" functionality for each variable. 
 */
public final class VariablePanel extends Pane implements IVariableWatcher {    
    public static final double DEF_WIDTH = 156;
    
    private static final Logger log = LoggerFactory
            .getLogger(VariablePanel.class); 
    
    private final ScrollPanePlus scrollPane = new ScrollPanePlus();
    private VBox vbox;
    private ProcedureEditController control;
    private final PanelHeader header;   
    
    public VariablePanel(final ProcedureEditController control) {        
        this.setWidth(DEF_WIDTH);
        this.setMinWidth(DEF_WIDTH);
        this.setPrefWidth(DEF_WIDTH);
        this.setMaxWidth(DEF_WIDTH);
        
        this.control = control;
        
        control.getVariableManager().registerVarWatcher(this);
        
        Label title = new Label("Values");
        title.setFont(Fonts.STANDARD);
        
        scrollPane.setStyle("-fx-border-width: 1; -fx-border-color: -pal-SystemDarkGray; -fx-background-color: white;");
        
        header = new PanelHeader(title, null);
        header.setPrefWidth(DEF_WIDTH - 1);        

        getStyleClass().add("variable-panel");

        scrollPane.setTranslateY(-1);
        scrollPane.setTranslateX(-1);
        
        getChildren().addAll(header, scrollPane);
        
        loadVariables(); 
    }
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        
        scrollPane.setPrefWidth(getWidth() + 3);
        scrollPane.setPrefHeight(getHeight() - header.getHeight() + 3);
        
        super.layoutChildren();
        
        scrollPane.relocate(0, header.getHeight() - 1);
    }
    
    //************************ IVariableWatcher *******************************
    @Override
    public void onAddedVariable(VariableModel newVar) {
        loadVariables();
    }
    @Override
    public void onRemovedVariable(VariableModel removedVar) {
        loadVariables();
    }
    @Override
    public void onRenamedVariable(VariableModel newVar) {
        loadVariables();
    }
    
    public void loadVariables() {
        vbox = new VBox(5);
        vbox.setStyle("-fx-background-color: white;");      
        
        List<VariableModel> vars = control.getVariableManager().getVariables();
        Collections.sort(vars);
        
        for (VariableModel model : vars) {
            VariableView n = buildVarView(model);           
            vbox.getChildren().add(n.getNode());
        }
        
        vbox.setPadding(new Insets(10, 0, 0, 10));
        scrollPane.setContent(vbox);
    }
    
    private EventHandler<MouseEvent> getHighlightAction(final VariableView vv) {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {                
                control.highlightVariable(vv.getVarModel());
                
                if (e.getClickCount() > 1)
                    scrollToOrigin(vv.getVarModel());                
            }
        };
    }    
    
    private VariableView buildVarView(VariableModel model) {

        final VariableView vv = new VariableView(model, null, null, control.getView());
        final TermSplitMenuButton button = (TermSplitMenuButton)vv.getNode();
        vv.setReadOnly(true);
          
        vv.setGenerateMenuItems(false);
        button.setOnAction(null);
        button.setOnMousePressed(getHighlightAction(vv));   
        button.getStyleClass().add("divided-split-menu-button");

        if (model instanceof ProcedureInputVariableModel)
            // set the tooltip based on type
            button.setTooltip(new Tooltip("Procedure input: "
                    + TypeUtilities.getAName(model.getTypeDef()) + "\n"
                    + "Click to highlight all instances;\n"
                    + "double-click to scroll to this value's origin."));
        else
            // *should* be existing value view
            button.setTooltip(new Tooltip("Existing value: "
                    + TypeUtilities.getAName(model.getTypeDef()) + "\n"
                    + "Click to highlight all instances;\n"
                    + "double-click to scroll to this value's origin."));
        
        final MenuItem mi = new MenuItem(VariableView.HIGHLIGHT_MSG);
        mi.setOnAction(new EventHandler<ActionEvent>() {            
            public void handle(ActionEvent e) {
                final VariableModel var = vv.getVarModel();
                if (var.isHighlighted()) {
                    control.unhighlightVariable(var);
                    mi.setText(VariableView.HIGHLIGHT_MSG);
                } else {                                  
                    control.highlightVariable(var);
                    mi.setText(VariableView.UNHIGHLIGHT_MSG);
                }
            }
        });
        
        MenuItem mi2 = new MenuItem("Show first occurrence");
        mi2.setOnAction(new EventHandler<ActionEvent> () {
           public void handle(ActionEvent e) {
               vv.getVarModel().setHighlighted(false);
               mi.getOnAction().handle(null);
               scrollToOrigin(vv.getVarModel());
           }
        });

        MenuItem mi3 = new MenuItem("Copy");
        mi3.setOnAction(new EventHandler<ActionEvent> () {
            public void handle(ActionEvent e) {
                ProcedureEditController.setClipboardTerm(vv.getVarModel()); 
            }
         });
        
        MenuItem mi4 = new MenuItem("Rename...");
        mi4.setOnAction(new EventHandler<ActionEvent> () {
            public void handle(ActionEvent e) {
                RenameVariableDialog.showDialog(
                        EditSessionManager.getActiveSession().getController().getView(),
                        vv.getVarModel(),
                        vv.getNode().getScene(), 
                        vv.getNode(),
                        null, null);
            }
         }); 
        
        button.getItems().addAll(mi, mi2, new SeparatorMenuItem(), mi3, mi4);

        return vv;
    }
    
    // scroll to the origin
    private void scrollToOrigin(VariableModel sender) {
        if (sender instanceof ProcedureInputVariableModel) {
            control.getView().getSelectionManager().selectNone();
            control.getView().getScrollPane().scrollToTop(); // procedureView inputs are all at the top
        }
        else {
            // for existing values, crawl
            StepView origin = control.findOriginStepView(sender);
            if (origin != null) {
                origin.requestFocus();
                CommonView proc = control.getView();
                proc.getScrollPane().scrollIntoView(origin);
                proc.getSelectionManager().selectOnly(origin); 
            } else
                log.error("Unable to find origin step for " + sender);
        }
    }    
}
