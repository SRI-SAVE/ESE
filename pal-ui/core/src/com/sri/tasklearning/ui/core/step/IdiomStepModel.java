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
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.task.ATRAction;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.AbstractActionDef;
import com.sri.pal.IdiomDef;
import com.sri.pal.PALException;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.procedure.StepSequence;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;

public class IdiomStepModel extends ContainerStepModel implements ATRAction {
    private static final Logger log = LoggerFactory
            .getLogger(IdiomStepModel.class);
    
    private final StepSequence body;
    private /*final*/ IdiomDef def;
    
    public IdiomStepModel(
            final String functor,
            final Collection<? extends ATRTerm> arguments,
            final ATRTask body) {
        super(functor);        
        
        this.body = (StepSequence)body;
        this.stepType = StepType.IDIOM;
        
        List<StepModel> steps = new ArrayList<StepModel>();
        if (body != null)
            for (ATRTask task : this.body.getTasks())
                steps.add((StepModel)task);
        
        setSteps(steps);
        
        try { 
            if (functor != null && functor.length() > 0) {
                def = (IdiomDef) BackendFacade.getInstance().getType(TypeNameFactory.makeName(functor));
                initFromActionDefinition(def, arguments);
                // TODO Fix this if we try to merge in to trunk!!!
                //bindAndLockParameters();
            }
        } catch (PALException e) { 
            log.error("Exception occurred while creating IdiomStepModel: ", e);            
            return; 
        }
    }

    public AbstractActionDef getActionDefinition() {
        return def;
    }
    
    /********************* StepModel overrides ********************************/
    
    @Override
    public List<Object> getFancyName() {
        return fancyNameFromActionDefinition(def, "fancyName");
    }       
    
    /********************* ATRAction implementations **************************/
    
    @Override
    public StepSequence getBody() {
        // Unlike ActionStepModel, IdiomStepModel tracks a body
        return body;
    }
    
    @Override
    public List<TermModel> getElements() {
        List<TermModel> args = new ArrayList<TermModel>();
        for (ParameterModel pm : getInputs()) {
            args.add(pm.getTerm());
        }
        for (ParameterModel pm : getResults()) {
            args.add(pm.getTerm());
        }
        return args;
    }
    
    /********************* Object overrides ***********************************/
    
    @Override
    public String toString() {
        return "Idiom Step: " + getName() + "[" + getInputs() + " "
                + getResults() + "]";
    }
}
