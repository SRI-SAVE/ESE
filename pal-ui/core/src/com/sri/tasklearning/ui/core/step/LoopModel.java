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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.core.LumenConstant;
import com.sri.ai.lumen.core.Sym;
import com.sri.ai.lumen.atr.task.ATRForin;
import com.sri.ai.lumen.atr.task.ATRSequence;
import com.sri.pal.CollectionTypeDef;
import com.sri.pal.PALException;
import com.sri.pal.TypeDef;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.VariableManager;
import com.sri.tasklearning.ui.core.resources.ResourceLoader;
import com.sri.tasklearning.ui.core.term.CompositeTermModel;
import com.sri.tasklearning.ui.core.term.ListModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.function.ZipModel;

/**
 * Model representing a loop step, which contains other steps as children.
 * A loop has an input list term and iterand term. Note that these terms 
 * are not necessarily variables, particularly in the case of parallel iteration 
 * where they are function calls. A loop may also have an accumulator (into) 
 * term and collect term for purposes of accumulating values output by a step
 * in the loop. 
 */
public class LoopModel extends ContainerStepModel implements ATRForin {
    private static final Logger log = LoggerFactory
        .getLogger(LoopModel.class);
    
    /** The lumen functor for a loop step. */
    public static final Sym FUNCTOR = LumenConstant.SYM_forall;   
    
    private final ParameterModel inputList;    
    private final ParameterModel iterand;
    private final ParameterModel into;
    private final ParameterModel collect; 

    public LoopModel(
            final TermModel argIterand, 
            final TermModel argInputList, 
            final List<StepModel> theSteps,
            final TermModel argCollect,
            final TermModel argInto) {
        super(FUNCTOR.toString());
        
        stepType = StepType.LOOP;                
        
        name.setValue("Repeat for each item in a collection");
        description = "Repeat the following steps for each item in the given collection.";
        
        setIconPath(ResourceLoader.getURL("loop-icon1.png"));        
                
        inputList = new ParameterModel(
                "Input collection",
                "The collection to loop over", 
                null, argInputList, null, true, this);

        iterand = new ParameterModel("Item", null, null, argIterand,
                ParameterModel.RESULT_FUNCTOR, true, this);     
        
        collect = new ParameterModel(
                "Collect variable", 
                "The variable to collect while looping (optional)",
                null, argCollect, null, true, this);           
        
        into = new ParameterModel("Accumulator", null, null, argInto, null,
                true, this);       
                
        if (theSteps != null)
            setSteps(theSteps);
        
        if (argInputList != null) {            
            TypeDef listType = argInputList.getTypeDef();

            if (listType != null) {
                inputList.setTypeDef(listType);               
                iterand.setTypeDef(((CollectionTypeDef) argInputList
                        .getTypeDef()).getElementType());
                argIterand.setTypeDef(iterand.getTypeDef());
            }
            
            // Handle type assignment for parallel iteration
            if (iterand.getTerm() instanceof ListModel &&
                inputList.getTerm() instanceof ZipModel) {
                ZipModel zip = ((ZipModel)inputList.getTerm());
                ListModel list = ((ListModel)iterand.getTerm());
                int idx = 0;
                for (ParameterModel term : zip.getInputs()) {
                    ParameterModel iter = list.getInputs().get(idx);
                    
                    if (!(term.getTerm().getTypeDef() instanceof CollectionTypeDef))
                        continue;
                    
                    CollectionTypeDef cType = (CollectionTypeDef) term
                            .getTerm().getTypeDef();
                    if (cType == null)
                        continue;
                    
                    iter.setTypeDef(cType.getElementType());
                    iter.getTerm().setTypeDef(cType.getElementType());
                    idx++;
                }
            }
        }
        
        if (collect.getTerm() != null &&
            collect.getTerm().getTypeDef() != null) {            
            into.getTerm().setTypeDef(getAccumulatorType(collect.getTerm()));        
        }
        
        refreshVariableBindStatus();
    }
    
    public void refreshVariableBindStatus() {
        if (into.getTerm() instanceof VariableModel)
            ((VariableModel)into.getTerm()).setBound(true);
        
        if (iterand.getTerm() instanceof VariableModel)
            ((VariableModel)iterand.getTerm()).setBound(true);
        else if (iterand.getTerm() instanceof CompositeTermModel)
            refreshVariableBindStatusAux((CompositeTermModel)iterand.getTerm());
    }
    
    private void refreshVariableBindStatusAux(CompositeTermModel cterm) {
        for (ParameterModel pm : cterm.getInputs())
            if (pm.getTerm() instanceof VariableModel)
                ((VariableModel)pm.getTerm()).setBound(true);
            else if (pm.getTerm() instanceof CompositeTermModel)
                refreshVariableBindStatusAux((CompositeTermModel)pm.getTerm());
    }
    
    public static TypeDef getAccumulatorType(final TermModel collectTerm) {
        try {
            // Accumulator variables are always lists of whatever the
            // type of the collect variable is
            TypeName accumName = TypeNameFactory.makeName("list<"
                    + collectTerm.getTypeDef().getName().getFullName() + ">");
            return (TypeDef) BackendFacade.getInstance().getType(accumName);
        }
        catch (PALException e) {
            log.error("Error deriving accumulator type", e);
            return null; 
        }    
    }

    public ParameterModel getInputCollection() {
        return inputList;
    }
    
    public ParameterModel getIterand() {
        return iterand; 
    }
    
    public ParameterModel getCollect() {
        return collect;
    }
    
    public ParameterModel getInto() {
        return into; 
    }
    
    @Override
    public List<ParameterModel> getResults() {
        List<ParameterModel> ret = new ArrayList<ParameterModel>();
        if (into.getTerm() != null)
            ret.add(into);
        return ret; 
    }
    
    public void configInputCollection(
            final TermModel newCollection,
            final VariableManager varMgr) {
        final TermModel oldCollection = getInputCollection().getTerm();
        CollectionTypeDef newCDef = (CollectionTypeDef)newCollection.getTypeDef();
        
        // If the collections have the same element type the operation is easy
        if (oldCollection != null) {
            CollectionTypeDef oldCDef = (CollectionTypeDef)oldCollection.getTypeDef();    
        
            if (oldCDef != null && oldCDef.getElementType().equals(newCDef.getElementType())) {
                getInputCollection().setTerm(newCollection);
                return; 
            }
        }
        
        // Otherwise, we have to create a new iterand
        TypeDef eleType = ((CollectionTypeDef) newCollection.getTypeDef())
                .getElementType();
        String typeName = TypeUtilities.getName(eleType);
        String varName = varMgr.createValidName(typeName);
        VariableModel iter = VariableModel.create(varName, varMgr);

        iterand.setTerm(iter);
        iterand.setTypeDef(eleType);
        iter.setTypeDef(eleType);
            
        inputList.setTerm(newCollection);        

        iterand.setTypeDef(((CollectionTypeDef) newCollection.getTypeDef())
                .getElementType());        
        
        return; 
    }
    
    public void configCollect(
            final TermModel newCollect,
            final VariableManager varMgr) {
        
        final TermModel oldCollect = collect.getTerm();
        
        getCollect().setTerm(newCollect);
        
        // If the types of the old/new collect are the same, the operation is
        // easy
        if (oldCollect != null && 
            newCollect != null &&
            oldCollect.getTypeDef().equals(newCollect.getTypeDef())) {
            return;        
        }
        
        if (newCollect == null) {
            into.setTerm(null);
            return;
        }
        
        // Otherwise, we have to create a new accumulator variable
        String typeName = TypeUtilities.getPluralName(newCollect.getTypeDef());
        String name = varMgr.createValidName("collected_" + typeName);
        VariableModel accum = VariableModel.create(name, varMgr);
        accum.setTypeDef(getAccumulatorType(newCollect));
        
        into.setTerm(accum);
    }
    
    @Override
    public List<Object> getFancyName() {
        String itemName = TypeUtilities.getName(iterand.getTypeDef());
        if (itemName == null)
            itemName = "item";
        
        String end = "for each " + itemName + " in";
        String front;
        
        if (getIndex() >= 0 && getSteps().size() > 0) {
            if (getSteps().size() > 1) 
                front = "Repeat steps " + (getIndex() + 1 + 1) + "\u2013"
                        + (getIndex() + 1 + getSteps().size());
            else 
                front = "Repeat step " + (getIndex()+1 + 1);
        }
        else 
            front = "Repeat";        
        
        List<Object> fancyNameObjs = new ArrayList<Object>();
        fancyNameObjs.add(front);
        fancyNameObjs.add(end);
        fancyNameObjs.add(inputList);
        
        return fancyNameObjs;
    }       

    @Override 
    public ATRSequence getBody() {
        return stepsAsSequence();
    }

    @Override 
    public TermModel getLoopList() {
        return inputList.getTerm();
    }

    @Override
    public TermModel getCollectTerm() {
        return collect.getTerm();
    }    

    @Override 
    public TermModel getIntoTerm() {
        return into.getTerm();
    }

    @Override
    public TermModel getLoopTerm() {
        return iterand.getTerm();
    }

    @Override
    public List<ParameterModel> findReferencesToVariable(VariableModel variable) {
        List<ParameterModel> refs = new ArrayList<ParameterModel>();
                
        if (variable.equals(iterand.getTerm()))
            refs.add(iterand);
        else
            refs.addAll(iterand.getTerm().findReferencesToVariable(variable));
        
        if (into.getTerm() != null) {
            if (variable.equals(into.getTerm()))
                refs.add(into);
            else
                refs.addAll(into.getTerm().findReferencesToVariable(variable));
        }
        
        if (variable.equals(inputList.getTerm()))
            refs.add(inputList);
        else
            refs.addAll(inputList.getTerm().findReferencesToVariable(variable));
        
        if (collect.getTerm() != null) {
            if (variable.equals(collect.getTerm()))
                refs.add(collect);
            else
                refs.addAll(collect.getTerm().findReferencesToVariable(variable));
        }
        
        return refs;              
    }
}

