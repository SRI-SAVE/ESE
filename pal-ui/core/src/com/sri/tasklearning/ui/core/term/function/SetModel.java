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

import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * Represents a setGen() function call, which creates set in ATR. 
 */
public class SetModel extends FunctionModel {
    public SetModel(String functor, Collection<? extends TermModel> elts) {
        super(functor, elts);
    }
    
    public SetModel() {
        super("setGen", new ArrayList<TermModel>());
    }
    
    @Override
    public String getDisplayString() {
        StringBuffer ret = new StringBuffer("(");

        for (ParameterModel pm : getInputs())
            ret.append(pm.getTerm().getDisplayString() + ",");
        
        if (ret.length() > 1)
            return ret.substring(0, ret.length() - 1) + ")";
        else
            return "(empty)";
    }    
}
