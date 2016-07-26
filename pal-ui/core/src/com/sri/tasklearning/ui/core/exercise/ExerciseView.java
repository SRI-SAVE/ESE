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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.SelectionManager;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.step.IStepViewContainer;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameter;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameterView;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Top-level view for a given {@link ExerciseModel}. 
 */
public class ExerciseView extends CommonView implements IStepViewContainer {

	private static final int SCROLLPANE_HEIGHT = 2200;

	private final ExerciseModel exerciseModel;
	private final ExerciseEditController controller; 
	private final SelectionManager selectionMgr;
	
	private final StepLayout stepLayout;
	private ScrollPanePlus scrollPane;
	
	private VBox vbox = new VBox();
	private HBox hbox = new HBox();    

	private Map<ExerciseStepParameter, ExerciseStepParameterView> allTermViews = new HashMap<ExerciseStepParameter, ExerciseStepParameterView>(); 

	public interface ExerciseOpener {
		public void open(ExerciseModel pm);
		public void close(ExerciseModel pm);
	}   

	private final ExerciseOpener opener;      

	private final ReadOnlyBooleanWrapper readOnly = new ReadOnlyBooleanWrapper(false);

	public ExerciseView(final ExerciseModel argProcModel,
			final ExerciseEditController controller, 
			final ScrollPanePlus argScrollPane, 
			// final boolean headerAndFooter, 
			final ExerciseOpener opener) {

		this.exerciseModel = argProcModel;
		this.opener = opener;
		this.controller = controller;

		selectionMgr = new SelectionManager(this);

		getStyleClass().add("exercise-view");

		scrollPane = argScrollPane;

		if (scrollPane != null)
			scrollPane.widthProperty().addListener(new ChangeListener<Number>() {
				public void changed(
						final ObservableValue<? extends Number> value,
						final Number oldVal,
						final Number newVal) {

					ExerciseView.this.requestLayout();
				}
			});

		//setMinHeight(Region.USE_PREF_SIZE);
		//setMaxHeight(Region.USE_PREF_SIZE);                     

		registerEvents();  

		stepLayout = new StepLayout(exerciseModel, this, this);
		stepLayout.setMinHeight(SCROLLPANE_HEIGHT); 

		if (exerciseModel.getOriginalExerciseModel() == null && exerciseModel.getReadOnlyProperty().getValue() ) {
			vbox.setStyle("-fx-background-color: -pal-SystemDarkGray;");			
		} else {
			vbox.setStyle("-fx-background-color: white;");
		}

		vbox.getChildren().add(stepLayout);
		hbox.getChildren().addAll(vbox);
		vbox.setAlignment(Pos.CENTER);
	
		getChildren().addAll(hbox, selectionRect);

	}

	@Override
	public void prepareToClose() {
		//header.prepareToClose();
	}

	@Override 
	protected void layoutChildren() 
	{
		
		double scrollWidth = scrollPane.getWidth() - 20; 
		double computedWidth = vbox.minWidth(-1);
		double width = scrollWidth > computedWidth ? scrollWidth : computedWidth;             

		vbox.setPrefWidth(width);
		setPrefWidth(width);
		
		super.layoutChildren();

	}

	// Getters/Setters

	@Override
	public ExerciseModel getModel() {
		return exerciseModel;
	}

	@Override
	public SelectionManager getSelectionManager() {
		return selectionMgr;    
	}

	@Override
	public ExerciseEditController getController() {
		return controller;
	}

	// IContainerStepView 
	@Override
	public StepView findStepView(StepModel step) {
		return stepLayout.findStepView(step);
	}

	@Override
	public ExerciseModel getContainerStepModel() {
		return exerciseModel; 
	}

	@Override
	public StepLayout getStepLayout() {
		return this.stepLayout;
	}    


	@Override
	public ScrollPanePlus getScrollPane() {
		return scrollPane; 
	}

	@Override
	public boolean isReadOnly() {
		return readOnly.getValue();
	}

	@Override
	public void setReadOnly(boolean ro) {
		readOnly.setValue(ro);
	}

	@Override
	public ReadOnlyBooleanProperty readOnlyProperty() {
		return readOnly.getReadOnlyProperty(); 
	}    


	private void registerEvents() {         
		setOnMousePressed(new EventHandler<MouseEvent>() {            
			public void handle(MouseEvent e) {
				e.consume();
				selectionMgr.setMultiSelect(e);
				if (!selectionMgr.isMultiSelecting())
					selectionMgr.selectNone();
				if (!readOnly.getValue() && !isDisabled())
					startSelectionRect(new Point2D(e.getSceneX(), e.getSceneY()));                
			}            
		});
		setOnMouseReleased(new EventHandler<MouseEvent>() {            
			public void handle(MouseEvent e) {
				selectionMgr.setMultiSelect(e);
				e.consume();
				if (!readOnly.getValue() && !isDisabled())
					clearSelectionRect();                
			}            
		});
		setOnMouseDragged(new EventHandler<MouseEvent>() {            
			public void handle(MouseEvent e) {
				selectionMgr.setMultiSelect(e);
				e.consume();
				if (!readOnly.getValue() && !isDisabled())
					moveSelectionRect(new Point2D(e.getSceneX(), e.getSceneY()));               
			}            
		});
	}      

	// *********************** Drag-selection rectangle ***********************

	// variables for tracking the mouse interaction
	private double downX = 0.0;
	private double downY = 0.0;
	private boolean mouseDown = false;

	// support a drag rectangle for selection
	// ie, click-and-drag from an empty spot to create a selection rectangle
	private final Rectangle selectionRect; {
		selectionRect = new Rectangle(30.0, 35.0, Color.color(Colors.Selection.getRed(), Colors.Selection.getGreen(), Colors.Selection.getBlue(), 0.1));
		selectionRect.setStroke(Colors.Selection);
		selectionRect.setOpacity(0.7);
		selectionRect.setVisible(false);
	}

	// set the selection rect variables
	public final boolean startSelectionRect(Point2D pt) {
		Point2D point = sceneToLocal(pt);
		downX = point.getX();
		downY = point.getY();
		return mouseDown = true;
	}

	// turn off the selection rect
	public boolean clearSelectionRect() {
		selectionRect.setVisible(false);
		return mouseDown = false;
	}

	// function controlling the drag behavior
	public void moveSelectionRect(Point2D pt) {

		Point2D point = sceneToLocal(pt);
		if (mouseDown && point.getX() >= 0 && point.getY() >= 0) {
			if (point.getX() < downX) {
				selectionRect.setX(point.getX());
				selectionRect.setWidth(downX - point.getX());
			} else {
				selectionRect.setX(downX);
				selectionRect.setWidth(point.getX() - downX);
			}

			if (point.getY() < downY) {
				selectionRect.setY(point.getY());
				selectionRect.setHeight(downY - point.getY());
			} else {
				selectionRect.setY(downY);
				selectionRect.setHeight(point.getY() - downY);
			}
			if (!selectionRect.isVisible())
				selectionRect.setVisible(true);  

			loopOverSteps(stepLayout.getStepViews());
		}
	}


	public void loopOverSteps(List<StepView> steps) {
		final Bounds selectionInScene = selectionRect.localToScene(selectionRect.getLayoutBounds());

		boolean status =  selectionMgr.isCommandInvokationDisabled();  
		selectionMgr.setCommandInvokationDisabled(true);

		for (StepView s : steps) {
			final javafx.geometry.Bounds selectionInStepLocal = s.sceneToLocal(selectionInScene);
			if (s.intersectsContent(selectionInStepLocal)) {
				boolean selectable = true;

				IStepViewContainer parent = s.getStepViewContainer();

				if (selectable) {
					selectionMgr.setSelection(s, true);

				} else
					selectionMgr.setSelection(s, false);

			} else {
				if (!selectionMgr.isMultiSelecting())
					selectionMgr.setSelection(s, false);

			}
		}

		selectionMgr.setCommandInvokationDisabled(status);
	}
	

	//
	//
	// 
	
	public ExerciseStepParameterView findExerciseTermView(ExerciseStepParameter param) {

		return  allTermViews.get(param);

	}

	public void registerTermView(ExerciseStepParameter argModel, ExerciseStepParameterView exerciseStepParameterView) {
		
		allTermViews.put(argModel, exerciseStepParameterView);
		
	}
	
}
