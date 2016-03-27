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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javafx.scene.layout.Pane;

import com.sri.tasklearning.ui.core.ISelectable;
import com.sri.tasklearning.ui.core.step.ExerciseGroupOfStepsModel;
import com.sri.tasklearning.ui.core.step.ExerciseGroupOfStepsView;
import com.sri.tasklearning.ui.core.step.ExerciseStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepView;
import com.sri.tasklearning.ui.core.step.ExerciseSubtaskModel;
import com.sri.tasklearning.ui.core.step.ExerciseSubtaskView;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;

public class GroupUngroupSequenceCommand extends AbstractCommand {

	protected static HashMap<ExerciseModel, List<StepModel>> theSteps = new HashMap<ExerciseModel, List<StepModel>>(); 
	protected static HashMap<ExerciseModel, ExerciseSubtaskModel> constructedGroup = new HashMap<ExerciseModel, ExerciseSubtaskModel>(); 
	protected static HashMap<ExerciseModel, ExerciseSubtaskView> constructedGroupView = new HashMap<ExerciseModel, ExerciseSubtaskView>(); 

	protected static ExerciseModel lastModel = null; 

	public GroupUngroupSequenceCommand(ExerciseEditController controller, Pane owner) {		
		super(controller, owner);		
		lastModel = controller.getModel();			
	}

	public void resetGroupingState(ExerciseEditController controller) {

		theSteps.put(controller.getModel(), new LinkedList<StepModel>());

	}

	public void resetGroupingState() {

		if (lastModel != null)
			theSteps.put(lastModel, new LinkedList<StepModel>());

	}


	@Override
	public void invokeCommand(Pane selectedItem) {

		StepModel exStep = (StepModel) ((StepView) selectedItem).getStepModel();
		ExerciseView exerciseView = controller.getView();
		ExerciseModel exerciseModel = controller.getModel();

		lastModel = exerciseModel; 

		if (theSteps.get(controller.getModel()) == null) {
			theSteps.put(controller.getModel(), new LinkedList<StepModel>());

		}

		if (exStep instanceof ExerciseStepModel) {

			ExerciseStepModel exStep1 = (ExerciseStepModel) exStep;

			if (exStep1.getContainer().getValue() instanceof ExerciseModel) {

				// we have selected an exercise step which is not in a group 

				int index = 0; 
				String name = ""; 		

				if (theSteps.get(controller.getModel()).size() == 0) {

					// are we building up a group? add this step...

					index = exStep1.computeIndexRelativeToContainer();
					controller.deleteStep(exStep1);		

					name = computeGroupName(); 

				} else {

					// this is the first step in a new group we are constructing 

					controller.deleteStep(exStep1);				
					index = constructedGroup.get(exerciseModel).computeIndexRelativeToContainer(); 		
					controller.deleteStep(constructedGroup.get(exerciseModel));
					name = constructedGroup.get(exerciseModel).getName();

				}; 

				theSteps.get(exerciseModel).add(exStep1);		

				ExerciseSubtaskModel newGroup = new ExerciseSubtaskModel("", theSteps.get(exerciseModel)); 
				newGroup.setName(name);

				constructedGroup.put(exerciseModel, newGroup);

				exerciseModel.addStep(constructedGroup.get(exerciseModel), index);																

				constructedGroupView.put(exerciseModel, new ExerciseSubtaskView(constructedGroup.get(exerciseModel),  exerciseView, exerciseView));

				exerciseView.getStepLayout().addStepView(constructedGroupView.get(exerciseModel), index);

				exerciseModel.updateIndices();

			} else {

				// we have selected a step which is already part of a group / subtask

				resetGroupingState(controller); 

				ExerciseSubtaskModel container = (ExerciseSubtaskModel) exStep1.getContainer().getValue(); 					

				container.deleteStep(exStep1);

				int index = container.computeIndexRelativeToContainer(); 

				exerciseModel.addStep(exStep1, index);							

				//
				// we remove the step from the subtask. if the subtasks gets empty, we also delete this 
				// 

				ExerciseSubtaskView constructedGroupView = (ExerciseSubtaskView) exerciseView.getStepLayout().findStepView(container);
				ExerciseStepView exStepView = (ExerciseStepView) constructedGroupView.findStepView(exStep1);   

				exerciseView.getStepLayout().deleteStepView(constructedGroupView);
				exerciseView.getStepLayout().addStepView(exStepView, index); 

				if (! container.getSteps().isEmpty()) {												
					constructedGroupView = new  ExerciseSubtaskView(container,  exerciseView, exerciseView);
					exerciseView.getStepLayout().addStepView(constructedGroupView, index+1);																	
				} else
					exerciseModel.deleteStep(container);

				exerciseModel.updateIndices();

			}

		} else if (exStep instanceof ExerciseSubtaskModel) {

			// we have selected a group / subtask step
			// that means disolve it 			

			resetGroupingState(controller); 

			ExerciseSubtaskModel exStep1 = (ExerciseSubtaskModel) exStep; 

			int index = exStep1.computeIndexRelativeToContainer(); 
			int oindex = index; 
			controller.deleteStep(exStep1);		

			for (StepModel step : ((ExerciseSubtaskModel) exStep).getSteps()) {
				ExerciseStepModel step1 = (ExerciseStepModel) step; 					
				exerciseModel.addStep(step1, index++);					
			}							

			((ExerciseSubtaskView) selectedItem).getStepIndexVisibility().unbind();
			((ExerciseSubtaskView) selectedItem).getStepIndexVisibility().setValue(true);

			for (StepView view : ((ExerciseSubtaskView) selectedItem).getStepViews()) {	
				ExerciseStepView view1 = (ExerciseStepView) view; 				
				exerciseView.getStepLayout().addStepView(view1, oindex++); 			
				view1.setStepViewContainer(exerciseView);
			}

			exerciseModel.updateIndices();		

		} 
		
		
		controller.unhighlightSteps();
		
		if ((controller.getAnnotationPanel().getSelectedItem() != null) && (controller.getAnnotationPanel().getSelectedItem().command == this))
			controller.highlightSteps();		

	}

	private String computeGroupName() { 

		String name = ""; 					

		boolean found = true;
		int groupCounter = 1; 
		while (found) {
			found = false; 
			for (StepModel step : controller.getModel().getSteps()) {
				name = "Group "+Integer.toString(groupCounter); 
				found = (step instanceof ExerciseSubtaskModel) && (step.getName().equals(name));  
				if (found)
					break;
			}
			groupCounter++; 
		}

		return name; 

	}

	@Override
	public void invokeCommand() {

		resetGroupingState();

		List<ISelectable> selectedSteps = controller.getView().getSelectionManager().getSelectedItems();

		if (selectedSteps.isEmpty()) {		

			controller.highlightSteps();

		} else {

			ExerciseView exerciseView = controller.getView();
			ExerciseModel exerciseModel = controller.getModel();			

			lastModel = exerciseModel; 

			if (theSteps.get(controller.getModel()) == null) {
				theSteps.put(controller.getModel(), new LinkedList<StepModel>());

			}

			//
			// analyze selection 
			// 

			List<ExerciseStepView> selectedToplevelSteps = new LinkedList<ExerciseStepView>(); 
			List<ExerciseStepView> selectedSubtaskSteps = new LinkedList<ExerciseStepView>();
			List<ExerciseGroupOfStepsView> selectedSubtasks = new LinkedList<ExerciseGroupOfStepsView>();

			for (ISelectable selectedStep : selectedSteps) { 
				StepView stepView = (StepView) selectedStep;
				if (stepView instanceof ExerciseGroupOfStepsView) {
					selectedSubtasks.add((ExerciseGroupOfStepsView) stepView);						
				} else {
					if (stepView.getStepViewContainer() instanceof ExerciseGroupOfStepsView)   
						selectedSubtaskSteps.add((ExerciseStepView) stepView);
					else
						selectedToplevelSteps.add((ExerciseStepView) stepView);				
				}
			}			

			//
			// no selected subtask steps, and a couple of subtasks and toplevel steps -> combine them into a new subtask, delete old ones  
			// 

			if (selectedSubtaskSteps.isEmpty() && ! ( selectedSubtasks.isEmpty() && selectedToplevelSteps.isEmpty())) {

				List<ExerciseStepView> stepsForGroup = new LinkedList<ExerciseStepView>();

				for (ExerciseGroupOfStepsView group : selectedSubtasks) {
					for (StepView step : group.getStepViews()) {					
						stepsForGroup.add((ExerciseStepView) step); 					
					}
				}

				for (StepView step : selectedToplevelSteps) {					
					stepsForGroup.add((ExerciseStepView) step); 					
				}

				//
				//
				// 

				Comparator<ExerciseStepView> comp = new Comparator<ExerciseStepView>() {

					@Override
					public int compare(ExerciseStepView arg0,
							ExerciseStepView arg1) {

						int i0 = arg0.getStepModel().getIndex(); 
						int i1 = arg1.getStepModel().getIndex();
						return i0 < i1 ? -1 : ( i0 == i1 ? 0 : 1 );  
					}
				}; 

				Comparator<ExerciseStepView> comp1 = new Comparator<ExerciseStepView>() {

					@Override
					public int compare(ExerciseStepView arg0,
							ExerciseStepView arg1) {

						int i0 = arg0.getStepModel().getRelativeIndex(); 
						int i1 = arg1.getStepModel().getRelativeIndex();
						return i0 < i1 ? -1 : ( i0 == i1 ? 0 : 1 );  
					}
				}; 

				Comparator<ExerciseGroupOfStepsView> comp2 = new Comparator<ExerciseGroupOfStepsView>() {

					@Override
					public int compare(ExerciseGroupOfStepsView arg0,
							ExerciseGroupOfStepsView arg1) {

						int i0 = arg0.getStepModel().getRelativeIndex(); 
						int i1 = arg1.getStepModel().getRelativeIndex();
						return i0 < i1 ? -1 : ( i0 == i1 ? 0 : 1 );  
					}
				}; 


				//
				//
				// 

				Collections.sort(stepsForGroup, comp);	

				Collections.sort(selectedToplevelSteps, comp1);	
				Collections.sort(selectedSubtasks, comp2);	

				//
				//
				// 

				ExerciseStepView firstToplevelStep = selectedToplevelSteps.isEmpty() ? null : selectedToplevelSteps.get(0);
				ExerciseGroupOfStepsView firstSubtask = selectedSubtasks.isEmpty() ? null : selectedSubtasks.get(0);

				int index1 = firstSubtask == null ? -1 : firstSubtask.getStepModel().getRelativeIndex(); 			
				int index2 = firstToplevelStep == null ? -1 : firstToplevelStep.getStepModel().getRelativeIndex(); 

				int index = index1 == -1 ? index2 : (index2 == -1 ? index1 : (index1 < index2 ? index1 : index2));  

				for (ExerciseGroupOfStepsView stepView : selectedSubtasks)
					controller.deleteStep(stepView);

				for (StepView stepView : selectedToplevelSteps)
					controller.deleteStep(stepView);	

				List<StepModel> stepModelsForGroup = new LinkedList<StepModel>();

				for (StepView stepView : stepsForGroup)
					stepModelsForGroup.add((ExerciseStepModel) stepView.getStepModel());				

				String name = "";
				boolean isOptional = false; 
				boolean inAnyOrder = false; 

				if ( index1 > -1 ) {

					name = firstSubtask.getStepModel().getName();
					isOptional = ((ExerciseGroupOfStepsModel) firstSubtask.getStepModel()).isOptional();
					inAnyOrder = ((ExerciseGroupOfStepsModel) firstSubtask.getStepModel()).inAnyOrder(); 

				} else

					name = computeGroupName(); 				

				//
				// create
				// 

				ExerciseSubtaskModel newGroup = new ExerciseSubtaskModel("", stepModelsForGroup); 

				newGroup.setName(name);
				newGroup.setOptional(isOptional);
				newGroup.setInAnyOrder(inAnyOrder);

				//
				// register context
				// 

				theSteps.put(exerciseModel, stepModelsForGroup); 

				constructedGroup.put(exerciseModel, newGroup);			

				exerciseModel.addStep(constructedGroup.get(exerciseModel), index);																

				constructedGroupView.put(exerciseModel, new ExerciseSubtaskView(constructedGroup.get(exerciseModel),  exerciseView, exerciseView));

				exerciseView.getStepLayout().addStepView(constructedGroupView.get(exerciseModel), index);

				exerciseModel.updateIndices();
				controller.getView().getSelectionManager().selectNone();

			} 		

			controller.unhighlightSteps();
			controller.getAnnotationPanel().unselectAll();

		}

	}

}
