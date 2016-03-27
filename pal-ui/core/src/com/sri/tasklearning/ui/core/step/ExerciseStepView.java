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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;

import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.control.ToolTippedImageView;
import com.sri.tasklearning.ui.core.exercise.ExerciseEditController;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.exercise.ExerciseView;
import com.sri.tasklearning.ui.core.exercise.GroupUngroupSequenceCommand;
import com.sri.tasklearning.ui.core.exercise.NewGroupSequenceCommand;
import com.sri.tasklearning.ui.core.exercise.ToggleStepOptionalCommand;
import com.sri.tasklearning.ui.core.layout.TextFlowLayout;

/**
 * View for {@link ExerciseStepModel}.  
 */

public class ExerciseStepView extends ActionStepView {

	protected static Image optionalIcon; 
	protected static Image requiredIcon;
	
	protected static Image groupIcon; 
	protected static Image ungroupIcon;
	
	protected static Image newGroupIcon; 
	
	protected ToggleStepOptionalCommand optionalCommand; 
	protected GroupUngroupSequenceCommand groupCommand;
	protected NewGroupSequenceCommand newGroupCommand;
	
	protected SimpleObjectProperty<Image> currentOptionalCommandIcon; 
	
	protected SimpleObjectProperty<Image> currentGroupCommandIcon; 
	
	protected SimpleObjectProperty<Image> currentNewGroupCommandIcon; 
	
	public ExerciseStepView(

			final ExerciseStepModel argModel, 
			final IStepViewContainer argParent, 
			final ExerciseView argProcView) {

		super(argModel, argParent, argProcView); 
		
	}

	protected Pane createTitlePane() {
		
		final ExerciseStepView owner = this; 
		
		boolean inGroup = ! (((ExerciseStepModel) stepModel).getContainer().getValue() instanceof ExerciseModel); 
		
		//
		// configure commands
		//
		
		optionalCommand = new ToggleStepOptionalCommand((ExerciseEditController) procedureView.getController(), this);
		groupCommand    = new GroupUngroupSequenceCommand((ExerciseEditController) procedureView.getController(), this);				
		newGroupCommand = new NewGroupSequenceCommand((ExerciseEditController) procedureView.getController(), this);				
		 
		optionalIcon = Utilities.getImage("letter-O-icon.png");
		requiredIcon   = Utilities.getImage("letter-R-icon.png");
		currentOptionalCommandIcon = new SimpleObjectProperty<Image>(((ExerciseStepModel) owner.stepModel).isOptional() ? optionalIcon : requiredIcon); 	
		
		groupIcon = Utilities.getImage("letter-G-icon.png");
		ungroupIcon   = Utilities.getImage("letter-U-icon.png");
		currentGroupCommandIcon = new SimpleObjectProperty<Image>(inGroup ? groupIcon : ungroupIcon);
		
		newGroupIcon = Utilities.getImage("letter-N-icon.png");
		currentNewGroupCommandIcon = new SimpleObjectProperty<Image>(newGroupIcon);
		
		//
		//
		//
		
		final ToolTippedImageView optionalToggleButton  = new ToolTippedImageView(requiredIcon);
		final ToolTippedImageView groupToggleButton  = new ToolTippedImageView(groupIcon);
		final ToolTippedImageView newGroupToggleButton  = new ToolTippedImageView(newGroupIcon);
		
		//
		//
		// 

		headerText = new TextFlowLayout(this);
		headerText.setPadding(new Insets(1.5 * PAD, 0, 1.5 * PAD, 0));

		final Line bottomBorder = new Line();		
			
		titlePane = new Pane() {
			@Override
			protected void layoutChildren() {
				super.layoutChildren(); // Resizes children to preferred sizes

				headerText.setLayoutX(TITLE_PANE_LHS);

				optionalToggleButton.setLayoutX(getWidth() - 2 * PAD - TITLE_PANE_BUTTON_SIZE);
				optionalToggleButton.setLayoutY(getHeight() / 2 - (TITLE_PANE_BUTTON_SIZE / 2));
				
				groupToggleButton.setLayoutX(getWidth() - 2 * PAD - 2 * TITLE_PANE_BUTTON_SIZE);
				groupToggleButton.setLayoutY(getHeight() / 2 - (TITLE_PANE_BUTTON_SIZE / 2));
				
				newGroupToggleButton.setLayoutX(getWidth() - 2 * PAD - 3 * TITLE_PANE_BUTTON_SIZE);
				newGroupToggleButton.setLayoutY(getHeight() / 2 - (TITLE_PANE_BUTTON_SIZE / 2));

			}

			@Override
			public double computePrefHeight(double width) {
				return headerText.prefHeight(-1);
			}
		};
	
		double right = 3 * PAD + 2 * TITLE_PANE_BUTTON_SIZE;

		headerText.prefWrapLengthProperty().bind(
				widthProperty().subtract(2 * DEFAULT_BORDER_WIDTH + TITLE_PANE_LHS + right));
		titlePane.prefWidthProperty().bind(widthProperty().subtract(2 * DEFAULT_BORDER_WIDTH));
		titlePane.setMinWidth(Region.USE_PREF_SIZE);
		titlePane.setMaxWidth(Region.USE_PREF_SIZE);
		titlePane.setMinHeight(Region.USE_PREF_SIZE);
		titlePane.setMaxHeight(Region.USE_PREF_SIZE);

		bottomBorder.setStroke(borderColor.getValue());
		bottomBorder.setStrokeWidth(DEFAULT_BORDER_WIDTH);

		bottomBorder.setStartX(0);
		bottomBorder.endXProperty().bind(titlePane.widthProperty());
		bottomBorder.startYProperty().bind(titlePane.heightProperty().subtract(DEFAULT_BORDER_WIDTH));
		bottomBorder.endYProperty().bind(titlePane.heightProperty().subtract(DEFAULT_BORDER_WIDTH));
		
		//
		// optional toggle button
		// 
			   
        optionalToggleButton.mouseTransparentProperty().bind(disabledProperty());        
        optionalToggleButton.imageProperty().bind(currentOptionalCommandIcon);
        optionalToggleButton.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                e.consume();
            }
        });
        
        optionalToggleButton.setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
            	e.consume();
                	if (optionalToggleButton.contains(e.getX(), e.getY())) {        
                		optionalCommand.invokeCommand(owner);          	      	
                	}
            	}
        	}); 
        
       ((ExerciseStepModel) owner.stepModel).getIsOptional().addListener(new InvalidationListener() {
			public void invalidated(Observable value) {			
												
				ExerciseStepModel model =  (ExerciseStepModel) owner.getStepModel(); 				
            	if (model.isOptional()) {
                	currentOptionalCommandIcon.setValue(optionalIcon);
                } else {                       
                	currentOptionalCommandIcon.setValue(requiredIcon);
                }
                recalcHeight();   
			}
		}); 
		
        //
        //
        //
            
        groupToggleButton.mouseTransparentProperty().bind(disabledProperty());        
        groupToggleButton.imageProperty().bind(currentGroupCommandIcon);
        groupToggleButton.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                e.consume();
            }
        });
        
        groupToggleButton.setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                e.consume();
                if (groupToggleButton.contains(e.getX(), e.getY())) {        
                	groupCommand.invokeCommand(owner);          	      	
                }
            }
        }); 

        ((ExerciseStepModel) owner.stepModel).getContainer().addListener(new InvalidationListener() {
			public void invalidated(Observable value) {			
												
				ExerciseStepModel model =  (ExerciseStepModel) owner.getStepModel(); 				
            	if (model.getContainer().getValue() instanceof ExerciseModel ) {
            		currentGroupCommandIcon.setValue(ungroupIcon);
                } else {                       
                	currentGroupCommandIcon.setValue(groupIcon);
                }
                recalcHeight();   
			}
		}); 
		
        //
        //
        //
           
        newGroupToggleButton.mouseTransparentProperty().bind(disabledProperty());        
        newGroupToggleButton.imageProperty().bind(currentNewGroupCommandIcon);
        newGroupToggleButton.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                e.consume();
            }
        });
        
        newGroupToggleButton.setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                e.consume();
                if (newGroupToggleButton.contains(e.getX(), e.getY())) {        
                	newGroupCommand.invokeCommand(owner);          	      	
                }
            }
        }); 

        //
        //
        //
   
        titlePane.getChildren().addAll(headerText, bottomBorder, // newGroupToggleButton, 
        		groupToggleButton, optionalToggleButton);
        
		if (((ExerciseStepModel) stepModel).isOptional()) {
			titlePane.getStyleClass().add("step-header-optional-rounded");
		} else {
			titlePane.getStyleClass().add("step-header-rounded");
		}

		InvalidationListener presentationChangedListener = new InvalidationListener() {
			public void invalidated(Observable value) {
				
				String cssClass;
				
				if (((ExerciseStepModel) stepModel).isOptional()) {               
					cssClass = "step-header-optional-rounded";
				} else 
					cssClass = "step-header-rounded";

				titlePane.getStyleClass().clear();
				titlePane.getStyleClass().add(cssClass);
				
				newGroupToggleButton.visibleProperty().setValue( 						
						((ExerciseStepModel) stepModel).getContainer().getValue() instanceof ExerciseModel);
				
			}
		};

		contentArea.addListener(presentationChangedListener);

		((ExerciseStepModel) stepModel).getIsOptional().addListener(presentationChangedListener);
		// ((ExerciseStepModel) stepModel).isOptional.addListener(presentationChangedListener);
		
		// ((ExerciseStepModel) stepModel).getContainer().addListener(presentationChangedListener);
		
		((ExerciseStepModel) stepModel).getContainer().addListener(presentationChangedListener);
		
		newGroupToggleButton.visibleProperty().setValue(! inGroup ); 
		
		return titlePane;
		
	}

}
