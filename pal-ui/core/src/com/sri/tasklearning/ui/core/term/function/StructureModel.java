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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.runtime.StructureGenFunOp;
import com.sri.pal.PALException;
import com.sri.pal.StructDef;
import com.sri.pal.TypeDef;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;

/**
 * Represents a structureGen function call, which is how structure instances are
 * represented in ATR. Fields of structures are accessed by the structureGet
 * function call. 
 */
public class StructureModel extends FunctionModel {
    private static final Logger log = LoggerFactory
            .getLogger(StructureModel.class);

    public StructureModel(String f, Collection<? extends TermModel> elts) {
        super(f, elts);
        
        ConstantValueModel lit = (ConstantValueModel)elts.iterator().next();
        TypeName name = TypeNameFactory.makeName(lit.toString());
        try {
            TypeDef type = (TypeDef)BackendFacade.getInstance().getType(name);
            setTypeDef(type);
        } catch (PALException e) {
            log.error("Failed to load structure type: " + name);
        }
    }
    
    /**
     * Constructor to be used when creating a new struct within the editor
     * @param type
     */
    public StructureModel(StructDef type) {
        super(StructureGenFunOp.NAME_STRING, new ArrayList<TermModel>());
        
        ATRParameter.Modality mode = ATRParameter.Modality.INPUT;
        
        ConstantValueModel cvm = new ConstantValueModel(type.getName().getFullName());
        ParameterModel typeParam = new ParameterModel(null, null, null, cvm, mode.toString(), false, this);
        
        getInputs().add(typeParam);
        
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
                && getInputs().size() == sdef.size() + 1)
            for (int i = 0; i < sdef.size(); i++) {
                getInputs().get(i + 1).setName(sdef.getFieldName(i));
                getInputs().get(i + 1).setDescription(sdef.getFieldName(i));
            }        
    }
    
    private String generateDisplayString(boolean truncate) {
        StringBuffer buff = new StringBuffer("[");
        StructDef structType = (StructDef)getTypeDef();
        
        int i = 0;
        for (ParameterModel pm : getInputs()) {
            if (i > 0) {
                buff.append(structType.getFieldName(i - 1) + ": ");
                if (truncate)
                    buff.append(getTruncatedDisplayString(pm.getTerm()) + ", ");
                else
                    buff.append(pm.getTerm().getDisplayString() + ", ");
            }
            i++;
        }
        
        String ret = buff.substring(0, buff.length() -2) + "]";               

        return ret;
    }
    
    @Override
    public String getDisplayString() {
        return generateDisplayString(false);
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
                            ret.append(getTruncatedDisplayString(getInputs().get(idx + 1).getTerm()));    
                    } else
                        ret.append(piece);
                } else
                    ret.append(piece);
            }
        } else
            return generateDisplayString(true); 
        
        return ret.toString();
    }
}
