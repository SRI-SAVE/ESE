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
import java.util.Collection;
import java.util.List;

import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.ATRParameter;

/**
 * Abstract base class for 'composite' terms, which consist of lists and
 * function calls. Note that some function calls create collections/structures,
 * and other function calls access those entities. Models for all of those 
 * cases descend from this class. 
 */
public abstract class CompositeTermModel extends TermModel {
    private final List<ParameterModel> inputs = new ArrayList<ParameterModel>();

    protected void initParams(Collection<? extends TermModel> elts) {
        for (TermModel tm : elts) {
            ATRParameter.Modality mode = ATRParameter.Modality.INPUT;
            ParameterModel param = new ParameterModel(null, null,
                    tm.getTypeDef(), tm, mode.toString(), false, this);
            
            inputs.add(param);
        }
    }
    
    public boolean referencesAnyVariable() {
        for (ParameterModel pm : getInputs()) {
            if (pm.getTerm() instanceof VariableModel)
                return true;
            else if (pm.getTerm() instanceof CompositeTermModel &&
                     ((CompositeTermModel)pm.getTerm()).referencesAnyVariable())
                return true;                             
        }
        
        return false; 
    }
    
    @Override
    public List<ParameterModel> findReferencesToVariable(final VariableModel var) {
        List<ParameterModel> refs = new ArrayList<ParameterModel>();
        for (ParameterModel pm : getInputs()) {
            final TermModel term = pm.getTerm();
            if (var.equals(term))
                refs.add(pm);
            else if (term instanceof CompositeTermModel)
                refs.addAll(term.findReferencesToVariable(var));                       
        }
        
        return refs; 
    }   
    
    public String getPreviewText() {
        String preview = "";
        
        if (getInputs().size() > 0) {
                preview += getTruncatedDisplayString(getInputs().get(0).getTerm());          
            
            if (getInputs().size() >= 2) {
                if (getInputs().size() > 2)         
                    preview += " \u2026 ";
                else
                    preview += ", ";
                preview += getTruncatedDisplayString(getInputs().get(
                        getInputs().size() -1).getTerm());
            }
        }
        
        if (preview.length() == 0)
            preview = "(empty)";
        
        return preview;
    }
    
    protected String getTruncatedDisplayString(TermModel term) {
        String disp = "";
        
        if (term != null) {
            disp = term.getDisplayString();
        
            if (disp.length() > 20)
                disp = disp.substring(0, 20) + "\u2026";
        }
        
        return disp;        
    }

    public List<ParameterModel> getInputs() {
        return inputs;
    }
    
    public int size() {
        return inputs.size();
    }

    @Override
    public String getDisplayString() {
        return inputs.toString();
    }

    public boolean equals(Object other) {
        if (other != null && other.getClass().equals(this.getClass())) {
            CompositeTermModel oth = (CompositeTermModel) other;
            if (size() != oth.size())
                return false;
            int i = 0;
            while (i < size()) {
                if (!this.inputs.get(i).getTerm()
                        .equals(oth.inputs.get(i).getTerm()))
                    return false;
                i++;
            }
            return true;
        }
        return false;
    }
    
    public int hashCode() {
        int hash = 0;
        for (ParameterModel pm : getInputs())
            hash += pm.getTerm().hashCode();
        
        return hash; 
    }

    @Override
    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }    
}
