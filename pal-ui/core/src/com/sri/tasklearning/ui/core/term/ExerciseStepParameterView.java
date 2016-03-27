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

package com.sri.tasklearning.ui.core.term;

import java.util.LinkedList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.ExerciseParameterSplitMenuButton;
import com.sri.tasklearning.ui.core.control.ToolTipper;
import com.sri.tasklearning.ui.core.step.StepView;

/**
 * View for exercise step parameters 
 */
public class ExerciseStepParameterView extends TermView {

	public static final double DEFAULT_HEIGHT = 16.0;
	public static final boolean PSYCHEDELIC = false;

	private EventHandler<ActionEvent> menuItemHandler;
	private EventHandler<ActionEvent> origItemHandler;
	private EventHandler<ActionEvent> anyTypeItemHandler;
	private EventHandler<ActionEvent> onActionHandler;    

	private boolean commandInvokationDisabled = false;

	private final ExerciseParameterSplitMenuButton splitButton = new ExerciseParameterSplitMenuButton(this) {

		@Override
		public void show() {
			if (isGenerateMenuItems()) {
				List<MenuItem> items = buildTermOptionsMenu();
				if (items != null) {
					splitButton.getItems().clear();
					splitButton.getItems().addAll(buildTermOptionsMenu());
				}
			}
			ToolTipper.setEnabled(false); 
			super.show();
		}

		@Override
		public void hide() {
			ToolTipper.setEnabled(true);
			super.hide();
		}

	};

	public ExerciseStepParameterView(
			final TermModel argModel,
			final ParameterModel argParamModel,
			final StepView argStepView,
			final CommonView argProcView) {

		super(argModel, argParamModel, argStepView, argProcView);

		menuItemHandler = new EventHandler<ActionEvent>() {
			@Override 
			public void handle(ActionEvent e) {
				String selected = ((MenuItem) e.getSource()).getText();
				// here we have selected an enum value 
				((ExerciseStepParameter) termModel).setCurrentSelection(selected);				
				splitButton.setText(((ExerciseStepParameter) termModel).getButtonLabel());   				
			}
		};

		anyTypeItemHandler = new EventHandler<ActionEvent>() {
			@Override 
			public void handle(ActionEvent e) {				
				// here we have selected the "Any <Class>" -> null 
				((ExerciseStepParameter) termModel).setCurrentSelection(null);		
				// for side effect:
				((ExerciseStepParameter) termModel).getChangeFromOriginalProperty(); 
				splitButton.setText(((MenuItem) e.getSource()).getText());   
			}
		};

		origItemHandler = new EventHandler<ActionEvent>() {
			@Override 
			public void handle(ActionEvent e) {
				String selected = ((MenuItem) e.getSource()).getText();
				// here we have selected the original equalTo value - restablish original equality constraint 
				((ExerciseStepParameter) termModel).setCurrentSelection(termModel);				
				splitButton.setText(((ExerciseStepParameter) termModel).getButtonLabel());   				
			}
		};
		
		onActionHandler = new EventHandler<ActionEvent>() {
			@Override 
			public void handle(ActionEvent e) {
				splitButton.show();  
			}
		}; 	

		configureButton(); 

	}

	public ExerciseParameterSplitMenuButton getNode() {
		return splitButton; 
	} 

	public void setSpecialMenuItems(List<MenuItem> items) {
		splitButton.getItems().clear();
		splitButton.getItems().addAll(items); 
	}        

	protected void configureButton() {

		splitButton.getStyleClass().clear();
		splitButton.getStyleClass().add("invisible");

		splitButton.setText(((ExerciseStepParameter) termModel).getButtonLabel());   	

		if (splitButton.getText().length() < 1) {
			setButtonText(termModel.getDisplayString());

			termModel.displayStringProperty().addListener(
					new ChangeListener<String>() {
						public void changed(
								ObservableValue<? extends String> val,
								String oldVal, String newVal) {
							setButtonText("X "+termModel.getDisplayString());
						}
					});
		}        

		termModel.highlightedProperty().addListener(
				new ChangeListener<Boolean>() {
					public void changed(
							ObservableValue<? extends Boolean> value,
							Boolean old, Boolean newVal) {

						if (newVal.booleanValue()) {
							splitButton.highlightBorder();
						}
						else { 
							splitButton.unhighlightBorder();
						}
					}
				});

		splitButton.setOnAction(onActionHandler);

	}

	private void setButtonText(String displayText) {     

		splitButton.setStyle("-fx-text-fill: black;");
		splitButton.getNode().setStyle("-fx-text-fill: black;");

		if (displayText != null && displayText.length() > VariableModel.MAX_NAME_LENGTH) {
			String str = displayText.substring(0, VariableModel.MAX_NAME_LENGTH)
					+ "\u2026";            
			splitButton.setText(str);                      
		} else {        	
			splitButton.setText(displayText);
		}
	}


	public boolean isCommandInvokationDisabled() {
		return commandInvokationDisabled;
	}

	public void setCommandInvokationDisabled(boolean commandInvokationDisabled) {
		this.commandInvokationDisabled = commandInvokationDisabled;
	}


	@Override
	public List<MenuItem> buildTermOptionsMenu() {

		if (!generateMenuItems)
			return null;

		ExerciseStepParameter var = (ExerciseStepParameter) termModel; 

		List<MenuItem> items = new LinkedList<MenuItem>();		
		
		ExerciseStepParameter origParam = var.getOriginalParameter();
		
		boolean separatorAdded = true;
		
		if (origParam.getEqualityConstraint() != null) {		
			MenuItem orig = new MenuItem(var.getOriginalParameter().getParameterDescription());   
			orig.setOnAction(origItemHandler);
			items.add(orig);
			separatorAdded = false;
		}				
		
		for (final String value: var.getPossibleValues()) {
			CheckMenuItem mi = new CheckMenuItem(value);
			mi.setSelected(var.valueIsSelected(value));
			if (!separatorAdded) {
				items.add(new SeparatorMenuItem());
				separatorAdded = true; 
			}			
			items.add(mi);
			mi.setOnAction(menuItemHandler);
		}
		
		items.add(new SeparatorMenuItem());	

		MenuItem any = new MenuItem("Any "+ TypeUtilities.getUnqualifiedTypeName(var.getTypeDef().getName())); 

		any.setOnAction(anyTypeItemHandler);

		items.add(any);    

		return items;

	}

}
