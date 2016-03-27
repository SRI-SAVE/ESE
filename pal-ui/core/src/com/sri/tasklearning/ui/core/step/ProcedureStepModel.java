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

package com.sri.tasklearning.ui.core.step;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sri.ai.lumen.atr.term.ATRTerm;

/**
 * Extension of ActionStepModel for procedures that are used as steps. These
 * models are currently only used in the library to distinguish between 
 * procedure steps and normal action steps. 
 */
public class ProcedureStepModel extends ActionStepModel {    
	
    public ProcedureStepModel(String functor, Collection<? extends ATRTerm> arguments) {
        super(functor, arguments);  
    }
    
    @Override
    public List<Object> getFancyName() {
        List<Object> fancy = new ArrayList<Object>();
            fancy.add(name);
        
        return fancy; 
    }
    
    @Override
    public void setFunctor(String functor) {
        super.setFunctor(functor);
        
        name.setValue(functor.substring(functor.lastIndexOf("^") + 1)); 
    }
}
