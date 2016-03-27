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

package com.sri.tasklearning.ui.core.control.constant;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

import com.sri.pal.CollectionTypeDef;
import com.sri.pal.CustomTypeDef;
import com.sri.pal.EnumeratedTypeDef;
import com.sri.pal.PrimitiveTypeDef;
import com.sri.pal.StructDef;
import com.sri.pal.TypeDef;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.ToolTippedImageView;
import com.sri.tasklearning.ui.core.control.ToolTipper;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTippable;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * Base class for controls that facilitate editing fixed/constant values in a
 * procedure. Constant values consist of values wrapped by ConstantValueModel 
 * (ATRLiteral) or structs/collections represented by instances of
 * CompositeTermModel. Constant values appear as the default values for 
 * procedure inputs and may be used for inputs to steps. The controls that 
 * derive from this class provide for their editing.  
 * 
 * In general, atomic (ATRLiteral) values can be edited in-place whereas
 * composite values are represented by a button that will display an editing
 * dialog window upon being clicked. 
 * 
 * Which constant control is used for a particular value in a procedure is 
 * determined by that value's type. The details of this calculation are 
 * contained in the static {@code ConstantEditor.create} method.  
 */
public abstract class ConstantEditor {    
    protected Callback<Object, Void> onChanged;
    protected Callback<Object, Void> onConfirmed;
    protected Runnable onCanceled;
    
    private Image okay = Utilities.getImage("toolbar/apply.png");
    private Image error = Utilities.getImage("error_small.png");
    protected static final double ICON_SIZE = 16;
    protected ToolTippedImageView icon = new ToolTippedImageView(okay);
    {
        // Most constant editors don't need this since they don't allow
        // illegal values
        icon.setFitHeight(ICON_SIZE);
        icon.setFitWidth(ICON_SIZE);
        ToolTipper.registerEventHandlers(icon);
    }
    
    protected SimpleBooleanProperty validValue = new SimpleBooleanProperty(true);  
    {
        validValue.addListener(new ChangeListener<Boolean>() {
            public void changed(
                    final ObservableValue<? extends Boolean> val, 
                    final Boolean oldVal, 
                    final Boolean newVal) {
                if (pane.equals(icon.getParent())) {
                    final String tip;
                    if (newVal) {
                        icon.setImage(okay);
                        tip = "The current value is valid";
                    } else {
                        icon.setImage(error);
                        tip = "The current value is not valid for this data type";
                    }
                    
                    icon.setToolTipCallback(new IToolTipCallback() {
                        public String getToolTipText() {
                            return tip; 
                        }
                    });
                }                
            }
        });
    }
    
    protected static final double HPAD = 5; 
    protected ToolTippedHBox pane = new ToolTippedHBox(HPAD); 
    {
        pane.setAlignment(Pos.CENTER_LEFT);
        pane.setMinWidth(80);
        pane.setMinHeight(18);
        pane.setPrefHeight(20);
        // Pref width set by parent
    }
    
    protected boolean doLiveUpdates = false;
    
    public static ConstantEditor create(
            final TypeDef type, 
            final StepView step,
            final CommonView procView) {        
        ConstantEditor special = getSpecialCaseEditor(type, step);        
        
        if (special != null)
            return special;
        
        if (type instanceof EnumeratedTypeDef)
            return new EnumConstantEditor((EnumeratedTypeDef)type);
        else if (type instanceof PrimitiveTypeDef ||
                 type instanceof CustomTypeDef) 
            return getEditorFromClass(type);
        else if (type instanceof CollectionTypeDef  ||
                 type instanceof StructDef)            
            return new ComplexConstantEditor(type, step, procView);
        else {
            return new StringConstantEditor(type);
        }
    }
    
    private static ConstantEditor getEditorFromClass(final TypeDef type) {
        final Class<?> clazz;
        boolean asString = false;
        
        if (type instanceof PrimitiveTypeDef)
            clazz = ((PrimitiveTypeDef)type).getKind().getRepresentationClass();
        else {
            try {        
                clazz = ((CustomTypeDef)type).getJavaClass();
                asString = true; 
            } catch (ClassNotFoundException e) {
                StringConstantEditor ed = new StringConstantEditor(type);
//                ed.setValue("Unable to display or edit values of this type");
//                ed.getPane().setDisable(true);
                return ed; 
            }
        }
        
        if (clazz.equals(Integer.class) ||
            clazz.equals(Long.class) ||
            clazz.equals(Short.class))
            return new IntConstantEditor(clazz, type, asString);
        else if (clazz.equals(Double.class) ||
                 clazz.equals(Float.class))
            return new RealConstantEditor(clazz, type, asString);
        else if (clazz.equals(Boolean.class)) 
            return new BoolConstantEditor(asString, type);
        else if (clazz.equals(String.class))
            return new StringConstantEditor(type);
        else {            
            StringConstantEditor ed = new StringConstantEditor(type);          
            //ed.getPane().setDisable(true);
            return ed; 
        }           
    }
    
    private static ConstantEditor getSpecialCaseEditor(
            final TypeDef type,
            final StepView step) {
        
        if (!(type.getName() instanceof SimpleTypeName))
            return null;
        
        final String typeName = ((SimpleTypeName)type.getName()).getSimpleName();
        
        if (type instanceof StructDef && typeName.equals("file")) {
            
            StructDef sd = (StructDef)type;
            if (sd.size() == 3 &&
                sd.getFieldNum("directory") >= 0 &&
                sd.getFieldNum("file name") >= 0 &&
                sd.getFieldNum("extension") >= 0) {
                return new FileConstantEditor(sd, step);
            }
        }
        
        return null;
    }
    
    public Pane getPane() {        
        return pane; 
    }    
    
    /**
     * If isDialog() returns true for a concrete subclass of ConstantEditor,
     * that class must put code in openDialog that bypasses the button or
     * whatever other mechanism is used to display the constant value inline
     * and opens the editing dialog directly. This is for purpose of conteXt
     * menus. 
     * 
     * @param scene - The parent scene of the editing dialog, which may not be
     * the scene of the dialog itself. 
     */
    public abstract void openDialog(Scene scene);
    
    /**
     * @return whether or not this constant editor is a dialog window
     */
    public abstract boolean isDialog();
    
    public abstract void setValue(Object val);
    
    public abstract void setAtrValue(TermModel atr);
    
    public abstract Object getValue();
    
    public abstract TermModel getATRValue();
    
    public abstract Object getDefault();
    
    public abstract void select();
    
    public boolean isValidValue(String errorMessage) {
        return validValue.getValue();
    }
    
    public SimpleBooleanProperty validValueProperty() {
        return validValue; 
    }
    
    public void setTooltip(IToolTipCallback call) {
        pane.setToolTipCallback(call);
    }
    
    public Callback<Object, Void> getOnChanged() {
        return onChanged;
    }

    public void setOnChanged(Callback<Object, Void> onChanged) {
        this.onChanged = onChanged;
    }

    public Callback<Object, Void> getOnConfirmed() {
        return onConfirmed;
    }

    public void setOnConfirmed(Callback<Object, Void> onConfirmed) {
        this.onConfirmed = onConfirmed;
    }
        
    public Runnable getOnCanceled() {
        return onCanceled;
    }

    public void setOnCanceled(Runnable onCanceled) {
        this.onCanceled = onCanceled;
    }
    
    public boolean isDoLiveUpdates() {
        return doLiveUpdates;
    }

    public void setDoLiveUpdates(boolean doLiveUpdates) {
        this.doLiveUpdates = doLiveUpdates;
    }

    protected static class ToolTippedHBox extends HBox implements IToolTippable {
        private IToolTipCallback toolTipCallback = new IToolTipCallback() {
            public String getToolTipText() {
                return null;    
            }
        };
        
        public ToolTippedHBox(double padding) {
            super(padding);
            
            ToolTipper.registerEventHandlers(this);
        }
            
        // *************************** IToolTippable ***************************
        
        public Node getToolTipNode() {
            return this; 
        }
        
        public IToolTipCallback getToolTipCallback() {
            return toolTipCallback;
        }
    
        public void setToolTipCallback(IToolTipCallback toolTipCallback) {
            this.toolTipCallback = toolTipCallback;
        }
    }
}
