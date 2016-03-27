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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.term.ActionDeclarationParameterModel;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableModel;

/**
 * The signature of a procedure including the names and types of its inputs and
 * outputs. Provides change detection through {@code isSignatureChanged} for
 * purposes of detecting breaking changes in procedure dependencies. 
 */
public abstract class SignatureModel implements ATRSig {
    
    protected String functor;
    protected final List<InputVariableModel> inputs = 
        new ArrayList<InputVariableModel>();
    
    protected List<VariableModel> results = new ArrayList<VariableModel>();  
    protected CommonModel owningActionDecl;
    protected final List<TypeDef> origInputTypes = new ArrayList<TypeDef>();
    protected final List<TypeDef> origResultTypes = new ArrayList<TypeDef>();
    
    public SignatureModel(String f,
            Collection<ActionDeclarationParameterModel> params) {
        this.functor = f;            
    
        for (ActionDeclarationParameterModel pm : params) {
            if (pm.isProcedureInput()) {
                inputs.add((ProcedureInputVariableModel)pm.getTerm());
                origInputTypes.add(pm.getTypeDef()); 
            } else {            
                results.add((VariableModel)pm.getTerm());
                origResultTypes.add(pm.getTypeDef());
            }
        }    
    }
    
    public SignatureModel() {
        // Leaving functor null causes an exception in backend during validation
        functor = "";
    }
    
    public CommonModel getOwningActionDecl() {
        return owningActionDecl;
    }

    public void setOwningActionDecl(CommonModel owningActionDecl) {
        this.owningActionDecl = owningActionDecl;
    }
    
    public String getFunctor() {
        return functor;
    }

    public List<InputVariableModel> getInputs() {
        return inputs;
    }

    public List<VariableModel> getResults() {
        return results;
    }
    
    public void setResults(List<VariableModel> results) {
        this.results = results;
    }
    
    public void setFunctor(String functor) {
        this.functor = functor;
    }
    
    /**
     * Returns whether or not this signature model has underwent a breaking 
     * change since the original loading of this procedure. A breaking change
     * entails changing the number of procedure inputs or outputs, changing
     * the type of inputs or outputs or changing the order of inputs and outputs
     * with different types. An example of a non-breaking change is renaming an 
     * input or output or reordering inputs of the same type.  
     * 
     * @return whether or not a breaking change has been made to the signature     
     */
    public boolean isSignatureChanged() {
        if (origResultTypes.size() != results.size())
            return true;
        
        if (origInputTypes.size() != inputs.size())
            return true;
        
        for (int i = 0; i < results.size(); i++)
            if (!TypeUtilities.isAssignable(results.get(i).getTypeDef(),
                    origResultTypes.get(i)))
                return true;
        
        for (int i = 0; i < inputs.size(); i++)
            if (!TypeUtilities.isSuperType(inputs.get(i).getTypeDef(),
                    origInputTypes.get(i)))
                return true;
        
        return false; 
    }
    
    public List<ActionDeclarationParameterModel> getElements() {
        // wrap the variables in parameters and return them as a list
        List<ActionDeclarationParameterModel> list = 
            new ArrayList<ActionDeclarationParameterModel>();
        
        for (InputVariableModel v : inputs)
            list.add(new ActionDeclarationParameterModel(v, Modality.INPUT, v
                    .getTypeDef().getName().getFullName(), v.getDefaultValue(),
                    false));

        for (VariableModel v : results)
            list.add(new ActionDeclarationParameterModel(v, Modality.OUTPUT, v
                    .getTypeDef().getName().getFullName(), null, false));
        
        return list;
    }
    
    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }
    
    public ATR getInternalSub() {
        return null;
    }
}
