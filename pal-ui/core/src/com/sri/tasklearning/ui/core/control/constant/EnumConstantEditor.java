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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;

import com.sri.pal.EnumeratedTypeDef;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.TooltipPlus;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * ConstantEditor for editing enum values. Enum values are always represented
 * as strings. 
 */
public class EnumConstantEditor extends ConstantEditor {
    private final ChoiceBox<String> cb = new ChoiceBox<String>();
    private final EnumeratedTypeDef type;
    
    public EnumConstantEditor(final EnumeratedTypeDef type) {    
        this.type = type;
        
        for (String value: this.type.getValues())
            cb.getItems().add(value);
        
        	cb.getSelectionModel().selectedItemProperty()
                .addListener(new ChangeListener<String>() {
            public void changed(final ObservableValue<? extends String> value, 
                                final String oldVal, 
                                final String newVal) {                
                if (onConfirmed != null)
                    onConfirmed.call(newVal);
            }
        });
            
        pane.getChildren().add(cb);
    }
    
    @Override
    public void setValue(final Object val) { 
        if (val != null && val instanceof String) {
            final String strVal = val.toString();
            cb.getSelectionModel().select(strVal);
        } else if (val == null)
            cb.getSelectionModel().clearSelection();
    }
    
    @Override
    public void setAtrValue(TermModel term) {
        if (term != null && term instanceof ConstantValueModel)
            setValue(((ConstantValueModel)term).getValue());
        else if (term == null || term instanceof NullValueModel)
            setValue(null);
    }
    
    @Override
    public String getValue() {
        if (cb.getSelectionModel().getSelectedItem() != null)
            return cb.getSelectionModel().getSelectedItem();
        return null;
    }  
    
    @Override
    public TermModel getATRValue() {
        String val = getValue();
        if (val != null) {
            ConstantValueModel cvm = new ConstantValueModel(cb
                    .getSelectionModel().getSelectedItem(), type);
            return cvm;
        }
        
        return NullValueModel.NULL;
    }
    
    @Override
    public String getDefault() {
        return cb.getItems().get(0);
    }
    
    @Override
    public void setTooltip(IToolTipCallback call) {
        super.setTooltip(call);
        if (call == null)
            cb.setTooltip(null);
        else
            cb.setTooltip(new TooltipPlus(pane));
    }    
    
    public void end() {
    }
    
    @Override
    public void select() {
        cb.requestFocus();
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
