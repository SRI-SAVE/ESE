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
import com.sri.tasklearning.ui.core.step.ExerciseSubtaskView;
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

	private static final double MIN_DRAG_DISTANCE = 12;
	private SelectionManager selectionManager;

	private PlaceholderStepView placeholder;
	private CommonView procedureView;    
	private EditController controller;

	private boolean dragging = false;
	private double dragBeginX;
	private double dragBeginY;

	private EventHandler<? super MouseEvent> originalDraggedEvt;
	private EventHandler<? super MouseEvent> originalReleasedEvt;

	private StepLayout topLayout;	
	
	//
	//
	//
	
	private StepLayout sourceSubLayout;
	private ExerciseSubtaskView sourceSubGroup;
	
	private int sourceIndex;
	private int sourceSubIndex;
	
	//
	//
	//
	
	private StepLayout targetSubLayout;
	private ExerciseSubtaskView targetSubGroup;
	
	private int targetIndex;
	private int targetSubIndex;
	
	//
	//
	//
	
	private boolean delete = false; 
	private boolean disabled = false;

	private StepView originalStep;
	
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

		if (disabled)
			return;
				
		placeholder = null;         
		dragging = false;
		
		targetIndex = -1;
		targetSubIndex = -1;
		
		sourceIndex = -1; 
		sourceSubIndex = -1; 
		
		delete = false; 

		originalStep = (StepView) source; 
		
		topLayout = originalStep.getView().getStepLayout();
		
		sourceSubLayout = null;
		sourceSubGroup = null; 
		targetSubLayout = null; 
		
		originalDraggedEvt  = originalStep.getOnMouseDragged();
		originalReleasedEvt = originalStep.getOnMouseReleased();

		//
		// identify coordinates - group and subgroup - and corresponding step layouts of step being dragged 
		// 
		
		List<StepView> stepViews = procedureView.getStepLayout().getStepViews(); 
		
		for (int i = 0; i < stepViews.size(); i++) {

			StepView stepView = stepViews.get(i);

			if (stepView == originalStep) {

				sourceIndex = i; 

				Transform trans = stepView.getLocalToSceneTransform();

				Point2D from = trans.transform( stepView.getLayoutBounds().getMinX(), stepView.getLayoutBounds().getMinY()); 
				Point2D to   = trans.transform( stepView.getLayoutBounds().getMaxX(), stepView.getLayoutBounds().getMaxY());

				double xf = from.getX(); 
				double xt = to.getX(); 
				double yf = from.getY(); 
				double yt = to.getY();

				topLayout.deleteStepView(stepView); 					

				scene = procedureView.getScene();

				((AnchorPane) scene.getRoot()).getChildren().add(stepView);

				PlaceholderStepModel model = new PlaceholderStepModel();

				placeholder = (PlaceholderStepView) topLayout.createStepView(model);      

				placeholder.setMinHeight(stepView.getHeight());
				placeholder.setMaxHeight(stepView.getHeight());

				topLayout.addStepView(placeholder, i);				

				dragBeginX = e.getSceneX();
				dragBeginY = e.getSceneY();                 

				stepView.setLayoutX(xf);
				stepView.setLayoutY(yf);

				break;

			} else if ( stepView instanceof ExerciseSubtaskView ) {

				List<StepView> stepViews1 = ((ExerciseSubtaskView) stepView).getStepLayout().getStepViews(); 
				
				boolean found = false; 
				
				for (int ii = 0; ii < stepViews1.size(); ii++) {

					StepView stepView1  = stepViews1.get(ii);

					if (stepView1 == originalStep) {

						sourceSubLayout = ((ExerciseSubtaskView) stepView).getStepLayout();
						sourceSubGroup =  ((ExerciseSubtaskView) stepView);
						
						sourceIndex = i; 
						sourceSubIndex = ii; 

						Transform trans = stepView1.getLocalToSceneTransform();

						Point2D from = trans.transform( stepView1.getLayoutBounds().getMinX(), stepView1.getLayoutBounds().getMinY()); 
						Point2D to   = trans.transform( stepView1.getLayoutBounds().getMaxX(), stepView1.getLayoutBounds().getMaxY());

						double xf = from.getX(); 
						double xt = to.getX(); 
						double yf = from.getY(); 
						double yt = to.getY();

						sourceSubLayout.deleteStepView(stepView1); 		

						scene = procedureView.getScene();

						((AnchorPane) scene.getRoot()).getChildren().add(stepView1);

						PlaceholderStepModel model = new PlaceholderStepModel();

						placeholder = (PlaceholderStepView) sourceSubLayout.createStepView(model);      

						placeholder.setMinHeight(stepView1.getHeight());
						placeholder.setMaxHeight(stepView1.getHeight());

						sourceSubLayout.addStepView(placeholder, ii);				

						dragBeginX = e.getSceneX();
						dragBeginY = e.getSceneY();                 

						stepView1.setLayoutX(xf);
						stepView1.setLayoutY(yf);

						found = true; 
						break;

					}				
				}
				
				if (found) 
					break; 
				
			}
		}
		
		//
		//
		//
		
		if (placeholder == null) 
			return; 

		//
		//
		// 

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

				//
				//
				//

				if (scrollPt.getY() < 15) {     
					sp.scoochUp();				
				} else if (scrollPt.getY() > sp.getHeight() - 15) {
					sp.scoochDown();
				}    

				//
				//
				//

				List<StepView> stepViews = procedureView.getStepLayout().getStepViews(); 

				targetIndex = -1; 
				targetSubIndex = -1; 				
				targetSubLayout = null;

				//
				//
				// 

				if (scrollPt.getX() < 0 || scrollPt.getX() > sp.getWidth() ) {

					delete = true;
					originalStep.setCursor(Cursor.CROSSHAIR);

				} else {

					boolean found = false; 
					double lastYt = 0;  

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

							if ( true || ( xf <= curX) && (curX <= xt)) {

								xf = from.getX(); 
								xt = to.getX(); 
								yf = from.getY(); 
								yt = to.getY(); 

								if ( true || ( xf <= curX) && (curX <= xt) ) {

									if ( curY < yf && i == 0) {
										found = true; 

									} else if ( yf <= curY && curY <= yt ) {
										found = true; 

									} else if ( curY <= yf && lastYt <= curY ) {
										found = true; 

									} else if ( curY > yt && i == stepViews.size() -1 ) {
										found = true; 
									} 								
								}														

								lastYt = yt;

								if (found ) {

									targetIndex = i; 									

									if (stepView instanceof ExerciseSubtaskView &&  ! ( originalStep instanceof ExerciseSubtaskView) ) {

										// find index within group 

										ExerciseSubtaskView group = (ExerciseSubtaskView) stepView; 

										targetSubGroup =  ((ExerciseSubtaskView) stepView);
										
										targetSubLayout = group.getStepLayout();		

										int ii = 0; 
										boolean found1 = false;  
										boolean found2 = false;  

										double lastYt1 = 0;  

										for (StepView subStepView : group.getStepViews()) {											

											Transform trans1 = subStepView.getLocalToSceneTransform();

											Point2D fromSub = trans1.transform( subStepView.getBoundsInLocal().getMinX(), subStepView.getBoundsInLocal().getMinY()); 
											Point2D toSub   = trans1.transform( subStepView.getBoundsInLocal().getMaxX(), subStepView.getBoundsInLocal().getMaxY());

											double xfSub = fromSub.getX(); 
											double xtSub = toSub.getX(); 
											double yfSub = fromSub.getY(); 
											double ytSub = toSub.getY(); 

											if (true || ( xfSub <= curX) && (curX <= xtSub) ) {

												if ( curY < yfSub ) {

													if ( curY < yf && sourceSubIndex != -1 ) {

														found2 = true; 													
														break;

													} else {		

														found1 = true; 
														break;
													}

												} else if ( yfSub <= curY && curY <= ytSub ) {
													found1 = true; 
													break; 												
												} else if ( curY <= yf && lastYt1 <= curY ) {
													found1 = true; 
													break; 
												}											
											}

											lastYt1 = ytSub; 										
											ii++;										

										}

										if (found1 ) {

											originalStep.setCursor(Cursors.OPEN_HAND);										


											targetIndex = i;
											targetSubIndex = ii; 		

											removePlaceholder();

											targetSubLayout.addStepView(placeholder, ii); 																
											targetSubLayout.requestLayout();		
											topLayout.requestLayout();		

										} else if (found2 ) {

										}


									} else {

										originalStep.setCursor(Cursors.OPEN_HAND);

										targetIndex = i;
										targetSubIndex = -1; 

										removePlaceholder();

										topLayout.addStepView(placeholder, i); 																
										topLayout.requestLayout();

									} 

									// quit outer loop 

									break;

								}
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
						
					restoreOriginalPosition(); 
					
					cleanup(); 

				} else { 
					
					cleanup(); 
				}
			}
		});

	}

	private void restoreOriginalPosition() {

		removePlaceholder(); 
		
		if ( sourceSubIndex != -1 ) 
			// add back to original sub group 						
			sourceSubLayout.addStepView(originalStep, sourceSubIndex);
		else 
			topLayout.addStepView(originalStep, sourceIndex);	
		
	}	

	private void removePlaceholder() {

		if (placeholder != null ) {
			
			if (sourceSubLayout != null) {
				sourceSubLayout.deleteStepView(placeholder); 					
				sourceSubLayout.requestLayout();
			}
			
			if (targetSubLayout != null) {
				targetSubLayout.deleteStepView(placeholder);
				targetSubLayout.requestLayout();
			}
			
			topLayout.deleteStepView(placeholder); 														
			topLayout.requestLayout();
		
		}

	}    

	void cleanup() {

		originalStep.setOpacity(1);
		originalStep.setCache(false);
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
							topLayout.addStepView((StepView)sel, index);
						}						
					} 

					return null;

				}
			};               

			String message = 
					getDeletePromptText(delSteps.size(), publishedResults);

			Alert.show("Confirm delete", message, AlertConfig.YES_NO,
					call);

		} else if (targetIndex >= 0) {

			originalStep.setTranslateX(0);
			originalStep.setTranslateY(0);
			
			boolean falseMove = false;
			
			if ( sourceSubIndex == -1 ) 
				// source was top level 				
				if ( targetSubIndex == -1 )					
					// target is top level 
					falseMove = sourceIndex == targetIndex; 					 
				else 					
					// target is subgroup 					
					falseMove = false; 			
			else 				
				// source was some subgroup 				
				if  ( targetSubIndex == -1 )  
					falseMove = false; 					
				else 		
					// target is (potentially same) subgroup 					
					falseMove = ( sourceIndex == targetIndex) && ( sourceSubIndex == targetSubIndex);  
					
			if ( falseMove ) {

				restoreOriginalPosition(); 

			} else {

				removePlaceholder(); 

				if ( sourceSubIndex == -1 ) {

					// source was top level 

					if ( targetSubIndex == -1 ) {

						// target is top level 

						if (! topLayout.stepCanBeMovedToPosition(originalStep, targetIndex))    

							showOrderConstraintViolationMessage();

						else

							controller.moveStep(originalStep, topLayout, topLayout, targetIndex);							 

					} else {

						// target is subgroup

						if ( originalStep.getStepModel().mustPrecede( targetSubGroup.getStepModel()) ||
								targetSubGroup.getStepModel().mustPrecede( originalStep.getStepModel()) ) 

							showOrderConstraintViolationMessage();

						else

							controller.moveStep(originalStep, topLayout, targetSubLayout, targetSubIndex);		

					}

				} else {

					// source was some subgroup 

					if  ( targetSubIndex == -1 ) { 

						// target is top level 

						if ( ! topLayout.stepCanBeMovedToPosition(originalStep, targetIndex))

							showOrderConstraintViolationMessage();

						else {

							if ( sourceSubLayout.getStepViews().size() == 0) {

								// last element of group moved out? dissolve the group	

								controller.deleteStep(sourceSubGroup);

								if (sourceIndex < targetIndex )
									targetIndex--; 

								controller.addStep(originalStep, topLayout, targetIndex);

							} else 

								controller.moveStep(originalStep, sourceSubLayout, topLayout, targetIndex);
						}

					} else {

						// target is (potentially same) subgroup

						if ( originalStep.getStepModel().mustPrecede( targetSubGroup.getStepModel()) ||
								targetSubGroup.getStepModel().mustPrecede( originalStep.getStepModel()) ) 

							showOrderConstraintViolationMessage();

						else {

							if ( sourceSubLayout.getStepViews().size() == 0) {

								// last element of group moved out? dissolve the group							

								controller.deleteStep(sourceSubGroup);							

								if (sourceIndex < targetIndex )
									targetIndex--; 

								controller.addStep(originalStep, targetSubLayout, targetSubIndex);

							} else 

								controller.moveStep(originalStep, sourceSubLayout, targetSubLayout, targetSubIndex);

						}
					}								
				}

				if (sourceSubLayout != null) {									
					sourceSubLayout.requestLayout();
				}

				if (targetSubLayout != null) {
					targetSubLayout.requestLayout();
				}

				topLayout.requestLayout();

			}
		}             

		dragging = false;       

	};

	private void showOrderConstraintViolationMessage() {
	
		Alert.show("Move not permitted", "Moving this step here would violate a required ordering.", AlertConfig.OK, null);
		restoreOriginalPosition(); 

	}
	
	
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

