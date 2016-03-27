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
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

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

/**
 * View for {@link LoopModel}. Consists of a loop header which appears much like
 * a normal action step, a {@code StepLayout} to visualize the loop's child 
 * steps and a footer.  
 */
public class ExerciseGroupOfStepsView extends StepViewBasicUI implements IStepViewContainer {

	private ExerciseGroupOfStepsModel loopModel; 
	private StepLayout stepLayout;
	private Pane loopFooter;
	private TextFlowLayout footerText;
	private TextFlowLayout accumText;
	private TextFlowLayout titleText;
	private Group subStepBackground;
	private Line leftBorder;       

	protected static Image groupedIcon;
	protected static Image optionalIcon; 
	protected static Image requiredIcon; 

	protected InAnyOrGivenOrderCommand orderCommand; 	
	protected GroupUngroupSequenceCommand ungroupCommand;
	
	protected ToggleStepOptionalCommand optionalCommand; 
	protected SimpleObjectProperty<Image> currentOptionalCommandIcon; 

	private static Image newGroupIcon; 
	private static Image inAnyOrderIcon;
	private static Image inGivenOrderIcon;

	private SimpleObjectProperty<Image> currentGroupCommandIcon;
	private SimpleObjectProperty<Image> currentOrderCommandIcon; 
	
	public ExerciseGroupOfStepsView(
			final ExerciseGroupOfStepsModel argModel, 
			final IStepViewContainer argParent, 
			final CommonView argProcView) {

		super(argModel, argParent, argProcView);        

		// don't show step index labels / counters in case the order doesn't matter
		this.stepIndexVisibilityProperty.bind(argModel.inAnyOrder.not());    	

		loopModel = argModel;
		stepLayout = new StepLayout(loopModel, this, procedureView);

		prefHeightProperty().unbind();
		setPrefHeight(Region.USE_COMPUTED_SIZE);

		getChildren().addAll(createSubStepBackground(), 
				selectionRect, 
				createLoopFooter(),
				//createFooterIcon(),
				titlePane,
				stepBackground,
				leftBorder, 
				stepLayout);

		getChildren().add(stepIndexLabel);  

		getChildren().addAll(
				//expansionArea,
				borderRect,
				createContentArea());

		// This is so the body of the loop is not displayed while a loop step is 
		// undergoing drag/drop.
		stepLayout.visibleProperty().bind(this.opacityProperty().isEqualTo(1.0, 0.0));

		InvalidationListener listener = new InvalidationListener() {
			public void invalidated(Observable value) {
				String cssClass;

				if (argModel.getInAnyOrder().getValue()) 
					cssClass = "subtask-background-unordered";
				else
					cssClass = "subtask-background-ordered";

				titlePane.getStyleClass().clear();
				titlePane.getStyleClass().add(cssClass);

			}            
		};

		titlePane.getStyleClass().add(
				((ExerciseGroupOfStepsModel) this.stepModel).inAnyOrder.getValue() ? 
						"subtask-background-unordered" : 
						"subtask-background-ordered");
		
		argModel.inAnyOrder.addListener(listener);

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

		return stepIndexLabel;
	}

	@Override
	protected void layoutChildren() {
		stepLayout.setMinWidth(getWidth() - LHS_OFFSET);

		super.layoutChildren();       

		stepLayout.setLayoutY(contentHeight.getValue());        
		stepLayout.setLayoutX(LHS_OFFSET);

		subStepBackground.setLayoutY(contentHeight.getValue());
		leftBorder.setLayoutY(subStepBackground.getLayoutY() - 5);

		loopFooter.setLayoutY(computePrefHeight(0)  - loopFooter.prefHeight(0)); 
	}

	@Override
	protected double computeMinWidth(double height) {
		return computePrefWidth(0);
	}

	@Override
	public double computePrefWidth(double height) {        
		return Math.max(stepLayout.computePrefWidth(height) + LHS_OFFSET,
				StepView.MIN_WIDTH + LHS_OFFSET);
	}    

	@Override
	public double computeMinHeight(double width) {
		return computePrefHeight(width);
	}

	@Override
	public double computePrefHeight(double width) {
		return super.computePrefHeight(width)
				+ (stepLayout.isVisible() ? stepLayout.prefHeight(width) : 0) 
				+ loopFooter.prefHeight(width);
	}

	@Override
	public double computeMaxHeight(double width) {
		return computePrefHeight(width);
	}

	@Override
	public double getCoreHeight() {
		return Math.min(titlePane.getHeight() * 2, getHeight());
	}

	@Override
	public ContainerStepModel getContainerStepModel() {
		return loopModel;
	}

	@Override
	public StepView findStepView(StepModel step) {
		return stepLayout.findStepView(step);
	}

	@Override
	public void updateIssuesVisualization() {
		for (StepView view : stepLayout.getStepViews()) {
			view.updateIssueVisualization();
			if (view instanceof IStepViewContainer)
				((IStepViewContainer)view).updateIssuesVisualization();
		}
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

	
	@Override
	protected Region createContentArea() {
		
		updateTitleText(); 
			
		Pane pane = new Pane() {
			@Override
			protected void layoutChildren() {
				super.layoutChildren(); 

				titleText.setLayoutX(LHS_OFFSET);
				titleText.setLayoutY(PAD * 1.5);

				// Not sure why I need to do this after build 23
				stepLayout.requestLayout();
			}

			@Override
			protected double computePrefHeight(double width) {
				return titleText.minHeight(width) + PAD * 3;
			}            

			@Override
			protected double computePrefWidth(double height) {
				return borderRect.getWidth() - LHS_OFFSET;
			}                  
		};

		titleText.prefWrapLengthProperty().bind(borderRect.widthProperty().subtract(LHS_OFFSET + PAD));   

		pane.getChildren().add(titleText);
		contentArea.setValue(pane);   
		return pane;   
	}

	private Group createSubStepBackground() {
		subStepBackground = new Group();

		Rectangle bg = new Rectangle();
		bg.widthProperty().bind(this.widthProperty());
		bg.heightProperty().bind(this.heightProperty().subtract(contentHeight));           

		LinearGradient lg = new LinearGradient(0, 0, 1, 0, true,
				CycleMethod.NO_CYCLE, new Stop[] {
				new Stop(0.0, (Color) stepLightBackground.getValue()),
				new Stop(1.0, (Color) Colors.WindowBackground) });
		bg.setFill(lg);

		leftBorder = new Line();
		leftBorder.startYProperty().bind(bg.layoutYProperty());
		leftBorder.endYProperty().bind(bg.layoutYProperty().add(bg.heightProperty()));
		leftBorder.startXProperty().bind(bg.layoutXProperty());
		leftBorder.endXProperty().bind(bg.layoutXProperty());
		leftBorder.setStroke(borderColor.getValue());
		leftBorder.strokeProperty().bind(borderColor);
		leftBorder.strokeWidthProperty().bind(borderRect.strokeWidthProperty());

		subStepBackground.getChildren().addAll(bg);

		return subStepBackground;
	}


	private Pane createLoopFooter() {
		
		footerText = new TextFlowLayout(this, 5, 5, false);
		footerText.setFont(Fonts.LARGE); 
		footerText.readOnlyProperty().bind(procedureView.readOnlyProperty());

		accumText = new TextFlowLayout(5, 5);
		accumText.readOnlyProperty().bind(procedureView.readOnlyProperty()); 
		accumText.setTextColor(Colors.DisabledText);
		accumText.setFont(Fonts.STANDARD);        


		final Rectangle bg = new Rectangle();

		loopFooter = new Pane() {
			@Override
			protected void layoutChildren() {
				super.layoutChildren(); 

				accumText.setLayoutX(LHS_OFFSET);
				accumText.setLayoutY(EDGE_ROUNDING);

				footerText.relocate(LHS_OFFSET,
						EDGE_ROUNDING
						+ (accumText.isVisible() ? accumText
								.getLayoutBounds().getHeight() + EDGE_ROUNDING
								: 0));
			}

			@Override
			protected double computePrefHeight(double width) {
				double height = footerText.prefHeight(width) + 2 * EDGE_ROUNDING;

				if (accumText.isVisible())
					height += accumText.prefHeight(width) + EDGE_ROUNDING;

				return height;
			}            

			@Override
			protected double computePrefWidth(double height) {
				return borderRect.getWidth();
			}                  
		};

		loopFooter.prefWidthProperty().bind(widthProperty());
		loopFooter.setMaxHeight(Region.USE_PREF_SIZE);

		footerText.prefWrapLengthProperty().bind(
				loopFooter.widthProperty().subtract(DEFAULT_BORDER_WIDTH + RHS_PAD));

		accumText.prefWrapLengthProperty().bind(
				loopFooter.widthProperty().subtract(RHS_PAD + DEFAULT_BORDER_WIDTH));

		bg.widthProperty().bind(loopFooter.widthProperty());
		bg.heightProperty().bind(loopFooter.heightProperty());
		bg.fillProperty().bind(stepLightBackground);
		bg.setStroke(borderColor.getValue());
		bg.strokeProperty().bind(borderColor);
		bg.strokeWidthProperty().bind(borderRect.strokeWidthProperty());
		bg.setArcHeight(2 * EDGE_ROUNDING);
		bg.setArcWidth(2 * EDGE_ROUNDING);        

		loopFooter.getChildren().addAll(bg);

		return loopFooter;
	}

	public void unselectDescendents() {
		for (Node node : stepLayout.getChildren()) {
			StepView sv = (StepView)node;
			if (sv.isSelected())
				procedureView.getSelectionManager().setSelection(sv, false);
			if (sv instanceof ExerciseGroupOfStepsView)
				((ExerciseGroupOfStepsView) sv).unselectDescendents();
		}
	}

	public List<StepView> getStepViews() {
		List<StepView> subStepViews = new ArrayList<StepView>();
		for (Node node : stepLayout.getChildren()) 
			subStepViews.add((StepView)node);        
		return subStepViews;
	}

	public StepLayout getStepLayout() {
		return this.stepLayout;
	}

	public int indexOf(StepView child) {
		return stepLayout.getChildren().indexOf(child);
	}

	/** 
	 * Container steps, unlike regular steps, have a central "content" portion that should not be
	 * selectable. This allows the contained steps to be surrounded by "white space", like any other
	 * step would be. Thus, we must override the isPointInContent, and have it only allow selection
	 * if the user clicks on the header, footer, or within a couple pixels of the line on the LHS.
	 */

	public boolean isPointInContent(double x, double y) {
		return (titlePane.getBoundsInParent().contains(x, y) || 
				stepBackground.getBoundsInParent().contains(x, y) ||
				loopFooter.getBoundsInParent().contains(x, y) ||
				leftBorder.getBoundsInParent().contains(x, y));
	}

	/**
	 * Just as we override isPointInContent, we also override intersectsContent for the same reason.
	 */
	public boolean intersectsContent(Bounds box) {
		return (titlePane.getBoundsInParent().intersects(box) ||
				stepBackground.getBoundsInParent().intersects(box) ||
				loopFooter.getBoundsInParent().intersects(box) ||
				leftBorder.getBoundsInParent().intersects(box));
	}

	protected Pane createTitlePane() {

		final ExerciseGroupOfStepsView owner = this;

		//
		// configure commands
		//

		ungroupCommand    = new GroupUngroupSequenceCommand((ExerciseEditController) procedureView.getController(), this);				
		groupedIcon   = Utilities.getImage("letter-G-icon.png");

		orderCommand     = new InAnyOrGivenOrderCommand((ExerciseEditController) procedureView.getController(), this);
		inAnyOrderIcon   = Utilities.getImage("letter-A-icon.png");
		inGivenOrderIcon = Utilities.getImage("letter-S-icon.png");
		
		currentOrderCommandIcon = new SimpleObjectProperty<Image>(
				((ExerciseGroupOfStepsModel) this.stepModel).inAnyOrder.getValue() ? inAnyOrderIcon : inGivenOrderIcon);	
		
		//
		//
		//
		
		optionalCommand = new ToggleStepOptionalCommand((ExerciseEditController) procedureView.getController(), this);
		optionalIcon = Utilities.getImage("letter-O-icon.png");
		requiredIcon   = Utilities.getImage("letter-R-icon.png");
		
		currentOptionalCommandIcon = new SimpleObjectProperty<Image>(requiredIcon); 	

		//
		//
		//

		final ToolTippedImageView ungroupButton  = new ToolTippedImageView(groupedIcon);
		final ToolTippedImageView orderToggleButton  = new ToolTippedImageView(inGivenOrderIcon);
		final ToolTippedImageView optionalToggleButton  = new ToolTippedImageView(requiredIcon);
		
		//
		//
		// 
	
		headerText = new TextFlowLayout("Group", this);	
		headerText.setPadding(new Insets(1.5 * PAD, 0, 1.5 * PAD, 0));

		final Line bottomBorder = new Line();
	
		titlePane = new Pane() {
			@Override
			protected void layoutChildren() {

				super.layoutChildren(); // Resizes children to preferred sizes

				optionalToggleButton.setLayoutX(getWidth() - 2 * PAD - TITLE_PANE_BUTTON_SIZE);
				optionalToggleButton.setLayoutY(getHeight() / 2 - (TITLE_PANE_BUTTON_SIZE / 2));
				
				ungroupButton.setLayoutX(getWidth() - 2 * PAD - 2 * TITLE_PANE_BUTTON_SIZE);
				ungroupButton.setLayoutY(getHeight() / 2 - (TITLE_PANE_BUTTON_SIZE / 2));
				
				orderToggleButton.setLayoutX(getWidth() - 2 * PAD - 3 * TITLE_PANE_BUTTON_SIZE);
				orderToggleButton.setLayoutY(getHeight() / 2 - (TITLE_PANE_BUTTON_SIZE / 2));
								
				headerText.setLayoutX(TITLE_PANE_LHS);
			}

			@Override
			public double computePrefHeight(double width) {
				return headerText.prefHeight(-1);
			}
		};

		double right = 3 * PAD + TITLE_PANE_BUTTON_SIZE + Knurling.WIDTH;
		right += showConfigButton.getValue() ? PAD + TITLE_PANE_BUTTON_SIZE : 0;

		headerText.prefWrapLengthProperty().bind(
				widthProperty().subtract(2 * DEFAULT_BORDER_WIDTH + TITLE_PANE_LHS + right));
		
		titlePane.prefWidthProperty().bind(widthProperty().subtract(2 * DEFAULT_BORDER_WIDTH));
		titlePane.setMinWidth(Region.USE_PREF_SIZE);
		titlePane.setMaxWidth(Region.USE_PREF_SIZE);
		titlePane.setMinHeight(Region.USE_PREF_SIZE);
		titlePane.setMaxHeight(Region.USE_PREF_SIZE);

		bottomBorder.visibleProperty().bind(contentArea.isNotNull().or(expanded));
		bottomBorder.setStroke(borderColor.getValue());
		bottomBorder.setStrokeWidth(DEFAULT_BORDER_WIDTH);

		bottomBorder.setStartX(0);
		bottomBorder.endXProperty().bind(titlePane.widthProperty());
		bottomBorder.startYProperty().bind(titlePane.heightProperty().subtract(DEFAULT_BORDER_WIDTH));
		bottomBorder.endYProperty().bind(titlePane.heightProperty().subtract(DEFAULT_BORDER_WIDTH));

		titlePane.getChildren().addAll(headerText, bottomBorder, orderToggleButton, ungroupButton, optionalToggleButton);               

		titlePane.getStyleClass().add("step-header-rounded");
		InvalidationListener roundedListener = new InvalidationListener() {
			public void invalidated(Observable value) {
				String cssClass;
				if (contentArea.getValue() == null && !expanded.getValue())
					cssClass = "step-header-rounded";
				else
					cssClass = "step-header-half-rounded";

				titlePane.getStyleClass().clear();
				titlePane.getStyleClass().add(cssClass);
				
			}
		};
		
		contentArea.addListener(roundedListener);
		expanded.addListener(roundedListener); 

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

		((ExerciseGroupOfStepsModel) owner.stepModel).getInAnyOrder().addListener(new InvalidationListener() {
			public void invalidated(Observable value) {			

				ExerciseGroupOfStepsModel model =  (ExerciseGroupOfStepsModel) owner.getStepModel(); 				
				if (model.getInAnyOrder().getValue()) {
					currentOrderCommandIcon.setValue(inAnyOrderIcon);
				} else {                       
					currentOrderCommandIcon.setValue(inGivenOrderIcon);
				}
				
				updateTitleText();
				recalcHeight();   
			}
		}); 
		
		
	
		
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
            	updateTitleText(); 
                recalcHeight();   
			}
		}); 
       
		//	
		//
		//

		return titlePane;
	}
	
}
