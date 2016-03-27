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

package com.sri.tasklearning.ui.core.step.dialog;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.VariableManager;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;

/**
 * Abstract base class for content of a {@link DialogStepView}.
 */
public abstract class DialogContentPanel extends Pane {
    
    protected SimpleBooleanProperty canContinue = new SimpleBooleanProperty(true);
    protected StepModel stepModel;
    protected String title;
    protected VariableManager varManager;
    protected EditController control;
    
    protected DialogContentPanel(StepView argView) {
        stepModel  = argView.getStepModel();
        control    = argView.getView().getController();
        varManager = control.getVariableManager();
        
        setPrefWidth(Region.USE_COMPUTED_SIZE);
        setMinWidth(Region.USE_PREF_SIZE);
        setMaxWidth(Region.USE_PREF_SIZE);
        
        setPrefHeight(Region.USE_COMPUTED_SIZE);
        setMinHeight(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);
        
        title = "Dialog for \u201C" + stepModel.getName() + "\u201D";
    }      
    
    @Override
    protected double computePrefWidth(double height) {
        return 500;
    }
    
    @Override
    protected double computePrefHeight(double width) {
        return 180;
    }    
        
    // Intended to be overriden
    public void activated (boolean isActive) {}
    public IUndoable writeChanges() {return null;}
    public void abandonChanges() {}
    
    // *********************** Getters/Setters ********************************
    
    public SimpleBooleanProperty canContinueProperty() {
        return canContinue; 
    }
    
    public String getTitle() {
        return title; 
    }
}
