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

package com.sri.tasklearning.ui.core.control.constant;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.util.Callback;

import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.TooltipPlus;
import com.sri.tasklearning.ui.core.popup.ComplexEditorDialog;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.CompositeTermModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * ConstantEditor for editing 'complex' values, which consist of collections
 * and structs. This ConstantEditor is merely a button with some preview
 * text for the value in question. Pressing the button will cause an appropriate
 * editing dialog to open. 
 */
public class ComplexConstantEditor extends ConstantEditor {
    protected final TypeDef type;
    protected final Button launcher = new Button("Click to configure");
    protected final StepView step;
    protected final CommonView pv;
    protected Scene scene;
    protected CompositeTermModel term = null; 
    
    public ComplexConstantEditor(final TypeDef type,
                                 final StepView step,
                                 final CommonView pv) {
        this.type = type;
        this.step = step; 
        this.pv = pv;
        
        launcher.setMinWidth(150);
        launcher.prefHeightProperty().bind(pane.prefHeightProperty());
        launcher.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                final ComplexEditorDialog ced = getEditorDialog(); 
                Callback<Object, Void> onOkay = new Callback<Object, Void>() {
                    public Void call(Object value) {
                        term = ced.getTerm();
                        launcher.setText(term.getPreviewText());
                        if (onConfirmed != null)
                            onConfirmed.call(value);
                        return null;
                    }
                };
                
                Runnable onCancelPressed = new Runnable() {
                    public void run() {
                        if (onCanceled != null)
                            onCanceled.run();
                    }
                };
                
                ced.setOnConfirmed(onOkay);
                ced.setOnCanceled(onCancelPressed);
                ced.showDialog();
            }
        });
        pane.getChildren().add(launcher);
    }
    
    protected ComplexEditorDialog getEditorDialog() {
        Scene argScene = (scene == null) ? launcher.getScene() : scene;
        return ComplexEditorDialog.create(argScene, launcher, type, term, null,
                step, (ProcedureView) pv);
    }    
    
    @Override
    public void setTooltip(IToolTipCallback cb) {
        super.setTooltip(cb);
        if (cb == null)
            launcher.setTooltip(null);
        else
            launcher.setTooltip(new TooltipPlus(pane));
    }
    
    // **************** ConstantEditor abstract methods ************************
    
    @Override
    public void setValue(Object val) {
        
    }
    
    @Override    
    public Object getValue() {
        return null; 
    }
    
    @Override    
    public void setAtrValue(final TermModel term) {
        if (term instanceof CompositeTermModel) {
            this.term = (CompositeTermModel)term;
            launcher.setText(this.term.getPreviewText());
        } else if (term == null || term instanceof NullValueModel)  {
            this.term = null;
        }
    }

    @Override
    public TermModel getATRValue() {
        if (term != null)
            return term;
        else
            return NullValueModel.NULL;
    }
    
    @Override    
    public Object getDefault() {
        return null;
    }    
    
    @Override
    public void select() {
        
    }
    
    @Override
    public boolean isDialog() {
        return true;
    }
    
    @Override
    public void openDialog(Scene scene) {
        this.scene = scene;
        launcher.fire();
    }
}
