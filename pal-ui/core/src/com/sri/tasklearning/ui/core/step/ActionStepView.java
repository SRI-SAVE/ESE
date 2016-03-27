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

package com.sri.tasklearning.ui.core.step;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ParameterView;

/**
 * View for a {@link ActionStepModel}. Most of the visual elements come from
 * {@code StepViewBasicUI}. This class provides logic for displaying 
 * "unimportant" input (inputs that do not appear in the step header via the 
 * fancyName) as well as any outputs. 
 */
public class ActionStepView extends StepViewBasicUI {
	
    public ActionStepView(
            final ActionStepModel argModel, 
            final IStepViewContainer argParent, 
            final CommonView argProcView) {
        super(argModel, argParent, argProcView);

        super.addChildren();
        Region content = createContentArea();
        if (content != null)
            getChildren().add(content);
        recalcHeight();
    }

    @Override
    protected void getAdditionalExpansionContent(
            final GridPane grid, 
            final int row) {

        List<ParameterModel> unimportantInputs = new ArrayList<ParameterModel>();
        for (ParameterModel pm : ((ActionStepModel)stepModel).getInputs())
            if (!pm.isImportant())
                unimportantInputs.add(pm);                        
        
        if (unimportantInputs.size() > 0) {
            Label inputsLabel = new Label("other inputs");
            inputsLabel.setFont(Fonts.STANDARD_TINY);
            inputsLabel.setTextFill(Colors.DisabledText);
            inputsLabel.setAlignment(Pos.TOP_RIGHT);       
            GridPane.setConstraints(inputsLabel, 0, row, 1, 1, HPos.RIGHT,
                    VPos.TOP, Priority.NEVER, Priority.NEVER, new Insets(5, 0, 0, 0));

            FlowPane inputs = new FlowPane();
            inputs.prefWidthProperty().bind(
                    grid.widthProperty().subtract(EXP_COL_1_WIDTH));

            inputs.setHgap(PAD * 2);
            inputs.setVgap(PAD);           
            
            for (ParameterModel input : unimportantInputs) {
                ParameterView pv = new ParameterView(input, this, procedureView, false, false, null);
                inputs.getChildren().add(pv);
                params.add(pv);
            }
            GridPane.setConstraints(inputs, 1, row, 1, 1, HPos.LEFT, VPos.TOP);
            grid.getChildren().addAll(inputsLabel, inputs);
        }
    }
}
