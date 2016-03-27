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

package com.sri.tasklearning.ui.core.common;

import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.VariableModel;

/**
 * Extension of VariableModel for procedure inputs, which support the concept
 * of default values. 
 */
public class InputVariableModel extends VariableModel {
	
    private TermModel defaultValue;
    
    public InputVariableModel(String name, TermModel dflt) {
        super();
        
        setVariableName(name);
        this.defaultValue = dflt; 
    }
    
    public InputVariableModel() {
		// TODO Auto-generated constructor stub
    	super(); 
	}

	public TermModel getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(TermModel defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    @Override
    public boolean isBound() {
        return true;
    }
    
    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }
    
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}