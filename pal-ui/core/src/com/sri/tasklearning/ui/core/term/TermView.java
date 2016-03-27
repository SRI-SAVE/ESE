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

package com.sri.tasklearning.ui.core.term;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.sri.tasklearning.ui.core.term.function.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.PalUiException;
import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.VariableManager;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.constant.ConstantEditor;
import com.sri.tasklearning.ui.core.popup.ConstantConfigDialog;
import com.sri.tasklearning.ui.core.popup.ReplaceVariableDialog;
import com.sri.tasklearning.ui.core.popup.ReplaceVariableDialog.ITermChosenCallback;
import com.sri.tasklearning.ui.core.step.LoopModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.exercise.ExerciseEditController; 

/**
 * Abstract base class for views of TermModels. Provides a static method
 * {@code create} that will create the appropriate TermView for a given 
 * TermModel. Contains common code for generating term menu options that may
 * optionally be used by any of its descendents. 
 */
public abstract class TermView {
    private static final Logger log = LoggerFactory
            .getLogger(ParameterView.class);

    protected final ParameterModel argument;
    protected final TermModel termModel;
    protected final StepView stepView;
    protected final CommonView procView;
    
    protected final EditController controller;    
    
    protected boolean generateMenuItems = true;

    protected SimpleBooleanProperty readOnly = new SimpleBooleanProperty(false);
    protected SimpleBooleanProperty inline = new SimpleBooleanProperty(false);    

    // Indicates if the term view is nested within an editing dialog
    protected boolean nested = false;
    
    protected TermView(            
            final TermModel argModel,
            final ParameterModel argParamModel,
            final StepView argStepView,
            final CommonView argProcView) {
        this.termModel = argModel;
        this.argument  = argParamModel;
        this.stepView  = argStepView;
        this.procView  = argProcView;
        
        if (procView != null)
            controller = procView.getController();
        else 
            controller = null;
    }

   
	// Subclasses must override this method to provide the UI region, control,
    // whatever that visualizes this term
    public abstract Node getNode();
    
    public abstract void setSpecialMenuItems(List<MenuItem> items);

    public void setReadOnly(boolean readonly) {
        readOnly.setValue(readonly);
    }

    public boolean isReadOnly() {
        return readOnly.getValue();
    }

    public boolean isNested() {
        return nested;
    }
    
    public boolean isGenerateMenuItems() {
        return generateMenuItems;
    }

    public void setGenerateMenuItems(boolean generateMenuItems) {
        this.generateMenuItems = generateMenuItems;
    }
    
    public TermModel getTermModel() {
        return termModel; 
    }

    public SimpleBooleanProperty readOnlyProperty() {
        return readOnly;
    }

    // ************************************************************************

    public static TermView create(
            final TermModel model,
            final ParameterModel argModel, 
            final StepView stepView,
            final CommonView procView, 
            final boolean nested)
            throws PalUiException {

        TermView tv = null;
        if (model instanceof StructureGetModel)
            tv = new StructureGetView((StructureGetModel)model, argModel, stepView, procView);
        else if (model instanceof FirstLastModel)
            tv = new FirstLastView((FirstLastModel) model, argModel, stepView, procView);
        else if (model instanceof ListModel || model instanceof SetModel
                || model instanceof BagModel || model instanceof StructureModel || model instanceof TupleModel)
            tv = new CollectionView((CompositeTermModel) model, argModel, stepView, procView);
        else if (model instanceof ZipModel)
            tv = new ZipView((ZipModel) model, argModel, stepView, procView);
        else if (model instanceof TupleGetModel)
            tv = new TupleGetView((TupleGetModel) model, argModel, stepView, procView);
        else if (model instanceof FunctionModel) {
            log.warn("Unknown function '"
                    + ((FunctionModel) model).getFunctor()
                    + "'encountered. Creating a default view");
            tv = new AccessorFunctionView((FunctionModel) model, argModel, stepView, procView);
        } else if (model instanceof ExerciseStepParameter) {
            tv = new ExerciseStepParameterView((ExerciseStepParameter) model, argModel, stepView, procView);          
        }  else if (model instanceof VariableModel) {
            tv = new VariableView((VariableModel) model, argModel, stepView, procView);
        } else if (model instanceof ConstantValueModel) {
            if (argModel.getOwner() instanceof StructureGetModel)
                tv = new StructureGetKeyView((ConstantValueModel) model,
                        (StructureGetModel)argModel.getOwner(), argModel, stepView,
                        procView);
            else
                tv = new ConstantValueView((ConstantValueModel) model,
                        argModel, stepView, procView);
        } else if (model instanceof NullValueModel)
            tv = new NullValueView((NullValueModel) model, argModel, stepView, procView);
        else if (model == null) {
            tv = new NullValueView(NullValueModel.NULL, argModel, stepView, procView);
        } else {
            throw new PalUiException("Unknown model type: " + model.getClass());
        }

        tv.nested = nested;

        return tv;
    }
   
    public List<MenuItem> buildTermOptionsMenu() {
        if (!generateMenuItems)
            return null;
        
        List<MenuItem> items = new ArrayList<MenuItem>();
        
        if (stepView != null && !isReadOnly()) {
            String constOpt;
            if (this instanceof ConstantValueView ||
                this instanceof CollectionView)
                constOpt = "Change value\u2026";
            else
                constOpt = "Use a fixed value\u2026";

            MenuItem constant = getUseConstantValueMenu(constOpt);
            
            if (constant != null)
                items.add(constant);
            
            MenuItem input = getProcedureInputMenu();
            
            if (input != null)
                items.add(input);            
            
            items.addAll(getExistingValueMenus()); 
        }        
        
        List<MenuItem> copyPaste = getCopyPasteMenus(!isReadOnly());
        if (copyPaste.size() > 0) {
            if (items.size() > 0)
                items.add(new SeparatorMenuItem());
            items.addAll(copyPaste);
        }
        
        return items;
    } 

    // ************************ menus *****************************************

    public boolean canMakeProcedureInput() {
        if (argument == null || argument.getTypeDef() == null)
            return false;
        
        if (BackendFacade.getInstance().debuggingProcedureProperty().getValue())
            return false; 
        
        if (stepView.getStepModel() instanceof LoopModel)
            return false; 

        if (!TypeUtilities.allowAskUser(argument.getTypeDef()))
            return false;

        return true;
    }
    
    // menu item for making this into a procedure input
    protected MenuItem getProcedureInputMenu() {
        if (!canMakeProcedureInput())
            return null;
        MenuItem menuitem = new MenuItem();
        menuitem.setText("Ask the user for this value");
        menuitem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                if (!nested) {
                    ((ProcedureEditController) controller).changeArgumentToProcedureInput(argument,
                            stepView.getStepModel());
                    stepView.layout();
                } else {
                    ProcedureInputVariableModel pim = 
                        ((ProcedureEditController) controller).createNewProcedureInput(argument.getTypeDef(), termModel);
                    argument.setTerm(pim);
                }
            }
        });

        return menuitem;
    }

    private void showConstantValueMenu() {
        final TypeDef type = argument.getTypeDef();

        final ConstantEditor ce = ConstantEditor.create(type, stepView, procView);

        if (ce.isDialog()) {
            // This is trIcky. If term is a CompositeTermModel, then we should
            // use that value in the dialog editor. But, inline accessor
            // functions
            // are also CompositeTermModels, so filter them out based on their
            // view...
            CompositeTermModel collModel = 
                    (termModel instanceof CompositeTermModel && !(this instanceof AccessorFunctionView)) ? 
                            (CompositeTermModel) termModel : null;

            if (termModel instanceof ProcedureInputVariableModel
                    && ((ProcedureInputVariableModel) termModel)
                            .getDefaultValue() instanceof CompositeTermModel)
                collModel = (CompositeTermModel) ((ProcedureInputVariableModel) termModel)
                        .getDefaultValue().deepCopy();

            if (collModel != null)
                ce.setAtrValue(collModel);

            final CompositeTermModel fCollModel = collModel;
            final StepModel step = stepView.getStepModel();

            Callback<Object, Void> onConfirmed = new Callback<Object, Void>() {
                public Void call(Object value) {
                    if ((fCollModel == null && ce.getATRValue() != null)
                            || (fCollModel != null && !fCollModel.equals(ce
                                    .getATRValue()))) {
                        controller.changeArgument(argument, ce.getATRValue(),
                                step, false);
                    }
                    return null;
                }
            };
            ce.setOnConfirmed(onConfirmed);
            ce.openDialog(getNode().getScene());
        } else {
            // If the constant editor isn't already a dialog, we need to wrap
            // it in a ConstantConfigDialog
            Callback<Object, Boolean> onOkayPressed = new Callback<Object, Boolean>() {
                public Boolean call(Object obj) {
                    // replace the variable with a constant using the value
                    // entered
                    final ConstantValueModel cvm = new ConstantValueModel(obj, argument.getTypeDef());
                    controller.changeArgument(argument, cvm,
                            stepView.getStepModel(), nested);
                    return true;
                }
            };
            ConstantConfigDialog.showDialog(argument, getNode().getScene(),
                    stepView, procView, onOkayPressed, null);
        }
    }
    
    protected MenuItem getUseConstantValueMenu(final String text) {
        if (argument == null)
            return null;
        
        // If this is a term to an accessor function we don't  allow 
        // the user to replace the current value with a 'constant' collection
        // or struct because it could lead to things such as 
        // nth(positionalTupleGen()), which from Lapdog and the Editor's 
        // perspective is illegal and would break the Editor's type inference.      
        // Accessors calling accessors is fine, but accessors calling 
        // inline generators is incorrect. 
        if (argument.getOwner() instanceof FunctionModel && 
            FunctionModel.isAccessorFunction((FunctionModel) argument.getOwner()))
            return null;
        
        // We don't allow configuration of loop terms (the input, iterand,
        // and collector) from the main procedure view. Configuration must
        // be performed through the loop configuration dialog. 
        if (stepView.getStepModel() instanceof LoopModel)
            return null; 

        if (!TypeUtilities.allowFixedValue(argument.getTypeDef()))
            return null;

        final MenuItem menuitem = new MenuItem();
        menuitem.setText(text);
        menuitem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                if (argument != null)
                    showConstantValueMenu();
            }
        });
        return menuitem;
    }

    protected List<MenuItem> getExistingValueMenus() {
        List<MenuItem> menus = new ArrayList<MenuItem>();

        if (argument == null)
            return menus;
        
        if (stepView.getStepModel() instanceof LoopModel)
            return menus; 

        if (!TypeUtilities.allowExistingValue(argument.getTypeDef()))
            return menus;

        List<VariableModel> exceptions = new ArrayList<VariableModel>();
        // the owning step's results shouldn't be listed, so add them to the
        // exceptions list
        for (ParameterModel arg : stepView.getStepModel().getResults())
            if (arg.getTerm() instanceof VariableModel)
                exceptions.add((VariableModel) arg.getTerm());

        VariableManager vm = procView.getController().getVariableManager();
        
        final List<VariableModel> existingValues = 
        		( vm != null ?  
        					vm.getValidInputs(stepView.getStepModel(), argument, exceptions) :	
        					new LinkedList<VariableModel>() );   

        final List<FunctionModel> functions = controller
                .getSuggestedFunctionCalls(stepView.getStepModel(), argument);

        // if there are no existing values, show a disabled menu saying as
        // much
        MenuItem menuitem3;
        if ((existingValues == null || existingValues.size() == 0)
                && (functions == null || functions.size() == 0)) {
            menuitem3 = new MenuItem();
            menuitem3.setText("No existing values available");
            menuitem3.setDisable(true);
            menus.add(menuitem3);
        } else {
            List<MenuItem> availableValues = new ArrayList<MenuItem>();
            for (int i = 0; i <= Math.min(existingValues.size(), 4) - 1; i++) {
                final VariableModel existingVal = existingValues.get(i);
                final EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent e) {
                        controller.changeArgument(argument, existingVal,
                                stepView.getStepModel(), nested);
                    }
                };
                availableValues.add(VariableView.makeVariableMenuItem(
                        existingValues.get(i), argument.getTerm(), handler));
            }
            menus.addAll(availableValues);
        }

        // add the "more" menu item and it's children if there
        // are more than 4 existing values or there are function calls
        if (existingValues != null && existingValues.size() > 4 || functions != null && functions.size() > 0) {
            final MenuItem moreValuesButton = new MenuItem("More...");
            moreValuesButton.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    ITermChosenCallback cb = new ITermChosenCallback() {
                        public void callback(TermModel term) {
                            controller.changeArgument(argument, term,
                                    stepView.getStepModel(), nested);
                        }
                    };
                    ReplaceVariableDialog.showDialog(existingValues, functions,
                            argument, getNode().getScene(), procView, cb);
                }
            });

            menus.add(moreValuesButton);
        }

        return menus;
    }
    
    protected List<MenuItem> getCopyPasteMenus(boolean includePaste) {
        
        List<MenuItem> menus = new ArrayList<MenuItem>();
        
        if (termModel.getTypeDef() != null) {
            MenuItem copy = new MenuItem("Copy");
            copy.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    ProcedureEditController.setClipboardTerm(termModel);
                }
            });
            menus.add(copy);
        }
        
        if (ProcedureEditController.getClipboardTerm() == null || !includePaste) 
            return menus;   
        
        final TermModel clipboardTerm = ProcedureEditController.getClipboardTerm();
        
        if (!TypeUtilities.isAssignable(clipboardTerm.getTypeDef(), argument.getTypeDef()))
            return menus;
        
        MenuItem paste = new MenuItem("Paste");
        paste.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                if (clipboardTerm.equals(argument.getTerm()))
                    return; 
                
                controller.changeArgument(argument, clipboardTerm.deepCopy(),
                        stepView.getStepModel(), nested);
            }
        });
        
        menus.add(paste);
        
        return menus;
    }
}
