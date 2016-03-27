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

package com.sri.tasklearning.ui.core.control;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import com.sri.tasklearning.ui.core.control.TextAreaPlus.ITextInputCallback;

/**
 * Extension of TextField that provides enforcement of a maximum length and 
 * provides callbacks for significant events such as losing focus. Also allows
 * for the specification of invalid characters and/or regex that can be used
 * to regulate the content of the TextField. 
 */
public class TextFieldPlus extends TextField {
    
    public static final String PATTERN_PROC_NAME= "[\\w_\\- ]*";

    private String pattern; 
    private String[] invalidChars;
    private ITextInputCallback callback;
    private String oldText;
    private int maximumLength = -1;

    private static EventHandler<KeyEvent> releaseHandler = new EventHandler<KeyEvent>() {
        public void handle(KeyEvent e) {
            TextFieldPlus tfp = (TextFieldPlus)e.getSource();
            if (tfp.maximumLength >= 0) {
                if (tfp.getText().length() > tfp.maximumLength)
                    tfp.setText(tfp.getText().substring(0, tfp.maximumLength));
            }
            if (e.getCode() == KeyCode.ENTER)               
                tfp.commit();           
        }
    };
           
    public TextFieldPlus() {
        super();      
        
        setOnKeyReleased(releaseHandler);       
        
        focusedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> val,
                    Boolean oldVal, Boolean newVal) {                
                commit();
            }
        });
    }
    
    public void setCallback(ITextInputCallback callback) {
        this.callback = callback;
    }
    
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void commit() {
        if (checkRegex() && checkInvalidChars()) {
            final String newText = getText();
            if (!newText.equals(oldText)) {
                if (callback != null)
                    callback.callback(oldText, newText);
                
                oldText = newText; 
            }
        } else {
            // This is kind of mean to do, but if we don't do this then the user
            // will continue to see their illegal textbox value and may believe
            // they were able to use that value when they actually weren't. 
            setText("");
            oldText = "";
        }
    }

    private boolean checkRegex() {
        if (pattern == null)
            return true;
        
        return getText().matches(pattern);
    }
    
    public void setInvalidChars(String[] invalidChars) {
        this.invalidChars = invalidChars.clone();
    }

    private boolean checkInvalidChars() {
        if (invalidChars == null)
            return true;        
        
        String val = getText();
        for (int i = 0; i < invalidChars.length; i++)
            if (val.contains(invalidChars[i])) {
                return false;
            }
        return true;
    }

    public void setTextPlus(String text) {
        super.setText(text);
        oldText = text; 
    }
    
    public int getMaximumLength() {
        return maximumLength;
    }

    public void setMaximumLength(int maximumLength) {
        this.maximumLength = maximumLength;
    }
}
