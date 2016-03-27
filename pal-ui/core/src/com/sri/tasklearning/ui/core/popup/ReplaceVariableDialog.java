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

import java.util.Collections;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.tasklearning.ui.core.PalUiException;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TermView;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.VariableView;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;

/**
 * A popup dialog for replacing a reference to one variable with a reference to
 * another. Note that this dialog is only used if there are more than x 
 * (currently 4) replacement options, in which case a menu item becomes 
 * available on the term that provides access to this popup. 
 */
public class ReplaceVariableDialog extends BasicModalPopup {
    private static final Logger log = LoggerFactory
            .getLogger(ReplaceVariableDialog.class);
    
    private final List<VariableModel> vars;
    private final List<FunctionModel> funcs;
    private final ParameterModel argument;
    private final CommonView procView;
    
    private final ToggleGroup tg = new ToggleGroup();
    {
        tg.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            public void changed(ObservableValue<? extends Toggle> value,
                    Toggle oldVal, Toggle newVal) {
                ok.setDisable(false);
            }
        });
    };

    public static void showDialog(
            final List<VariableModel> vars,
            final List<FunctionModel> funcs,
            final ParameterModel arg,
            final Scene owner, 
            final CommonView sender, 
            final ITermChosenCallback callback) {
        ReplaceVariableDialog rvd = new ReplaceVariableDialog(sender, vars, funcs, arg, callback);
        rvd.show(owner, sender);
    }

    protected ReplaceVariableDialog(
            final CommonView procView,
            final List<VariableModel> vars,
            final List<FunctionModel> funcs,
            final ParameterModel arg, 
            final ITermChosenCallback callback) {
        this.vars = vars;
        this.funcs = funcs;
        this.argument = arg;
        this.procView = procView;

        Collections.sort(vars);

        prefWidth = 600;
        prefHeight = 450;
        
        titleText = "Choose a Replacement Value";
        okButtonText = "Replace Value";
        ok.setDisable(true);

        onOkayPressed = new Callback<Object, Boolean>() {
            public Boolean call(Object obj) {
                // replace the variable with a constant using the value entered
                TermView tv = ((TermViewRadioButton) tg
                        .getSelectedToggle()).getTermView();
                callback.callback(tv.getTermModel()); 
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
        final VBox vbox = new VBox(8);
        final GridPane grid = new GridPane();
        
        vbox.setPadding(new Insets(10, 0, 0, 10));

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

        grid.setVgap(8);

        int col = 0, row = 0;
        for (VariableModel var : vars) {
            HBox box = new HBox(10);
            box.setAlignment(Pos.CENTER_LEFT);
            VariableView vv = new VariableView(var, null, null, null);
            vv.setDisable(true);
            TermViewRadioButton rb = new TermViewRadioButton(vv);
            rb.setToggleGroup(tg);
            box.getChildren().addAll(rb, vv.getNode());
            grid.getChildren().add(box);
            GridPane.setConstraints(box, col, row++);
            if (row > vars.size() / 2) {
                col = 1;
                row = 0;
            }
        }

        vbox.getChildren().add(grid);
        
        for (FunctionModel func : funcs) {
            HBox box = new HBox(10);
            box.setAlignment(Pos.CENTER_LEFT);
            try {
                TermView tv = TermView.create(func, argument, null, procView, true);
                tv.getNode().setDisable(true);
                TermViewRadioButton rb = new TermViewRadioButton(tv);
                rb.setToggleGroup(tg);
                box.getChildren().addAll(rb, tv.getNode());
                vbox.getChildren().add(box);
            } catch (PalUiException e) {
                log.error("Failed to create term view for " + func.getDisplayString());
            }
        }
        
        scrollPane.setContent(vbox);
        pane.getChildren().add(scrollPane);

        return pane;
    }

    private static class TermViewRadioButton extends RadioButton {
        private TermView tv;

        private TermViewRadioButton(TermView tv) {
            super();

            this.tv = tv;
        }

        private TermView getTermView() {
            return tv;
        }
    }
    
    public interface ITermChosenCallback {
        public void callback(TermModel term);
    }
}
