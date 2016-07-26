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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.PalUiException;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.constant.ConstantEditor;
import com.sri.tasklearning.ui.core.exercise.ExerciseView;
import com.sri.tasklearning.ui.core.popup.ConstantConfigDialog;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.function.AccessorFunctionView;
import com.sri.tasklearning.ui.core.term.function.BagModel;
import com.sri.tasklearning.ui.core.term.function.FirstLastModel;
import com.sri.tasklearning.ui.core.term.function.FirstLastView;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;
import com.sri.tasklearning.ui.core.term.function.SetModel;
import com.sri.tasklearning.ui.core.term.function.StructureGetKeyView;
import com.sri.tasklearning.ui.core.term.function.StructureGetModel;
import com.sri.tasklearning.ui.core.term.function.StructureGetView;
import com.sri.tasklearning.ui.core.term.function.StructureModel;
import com.sri.tasklearning.ui.core.term.function.TupleGetModel;
import com.sri.tasklearning.ui.core.term.function.TupleGetView;
import com.sri.tasklearning.ui.core.term.function.TupleModel;
import com.sri.tasklearning.ui.core.term.function.ZipModel;
import com.sri.tasklearning.ui.core.term.function.ZipView;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.util.Callback; 

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
            tv = new ExerciseStepParameterView((ExerciseStepParameter) model, argModel, stepView, (ExerciseView) procView);          
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
                     
        }             
        
        return items;
    } 

    // ************************ menus *****************************************

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
    
    public CommonView getOwner() {

    	return procView; 
    	
    }
    
    
}
