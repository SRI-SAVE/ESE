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

package com.sri.tasklearning.ui.core.exercise;

import java.util.List;

import javafx.scene.layout.Pane;

import com.sri.tasklearning.ui.core.ISelectable;
import com.sri.tasklearning.ui.core.step.ExerciseGroupOfStepsModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;

public class ToggleStepOptionalCommand extends AbstractCommand {

	public ToggleStepOptionalCommand(ExerciseEditController controller, Pane owner) {		
		super(controller, owner);		
	}


	@Override
	public void invokeCommand(Pane selectedItem) {

		StepModel stepModel = ((StepModel) ((StepView) selectedItem).getStepModel());		

		if (stepModel instanceof ExerciseStepModel) {				
			ExerciseStepModel exStep = (ExerciseStepModel) stepModel; 
			exStep.setOptional(! exStep.isOptional()); 
		} else if (((StepView) selectedItem).getStepModel() instanceof ExerciseGroupOfStepsModel) {
			ExerciseGroupOfStepsModel exStep = (ExerciseGroupOfStepsModel) stepModel; 
			((ExerciseGroupOfStepsModel) exStep).setOptional(! exStep.isOptional());
		}

	}
	
	@Override
	public void invokeCommand() {

		List<ISelectable> selectedSteps = controller.getView().getSelectionManager().getSelectedItems();

		if (selectedSteps.isEmpty()) {		

			controller.highlightSteps();			

		} else {

			controller.unhighlightSteps();
			
			for (ISelectable step : selectedSteps)
				invokeCommand((Pane) step);

			controller.getAnnotationPanel().unselectAll();
			controller.getAnnotationPanel().getExerciseViewSelectionManager().selectNone();
		}	
	}


}
