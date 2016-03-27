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

import com.sri.ai.lumen.atr.term.ATRList;

/**
 * Holds the state for a list of terms. The types of terms should be 
 * homogeneous. 
 */

public class ListModel extends CompositeTermModel implements ATRList {
    
    public ListModel(Collection<? extends TermModel> elts) {
        initParams(elts);
    }
    
    public ListModel() {
        initParams(new ArrayList<TermModel>());
    }

    @Override
    public String getDisplayString() {
        StringBuffer ret = new StringBuffer("[");

        for (ParameterModel pm : getInputs())
            ret.append(pm.getTerm().getDisplayString() + ",");
        
        if (ret.length() > 1)
            return ret.substring(0, ret.length() - 1) + "]";
        else
            return "[empty]";
    }    

    @Override
    public boolean isEmpty() {
        return getInputs().isEmpty();
    }

    @Override
    public TermModel get(int i) {
        return getInputs().get(i).getTerm();
    }

    @Override
    public <T> T[] toArray(T[] obj) {
        return getTerms().toArray(obj);
    }
    
    @Override
    public List<TermModel> getTerms() {
        List<TermModel> list = new ArrayList<TermModel>();
        for (ParameterModel pm : getInputs())
            list.add(pm.getTerm());
        return list;
    }
    
    @Override
    public ListModel deepCopy() {
        List<TermModel> elts = new ArrayList<TermModel>();
        
        for (ParameterModel pm : getInputs())
            elts.add(pm.getTerm().deepCopy());
        
        ListModel copy = new ListModel(elts);
        copy.setTypeDef(getTypeDef());
        return copy;
    }
}