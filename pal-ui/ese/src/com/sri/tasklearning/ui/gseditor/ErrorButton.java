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

package com.sri.tasklearning.ui.gseditor;

import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.text.TextAlignment;

import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.EditSession;
import com.sri.tasklearning.ui.core.EditSessionManager;
import com.sri.tasklearning.ui.core.EditSessionManager.ISessionListener;
import com.sri.tasklearning.ui.core.ISelectable;
import com.sri.tasklearning.ui.core.SelectionManager;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.step.StepModel;

public class ErrorButton extends Button implements ISessionListener {

    private EditController control; 
    private Separator errorSep;
    private int lastNumErrors = -1;
    private int lastNumWarnings = -1;

    public ErrorButton(final GsEditor editor, final Separator errorSep) {
        super();

        this.errorSep = errorSep;
        
        EditSessionManager.addSessionListener(this);
        
        this.setPrefHeight(GsEditorToolBar.DEF_HEIGHT - 10);
        setContentDisplay(ContentDisplay.LEFT);
        setTextAlignment(TextAlignment.CENTER);
        setTooltip(new Tooltip("Click to select the next step with an error or warning."));      

        setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                SelectionManager mgr = control.getView().getSelectionManager();
                
                int index;
                if (mgr != null) {
                    ISelectable top = mgr.getTopSelection();
                    index = top != null ? top.getIndex() : -1;
                } else
                    index = -1;

                List<StepModel> steps = control.getStepsWithIssues();
                StepModel target = steps.get(0);

                for (StepModel step : steps)
                    if (step.getIndex() > index) {
                        target = step;
                        break;
                    }

                control.selectStep(target);
            }
        });
    }
    
    private InvalidationListener listen = new InvalidationListener() {
        public void invalidated(Observable val) {
            int newNumErrors = control.numStepsWithErrorsProperty().getValue();
            int newNumWarnings = control.numStepsWithWarningsProperty()
                    .getValue();

            if (newNumErrors != lastNumErrors
                    || newNumWarnings != lastNumWarnings) {
                String errLabel = (newNumErrors == 1) ? " step" : " steps";
                String warLabel = (newNumWarnings == 1) ? " step"
                        : " steps";

                String text = "";
                if (newNumErrors > 0)
                    text += newNumErrors + errLabel + " with errors";
                if (newNumErrors > 0 && newNumWarnings > 0)
                    text += ", ";
                if (newNumWarnings > 0)
                    text += newNumWarnings + warLabel + " with warnings";

                setText(text);
                if (newNumErrors == 0 && newNumWarnings == 0) {
                    errorSep.setVisible(false);
                    setVisible(false);
                } else {
                    errorSep.setVisible(true);
                    setVisible(true);
                    if (lastNumErrors <= 0 && newNumErrors > 0) {
                        setGraphic(Utilities
                                .getImageView("error_small.png"));
                    } else if (newNumErrors == 0 && lastNumWarnings <= 0) {
                        setGraphic(Utilities
                                .getImageView("warning_small.png"));
                    }
                }
            }
            lastNumErrors = newNumErrors;
            lastNumWarnings = newNumWarnings;
        }
    };
    
    public void activeSessionChanged(
            final EditSession oldSession, 
            final EditSession newSession) {
        if (oldSession != null) {
            oldSession.getController().numStepsWithErrorsProperty().removeListener(listen);
            oldSession.getController().numStepsWithWarningsProperty().removeListener(listen);
        }
        
        if (newSession == null) {
            control = null;
            return; 
        }
            
        control = newSession.getController();
        
        control.numStepsWithErrorsProperty().addListener(listen);
        control.numStepsWithWarningsProperty().addListener(listen);
        
        listen.invalidated(null); 
    }
}
