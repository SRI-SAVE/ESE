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

package com.sri.tasklearning.ui.core.step.dialog;

import javafx.scene.image.Image;

import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.PalUiException;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.step.ActionStepModel;
import com.sri.tasklearning.ui.core.step.IStepViewContainer;
import com.sri.tasklearning.ui.core.step.LoopModel;
import com.sri.tasklearning.ui.core.step.LoopView;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.validation.EditorIssue;

/**
 * Provides static methods for creating instances of DialogStepView at specified
 * locations in the currently loaded procedure. Each DialogStepView is associated
 * with a 'parent' StepView that is being configured in some way by the 
 * DialogStepView. 
 */
public final class DialogManager {
    private DialogManager() {}
    
    public static DialogStepView showAddDialog(final StepView theStep) {
        final EditController controller = 
                theStep.getView().getController();

        if (theStep != null) {
            StepModel model = theStep.getStepModel();
            if (model instanceof ActionStepModel) {
                // Add a regular "do" step, only show the dialog if there are inputs to configure
                if (model.getInputs().size() > 0) {
                	
                    final DialogInputsPanel dialoginputspanel;
                    try {
                        dialoginputspanel = new DialogInputsPanel(theStep);                    
                    } catch (PalUiException e) {
                        Alert.show("Cannot add step here", 
                                   e.getMessage(),
                                   Alert.AlertConfig.OK, null);
                         controller.deleteStep_impl(theStep);
                        return null; 
                    }
                    final DialogStepView dialogView = new DialogStepView(
                            theStep, dialoginputspanel, true,
                            Utilities.getImage(model.getIconPath())); 
                    dialogView.setOnOkayClicked(new Runnable() {
                        @Override
                        public void run() {
                            IUndoable undo = dialoginputspanel.writeChanges();
                            pushAddStepUndo(theStep, undo);
                            dialogView.handleClose(false, false);
                        }
                    });
                    insertDialogAfterStep(dialogView, theStep);
                    dialoginputspanel.layout();
                    return dialogView;
                } else {
                    // if there's nothing to set, accept the step as-is and
                    // setup the undo
                    pushAddStepUndo(theStep, null);
                    return null;
                }
            } else if (model instanceof LoopModel) {
                // ADD A LOOP
                final LoopConfigDialogPanel panel;
                try {
                    panel = new LoopConfigDialogPanel((LoopView)theStep, false);                 
                } catch (PalUiException e) {
                    Alert.show("Cannot add repeat step here", 
                               e.getMessage(),
                               Alert.AlertConfig.OK, null);
                     controller.deleteStep_impl(theStep);
                    return null; 
                }

                final DialogStepView dialogView = new DialogStepView(theStep,
                        panel, true, Utilities.getImage(model.getIconPath()));
                dialogView.setOnOkayClicked(new Runnable() {
                    @Override
                    public void run() {
                        IUndoable undo = panel.writeChanges();
                        pushAddStepUndo(theStep, undo);
                        dialogView.handleClose(false, false);                        
                    }
                });
                insertDialogAfterStep(dialogView, theStep);
                return dialogView;
            } 
        } 
        
        return null;
    }

    public static DialogStepView showEditDialog(final StepView theStep) {
        final EditController controller = 
               theStep.getView().getController();

        if (theStep != null) {
            StepModel model = theStep.getStepModel();
            if (model instanceof ActionStepModel) {
                return null;
            } else if (model instanceof LoopModel) {
                final LoopConfigDialogPanel panel;
                try {
                    panel = new LoopConfigDialogPanel((LoopView)theStep, true);                
                } catch (PalUiException e) {
                     controller.deleteStep_impl(theStep);
                     return null; 
                }       
                final DialogStepView dialogView = new DialogStepView(theStep, panel, false, Utilities.getImage(model.getIconPath()));
                dialogView.setOnOkayClicked(new Runnable() {
                    @Override
                    public void run() {
                        IUndoable undo;
                        
                        if ((undo = panel.writeChanges()) != null)
                            controller.getUndoManager().pushUndo(undo);
                        
                        dialogView.handleClose(false, false);
                    }
                });
                insertDialogAfterStep(dialogView, theStep);
                return dialogView;
            } 
        } 
            
        return null;
    }

    public static DialogStepView showErrorDialog(StepView theStep, EditorIssue issue) {
        if (theStep != null && issue != null) {
            final Image icon = issue.isError() ? Utilities.getImage("error.png") : Utilities.getImage("warning.png");
            final ErrorCorrectionPanel errorcorrectionpanel = new ErrorCorrectionPanel(theStep, issue);
            final DialogStepView dialogView = new DialogStepView(theStep, errorcorrectionpanel, false, icon);
            dialogView.setOkButtonText("Fix");
            // show the error icon
            dialogView.setOnOkayClicked(new Runnable() {
                    @Override
                    public void run() {
                        // In this case we close before writing the changes
                        // because the presence of the dialog stepview can 
                        // throw off some of the repair operations
                        dialogView.handleClose(false, false);
                        errorcorrectionpanel.writeChanges();
                    }
                });

            insertDialogAfterStep(dialogView, theStep);
            return dialogView;
        } else 
            return null;
    }

    private static void insertDialogAfterStep(DialogStepView dialogView, StepView theStep) {
        StepLayout layout;
        
        if (theStep instanceof IStepViewContainer) {
            layout = ((IStepViewContainer)theStep).getStepLayout();
            layout.addStepView(dialogView, 0);
        }
        else {       
            layout = theStep.getStepViewContainer().getStepLayout();
            layout.addStepView(dialogView, layout.indexOf(theStep)+1);
        }
        
        theStep.getView().layout();
        theStep.getView().getScrollPane().scrollIntoView(dialogView);
    }

    private static void pushAddStepUndo(
            final StepView addedStep,
            final IUndoable nestedUndo) {
        final EditController controller = addedStep.getView().getController();
        
        // make the undo/redo actions
        final IStepViewContainer newParent = addedStep.getStepViewContainer();
        final StepModel model = addedStep.getStepModel();
        final int newIndex = newParent.getContainerStepModel().indexOf(model);        

        final IUndoable undoAction = new IUndoable() {
            public String getDescription() {
                return "Add step " + (model.getIndex() + 1) + ", \""
                        + model.getName() + "\"";
            }

            public boolean undo() {
                if (nestedUndo != null)
                    nestedUndo.undo();
                
                controller.deleteStep(addedStep);
                return true; 
            }

            public boolean redo() {
                if (nestedUndo != null)
                    nestedUndo.redo();
                
                controller.addStep(addedStep, newParent.getStepLayout(), newIndex);
                return true;
            }
        };

        controller.getUndoManager().pushUndo(undoAction);
    }
}
