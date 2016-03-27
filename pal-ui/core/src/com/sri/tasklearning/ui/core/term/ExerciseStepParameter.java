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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javafx.beans.property.SimpleBooleanProperty;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.impl.CTRLiteral;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.pal.CustomTypeDef;
import com.sri.pal.EnumeratedTypeDef;
import com.sri.pal.TypeDef;
import com.sri.pal.training.core.exercise.EqualityConstraint;
import com.sri.pal.training.core.exercise.Parameter;
import com.sri.pal.training.core.exercise.Value;
import com.sri.pal.training.core.exercise.ValueConstraint;
import com.sri.tasklearning.ui.core.common.InputVariableModel;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.step.ExerciseCreateStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepModel;

/**
 * Extension of VariableModel for exercise inputs, which support the concept
 * of default values. 
 */
public class ExerciseStepParameter extends InputVariableModel {

	// owner
	private ExerciseStepModel owner; 
	// owner ExerciseModel
	private ExerciseModel ownerExerciseModel; 
	// original parameter computed and cached on demand  
	private ExerciseStepParameter original; 

	private List<String> possibleValues; 
	private Parameter parameter;
	
	// sub-parameter have a parent
	private ExerciseStepParameter parent;
	// and conversely: 
	private List<ExerciseStepParameter> subParameters = new LinkedList<ExerciseStepParameter>();  

	private ValueConstraint vc;
	private EqualityConstraint ec;
	
	//
	private SimpleBooleanProperty changedFromOriginal = new SimpleBooleanProperty(false); 	
	
	private boolean valueConstraintRemoved = false;
	private boolean equalityConstraintRemoted = false;
	
	// result of following the equality constraint
	private String equalTo = null;   
		
	// current literals from value constraints on this parameter
	private List<CTRLiteral> currentValueConstraintLiterals = new LinkedList<CTRLiteral>();
	
	public ExerciseStepParameter(ExerciseModel model, Parameter p, ExerciseStepParameter parent, ValueConstraint vc, EqualityConstraint ec) {

		super();           

		this.vc = vc;
	
		
		this.ec = ec; 
		this.parameter = p; 
		this.parent = parent;
		this.ownerExerciseModel = model; 
		
		this.original = getOriginalParameter(); 
		
		if (parent != null)
			parent.subParameters.add(this); 

		//
		//
		// 
		
		if (ec != null) {			
			if (ec.getParameters().get(0).equals(p.getId())) 
				equalTo = ec.getParameters().get(1); 
			else 
				equalTo = ec.getParameters().get(0);								
		}
		
		//
		//
		// 

		if (vc != null) {
			currentValueConstraintLiterals = getValueConstraintLiterals(vc);
		}
	
		// 
		//
		// 		
		
	}
	
	public List<CTRLiteral> getValueConstraintLiterals(ValueConstraint vc) {

		Set<CTRLiteral> valueConstraintLiterals = new HashSet<CTRLiteral>(); 
	
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

	public void initializeParameterDescription() {
			
		if (vc == null) {

			setVariableName( getParameterDescription() );						

		} else {			

			if ( ! currentValueConstraintLiterals.isEmpty()) {

				boolean first = true; 
				String description = currentValueConstraintLiterals.size() > 1 ? "{ " : "";   			
				for (CTRLiteral lit : currentValueConstraintLiterals) {
					if (! first) 
						description += ", ";  
					description += lit.getValue();
					first = false; 
				}
				
				description += currentValueConstraintLiterals.size() > 1 ? " }" : ""; 
				setVariableName(description); 

			} else {

				setVariableName(this.getParameter().getId());
				
			}
		}
		
	
	}

	public String getParameterDescription() {
		
		if (this.getParent() != null) {
			
			return "the " + this.parameter.getAccessor() + " of " + this.getParent().getParameterDescription();
			
		}  else if (this.equalTo != null) {
			
			// return "the " + this.parameter.getAccessor() + " of " + this.ownerExerciseModel.lookupParameter(this.equalTo).getParameterDescription();
			
			return this.ownerExerciseModel.lookupParameter(this.equalTo).getParameterDescription();
			
		} else if (this.owner instanceof ExerciseCreateStepModel) {
			
			return "the " + ((ExerciseCreateStepModel) this.owner).getCreatedObject();

		} else			

			return "the " + this.getParameter().getAccessor();
	}

	@Override
	public boolean isBound() {
		return true;
	}

	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public List<CTRLiteral> getCurrentSelection() { 
		return currentValueConstraintLiterals;  
	}
		
	public void setCurrentSelection(Object currentSelection) {					
	
		List<CTRLiteral> newCurrentValueConstraintLiterals = new LinkedList<CTRLiteral>();
		
		ExerciseStepParameter orig = getOriginalParameter();		
		
		if (currentSelection == null) {		
			
			// "Any" Type menu item was selected
					
			this.valueConstraintRemoved = true;
			this.equalityConstraintRemoted = true;		
			
			if (vc != null) 
				ownerExerciseModel.registerValueConstraintAsInvalid(vc);
			if (ec != null)
				ownerExerciseModel.registerEqualityConstraintAsInvalid(ec);		
			
			this.equalTo = null; 

		} else if (currentSelection == this) {
			
			// original value from equality constraint on orig parameter was selected 
			
			equalityConstraintRemoted = false;
			
			if (ec == null) {
				ec = new EqualityConstraint();
				ec.getParameters().addAll(orig.ec.getParameters());
				ownerExerciseModel.getSolutionOption().getEqualityConstraints().add(ec);
			}
					
			ownerExerciseModel.registerEqualityConstraintAsValid(ec);			
			equalTo = orig.equalTo; 			
			
		} else  {		
			
			if (vc == null) {
				vc = new ValueConstraint();
				vc.setParameter(this.getParameter().getId()); 
			}
		
			// Enum value was selected 
			
			this.valueConstraintRemoved = false;
			this.equalityConstraintRemoted = false;
			
			if (vc != null)
				ownerExerciseModel.registerValueConstraintAsValid(vc);
			if (ec != null)
				ownerExerciseModel.registerEqualityConstraintAsValid(ec);
			
			boolean found = false; 
			for (CTRLiteral a : currentValueConstraintLiterals) {
				if ( a.getValue().toString().equals(currentSelection) ) {
					found = true; 
				} else
					newCurrentValueConstraintLiterals.add(a);
			}
		
			//
			//
			//
			
			String type = TypeUtilities.getQualifiedTypeName(this.getTypeDef().getName());
			String ctrs = "typed(\"" +currentSelection + "\", \"" + type + "\")";   
			
			if ( ! found ) {
				
				// add 
				
				ATRTerm newTerm;
				
				try {
					newTerm = ATRSyntax.CTR.termFromSource(ctrs);
					if ( newTerm instanceof CTRLiteral) {
						CTRLiteral newLit = (CTRLiteral) newTerm; 
						newCurrentValueConstraintLiterals.add(newLit); 
					}
				} catch (LumenSyntaxError e) {
					e.printStackTrace();
				}			
			}		
		}
			
		currentValueConstraintLiterals = newCurrentValueConstraintLiterals; 	
		
	}

	public List<String> getPossibleValues() {    	    

		// use a set to ensure no duplication possible
		Set<String> ret = new HashSet<String>(); 
		
		TypeDef type = this.getTypeDef(); 

		if (possibleValues == null) {
			possibleValues = new LinkedList<String>();
			if (type instanceof EnumeratedTypeDef) {	    	
				for (String svalue: ((EnumeratedTypeDef) type).getValues()) {
					possibleValues.add(svalue); 
				}
			}
		}
		
		for (String val : possibleValues)
			ret.add(val); 
		
		if (this.original != null)
			for (CTRLiteral svalue: this.original.currentValueConstraintLiterals) 
				ret.add(svalue.getString());

		return new LinkedList<String>(ret); 

	}

	public ExerciseStepParameter getParent() {
		return parent;
	}
	
	public List<ExerciseStepParameter> getSubParameters() {
		return subParameters;
	}

	public ExerciseStepModel getOwner() {
		return owner;
	}

	public void setOwner(ExerciseStepModel owner) {
		this.owner = owner;
	}
	
	public Parameter getParameter() {
		return parameter;
	}

	public boolean isValueConstraintRemoted() {
		return valueConstraintRemoved;
	}

	public void setValueConstraintRemoted(boolean valueConstraintRemoted) {
		this.valueConstraintRemoved = valueConstraintRemoted;
	}

	public boolean isEqualityConstraintRemoted() {
		return equalityConstraintRemoted;
	}

	public void setEqualityConstraintRemoted(boolean equalityConstraintRemoted) {
		this.equalityConstraintRemoted = equalityConstraintRemoted;
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
		
			if ( this.equalTo == null && original.equalTo != null ) 
				return true; 
			
			if ( this.equalTo != null && original.equalTo == null ) 
				return true; 
										
			if ( this.vc == null && original.vc != null) 
				return true;
	
			if ( this.vc != null && original.vc == null) 
				return true;
	
			// both have value constraints -
			// check if the current selection is different from the original one
			// first compute the original selection
				
			if (currentValueConstraintLiterals.size() != original.currentValueConstraintLiterals.size())
				return true; 
			
			boolean equal = true; 
			for (CTRLiteral a : currentValueConstraintLiterals) {
				boolean found = false; 
				for (CTRLiteral b : original.currentValueConstraintLiterals) {
					if (a.getValue().toString().equals(b.getValue().toString())) {
						found = true; 
						break;
					}
				}
				if ( ! found ) {
					equal = false; 
					break;
				}
			}
			
			return !equal; 
			
		}
		
	}

	public boolean valueIsSelected(String value) {	
		for (CTRLiteral a : currentValueConstraintLiterals) {
			if ( a.getValue().toString().equals(value) ) 
				return true; 
		}		
		return false; 
	}

	public String getButtonLabel() {
		initializeParameterDescription(); 			
		changedFromOriginal.setValue(differentFromOriginalParameter()); 		
		return getVariableName(); 		
	}

	public ValueConstraint getValueConstraint() {
		return vc;
	}

	public EqualityConstraint getEqualityConstraint() {
		return ec;
	}

	public SimpleBooleanProperty getChangeFromOriginalProperty() {
		initializeParameterDescription(); 			
		changedFromOriginal.setValue(differentFromOriginalParameter());
		return changedFromOriginal;
	}

}