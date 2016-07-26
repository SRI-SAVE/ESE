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

package com.sri.tasklearning.ui.core.layout;

import java.util.ArrayList;
import java.util.List;

import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.exercise.ExerciseView;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseGroupOfStepsModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepView;
import com.sri.tasklearning.ui.core.step.ExerciseSubtaskModel;
import com.sri.tasklearning.ui.core.step.ExerciseSubtaskView;
import com.sri.tasklearning.ui.core.step.IStepViewContainer;
import com.sri.tasklearning.ui.core.step.PlaceholderStepView;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.step.StepViewBasicUI;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineJoin; 

/**
 * An extension of VBox intended for visualizing an ordered group of steps. 
 * The top-level of a procedure has its own StepLayout, as does each loop in 
 * a procedure regardless of nesting. 
 */
public final class StepLayout extends Pane {

	private static final double ARROW_STROKE_WIDTH = 3; 
	private static final double ARROW_HEAD_SIZE = 6;
	private static final double EXTRA_TOP_SPACE = 80; 

	private CommonView procedureView;
	private IStepViewContainer parentView;
	private ContainerStepModel containerStepModel; // Loop or Procedure
	
	private ExerciseModel exerciseModel; 

	public VBox vbox; 
	public Pane arrows;
	private ExerciseView exerciseView; 
	
	public StepLayout(
			final ContainerStepModel argContainer, 
			final IStepViewContainer argParent, 
			final CommonView argProcView) {
		
		
			if ( argProcView.getModel() instanceof ExerciseModel ) {
		    	exerciseModel = (ExerciseModel) argProcView.getModel();
			} 
			
			exerciseView = (ExerciseView) argProcView; 
		
		vbox = new VBox( StepView.PAD );
		arrows = new Pane(); 

		if ( argParent instanceof ExerciseView ) { 
			// in case this is part of an ExerciseView, add extra space at top 
			vbox.setTranslateY(EXTRA_TOP_SPACE);
			arrows.setTranslateY(EXTRA_TOP_SPACE);
		}

		arrows.setMouseTransparent(true);
			
		this.getChildren().addAll(vbox, arrows);

		containerStepModel = argContainer; 
		parentView = argParent;
		procedureView = argProcView; 

		StepViewBasicUI lastStepView = null; 

		for (StepModel step : containerStepModel.getSteps()) {
			StepViewBasicUI stepView = (StepViewBasicUI) createStepView(step);   
			if (stepView != null) {
				vbox.getChildren().add(stepView);     

				/* 
            	if (lastStepView != null) {

            		Arrow arrow = new Arrow(
            				lastStepView.socketSouthX, lastStepView.socketSouthY, 
            				stepView.socketNorthX, stepView.socketNorthY); 

            		arrows.getChildren().add(arrow);

            	} */

				lastStepView = stepView; 
			}
		}               

		setOnMousePressed(procedureView.getOnMousePressed());
		setOnMouseReleased(procedureView.getOnMouseReleased());
		setOnMouseDragged(procedureView.getOnMouseDragged());

	}   

	//
	//
	//
	

	protected CommonView getExerciseView() { 
		
		return getExerciseView(this.procedureView); 
	}
	
	protected CommonView getExerciseView(CommonView self) {
		
		if ( self instanceof ExerciseView ) 
			return self; 
		else return getExerciseView((CommonView) self.getParent()); 
		
	}
	
	@Override
	protected double computeMinWidth(double height) {

		double maxStepWidth = 0;
		for (Object stepObj : vbox.getChildren()) {
			if (stepObj instanceof StepView) {
				if (((StepView) stepObj).prefWidth(0) > maxStepWidth)
					maxStepWidth = ((StepView) stepObj).prefWidth(0);
			}

		}
		return maxStepWidth;
	}
	
	@Override
	public double computePrefWidth(double height) {
		return computeMinWidth(height);
	}

	@Override
	protected double computeMaxWidth(double height) {
		return computeMinWidth(height);
	}

	//
	//
	//
	/* 
	@Override
	protected double computeMinHeight(double width) {
		
		double height = 0;
			for (Object stepObj : vbox.getChildren()) {
				if (stepObj instanceof StepView) 
					height += (((StepView) stepObj).prefHeight(0)) + INTER_STEP_PADDING; 
			}
	  return height; 
	}        
	
	@Override
	protected double computePrefHeight(double width) {
		return computeMinHeight(getWidth());
	}     
	
	
	@Override
	protected double computeMaxHeight(double width) {
		return computeMinHeight(getWidth());
	}          
*/ 
	//
	//
	//

	public StepView createStepView(StepModel step) {

		StepView stepView; 

		switch (step.getStepType()) {

		case PLACEHOLDER: stepView = new PlaceholderStepView(step, parentView, procedureView); break; 

		//        
		case EXERCISE_STEP : stepView = ((ExerciseStepModel) step).isHidden() ? null : new ExerciseStepView((ExerciseStepModel) step, parentView, (ExerciseView) procedureView); break;
		case EXERCISE_GROUP : stepView = new ExerciseSubtaskView((ExerciseGroupOfStepsModel) step, parentView, (ExerciseView) procedureView); break;
		case EXERCISE_SUBTASK : stepView = new ExerciseSubtaskView((ExerciseSubtaskModel) step, parentView, (ExerciseView) procedureView); break; 

		//

		default:
			throw new RuntimeException("Unsupported model type passed to getStepView()");   
		}                  

		return stepView;  
	}

	public ContainerStepModel getContainerStepModel() {
		return containerStepModel;
	}

	public int indexOf(StepView child) {
		return vbox.getChildren().indexOf(child);
	}

	public StepView findStepView(StepModel model) {
		for (Node node : vbox.getChildren()) {
			if (node instanceof StepView) {
				StepView sv = (StepView)node; 
				if (sv.getStepModel() == model) 
					return sv;
				else if (sv.getStepModel() instanceof ContainerStepModel) {
					StepView result = ((IStepViewContainer)sv).findStepView(model);
					if (result != null)
						return result; 
				}
			}
		}
		return null; 
	}

	// WARNING: This method should not be called directly except from the 
	// EditController or to facilitate drag placeholder logic
	public boolean addStepView(StepView view, int index) {
		if (vbox.getChildren().indexOf(view) < 0) {
			view.setView(procedureView);
			view.setStepViewContainer(parentView);           
			if (index >= vbox.getChildren().size())
				vbox.getChildren().add(view);
			else 
				vbox.getChildren().add(Math.max(0, index), view);
		}
		
		layout();
		
		return true; 
	}

	public boolean stepCanBeMovedToPosition(StepView view, int index) {

		List<StepView> steps = getStepViews(); 

		steps.remove(view);				

		steps.add(index, view);

		StepModel model = view.getStepModel();
		if (index > 0)
			for (StepView before : steps.subList(0,  index)) {
				if ( ! before.getStepModel().mayPrecede(model))  
					return false; 									
			}	

		if (index+1 < steps.size()) 
			for (StepView after : steps.subList(index+1, steps.size())) {
				if ( ! model.mayPrecede(after.getStepModel())) 
					return false; 		
			}

		return true; 

	}
	
	/*
	public boolean stepCanBeMovedToPosition(StepView view, int index) {
					
		List<StepView> steps = getStepViews(); 

		steps.remove(view);		
		steps.add(index, view);
		
		StepModel model = view.getStepModel();
		
		if (index > 0)
			for (StepView before : steps.subList(0,  index)) 
			if ( ! before.getStepModel().mayPrecede(model)) 
				return false; 									
		
		if (index < steps.size()) 
		  for (StepView after : steps.subList(index, steps.size())) 
			if ( ! model.mayPrecede(after.getStepModel())) 
				return false; 									
	    		
		return true; 
		
	}
	
	*/

	// WARNING: This method should not be called directly except from the 
	// EditController or to facilitate drag placeholder logic
	public boolean deleteStepView(StepView view) {
		return vbox.getChildren().remove(view);
	}

	public List<StepView> getStepViews() {
		List<StepView> subStepViews = new ArrayList<StepView>();
		for (Node node : vbox.getChildren()) 
			if (node instanceof StepView)
				subStepViews.add((StepView)node);        
		return subStepViews;
	}
	
	public List<StepView> getFlattenedStepViews() {
		
		List<StepView> subStepViews = new ArrayList<StepView>();
		
		for (Node node : vbox.getChildren()) {
			if (node instanceof ExerciseSubtaskView) 
				subStepViews.addAll(((ExerciseSubtaskView) node).getStepViews());
			else
				subStepViews.add((StepView) node);
		}
		
		return subStepViews;
	}


	public IStepViewContainer getStepViewContainer() {
		if (parentView != null)
			return parentView;
		else
			return procedureView; 
	}     

	
	@Override 
	public void layoutChildren() {

		super.layoutChildren();
		
		redrawArrows();
	}
	
	
	public void redrawArrows() {

		StepView lastStepView = null; 
		
		vbox.requestLayout();

		arrows.getChildren().clear();
		
		for (StepView fromView : getStepViews()) 
			fromView.layout();

		//
		// draw the arrows 
		// 

		if ( this.getStepViewContainer() instanceof ExerciseView) { 


			// fixed successors in red 

			for (StepView fromView : getFlattenedStepViews()) {

				if ( ! ( fromView instanceof PlaceholderStepView ) ) {

					StepModel from = fromView.getStepModel(); 

					for (StepModel to : from.getFixedSuccessors()) {

						for (StepView toView : getFlattenedStepViews()) {

							if (to != from && toView.getStepModel() == to) {

								boolean fromInGroup = (fromView.getStepViewContainer() instanceof ExerciseSubtaskView);
								boolean toInGroup = (toView.getStepViewContainer() instanceof ExerciseSubtaskView);

								// System.out.println("   " + fromInGroup + " : " + toInGroup);

								if (! fromInGroup && ! toInGroup ) {

									double centerX = fromView.socketWestX.doubleValue();														
									double centerY = (fromView.socketWestY.doubleValue() + toView.socketWestY.doubleValue()) / 2;
									double radiusY = Math.abs(fromView.socketWestY.doubleValue() - toView.socketWestY.doubleValue()) / 2;
									double radiusX = Math.min(radiusY / 2.5, 50);  

									Arc arrow = new Arc(centerX, centerY, radiusX, radiusY, 90, 180);

									arrow.setType(ArcType.OPEN);
									arrow.setStroke(Color.RED);
									arrow.setFill(null);
									arrow.setStrokeWidth(ARROW_STROKE_WIDTH);

									Polygon head = new Polygon();

									head.getPoints().addAll(new Double[]{
											centerX - ARROW_HEAD_SIZE - radiusX,  
											centerY - ARROW_HEAD_SIZE,

											centerX - radiusX,  
											centerY, 

											centerX + ARROW_HEAD_SIZE - radiusX, 
											centerY - ARROW_HEAD_SIZE
									}); 

									head.setStroke(Color.RED);
									head.setFill(Color.RED);
									head.setStrokeWidth(ARROW_STROKE_WIDTH);

									arrow.setMouseTransparent(true);
									head.setMouseTransparent(true);



									arrows.getChildren().addAll(arrow, head);							
									break;

								} else if ( true || fromInGroup != toInGroup ) { // one is in group 

									//fromView = fromInGroup ? (ExerciseSubtaskView) fromView.getStepViewContainer() : fromView; 
									//toView = toInGroup ? (ExerciseSubtaskView) toView.getStepViewContainer() : toView; 

									double fromWestX = fromView.socketWestX.doubleValue();		
									double fromWestY = fromInGroup ? (((ExerciseStepView) fromView).socketWestYInParent.doubleValue()) : fromView.socketWestY.doubleValue();									
									double fromIncX = fromInGroup ? (((ExerciseStepView) fromView).socketWestXInParent.doubleValue()) - fromWestX : 0; 

									double toWestX = toView.socketWestX.doubleValue();		
									double toWestY = toInGroup ? (((ExerciseStepView) toView).socketWestYInParent.doubleValue()) : toView.socketWestY.doubleValue();										
									double toIncX = toInGroup ? (((ExerciseStepView) toView).socketWestXInParent.doubleValue()) - toWestX : 0; 

									double centerX = fromWestX;														
									double centerY = (fromWestY + toWestY) / 2;
									double radiusY = Math.abs(fromWestY - toWestY) / 2;
									double radiusX = Math.min(radiusY / 2.5, 50);  

									Arc arrow = new Arc(centerX, centerY, radiusX, radiusY, 90, 180);

									arrow.setType(ArcType.OPEN);
									arrow.setStroke(Color.RED);
									arrow.setFill(null);
									arrow.setStrokeWidth(ARROW_STROKE_WIDTH);

									Polygon head = new Polygon();

									head.getPoints().addAll(new Double[]{
											centerX - ARROW_HEAD_SIZE - radiusX,  
											centerY - ARROW_HEAD_SIZE,

											centerX - radiusX,  
											centerY, 

											centerX + ARROW_HEAD_SIZE - radiusX, 
											centerY - ARROW_HEAD_SIZE
									}); 

									head.setStroke(Color.RED);
									head.setFill(Color.RED);
									head.setStrokeWidth(ARROW_STROKE_WIDTH);															

									arrow.setStroke(Color.RED);
									arrow.setFill(null);
									arrow.setStrokeWidth(ARROW_STROKE_WIDTH);

									arrow.setMouseTransparent(true);
									head.setMouseTransparent(true);

									arrows.getChildren().addAll(arrow, head);

									if ( fromIncX > 0)  {
										Line line = new Line(fromWestX, fromWestY, fromWestX + fromIncX, fromWestY);
										line.setMouseTransparent(true);
										line.setStroke(Color.RED);
										line.setFill(null);
										line.setStrokeLineJoin(StrokeLineJoin.ROUND);
										line.setStrokeWidth(ARROW_STROKE_WIDTH);
										arrows.getChildren().add(line); 
									}


									if ( toIncX > 0)  {
										Line line = new Line(toWestX, toWestY, toWestX + toIncX, toWestY);
										line.setMouseTransparent(true);
										line.setStroke(Color.RED);
										line.setFill(null);
										line.setStrokeLineJoin(StrokeLineJoin.ROUND);
										line.setStrokeWidth(ARROW_STROKE_WIDTH);
										arrows.getChildren().add(line); 
									}																	

									break;								

								}  else { // both are in groups 

									double fromWestX = ((ExerciseStepView) fromView).socketWestXInParent.doubleValue();		
									double fromWestY = ((ExerciseStepView) fromView).socketWestYInParent.doubleValue(); 

									double toWestY = ((ExerciseStepView) toView).socketWestYInParent.doubleValue(); 

									double centerX = fromWestX;														
									double centerY = (fromWestY + toWestY) / 2;
									double radiusY = Math.abs(fromWestY - toWestY) / 2;
									double radiusX = Math.min(radiusY / 2.5, 50);  

									Arc arrow = new Arc(centerX, centerY, radiusX, radiusY, 90, 180);

									arrow.setType(ArcType.OPEN);
									arrow.setStroke(Color.RED);
									arrow.setFill(null);
									arrow.setStrokeWidth(ARROW_STROKE_WIDTH);

									Polygon head = new Polygon();

									head.getPoints().addAll(new Double[]{
											centerX - ARROW_HEAD_SIZE - radiusX,  
											centerY - ARROW_HEAD_SIZE,

											centerX - radiusX,  
											centerY, 

											centerX + ARROW_HEAD_SIZE - radiusX, 
											centerY - ARROW_HEAD_SIZE
									}); 

									head.setStroke(Color.RED);
									head.setFill(Color.RED);
									head.setStrokeWidth(ARROW_STROKE_WIDTH);															

									arrow.setStroke(Color.RED);
									arrow.setFill(null);
									arrow.setStrokeWidth(ARROW_STROKE_WIDTH);

									arrows.getChildren().addAll(arrow, head);		

									arrow.setMouseTransparent(true);
									head.setMouseTransparent(true);

									break;		

								}

							}
						}
					}
				}
			}

			//
			// editable successors (blue arrows) 
			// 

			for (StepView fromView : getStepViews()) {

				if ( ! ( fromView instanceof PlaceholderStepView ) ) {

					StepModel from = fromView.getStepModel(); 

					for (StepModel to : from.getEditableSuccessors()) {

						for (StepView toView : getStepViews()) {

							if (to != from && toView.getStepModel() == to) {

								/* 
							Arrow arrow = new Arrow(
								fromView.socketSouthX, fromView.socketSouthY, 
								toView.socketNorthX, toView.socketNorthY); */  

								double centerX = fromView.socketEastX.doubleValue();														
								double centerY = (fromView.socketEastY.doubleValue() + toView.socketEastY.doubleValue()) / 2;
								double radiusY = Math.abs(fromView.socketEastY.doubleValue() - toView.socketEastY.doubleValue()) / 2;
								double radiusX = Math.min(radiusY / 2.5, 50);  

								Arc arrow = new Arc(centerX, centerY, radiusX, radiusY, 270, 180);
								arrow.setType(ArcType.OPEN);
								arrow.setStroke(Color.BLUE);
								arrow.setFill(null);
								arrow.setStrokeWidth(ARROW_STROKE_WIDTH);

								Polygon head = new Polygon();

								head.getPoints().addAll(new Double[]{
										centerX - ARROW_HEAD_SIZE + radiusX,  
										centerY - ARROW_HEAD_SIZE,

										centerX + radiusX,  
										centerY, 

										centerX + ARROW_HEAD_SIZE + radiusX, 
										centerY - ARROW_HEAD_SIZE
								}); 

								head.setStroke(Color.BLUE);
								head.setFill(Color.BLUE);
								head.setStrokeWidth(ARROW_STROKE_WIDTH);

								arrows.getChildren().addAll(arrow, head);							
								break;

							}
						}
					}
				}
			}

		}				

	}                           	    	

}
