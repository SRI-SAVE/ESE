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

package com.sri.tasklearning.ui.core.library;

import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.EditSession;
import com.sri.tasklearning.ui.core.EditSessionManager;
import com.sri.tasklearning.ui.core.ProcedureMap;
import com.sri.tasklearning.ui.core.StorageAssistant;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.control.Alert.AlertResult;
import com.sri.tasklearning.ui.core.control.TextFieldPlus;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;

/**
 * A dialog for saving a procedure (under a new name). This dialog is used when
 * a new procedure is saved, or when the user chooses to 'Save As' on an 
 * existing procedure. Similar in appearance to a {@code ProcedureManager} and
 * likewise utilizes {@code ProcedureBrowser} for procedure listing/selection
 * capability. 
 */
public class SaveProcedureDialog extends Pane {       
    private final Scene owner;
    private final Stage stage;
    private final ProcedureModel procModel;
    private final Callback<CommonModel, Void> onSuccess;
    private final ProcedureBrowser browser;
    private final boolean rename; 
    
    private static final double HEIGHT = 400;
    private static final double WIDTH = 600;    
    private static final double FOOTER_HEIGHT = 80;
    private static final double PADDING = 3;    
    private static final double BUTTON_HEIGHT = 28;
    private static final double BUTTON_WIDTH = 90;
    private static final double BUTTON_PAD = 15;
    private static final double BUTTON_Y = HEIGHT - FOOTER_HEIGHT + (FOOTER_HEIGHT / 2) - (BUTTON_HEIGHT);
    
    public SaveProcedureDialog(final Scene owner, 
                               final ProcedureModel procModel, 
                               final Callback<CommonModel, Void> onSuccess, 
                               final String title, 
                               final String buttonText, 
                               final boolean rename) {
        this.owner = owner;
        this.procModel = procModel;
        this.onSuccess = onSuccess;
        this.rename = rename; 
          
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL); 
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);
        stage.setTitle(title);
        stage.setResizable(false);
        stage.setOnHidden(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent e) {
                for (Node n : owner.getRoot().getChildrenUnmodifiable())
                    if (!n.disableProperty().isBound())
                        n.setDisable(false);
            }});

        Scene scene = new Scene(this);
        stage.setScene(scene);
        scene.setFill(Colors.SystemLightGray);
        
        Utilities.initPalStage(stage, scene);
        
        browser = new ProcedureBrowser(null);
        browser.setPrefHeight(HEIGHT - FOOTER_HEIGHT);
        browser.setPrefWidth(WIDTH - 4 * PADDING);
        browser.relocate(PADDING, 0);

        if (saveButton != null)
            saveButton.setText(buttonText);        
        
        getChildren().addAll(browser, saveButton, closeButton, name);
    }    

    public void showSaveDialog() {
        for (Node n : owner.getRoot().getChildrenUnmodifiable())
            if (!n.disableProperty().isBound())
                n.setDisable(true);
              
        stage.show();
        stage.toFront();
    }   
    
    // ************************** UI Components *******************************   
    
    private final TextFieldPlus name = new TextFieldPlus(); {
        name.setPattern(TextFieldPlus.PATTERN_PROC_NAME);
        name.setPromptText("Enter a unique name");
        name.relocate(PADDING, BUTTON_Y + 4);
        name.setPrefWidth(WIDTH - 2 * BUTTON_WIDTH - 4 * BUTTON_PAD);
        name.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) { 
                // TODO get rid of this length code when JavaFX has its own
                // setMaximumLength() method. 
                if (name.getText().length() > 80)
                    name.setText(name.getText().substring(0, 80));
                if (e.getCode().equals(KeyCode.ENTER)) {
                    name.commit();                    
                    saveButton.getOnAction().handle(null);
                }
                else if (e.getCode().equals(KeyCode.ESCAPE)) {
                    closeButton.getOnAction().handle(null);
                } else {
                    saveButton.setDisable(name.getText().trim().length() == 0);
                }
            }
        });
    };
    
    private final Button saveButton = new Button(); {
    saveButton.setGraphicTextGap(9.0);
    saveButton.setPrefHeight(BUTTON_HEIGHT);
    saveButton.setPrefWidth(BUTTON_WIDTH);
    saveButton.relocate(WIDTH - BUTTON_WIDTH * 2 - 2 * BUTTON_PAD, BUTTON_Y);
    saveButton.setText("Save");
    saveButton.setDefaultButton(true);
    saveButton.setTooltip(new Tooltip("Save the procedure."));                  
    saveButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            owner.setCursor(Cursor.WAIT);
            final String newName;
            name.commit();
            
            if (name.getText() != null)
                newName = name.getText().trim();
            else
                newName = "";
            
            if (newName.equals(procModel.getName()))
                return; // If they didn't change anything, do nothing
            
            if (newName.length() > 0
                    && (!ActionModelAssistant.getInstance()
                            .isProcedureNameInUse(newName))) {
                save(newName);
            } else if (newName.length() > 0) {
                final Set<String> callers = ProcedureMap.getInstance()
                    .getCallers(newName);
                final boolean usedAsStep = (callers != null && callers
                     .size() > 0) ? true : false;
                            
                if (usedAsStep) {
                    Alert.show("Cannot overwrite procedure",
                               "A procedure named \"" + newName +
                               "\" already exists and is used as a step " +
                               "in other procedures. You may not ovewrite it.",
                               AlertConfig.OK, null);
                    return;
                }
                
                Callback<AlertResult, Void> call = new Callback<AlertResult, Void>() {
                    public Void call(AlertResult result) {
                        if (result == AlertResult.YES) {
                            save(newName); 
                        }
                        return null; 
                    }
                };

                Alert.show("Overwrite procedure?",
                           "A procedure named \"" + newName +
                           "\" already exists. " +
                           "Are you sure you want to overwrite it?",
                           AlertConfig.YES_NO,
                           call);

            } else {            
                Alert.show("Illegal procedure name",
                           "Your procedure name contains illegal characters." +
                           "Please specify a different name",
                           AlertConfig.OK, null);
            }
            owner.setCursor(Cursor.DEFAULT);
        }});
    }
    
    private void save(String newName) {
        boolean result;
        
        String oldFunctor = procModel.getFunctor();
        
        result = rename ? StorageAssistant.renameProcedure(newName, procModel) 
                        : StorageAssistant.saveProcedure(newName, procModel, true);
        
        String newFunctor = procModel.getFunctor(); 

        if (rename) 
            for (EditSession sess : EditSessionManager.getSessions())
                sess.getController().getModel()
                        .updateFunctorReferences(oldFunctor, newFunctor);        

        stage.hide();

        if (result && onSuccess != null)
            onSuccess.call(procModel);
    }

    private final Button closeButton = new Button("Cancel"); {
        closeButton.setGraphicTextGap(9.0);
        closeButton.setPrefHeight(BUTTON_HEIGHT);
        closeButton.setPrefWidth(BUTTON_WIDTH);
        closeButton.relocate(WIDTH - BUTTON_PAD - BUTTON_WIDTH, BUTTON_Y);
        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                stage.hide();
            }
        });
    }    
}
