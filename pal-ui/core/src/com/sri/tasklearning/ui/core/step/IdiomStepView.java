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

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;

public class IdiomStepView extends StepViewBasicUI implements IStepViewContainer {
    private final IdiomStepModel idiomStepModel;
    private StepLayout stepLayout; 
    
    public IdiomStepView(
            final IdiomStepModel argModel, 
            final IStepViewContainer argParent, 
            final CommonView argProcView) {
        super(argModel, argParent, argProcView);
        
        idiomStepModel = argModel;
        
        super.addChildren();
        
        Region content = createContentArea();
        if (content != null)
            getChildren().add(content);           
    }
    
    /****************** StepViewBasicUI Overrides *****************************/
    
    @Override
    protected void getAdditionalExpansionContent(
            final GridPane grid, 
            final int row) {

        stepLayout = new StepLayout((IdiomStepModel)stepModel, this, procedureView);
        
        // These fellas don't get mouse events since they are strongly owned
        // by the idiom step view
        for (StepView step : stepLayout.getStepViews()) {
            step.setOnMousePressed(null);
            step.setOnMouseReleased(null);
            step.setOnMouseDragged(null);
        }
        
        Label lbl = new Label("details");
        lbl.setFont(Fonts.STANDARD_TINY);
        lbl.setTextFill(Colors.DisabledText);
        lbl.setTextAlignment(TextAlignment.RIGHT);        
        GridPane.setConstraints(lbl, 0, row, 1, 1, HPos.RIGHT, VPos.TOP);
        GridPane.setConstraints(stepLayout, 1, row, 1, 1, HPos.LEFT, VPos.TOP);
        grid.getChildren().addAll(lbl, stepLayout);
    }
        
    /******************* IStepViewContainer ***********************************/
    
    @Override
    public ContainerStepModel getContainerStepModel() {
        return idiomStepModel; 
    }
    
    @Override
    public StepLayout getStepLayout() {
        return stepLayout; 
    }
    
    @Override
    public void updateIssuesVisualization() {
        for (StepView view : stepLayout.getStepViews()) {
            view.updateIssueVisualization();
            if (view instanceof IStepViewContainer)
                ((IStepViewContainer)view).updateIssuesVisualization();
        }
    }
    
    @Override
    public StepView findStepView(StepModel step) {
        return stepLayout.findStepView(step);
    }

}
