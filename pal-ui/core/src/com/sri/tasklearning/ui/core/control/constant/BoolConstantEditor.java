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

import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.TooltipPlus;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * ConstantEditor for editing boolean values.
 */
public class BoolConstantEditor extends ConstantEditor {
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private final ChoiceBox<String> cb = new ChoiceBox<String>();
    private final boolean asString;
    private final TypeDef type;
    
    public BoolConstantEditor(final boolean asString, 
                              final TypeDef type,
                              final String...extraOpts) {
        this.asString = asString;
        this.type = type; 
        
        cb.getItems().addAll(TRUE, FALSE);
        cb.getItems().addAll(extraOpts); 
        cb.getSelectionModel().selectedItemProperty()
                .addListener(new ChangeListener<String>() {
            public void changed(final ObservableValue<? extends String> value, 
                                final String oldVal, 
                                final String newVal) {
                if (onConfirmed != null)
                    onConfirmed.call(getValue());
            }
        });
            
        pane.getChildren().add(cb);
    }   
    
    @Override
    public void setValue(final Object val) {
        if (!asString && val instanceof Boolean) {
            final String strVal = ((Boolean)val) ? TRUE : FALSE;
            cb.getSelectionModel().select(strVal);
        } else if (val != null) {
            String valStr = val.toString().toLowerCase();
            cb.getSelectionModel().select(valStr);
        } else
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
    public Object getValue() {
        if (cb.getSelectionModel().getSelectedItem() == null)
            return null;
        
        if (asString) 
            return cb.getSelectionModel().getSelectedItem();
        else 
            return cb.getSelectionModel().getSelectedItem().equals(TRUE) ? true
                   : false;
    }  
    
    @Override
    public TermModel getATRValue() {
        if (getValue() == null)
            return NullValueModel.NULL;
        
        ConstantValueModel cvm = new ConstantValueModel(getValue(), type);        
        return cvm; 
    }
    
    @Override
    public Object getDefault() {
        if (asString)
            return "true";
        else
            return true;
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
