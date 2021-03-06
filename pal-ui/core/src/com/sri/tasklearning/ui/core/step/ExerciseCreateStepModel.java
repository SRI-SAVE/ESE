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
import java.util.List;

import com.sri.pal.training.core.exercise.Atom;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameter;

public class ExerciseCreateStepModel extends ExerciseStepModel {
	
	private String createdObject; 

	public ExerciseCreateStepModel(ExerciseModel parent, Atom step,
			Collection<ExerciseStepParameter> arguments) {
		
		super(parent, step, arguments);		
		
		if (arguments != null)
			createdObject = ((List<ExerciseStepParameter>) arguments).get(0).getParameter().getAccessor(); 
			
	}
	
	public String getCreatedObject() {

		return createdObject;
	}

}
 