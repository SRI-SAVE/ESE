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

import com.sri.pal.BagDef;
import com.sri.pal.CollectionTypeDef;
import com.sri.pal.CustomTypeDef;
import com.sri.pal.ListDef;
import com.sri.pal.SetDef;
import com.sri.pal.TypeDef;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;

/**
 * Static utility methods for comparing types and extracting relevant pieces
 * of metadata. 
 */
public abstract class TypeUtilities {
    public static final String TYPE_MDK_NAME = "name";
    public static final String TYPE_MDK_A_NAME = "aName";
    public static final String TYPE_MDK_THIS_NAME = "thisName";
    public static final String TYPE_MDK_PLURAL_NAME = "pluralName";
    public static final String TYPE_MDK_EDITOR_DISALLOW_ASK_USER = "editor_disallowAskUser";
    public static final String TYPE_MDK_EDITOR_DISALLOW_FIXED_VALUE = "editor_disallowFixedValue";
    public static final String TYPE_MDK_EDITOR_DISALLOW_EXISTING_VALUE = "editor_disallowExistingValue";
    public static final String TYPE_MDK_PREVIEW_TEXT = "previewText";
    
    public static String getName(TypeDef type) {
        if (type == null)
            return null;
        
        String name = type.getMetadata(TYPE_MDK_NAME);
        
        if (name == null) {            
            TypeName tname = type.getName();        
            if (tname instanceof SimpleTypeName) 
                name = ((SimpleTypeName)tname).getSimpleName();
            else {
                CollectionTypeDef cdef = (CollectionTypeDef)type;
                name = "";
                
                if (cdef instanceof ListDef)
                    name += "list of "; 
                else if (cdef instanceof BagDef)
                    name += "bag of ";
                else if (cdef instanceof SetDef)
                    name += "set of ";;
                
                name += getPluralName(cdef.getElementType());
            }
        }
        
        return name; 
    }
    
    public static String getCollectionExplanationText(CollectionTypeDef type) {
        if (type instanceof ListDef)
            return "an ordered collection of " + 
                   getPluralName(type.getElementType()) + 
                   " that allows duplicates";
        else if (type instanceof SetDef)
            return "an unordered collection of " + 
                   getPluralName(type.getElementType()) + 
                   " that doesn't allow duplicates";
        else if (type instanceof BagDef)
            return "an unordered collection of " + 
            getPluralName(type.getElementType()) + 
            "that allows duplicates";
        else
            return "";
    }
    
    public static String getPluralName(TypeDef type) {
        if (type == null)
            return null; 
        
        String name = type.getMetadata(TYPE_MDK_PLURAL_NAME);
        
        if (name == null)
            name = getName(type) + "s";
        
        return name; 
    }
    
    public static String getAName(TypeDef type) {
        if (type == null)
            return null; 
        
        String name = type.getMetadata(TYPE_MDK_A_NAME);
        
        if (name == null)
            name = "a " + getName(type);
        
        return name; 
    }
    
    public static String getThisName(TypeDef type) {
        if (type == null)
            return null; 
        
        String name = type.getMetadata(TYPE_MDK_THIS_NAME);
        
        if (name == null)
            name = "this " + getName(type);
        
        return name; 
    }
    
    public static Object getDefaultValue(TypeDef type) {
        if (type == null)
            return null; 
        
        return type.getMetadata("default");
    }
    
    /**    
     * @param a 
     * @param b
     * @return true if type a is assignable to type b
     */
    public static boolean isAssignable(final TypeDef a, final TypeDef b) {
        // There shouldn't be null types in the editor, but just in case
        if (a == null || b == null)
            return false;
        
        return a.isAssignableTo(b);
    }
    
    /**
     * Determines if one instance of TypeDef is a super type of another. 
     * This implementation considers equivalent types in its calculation.
     *     
     * @param a
     * @param b
     * @return true if type a is a super type of b. 
     */
    public static boolean isSuperType(final TypeDef a, final TypeDef b) {
        if (a == null || b == null)
            return false;
        
        if (a.equals(b))
            return true;
        
        if (a.isEquivalentTo(b))
            return true;
        
        if (b instanceof CustomTypeDef) {
            CustomTypeDef bCust = (CustomTypeDef)b;
            CustomTypeDef parent = bCust;
            while ((parent = parent.getParentType()) != null) {
                if (parent.equals(a) || parent.isEquivalentTo(a))
                    return true;
            }
        }
        
        return false; 
   }
    
    public static boolean allowAskUser(final TypeDef def) {
        String disallow = 
            def.getMetadata(TYPE_MDK_EDITOR_DISALLOW_ASK_USER);
        
        if (disallow != null && disallow.equalsIgnoreCase("true"))
            return false;
        
        return true; 
    }
    
    public static boolean allowFixedValue(final TypeDef def) {
        String disallow = 
            def.getMetadata(TYPE_MDK_EDITOR_DISALLOW_FIXED_VALUE);
            
        if (disallow != null && disallow.equalsIgnoreCase("true"))
            return false;

        return true;
    }
    
    public static boolean allowExistingValue(final TypeDef def) {
        String disallow = 
            def.getMetadata(TYPE_MDK_EDITOR_DISALLOW_EXISTING_VALUE);
            
        if (disallow != null && disallow.equalsIgnoreCase("true"))
            return false;

        return true;
    }      
    

	public static String getUnqualifiedTypeName(TypeName name) {

		String typeName = name.getFullName().toString();    	
		return typeName.substring(typeName.lastIndexOf('^')+1, typeName.length()); 

	}

	public static String getQualifiedTypeName(TypeName name) {

		String typeName = name.getFullName().toString();    	
		return typeName; 

	}

}
