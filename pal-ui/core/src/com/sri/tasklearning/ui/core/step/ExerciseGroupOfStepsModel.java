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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.resources.ResourceLoader;

/**
 * Model representing a set of exercise steps. 
 */
public class ExerciseGroupOfStepsModel extends ContainerStepModel  {
	
	public SimpleBooleanProperty isOptional = new SimpleBooleanProperty(); 
	public SimpleBooleanProperty inAnyOrder = new SimpleBooleanProperty(); 	
	
	private static final List<StepModel> emptySteps = new ArrayList<StepModel>(); 

	// private static final Logger log = LoggerFactory
    //    .getLogger(ExerciseGroupOfStepsModel.class);
   
    public ExerciseGroupOfStepsModel(StepModel parent, String descr, 
            final List<StepModel> theSteps) {
        
    	super(null);
    	setSteps(theSteps);
        setParent(parent); 
    	         
        stepType = StepType.EXERCISE_GROUP;         
        
        setName(descr);         
        setIconPath(ResourceLoader.getURL("loop-icon1.png"));     
                
    	InvalidationListener listener = new  InvalidationListener() {

			@Override
			public void invalidated(Observable arg0) {
				
				for (StepModel step : getSteps()) 
					((ExerciseStepModel) step).setOptional(((SimpleBooleanProperty) arg0).getValue());  			    		
				
			}
    		
    	}; 
    			
		isOptional.addListener(listener);
		
		// new default value for cooking domain: we no longer have ordered subgroups
		setInAnyOrder(true); 
    
    }
    
    public ExerciseGroupOfStepsModel(StepModel parent, String descr) {
        
    	this(parent, descr, new LinkedList<StepModel>()); 
    	
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
		
	public boolean inAnyOrder() {
			return inAnyOrder.getValue();
	}
	
    public SimpleBooleanProperty getInAnyOrder() {
		return inAnyOrder; 
	}

	public void setInAnyOrder(boolean inAnyOrder) {
		this.inAnyOrder.setValue(inAnyOrder);
	}
	

	//
	//
	// 

	public List<StepModel> getFixedSuccessors() {
		
		return emptySteps; 
	
	}    
		
	
	public List<StepModel> getEditableSuccessors() {
		
		return ((ExerciseModel) this.getParent()).getEditableSuccessors(this); 
		
	}	
	
}

