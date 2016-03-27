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

import com.sri.pal.StructDef;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * Models the nth() function call, which is used to access individual elements 
 * of a struct.
 */
public class TupleGetModel extends FunctionModel {

    public static final int IDX_IDX = 0;
    public static final int TUPLE_IDX = 1;
    
    private String key;

    public static TupleGetModel create(
            final ConstantValueModel key,
            final TermModel value) {
        List<TermModel> elts = new ArrayList<TermModel>();
        elts.add(key);
        elts.add(value);
        
        return new TupleGetModel("nth", elts);
    }
    
    public TupleGetModel(String f, Collection<? extends TermModel> elts) {
        super(f, elts);

        if (elts.size() != 2)
            throw new RuntimeException(
                    "nth expects exactly two parameters");
        if (!(this.getInputs().get(IDX_IDX).getTerm() instanceof ConstantValueModel))
            throw new RuntimeException(
                    "1st parameter to nth must be a an integer index");
        
        getInputs().get(IDX_IDX).setName("field");
        getInputs().get(TUPLE_IDX).setName("input");
        
        TermModel term = getInputs().get(TUPLE_IDX).getTerm();
        StructDef def = (StructDef)term.getTypeDef();
        
        if (def != null) {
            int index = ((ConstantValueModel) getInputs().get(IDX_IDX)
                    .getTerm()).getInt();
            key = def.getFieldName(index);        
            setTypeDef(def.getFieldType(index));
        }
    }       

    public String getDisplayString() {
        return "the " + key + " of "
                + getInputs().get(TUPLE_IDX).getTerm().getDisplayString();
    }
}
