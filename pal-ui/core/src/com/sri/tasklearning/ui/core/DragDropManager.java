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

package com.sri.tasklearning.ui.core;

import java.util.ArrayList;
import java.util.List;

import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.common.SignatureModel;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.control.Alert.AlertResult;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.step.ExerciseStepView;
import com.sri.tasklearning.ui.core.step.PlaceholderStepModel;
import com.sri.tasklearning.ui.core.step.PlaceholderStepView;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ParameterModel;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.transform.Transform;
import javafx.util.Callback;

/**
 * Singleton that manages drag-drop operations on steps and items in the 
 * library. 
 * 
 * TODO: JavaFX 2.0 introduced special events for drag/drop that we should
 * probably utilize. However, they weren't introduced until after this code
 * was ported and we didn't want to reimplement/retest at that time.  
 */
public class DragDropManager {

	private static final double MIN_DRAG_DISTANCE = 7;
	private SelectionManager selectionManager;

	private PlaceholderStepView placeholder;
	private CommonView procedureView;    
	private EditController controller;

	private boolean dragging = false;
	private double dragBeginX;
	private double dragBeginY;

	private EventHandler<? super MouseEvent> originalDraggedEvt;
	private EventHandler<? super MouseEvent> originalReleasedEvt;

	private StepLayout dropLayout;
	private int dropIndex;
	private boolean delete = false; 
	private boolean disabled = false;

	private StepView originalStep;
	private int originalIndex;
	private Scene scene;

	// All SelectionManagers share a singleton DragDropManager
	private static DragDropManager instance;

	public static DragDropManager getInstance() {
		if (instance == null) 
			instance = new DragDropManager();

		return instance;
	}

	public void configure(
			final SelectionManager argSelectionManager,
			final CommonView argProcedureView) {

		selectionManager = argSelectionManager;
		procedureView = argProcedureView;
		controller = procedureView.getController();
		controller.getVariableManager();

	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public boolean isDragging() {
		return dragging;
	}

	public void handleDragging(Node source, MouseEvent e) {    	

		if ((disabled) || 
				! ( source instanceof ExerciseStepView ) ) 
			return;

		placeholder = null;         
		dragging = false;
		dropIndex = -1; 
		delete = false; 

		originalStep = (StepView) source; 
		dropLayout = originalStep.getView().getStepLayout();

		originalDraggedEvt  = originalStep.getOnMouseDragged();
		originalReleasedEvt = originalStep.getOnMouseReleased();

		List<StepView> stepViews = procedureView.getStepLayout().getStepViews(); 

		for (int i = 0; i < stepViews.size(); i++) {

			StepView stepView = stepViews.get(i);

			if (stepView == originalStep) {

				originalIndex = i; 

				Transform trans = stepView.getLocalToSceneTransform();

				Point2D from = trans.transform( stepView.getLayoutBounds().getMinX(), stepView.getLayoutBounds().getMinY()); 
				Point2D to   = trans.transform( stepView.getLayoutBounds().getMaxX(), stepView.getLayoutBounds().getMaxY());

				double xf = from.getX(); 
				double xt = to.getX(); 
				double yf = from.getY(); 
				double yt = to.getY();

				dropLayout.deleteStepView(stepView); 		    		

				scene = procedureView.getScene();

				((AnchorPane) scene.getRoot()).getChildren().add(stepView);

				PlaceholderStepModel model = new PlaceholderStepModel();
				placeholder = (PlaceholderStepView) dropLayout.createStepView(model);                                                           

				dropLayout.addStepView(placeholder, i);

				placeholder.setPrefHeight(Math.min(stepView.getHeight(), 180));            	
				placeholder.setMinHeight(Region.USE_PREF_SIZE);


				dragBeginX = e.getSceneX();
				dragBeginY = e.getSceneY();                 

				stepView.setLayoutX(xf);
				stepView.setLayoutY(yf);

				break;
			}
		}


		originalStep.setOnMouseDragged(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent me) {                  	

				double curX = me.getSceneX();
				double curY = me.getSceneY(); 

				double incX = curX - dragBeginX;
				double incY = curY - dragBeginY; 

				if (!dragging
						&& Math.abs(incX) < MIN_DRAG_DISTANCE
						&& Math.abs(incY) < MIN_DRAG_DISTANCE )
					return;                

				dragging = true; 

				originalStep.setTranslateX(curX - dragBeginX );
				originalStep.setTranslateY(curY - dragBeginY );

				originalStep.setCursor(Cursor.DEFAULT);

				ScrollPanePlus sp = procedureView.getScrollPane();
				Point2D scrollPt = sp.sceneToLocal(curX, curY); 

				if (scrollPt.getY() < 15) {               
					sp.scoochUp();

				} else if (scrollPt.getY() > sp.getHeight() - 15) {
					sp.scoochDown();
				}    

				List<StepView> stepViews = procedureView.getStepLayout().getStepViews(); 

				if (scrollPt.getX() < 0 || scrollPt.getX() > sp.getWidth() ) {
					
					delete = true;
					originalStep.setCursor(Cursor.CROSSHAIR);

				} else {

					for (int i = 0; i < stepViews.size(); i++) {

						StepView stepView = stepViews.get(i);

						if (stepView != originalStep) {

							Transform trans = stepView.getLocalToSceneTransform();

							Point2D from = trans.transform( stepView.getBoundsInLocal().getMinX(), stepView.getBoundsInLocal().getMinY()); 
							Point2D to   = trans.transform( stepView.getBoundsInLocal().getMaxX(), stepView.getBoundsInLocal().getMaxY());

							double xf = from.getX(); 
							double xt = to.getX(); 
							double yf = from.getY(); 
							double yt = to.getY(); 

							boolean inside = ( xf <= curX) && (curX <= xt) && (yf <= curY) && (curY <= yt);

							if (inside) {
								originalStep.setCursor(Cursors.OPEN_HAND);

								dropLayout.deleteStepView(placeholder); 
								dropLayout.addStepView(placeholder, i); 
								dropLayout.requestLayout();
								dropIndex = i;

								break;

							}
						}
					}
				}
			}
		}
				);        


		originalStep.setOnMouseReleased(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent me) {

				if (! dragging) {

					int index = dropLayout.indexOf(placeholder);            

					dropLayout.addStepView(originalStep, index);
					dropLayout.deleteStepView(placeholder); 

					originalStep.setLayoutX(0); 
					originalStep.setLayoutY(0); 
					originalStep.setTranslateX(0); 
					originalStep.setTranslateY(0);

					procedureView.requestLayout();

				} else { 

					cleanup(); 
				}
			}
		});

	}


	private boolean removePlaceholder() {

		if (placeholder != null ) {
			StepLayout deleteLayout = placeholder.getStepViewContainer().getStepLayout();

			boolean success = deleteLayout.deleteStepView(placeholder);
			return success;

		}

		return false;

	}    

	void cleanup() {

		originalStep.setOpacity(1);
		originalStep.setCache(false);
		originalStep.setPrefWidth(Region.USE_COMPUTED_SIZE);
		originalStep.setCursor(Cursor.DEFAULT);
		originalStep.setOnMouseDragged(originalDraggedEvt);
		originalStep.setOnMouseReleased(originalReleasedEvt);		


		if (delete) {

			final List<StepView> delSteps = new ArrayList<StepView>();
			final SignatureModel sm = procedureView.getModel()
					.getSignature();
			int publishedResults = 0;
			for (ISelectable sel : selectionManager.getSelectedItems()) {
				StepView sv = (StepView)sel;
				delSteps.add(sv);
				for (ParameterModel pm : sv.getStepModel().getResults())
					if (sm.getResults().contains(pm.getTerm()))
						publishedResults++; 
			}

			Callback<AlertResult, Void> call = new Callback<AlertResult, Void>() {
				public Void call(AlertResult result) {
					if (result == AlertResult.YES) {          						
						removePlaceholder();						
						((AnchorPane) scene.getRoot()).getChildren().remove(originalStep);
						controller.deleteSteps(delSteps);                                
						selectionManager.selectNone();
						procedureView.requestFocus();
					} 
					else {
						// no 
						removePlaceholder();
						originalStep.setTranslateX(0);
						originalStep.setTranslateY(0);
						for (ISelectable sel : selectionManager.getSelectedItems()) {
							int index = ((StepView)sel).getStepViewContainer().getContainerStepModel()
									.indexOf(((StepView) sel).getStepModel());
							dropLayout.addStepView((StepView)sel, index);
						}						
					} 

					return null;

				}
			};               

			String message = 
					getDeletePromptText(delSteps.size(), publishedResults);

			Alert.show("Confirm delete", message, AlertConfig.YES_NO,
					call);

		} else if (dropIndex >= 0) {

			removePlaceholder();

			originalStep.setTranslateX(0);
			originalStep.setTranslateY(0);

			boolean falseMove = (originalIndex == dropIndex);

			if (falseMove) {
				// Not deleting. Add the original step back its original location
				// Placeholder is already gone at this point (works for single & multi)
				for (ISelectable sel : selectionManager.getSelectedItems()) {
					int index = ((StepView)sel).getStepViewContainer().getContainerStepModel()
							.indexOf(((StepView) sel).getStepModel());
					dropLayout.addStepView((StepView)sel, index);
				}
			} else {

				controller.moveStep(originalStep, dropLayout,
						dropLayout, dropIndex);

			}
		}             

		dragging = false;       

	};

	public static String getDeletePromptText(int numSteps, int numResults) {
		String message = "";

		if (numResults > 0 && numSteps > 1) 
			message = numResults + " of the steps you wish "
					+ "to delete contain published results. ";
		else if (numResults > 0)
			message = "This step contains published results. "
					+ "Deleting it will change the structure of "
					+ "this procedure. ";

		message += "Are you sure want to delete " 
				+ (numSteps > 1 ? "these" : "this")
				+ " step" + (numSteps > 1 ? "s?" : "?");

		return message; 
	}

}


