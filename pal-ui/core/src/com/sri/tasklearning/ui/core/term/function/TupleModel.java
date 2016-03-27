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

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.pal.StructDef;
import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;

/**
 * Represents a positionalTupleGen function call, which is currently used as 
 * the canonical way to represents structs. Fields of a positionalTupleGen 
 * are accessed with nth. 
 */
public class TupleModel extends FunctionModel {    
    public TupleModel(String f, Collection<? extends TermModel> elts) {
        super(f, elts);
    }
    
    /**
     * Constructor to be used when creating a new struct within the editor
     * @param type
     */
    public TupleModel(StructDef type) {
        super("positionalTupleGen", new ArrayList<TermModel>());
        
        ATRParameter.Modality mode = ATRParameter.Modality.INPUT;
        
        for (int i = 0; i < type.size(); i++) {
            ParameterModel pm = new ParameterModel(null, null,
                    type.getFieldType(i), NullValueModel.NULL, mode.toString(),
                    false, this);          
   
            getInputs().add(pm);
        }
        
        setTypeDef(type);
    }
    
    @Override
    public void setTypeDef(TypeDef argType) {
        StructDef sdef = (StructDef)argType;
        
        super.setTypeDef(argType);
        
        if (getTypeDef() != null && getInputs() != null
                && getInputs().size() == sdef.size())
            for (int i = 0; i < sdef.size(); i++) {
                getInputs().get(i).setName(sdef.getFieldName(i));
                getInputs().get(i).setDescription(sdef.getFieldName(i));
            }        
    }
    
    @Override
    public String getDisplayString() {
        StringBuffer buff = new StringBuffer("[");
        StructDef structType = (StructDef)getTypeDef();
        
        int i = 0;
        for (ParameterModel pm : getInputs()) {
            buff.append(structType.getFieldName(i) + ": ");
            buff.append(pm.getTerm().getDisplayString() + ", ");
            i++;
        }
        
        String ret = buff.substring(0, buff.length() -2) + "]";

        return ret;
    }
    
    @Override
    public String getPreviewText() {
        StructDef sdef = (StructDef)getTypeDef();
        String meta = sdef.getMetadata(TypeUtilities.TYPE_MDK_PREVIEW_TEXT);
        StringBuffer ret  = new StringBuffer();
        String flag = ParameterModel.ARGUMENT_FLAG;
        
        if (meta != null && meta.length() > 0) {
            String[] pieces = meta.split(",");
            for (String piece : pieces) {
                if (piece.startsWith(flag)) {
                    String fieldName = piece.substring(flag.length());
                    if (fieldName.length() > 0) {
                        int idx = sdef.getFieldNum(fieldName);
                        if (idx >= 0)
                            ret.append(getTruncatedDisplayString(getInputs().get(idx).getTerm()));    
                    } else
                        ret.append(piece);
                } else
                    ret.append(piece);
            }
        }
        
        return ret.toString();
    }
}
