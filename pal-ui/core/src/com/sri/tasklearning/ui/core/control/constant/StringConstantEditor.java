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

import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * ConstantEditor for editing string values.
 */
public class StringConstantEditor extends TextFieldEditor {
    private final TypeDef type;
    public StringConstantEditor(TypeDef type) {
        super();
        
        this.type = type;
        
        textfield.setOnKeyReleased(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent e) {
                if (e.getCode() == KeyCode.ENTER) {
                    confirm();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    if (onCanceled != null)
                        onCanceled.run(); 
                } else {
                    if (doLiveUpdates && !textfield.getText().equals(oldText) && onChanged != null)
                        onChanged.call(getValue());
                }
                oldText = textfield.getText();
            }
        }); 
    }
    
    @Override
    public void setValue(Object val) {        
        textfield.setText(val == null ? "" : val.toString());   
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
        return textfield.getText(); 
    }  
    
    @Override
    public ConstantValueModel getATRValue() {
        ConstantValueModel cvm = new ConstantValueModel(textfield.getText(), type);
        return cvm;
    }
    
    @Override
    public String getDefault() {
        return "";
    }
}
