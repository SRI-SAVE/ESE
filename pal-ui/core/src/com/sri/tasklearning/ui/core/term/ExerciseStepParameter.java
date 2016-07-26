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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.impl.CTRLiteral;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.pal.ActionModelDef;
import com.sri.pal.TypeDef;
import com.sri.pal.training.core.exercise.EqualityConstraint;
import com.sri.pal.training.core.exercise.Parameter;
import com.sri.pal.training.core.exercise.TypeConstraint;
import com.sri.pal.training.core.exercise.Value;
import com.sri.pal.training.core.exercise.ValueConstraint;
import com.sri.tasklearning.ui.core.common.InputVariableModel;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepModel;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Pair;

/**
 * Extension of VariableModel for exercise inputs, which support the concept
 * of default values. 
 */
public class ExerciseStepParameter extends InputVariableModel {

	//
	//
	//

	public static final Set<String> NUMERIC_RANGE_TYPES = new HashSet<String>(); 
	{ NUMERIC_RANGE_TYPES.add("integer");
	  NUMERIC_RANGE_TYPES.add("float");
	  NUMERIC_RANGE_TYPES.add("quantity");
	  NUMERIC_RANGE_TYPES.add("Integer");
	  NUMERIC_RANGE_TYPES.add("Float");
	  NUMERIC_RANGE_TYPES.add("Quantity");
	}
	
	public static final Set<String> ENUM_RANGE_TYPES = new HashSet<String>(); 
	{ ENUM_RANGE_TYPES.add("quantity");
	  ENUM_RANGE_TYPES.add("Quantity");
	}
	
	//
	// taxonomy initialization root types 
	//

	private static boolean initialized = false;	
	private static String[] rootTypes = { "cooking_am^0.1^Concept", "cooking_am^0.1^Thing" };

	private static String version = "0.1";
	private static String am = "cooking_am"; 
	private static Set<CTRLiteral> emptyCTRLiterals = new HashSet<CTRLiteral>();  

	// owner
	private ExerciseStepModel owner; 
	// owner ExerciseModel
	private ExerciseModel ownerExerciseModel; 
	// original parameter computed and cached on demand  
	private ExerciseStepParameter original; 

	Parameter parameter;


	@Override
	public boolean equals(Object other) {
		if (other instanceof ExerciseStepParameter)
			return parameter.getId().equals(((ExerciseStepParameter) other).parameter.getId());  
		else 
			return false; 
	}

	@Override
	public int hashCode() {
		return parameter.getId().hashCode();
	}
	
	
	// the current value, equality and type constraints 
	// 

	private ValueConstraint vc;
	private EqualityConstraint ec;
	private TypeConstraint tc;

	// 
	// the value, equality, and type constraints that were active when the 
	// file was loaded - these are NOT the "original" ones. the original   
	// constraints are available via the original parameter, and this is
	// what was demonstrated.
	// 

	private ValueConstraint lvc;  
	private EqualityConstraint lec;
	private TypeConstraint ltc; 

	//	
	//
	//

	private SimpleBooleanProperty changedFromOriginal = new SimpleBooleanProperty(false);

	//
	//
	//

	private HashSet<CTRLiteral> allValueConstraintLiterals;
	private HashSet<String> allTypeConstraints; 	

	//
	//
	// 

	public ExerciseStepParameter(ExerciseModel model, Parameter p, ExerciseStepParameter parent, ValueConstraint vc, EqualityConstraint ec, TypeConstraint tc) {

		super();           

		this.lvc = vc;		
		this.lec = ec;
		this.ltc = tc; 

		this.vc = vc;		
		this.ec = ec;
		this.tc = tc; 

		this.parameter = p; 
		this.ownerExerciseModel = model; 

		this.original = getOriginalParameter();
		
	}

	//
	//
	// 

	public String getEqualParameter() { 

		if (ec != null) 
			if (ec.getParameters().get(0).equals(parameter.getId())) 
				return ec.getParameters().get(1); 			
	
		return null;
		
	}		

	public Set<ExerciseStepParameter> getEqualByValueParameters() {
			
		Stream<ExerciseStepParameter> params = ownerExerciseModel.getExerciseStepParameters().stream();				
		return (Set<ExerciseStepParameter>) params.filter(x -> x.isEqualParameter(this) && x != this).collect(Collectors.toSet());	
		
	}		
	
	
	
	public Set<ActionModelDef> getValueConstraintTypes(ValueConstraint vc) {

		Set<ActionModelDef> types = new HashSet<ActionModelDef>(); 

		if (vc != null) {
			for (Value v : vc.getValues())  
				types.add(TypeUtilities.getTypeFromString(v.getType()));

			if (vc.getMinValue() != null)  
				types.add(TypeUtilities.getTypeFromString(vc.getMinValue().getType()));

			if (vc.getMaxValue() != null) 
				types.add(TypeUtilities.getTypeFromString(vc.getMaxValue().getType()));

		}

		return types;  

	}

	public List<CTRLiteral> getValueConstraintLiterals(ValueConstraint vc) {

		Set<CTRLiteral> valueConstraintLiterals = new HashSet<CTRLiteral>(); 

		if (vc != null)

			for (Value v : vc.getValues())  {

				try {

					ATRTerm term = ATRSyntax.CTR.termFromSource(v.getCtrs());

					if ( term instanceof CTRLiteral) {
						CTRLiteral lit = (CTRLiteral) term; 
						valueConstraintLiterals.add(lit); 
					}

				} catch (LumenSyntaxError e) {
					e.printStackTrace();
				}
			}

		return new LinkedList<CTRLiteral>(valueConstraintLiterals); 

	}
	
	
	public CTRLiteral getValueConstraintMin(ValueConstraint vc) {

		if (vc != null && vc.getMinValue() != null ) 

			try {

				ATRTerm term = ATRSyntax.CTR.termFromSource(vc.getMinValue().getCtrs());

				if ( term instanceof CTRLiteral) {
					CTRLiteral lit = (CTRLiteral) term; 
					return lit; 
				}	

			} catch (LumenSyntaxError e) {
				e.printStackTrace();
			}	
		
		return null; 

	}

	
	public CTRLiteral getValueConstraintMax(ValueConstraint vc) {

		if (vc != null && vc.getMaxValue() != null)

			try {

				ATRTerm term = ATRSyntax.CTR.termFromSource(vc.getMaxValue().getCtrs());

				if ( term instanceof CTRLiteral) {
					CTRLiteral lit = (CTRLiteral) term; 
					return lit; 
				}	

			} catch (LumenSyntaxError e) {
				e.printStackTrace();
			}		
		
		return null; 

	}

	//
	//
	//

	public void initializeParameterDescription() {
		
		initializeParameterDescription(true); 
		
	}

	public void initializeParameterDescription(boolean firstTime) {				

		if (! initialized ) {
			for (String rootType : rootTypes) 
				TypeUtilities.initializeTaxonomy( (TypeDef) TypeUtilities.getTypeFromString(rootType));
			initialized = true;
		}		

		String equalPar = getEqualParameter();

		if (equalPar != null && firstTime ) {

			if ( vc != null )
				System.out.println("This parameter is declared to be equal to " + equalPar + " - ignoring value constraint " + vc + " on " + this.getParameter().getId()); 			
			if ( tc != null )
				System.out.println("This parameter is declared to be equal to " + equalPar + " - ignoring type constraint " + tc + " on " + this.getParameter().getId());
			if ( lvc != null )
				System.out.println("This parameter is declared to be equal to " + equalPar + " - ignoring loaded value constraint " + lvc + " on " + this.getParameter().getId()); 			
			if ( ltc != null )
				System.out.println("This parameter is declared to be equal to " + equalPar + " - ignoring loaded type constraint " + ltc + " on " + this.getParameter().getId()); 

			vc = null; 
			tc = null;			

			List<ExerciseStepParameter> params = ownerExerciseModel.getExerciseStepParameters();

			for (ExerciseStepParameter other : params) {				
				if (other.getParameter().getId().equals(equalPar)) {

					if (other.vc != null) 
						vc = copyValueConstraint(other.vc); 

					if (other.tc != null)
						tc = copyTypeConstraint(other.tc); 			

					if (other.lvc != null) 
						lvc = copyValueConstraint(other.lvc);

					if (other.ltc != null)
						ltc = copyTypeConstraint(other.ltc);			

					ec = null;

					break; 

				}
			}										
		} 

		if (vc == null && tc == null ) {

			setVariableName(this.getParameter().getId());
			return;

		};

		if (vc != null ) {			

			boolean first = true; 

			if ( getCurrentValueConstraintLiterals().size() > 1) {				
				String description = "{ "; 

				for (CTRLiteral lit : getCurrentValueConstraintLiterals()) {
					if (! first) 
						description += ", ";  
					description += lit.getValue();
					first = false; 
				}

				description += " }";  								
				setVariableName(description);

			} else {

				String min = this.getCurrentValueConstraintMinLiteral() != null ? this.getCurrentValueConstraintMinLiteral().getString() : null; 
				String max = this.getCurrentValueConstraintMaxLiteral() != null ? this.getCurrentValueConstraintMaxLiteral().getString() : null; 

				String description;

				if (min != null)
					if (max != null) 
						if (min.equals(max))
							description = "exactly " + min;
						else 
							description = "between " + min + " and " + max;
					else 
						description = "greater than " + min; 
				else 
					if (max != null)
						description = "smaller than " + max;
					else 
						if (!getCurrentValueConstraintLiterals().isEmpty()) 
							description = getCurrentValueConstraintLiterals().get(0).getString();
						else description = this.getParameter().getAccessor().toString(); 

				setVariableName(description);
			}

			return;

		} 

		setVariableName(TypeUtilities.getUnqualifiedTypeName(tc.getType().get(0))); 

	}


	private TypeConstraint copyTypeConstraint(TypeConstraint tc2) {
	
		TypeConstraint tc1 = new TypeConstraint(); 
		tc1.setParameter(this.getParameter().getId()); 	
		tc1.getType().addAll(tc2.getType());
		
		return tc1;
		
	}

	private ValueConstraint copyValueConstraint(ValueConstraint vc2) {
	
		ValueConstraint vc1 = new ValueConstraint();
		vc1.setParameter(this.getParameter().getId()); 	
		vc1.getValues().addAll(vc2.getValues());
		
		return vc1;
		
	}

	@Override
	public boolean isBound() {
		return true;
	}


	public void setCurrentSelection(Pair<Integer, Integer> currentSelection) {										
		
		ValueConstraint ovc = vc; 
		vc = new ValueConstraint();
		vc.setParameter(this.getParameter().getId());

		if (currentSelection.getKey().equals(currentSelection.getValue())) {
			
			Value val = new Value(currentSelection.getKey().toString(), "integer");
			vc.getValues().add(val); 
			
		} else {
					
			Value minVal = new Value(currentSelection.getKey().toString(), "integer");
			Value maxVal = new Value(currentSelection.getValue().toString(), "integer");
		
			vc.setMinValue(minVal);
			vc.setMaxValue(maxVal);
		}
		
		// also kill the type constraint, as a range is given!
		tc = null; 
	}

	
	public void setCurrentSelection(Pair<String, String> currentSelection, String type) {		
		
		// used for min / max range intervals 
		
		ValueConstraint ovc = vc; 
		vc = new ValueConstraint();
		vc.setParameter(this.getParameter().getId());
		
		if (currentSelection.getKey().equals(currentSelection.getValue())) {
			
			Value val = new Value("\""+ currentSelection.getKey() + "\"", type);
			vc.getValues().add(val); 
			
		} else {
					
			Value minVal = new Value("\""+ currentSelection.getKey() + "\"",  type); 
			Value maxVal = new Value("\"" + currentSelection.getValue() + "\"" , type); 
		
			vc.setMinValue(minVal);
			vc.setMaxValue(maxVal);
		}
		
		// also kill the type constraint, as a range is given!
		tc = null;
	}

	
	public void setCurrentSelection(Object currentSelection, boolean typeSelected) {							

		if (typeSelected) {

			vc = null;

			String type = currentSelection.toString();
			// String type = currentSelection.toString().substring(4); // cut off heading "Any "

			boolean selected = enumValueOrTypeIsSelected(type); 

			if (selected) {

				tc = null;

			} else {			

				ActionModelDef type1 = TypeUtilities.getTypeFromShortTypeName(type, version, am);
				List<String> types = new LinkedList<String>(); 
				types.add(type1.getName().getFullName()); 								

				tc = new TypeConstraint(this.getParameter().getId(), 
						types);

			}

		}  else  {					

			String type = TypeUtilities.getQualifiedTypeName(this.getTypeDef().getName());
			String ctrs = "typed(\"" +currentSelection + "\", \"" + type + "\")";   				

			tc = null;

			boolean selected = enumValueOrTypeIsSelected(currentSelection.toString()); 			
			List<Value> newValues = new LinkedList<Value>();

			if (vc != null) {

				List<Value> currentValues = vc.getValues();

				for (Value val : currentValues) {
					// I have to look for substring, because of 
					// type("Burner", "cooking_am^0.1^Equipment") vs. typed("Burner", "cooking_am^0.1^Thing") ! 
					if ( val.getCtrs().indexOf(currentSelection.toString()) == -1) {
						newValues.add(val); 															
					}
				}
			}

			vc = new ValueConstraint();
			vc.setParameter(this.getParameter().getId());

			if (! selected ) {
				Value newVal = new Value(ctrs, type); 
				newValues.add(newVal);
			}

			vc.getValues().clear();
			vc.getValues().addAll(newValues);   									

		}	
	}
	
	public boolean isEqualParameter(ExerciseStepParameter other) {
		
		return this.getButtonLabel().equals(other.getButtonLabel()); 
		
	}

	//
	//
	//

	public boolean isRangeParameter() {
	
		if (vc != null ) {			

			String min = this.getCurrentValueConstraintMinLiteral() != null ? this.getCurrentValueConstraintMinLiteral().getString() : null; 
			String max = this.getCurrentValueConstraintMaxLiteral() != null ? this.getCurrentValueConstraintMaxLiteral().getString() : null; 
			
			if (min != null || max != null)
				return true; 
			
			Set<String> types = getPossibleEnumTypes(false);	
			Stream<String> stream = types.stream();
			
			// Stream<String> stream2 = types.stream();
			// stream2.map(x -> TypeUtilities.getUnqualifiedTypeName(x)).forEach(x -> System.out.println("Element " + x));					
					
			return stream.map(x -> TypeUtilities.getUnqualifiedTypeName(x))
					     .anyMatch(x -> NUMERIC_RANGE_TYPES.contains(x));
						
		}
				
		return false;
 
	}	
		
    //
	//
	// 
		
	
	public Set<String> getPossibleEnumValues() {    	    

		allValueConstraintLiterals = new HashSet<CTRLiteral>();  

		allValueConstraintLiterals.addAll(emptyIfNull(getLoadedValueConstraintLiterals()));
		allValueConstraintLiterals.addAll(emptyIfNull(getCurrentValueConstraintLiterals()));
		
		if (getCurrentValueConstraintMinLiteral() != null) 
			allValueConstraintLiterals.add(getCurrentValueConstraintMinLiteral()); 
	
		if (getCurrentValueConstraintMaxLiteral() != null) 
			allValueConstraintLiterals.add(getCurrentValueConstraintMaxLiteral()); 
		
		if (original != null) {
			original.getPossibleEnumValues();		
			allValueConstraintLiterals.addAll( original.allValueConstraintLiterals);
		}

		return TypeUtilities.getPossibleValuesAllSiblings(allValueConstraintLiterals, this); 		

	}

	private Collection<? extends CTRLiteral> emptyIfNull(List<CTRLiteral> literals) {
		if (literals == null)
			return emptyCTRLiterals;
		else 
			return literals;
	}

	public Set<String> getPossibleEnumTypes(boolean filter) {    	    

		allTypeConstraints = new HashSet<String>(); 

		for (String superType : TypeUtilities.getEnumSuperTypes(getPossibleEnumValues())) {
			if ( !filter || ! ExerciseStepParameter.isBadType(superType)) {						
				allTypeConstraints.add(superType); 
			}
		}

		String paramType = getTypeDef().getName().getFullName();

		if ( ! ExerciseStepParameter.isBadType(paramType)) {		
			allTypeConstraints.add(paramType);			
		}

		if (original != null) {
			original.getPossibleEnumTypes();		
			allTypeConstraints.addAll( original.allTypeConstraints);
		}

		if (ltc != null) 
			allTypeConstraints.add(ltc.getType().get(0));
		if (tc != null) 
			allTypeConstraints.add(tc.getType().get(0));
		if (original != null && original.tc != null)  
			allTypeConstraints.add(original.tc.getType().get(0));

		return allTypeConstraints; 

	}
	
	public Set<String> getPossibleEnumTypes() {    	    

		return getPossibleEnumTypes(true);
		
	}


	//
	//
	//

	public ExerciseStepModel getOwner() {
		return owner;
	}

	public void setOwner(ExerciseStepModel owner) {
		this.owner = owner;
	}

	public Parameter getParameter() {
		return parameter;
	}


	public ExerciseStepParameter getOriginalParameter() {

		if (original == null) {		
			if (ownerExerciseModel.getOriginalExerciseModel() != null) {
				original = ownerExerciseModel.getOriginalExerciseModel().lookupParameter(this.parameter.getId()); 
			}
		}

		return original; 

	}

	public boolean differentFromOriginalParameter() {

		ExerciseStepParameter original = getOriginalParameter(); 

		if (original == null)  

			return false; 

		else {

			if (vc != null) {
				
				if (getCurrentValueConstraintLiterals().size() != getOriginalValueConstraintLiterals().size())
					return true;
				
				for (CTRLiteral cur : getCurrentValueConstraintLiterals()) {
					boolean found = false; 		
					for (CTRLiteral orig : getOriginalValueConstraintLiterals()) {
						 
						if ( cur.getString().equals(orig.getString()) ) {
							found = true; 				
							break; 
						}
					}
					
					if (! found) {					
						return true; 
					}
				}				
								
				boolean changedMinMax = 
						this.getCurrentValueConstraintMinLiteral() != null && original.getCurrentValueConstraintMinLiteral() == null ||
						this.getCurrentValueConstraintMinLiteral() == null && original.getCurrentValueConstraintMinLiteral() != null ||
						this.getCurrentValueConstraintMinLiteral() != null && original.getCurrentValueConstraintMinLiteral() != null && 
						! this.getCurrentValueConstraintMinLiteral().getString().equals(original.getCurrentValueConstraintMinLiteral().getString()) || 
						this.getCurrentValueConstraintMaxLiteral() != null && original.getCurrentValueConstraintMaxLiteral() == null ||
						this.getCurrentValueConstraintMaxLiteral() == null && original.getCurrentValueConstraintMaxLiteral() != null ||
						this.getCurrentValueConstraintMaxLiteral() != null && original.getCurrentValueConstraintMaxLiteral() != null && 
						! this.getCurrentValueConstraintMaxLiteral().getString().equals(original.getCurrentValueConstraintMaxLiteral().getString());						
						
				if (changedMinMax) 
					return true; 										

			} else if (tc != null) {
				if ( original.tc == null)
					return true; 
				else  
					return ! tc.getType().get(0).equals(original.tc.getType().get(0)); 
			} 
			
						
			return (tc == null && original.tc != null);

		}
	}

	public CTRLiteral getCurrentValueConstraintMinLiteral() { 
		return getValueConstraintMin(vc); 
	}
	
	public CTRLiteral getCurrentValueConstraintMaxLiteral() { 
		return getValueConstraintMax(vc); 
	}
	
	public List<CTRLiteral> getCurrentValueConstraintLiterals() { 
		return getValueConstraintLiterals(vc); 
	}


	public List<CTRLiteral> getLoadedValueConstraintLiterals() { 
		return getValueConstraintLiterals(lvc); 
	}


	public List<CTRLiteral> getOriginalValueConstraintLiterals() { 
		return getValueConstraintLiterals(original.vc); 
	}


	public boolean enumValueOrTypeIsSelected(String valueOrType) {	

		for (CTRLiteral a : getCurrentValueConstraintLiterals()) {
			if ( a.getValue().toString().equals(valueOrType) ) 
				return true; 
		}

		if (tc != null) { 
			return tc.getType().get(0).contains(valueOrType);
		}

		return false; 
	}

	//
	//
	//

	public String getButtonLabel() {
		initializeParameterDescription(false); 			
		changedFromOriginal.setValue(differentFromOriginalParameter());
		return getVariableName(); 		
	}

	//
	//
	// 

	public ValueConstraint getValueConstraint() {
		return vc;
	}

	public EqualityConstraint getEqualityConstraint() {
		return ec;
	}

	public TypeConstraint getTypeConstraint() {
		return tc;
	}
	
	//
	//
	//

	public ValueConstraint getLoadedValueConstraint() {
		return lvc;
	}

	public EqualityConstraint getLoadedEqualityConstraint() {
		return lec;
	}

	public TypeConstraint getLoadedTypeConstraint() {
		return ltc;
	}

	//
	//
	// 

	public ValueConstraint getOriginalValueConstraint() {
		if (original != null)
			return original.vc;
		else
			return null; 
	}

	public EqualityConstraint getOriginalEqualityConstraint() {
		if (original != null)
			return original.ec;
		else
			return null; 	
	}

	public TypeConstraint getOriginalTypeConstraint() {
		if (original != null)
			return original.tc;
		else
			return null; 	
	}

	//
	//
	// 


	public SimpleBooleanProperty getChangeFromOriginalProperty() {
		initializeParameterDescription(false); 			
		changedFromOriginal.setValue(differentFromOriginalParameter());
		return changedFromOriginal;
	}

	//
	//
	//

	public static boolean isBadType(String paramType) {

		return ArrayUtils.contains(rootTypes, paramType) || 				
				TypeUtilities.hasSubType("cooking_am^0.1^Concept", paramType);
	}


}