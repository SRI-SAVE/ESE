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
import com.sri.tasklearning.ui.core.DragDropManager;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.ISelectable;
import com.sri.tasklearning.ui.core.SelectionManager;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.layout.TextFlowLayout;
import com.sri.tasklearning.ui.core.term.ParameterView;
import com.sri.tasklearning.ui.core.term.TermView;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

/**
 * Abstract class that defines the common visual elements of steps as visualized
 * in a {@code ProcedureView}. This includes rounded rectangle that represents
 * the step, a step header, a step number lable, a collapsible information panel
 * and a content panel which is defined/populated by subclasses
 */
public abstract class StepViewBasicUI extends StepView {
	    
    protected final SimpleObjectProperty<Paint> borderColor = new SimpleObjectProperty<Paint>(Color.TRANSPARENT);
      
    protected Pane titlePane; // this is the anchor. bind to its properties, the other panes adjust to this!!
    
    protected Rectangle borderRect;    
    protected Rectangle selectionRect;    
    protected Label stepIndexLabel;
   
    protected TextFlowLayout headerText;

    protected StepViewBasicUI(final StepModel argModel, final IStepViewContainer argParent,
            final CommonView argProcView) {

        super(argModel, argParent, argProcView);

        stepModel.highlightedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(final ObservableValue<? extends Boolean> value, final Boolean oldVal,
                    final Boolean newVal) {
           
            	borderColor.set(newVal ? Colors.SelectedVariableBackground : Color.TRANSPARENT);
                
            }
        });
        

        createTitlePane();
        
        createSelectionRect();        
        createBorderRect(); 
        // createStepIndexLabel();
    
        registerEventHandlers();              
	
    }

 
    /* public TermView getTermView(List<String> accessors) {
        // First accessor is the arg name
        String arg = accessors.get(0);
        TermView view = null;

        for (Node n : headerText.getChildren())
            if (n instanceof ParameterView)
                allParams.add((ParameterView) n);

        for (ParameterView pv : allParams) {
            if (pv.getParameterModel().getName().equals(arg)) {
                view = pv.getTermView();
                break;
            }
        }

        return view;
    }*/

    public final void addChildren() {
    	
        getChildren().addAll(selectionRect, titlePane, borderRect);
        
        /* 
        if (!(this instanceof ExerciseStepView) || ((ActionStepModel) this.getStepModel()).getActionStreamEvent() == null)
            getChildren().add(stepIndexLabel); */
    }

    protected Rectangle createSelectionRect() {
        
    	selectionRect = new Rectangle();
        selectionRect.relocate(-(PAD - 1), -(PAD - 1));
        
        selectionRect.setArcHeight(EDGE_ROUNDING * 4);
        selectionRect.setArcWidth(EDGE_ROUNDING * 4);
        
        selectionRect.visibleProperty().bind(selected);
        
        selected.addListener(new ChangeListener<Boolean>() {
            public void changed(final ObservableValue<? extends Boolean> value, final Boolean oldVal,
                    final Boolean newVal) {
           
                if (newVal) {
                	
                	selectionRect.setStroke(new LinearGradient(0.0, 0.0, 1.0, 0.0, true, null, new Stop[] {                
                       new Stop(0.25, Colors.SelectedStepBorder), new Stop(1.0, Colors.SelectedStepBackground) }));
                	
                	selectionRect.setFill(new LinearGradient(0.0, 0.0, 1.0, 0.0, true, null, new Stop[] {
                        new Stop(0.1, Colors.SelectedStepBackground), new Stop(1.0, Colors.SelectedStepLightBackground) }));
                	
                } else {
                	
                	selectionRect.setStroke(Color.TRANSPARENT);                      	
                    selectionRect.setFill(Color.TRANSPARENT); 
                	
                }                                
                
            }
        });        
        

    	selectionRect.setStroke(Color.TRANSPARENT);                      	
        selectionRect.setFill(Color.TRANSPARENT); 
        selectionRect.setOpacity(0.8);
        
        selectionRect.widthProperty().bind(titlePane.widthProperty().add( ( PAD - 1 ) * 2));
        selectionRect.heightProperty().bind(titlePane.heightProperty().add( ( PAD - 1) * 2));
        
        return selectionRect;
    }

    protected Rectangle createBorderRect() {
    	
        borderRect = new Rectangle();
        borderRect.setFill(null);
        borderRect.setStrokeWidth(DEFAULT_BORDER_WIDTH);
        borderRect.strokeProperty().bind(borderColor);
        borderRect.setArcHeight(EDGE_ROUNDING * 2);
        borderRect.setArcWidth(EDGE_ROUNDING * 2);
        
        borderRect.widthProperty().bind(titlePane.widthProperty());
        borderRect.heightProperty().bind(titlePane.heightProperty());
        
        return borderRect;
    }

    protected Label createStepIndexLabel() {
    	    	
        stepIndexLabel = new Label();        
        stepIndexLabel.textProperty().bind(getStepModel().indexProperty().add(1).asString());
        
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


    protected static final double TITLE_PANE_LHS = 40.0;
    protected static final double TITLE_PANE_BUTTON_SIZE = 18.0;
    
    protected Pane createTitlePane() {
    	
      return null; 
    }
    
  
    private static boolean selectedAtStart = false;

    protected void registerEventHandlers() {
    	
        setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {

                me.consume();
                
                SelectionManager selectionManager = commonView.getSelectionManager();
                selectionManager.setMultiSelect(me);

                StepView sv = (StepView) me.getSource();

                boolean currentlySelected = selectedAtStart = sv.isSelected();

                if (sv.isPointInContent(me.getX(), me.getY())) {
                	
                    boolean dragListen = true;

                    // Prevent user from multi-selecting a step from a different
                    // StepLayout
                    // than the currently selected steps.
                    if (selectionManager.isMultiSelecting()) {
                        for (ISelectable sel : selectionManager.getSelectedItems())
                            if (((StepView) sel).getStepViewContainer().getStepLayout() != sv.getStepViewContainer()
                                    .getStepLayout())
                                return;
                    }

                    // Don't allow steps within loops and their sub-steps to be
                    // selected at the same time
                    IStepViewContainer parent = sv.getStepViewContainer();
               
                    if (currentlySelected && selectionManager.isMultiSelecting()) {
                        selectionManager.setSelection(sv, false);
                        dragListen = false; // Don't listen for drag on a step
                                            // just got unselected
                    } else if (currentlySelected) {
                        // Do nothing, let the released event handle it in case
                        // user
                        // is trying to initiate a multi-step drag
                    } else if (!selectionManager.isMultiSelecting()) {
                        selectionManager.selectOnly(sv);
                        // dragListen = false; 
                    } else {
                        selectionManager.setSelection(sv, true);
                    }
                    
                    // Start watching for a drag
                    if (dragListen) {
                        DragDropManager.getInstance().handleDragging(sv, me);
                    }
                    
                } else {
                    if (!selectionManager.isMultiSelecting())
                        selectionManager.selectNone();
                    commonView.startSelectionRect(new Point2D(me.getSceneX(), me.getSceneY()));
                }
            } 
        });
        
        setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
            	
                SelectionManager selectionManager = commonView.getSelectionManager();
                StepView sv = (StepView) e.getSource();
                if (sv.isPointInContent(e.getX(), e.getY())) {
                    if (selectedAtStart && !selectionManager.isMultiSelecting()
                            && !DragDropManager.getInstance().isDragging()) {
                        selectionManager.selectOnly(sv);
                        e.consume();
                    }
                }

               
                commonView.clearSelectionRect();
            }
        });
        
      
    }

    // Properties
    public SimpleObjectProperty<Paint> getBorderColor() {
        return borderColor;
    }

    public Label getStepIndexLabel() {
        return stepIndexLabel;
    }
    
    public int getIndex() {
        return getStepModel().getIndex();
    }
    
	public void updateAnchors() {
		
		Bounds bounds = localToParent( titlePane.localToParent(titlePane.getLayoutBounds()));   							
	
		socketNorthX.setValue( (bounds.getMinX() + bounds.getMaxX()) / 2); 
		socketSouthX.setValue( (bounds.getMinX() + bounds.getMaxX()) / 2); 
	
		socketNorthY.setValue( (bounds.getMinY() - 4 ));  
		socketSouthY.setValue( (bounds.getMinY() + getHeight() - 8 ));
		
		socketEastX.setValue( bounds.getMaxX() ); 
		socketEastY.setValue( (bounds.getMinY() + bounds.getMaxY()) / 2);
		
		socketWestX.setValue( bounds.getMinX() ); 
		socketWestY.setValue( (bounds.getMinY() + bounds.getMaxY()) / 2); 
		
	}; 

    
}
