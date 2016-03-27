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

import com.sri.pal.CollectionTypeDef;
import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;

/**
 * Model for the first and last accessor function calls. These functions
 * operate on lists and return the first or last element. This model will infer
 * the return type of the function call based on the type the collection. 
 */

public class FirstLastModel extends FunctionModel {

    public static FirstLastModel create(String function, TermModel term) {
        Collection<TermModel> terms = new ArrayList<TermModel>();
        terms.add(term);
        return new FirstLastModel(function, terms);
    }
    
    public FirstLastModel(String f, Collection<? extends TermModel> elts) {

        super(f, elts);

        if (elts.size() != 1)
            throw new RuntimeException(
                    "Function {f} expects exactly one parameter");
        
        getInputs().get(0).setName("input list");

        // Assumes term is a variable with a known type. This should be safe. 
        TypeDef elementType = ((CollectionTypeDef) (getInputs().get(0)
                .getTerm()).getTypeDef()).getElementType();

        setTypeDef(elementType);
    }
    
    @Override
    public String getDisplayString() {
        return "the " + functor + " " + TypeUtilities.getName(getTypeDef()) + " in " + getInputs().get(0).getTerm().getDisplayString();
    }
}
