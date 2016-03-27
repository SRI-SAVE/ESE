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
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Extension of TextArea that provides enforcement of a maximum length and 
 * provides callbacks for significant events such as losing focus. 
 */
public class TextAreaPlus extends TextArea {    
    private String oldText = ""; 
    private ITextInputCallback callback;
    private int maximumLength = -1;
    
    private static EventHandler<KeyEvent> releaseHandler = new EventHandler<KeyEvent>() {
        public void handle(KeyEvent e) {
            TextAreaPlus tap = (TextAreaPlus)e.getSource();
            if (tap.maximumLength >= 0) {
                if (tap.getText().length() > tap.maximumLength)
                    tap.setText(tap.getText().substring(0, tap.maximumLength));
            }
            if (e.getCode() == KeyCode.ENTER)               
                ((TextFieldPlus)e.getSource()).commit();           
        }
    };
    
    public TextAreaPlus() {
        super();
        
        this.setOnKeyReleased(releaseHandler);
             
        focusedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> val,
                    Boolean oldVal, Boolean newVal) {                
                commit();
            }
        });
    }
    
    public ITextInputCallback getCallback() {
        return callback;
    }

    public void setCallback(ITextInputCallback callback) {
        this.callback = callback;
    }
    
    public void setTextPlus(String text) {
        super.setText(text);
        oldText = text; 
    }
    
    private void commit() {
        final String newText = getText();
        if (!newText.equals(oldText)) {
            if (callback != null) 
                callback.callback(oldText, newText);
            oldText = newText; 
        }
    }
    
    public int getMaximumLength() {
        return maximumLength;
    }

    public void setMaximumLength(int maximumLength) {
        this.maximumLength = maximumLength;
    }
    
    public interface ITextInputCallback {
        public void callback(String oldVal, String newVal);
    }   
}
