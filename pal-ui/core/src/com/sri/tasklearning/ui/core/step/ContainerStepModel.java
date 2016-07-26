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
import java.util.LinkedList;
import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.tasklearning.ui.core.common.SignatureModel;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;

/**
 * Abstract model representing any step that contains other steps, such as a
 * {@link ProcedureModel} or {@link ExerciseModel} or {@link LoopModel} 
 */
public abstract class ContainerStepModel extends StepModel {
    private final static Logger log = LoggerFactory.getLogger(ContainerStepModel.class); 

    protected String explanationText = "This step contains other steps."; // Most containment steps will explain themselves, by overriding this value.
    private List<StepModel> steps = new ArrayList<StepModel>();
    private SimpleBooleanProperty hasSteps = new SimpleBooleanProperty(false);

    public ContainerStepModel(String functor) {
        super(functor);
    }
    
    public List<StepModel> getSteps() {
        return steps;
    }
    
    
    public StepModel getStepNo(int n) {
        return steps.get(n); 
    }
    
    
    public void setSteps(List<StepModel> argSteps) {
    	
    	for (StepModel step : argSteps) 
    		step.setContainer(this);    	
    	
        steps.clear();
        steps.addAll(argSteps);
        updateHasSteps();
    }
    
  public void clearSteps() {
    	
        steps.clear();
        updateHasSteps();
    }
    
    private void updateHasSteps() {
        hasSteps.setValue(steps != null && steps.size() > 0);
    }
    
    public SimpleBooleanProperty hasStepsProperty() {
        return hasSteps; 
    }

    @Override
    public String toString() {
        return "Container Step: " + getName() + "(contains "
                + getSteps().size() + " steps) [" + getInputs() + " "
                + getResults() + "]";
    }

    public boolean addStep(StepModel newStep, int position) {
    	   	    	
        if (!steps.contains(newStep)) {
            
        	steps.add(position, newStep);
            
        	newStep.setContainer(this);
                    	
            updateHasSteps();
            return true;
        } 
        else {
            log.error("Error: Tried to add duplicate step: {}", newStep);
            return false;
        }
    }

    public boolean deleteStep(StepModel theStep) {
        int index = steps.indexOf(theStep);
        if (index >= 0 && index < steps.size()) {
            steps.remove(index);
            updateHasSteps();
            return true;
        } else 
            return false;
    }

    public boolean moveStep(StepModel theStep, int newPosition) {
        int oldPosition = steps.indexOf(theStep);
        if (oldPosition >= 0) {
            steps.remove(oldPosition);
            if (newPosition == steps.size())
                steps.add(theStep);
            else
                steps.add(newPosition, theStep);
            return true;
        } 
        else {
            log.error("Error: Can't move a step between parents: {}", theStep);
            return false;
        }
    }

    public boolean contains (StepModel theStep, boolean searchRecursively) {
        boolean found = false;
        for (StepModel s : steps) {
            if (s.equals(theStep)) {
                found = true;
                break;
            } 
            else if (searchRecursively && s instanceof ContainerStepModel) {
                found = ((ContainerStepModel)s).contains(theStep, searchRecursively);
                if (found) 
                    break;
            }
        } 
        return found;
    }
    
    public boolean referencesFunctor(String functor) {
        for (StepModel s : getSteps()) {
            if (functor.equals(s.getFunctor()))
                return true;
            if (s instanceof ContainerStepModel)
                if (((ContainerStepModel)s).referencesFunctor(functor))
                    return true;
        }
        return false; 
    }
    
    public void updateFunctorReferences(
            final String oldFunctor,
            final String newFunctor) {
        for (StepModel s : getSteps()) {
           
            if (s instanceof ContainerStepModel)
                ((ContainerStepModel)s).updateFunctorReferences(oldFunctor, newFunctor);
        }
    }

    public ContainerStepModel findParent(StepModel theStep) {
        return findParent(theStep, this);
    }
    
    public ContainerStepModel findParent(StepModel theStep, ContainerStepModel parent) {
        ContainerStepModel found = null;
        for (StepModel s : parent.steps) {
            if (s.equals(theStep)) {
                found = parent;
                break;
            } 
            else if (s instanceof ContainerStepModel) {
                found = ((ContainerStepModel)s).findParent(theStep, ((ContainerStepModel)s));
                if (found != null) 
                    break;
            }
        }
        return found;
    }

    public int indexOf (StepModel theStep) {
        return steps.indexOf(theStep);
    }

    public int getStepCount() {
        return steps.size();
    }

	public SignatureModel getSignature() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ActionStepModel> getFlattenedSteps() {
		
		List<ActionStepModel> res = new LinkedList<ActionStepModel>();
		
		for (StepModel step : steps) {			
			if (step instanceof ActionStepModel) 
				res.add((ActionStepModel) step); 
			else if (step instanceof ContainerStepModel)
				res.addAll(((ContainerStepModel) step).getFlattenedSteps());
		}
		
		return res; 
		
	}

}
