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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.StorageAssistant;
import com.sri.tasklearning.ui.core.UndoManager;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepView;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameter;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.util.Callback;

/**
 * The controller in our Model-View-Controller implementation. Provides 
 * essential functionality for editing exercises that operates on both visuals
 * and models.
 */
public final class ExerciseEditController extends EditController {
	
    private static final Logger log = LoggerFactory
            .getLogger(ExerciseEditController.class);
    
    // #define       
    
    public static final boolean ADEPT = true;    

    private ExerciseModel  procModel; 
    private ExerciseView   procView;
    private UndoManager     undoMgr;
    
    private AnnotationPanel annotationPanel;
    
    private final SimpleBooleanProperty unsavedChanges = 
        new SimpleBooleanProperty(false);
    
    private IUndoable undoAtLastSave;       

    public ExerciseEditController(final ExerciseModel model) {
        procModel = model;
        undoMgr = new UndoManager();
        
        undoMgr.registerWatcher(this);        
        procModel.setController(this);
    }
    
    public void setView(ExerciseView view) {
        this.procView = view;
    }
    
    
    public ExerciseModel getModel() {
        return procModel; 
    }
    
    public ExerciseView getView() {
        return procView;
    }
     
    public UndoManager getUndoManager() {
        return undoMgr;
    }
    
    public void updateUndoAtLastSave() {
        undoAtLastSave = undoMgr.peekUndo();
        unsavedChanges.setValue(false); 
    }
    
    public boolean isUnsavedChanges() {
        return unsavedChanges.getValue();
    }
    
    public SimpleBooleanProperty unsavedChangesProperty() {
        return unsavedChanges; 
    }   
    
    //**************************************************************************
    // IUndoWatcher
    //**************************************************************************
    
    public void onUndoCleared() {
        // undo is cleared when a file is opened
        unsavedChanges.setValue(false);
        undoAtLastSave = null;
    }
    
    public void onUndoChanged(IUndoable undo) {
        unsavedChanges.setValue(undoAtLastSave != undoMgr.peekUndo());
        
    }
    
    public void onRedoChanged(IUndoable redo) {
        // Empty
    }    
    
    //**************************************************************************
    // Methods for step-level edit operations
    //**************************************************************************
    
    /**
     * Adds a step to a specified layout at a specified index and returns
     * an appropriate undo but doesn't push it on the undo stack. Don't call
     * this externally unless you know what you're doing. 
     * 
     * @param step the step to add
     * @param layout the step layout to add the step to 
     * @param index the index at which to add the step
     * @return an undo for this operation that has not been added to the undo 
     *         stack
     */
    public IUndoable addStep_impl(            
            final StepView step,
            final StepLayout layout, 
            final int index) {
        
        final StepModel model = step.getStepModel();
        
        layout.getContainerStepModel().addStep(model, index);
        layout.addStepView(step, index);
        
        procModel.updateIndices(); 
        
        return new IUndoable() {
            public boolean undo() {
                deleteStep(step);
                return true;
            }

            public boolean redo() {
                addStep(step, layout, index);
                return true;
            }

            public String getDescription() {
                return "Add step " + (model.getIndex() + 1) + ", \""
                        + model.getName() + "\"";
            }
        };
    }
    
    /**
     * Adds a step to the specified layout at the specified index.
     * 
     * @param step the step to add
     * @param layout the step layout to add the step to 
     * @param index the index at which to add the step
     */    
    public void addStep(
            final StepView step,
            final StepLayout layout, 
            final int index) {
                
        IUndoable undo = addStep_impl(step, layout, index);
       
        procView.getSelectionManager().selectOnly(step);
        
        undoMgr.pushUndo(undo);
   
        validate(true);
    }    

    /**
     * Deletes a step and creates an appropriate undo but doesn't push it on
     * the undo stack. Don't use this externally unless you know what you're
     * doing.
     * 
     * @param step the step to be deleted
     * @return an undo for the delete operation
     */
    public IUndoable deleteStep_impl(final StepView step) {
    	
        final StepModel model = step.getStepModel();
        final ContainerStepModel parent = step.getStepViewContainer().getContainerStepModel();        
        final StepLayout deleteLayout = step.getStepViewContainer().getStepLayout();  
        final int originalIndex = parent.indexOf(model);
        final int overallIndex = model.getIndex();
        
        parent.deleteStep(model);
        
        deleteLayout.deleteStepView(step);

        getModel().updateIndices(); 
        
        return new IUndoable() {
            public String getDescription() {
                return "Delete step " + (overallIndex + 1) + " ("
                        + model.getName() + ")";
            }

            public boolean undo() {

                addStep(step, deleteLayout, originalIndex);
                return true;
            }

            public boolean redo() {
                deleteStep(step);
                return true;
            }
        };
    }
    
    /**
     * Deletes a step from the currently loaded procedure view/model. Creates
     * an appropriate undo.
     * 
     * @param step the step to be deleted
     */
    public void deleteStep(final StepView step) {        
        IUndoable undo = deleteStep_impl(step);
        
        getView().getSelectionManager().selectNone();
        
        undoMgr.pushUndo(undo); 
    
        validate(true);        
    }
    
    /**
     * Deletes a step from the currently loaded procedure view/model. Creates
     * an appropriate undo.
     * 
     * @param step the step to be deleted
     */
    public void deleteStep(final StepModel step) {
        deleteStep(procView.findStepView(step));
    }

    /** 
    * Deletes a list of steps from wherever they happen to be in the procedure.
    * The steps don't need to be contiguous, from the same layout, etc...they
    * can come from anywhere. An undo is created. 
    * 
    * @param steps the list of steps to be deleted
    */
    public void deleteSteps(final List<StepView> steps) {
        if (steps == null || steps.size() == 0)
            return; 
        
        if (steps.size() == 1) {
            deleteStep(steps.get(0));
            return; 
        }
        
        final List<IUndoable> undos = new ArrayList<IUndoable>();
        
        for (StepView step : steps)
            undos.add(deleteStep_impl(step));
        
        CommonView pv = getView();
        pv.getSelectionManager().selectNone();

        undoMgr.pushUndo(new IUndoable() {
            public String getDescription() {
                return "Delete " + steps.size() + " steps";
            }

            public boolean undo() {
                for (IUndoable undo : undos)
                    undo.undo();
                return true;
            }

            public boolean redo() {
                for (IUndoable undo : undos)
                    undo.redo();
                return true;
            }
        });
    
        validate(true);
        
    }      
    
    /**
     * Move a step to a new location in the current procedure. Creates an 
     * appropriate undo operation. This method allows a step to moved within or 
     * among StepLayouts. This method assumes that you've already removed the 
     * StepView from oldLayout because that is the case in step drag/drop, but 
     * could fairly easily be enhanced to remove that assumption.  
     *
     * @param step the step to be moved
     * @param oldLayout the step's current step layout
     * @param newLayout the step's new step layout
     * @param newIndex the index the step should have within newLayout after the 
     *        move
     */
    public void moveStep(
            final StepView step, 
            final StepLayout oldLayout, 
            final StepLayout newLayout, 
            final int newIndex) {
        
        final int oldIndex = oldLayout.getContainerStepModel().getSteps().indexOf(step.getStepModel());
        final int overallIndex = step.getStepModel().getIndex();
        
        // Check for false move
        if (newLayout == oldLayout && newIndex == oldIndex)
            return;  
        
        if (oldLayout.getChildren().contains(step))
            oldLayout.getChildren().remove(step); 
        
        if (newLayout == oldLayout) {
            // Move within same parent. 
            newLayout.getContainerStepModel().moveStep(step.getStepModel(), newIndex);
            newLayout.addStepView(step, newIndex);
        } else {
            // Move between parents
            oldLayout.getContainerStepModel().deleteStep(step.getStepModel());            
            addStep_impl(step, newLayout, newIndex);                            
        }  
        
        procModel.updateIndices(); 
        
        final int overallNewIndex = step.getStepModel().getIndex() + 1;
        undoMgr.pushUndo(new IUndoable() {
            public boolean undo() {
                moveStep(step, newLayout, oldLayout, oldIndex);
                return true;
            }
            public boolean redo() { 
                moveStep(step, oldLayout, newLayout, newIndex);
                return true;
            }
            public String getDescription() { 
                return "Move step " + (overallIndex + 1) + " ("
                        + step.getStepModel().getName() + ") to position "
                        + overallNewIndex;
            }
        });
        
        validate(true);        
    } 


    //**************************************************************************
    // CONSOLIDATED WHOLISTIC OPERATIONS
    //**************************************************************************
    
  
    
    /**
     * Tracks down the view for the given step model, selects it and causes
     * it to scroll in to view
     * 
     * TODO: Move this?
     */
    public boolean selectStep(StepModel step) {
        StepView view = procView.findStepView(step);
        if (view != null) {
        	procView.getScrollPane().scrollIntoView(view);
            procView.getSelectionManager().selectOnly(view);
        }
        return false;
    }
    
    /**
     * Unhighlights any steps that are currently highlighted. Note that 
     * step highlighting is distinct from step selection. 
     */
    @Override
    public void unhighlightSteps() {
        unhighlightSteps(getModel());
    }
    
       
    public void highlightSteps() {
        highlightSteps(getModel());
    }
    
    public void highlightSubtasks() {
        highlightSubtasks(getModel());
    }
    
    
    public void highlightParameters() {
    	for (ExerciseStepParameter ep : procModel.getExerciseStepParameters())  {
			ep.setHighlighted(true);
    	}
    }
        
    public void highlightEnumParameters() {
    	for (ExerciseStepParameter ep : procModel.getExerciseStepParameters()) 
    		if (ep.getPossibleEnumValues().size() > 1)
    			ep.setHighlighted(true);    
    }
    
    public void highlightRangeParameters() {
    	for (ExerciseStepParameter ep : procModel.getExerciseStepParameters()) 
    		if (ep.isRangeParameter()) 
    			ep.setHighlighted(true);    
    }
    
    
    
    public void unhighlightParameters() {
    	for (ExerciseStepParameter ep : procModel.getExerciseStepParameters()) 
			ep.setHighlighted(false);    
    }
    
    /*
     * Helper method for unhighlighting steps
     */
  
    private void unhighlightSteps(final ContainerStepModel container) {
        if (container.isHighlighted())
            container.setHighlighted(false);
        for (StepModel step : container.getSteps()) {
            if (step.isHighlighted())
                step.setHighlighted(false);
            if (step instanceof ContainerStepModel)
                unhighlightSteps((ContainerStepModel)step);
        }
    }
    
    private void highlightSteps(final ContainerStepModel container) {
        if (! container.isHighlighted())
            container.setHighlighted(true);
        for (StepModel step : container.getSteps()) {
            if (! step.isHighlighted())
                step.setHighlighted(true);
            if (step instanceof ContainerStepModel)
                highlightSteps((ContainerStepModel)step);
        }
    }
    
    private void highlightSubtasks(final ContainerStepModel container) {
         for (StepModel step : container.getSteps()) {
             if (step instanceof ContainerStepModel)
            	step.setHighlighted(true);
        }
    }
    
  
    @Override
    public void attemptSave(final boolean allowSaveAs,
    		final boolean forceSaveAs,
    		final Callback<CommonModel, Void> onSuccess,
    		final Scene scene) {

    	final ExerciseModel proc = procModel;

    	if ( !forceSaveAs ) {
    		if (StorageAssistant.saveExerciseFile(proc)) {
    			updateUndoAtLastSave();
    			if (onSuccess != null)
    				onSuccess.call(proc);
    		}
    	} else  {

    		File file =  Utilities.browseForNewExerciseFile(this.getView().getScene().getWindow());   

    		if (file != null) { // not canceled?
    			
    			proc.setFileSource(file);

    			if (StorageAssistant.saveExerciseFile(proc)) {
    				updateUndoAtLastSave();
    				if (onSuccess != null)
    					onSuccess.call(proc);
    			}
    		}

    	}
    }

	public void setAnnotationPanel(AnnotationPanel annotationPanel) {
		this.annotationPanel = annotationPanel; 
	}
	
	public AnnotationPanel getAnnotationPanel() {
		return annotationPanel;
	}

	
	 public void getStepParameterViews() {
		 
	    	for (StepView sv : procView.getSteps()) { 
	    		if (sv instanceof ExerciseStepView ) {
	    			
	    			ExerciseStepView sv1 = (ExerciseStepView) sv; 

	    				
	    		}
	    	}
	    }

}
