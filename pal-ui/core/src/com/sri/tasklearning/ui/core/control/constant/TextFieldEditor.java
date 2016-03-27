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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.TextField;

import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.TooltipPlus;

/** 
 * Abstract ConstantEditor for ConstantEditors that are built on TextField. 
 */
public abstract class TextFieldEditor extends ConstantEditor {
    protected String oldText = "";
    protected TextField textfield = new TextField(); {
        textfield.setText("");
        pane.prefWidthProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable obs) {
                if (pane.equals(icon.getParent()))
                    textfield.setPrefWidth(pane.prefWidth(-1) - HPAD - ICON_SIZE);
                else
                    textfield.setPrefWidth(pane.prefWidth(-1));
            }
        });
        textfield.focusedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> value,
                    Boolean oldVal, Boolean newVal) {
                if (!newVal && !doLiveUpdates) { 
                    confirm();               
                }
            }
        });      
    }
    
    public TextField getTextField() {
        return textfield;
    }
    
    protected void confirm() {
        if (onConfirmed != null)
            onConfirmed.call(getValue());       
    }
    
    protected TextFieldEditor() {        
        pane.getChildren().add(textfield);
        pane.prefHeightProperty().bind(textfield.prefHeightProperty());
    }
    
    @Override
    public void setTooltip(IToolTipCallback cb) {
        super.setTooltip(cb);
        if (cb == null)
            textfield.setTooltip(null);
        else
            textfield.setTooltip(new TooltipPlus(pane));
    }
    
    public void end() {
        textfield.end();
    }
    
    @Override
    public void select() {
        textfield.requestFocus();       
        // 2.0 This method is still flaky in 2.0 textfield.selectAll();
    }
    
    @Override
    public boolean isDialog() {
        return false;
    }
    
    @Override
    public void openDialog(Scene scene) {
        // Intentionally empty
    }
}
