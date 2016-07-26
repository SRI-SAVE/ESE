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
import java.util.List;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.Knurling;
import com.sri.tasklearning.ui.core.control.ToolTippedImageView;
import com.sri.tasklearning.ui.core.exercise.ExerciseEditController;
import com.sri.tasklearning.ui.core.exercise.GroupUngroupSequenceCommand;
import com.sri.tasklearning.ui.core.exercise.InAnyOrGivenOrderCommand;
import com.sri.tasklearning.ui.core.exercise.ToggleStepOptionalCommand;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.layout.TextFlowLayout;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * View for {@link ExerciseGroupOfSteps}. 
 */
public class ExerciseSubtaskView extends StepViewBasicUI implements IStepViewContainer {

	private ExerciseGroupOfStepsModel loopModel; 
	private StepLayout stepLayout;

	private TextFlowLayout titleText;

	protected static Image groupedIcon;
	
	protected static Image optionalIcon; 
	protected static Image requiredIcon; 

	// protected InAnyOrGivenOrderCommand orderCommand; 	
	protected GroupUngroupSequenceCommand ungroupCommand;
	
	protected ToggleStepOptionalCommand optionalCommand; 
	protected SimpleObjectProperty<Image> currentOptionalCommandIcon; 

	private static Image newGroupIcon;
	
	// private static Image inAnyOrderIcon;
	// private static Image inGivenOrderIcon;
	
	private SimpleObjectProperty<Image> currentGroupCommandIcon;
	private SimpleObjectProperty<Image> currentOrderCommandIcon; 
	
	public ExerciseSubtaskView(
			final ExerciseGroupOfStepsModel argModel, 
			final IStepViewContainer argParent, 
			final CommonView argProcView) {

		super(argModel, argParent, argProcView);        

		// don't show step index labels / counters in case the order doesn't matter
		
		this.stepIndexVisibilityProperty.bind(argModel.inAnyOrder.not());    	

		loopModel = argModel;
		stepLayout = new StepLayout(loopModel, this, commonView);
		
		// stepLayout.showArrows.bind(argModel.inAnyOrder.not());		
		
		// updateTitleText(); 
		
		//titleText.relocate(LHS_OFFSET, PAD / 2);		
		//titlePane.getChildren().addAll(titleText);

			
		//prefHeightProperty().unbind();
		//setPrefHeight(Region.USE_COMPUTED_SIZE);

		/* 
		getChildren().addAll( 
				selectionRect, 
				titlePane,
				stepLayout, 
				stepIndexLabel, 
				borderRect); */
		
		addChildren();
		

		
		titlePane.getChildren().addAll(stepLayout); 

		stepLayout.setLayoutX(LHS_OFFSET);
		stepLayout.setLayoutY(UP_OFFSET); 			
		
		stepLayout.visibleProperty().bind(this.opacityProperty().isEqualTo(1.0, 0.0));

		// stepLayout.setPrefWidth(StepView.DEF_WIDTH );
		titlePane.setPrefWidth(StepView.DEF_WIDTH);
		
		InvalidationListener listener = new InvalidationListener() {
			public void invalidated(Observable value) {
				String cssClass;

				/* if (argModel.getInAnyOrder().getValue()) 
					cssClass = "subtask-background-unordered";
				else
					cssClass = "subtask-background-ordered";

				titlePane.getStyleClass().clear();
				titlePane.getStyleClass().add(cssClass); */

			}            
		};

		titlePane.getStyleClass().add(
				"subtask-background-new"); 
		

		argModel.inAnyOrder.addListener(listener);
		
		  
        ChangeListener listener1 = new ChangeListener() {		
			@Override
			public void changed(ObservableValue observable, Object oldValue, Object newValue) {
				updateAnchors();  	
				
				for (StepView view: getStepViews()) {
					// force this, because the sub-arrows need to be recomputed!!!
					if (! ( view instanceof PlaceholderStepView))
					  ((ExerciseStepView) view).updateAnchors();					
				}
				
			}
		}; 
        
		boundsInParentProperty().addListener(listener1);
				
		parentView.addListener(listener1);  
		listener1.changed(null,  null,  null);
		
		if (argModel.isOptional()) {
        	currentOptionalCommandIcon.setValue(optionalIcon);
        } else {                       
        	currentOptionalCommandIcon.setValue(requiredIcon);
        }

	}

	@Override
	protected Label createStepIndexLabel() {

		stepIndexLabel = new Label();        
		stepIndexLabel.textProperty().bind(new SimpleStringProperty("G").concat(((ExerciseGroupOfStepsModel) this.getStepModel()).getSteps().get(0).indexProperty().add(1).asString()));  

		stepIndexLabel.setFont(Fonts.STEP_NUMBERING);
		stepIndexLabel.setTextFill(Colors.SystemGray);

		this.selected.addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> val, Boolean oldVal, Boolean newVal) {
				stepIndexLabel.setTextFill(newVal ? Colors.SelectedStepBorder : Colors.SystemGray);
			}
		});

		// Don't show label while a step is being dragged around the Scene

		stepIndexLabel.visibleProperty().bind(this.opacityProperty().isEqualTo(1).and(this.getStepViewContainer().getStepIndexVisibility())); 

		stepIndexLabel.layoutXProperty().bind(titlePane.layoutXProperty().subtract(stepIndexLabel.widthProperty()).subtract(PAD * 1.5));
		stepIndexLabel.layoutYProperty().bind(titlePane.heightProperty().divide(2).subtract(10)); 

		return stepIndexLabel;
	}

	@Override
	protected void layoutChildren() {		

		super.layoutChildren();
		
	}

	@Override
	public ContainerStepModel getContainerStepModel() {
		return loopModel;
	}

	@Override
	public StepView findStepView(StepModel step) {
		return stepLayout.findStepView(step);
	}
	
	protected void updateTitleText() {
		
		if (titleText == null) {
			titleText = new TextFlowLayout(this);  
			titleText.setFont(Fonts.STANDARD);
			titleText.setTextColor(Colors.DisabledText);
			titleText.setAlignment(Pos.CENTER_LEFT);
		}

		List<Object> list = new ArrayList<Object>(); 
		list.add("Steps of this " + ( loopModel.isOptional() ? "optional " : "mandatory " ) + ( loopModel.inAnyOrder() ? "unordered " : "ordered " ) +  "group:");
	
		titleText.setContents(list);
	}

	public void unselectDescendents() {
		for (Node node : stepLayout.vbox.getChildren()) {
			StepView sv = (StepView)node;
			if (sv.isSelected())
				commonView.getSelectionManager().setSelection(sv, false);
			if (sv instanceof ExerciseSubtaskView )
				((ExerciseSubtaskView) sv).unselectDescendents();
		}
	}

	public List<StepView> getStepViews() {
		List<StepView> subStepViews = new ArrayList<StepView>();
		for (Node node : stepLayout.vbox.getChildren()) 
			subStepViews.add((StepView)node);        
		return subStepViews;
	}

	public StepLayout getStepLayout() {
		return this.stepLayout;
	}

	public int indexOf(StepView child) {
		return stepLayout.vbox.getChildren().indexOf(child);
	}

	public boolean isPointInContent(double x, double y) {
		return (titlePane.getBoundsInParent().contains(x, y) );
	}

	public boolean intersectsContent(Bounds box) {
		return (titlePane.getBoundsInParent().intersects(box) );
	}
	
	protected Pane createTitlePane() {

		final ExerciseSubtaskView owner = this;

		//
		// configure commands
		//

		ungroupCommand    = new GroupUngroupSequenceCommand((ExerciseEditController) commonView.getController(), this);				
		groupedIcon   = Utilities.getImage("letter-G-icon.png");

		/* 
		  
		 
		orderCommand     = new InAnyOrGivenOrderCommand((ExerciseEditController) commonView.getController(), this);
		inAnyOrderIcon   = Utilities.getImage("letter-A-icon.png");
		inGivenOrderIcon = Utilities.getImage("letter-S-icon.png");
		
		currentOrderCommandIcon = new SimpleObjectProperty<Image>(
				((ExerciseGroupOfStepsModel) this.stepModel).inAnyOrder.getValue() ? inAnyOrderIcon : inGivenOrderIcon);	
		
		*/ 
		
		//
		//
		//
				
		optionalCommand = new ToggleStepOptionalCommand((ExerciseEditController) commonView.getController(), this);
		optionalIcon = Utilities.getImage("letter-O-icon.png");
		requiredIcon   = Utilities.getImage("letter-R-icon.png");
		
		currentOptionalCommandIcon = new SimpleObjectProperty<Image>(requiredIcon);
		
        	
		//
		//
		//

		final ToolTippedImageView ungroupButton  = new ToolTippedImageView(groupedIcon);
				
		//final ToolTippedImageView orderToggleButton  = new ToolTippedImageView(inGivenOrderIcon);
		
		final ToolTippedImageView optionalToggleButton  = new ToolTippedImageView(requiredIcon);
		
		//
		//
		// 
	
		// headerText = new TextFlowLayout("Group", this);	
		// headerText.setPadding(new Insets(1.5 * PAD, 0, 1.5 * PAD, 0));

		titlePane = new Pane() {
			@Override
			protected void layoutChildren() {

				super.layoutChildren(); // Resizes children to preferred sizes

				optionalToggleButton.setLayoutX(getWidth() - 2 * PAD - TITLE_PANE_BUTTON_SIZE);
				//optionalToggleButton.setLayoutY(StepView.DEFAULT_STEP_HEIGHT / 2 + (TITLE_PANE_BUTTON_SIZE / 2));
				optionalToggleButton.setLayoutY( (TITLE_PANE_BUTTON_SIZE / 2));
				
				ungroupButton.setLayoutX(getWidth() - 2 * PAD - 2 * TITLE_PANE_BUTTON_SIZE);
				//ungroupButton.setLayoutY(StepView.DEFAULT_STEP_HEIGHT / 2 + (TITLE_PANE_BUTTON_SIZE / 2));
				ungroupButton.setLayoutY( (TITLE_PANE_BUTTON_SIZE / 2));
				
				// orderToggleButton.setLayoutX(getWidth() - 2 * PAD - 3 * TITLE_PANE_BUTTON_SIZE);
				//orderToggleButton.setLayoutY(StepView.DEFAULT_STEP_HEIGHT / 2 + (TITLE_PANE_BUTTON_SIZE / 2));
				// orderToggleButton.setLayoutY( (TITLE_PANE_BUTTON_SIZE / 2));
								
				// headerText.setLayoutX(TITLE_PANE_LHS);
				
			}

		
		};

		// double right = 3 * PAD + TITLE_PANE_BUTTON_SIZE + Knurling.WIDTH;
	
		/* headerText.prefWrapLengthProperty().bind(
				widthProperty().subtract(2 * DEFAULT_BORDER_WIDTH + TITLE_PANE_LHS + right)); */
		

		// titlePane.getChildren().addAll(headerText, orderToggleButton, ungroupButton, optionalToggleButton);
		// titlePane.getChildren().addAll(headerText, ungroupButton, optionalToggleButton);
		
		// titlePane.getChildren().addAll(ungroupButton, optionalToggleButton);
		titlePane.getChildren().addAll(ungroupButton); 
		
		titlePane.getStyleClass().add("step-header-rounded");

		//
		// push ungroup button
		// 

		ungroupButton.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
			public void handle(MouseEvent e) {
				e.consume();
			}
		});

		ungroupButton.setOnMouseReleased(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent e) {
				e.consume();
				if (ungroupButton.contains(e.getX(), e.getY())) {      					
					ungroupCommand.invokeCommand(owner);          	      	
				}
			}
		}); 

		//
		// in any / given order toggle button  
		//

		/* 
		orderToggleButton.mouseTransparentProperty().bind(disabledProperty());        
		orderToggleButton.imageProperty().bind(currentOrderCommandIcon);
		orderToggleButton.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
			public void handle(MouseEvent e) {
				e.consume();
			}
		});

	
		orderToggleButton.setOnMouseReleased(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent e) {
				e.consume();
				if (orderToggleButton.contains(e.getX(), e.getY())) {        
					orderCommand.invokeCommand(owner);          	      	
				}
			}
		}); 
		 */
		
		/*
		((ExerciseGroupOfStepsModel) owner.stepModel).getInAnyOrder().addListener(new InvalidationListener() {
			public void invalidated(Observable value) {			

				ExerciseGroupOfStepsModel model =  (ExerciseGroupOfStepsModel) owner.getStepModel(); 		
				
				if (model.getInAnyOrder().getValue()) {
					currentOrderCommandIcon.setValue(inAnyOrderIcon);
				} else {                       
					currentOrderCommandIcon.setValue(inGivenOrderIcon);
				}				

				// updateTitleText();
				//recalcHeight();   
			}
		}); */ 
		
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
        
       ((ExerciseGroupOfStepsModel) owner.stepModel).getIsOptional().addListener(new InvalidationListener() {
			public void invalidated(Observable value) {			
												
				ExerciseGroupOfStepsModel model =  (ExerciseGroupOfStepsModel) owner.getStepModel(); 				
            	if (model.isOptional()) {
                	currentOptionalCommandIcon.setValue(optionalIcon);
                } else {                       
                	currentOptionalCommandIcon.setValue(requiredIcon);
                }
            	// updateTitleText(); 
                //recalcHeight();   
			}
		}); 
       
		//	
		//
		//

		return titlePane;
	}



}
