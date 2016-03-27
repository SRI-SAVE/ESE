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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.TypeDef;

/**
 * Abstract base class for all descendants of ATRTerm in PUTR. 
 */
public abstract class TermModel implements ATRTerm {    
    private SimpleObjectProperty<TypeDef> type = new SimpleObjectProperty<TypeDef>(null);
    
    private SimpleStringProperty displayStringProperty = new SimpleStringProperty("");
    private SimpleBooleanProperty highlighted = new SimpleBooleanProperty(false);    

    public String getDisplayString() {
        return displayStringProperty.getValue();
    }

    public SimpleStringProperty displayStringProperty() {
        return displayStringProperty;
    }

    public TypeDef getTypeDef() {
        return type.getValue();
    }

    public void setTypeDef(TypeDef type) {
        this.type.setValue(type);
    }
    
    public SimpleObjectProperty<TypeDef> typeProperty() {
        return type; 
    }
    
    public final boolean referencesVariable(final VariableModel var) {
        return findReferencesToVariable(var).size() > 0;
    }
    
    public List<ParameterModel> findReferencesToVariable(VariableModel variable) {
        return new ArrayList<ParameterModel>();
    }
    
    public abstract TermModel deepCopy();

    public ATR getInternalSub() {
        return null;
    }
    
    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }
    
    
    public SimpleBooleanProperty highlightedProperty() {
        return highlighted;
    }
    
    public boolean isHighlighted() {
        return highlighted.getValue();
    }
    
    public void setHighlighted(boolean highlighted) {
        this.highlighted.setValue(highlighted);
    }    
}
