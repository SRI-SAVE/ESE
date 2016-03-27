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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.VariableManager;
import com.sri.tasklearning.ui.core.common.SignatureModel;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.step.LoopView;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.VariableView;

/**
 * A popup dialog for configuring the published results of a procedure. 
 */
public class PublishResultsDialog extends BasicModalPopup {
    private final List<VariableModel> publishedList = new ArrayList<VariableModel>();
    private final SignatureModel sig;
    private final VariableManager varMgr;
    private final ProcedureEditController control;
    
    public static void showDialog(ProcedureView pv, Scene owner, Node sender) {
        PublishResultsDialog prd = new PublishResultsDialog(pv);
        prd.show(owner, sender);
    }

    private PublishResultsDialog(final ProcedureView pv) {
        this.control = pv.getController();
        this.varMgr  = control.getVariableManager();
        this.sig     = control.getModel().getSignature();
        
        titleText = "Select which values to publish";
        ok.setDisable(false);
        
        prefHeight = 300; // Make this guy a bit taller
        
        for (VariableModel res : sig.getResults())
            publishedList.add(res);

        onOkayPressed = new Callback<Object, Boolean>() {
            public Boolean call(Object obj) {
                control.changePublishedResults(publishedList);
                return true;
            }
        };
    }
    
    @Override
    public void focus() {
        
    }
    
    @Override
    public Pane getContent() {
        final ScrollPanePlus scrollPane = new ScrollPanePlus();
        final GridPane grid = new GridPane();

        Pane pane = new Pane() {
            @Override
            protected void layoutChildren() {
                scrollPane.setPrefHeight(getHeight());
                scrollPane.setPrefWidth(getWidth() - PAD * 2);
                scrollPane.relocate(PAD, 0);
                
                grid.setPrefWidth(scrollPane.getPrefWidth() - 20);                 
                grid.getColumnConstraints().clear();
                grid.getColumnConstraints().add(
                        new ColumnConstraints(grid.getPrefWidth() / 2));

                super.layoutChildren();
            }
        };

        grid.setPadding(new Insets(10, 0, 0, 10));
        grid.setVgap(8);

        List<VariableModel> vars = new ArrayList<VariableModel>();
        for (VariableModel var : varMgr.getVariables()) {
            // Can't publish inputs
            if (var instanceof ProcedureInputVariableModel)
                continue;
        
            final StepView origin = control.findOriginStepView(var);
            
            // Can't publish loop iterands
            if (origin == null || origin instanceof LoopView)
                continue;
            
            // Can't publish variables bound in loops
            if (origin.getStepViewContainer() instanceof LoopView)
                continue;

            vars.add(var);
        }

        Collections.sort(vars);
            
        int col = 0, row = 0;
        for (VariableModel var : vars) {
            final VariableModel tempVar = var;
            
            HBox box = new HBox(10);
            box.setAlignment(Pos.CENTER_LEFT);
            VariableView vv = new VariableView(var, null, null, null);
            vv.setDisable(true);
            CheckBox cb = new CheckBox();
            cb.setAllowIndeterminate(false);
            cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
                public void changed(ObservableValue<? extends Boolean> value,
                        Boolean oldVal, Boolean newVal) {
                    if (newVal) {
                        if (!publishedList.contains(tempVar))
                            publishedList.add(tempVar);
                    }
                    else
                        publishedList.remove(tempVar);
                }
            });
            
            if (sig.getResults().contains(var))
                cb.setSelected(true);
            
            box.getChildren().addAll(cb, vv.getNode());
            grid.getChildren().add(box);
            GridPane.setConstraints(box, col, row++);
            if (row > vars.size() / 2) {
                col = 1;
                row = 0;
            }
        }

        scrollPane.setContent(grid);
        pane.getChildren().add(scrollPane);

        return pane;
    }
}
