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

import javafx.geometry.VPos;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.control.ToolTippedImageView;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;

/**
 * The small pane at the bottom of the library that lends insight in to the
 * currently selected action or procedure. 
 */
public class LibraryInfoPanel extends Pane {
    public static final double DEF_HEIGHT = 180;
    public static final double PAD = 4;

    private StepModel action;

    public LibraryInfoPanel() {
        getStyleClass().add("library-info-panel");
        setPrefHeight(DEF_HEIGHT);
        
        getChildren().addAll(explanation, name, desc, imageviewwithtooltip);
    }
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        explanation.setWrappingWidth((int)(getWidth() - PAD * 2));
        name.setWrappingWidth((int)(getWidth() - (PAD * 5 + StepView.ICON_SIZE)));
        desc.setWrappingWidth((int)(getWidth() - PAD * 3));
        desc.setLayoutY(Math.max(name.getBoundsInParent().getMaxY() + PAD * 3, StepView.ICON_SIZE + PAD * 3));
    }

    public void setAction(StepModel action) {
        this.action = action;
        
        explanation.setVisible(action == null);
        name.setText((action == null) ? "" : action.getName());

        // set description
        StringBuffer content = new StringBuffer("");
        if (action != null) {
            content.append(action.getDescriptionText() + "\n ");
            if (action.getInputs().size() > 0) {
                content.append("\nInputs: ");
                for (ParameterModel input : action.getInputs())
                    content.append(input.getName() + ", ");
                content = new StringBuffer(content.substring(0, content.length()-2));
            }
            if (action.getResults().size() > 0) {
                content.append(
                    "\nResult: " + 
                    TypeUtilities.getName(action.getResults().get(0).getTypeDef()));
            }
            imageviewwithtooltip.setImage(Utilities.getImage(action.getIconPath()));
        } 
        
        desc.setText(content.toString());
        this.requestLayout();
    }
    
    // ************************** UI Components *******************************

    private final Text explanation = new Text(PAD, PAD * 2,
            "No action selected."); {
        explanation.setTextOrigin(VPos.TOP);
        explanation.setFill(Colors.DisabledText);
        explanation.setFont(Fonts.STANDARD_ITALICIZED);
        explanation.setWrappingWidth((int) getWidth());
    }

    private final ToolTippedImageView imageviewwithtooltip = new ToolTippedImageView();{
        imageviewwithtooltip.setToolTipCallback(new IToolTipCallback() {
            public String getToolTipText() {
                return "This action is part of \"" + action.getNamespace() + "\".";
            }
        });
        imageviewwithtooltip.setX(PAD);
        imageviewwithtooltip.setY(PAD);
        imageviewwithtooltip.setPreserveRatio(true);
        imageviewwithtooltip.setFitWidth(StepView.ICON_SIZE);
        imageviewwithtooltip.setFitHeight(StepView.ICON_SIZE);
    }

    private final Text name = new Text(); {
        name.setLayoutX(PAD * 3 + StepView.ICON_SIZE);
        name.setLayoutY(PAD + 5);
        name.setTextOrigin(VPos.TOP);
        name.setFont(Fonts.DIALOG_TITLE);
        name.setWrappingWidth((int) (getWidth() - PAD * 5 + StepView.ICON_SIZE));
    }

    private final Text desc = new Text(); {
        desc.setX(PAD * 2);
        desc.setY(PAD * 2);
        desc.setFont(Fonts.STANDARD);
        desc.setTextOrigin(VPos.TOP);
    }
}
