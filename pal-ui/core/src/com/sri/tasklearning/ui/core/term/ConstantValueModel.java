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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.pal.PALException;
import com.sri.pal.PrimitiveTypeDef;
import com.sri.pal.ToStringFactory;
import com.sri.pal.TypeDef;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.ui.core.BackendFacade;

/**
 * Model that holds the state for a constant value. Note that 'constant'
 * collection and struct values are modeled by {@link CompositeTermModel}, 
 * while this class models atomic constant values. However, the atomic value
 * could be of a custom type that maps to a class that implements some sort
 * of complex structure in the client application, but from PAL's perspective
 * is still 'atomic'. 
 */
public class ConstantValueModel extends AtomicTermModel implements ATRLiteral {
    private static final Logger log = LoggerFactory
            .getLogger(ConstantValueModel.class);
    
    private Object obj = null;  

    /**
     * Create a default constant value model based on a type
     *
     * @param type the type of constant to make
     */
    
    public static ConstantValueModel createDefaultConstantValueModel(TypeDef type) {
        ConstantValueModel cvm = new ConstantValueModel(TypeUtilities.getDefaultValue(type));
        cvm.setTypeDef(type);
        return cvm; 
    }
    
    public ConstantValueModel() {
        
    }

    /** 
     * Special constructor for when we use ConstantValueModel as our 
     * implementation of ATRLiteral for purposes of implementing
     * ActionDeclaratoinParameterModel.getType(); In this case, the type
     * is the constant value!
     *  
     * @param type
     */
    public ConstantValueModel(TypeDef type) {
        obj = type.getName().getFullName();        
    }
    
    public ConstantValueModel(Object o) {
        this.obj = o;
    }
    
    public ConstantValueModel(Object o, String type) {
        this.obj = o;
        
        if (type != null) {
            TypeName name = TypeNameFactory.makeName(type);
            try {
                TypeDef td = (TypeDef)BackendFacade.getInstance().getType(name);
                setTypeDef(td);
            } catch (PALException e) {
                log.error("Failed to retrieve type for constant value: ", type);
            }
        }
        updateDisplayString();
    }
    
    public ConstantValueModel(Object o, TypeDef type) {
        this.obj = o;
        setTypeDef(type);
        updateDisplayString(); 
    }

    @Override
    public Object getValue() {
        return obj;
    }
    
    @Override
    public ConstantValueModel deepCopy() {
        ConstantValueModel copy = new ConstantValueModel();
        copy.setTypeDef(getTypeDef());
        
        if (getTypeDef() instanceof PrimitiveTypeDef) {
            // Some of the primitive type defs are mutable (ICal*) so we need
            // to create a copy. This code creates a 'copy' for all primitive
            // types although some are immutable and don't actually need it.
            String className = ((PrimitiveTypeDef)getTypeDef()).getKind().getRepresentationClass().getName();
            ToStringFactory factory = new ToStringFactory(className);
            copy.obj = factory.makeObject(obj.toString());
        } else  {
            // Enums and custom types always represented as instances of String,
            // which are immutable. Therefore we can just use a reference to
            // the current value for the 'copy'
            copy.obj = obj;               
        } 
        
        return copy; 
    }
    
    @Override
    public String optType() {
        // Although we may know the type of this literal to be a primitive, 
        // don't return a primitive type name from this method, otherwise it will
        // result in an incorrect typed() function call in the CTR-S for the lit
        if (getTypeDef() != null && !(getTypeDef() instanceof PrimitiveTypeDef))
            return getTypeDef().getName().getFullName();
        
        return null; 
    }
    
    public void setValue(Object obj) {
        this.obj = obj;
        updateDisplayString();
    }
    
    private void updateDisplayString() {
        if (obj != null) {
            String str = obj.toString();        
            displayStringProperty().setValue(str);
        }
    }

    @Override
    public int getInt() {
        return ((Integer)obj).intValue();
    }

    @Override
    public double getDouble(){
        return ((Double)obj).doubleValue();
    }

    @Override
    public String getString() {
        return obj.toString();
    }
    
    @Override
    public String getDisplayString() {
        if (obj != null)
            return obj.toString();
        else
            return null;
    }
    
    @Override
    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }

    public boolean equals(Object other) {
        if (other instanceof ConstantValueModel) {
            final ConstantValueModel oth = (ConstantValueModel)other;
            if ((obj != null && obj.equals(oth.obj)) ||
                (obj == null && oth.obj == null)) 
                return true;
        }
        return false;
    }    
    
    @Override
    public int hashCode() {
        return obj.hashCode();
    }

    @Override
    public String toString() {
        return getDisplayString();
    }
}
