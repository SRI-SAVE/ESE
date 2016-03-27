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

package com.sri.tasklearning.ui.core.layout;

import java.util.ArrayList;
import java.util.List;

import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.exercise.ExerciseView;
import com.sri.tasklearning.ui.core.step.ActionStepModel;
import com.sri.tasklearning.ui.core.step.ActionStepView;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseGroupOfStepsModel;
import com.sri.tasklearning.ui.core.step.ExerciseGroupOfStepsView;
import com.sri.tasklearning.ui.core.step.ExerciseStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepView;
import com.sri.tasklearning.ui.core.step.ExerciseSubtaskModel;
import com.sri.tasklearning.ui.core.step.ExerciseSubtaskView;
import com.sri.tasklearning.ui.core.step.IStepViewContainer;
import com.sri.tasklearning.ui.core.step.IdiomStepModel;
import com.sri.tasklearning.ui.core.step.IdiomStepView;
import com.sri.tasklearning.ui.core.step.LoopModel;
import com.sri.tasklearning.ui.core.step.LoopView;
import com.sri.tasklearning.ui.core.step.PlaceholderStepView;
import com.sri.tasklearning.ui.core.step.ProcedureStepModel;
import com.sri.tasklearning.ui.core.step.ProcedureStepView;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.step.StepViewBasicUI;
import com.sri.tasklearning.ui.core.step.dialog.DialogStepView;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.VBox; 

/**
 * An extension of VBox intended for visualizing an ordered group of steps. 
 * The top-level of a procedure has its own StepLayout, as does each loop in 
 * a procedure regardless of nesting. 
 */
public final class StepLayout extends VBox {
    
	public static final int INTER_STEP_PADDING = 20;
	
	// public static final int INTER_STEP_PADDING = 14;

    private CommonView procedureView;
    private IStepViewContainer parentView;
    private ContainerStepModel containerStepModel; // Loop or Procedure
        
    public StepLayout(
            final ContainerStepModel argContainer, 
            final IStepViewContainer argParent, 
            final CommonView argProcView) {
        super(INTER_STEP_PADDING);       
        
        this.setFillWidth(true);
        
        // this.setTranslateX(-55);
        
        containerStepModel = argContainer; 
        parentView = argParent;
        procedureView = argProcView; 
        
        setPadding(new Insets(INTER_STEP_PADDING, 0, INTER_STEP_PADDING, 0));
        
        for (StepModel step : containerStepModel.getSteps()) {
            StepViewBasicUI stepView = (StepViewBasicUI) createStepView(step);   
            if (stepView != null)
            	getChildren().add(stepView);            
        }
        
        setOnMousePressed(procedureView.getOnMousePressed());
        setOnMouseReleased(procedureView.getOnMouseReleased());
        setOnMouseDragged(procedureView.getOnMouseDragged());
    }   

    @Override
    protected double computeMinWidth(double height) {

        double maxStepWidth = 0;
        for (Object stepObj : getChildren()) {
            if (stepObj instanceof DialogStepView)
                continue;
            if (((StepView) stepObj).prefWidth(0) > maxStepWidth)
                maxStepWidth = ((StepView) stepObj).prefWidth(0);

        }
        return maxStepWidth;
    }

    @Override
    public double computePrefWidth(double height) {
        return computeMinWidth(height);
    }

    @Override
    protected double computeMaxWidth(double height) {
        return computeMinWidth(height);
    }

    @Override
    protected double computeMinHeight(double width) {
        return prefHeight(getWidth());
    }          
    
    public StepView createStepView(StepModel step) {
    	
    	StepView stepView; 
    	
        switch (step.getStepType()) {
        
        case PROCEDURE: stepView = new ProcedureStepView((ProcedureStepModel)step, parentView, procedureView); break; 
        case ACTION: stepView = new ActionStepView((ActionStepModel)step, parentView, procedureView); break; 
        case IDIOM: stepView = new IdiomStepView((IdiomStepModel)step, parentView, procedureView); break; 
        case LOOP: stepView = new LoopView((LoopModel)step, parentView, procedureView); break; 
        case PLACEHOLDER: stepView = new PlaceholderStepView(step, parentView, procedureView); break; 
        
        //        
        case EXERCISE_STEP : stepView = ((ExerciseStepModel) step).isHidden() ? null : new ExerciseStepView((ExerciseStepModel) step, parentView, (ExerciseView) procedureView); break;
        case EXERCISE_GROUP : stepView = new ExerciseGroupOfStepsView((ExerciseGroupOfStepsModel) step, parentView, (ExerciseView) procedureView); break;
        case EXERCISE_SUBTASK : stepView = new ExerciseSubtaskView((ExerciseSubtaskModel) step, parentView, (ExerciseView) procedureView); break; 
         
        //
        
        default:
            throw new RuntimeException("Unsupported model type passed to getStepView()");   
        }                  
        
        return stepView;  
    }
       
	public ContainerStepModel getContainerStepModel() {
        return containerStepModel;
    }
    
    public int indexOf(StepView child) {
        return getChildren().indexOf(child);
    }
    
    public StepView findStepView(StepModel model) {
        for (Node node : getChildren()) {
            StepView sv = (StepView)node; 
            if (sv.getStepModel() == model) 
                return sv;
            else if (sv.getStepModel() instanceof ContainerStepModel) {
                StepView result = ((IStepViewContainer)sv).findStepView(model);
                if (result != null)
                    return result; 
            }
        }
        return null; 
    }
    
    // WARNING: This method should not be called directly except from the 
    // EditController or to facilitate drag placeholder logic
    public boolean addStepView(StepView view, int index) {
         if (getChildren().indexOf(view) < 0) {
             view.setView(procedureView);
             view.setStepViewContainer(parentView);           
             if (index >= getChildren().size())
                 getChildren().add(view);
             else
                 getChildren().add(index, view);
         }
         layout();
         return true; 
    }
    
    // WARNING: This method should not be called directly except from the 
    // EditController or to facilitate drag placeholder logic
    public boolean deleteStepView(StepView view) {
        return getChildren().remove(view);
    }
    
    public List<StepView> getStepViews() {
        List<StepView> subStepViews = new ArrayList<StepView>();
        for (Node node : getChildren()) 
            subStepViews.add((StepView)node);        
        return subStepViews;
    }
           
    public IStepViewContainer getStepViewContainer() {
        if (parentView != null)
            return parentView;
        else
            return procedureView; 
    }     
}
