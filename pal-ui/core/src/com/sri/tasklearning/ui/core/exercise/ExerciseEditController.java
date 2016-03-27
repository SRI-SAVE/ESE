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
import java.util.HashMap;
import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Scene;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.TypeDef;
import com.sri.pal.training.core.exercise.Exercise;
import com.sri.pal.training.core.storage.ExerciseStorage;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.procedure.ProcedureInputsPanel.ProcedureInputRow;
import com.sri.tasklearning.ui.core.procedure.SignatureModel;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;
import com.sri.tasklearning.ui.core.step.LoopModel;
import com.sri.tasklearning.ui.core.step.LoopView;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.CompositeTermModel;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameter;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.SelectionManager;
import com.sri.tasklearning.ui.core.StorageAssistant;
import com.sri.tasklearning.ui.core.UndoManager;
import com.sri.tasklearning.ui.core.Utilities; 
import com.sri.tasklearning.ui.core.VariableManager;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameterView;
import com.sun.javafx.collections.MappingChange.Map;

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
    private static TermModel clipboardTerm = null;

    private ExerciseModel  procModel; 
    private ExerciseView   procView;
    private UndoManager     undoMgr;
    private VariableManager varMgr;
    
    private AnnotationPanel annotationPanel;
    
    private final BackendFacade backend = BackendFacade.getInstance();

    // list of all the steps with issues, sorted by StepModel.index
    private List<StepModel> stepsWithIssues;
    private final SimpleIntegerProperty numStepsWithErrors = 
        new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty numStepsWithWarnings = 
        new SimpleIntegerProperty(0);    
    private final SimpleBooleanProperty unsavedChanges = 
        new SimpleBooleanProperty(false);
    private IUndoable undoAtLastSave;       

    public ExerciseEditController(final ExerciseModel model) {
        procModel = model;
        varMgr  = model.getVariableManager();
        undoMgr = new UndoManager();
        
        undoMgr.registerWatcher(this);        
        procModel.setController(this);
    }
    
    public void setView(ExerciseView view) {
        this.procView = view;
    }
    
    public SimpleIntegerProperty numStepsWithErrorsProperty() {
        return numStepsWithErrors;
    }
    
    public SimpleIntegerProperty numStepsWithWarningsProperty() {
        return numStepsWithWarnings;
    }
    
    public List<StepModel> getStepsWithIssues() {
        return stepsWithIssues;
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
    
    public VariableManager getVariableManager() {
        return varMgr; 
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
    
    //*************************************************************************
    // Methods for changing arguments to parameters and other term-level edits
    //*************************************************************************

    /**
     * Modifies the term argument associated with a ParameterModel.
     * 
     * @param parameter the parameter to modify 
     * @param newArg the new term argument for the parameter 
     * @param step the step to which parameter belongs
     * @param nested if true this method will skip creating an undo
     *        operation and validating the procedure. Intended for when
     *        terms are changed as part of a collection/struct edit,
     *        or if this method is being used in support of another 
     *        controller method that provides its own undo, etc.
     * @return true if the parameters's term was modified, false otherwise. 
     */
    public boolean changeArgument(
            final ParameterModel parameter, 
            final TermModel newArg,
            final StepModel step,
            final boolean nested) {
        
        final TermModel oldArg = parameter.getTerm();
        
        if (oldArg == null || newArg == null || oldArg.equals(newArg))
            return false;                   

        // Do the action
        if (newArg instanceof VariableModel)
            parameter.setTerm(varMgr.manageVariable((VariableModel)newArg));
        else
            parameter.setTerm(newArg);
        
        if (nested)
            return true;
        
        // Calling this here pre-loads the data before it's needed by a tooltip
        if (backend.isDebuggingProcedure())
            backend.lookupDebugValue(newArg);        
                            
        // make the undo/redo actions
        final String oldName = "\"" + oldArg.getDisplayString() + "\"";
        final String newName = "\"" + newArg.getDisplayString() + "\"";

        undoMgr.pushUndo(new IUndoable() {
            public String getDescription() {
                if (!(newArg instanceof CompositeTermModel)
                        && !(oldArg instanceof CompositeTermModel)) {
                    return "Replace " + oldName + " with " + newName
                            + " in step " + (step.getIndex() + 1) + " ("
                            + step.getName() + ")";
                }

                // Otherwise, give a more generic description
                String desc = "Modify the value of '" + parameter.getName()
                        + "' ";
                if (parameter.getOwner() instanceof StepModel)
                    desc += "in step "
                            + (((StepModel) parameter.getOwner()).getIndex() + 1);
                return desc;
            }

            public boolean undo() {
                boolean ret = changeArgument(parameter, oldArg, step, nested);
                selectStep(step);
                return ret;
            }

            public boolean redo() {
                boolean ret = changeArgument(parameter, newArg, step, nested);
                selectStep(step);
                return ret;
            }
        });
     
        validate(true);

        return true;
    }
    
    /*
     * Helper method for creating a new procedure input with a default value
     * derived from an existing argument
     */
    private ProcedureInputVariableModel createNewInput(
            final String name, 
            final TypeDef type,
            final TermModel defaultValue) {
        final ProcedureInputVariableModel newInput = 
                new ProcedureInputVariableModel(name, null);
        newInput.setTypeDef(type);       
        
        // Set a default value for the input based on the argument that is
        // there currently
        if (defaultValue instanceof ConstantValueModel) {
            // set default value to constant's value
            newInput.setDefaultValue(defaultValue.deepCopy());
        } else if (defaultValue instanceof CompositeTermModel) {
            if (!((CompositeTermModel)defaultValue).referencesAnyVariable())
                newInput.setDefaultValue(defaultValue.deepCopy());
        } else if (defaultValue instanceof ProcedureInputVariableModel &&
                   ((ProcedureInputVariableModel) defaultValue)
                        .getDefaultValue() != null) {
            TermModel copy = ((ProcedureInputVariableModel) defaultValue)
                    .getDefaultValue().deepCopy();
            newInput.setDefaultValue(copy);
        }
        
        return newInput;
    }
        
    /**
     * Change's a parameter's argument to a new procedure input.
     * 
     * @param parameter the parameter that should have its argument be turned in
     *        to a procedure input
     * @param step the step that contains the argument
     * @return true if the argument was changed to a procedure input
     */
    public boolean changeArgumentToProcedureInput(
            final ParameterModel parameter, 
            final StepModel step) {
        
        if (parameter == null) 
            return false;
        
        String paramName = parameter.getName();
        
        if (paramName == null)
            paramName = TypeUtilities.getName(parameter.getTypeDef());
        
        final String newVarName = varMgr.createValidName(paramName);      
        final ProcedureInputVariableModel newInput = createNewInput(newVarName,
                parameter.getTypeDef(), parameter.getTerm());
        
        varMgr.manageVariable(newInput);
        
        // make the undo/redo actions
        final TermModel oldVal = parameter.getTerm();
        final String oldName = "\"" + oldVal.getDisplayString() + "\"";
        final String newName = "\"" + newInput.getDisplayString() + "\"";
        
        // Use changeArgument, but don't let it create an undo since we're
        // making our own that will completely remove the input when undone
        if (!changeArgument(parameter, newInput, step, true))
            return false;            

        undoMgr.pushUndo(new IUndoable() {
            public String getDescription() {
                return "Replace " + oldName + " with " + newName + " in step "
                        + (step.getIndex() + 1) + " (" + step.getName()
                        + ")";
            }

            public boolean undo() {
                varMgr.unmanageVariable(newInput);
                boolean ret = changeArgument(parameter, oldVal, step, false);            
                selectStep(step);                
                return ret;
            }

            public boolean redo() {
                varMgr.manageVariable(newInput);  
                boolean ret = changeArgument(parameter, newInput, step, false);            
                selectStep(step);               
                return ret;
            }
        });
               
        validate(true);
        
        return true; 
    }
    
    /**
     * Creates a new procedure input of a given type with an optional default 
     * value, but doesn't make the new input an argument to any parameter. 
     * This edit is needed when the user chooses to 'Ask the user' for a value
     * while they're editing a struct or collection. Since struct/collection 
     * edits are treated as atomic operations that are committed when the 
     * editing dialogs are closed, we don't want to use 
     * changeArgumentToProcedureInput because the undo it creates assumes that
     * the struct/collection edit was committed when that may not be the case.
     * 
     * @param type the type that should be associated with the new input
     * @param defaultValue an optional default value to associate with the input
     * @return the newly created input variable
     */
    public ProcedureInputVariableModel createNewProcedureInput(
            final TypeDef type,
            final TermModel defaultValue) {
        final String newInputName = 
                varMgr.createValidName(TypeUtilities.getName(type));
        final ProcedureInputVariableModel newInput = 
                createNewInput(newInputName, type, defaultValue);        
        
        varMgr.manageVariable(newInput);
        
        undoMgr.pushUndo(new IUndoable() {
                public String getDescription() {
                    return "Create new procedure input '"
                            + newInput.getVariableName() + "'";
                }

                public boolean undo() {
                    varMgr.unmanageVariable(newInput);
                    return true;
                }

                public boolean redo() {
                    varMgr.manageVariable(newInput);
                    return false;
                }
            });

        return newInput;
    }

    /**
     * Renames a variable and creates an appropriate undo. This function does 
     * not trigger validation
     * 
     * @param variable the variable to be renamed
     * @param newName the new name of the variable
     * @return true if the rename succeeded, false otherwise. 
     */
    public boolean renameVariable(
            final VariableModel variable, 
            final String newName) {
        
        final String oldName = variable.getVariableName();        
        boolean succeeded = varMgr.renameVariable(variable, oldName, newName);
        if (succeeded) {
            undoMgr.pushUndo(new IUndoable() {
                public String getDescription() {
                    return "Rename the value \"" + oldName + "\" to \"" + newName + "\"";
                }
                public boolean undo() {
                    return renameVariable(variable, oldName);
                }
                 
                public boolean redo() {
                    return renameVariable(variable, newName);
                }                   
            });
        } else 
            log.error("Failed to rename \"" + oldName + "\" to \"" + newName + "\"");
        
        return succeeded;
    }
    
    public boolean renameReplaceVariable(
            final VariableModel variable,
            final String varToReplace) {
        
        IUndoable undo = renameReplaceVariable_impl(variable, varToReplace);
        
        if (undo == null)
            return false;
        
        undoMgr.pushUndo(undo);
            
        validate(true);
        
        return true; 
    }
    
    public IUndoable renameReplaceVariable_impl(
            final VariableModel variable,
            final String varToReplace) {
        
        final VariableModel other = varMgr.getVariableByName(varToReplace);
        final boolean varManaged = variable == varMgr.getVariableByName(variable.getVariableName());
        
        if (other == null)
            return null;
        
        // Cannot do a rename/replace on two variables that are both bound
        if (variable.isBound() && other.isBound())
            return null;
        
        // Variables must be of the same type for a rename/replace
        if (!variable.getTypeDef().equals(other.getTypeDef()))
            return null;
        
        final VariableModel replacer;
        final VariableModel replacee;
        
        if (variable.isBound()) {
            replacer = variable;
            replacee = other;
        } else {
            replacer = other;
            replacee = variable; 
        }
        
        final List<ParameterModel> replacements = new ArrayList<ParameterModel>();
        
        for (StepModel step : varMgr.findReferencesToVariable(procModel, replacee)) {
            List<ParameterModel> refs = step.findReferencesToVariable(replacee);
            for (ParameterModel ref : refs) {
                replacements.add(ref);
            }
        }
        
        final String oldName = variable.getVariableName();
        
        final Runnable doIt = new Runnable() {
            public void run() {
                for (ParameterModel rep : replacements) {
                    rep.setTerm(replacer);
                }
                varMgr.unmanageVariable(replacee);
                varMgr.unmanageVariable(replacer);
                replacer.setVariableName(varToReplace);
                varMgr.manageVariable(replacer);       
            }
        };
        
        doIt.run();
        
        IUndoable undo = new IUndoable() {
            public String getDescription() {
                return "Rename the value \"" + oldName + "\" to \"" + varToReplace + "\"";
            }
            
            public boolean undo() {
                varMgr.unmanageVariable(replacer);
                variable.setVariableName(oldName);
                other.setVariableName(varToReplace);
                
                if (varManaged)
                    varMgr.manageVariable(variable);
                
                varMgr.manageVariable(other);
                
                for (ParameterModel rep : replacements) {
                    rep.setTerm(replacee);
                }
                
                validate(true);
                return true;
            }
            
            public boolean redo() {
                doIt.run();
                validate(true);
                return true; 
            }
        };
        
        return undo;
    }
    
    /**
     * Clears the default value of a procedure input variable
     * @param input the input to clear the default on
     * @param row the row in which the input lives in the header
     */
    public void clearDefault(
            final ProcedureInputVariableModel input,
            final ProcedureInputRow row) {
        
        final TermModel oldDefault = input.getDefaultValue();
        
        input.setDefaultValue(null);

        row.refreshEditor(false);
        
        IUndoable undo = new IUndoable() {
            public String getDescription() {
                return "Clear default value for input '"
                        + input.getDisplayString() + "'";
            }
            public boolean undo() {
                input.setDefaultValue(oldDefault);
                row.refreshEditor(false);
                return true; 
            }
            public boolean redo() {
                input.setDefaultValue(null);
                row.refreshEditor(false);
                return true;
            }
        };
        
        undoMgr.pushUndo(undo);
    }
   
    
    private static <T> boolean insureMatchingListElements(List<T> a, List<T> b) {
        boolean allElementsFound = true;
        for (T o1 : a) {
            boolean matchFound = false;
            for (T o2 : b)
                if (o1 == o2) {
                    matchFound = true;
                    break;
                }
            allElementsFound &= matchFound;
            if (!allElementsFound) 
                break;
        }
        return allElementsFound;
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
        
        // If this delete was triggered by dragging the step off screen, it 
        // won't be a child of deleteLayout so we shouldn't try to delete it. 
        if (step.getParent() == deleteLayout)
            deleteLayout.deleteStepView(step);
        
        final SignatureModel sm = (SignatureModel) procModel.getSignature();
        final List<VariableModel> oldResults = sm != null ? sm.getResults() : null; 
        final List<VariableModel> newResults = new ArrayList<VariableModel>();
        
        if (oldResults != null)
        for (VariableModel var : oldResults) {
            boolean found = false;
            for (ParameterModel pm : model.getResults())
                if (pm.getTerm().equals(var)) {
                    found = true;
                    break;
                }
            if (!found)
                newResults.add(var);      
        }           
        
        // Check to see if any of the outputs from this step were referenced
        // in loop accumulations and if so remove the loop accumulation
        final List<LoopModel> loops = new ArrayList<LoopModel>();
        final List<TermModel> collects = new ArrayList<TermModel>();
        final List<TermModel> intos = new ArrayList<TermModel>();
        
        StepView vParent = step;   
       
        while (vParent.getStepViewContainer() instanceof LoopView) {
            vParent = (LoopView)vParent.getStepViewContainer();
            LoopModel loop = (LoopModel)vParent.getStepModel();            
            for (ParameterModel pm : model.getResults()) {
                if (pm.getTerm().equals(loop.getCollectTerm())) {
                    loops.add(loop);
                    collects.add(loop.getCollectTerm());
                    intos.add(loop.getIntoTerm());
                    
                    loop.getCollect().setTerm(null);
                    loop.getInto().setTerm(null);
                }
            }
        }
        
        if (oldResults != null && newResults.size() != oldResults.size())
            sm.setResults(newResults);               

        // Any variables bound by this step should be marked as unbound
        for (ParameterModel pm : model.getResults())
            if (pm.getTerm() instanceof VariableModel) 
                ((VariableModel)pm.getTerm()).setBound(false);

        getModel().updateIndices(); 
        
        return new IUndoable() {
            public String getDescription() {
                return "Delete step " + (overallIndex + 1) + " ("
                        + model.getName() + ")";
            }

            public boolean undo() {
                if (newResults.size() != oldResults.size())
                    sm.setResults(oldResults);

                // Replace any accumulation bindings
                int idx = 0;
                for (LoopModel lm : loops) {
                    lm.getCollect().setTerm(collects.get(idx));
                    lm.getInto().setTerm(intos.get(idx));
                    idx++;
                }

                // Mark any output variables as bound
                for (ParameterModel pm : model.getResults())
                    ((VariableModel)pm.getTerm()).setBound(true);

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

    /**
     * Moves multiple steps to a new location. The steps must all originate from
     * the same StepLayout, but do not have to be contiguous. When they are 
     * moved to the new StepLayout, they will become contiguous. An undo is
     * created. 
     *  
     * @param steps the steps to be moved
     * @param oldLayout the step layout the steps originate from
     * @param newLayout the step layout the steps should be moved to
     * @param newIndex the starting index of steps when they are moved to 
     *        newLayout
     */
    public void moveSteps(final List<StepView> steps, 
                          final StepLayout oldLayout,
                          final StepLayout newLayout,
                          final int newIndex) {
        final int[] newIndexes = new int[steps.size()];
        
        int idx = newIndex; 
        for (int i = 0; i < steps.size(); i++)
            newIndexes[i] = idx++;
            
        moveStepsLocal(steps, oldLayout, newLayout, newIndexes);
    }
     
    /*
     * Helper function for moving multiple steps (and undoing the same 
     * operation)
     */
    private void moveStepsLocal(
            final List<StepView> steps, 
            final StepLayout oldLayout,
            final StepLayout newLayout,
            final int[] newIndexes) {
        // Need to save the old indexes for purposes of executing an undo
        final int[] oldIndexes = new int[steps.size()];
        
        // new index value is passed in as if the steps in question have been removed
        // from the display, so make sure that is the case (primarily for redo)...
        for (StepView step : steps)
            if (oldLayout.getChildren().contains(step))
                oldLayout.getChildren().remove(step);
        
        for (int i = 0; i < steps.size(); i++) {
            StepView sv = steps.get(i);
            oldIndexes[i] = oldLayout.getContainerStepModel().indexOf(sv.getStepModel());
        }
        
        for (StepView sv : steps)
            oldLayout.getContainerStepModel().deleteStep(sv.getStepModel());        
                     
        for (int i = 0; i < newIndexes.length; i++) {
            StepView sv = steps.get(i);
            newLayout.getContainerStepModel().addStep(sv.getStepModel(), newIndexes[i]);
            newLayout.addStepView(sv, newIndexes[i]);         
        }           
        
        final SelectionManager mgr = procView.getSelectionManager();
        mgr.selectNone();
        for (StepView sv : steps)
            mgr.setSelection(sv, true);
        
        procModel.updateIndices(); 
        
        undoMgr.pushUndo(new IUndoable() {
            public String getDescription() {
                StringBuffer desc = new StringBuffer("Move multiple steps (");
                for (StepView step : steps)
                    desc.append((step.getStepModel().getIndex() + 1) + ", ");
                String ret = desc.substring(0, desc.length() - 2) + ")";
                return ret;
            }

            public boolean undo() {
                moveStepsLocal(steps, newLayout, oldLayout, oldIndexes);
                return true;
            }

            public boolean redo() {
                moveStepsLocal(steps, oldLayout, newLayout, newIndexes);
                return true;
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
    	for (ExerciseStepParameter ep : procModel.getExerciseStepParameters()) 
			ep.setHighlighted(true);    
    }
        
    public void highlightEnumParameters() {
    	for (ExerciseStepParameter ep : procModel.getExerciseStepParameters()) 
    		if (ep.getPossibleValues().size() > 1)
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
    
  
    
    public static void setClipboardTerm(final TermModel term) {
        if (term != null)
            clipboardTerm = term.deepCopy();
        else
            clipboardTerm = null;        
    }
    
    public static TermModel getClipboardTerm() {
        return clipboardTerm;
    }
    
    /**
     * Highlights a variable. This causes all appearances of that variable to
     * become bright orange. Some references to the variable may be nested
     * inside of a step's details, or inside of a structure or collection. 
     * Because of this, highlighting a variable also causes all steps that 
     * reference that variable (no matter how indirectly) to become highlighted
     * as well so that the user knows there's a reference somewhere in that step
     * 
     * @param var the variable to be highlighted
     */
    public void highlightVariable(final VariableModel var) {
        for (VariableModel v : varMgr.getVariables())
            v.setHighlighted(false);
    
        var.setHighlighted(true);
        
        unhighlightSteps();
                             
        List<StepModel> references = 
            varMgr.findReferencesToVariable(getModel(), var);
        
        for (StepModel step : references)
            step.setHighlighted(true);
    }
    
    /**
     * Unhighlights a variable. This imples unhighlighting any steps that 
     * reference the variable as well.
     * 
     * @param var the variable to be unhighlighted. 
     */
    public void unhighlightVariable(final VariableModel var) {
        unhighlightSteps();
        var.setHighlighted(false);
    }
   
    @Override
    public void attemptSave(final boolean allowSaveAs,
    		final boolean forceSaveAs,
    		final Callback<CommonModel, Void> onSuccess,
    		final Scene scene) {

    	final ExerciseModel proc = procModel;
    	final File source = procModel.getFileSource(); 

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


}
