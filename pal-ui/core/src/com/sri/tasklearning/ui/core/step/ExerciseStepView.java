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

import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.control.ToolTippedImageView;
import com.sri.tasklearning.ui.core.exercise.ExerciseEditController;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.exercise.ExerciseView;
import com.sri.tasklearning.ui.core.exercise.GroupUngroupSequenceCommand;
import com.sri.tasklearning.ui.core.exercise.NewGroupSequenceCommand;
import com.sri.tasklearning.ui.core.exercise.ToggleStepOptionalCommand;
import com.sri.tasklearning.ui.core.layout.TextFlowLayout;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;

/**
 * View for {@link ExerciseStepModel}.  
 */

public class ExerciseStepView extends StepViewBasicUI {

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
	
	protected ChangeListener listener = null;
	protected ChangeListener listener2 = null;
	

	public SimpleDoubleProperty socketNorthXInParent = new SimpleDoubleProperty(); 
	public SimpleDoubleProperty socketNorthYInParent = new SimpleDoubleProperty(); 
    
	public SimpleDoubleProperty socketSouthXInParent = new SimpleDoubleProperty(); 
	public SimpleDoubleProperty socketSouthYInParent = new SimpleDoubleProperty();     
    
	public SimpleDoubleProperty socketEastXInParent = new SimpleDoubleProperty(); 
	public SimpleDoubleProperty socketEastYInParent = new SimpleDoubleProperty(); 
    
	public SimpleDoubleProperty socketWestXInParent = new SimpleDoubleProperty(); 
	public SimpleDoubleProperty socketWestYInParent = new SimpleDoubleProperty(); 
    	 
	public ExerciseStepView(

			final ExerciseStepModel argModel, 
			final IStepViewContainer argParent, 
			final ExerciseView argProcView) {

	     super(argModel, argParent, argProcView);
	     
	     super.addChildren();        

	}
	
/*
    public void recalcHeight() {    	

        contentHeight.setValue( StepView.DEFAULT_STEP_HEIGHT); 
    	
    } */


	protected Pane createTitlePane() {
		
		final ExerciseStepView owner = this; 
		
		boolean inGroup = ! (((ExerciseStepModel) stepModel).getContainer().getValue() instanceof ExerciseModel); 
		
		//
		// configure commands
		//
		
		optionalCommand = new ToggleStepOptionalCommand((ExerciseEditController) commonView.getController(), this);
		groupCommand    = new GroupUngroupSequenceCommand((ExerciseEditController) commonView.getController(), this);				
		newGroupCommand = new NewGroupSequenceCommand((ExerciseEditController) commonView.getController(), this);				
		 
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
		
		};
				
		
	    listener = new ChangeListener() {		
				@Override
				public void changed(ObservableValue observable, Object oldValue, Object newValue) {
					updateTitlePaneWidth(); 						
				}
			}; 	        		
			
	   listener2 = new ChangeListener() {		
					@Override
					public void changed(ObservableValue observable, Object oldValue, Object newValue) {
						updateAnchors(); 						
					}
				}; 	        		
			
		
		parentView.addListener(listener); 				
		parentView.addListener(listener2);	        

		boundsInParentProperty().addListener(listener); 
		boundsInParentProperty().addListener(listener2); 
		
		forceRefresh(); 
						
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
                //recalcHeight();   
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
            	            	
                //recalcHeight();   
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
   
        titlePane.getChildren().addAll(headerText, // bottomBorder,  newGroupToggleButton, 
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

		((ExerciseStepModel) stepModel).getIsOptional().addListener(presentationChangedListener);
		// ((ExerciseStepModel) stepModel).isOptional.addListener(presentationChangedListener);
		
		// ((ExerciseStepModel) stepModel).getContainer().addListener(presentationChangedListener);
		
		((ExerciseStepModel) stepModel).getContainer().addListener(presentationChangedListener);
		
		newGroupToggleButton.visibleProperty().setValue(! inGroup ); 
		
		return titlePane;
		
	}
	
	
	void updateTitlePaneWidth() {
		
	  if ( parentView.getValue() instanceof ExerciseView )  
		  titlePane.setPrefWidth( StepView.DEF_WIDTH); 
		else 
		 titlePane.setPrefWidth( StepView.DEF_WIDTH - LHS_OFFSET );
	  
	}
	
	public void forceRefresh() {
		
		// updateAnchors(); 

		if (listener != null)
			listener.changed(null,  null,  null);
		if (listener2 != null) 
			listener2.changed(null,  null,  null);
		
	}
	
		  
	public void updateAnchors() {

		super.updateAnchors();		

		if ( getStepViewContainer() instanceof ExerciseSubtaskView) {

			Pane parent = titlePane;
			Bounds bounds = parent.getLayoutBounds();

			int i = 0;

			// umm, number 6 is the magic number of transformations that 
			// we need to compose in order to get to the right parent coordinate system!!
			// that should be rewritten..			
			while (parent != null && i < 6) {
				bounds = parent.localToParent(bounds); 				 
				parent = (Pane) parent.getParent();
				i++; 
			}

			if (parent != null) {

				socketNorthXInParent.setValue( (bounds.getMinX() + bounds.getMaxX()) / 2); 
				socketSouthXInParent.setValue( (bounds.getMinX() + bounds.getMaxX()) / 2); 

				socketNorthYInParent.setValue( (bounds.getMinY() - 4 ));  
				socketSouthYInParent.setValue( (bounds.getMinY() + getHeight() - 8 ));

				socketEastXInParent.setValue( bounds.getMaxX() ); 
				socketEastYInParent.setValue( (bounds.getMinY() + bounds.getMaxY()) / 2);

				socketWestXInParent.setValue( bounds.getMinX() ); 
				socketWestYInParent.setValue( (bounds.getMinY() + bounds.getMaxY()) / 2);
			}
		}

	}; 


}
