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

import java.net.MalformedURLException;
import java.net.URL;

import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import com.sri.pal.TypeDef;

/**
 * A special ConstantEditor for editing URL values. 
 * 
 * TODO This was used by the WebTAS 4.3 release but there is currently no way 
 * to trigger this constant editor. 
 */
public class URLConstantEditor extends StringConstantEditor {
    private String lastConfirmed = null;
    
    public URLConstantEditor(TypeDef type) {
        super(type);
        
        pane.getChildren().add(0, icon);
        pane.requestLayout();
        
        // Override handler from StringConstantEditor because this
        // editor doesn't support live updates
        textfield.setOnKeyReleased(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent e) {
                try {
                    new URL(textfield.getText());
                    validValue.setValue(true);
                } catch (MalformedURLException ex) {
                    validValue.setValue(false);
                }
                
                if (e.getCode() == KeyCode.ENTER) {
                    confirm(); 
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    if (onCanceled != null)
                        onCanceled.run(); 
                } else {
                    if (validValue.getValue() && doLiveUpdates
                            && !textfield.getText().equals(oldText)
                            && onChanged != null)
                        onChanged.call(getValue());
                }
                if (validValue.getValue())
                    oldText = textfield.getText();
            }
        }); 
    }
    
    @Override
    public void setValue(Object val) {
        super.setValue(val);
        lastConfirmed = val.toString();
    }
    
    @Override
    protected void confirm() {
        if (!validValue.getValue()) {
            oldText = lastConfirmed;
            validValue.setValue(true); 
            textfield.setText(oldText == null ? "" : oldText); 
            return;
        }
        super.confirm();
        lastConfirmed = textfield.getText();
    }    
}
