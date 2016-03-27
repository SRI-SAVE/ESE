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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import com.sri.ai.lumen.atr.ATR;
import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;

/**
 * Represents the indirection between a parameter to a step or procedure and
 * the value that is currently satisfying the parameter. This class is very 
 * important in the PAL UI and is an example of where we deviate from ATR by 
 * adding this indirection, which facilitates editing. 
 */
public class ParameterModel {
    public static final String ARGUMENT_FLAG = "#";
    public static final String INPUT_FUNCTOR = "+"; // The functor for an input
    public static final String RESULT_FUNCTOR = "-"; // The functor for a result    
    
    protected String name; // Name of the argument
    protected String description;
    protected String modality = INPUT_FUNCTOR;
    protected boolean important = false;
    protected ATR owner; // The owner of the parameter such as an ActionStepModel or a FunctionModel
    protected TypeDef type;

    // Property that stores the term argument to this parameter
    private final SimpleObjectProperty<TermModel> term = 
        new SimpleObjectProperty<TermModel>(null); 
    private final BooleanProperty bound = 
        new SimpleBooleanProperty(false);
    
    public ParameterModel(
            final String name, 
            final String description,
            final TypeDef type,
            final TermModel variable, 
            final String modality,
            final boolean important, 
            final ATR owner) {
        
        this.name = name;
        this.type = type;
        this.description = description;
        this.important = important;
        this.owner = owner; 
        
        setTerm(variable);
        
        if (modality != null)
            this.modality = modality;
        
        if (description == null || description.length() == 0)
            this.description = name;        
        
        // In most cases, the type assigned below will be overwritten. This is a fail-safe. 
        if (type == null && variable != null && variable.getTypeDef() != null)
            this.type = variable.getTypeDef();          
    }
    
    public ParameterModel() {
    
    }             
    
    @Override
    public boolean equals(Object arg0) {
        return arg0 instanceof ParameterModel
                && ((ParameterModel) arg0).name.equals(this.name)
                && ((ParameterModel) arg0).term.equals(this.term);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public boolean isResult() {
        return modality.equals(RESULT_FUNCTOR);
    }
    
    public void setImportant(boolean important) {
        this.important = important;
    }
    
    public boolean isImportant() {
        return important;
    }

    public void setTerm(final TermModel term) {
        if (term instanceof FunctionModel)
            ((FunctionModel)term).setParentArg(this);
        
        this.term.setValue(term);      
    }

    public TermModel getTerm() {
        return term.getValue();
    }
    
    public SimpleObjectProperty<TermModel> termProperty() {
        return term; 
    }
    
    public BooleanProperty boundProperty() {
        return bound;
    }
    
    public void setBound(boolean bound) {
        this.bound.setValue(bound);
    }
    
    public boolean isBound() {
        return bound.getValue();
    }
    
    public ATR getOwner() {
        return owner;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName(){
        return name;
    }
    
    public void setDescription(String desc) {
        this.description = desc;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }    
    
    public String toString() {
        return name + "=" + term;
    }

    public TypeDef getTypeDef() {
        return type;
    }
    
    public void setTypeDef(TypeDef type) {
        this.type = type;
    }
    
    public ConstantValueModel getType() {
        return new ConstantValueModel(type);
    }
    
    public ParameterModel copy() {
        return new ParameterModel(
                name, description, type, term.getValue(), 
                 modality, important, owner);
    }
}
