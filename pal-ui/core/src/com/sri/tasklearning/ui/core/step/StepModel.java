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

package com.sri.tasklearning.ui.core.step;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.AbstractActionDef;
import com.sri.pal.CollectionTypeDef;
import com.sri.pal.PALException;
import com.sri.pal.StructDef;
import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.library.ActionModelAssistant;
import com.sri.tasklearning.ui.core.library.Namespace;
import com.sri.tasklearning.ui.core.resources.ResourceLoader;
import com.sri.tasklearning.ui.core.term.CompositeTermModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.validation.EditorIssue;

/**
 * Holds state common to all steps.
 */
public abstract class StepModel implements ATRTask, Comparable<StepModel> {
    private static final Logger log = LoggerFactory
            .getLogger(StepModel.class);
    public static final String DEFAULT_ICON_PATH = "gear.png"; 

	private SimpleObjectProperty<ContainerStepModel> container = new SimpleObjectProperty<ContainerStepModel>(); 
	
    protected SimpleStringProperty name = new SimpleStringProperty("");
    private String functor;
    protected Namespace namespace;
    protected String description ="";
    protected StepType stepType;
    private List<ParameterModel> inputs = new ArrayList<ParameterModel>();
    private List<ParameterModel> results = new ArrayList<ParameterModel>();
    // The model maintains the "topmost" error or warning associated with this step.    
    private List<EditorIssue> issues = new ArrayList<EditorIssue>();
    protected SimpleIntegerProperty index = new SimpleIntegerProperty(-1);
    private SimpleIntegerProperty relativeIndex = new SimpleIntegerProperty(-1);
    private final ReadOnlyBooleanWrapper highlighted = new ReadOnlyBooleanWrapper(false);
    
    private static final URL DEFAULT_ICON = ResourceLoader.getURL("gear.png");
    private URL iconPath = DEFAULT_ICON;
    
    public StepModel(String functor) {
        this.functor = functor;            
    }
    
    public String getFunctor() {
        return functor;
    }
    
    protected void setFunctor(String functor) {
        this.functor = functor;
    }
    
    public String getName() {
        return name.getValue();
    }
    
    public void setName(String name1) {
        name.setValue(name1);
    }
    
    public SimpleStringProperty nameProperty() {
        return name; 
    }
    
    public List<Object> getFancyName() {
        List<Object> fancy = new ArrayList<Object>();
        fancy.add(name);
        
        return fancy; 
    } 
    
    public StepType getStepType() {
        return stepType;
    }
    
    public Namespace getNamespace() {
        return namespace;
    } 

    public URL getIconPath() {
        return iconPath;
    }    

    protected void setIconPath(final URL iconPath) {
        if (iconPath == null)
            this.iconPath = DEFAULT_ICON;
        else
            this.iconPath = iconPath;
    }
    
    public String getDescription() {
        return description; 
    }
    
    public String getDescriptionText() {
        if (description == null || description.length() == 0)
            return getName();
        else
            return description; 
    }
    
    public ReadOnlyBooleanProperty highlightedProperty() {
        return highlighted.getReadOnlyProperty();
    }
    
    public boolean isHighlighted() {
        return highlighted.getValue();
    }
    
    public void setHighlighted(boolean hl) {
        highlighted.setValue(hl);
    }

    public List<EditorIssue> getIssues() {
        return issues;
    }

    public List<ParameterModel> getInputs() {
        return inputs;
    }

    protected void setInputs(final List<ParameterModel> inputArgs) {
        inputs.clear();
        inputs.addAll(inputArgs);
    }

    public List<ParameterModel> getResults() {
        return results;
    }
    
    protected void setResults(final List<ParameterModel> resultArgs) {
        results.clear();
        results.addAll(resultArgs);
        
        for (ParameterModel pm : results) 
            pm.setModality(ParameterModel.RESULT_FUNCTOR);        
    }
    
    @Override
    public int compareTo(StepModel other) {
        final int compareNames = getName().compareTo(other.getName());
        if (compareNames == 0) {
            // if the names match, compare the IDs             
            return getFunctor().compareTo(other.getFunctor());
        } else 
            return compareNames;
    }    
    
    //
    //
    // 
  
	public SimpleIntegerProperty indexProperty() {
		computeIndex(); 
        return index; 
    }   
	
	public int getIndex() {
		computeIndex();
		return index.getValue(); 
    }  
    
	public SimpleIntegerProperty relativeIndexProperty() {    	
		computeIndexRelativeToContainer();     	
		return relativeIndex; 
	}
	    	 
    public int getRelativeIndex() {
    	computeIndexRelativeToContainer();
        return relativeIndex.getValue();
    }         
     
    //
    //
    //
    
    protected ContainerStepModel findRootContainer() {
    	
    	if (container.getValue() != null) 
    		return (ContainerStepModel) (container.getValue().findRootContainer());
    	else if (this instanceof ContainerStepModel)
    		return (ContainerStepModel) this;
    	else
    		return null; 
	}
       
    public int computeIndex() {
    	
    	ContainerStepModel topContainer = findRootContainer();  
    	
    	int index1 = 0; 
    	
    	if (topContainer != null) { 
    		List<ActionStepModel> steps = topContainer.getFlattenedSteps();     	
    		for (ActionStepModel step : steps) {
    			step.index.set(index1++);    		
    		}
    	}
    	
    	return index.get();    
    }
    
    public int computeIndexRelativeToContainer() {		    	
    	int index = container.getValue().indexOf(this);    	
    	this.relativeIndex.setValue(index);
		return index; 
	}	
        

    /**
     * Test if this step references a variable.
     * 
     * @param variable the variable to test
     * 
     * @return true if this step references the given variable.
     */
    public final boolean referencesVariable(final VariableModel variable) {
        return findReferencesToVariable(variable).size() > 0;
    }
    
    public List<ParameterModel> findReferencesToVariable(final VariableModel variable) {
        List<ParameterModel> refs = new ArrayList<ParameterModel>();
        for (final ParameterModel r : results) 
            if (r.getTerm().equals(variable))
                refs.add(r);        
        
        for (final ParameterModel i : inputs)
            if (i.getTerm().equals(variable))
                refs.add(i);
            else  
                refs.addAll(i.getTerm().findReferencesToVariable(variable));
        
        return refs;
    }

    public void addIssue(final EditorIssue newIssue) {
        if (newIssue.isError())
            issues.add(0, newIssue); // Errors trump everything else
        else
            issues.add(newIssue); // Put non-errors at the end        
    }

    public void clearIssues() {
        issues.clear();
    }

    @Override
    public String toString() {
        return "Step: " + name + " [" + inputs + " " + results + "]";
    }

    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }

    public ATR getInternalSub() {
        return null;
    }
    
    protected void initFromActionDefinition(
            final AbstractActionDef actDef,
            final Collection<? extends ATRTerm> arguments) throws PALException {

        // See if it's a primitive action, and if so, get its metadata
        String n = actDef.getMetadata("name");
        name.setValue((n != null) ? n : actDef.getName().getSimpleName());

        String metaDesc = actDef.getMetadata(TypeDef.DESCRIPTION);
        if (metaDesc != null)
            this.description = metaDesc.trim();

        String ns = actDef.getName().getNamespace();
        String vers = actDef.getName().getVersion();
        
        namespace = ActionModelAssistant.getInstance().getNamespace(ns, vers);
        
        if (actDef.getMetadata("icon") != null) {
            String resourcePath = namespace.getResourcePath();
            if (resourcePath != null) {
                try {
                    URL icon = new URL(resourcePath + actDef.getMetadata("icon"));
                    setIconPath(icon);
                } catch (MalformedURLException e) {
                    log.error("Unable to create URL for icon: " + resourcePath + actDef.getMetadata("icon"));
                    setIconPath(null);
                }
            }
        } else
            setIconPath(namespace.getIcon());

        // Parse the inputs and results
        List<ParameterModel> ins = new ArrayList<ParameterModel>();
        List<ParameterModel> outs = new ArrayList<ParameterModel>();

        // parse all the arguments
        List<ATRTerm> argList;

        if (arguments != null)
            argList = new ArrayList<ATRTerm>(arguments);
        else
            argList = new ArrayList<ATRTerm>(0);

        for (int i = 0; i < actDef.size(); i++) {
            // get all the argument properties
            TypeDef argTypeDef = actDef.getParamType(i);

            // get the variable, or make a new placeholder var
            TermModel newVar;

            if (arguments != null && arguments.size() > i)            
                newVar = ((TermModel) argList.get(i));            
            else
                newVar = NullValueModel.NULL;           
            
            if (newVar instanceof CompositeTermModel &&
                (argTypeDef instanceof CollectionTypeDef || 
                 argTypeDef instanceof StructDef) && newVar.getTypeDef() == null) {
                ActionStepModel.setCollectionModelTypes((CompositeTermModel) newVar, argTypeDef);
            } else if (newVar.getTypeDef() == null)
                newVar.setTypeDef(argTypeDef);

            // Create the parameter
            ATRParameter.Modality mode = ATRParameter.Modality.UNKNOWN;
            ParameterModel pm = new ParameterModel(actDef.getParamName(i),
                    actDef.getParamDescription(i), argTypeDef, newVar,
                    mode.toString(), false, this);
            
            if (actDef.isInputParam(i))
                ins.add(pm);
            else {
                // This is where a variable actually gets bound. Mark it as such.
                if (pm.getTerm() instanceof VariableModel)
                    ((VariableModel)pm.getTerm()).setBound(true);
                outs.add(pm);
            }
        }

        setInputs(ins);
        setResults(outs);
    }
    
    public List<Object> fancyNameFromActionDefinition(
            final AbstractActionDef def,
            final String metaKey) {
        String argFlag = ParameterModel.ARGUMENT_FLAG;
        List<Object> ret = new ArrayList<Object>();
        List<String> tokens = new ArrayList<String>();
        
        String fancy = def.getMetadata(metaKey);
        if (fancy != null) {
            for (String s : Arrays.asList(fancy.split(",")))
                tokens.add(s);
        }

        for (String token : tokens) {
            if (token.startsWith(argFlag)) {
                // this is a ref. to an argument... so go find the argument
                boolean found = false;
                String argName = token.replaceFirst(argFlag, "");
                for (ParameterModel arg : getInputs()) {
                    if (arg.getName().equals(argName)) {
                        arg.setImportant(true);
                        ret.add(arg);
                        found = true;
                        break;
                    }
                }
                if (!found){
                    for (ParameterModel arg : getResults()) {
                        if (arg.getName().equals(argName)) {
                            arg.setImportant(true);
                            ret.add(arg);
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    log.warn("Parameter name reference '" + argName
                            + "' was unresolvable");
                    ret.add(argName);
                }
            } else
                ret.add(token);
        }

        return ret;
    }
    
	public void setContainer(ContainerStepModel container) {
		this.container.setValue(container); 
	}
    	   
	public SimpleObjectProperty<ContainerStepModel> getContainer() {    	
		return container;	
	 }
	
	public void updateIndices() {		
		computeIndex(); 		
	}
	
		 
}
