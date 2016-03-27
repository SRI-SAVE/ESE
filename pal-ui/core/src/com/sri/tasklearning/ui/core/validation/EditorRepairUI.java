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

package com.sri.tasklearning.ui.core.validation;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import com.sri.tasklearning.ui.core.Colors;

/**
 * Visualization for repair operations. These are displayed within instances of
 * {@link com.sri.tasklearning.ui.core.step.dialog.ErrorCorrectionPanel}
 */
public class EditorRepairUI extends HBox {

    private static final double HEIGHT = 20;
    private EditorRepairOperation suggestion;     
    private String messageText = "Repair suggestion";
    
    public EditorRepairUI(EditorRepairOperation editorRepairOperation, String message) {
        super(2);
                
        suggestion = editorRepairOperation; 
        messageText = message; 
        
        setMinWidth(Region.USE_PREF_SIZE);
        setMaxWidth(Region.USE_PREF_SIZE);
        setPrefWidth(Region.USE_COMPUTED_SIZE);
        
        setMinHeight(HEIGHT);
        setMaxHeight(HEIGHT);
        setPrefHeight(HEIGHT);
        
        this.setAlignment(Pos.CENTER_LEFT);
        
        getChildren().add(makeLabel(messageText));    
    }
  
    public void addContent(Node node) {
        getChildren().add(node);
    }
    
    public void removeContent(Node node) {
        getChildren().remove(node);
    }

    public void addText(String string) {
        getChildren().add(makeLabel(string));
    }    

    public EditorRepairOperation getSuggestion() {
        return suggestion;
    }

    private Label makeLabel(String text) {
        Label lbl = new Label(text);
        //lbl.setFont(Fonts.DIALOG_TEXT) using this font was causing label to be cut off due to RT-13924
        lbl.setTextFill(Colors.DefaultText);
        lbl.setPrefHeight(HEIGHT);
        lbl.setWrapText(false);
        return lbl;
    }
}
