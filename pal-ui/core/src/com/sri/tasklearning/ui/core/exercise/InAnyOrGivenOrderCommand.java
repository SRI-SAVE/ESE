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

import java.util.LinkedList;
import java.util.List;

import javafx.scene.layout.Pane;

import com.sri.tasklearning.ui.core.ISelectable;
import com.sri.tasklearning.ui.core.step.ExerciseGroupOfStepsModel;
import com.sri.tasklearning.ui.core.step.ExerciseSubtaskView;
import com.sri.tasklearning.ui.core.step.StepView;

public class InAnyOrGivenOrderCommand extends AbstractCommand {

	public InAnyOrGivenOrderCommand(ExerciseEditController controller, Pane owner) {
		super(controller, owner);		
	}

	@Override
	public void invokeCommand(Pane selectedItem) {

		if (((StepView) selectedItem).getStepModel() instanceof ExerciseGroupOfStepsModel) {
			ExerciseGroupOfStepsModel exStep = (ExerciseGroupOfStepsModel) ((StepView) selectedItem).getStepModel();
			exStep.setInAnyOrder( ! exStep.getInAnyOrder().getValue() );

		}		
	}

	@Override
	public void invokeCommand() {

		List<ExerciseSubtaskView> selectedSteps = new LinkedList<ExerciseSubtaskView>(); 

		for (ISelectable step : controller.getView().getSelectionManager().getSelectedItems()) {
			if (step instanceof ExerciseSubtaskView)
				selectedSteps.add((ExerciseSubtaskView) step);
		}

		if (selectedSteps.isEmpty()) {		
 
			controller.highlightSubtasks(); 				

		} else {

			controller.unhighlightSteps();

			for (ExerciseSubtaskView step : selectedSteps)				
				invokeCommand(step);	

			controller.getAnnotationPanel().unselectAll();
			controller.getAnnotationPanel().getExerciseViewSelectionManager().selectNone();
		}	
	}

}
