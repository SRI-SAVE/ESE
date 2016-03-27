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

package com.sri.tasklearning.ui.core.term.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sri.ai.lumen.atr.term.ATRFunction;
import com.sri.tasklearning.ui.core.CoreUIModelFactory;
import com.sri.tasklearning.ui.core.term.CompositeTermModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * Base class for all function calls. All collections are implemented as 
 * function calls except for lists. {@code ListModel} is a sibling to this class
 * under CompositeTermModel. 
 */
public class FunctionModel extends CompositeTermModel implements ATRFunction {
    protected String functor;
    protected ParameterModel parentArg;

    public FunctionModel(String f, Collection<? extends TermModel> elts) {
        this.functor = f;

        initParams(elts);
    }

    @Override
    public String getDisplayString() {
        StringBuffer ret = new StringBuffer(functor);

        for (ParameterModel pm : getInputs()) {
            ret.append(" " + pm.getTerm().getDisplayString() + ", ");
        }
        return ret.substring(0, ret.length() - 3);
    }

    @Override
    public String getFunctor() {
        return functor;
    }

    @Override
    public List<TermModel> getElements() {
        List<TermModel> list = new ArrayList<TermModel>();
        for (ParameterModel pm : getInputs())
            list.add(pm.getTerm());
        return list;
    }

    public ParameterModel getParentArg() {
        return parentArg;
    }

    public void setParentArg(ParameterModel parentArg) {
        this.parentArg = parentArg;
    }
    
    @Override
    public FunctionModel deepCopy() {
        List<TermModel> elts = new ArrayList<TermModel>();
        
        for (ParameterModel pm : getInputs())
            elts.add(pm.getTerm().deepCopy());
        
        CoreUIModelFactory factory = new CoreUIModelFactory();
        FunctionModel copy = factory.createFunction(functor, elts);
        copy.setTypeDef(getTypeDef());
        
        int i = 0;
        for (ParameterModel pm : getInputs()) {
            copy.getInputs().get(i).setTypeDef(pm.getTypeDef());
            i++;
        }
        return copy;
    }
    
    public boolean equals(Object other) {
        if (other instanceof FunctionModel)
            return super.equals(other) && functor.equals(((FunctionModel)other).getFunctor());        
        
        return false;
    }
    
    public int hashCode() {
        return super.hashCode() + functor.hashCode();
    }
    
    public static boolean isAccessorFunction(final TermModel function) {
        if (function instanceof FirstLastModel ||
            function instanceof StructureGetModel || 
            function instanceof ZipModel)
            return true;
        
        return false; 
    }
}
