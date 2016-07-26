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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.term.ATRVariable;

/**
 * Holds the state common to all types of variables.
 */
public class VariableModel extends AtomicTermModel 
      implements ATRVariable, Comparable<VariableModel> {

    public static final int MAX_NAME_LENGTH = 220;
    
    private SimpleStringProperty name = new SimpleStringProperty();
    private SimpleBooleanProperty referenced = new SimpleBooleanProperty(true);
    
    private boolean bound = false;

    // Should generally use the create factory method rather than this public constructor
    public VariableModel() {
        displayStringProperty().bind(name);
    }
    
    public static VariableModel create(String name) {
    	VariableModel newVar = new VariableModel();
    	newVar.name.setValue(name);
          return newVar;
        
    }
      
    public boolean isReferenced() {
        return referenced.getValue();
    }

    public void setReferenced(boolean referenced) {
        this.referenced.setValue(referenced);
    }
    
    public SimpleBooleanProperty referencedProperty() {
        return referenced; 
    }
    
    public boolean isBound() {
        return bound;
    }
    
    public void setBound(boolean bound) {
        this.bound = bound;
    }

    @Override
    public String toString() {
        String ret = "$" + name.getValue();

        if (getTypeDef() != null) 
            ret += " (" + TypeUtilities.getName(getTypeDef()) + ") ";
        
        return ret; 
    }
    
    @Override
    public int hashCode() {
        return name.getValue().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other.getClass().equals(this.getClass())) {
            if (name.getValue() == null) {
                if (((VariableModel) other).getVariableName() == null)
                    return true;
            } else {
                return name.getValue().equals(
                        ((VariableModel) other).getVariableName());
            }
        }
        return false;
    }    

    public int compareTo(VariableModel arg0) {
        return this.name.getValue().compareToIgnoreCase(arg0.getVariableName()); 
    }

    public String getVariableName() {
        return name.getValue();
    }
    
    public void setVariableName(String name) {
        this.name.setValue(name);
    }

    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }
    
    public ATR getInternalSub() {
        return null;
    }
    
    @Override
    public VariableModel deepCopy() {
        return this;    
    }
}
