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

package com.sri.tasklearning.ui.core.popup;

import java.util.ArrayList;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.util.Callback;

import com.sri.pal.BagDef;
import com.sri.pal.CollectionTypeDef;
import com.sri.pal.ListDef;
import com.sri.pal.SetDef;
import com.sri.pal.StructDef;
import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.control.constant.ConstantEditor;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.CompositeTermModel;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.ListModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ParameterView;
import com.sri.tasklearning.ui.core.term.function.BagModel;
import com.sri.tasklearning.ui.core.term.function.SetModel;
import com.sri.tasklearning.ui.core.term.function.StructureModel;

/**
 * Abstract base class for collection and struct editing popup dialogs. 
 * Provides logic to handle nesting of collection/struct popups since those
 * types can be arbitrarily nested. 
 */
public abstract class ComplexEditorDialog extends BasicModalPopup {
    protected static final double PANE_PAD = 10;
    
    private static int nestingLevel = 0;
    protected static final ArrayList<String> titles = new ArrayList<String>();
    
    protected int myNestingLevel = 0;
    
    protected final Scene scene;
    protected final Node sender;
    protected final TypeDef type;
    protected final StepView step;
    protected final ProcedureView procView;
    
    protected CompositeTermModel newTerm;
    protected CompositeTermModel origTerm;
    protected Callback<Object, Void> onConfirmed;    

    public static ComplexEditorDialog create(
            final Scene scene, 
            final Node sender, 
            final TypeDef type, 
            final CompositeTermModel argTerm,
            final Callback<Object, Void> argOnConfirmed,
            final StepView step,
            final ProcedureView procView) {
        if (type instanceof CollectionTypeDef)
            return new CollectionEditorDialog(scene, sender, 
                    (CollectionTypeDef)type, argTerm, argOnConfirmed, step, procView);
        else if (type instanceof StructDef)
            return new StructEditorDialog(scene, sender, 
                    (StructDef)type, argTerm, argOnConfirmed, step, procView);
        
        return null;
    }

    protected ComplexEditorDialog(
            final Scene scene, 
            final Node sender,
            final TypeDef type, 
            final CompositeTermModel argTerm,
            final Callback<Object, Void> argOnConfirmed,
            final StepView step,
            final ProcedureView procView) {
        
        this.scene = scene;
        this.sender = sender;
        this.type = type;
        this.onConfirmed = argOnConfirmed;
        this.origTerm = argTerm;
        this.step = step;
        this.procView = procView;

        if (argTerm == null)
            createNewTerm();
        else
            newTerm = (CompositeTermModel) argTerm.deepCopy();

        prefWidth = 600;
        prefHeight = 450;

        okButtonText = "Save";
        cancelButtonText = "Back";
                
        myNestingLevel = nestingLevel++;

        onOkayPressed = new Callback<Object, Boolean>() {
            public Boolean call(Object obj) {
                if (saveChanges()) {
                    if (onConfirmed != null)
                        onConfirmed.call(newTerm);
                    return true;
                }
                
                return false; 
            }
        };        
        
        onClosing = new Runnable() {
            public void run() {
                nestingLevel--;
            }            
        };
    }
    
    protected abstract void refresh();
    protected abstract boolean saveChanges();
    
    protected void updateTitleText(String myTitle) {
        if (titles.size() > myNestingLevel)
            titles.set(myNestingLevel, myTitle);
        else
            titles.add(myTitle);
        
        titleText = "";
        
        for (int i = 0; i <= myNestingLevel; i++) {
            String s = titles.get(i);
            if (i > 0) {
                StringBuffer pad = new StringBuffer("");
                for (int j = 0; j < i; j++)
                    pad.append(" ");
                
                titleText += "\n" + pad + "\u21b3 " + s;
            } else
                titleText += s;               
        }
    }

    public void showDialog() {
        show(scene, sender);
    }

    protected void createNewTerm() {
        if (type instanceof BagDef)
            newTerm = new BagModel();
        else if (type instanceof SetDef)
            newTerm = new SetModel();
        else if (type instanceof ListDef)
            newTerm = new ListModel();
        else if (type instanceof StructDef)
            newTerm = new StructureModel((StructDef) type);

        newTerm.setTypeDef(type);
    }

    public CompositeTermModel getTerm() {
        return newTerm;
    }

    public void setTerm(CompositeTermModel argTerm) {
        newTerm = (CompositeTermModel) argTerm.deepCopy();
        refresh();
    }

    public Callback<Object, Void> getOnConfirmed() {
        return onConfirmed;
    }

    public void setOnConfirmed(Callback<Object, Void> onConfirmed) {
        this.onConfirmed = onConfirmed;
    }
        
    public Runnable getOnCanceled() {
        return onCancelPressed;
    }

    public void setOnCanceled(Runnable onCanceled) {
        this.onCancelPressed = onCanceled;
    }
    
    protected Node getNodeForParameterModel(final ParameterModel pm,
                                            final boolean forceTermView) {
        Node n = null;
        if (!forceTermView &&
                (pm.getTerm() == null ||
                 pm.getTerm() instanceof NullValueModel ||
                 pm.getTerm() instanceof ConstantValueModel ||
                 pm.getTerm() instanceof SetModel ||
                 pm.getTerm() instanceof ListModel ||
                 pm.getTerm() instanceof BagModel ||
                 pm.getTerm() instanceof StructureModel)) {
            final ConstantEditor e = ConstantEditor.create(pm.getTypeDef(), step, procView);
            e.setAtrValue(pm.getTerm());
            e.setOnConfirmed(new Callback<Object, Void>() {
                public Void call(Object value) {
                    pm.setTerm(e.getATRValue());
                    return null;
                }
            });

            n = e.getPane();
        } else {
            ParameterView pv = new ParameterView(pm, step, procView, true, true, null);
            if (!forceTermView)
                pv.setDisable(true);

            n = pv;
        }

        return n; 
    }

    @Override
    public void focus() {
    }       
}
