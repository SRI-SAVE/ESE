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

import javafx.scene.image.Image;

import com.sri.tasklearning.ui.core.control.Knurling;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.step.StepModel;

/**
 * Visualization for an action (step) in the library, which may be a true
 * action, procedure step or tool (such as a loop step). 
 */
public class LibraryRowStep extends LibraryRowBasicUI {
    private final StepModel stepModel;
    private final boolean dialog;
    private final Knurling knurling = new Knurling("Click and drag to add a new step"); {
        knurling.setVisible(false);
        knurling.visibleProperty().bind(selected);
    }
    
    public LibraryRowStep(
            final String argText, 
            final Image argIcon, 
            final StepModel stepModel, 
            final boolean dialog) {
        super(argText, argIcon);
        
        this.stepModel = stepModel;
        this.dialog = dialog; 
        
        setToolTipCallback(new IToolTipCallback() {
            public String getToolTipText() {
                return stepModel.getName() + ":\n" + stepModel.getDescriptionText();
            }
        });
        
        label.maxWidthProperty().bind(widthProperty().subtract(10));
        
        if (!dialog)
            getChildren().add(knurling);
    }

    public StepModel getStepModel() {
        return stepModel;
    }
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        
        rectangle.setLayoutX(0);
        
        if (!dialog && getChildren().contains(knurling)) {
            double kwidth = knurling.getLayoutBounds().getWidth();
            double kheight = knurling.getLayoutBounds().getHeight();
            knurling.relocate(getWidth() - kwidth - PAD, 
                              (getHeight() - kheight) / 2);
        }
    }    
}
