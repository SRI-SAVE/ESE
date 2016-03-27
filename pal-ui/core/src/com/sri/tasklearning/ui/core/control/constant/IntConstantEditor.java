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
import javafx.scene.input.KeyEvent;

import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * ConstantEditor for editing integer values. This includes Short, Integer &
 * Long integer values. It currently does not enforce the ranges of the 
 * different integer flavors. String representations are also supported. 
 */
public class IntConstantEditor extends TextFieldEditor {
    private final Class<?> representationClass;
    private final boolean asString;
    private final TypeDef type;
    
    public IntConstantEditor(final Class<?> representationClass,
                             final TypeDef type,
                             final boolean asString) {
        super();
        
        this.asString = asString; 
        this.representationClass = representationClass;
        this.type = type;
        
        textfield.setOnKeyReleased(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent e) {
                StringBuffer filtered = new StringBuffer();
                for (char c : textfield.getText().toCharArray()) {
                    switch (c) {
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                        case '0':
                            filtered.append(c);
                            break;                                
                    }
                }
                if (!textfield.getText().equals(filtered.toString())) {
                    textfield.setText(filtered.toString());
                    textfield.end();
                }
                
                switch (e.getCode()) {                    
                    case ENTER:
                        confirm();
                        break;
                    case ESCAPE:
                        if (onCanceled != null)
                            onCanceled.run(); 
                        break;
                    default:
                        if (doLiveUpdates && !textfield.getText().equals(oldText)
                            && onChanged != null) {
                            onChanged.call(getValue());
                        }
                      break;
                }
                oldText = textfield.getText();
            }
        });  
    }
    
    @Override
    public void setValue(Object val) {
        if (val == null)
            textfield.setText("");
        else
            textfield.setText(val.toString());   
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
        if (textfield.getText().length() > 0) {
            if (asString)
                return textfield.getText();
            else if (representationClass.equals(Integer.class))
                return Integer.parseInt(textfield.getText());
            else if (representationClass.equals(Long.class))
                return Long.parseLong(textfield.getText());
            else if (representationClass.equals(Short.class))
                return Short.parseShort(textfield.getText());
        }
            
        return null; 
    }  
    
    @Override
    public TermModel getATRValue() {
        Object value = getValue();
        
        if (value != null) {
            ConstantValueModel cvm = new ConstantValueModel(value, type);
            return cvm;
        }
        
        return NullValueModel.NULL;
    }
    
    @Override
    public Object getDefault() {
        if (asString)
            return "0";
        else if (representationClass.equals(Integer.class))
            return Integer.valueOf(0);
        else if (representationClass.equals(Long.class))
            return new Long(0);
        else if (representationClass.equals(Short.class))
            return new Short("0");
        
        return Integer.valueOf(0);
    }      
}
