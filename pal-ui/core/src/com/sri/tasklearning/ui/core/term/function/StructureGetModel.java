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

import com.sri.ai.lumen.runtime.StructureGetFunOp;
import com.sri.pal.StructDef;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;

/**
 * Models the structGet function call, which is used to access named
 * fields of a struct.
 */
public class StructureGetModel extends FunctionModel {

    public static final int TERM_IDX = 0;
    public static final int KEY_IDX = 1;
    public static final int IDX_IDX  = 2;
    
    private final ConstantValueModel key;
    private final ConstantValueModel idx; 
    private final ParameterModel termParameter;
    
    public static StructureGetModel create(
            final TermModel term,
            final String key,
            final int idx) {
        List<TermModel> elts = new ArrayList<TermModel>();
        elts.add(term);
        elts.add(new ConstantValueModel(key));
        elts.add(new ConstantValueModel(idx));
        
        return new StructureGetModel(StructureGetFunOp.NAME.getFunctor().toString(), elts);
    }
    
    public StructureGetModel(String f, Collection<? extends TermModel> elts) {
        super(f, elts);

        if (elts.size() != 3)
            throw new RuntimeException("structGet expects exactly three parameters");
        
        getInputs().get(KEY_IDX).setName("field");
        getInputs().get(TERM_IDX).setName("input");
        
        key = (ConstantValueModel)getInputs().get(KEY_IDX).getTerm();
        idx = (ConstantValueModel)getInputs().get(IDX_IDX).getTerm();
        
        termParameter = getInputs().get(TERM_IDX);               
            
        if (termParameter.getTerm().getTypeDef() != null)
            setTypeDef(((StructDef)termParameter.getTerm().getTypeDef()).getFieldType(idx.getInt() - 1));
    }
    
    public List<String> getKeyOptions() {
        StructDef sdef = (StructDef)termParameter.getTerm().getTypeDef();
        List<String> opts = new ArrayList<String>();
        
        for (int i = 0; i < sdef.size(); i++)
            if (TypeUtilities.isAssignable(sdef.getFieldType(i), getTypeDef()))
                opts.add(sdef.getFieldName(i));        
        
        return opts; 
    }
    
    public String getKey() {
        return key.getString();
    }
    
    public void setKey(String key) {
        StructDef sdef = (StructDef)termParameter.getTerm().getTypeDef();
        
        this.key.setValue(key);
        
        int intIdx = sdef.getFieldNum(key);
        idx.setValue(intIdx);        
    }

    public String getDisplayString() {
        return "the " + key + " of "
                + getInputs().get(TERM_IDX).getTerm().getDisplayString();
    }
}
