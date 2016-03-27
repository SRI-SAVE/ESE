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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;

import com.sri.pal.training.core.exercise.Atom;
import com.sri.pal.training.core.exercise.Step;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameter;

/**
 * Extension of ActionStepModel for exercise steps.
 */

public class ExerciseStepModel extends ActionStepModel {   
	
	private Atom step;	
	private SimpleBooleanProperty isOptional = new SimpleBooleanProperty(); 
	private SimpleBooleanProperty isHidden = new SimpleBooleanProperty(); 
	
	private List<ExerciseStepModel> predecessors = new LinkedList<ExerciseStepModel>(); 
	private List<ExerciseStepModel> successors = new LinkedList<ExerciseStepModel>(); 
	
	 public ExerciseStepModel(
	            final String functor,
	            final Collection<ExerciseStepParameter> arguments) {
		 
	    	super(functor, arguments);
	    	
	    	Atom atom = new Atom(functor); 
	    	atom.setFunctor(functor);	    	
	    	
	    	Step step1 =  new Step(); 
	    	step1.setAtom(atom);
	    	
	    	atom.setStep(step1);	    	
	        this.setStep(atom);     	        
	        this.stepType = StepType.EXERCISE_STEP;
	        
	        registerParameterOwner(arguments);
	        	    
	    }

	public ExerciseStepModel(Atom step, final Collection<ExerciseStepParameter> arguments) {

		super(step.getFunctor(), arguments);   
		 
		this.setStep(step); 
		this.stepType = StepType.EXERCISE_STEP;
	        
	    registerParameterOwner(arguments);
    
    }
	
	private void registerParameterOwner(Collection<ExerciseStepParameter> arguments) {
		
		for (ExerciseStepParameter arg : arguments) {
			arg.setOwner(this);
			registerParameterOwner(arg.getSubParameters()); 
		}
		
	}

    public void registerPredecessor(ExerciseStepModel pred) {
    	predecessors.add(pred);
    	pred.successors.add(this); 
    }
    
    public List<ExerciseStepModel> getPredecessors() {
    	return predecessors; 
    }
    
    public List<ExerciseStepModel> getSuccessors() {
    	return successors; 
    }

	public Atom getStep() {
		return step;
	}

	public void setStep(Atom step) {
		this.step = step;
	}

	public boolean isOptional() {
		return isOptional.getValue();
	}

	public void setOptional(boolean isOptional) {
		this.isOptional.setValue(isOptional);
	}

	public SimpleBooleanProperty getIsOptional() {
		return isOptional;
	}
	
	public boolean isHidden() {
		return isHidden.getValue();
	}

	public void setHidden(boolean isHidden) {
		this.isHidden.setValue(isHidden);
	}

	public SimpleBooleanProperty getIsHidden() {
		return isHidden;
	}

}
