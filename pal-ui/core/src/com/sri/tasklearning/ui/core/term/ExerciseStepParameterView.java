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

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sri.ai.lumen.atr.term.impl.CTRLiteral;
import com.sri.tasklearning.ui.core.EditSession;
import com.sri.tasklearning.ui.core.EditSessionManager;
import com.sri.tasklearning.ui.core.SelectionEventCallback;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.control.Alert.AlertResult;
import com.sri.tasklearning.ui.core.control.ExerciseParameterSplitMenuButton;
import com.sri.tasklearning.ui.core.control.ToolTipper;
import com.sri.tasklearning.ui.core.control.constant.IntegerRangeEditorMenuItem;
import com.sri.tasklearning.ui.core.exercise.ExerciseView;
import com.sri.tasklearning.ui.core.step.StepView;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;
import javafx.util.Pair;

/**
 * View for exercise step parameters 
 */
public class ExerciseStepParameterView extends TermView {

	public static final double DEFAULT_HEIGHT = 16.0;
	public static final boolean PSYCHEDELIC = false;

	private EventHandler<ActionEvent> enumValueItemHandler;
	private EventHandler<ActionEvent> anyTypeItemHandler;
	private EventHandler<ActionEvent> onActionHandler;

	private EventHandler<ActionEvent> okHandler;
	private EventHandler<ActionEvent> cancelHandler;
	
	private boolean commandInvokationDisabled = false;

	private static Image okIcon = Utilities.getImage("dialog-ok-2.png");
	private static Image cancelIcon = Utilities.getImage("dialog-cancel-2.png");
	
	private Button buttonOk;
	private Button buttonCancel;
	
	boolean singleTypeRadioToggle = false; 

	private final ExerciseParameterSplitMenuButton splitButton = new ExerciseParameterSplitMenuButton(this) {

		@Override
		public void show() {
			if (isGenerateMenuItems()) {
				
				List<MenuItem> items = buildTermOptionsMenu();
				
				if (items != null) {
					splitButton.getItems().clear();
					splitButton.getItems().addAll(items);
					
					EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {

						@Override
						public void handle(ActionEvent arg0) {
							boolean someSelected = false;  
						
							for (MenuItem item : items) {
								if (((CustomMenuItem) item).getContent() instanceof RadioButton) {						
									RadioButton rb = (RadioButton) ((CustomMenuItem) item).getContent();
									someSelected |= rb.isSelected();	
									/* 
									if (rb.isSelected() && singleTypeRadioToggle && 
											 ((ExerciseStepParameter) termModel).enumValueOrTypeIsSelected(rb.getText())) {
										someSelected = false; 
										break;
								   } */
								} else if (((CustomMenuItem) item).getContent() instanceof CheckBox) {
									CheckBox cb = (CheckBox) ((CustomMenuItem) item).getContent();
									someSelected |= cb.isSelected();
								}
							}
							
							buttonOk.disableProperty().setValue(! someSelected);
															
						}
					};
					
					for (MenuItem item : items) {						
						item.setOnAction(handler); 
					}
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

	
	private void changeParameter(ActionEvent e, boolean isType) {

		Object selected1 = e.getSource();
		
		String selected = ""; 

		if (selected1 instanceof CheckBox) {

			selected = ((CheckBox) selected1).getText();

			// deselect all radio buttons
			for (MenuItem item : splitButton.getItems()) {			
				if (item instanceof CustomMenuItem) {					
					if (((CustomMenuItem) item).getContent() instanceof RadioButton) {						
						RadioButton rb = (RadioButton) ((CustomMenuItem) item).getContent();
						rb.setSelected(false);						
					}						
				}
			}

		} else if (selected1 instanceof RadioButton ) {

			selected = ((RadioButton) selected1).getText();

			// deselect all check boxes
			for (MenuItem item : splitButton.getItems()) {			
				if (item instanceof CustomMenuItem) {					
					if (((CustomMenuItem) item).getContent() instanceof CheckBox) {						
						CheckBox cb = (CheckBox) ((CustomMenuItem) item).getContent();
						cb.setSelected(false);						
					}
				}				
			}			
		}
	}
		
	private void cancelChanges() {
		
		splitButton.hide(); 
		
	}


	private void confirmChanges() {
		
		EditSession sess = EditSessionManager.getActiveSession();    				
		sess.getController().unsavedChangesProperty().setValue(true);
	
		// check for range editor 
		for (MenuItem item : splitButton.getItems()) {		

			if (item instanceof IntegerRangeEditorMenuItem) {

				IntegerRangeEditorMenuItem range = (IntegerRangeEditorMenuItem) item;
				if ( range.getLabels() == null) {

					// type integer selected? 	
					for (MenuItem item2 : splitButton.getItems()) {		
						// check for radio
						if (((CustomMenuItem) item2).getContent() instanceof RadioButton) {	
							RadioButton rb = (RadioButton) ((CustomMenuItem) item2).getContent();
							String name = removeAny(rb.getText());
							boolean selected = ((ExerciseStepParameter) termModel).enumValueOrTypeIsSelected(name)^rb.isSelected();					
							if (selected) {
								((ExerciseStepParameter) termModel).setCurrentSelection(name, true);
								splitButton.setText(((ExerciseStepParameter) termModel).getButtonLabel());   	
								if (((ExerciseStepParameter) termModel).enumValueOrTypeIsSelected(name))
									// only return if type is now really selected (has not become unselected) 
									// we want to avoid parameter names to show up
									return; 
							}
						}
					}

					// else intervall specified
					int min = range.getMin();
					int max = range.getMax();
					
					Pair<Integer, Integer> selected =  new Pair<Integer, Integer>(min, max); 
					((ExerciseStepParameter) termModel).setCurrentSelection(selected);
					splitButton.setText(((ExerciseStepParameter) termModel).getButtonLabel());
					
					return; 			

				} else { 
					// sorted enum quantity range selected
					
					// quantity type selected? 
					for (MenuItem item2 : splitButton.getItems()) {		
						// check for radio
						if (((CustomMenuItem) item2).getContent() instanceof RadioButton) {	
							RadioButton rb = (RadioButton) ((CustomMenuItem) item2).getContent();
							String name = removeAny(rb.getText());
							boolean selected = ((ExerciseStepParameter) termModel).enumValueOrTypeIsSelected(name)^rb.isSelected();												
							if (selected) {
								((ExerciseStepParameter) termModel).setCurrentSelection(name, true);
								splitButton.setText(((ExerciseStepParameter) termModel).getButtonLabel());   	
								if (((ExerciseStepParameter) termModel).enumValueOrTypeIsSelected(name))
									// only return if type is now really selected (has not become unselected) 
									// we want to avoid parameter names to show up
									return; 
							}
						}
					}

					// else intervall specified
					int min1 = range.getMin();
					int max1 = range.getMax();
					String type = range.getType();
					
					Pair<String, String> selected =  new Pair<String, String>(range.getLabels().get(min1), range.getLabels().get(max1));
					((ExerciseStepParameter) termModel).setCurrentSelection(selected, type); 
					splitButton.setText(((ExerciseStepParameter) termModel).getButtonLabel());					
					return; 			

				}
			}
		}
		
		//
		// ordinary enums and types 
		//
	
		String oldLabel = ((ExerciseStepParameter) termModel).getButtonLabel();	
		final Set<ExerciseStepParameter> equals = ((ExerciseStepParameter) termModel).getEqualByValueParameters();

		for (MenuItem item : splitButton.getItems()) {			
			if (item instanceof CustomMenuItem) {					
				if (((CustomMenuItem) item).getContent() instanceof RadioButton) {	
					RadioButton rb = (RadioButton) ((CustomMenuItem) item).getContent();
					String name = removeAny(rb.getText());
					boolean selected = ((ExerciseStepParameter) termModel).enumValueOrTypeIsSelected(name)^rb.isSelected();					
					if (selected) {
						((ExerciseStepParameter) termModel).setCurrentSelection(name, true);							
					}
				} else if (((CustomMenuItem) item).getContent() instanceof CheckBox) {	
					CheckBox cb = (CheckBox) ((CustomMenuItem) item).getContent();
					String name = cb.getText();
					boolean selected = ((ExerciseStepParameter) termModel).enumValueOrTypeIsSelected(name)^cb.isSelected();					
					if (selected) {
						((ExerciseStepParameter) termModel).setCurrentSelection(name, false);							
					}
				}
			}
		} 
		
		//
		// check for change all?
		// 
		
		String newLabel = ((ExerciseStepParameter) termModel).getButtonLabel();		
		setButtonText(newLabel); 		
		((ExerciseStepParameter) termModel).getChangeFromOriginalProperty(); 
	
		if ( equals.size() > 0 ) {

			Alert.show("Question", "Do you want to change all instances of '" + oldLabel + "' or just this one?",   
					AlertConfig.ALL_THIS, new Callback<AlertResult, Void> () {

				@Override
				public Void call(AlertResult arg0) {

					if ( arg0.equals(AlertResult.ALL)) {								

						for (ExerciseStepParameter other : equals) {

							ExerciseStepParameterView otherView = ((ExerciseView) getOwner()).findExerciseTermView( other);
														
							for (MenuItem item : splitButton.getItems()) {			
								if (item instanceof CustomMenuItem) {					
									if (((CustomMenuItem) item).getContent() instanceof RadioButton) {	
										RadioButton rb = (RadioButton) ((CustomMenuItem) item).getContent();
										String name = removeAny(rb.getText());
										boolean selected = other.enumValueOrTypeIsSelected(name)^rb.isSelected();					
										if (selected) {
											other.setCurrentSelection(name, true);		
											otherView.setButtonText(newLabel);
											other.getChangeFromOriginalProperty();
										}
									} else if (((CustomMenuItem) item).getContent() instanceof CheckBox) {	
										CheckBox cb = (CheckBox) ((CustomMenuItem) item).getContent();
										String name = cb.getText();
										boolean selected = other.enumValueOrTypeIsSelected(name)^cb.isSelected();					
										if (selected) {
											other.setCurrentSelection(name, false);		
											otherView.setButtonText(newLabel);
											other.getChangeFromOriginalProperty();
										}
									}
								}
							} 											
						}
					}
					
					splitButton.hide();
					return null;
					
				}
			}); 		
		} 
	}
	

	public ExerciseStepParameterView(

			final ExerciseStepParameter argModel,
			final ParameterModel argParamModel,
			final StepView argStepView,
			final ExerciseView exerciseView) {

		super(argModel, argParamModel, argStepView, exerciseView);

		exerciseView.registerTermView(argModel, this);

		enumValueItemHandler = new EventHandler<ActionEvent>() {
			@Override 
			public void handle(ActionEvent e) {
				 changeParameter(e, false);
			}
		};

		anyTypeItemHandler = new EventHandler<ActionEvent>() {
			@Override 
			public void handle(ActionEvent e) {				
				changeParameter(e, true);												
			}
		};

		onActionHandler = new EventHandler<ActionEvent>() {
			@Override 
			public void handle(ActionEvent e) {
				splitButton.show();
			}
		}; 	
		
		okHandler = new EventHandler<ActionEvent>() {
			@Override 
			public void handle(ActionEvent e) {
				confirmChanges(); 
			}
		}; 	
		
		cancelHandler = new EventHandler<ActionEvent>() {
			@Override 
			public void handle(ActionEvent e) {
				cancelChanges(); 
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

	public static double getQuantityValue(String arg0) { 

		// unparse 1 3/4 -> 1.75 etc. 

		arg0 = arg0.trim();

		int pos1 = arg0.indexOf(' ');					
		double n1; 

		if (pos1 > -1) {

			// whole number plus fraction

			String whole  = arg0.substring(0,  pos1); 
			String rest = arg0.substring(pos1+1);

			pos1 = rest.indexOf("/"); 
			String numerator = rest.substring(0, pos1);
			String denominator = rest.substring(pos1+1);

			n1 = Double.parseDouble(whole) + (Double.parseDouble(numerator) / Double.parseDouble(denominator));

		} else {

			pos1 = arg0.indexOf("/");
			if (pos1 > -1 ) { // fraction ? 

				String numerator = arg0.substring(0, pos1);
				String denominator = arg0.substring(pos1+1);

				n1 = (Double.parseDouble(numerator) / Double.parseDouble(denominator));
			} else 

				n1 = Double.parseDouble(arg0); 

		}

		return n1;
	}
 
	@Override
	public List<MenuItem> buildTermOptionsMenu() {
 
		if (!generateMenuItems)
			return null;
		
		singleTypeRadioToggle = false; 		

		ExerciseStepParameter var = (ExerciseStepParameter) termModel; 
		List<MenuItem> items = new LinkedList<MenuItem>();			
		
		boolean foundNumeric = false;
		boolean foundEnumNumeric = false;
		String typeFound1 = ""; 

		Set<String> allTypes = new HashSet<String>(); 
		allTypes.addAll(var.getPossibleEnumTypes());
		allTypes.addAll(TypeUtilities.getEnumSuperTypes(var.getPossibleEnumValues()));

		for (String type : allTypes ) { 
			String type1 = TypeUtilities.getUnqualifiedTypeName(type);
			if ( ExerciseStepParameter.NUMERIC_RANGE_TYPES.contains(type1)) {
				typeFound1 = type; 
				foundNumeric = true; 
				foundEnumNumeric = ExerciseStepParameter.ENUM_RANGE_TYPES.contains(type1); 
				break;  
			}
		}

		final String typeFound = typeFound1; 
		
		List<String> sorted = null; 

		if (foundNumeric) {
			
			ToggleGroup toggle = new ToggleGroup(); 		

			for (String type: var.getPossibleEnumTypes(false)) {

				String type1 = TypeUtilities.getUnqualifiedTypeName(type);	

				RadioButton mi = new RadioButton(); 
				mi.setText("Any " + type1);
				mi.setToggleGroup(toggle);
				
				mi.setSelected(var.enumValueOrTypeIsSelected(type1));	
				
				if ( singleTypeRadioToggle ) 
					singleTypeRadioToggle = false; 
				else 
					singleTypeRadioToggle = true; 
				
				// mi.setOnAction(anyTypeItemHandler);							
				
				CustomMenuItem cmi = new CustomMenuItem(mi);
				cmi.setHideOnClick(false);				
				items.add(cmi);
				
			}	

			if (items.size() > 0) 
				items.add(new SeparatorMenuItem());

			SelectionEventCallback<Pair<Integer, Integer>> selectionEventCallback = new SelectionEventCallback<Pair<Integer, Integer>>() {

				@Override
				public void invoke(Pair<Integer, Integer> selectedItem) {							
					
					for (MenuItem item : splitButton.getItems()) {			
						if (item instanceof CustomMenuItem) {					
							if (((CustomMenuItem) item).getContent() instanceof RadioButton) {						
								RadioButton rb = (RadioButton) ((CustomMenuItem) item).getContent();
								rb.setSelected(false);						
							}	
							if (((CustomMenuItem) item).getContent() instanceof CheckBox) {						
								CheckBox cb = (CheckBox) ((CustomMenuItem) item).getContent();
								cb.setSelected(false);						
							}	
						}
					}
					
					// ((ExerciseStepParameter) termModel).setCurrentSelection(selectedItem);					
					// splitButton.setText(((ExerciseStepParameter) termModel).getButtonLabel());   	

				}};  

				List<CTRLiteral> curVals = var.getCurrentValueConstraintLiterals();
				CTRLiteral min = var.getCurrentValueConstraintMinLiteral();
				CTRLiteral max = var.getCurrentValueConstraintMaxLiteral();

				if ( foundEnumNumeric ) {

					sorted =  new LinkedList<String>(var.getPossibleEnumValues());
					
					sorted.sort(new Comparator<String>() {
						@Override
						public int compare(String arg0, String arg1) {											
							return Double.compare(getQuantityValue(arg0), getQuantityValue(arg1));  
						}
					}); 

					boolean added = false; 

					/* 
					for (String value: sorted) {

						CheckBox mi = new CheckBox(); 						
						mi.setText(value);						
						mi.setSelected(var.enumValueOrTypeIsSelected(value));				
						mi.setOnAction(enumValueItemHandler);
						added = true; 
						
						CustomMenuItem cmi = new CustomMenuItem(mi);
						cmi.setHideOnClick(false);				
						items.add(cmi);						
					}
					*/ 
					
					if (added) 
						items.add(new SeparatorMenuItem());

					selectionEventCallback = new SelectionEventCallback<Pair<Integer, Integer>>() {

						@Override
						public void invoke(Pair<Integer, Integer> selectedItem) {
							
							for (MenuItem item : splitButton.getItems()) {			
								if (item instanceof CustomMenuItem) {					
									if (((CustomMenuItem) item).getContent() instanceof RadioButton) {						
										RadioButton rb = (RadioButton) ((CustomMenuItem) item).getContent();
										rb.setSelected(false);						
									}	
									if (((CustomMenuItem) item).getContent() instanceof CheckBox) {						
										CheckBox cb = (CheckBox) ((CustomMenuItem) item).getContent();
										cb.setSelected(false);						
									}	
								}
							}
							
							// Pair<String, String> newSelected =  new Pair<String, String>(sorted.get(selectedItem.getKey()), sorted.get(selectedItem.getValue()));  
							// ((ExerciseStepParameter) termModel).setCurrentSelection(newSelected, typeFound);					
							// splitButton.setText(((ExerciseStepParameter) termModel).getButtonLabel());   	

						}};  

						if (curVals.size() == 1) { 

							int curVal = sorted.indexOf(curVals.get(0).getString()); 	
							int minVal = min != null ? sorted.indexOf(min.getString()) : curVal;
							int maxVal = max != null ? sorted.indexOf(max.getString()) : curVal;

							items.add(new IntegerRangeEditorMenuItem(minVal, maxVal, sorted, typeFound, selectionEventCallback));

						} else { 

							int curVal = 0;  				
							int minVal = min != null ? sorted.indexOf(min.getString()) : curVal;
							int maxVal = max != null ? sorted.indexOf(max.getString()) : curVal;

							items.add(new IntegerRangeEditorMenuItem(minVal, maxVal, sorted, typeFound, selectionEventCallback));

						}

				} else {

					boolean added = false; 
					
					for (String value: var.getPossibleEnumValues()) {

						// CheckMenuItem mi = new CheckMenuItem(value);
						// RadioMenuItem mi = new RadioMenuItem(value);
						
						CheckBox mi = new CheckBox(); 
						mi.setText(value);
						
						mi.setSelected(var.enumValueOrTypeIsSelected(value));		
						
						mi.setOnAction(enumValueItemHandler);

						added = true;
						
						
						CustomMenuItem cmi = new CustomMenuItem(mi);
						cmi.setHideOnClick(false);				
						items.add(cmi);			

					}

					if (added) 
						items.add(new SeparatorMenuItem());

					if (curVals.size() == 1) { 

						int curVal = Integer.parseInt(curVals.get(0).getString()); 				
						int minVal = min != null ? Integer.parseInt(min.getString()) : curVal;
						int maxVal = max != null ? Integer.parseInt(max.getString()) : curVal;

						items.add(new IntegerRangeEditorMenuItem(minVal, maxVal, null, null, selectionEventCallback));

					} else {

						int curVal = 1; 				
						int minVal = min != null ? Integer.parseInt(min.getString()) : curVal;
						int maxVal = max != null ? Integer.parseInt(max.getString()) : curVal;

						items.add(new IntegerRangeEditorMenuItem(minVal, maxVal, null, null, selectionEventCallback));

					}
				}

		} else {

			int itemAdded = 0; 

			for (String value: var.getPossibleEnumValues()) {

				CheckBox mi = new CheckBox(); 
				mi.setText(value);
				
				mi.setSelected(var.enumValueOrTypeIsSelected(value));				
				mi.setOnAction(enumValueItemHandler);
				
				CustomMenuItem cmi = new CustomMenuItem(mi);
				cmi.setHideOnClick(false);				
				items.add(cmi);
				itemAdded = 1;
				
			}

			ToggleGroup toggle = new ToggleGroup(); 			

			for (String type: var.getPossibleEnumTypes()) {

				String type1 = TypeUtilities.getUnqualifiedTypeName(type);

				RadioButton mi = new RadioButton(); 
				mi.setText("Any " + type1);
				mi.setToggleGroup(toggle);
								
				if ( singleTypeRadioToggle ) 
					singleTypeRadioToggle = false; 
				else 
					singleTypeRadioToggle = true; 
				
				mi.setSelected(var.enumValueOrTypeIsSelected(type1));				
				mi.setOnAction(anyTypeItemHandler);			

				if (itemAdded == 1) {
					items.add(new SeparatorMenuItem());
					itemAdded = 2;
				}
				
				CustomMenuItem cmi = new CustomMenuItem(mi);
				cmi.setHideOnClick(false);				
				items.add(cmi);
			}	

		}
		
		items.add(new SeparatorMenuItem()); 
		
		
		FlowPane buttons = new FlowPane();
		buttons.setAlignment(Pos.BASELINE_CENTER);
		buttons.setMinWidth(150);
		buttons.setPrefWidth(150);
		
		buttonOk = new Button("", new ImageView(okIcon));	
		buttonCancel = new Button("", new ImageView(cancelIcon));
		
		FlowPane.setMargin(buttonOk, new Insets(4)); 
		FlowPane.setMargin(buttonCancel, new Insets(4)); 
		
		buttons.getChildren().addAll(buttonOk, buttonCancel);
		
		buttonOk.setOnAction(okHandler);
		buttonCancel.setOnAction(cancelHandler);
		
		items.add(new CustomMenuItem(buttons));
				
		return items; 
	}
	
	public static String removeAny(String type) {
		return type.toString().substring(4); // cut off heading "Any "
	}
	
}
