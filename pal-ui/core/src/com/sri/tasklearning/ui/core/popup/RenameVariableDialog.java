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

package com.sri.tasklearning.ui.core.popup;

import java.util.Collection;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.Callback;

import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.VariableManager;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.constant.StringConstantEditor;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableModel;

/**
 * A popup dialog for renaming a variable. Utilizes methods in 
 * {@code VariableManager} to insure unique/legal variable names. 
 */
public class RenameVariableDialog extends BasicModalPopup {
    private CommonView procView;
    private VariableModel variable;
    private boolean replace = false;
    private StringConstantEditor constantEditor = new StringConstantEditor(null);
    private Collection<String> forbiddens;

    public static void showDialog(
            final CommonView procView,
            final VariableModel variable,
            final Scene scene, 
            final Node sender,
            final Callback<String, Boolean> onOkay,
            final Collection<String> forbiddens) {
        RenameVariableDialog rvd = new RenameVariableDialog(procView, variable, onOkay, forbiddens);
        rvd.show(scene, sender);
    }

    protected RenameVariableDialog(
            final CommonView procView,
            final VariableModel variable,
            final Callback<String, Boolean> onOkay,
            final Collection<String> forbiddens) {
        
        this.procView = procView;
        this.variable = variable;
        this.forbiddens = forbiddens; 
        
        titleText = "Rename";
        okButtonText = "Set Name";

        onOkayPressed = new Callback<Object, Boolean>() {
            public Boolean call(Object obj) {
                String newName = constantEditor.getValue().toString().trim();
                
                // No-op, but not an error
                if (newName.equals(variable.getVariableName()))
                    return true; 
                
                if (onOkay != null) {
                    onOkay.call(newName);
                    return true;
                }
                
                // Otherwise, this is the default behavior for the dialog
                if (replace)
                    procView.getController().renameReplaceVariable(variable, newName);
                else
                    procView.getController().renameVariable(variable, newName);
                
                return true;
            }
        };

        onLoaded = new Runnable() {
            public void run() {
                constantEditor.select();
            }
        };
    }

    @Override
    public void focus() {
        constantEditor.select();
        constantEditor.end();
    }

    public Pane getContent() {
        final Text instructions = new Text("Enter a new name for \""
                + variable.getVariableName() + "\" ("
                + TypeUtilities.getAName(variable.getTypeDef()) + ")");
        instructions.setFont(Fonts.DIALOG_TEXT);

        final Label errorLabel = new Label(
                "New name must be unique and must start with a letter.");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setGraphicTextGap(6);
        ImageView iv = Utilities.getImageView("error.png");
        iv.setFitHeight(18);
        iv.setFitWidth(18);
        iv.setPreserveRatio(true);
        errorLabel.setGraphic(iv);

        constantEditor.setValue(variable.getVariableName());
        constantEditor.setDoLiveUpdates(true);
        constantEditor.setOnChanged(new Callback<Object, Void>() {
            public Void call(Object value) {
                String newName = ((String)value).trim();

                if (!newName.equals(variable.getVariableName())) {
                    VariableManager varMgr = procView.getController().getVariableManager();
                    
                    boolean allow = false;
                    
                    if (varMgr.isValidName(newName) && varMgr.isNewName(newName))
                        allow = true;
                    else if (newName.equalsIgnoreCase(variable.getVariableName())) 
                        allow = true;
                    else if (varMgr.isValidName(newName)) {
                        VariableModel other = varMgr.getVariableByName(newName);
                        
                        if (other != null && (!other.isBound() || !variable.isBound()))
                            if (other.getTypeDef().equals(variable.getTypeDef())) {
                                allow = true;
                                replace = true; 
                            }                        
                    }
                    
                    if (forbiddens != null)
                        for (String forb : forbiddens)
                            if (forb.equalsIgnoreCase(newName))
                                allow = false;
                    
                    if (allow) {                        
                        if (!replace && (newName).trim().length() > VariableModel.MAX_NAME_LENGTH) {
                            // name is too long, show error
                            ok.setDisable(true);
                            errorLabel
                                .setText("Name cannot be more than "
                                        + VariableModel.MAX_NAME_LENGTH
                                        + " characters in length;\ncurrently it is "
                                        + newName.length() + " characters.");
                            errorLabel.setVisible(true);
                        } else {
                            // name is ok, so enable OK button and hide error
                            ok.setDisable(false);
                            errorLabel.setVisible(false);
                        }
                    } else {
                        // name is invalid, show error
                        ok.setDisable(true);
                        errorLabel
                                .setText("New name must be unique and must start with a letter.");
                        errorLabel.setVisible(true);
                    }
                } else {                
                    ok.setDisable(false);
                    errorLabel.setVisible(false);
                }
                return null; 
            }
        });

        // map the Enter/Esc keys to OK/Cancel
        constantEditor.setOnConfirmed(new Callback<Object, Void>() {
            public Void call(Object value) {
                if (!ok.isDisabled())
                    ok.fire();
                return null; 
            }
        });
        constantEditor.setOnCanceled(new Runnable() {
            public void run() {
                cancel.fire();
            }
        });

        Pane content = new Pane() {
            protected void layoutChildren() {
                errorLabel.setPrefHeight(18);
                errorLabel.setPrefWidth(getWidth() - PAD * 2);

                instructions.setWrappingWidth((int) (getWidth() - PAD * 2));
                final Pane ed = constantEditor.getPane();
                ed.setPrefWidth(getWidth() - 4 - PAD * 2);

                super.layoutChildren();

                instructions.relocate(PAD, PAD);

                ed.setLayoutY(instructions.getBoundsInParent()
                        .getMaxY() + PAD);
                ed.setLayoutX(PAD);

                errorLabel.setLayoutY(ed.getLayoutY() + ed.getHeight() + PAD);
                errorLabel.setLayoutX(PAD);
            }
        };

        content.getChildren().addAll(instructions, errorLabel, constantEditor.getPane());

        return content;
    }
}
