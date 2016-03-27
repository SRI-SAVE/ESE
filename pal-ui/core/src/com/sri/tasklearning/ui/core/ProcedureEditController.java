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
import java.util.Set;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.util.repair.RepairOperation;
import com.sri.ai.lumen.util.repair.RepairSuggestion;
import com.sri.pal.CollectionTypeDef;
import com.sri.pal.ListDef;
import com.sri.pal.StructDef;
import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.control.Alert.AlertResult;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.exercise.ExerciseView;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.library.SaveProcedureDialog;
import com.sri.tasklearning.ui.core.procedure.ProcedureInputsPanel.ProcedureInputRow;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.procedure.SignatureModel;
import com.sri.tasklearning.ui.core.step.ActionStepModel;
import com.sri.tasklearning.ui.core.step.ActionStepView;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;
import com.sri.tasklearning.ui.core.step.IStepViewContainer;
import com.sri.tasklearning.ui.core.step.IdiomStepModel;
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
import com.sri.tasklearning.ui.core.term.function.FirstLastModel;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;
import com.sri.tasklearning.ui.core.term.function.StructureGetModel;
import com.sri.tasklearning.ui.core.validation.EditorIssue;
import com.sri.tasklearning.ui.core.validation.EditorRepairOperationUndo;

/**
 * The controller in our Model-View-Controller implementation. Provides 
 * essential functionality for editing procedures that operates on both visuals
 * and models.
 */
public final class ProcedureEditController extends EditController {
    private static final Logger log = LoggerFactory
            .getLogger(ProcedureEditController.class);
    
    // #define
    public static final boolean ADEPT = true;
    
    private static TermModel clipboardTerm = null;

    private ProcedureModel  procModel; 
    private ProcedureView   procView;
    private UndoManager     undoMgr;
    private VariableManager varMgr;

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
    private boolean disableValidation = false;         

    public ProcedureEditController(final ProcedureModel model) {
        procModel = model;
        varMgr  = model.getVariableManager();
        undoMgr = new UndoManager();
        
        undoMgr.registerWatcher(this);        
        procModel.setController(this);
    }
    
    public void setView(ProcedureView view) {
        this.procView = view;
        
        validate(false);
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
    
    public ProcedureModel getModel() {
        return procModel; 
    }
    
    public ProcedureView getView() {
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
    
    public void setDisableValidation(boolean disable) {
        disableValidation = disable; 
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
        
        if (stepsWithIssues == null)
            return;
        
        for (StepModel step : stepsWithIssues) {
            for (EditorIssue issue : step.getIssues()) {
                nextIssue:
                for (RepairSuggestion sugg : issue.getSuggestions()) {
                    for (RepairOperation op : sugg.getOperations())
                        if (op instanceof EditorRepairOperationUndo) {
                            issue.getSuggestions().remove(sugg);
                            break nextIssue;
                        }
                }
            }
        }
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

        manageVariables();
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
        
        manageVariables();
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
        
        manageVariables();
        
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
    
    /**
     * Changes the list of results published by the currently loaded procedure
     * 
     * @param newResults the new list of variables to be published
     * @return true if the published results were changed
     */
    public boolean changePublishedResults(final List<VariableModel> newResults) {
        final List<VariableModel> oldResults = 
                ((SignatureModel) procModel.getSignature()).getResults();
        
        // Perform an item-by-item comparison of the two lists to ensure 
        // something actually changed before creating an Undoable
        boolean listsEqual = true;        

        listsEqual &= insureMatchingListElements(oldResults, newResults);
        listsEqual &= insureMatchingListElements(newResults, oldResults);
                
        if (listsEqual)
            return false;
        
        // Make the change
        ((SignatureModel) procModel.getSignature()).setResults(newResults);
        procView.refreshResults();

        undoMgr.pushUndo(new IUndoable() {
            public String getDescription() {
                return "Change the published results of the procedure";
            }

            public boolean undo() {
                return changePublishedResults(oldResults);
            }

            public boolean redo() {
                return changePublishedResults(newResults);
            }
        });  
        
        return true;
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
        
        manageVariables();
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
        final List<VariableModel> oldResults = sm.getResults();
        final List<VariableModel> newResults = new ArrayList<VariableModel>();
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
        
        if (newResults.size() != oldResults.size())
            sm.setResults(newResults);               

        // Any variables bound by this step should be marked as unbound
        for (ParameterModel pm : model.getResults())
            if (pm.getTerm() instanceof VariableModel) 
                ((VariableModel)pm.getTerm()).setBound(false);

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
    
        manageVariables();
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

        manageVariables(); 
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
    // VALIDATION
    //**************************************************************************
    
    /**
     * Validates the currently loaded procedure. Triggers the ProcedureView to
     * visualize any validation issues. 
     * 
     * @param runAsync whether or not to run the validation asynchronously. 
     */
    public void validate(final boolean runAsync) {
        if (disableValidation)
            return;
        
        if (runAsync) {
            // call the validation asynchronously
            final List<EditorIssue> issues = new ArrayList<EditorIssue>();
            final Task<Void> asyncTask = new Task<Void>() {
                public Void call() {
                    try {
                        List<EditorIssue> issues_temp = backend.validateProcedure((ProcedureModel) procModel);
                        issues.clear();
                        if (issues_temp != null)
                            for (EditorIssue iss : issues_temp)
                                issues.add(iss);
                    } catch (Exception e) {
                        log.error("Validation error: ", e);
                    }
                    return null; 
                }
            };            
            
            asyncTask.stateProperty().addListener(new ChangeListener<State>() {
               public void changed(
                       final ObservableValue<? extends State> value, 
                       final State oldVal, 
                       final State newVal) {
                   switch (newVal) {
                   case CANCELLED:
                   case RUNNING:
                   case READY:
                   case SCHEDULED:
                       break;
                   case FAILED:
                        log.error(
                                "Exception occurred during async validation task",
                                asyncTask.getException());
                       break;
                   case SUCCEEDED:
                       processIssues(issues); 
                       break;
                   }
               }
            });

            new Thread(asyncTask).start();
        } else {
            try {
                // ... and seek out the new issues/errors/warnings
                final List<EditorIssue> issues = backend.validate(procModel);
                processIssues(issues);
            } catch (Throwable exception) {
                log.error("Validation error: ", exception);
                stepsWithIssues = null; 
            }
        }
    }

    private void processIssues(List<EditorIssue> issues) {
        numStepsWithErrors.setValue(0);
        numStepsWithWarnings.setValue(0);
        List<StepModel> tempStepsWithIssues = new ArrayList<StepModel>();
        
        // clear all the current issues
        if (stepsWithIssues != null)
            for (StepModel model : stepsWithIssues) 
                model.clearIssues();
        
        if (issues == null || issues.size() == 0) {
            stepsWithIssues = tempStepsWithIssues;  
            procView.updateIssuesVisualization();
            return; 
        }
        
        for (EditorIssue issue : issues) {
            // get the owning step and add the issue to it
            final StepModel theStep = issue.getStep();
            if (theStep != null)
                theStep.addIssue(issue);

            // only add theStep if it's not in our list yet
            if (theStep != null && !(theStep instanceof ProcedureModel)
                    && tempStepsWithIssues.indexOf(theStep) < 0) {
                // insert the step into the right spot in the list (sorted by
                // step's index)
                for (int i = 0; i <= tempStepsWithIssues.size(); i++) {
                    if (i == tempStepsWithIssues.size()) {
                        // we reached the end of the list, so just append it
                        tempStepsWithIssues.add(theStep);
                        break;
                    } else if (tempStepsWithIssues.get(i).getIndex() > theStep
                            .getIndex()) {
                        // if index of ith step is bigger than index of theStep,
                        // put it before the ith
                        tempStepsWithIssues.add(i, theStep);
                        break;
                    } else {
                        // continue looping
                        continue;
                    }
                }
            }       
        }
        stepsWithIssues = tempStepsWithIssues;  
        
        int errors = 0;
        int warnings = 0;
        for (StepModel step : stepsWithIssues) {
            if (step.getIssues().get(0).isError()) errors++;
            else warnings++;
        }
        numStepsWithErrors.setValue(errors);
        numStepsWithWarnings.setValue(warnings);
        procView.updateIssuesVisualization();
    }
    
    /**
     * Causes the variable manager to collect unused variables and traverses
     * the currently loaded ProcedureModel and insures all variables used by
     * the procedure are under variable management. 
     */
    public void manageVariables() {
        manageVariablesAux(getModel());
        varMgr.collectUnusedVariables(getModel());
    }
    
    private void manageVariablesAux(final ContainerStepModel container) {
        for (StepModel step : container.getSteps()) {
            if (step instanceof ActionStepModel ||
                step instanceof IdiomStepModel) {
                for (ParameterModel pm : step.getResults()) {
                    // This can happen during demonstration
                    if (!(pm.getTerm() instanceof VariableModel))
                        continue;
                    VariableModel var = (VariableModel)pm.getTerm();
                    if (!varMgr.isManaged(var))
                        varMgr.manageVariable(var);
                    var.setBound(true);
                }
            } else if (step instanceof LoopModel) {
                LoopModel loop = (LoopModel)step;
                if (loop.getIntoTerm() instanceof VariableModel) {
                    VariableModel var = (VariableModel)loop.getIntoTerm();
                    if (!varMgr.isManaged(var))
                        varMgr.manageVariable(var);
                }
                
                if (loop.getLoopTerm() instanceof VariableModel) {
                    VariableModel var = (VariableModel)loop.getLoopTerm();
                    if (!varMgr.isManaged(var))
                        varMgr.manageVariable(var);
                }
                
                manageVariablesAux(loop); 
            } else
                throw new RuntimeException("Unexpected StepModel type: "
                        + step.getClass());
        }
    }
    
    //**************************************************************************
    // CONSOLIDATED WHOLISTIC OPERATIONS
    //**************************************************************************
    
    @Override
    public void attemptSave(final boolean allowSaveAs,
    		final boolean forceSaveAs,
    		final Callback<CommonModel, Void> onSuccess,
    		final Scene scene) {

    	final ProcedureModel proc = procModel;


    		final SignatureModel sig = (SignatureModel) proc.getSignature();
    		final String name = proc.getName();             
    		final Set<String> callers = ProcedureMap.getInstance().getCallers(name);
    		final boolean usedAsStep = (callers != null && callers
    				.size() > 0) ? true : false;

    		if ((!usedAsStep || !sig.isSignatureChanged()) && name != null
    				&& name != "" && !forceSaveAs) {
    			// call save directly, using the existing name
    			if (StorageAssistant.saveProcedure(name, (ProcedureModel) proc, false)) {
    				updateUndoAtLastSave();
    				if (onSuccess != null)
    					onSuccess.call(proc);
    			}
    		} else if (allowSaveAs) {
    			// show the Save Procedure dialog, prompting the user to pick a name
    			Callback<CommonModel, Void> innerSuccess = 
    					new Callback<CommonModel, Void>() {
    				public Void call(CommonModel pm) {
    					updateUndoAtLastSave();
    					if (onSuccess != null)
    						onSuccess.call(pm);
    					return null; 
    				}
    			};

    			SaveProcedureDialog spd = new SaveProcedureDialog(scene, (ProcedureModel) proc, 
    					innerSuccess, "Save Procedure", "Save", false);

    			spd.showSaveDialog();

    			if (usedAsStep && !forceSaveAs) {
    				Alert.show("Must save copy",
    						"This procedure is used as a step in another " +
    								"procedure and you have made incompatible changes. "+
    								"You must save a copy and manually update " +
    								"any procedures you want to reference the new " +
    								"version.", AlertConfig.OK, null); 
    			}
    		}
    		
    	
    }
    
    @Override
    public void attemptRename(
            final Callback<CommonModel, Void> onRenameSuccess,
            final Scene scene) {
        final String name = procModel.getName();             
        final Set<String> callers = ProcedureMap.getInstance().getCallers(name);
        final SignatureModel sig = (SignatureModel) procModel.getSignature(); 
        final boolean usedAsStep = (callers != null && callers
                .size() > 0) ? true : false;
        
        if (numStepsWithErrorsProperty().getValue() > 0) {
            Alert.show("Errors in procedure",
                       "There are currently errors in your procedure. " +
                       "You must resolve the errors before renaming " +
                       "your procedure.",
                       AlertConfig.OK, null);
        } else if (isUnsavedChanges() && usedAsStep
                && sig.isSignatureChanged()) {
            Alert.show("Cannot rename procedure",
                       "You cannot rename this procedure that is referenced " +
                       "as a step by another procedure because it must be " +
                       "saved in order to be renamed and you have made " +
                       "incompatible changes. You must save a copy.",
                       AlertConfig.OK, null); 
        } else if (isUnsavedChanges()) {
            Callback<AlertResult, Void> call = 
                new Callback<AlertResult, Void>() {
                public Void call(AlertResult result) {
                    if (result == AlertResult.YES) {
                        Callback<CommonModel, Void> onSaveSuccess = 
                            new Callback<CommonModel, Void>() {
                            public Void call(CommonModel pm) {
                                doRename(procModel, onRenameSuccess, scene);
                                return null;
                            }
                        };
                        attemptSave(false, false, onSaveSuccess, scene);
                    }
                    return null;
                }
            };
            Alert.show("Save changes?",
                       "Your procedure must be saved before it can be " +
                       "renamed. Do you wish to proceed?",
                       AlertConfig.YES_NO, call);
        } else 
            doRename(procModel, onRenameSuccess, scene);   
    }
    
    public static void doRename(
            final ProcedureModel argProc,
            final Callback<CommonModel, Void> onRenameSuccess,
            final Scene scene) {             
        SaveProcedureDialog spd = new SaveProcedureDialog(scene, (ProcedureModel) argProc, 
                onRenameSuccess, "Rename Procedure", "Save", true);
        spd.showSaveDialog();
    }
    
    /**
     * Tracks down the view for the given step model, selects it and causes
     * it to scroll in to view
     * 
     * TODO: Move this?
     */
    @Override
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
    @Override
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
    @Override
    public void unhighlightVariable(final VariableModel var) {
        unhighlightSteps();
        var.setHighlighted(false);
    }
    
    /**
     * Finds the StepView that visualizes the StepModel that introduced
     * (bound) a specified variable.
     * 
     * @param variable the variable to find the origin StepView for
     * @return the step view that originated the variable, or null if the 
     *         origin was not found. 
     */
    public StepView findOriginStepView(final VariableModel variable) {
        return findOriginStepViewAux(variable, getView());
    }
    
    private StepView findOriginStepViewAux(
            final VariableModel variable,
            final IStepViewContainer container) {
        for (StepView sv : container.getStepLayout().getStepViews()) {
            if (sv instanceof ActionStepView) {
                ActionStepModel asm = (ActionStepModel)sv.getStepModel();
                if (asm.getResults().size() > 0 && asm.getResults().get(0).getTerm() == variable) 
                    return sv;                
            } else if (sv instanceof IStepViewContainer) {
                if (sv.getStepModel() instanceof LoopModel) {
                    LoopModel lm = (LoopModel)sv.getStepModel();
                    if (lm.getLoopTerm().referencesVariable(variable) ||
                        (lm.getIntoTerm() != null && lm.getIntoTerm().referencesVariable(variable))) 
                        return sv;
                }
                StepView retVal = findOriginStepViewAux(variable, (IStepViewContainer)sv);
                if (retVal != null) 
                    return retVal;
            }
        }
        return null;
    }
    
    public List<FunctionModel> getSuggestedFunctionCalls(
            final ATRTask task,
            final ParameterModel parameter) {
        final List<FunctionModel> suggestions = new ArrayList<FunctionModel>();
        final List<VariableModel> vars        =  backend.getInScopeVariables(getModel(), task);
        final List<TermModel>     compounds   = new ArrayList<TermModel>();
        final boolean anyCollectionType;                  
        
        if (vars == null || vars.size() == 0)
            return suggestions;
        
        if (task instanceof LoopModel &&
                ((LoopModel)task).getInputCollection().equals(parameter))
                anyCollectionType = true;
            else
                anyCollectionType = false; 
        
        for (VariableModel var : vars)
            if (var.getTypeDef() instanceof StructDef ||
                var.getTypeDef() instanceof ListDef)
                compounds.add(var);
        
        if (compounds.size() == 0)
            return suggestions;  
        
        suggestFunctionCalls(compounds, suggestions, parameter.getTypeDef(), anyCollectionType, 0);
        
        return suggestions; 
    }
    
    private void suggestFunctionCalls(
            final List<TermModel> terms, 
            final List<FunctionModel> suggestions,
            final TypeDef targetType,
            final boolean anyCollectionType,
            final int depth) {
        
        // Don't want to go arbitrarily deep in recursive type definitions. 
        // Anything more than two    seems like it would be too complex anyway. 
        if (depth > 2)
            return;
        
        List<TermModel> extensions = new ArrayList<TermModel>();
        
        for (TermModel term : terms) {
            if (term.getTypeDef() instanceof StructDef) {
                StructDef sdef = (StructDef)term.getTypeDef();
                
                for (int i = 0; i < sdef.size(); i++) {
                    String key = sdef.getFieldName(i);
                    StructureGetModel nth = StructureGetModel.create(term.deepCopy(), key, i + 1);
                    
                    if (TypeUtilities.isAssignable(sdef.getFieldType(i), targetType))
                        suggestions.add(nth);
                    else if (anyCollectionType && sdef.getFieldType(i) instanceof CollectionTypeDef)
                        suggestions.add(nth);
                    
                    if (sdef.getFieldType(i) instanceof ListDef ||
                        sdef.getFieldType(i) instanceof StructDef) {
                        extensions.add(nth); 
                    }
                }                
            } else {
                // ListDef
                ListDef ldef = (ListDef)term.getTypeDef();
                TypeDef eleType = ldef.getElementType();
                FirstLastModel first = FirstLastModel.create("first", term.deepCopy());
                FirstLastModel last  = FirstLastModel.create("last", term.deepCopy());
                
                if (TypeUtilities.isAssignable(eleType, targetType)) {
                    suggestions.add(first);
                    suggestions.add(last);
                } else if (anyCollectionType && eleType instanceof CollectionTypeDef) {
                    suggestions.add(first);
                    suggestions.add(last);
                }
                
                if (eleType instanceof ListDef ||
                    eleType instanceof StructDef) {
                    extensions.add(first);
                    extensions.add(last); 
                }
            }
        }
        
        suggestFunctionCalls(extensions, suggestions, targetType, anyCollectionType, depth + 1);
    }

	/* public void clearDefault(ExerciseInputVariableModel input,
			ExerciseInputRow exerciseInputRow) {
		// TODO Auto-generated method stub
		
	} */
}
