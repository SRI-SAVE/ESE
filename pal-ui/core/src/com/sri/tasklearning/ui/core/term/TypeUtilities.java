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

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.sri.ai.lumen.atr.term.impl.CTRLiteral;
import com.sri.pal.ActionModelDef;
import com.sri.pal.BagDef;
import com.sri.pal.Bridge;
import com.sri.pal.CollectionTypeDef;
import com.sri.pal.CustomTypeDef;
import com.sri.pal.EnumeratedTypeDef;
import com.sri.pal.ListDef;
import com.sri.pal.PALException;
import com.sri.pal.SetDef;
import com.sri.pal.TypeDef;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.ui.core.BackendFacade;

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


	private static HashMap<String, Set<String>> taxonomyDown = new HashMap<String, Set<String>> ();
	private static HashMap<String, Set<String>> taxonomyUp = new HashMap<String, Set<String>> ();

	private static Set<String> emptySet = new HashSet<String>(); 


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


	public static String getUnqualifiedTypeName(TypeName typeName) {

		return getUnqualifiedTypeName(typeName.getFullName().toString()); 

	}


	public static String getUnqualifiedTypeName(String typeName) {

		return typeName.substring(typeName.lastIndexOf('^')+1, typeName.length()); 

	}

	public static String getQualifiedTypeName(TypeName name) {

		String typeName = name.getFullName().toString();    	
		return typeName; 

	}

	//
	// MW additions - 2016 - 05 - 12 
	//

	public static void initializeTaxonomy(TypeDef type) {

		Deque<ActionModelDef> types = new LinkedList<ActionModelDef> ();

		types.add(type);

		while (! types.isEmpty()) {
			ActionModelDef type1 = types.pop(); 

			if (type1 instanceof EnumeratedTypeDef) {

				EnumeratedTypeDef type2 = (EnumeratedTypeDef) type1;  
				types.addAll(type2.getSubTypes()); 

				Set<String> down = taxonomyDown.get(type1.getName().getFullName()); 
				if (down == null) {
					down = new HashSet<String>();
					taxonomyDown.put(type1.getName().getFullName(), down); 
				}
				for (EnumeratedTypeDef subType : type2.getSubTypes()) { 
					down.add(subType.getName().getFullName());  			

					Set<String> up = taxonomyUp.get(subType.getName().getFullName()); 
					if (up == null) {
						up = new HashSet<String>();
						taxonomyUp.put(subType.getName().getFullName(), up); 
					}
					up.add(type1.getName().getFullName());  	

					initializeTaxonomy(subType); 

				}

				//
				// cache enums parent types 
				// 

				for (String value : type2.getValues()) {
					down.add(value);   				
					Set<String> up = taxonomyUp.get(value); 
					if (up == null) {
						up = new HashSet<String>();
						taxonomyUp.put(value, up); 
					}
					up.add(type2.getName().getFullName());  													
				}							

			}
		}
	}; 


	public static Set<String> getSuperTypes(TypeDef type) {
		return emptyIfNull(taxonomyUp.get( type.getName().getFullName())); 
	}

	public static Set<String> getSubTypes(TypeDef type) {
		return emptyIfNull(taxonomyDown.get( type.getName().getFullName())); 
	}

	public static Set<String> getEnumSuperTypes(Set<String> enums) {

		Deque<String> vals = new LinkedList<String> (enums);
		Set<String> res = new HashSet<String>(); 

		while (! vals.isEmpty()) {		
			String val = vals.pop(); 
			res.addAll( emptyIfNull (taxonomyUp.get(val))); 
		}

		return res; 		

	}

	public static Set<String> getPossibleValuesAllSiblings(Set<CTRLiteral> enums, ExerciseStepParameter param) {

		Set<ActionModelDef> types = new HashSet<ActionModelDef> ();
		
		for (CTRLiteral literal : enums) {
			ActionModelDef type = extractTypeFromLiteral(literal); 
			if (type != null)
				types.add(type); 			
		}
		
		types.addAll( param.getValueConstraintTypes (param.getValueConstraint()));  
		types.addAll( param.getValueConstraintTypes (param.getOriginalValueConstraint()));
		types.addAll( param.getValueConstraintTypes (param.getLoadedValueConstraint())); 
 		
		return getPossibleValuesAllSiblings(enums, types); 
			
	}
	
	 
	public static Set<String> getPossibleValuesAllSiblings(Set<CTRLiteral> enums, Set<ActionModelDef> types1) {

		Deque<ActionModelDef> types = new LinkedList<ActionModelDef> (types1); 
		HashSet<String> res = new HashSet<String>();  				
		
		while (! types.isEmpty()) {			
			ActionModelDef type = types.pop();
			if (type instanceof EnumeratedTypeDef) {
				EnumeratedTypeDef type2 = (EnumeratedTypeDef) type;  
				res.addAll( type2.getValues()); 			
			}
		}

		return res; 		

	}
	
	

	//
	//
	// 

	public static ActionModelDef extractTypeFromLiteral(CTRLiteral svalue) {

		return getTypeFromString( svalue.optType() ); 

	}

	public static ActionModelDef getTypeFromString(String ctrtype) {

		if (ctrtype != null) {
			return getTypeFromTypeName(TypeNameFactory.makeName(ctrtype)); 
		}

		return null; 

	}	

	public static ActionModelDef getTypeFromString2(String ctrtype) {

		try {
			Bridge bridge = BackendFacade.getInstance().getBridge();				
			ActionModelDef conceptType = TypeUtilities.getTypeFromString(ctrtype); 			
			return bridge.getActionModel().getType(conceptType.getName());
		} catch (PALException e1) {				
		}

		return null; 

	}

	public static ActionModelDef getTypeFromTypeName(TypeName typeName) {

		try {
			Bridge bridge = BackendFacade.getInstance().getBridge();				
			return bridge.getActionModel().getType(typeName);
		} catch (PALException e1) {				
		}

		return null; 

	}	

	public static ActionModelDef getTypeFromShortTypeName(String typeName, String version,  String namespace) {

		TypeName type = TypeNameFactory.makeName(typeName, version, namespace);  
		return getTypeFromTypeName(type); 

	}	

	public static Set<String> emptyIfNull(Set<String> set) {
		if (set == null)
			return emptySet ; 
		else 
			return set; 
	}

	public static boolean hasSubType(String string, String paramType) {
		
		boolean found = string.equals(paramType) ||
				emptyIfNull(taxonomyDown.get(string)).contains(paramType);

		if (found)
			return true;
		else
			for (String subType : emptyIfNull(taxonomyDown.get(string))) {
				found |= hasSubType(subType, paramType);
				if (found)
					return true; 						
			}

		return false; 

	}

}
