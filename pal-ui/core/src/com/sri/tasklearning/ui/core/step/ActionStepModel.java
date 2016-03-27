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

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.task.ATRAction;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.AbstractActionDef;
import com.sri.pal.ActionDef;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.CollectionTypeDef;
import com.sri.pal.PALException;
import com.sri.pal.StructDef;
import com.sri.pal.TypeDef;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.CoreUIModelFactory;
import com.sri.tasklearning.ui.core.term.CompositeTermModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * Represents a typical action step which may have inputs and outputs. Contains
 * logic for inferring the type of variables based on their usage within this
 * action and the action's definition. 
 */
public class ActionStepModel extends StepModel implements ATRAction {
    private static final Logger log = LoggerFactory
            .getLogger(ActionStepModel.class);
    protected AbstractActionDef actDef = null;
    protected final ActionStreamEvent actionEvent;
    
    public ActionStepModel(
            final String functor,
            final Collection<? extends ATRTerm> arguments) {
        super(functor);
        
        actionEvent = null; 
        stepType = StepType.ACTION;
                
        try { 
            actDef = (ActionDef) BackendFacade.getInstance().getType(TypeNameFactory.makeName(functor));            
            if (actDef != null)
            	initFromActionDefinition(actDef, arguments);
            else 
            	log.error("Warning - cannot retrieve action definition for "+functor); 
        } catch (PALException e) {
            log.error("Exception occurred while creating ActionStepModel: ", e);            
            return; 
        }
    }
    
    public ActionStepModel(final ActionStreamEvent act) {
        super(act.getDefinition().getName().toString());
                        
        actDef = act.getDefinition();
        actionEvent = act;
        
        stepType = StepType.ACTION;
        
        List<TermModel> arguments = new ArrayList<TermModel>();
        CoreUIModelFactory factory = new CoreUIModelFactory();
        
        try {
            for (int i = 0; i < actDef.size(); i++) {
                Object value = act.getValue(i);
                TypeDef pType = act.getParamType(i);
                TermModel tm = (TermModel)pType.toAtr(factory, value);
                tm.setTypeDef(pType);                
                arguments.add((TermModel)pType.toAtr(factory, value));
            }
            initFromActionDefinition(actDef, arguments);
        } catch (PALException e) {
            log.error("Exception occurred while creating ActionStepModel: ", e);    
        }
    }

    public ActionStreamEvent getActionStreamEvent() {
        return actionEvent; 
    }
    
    public AbstractActionDef getActionDefinition() {
        return actDef;
    }

    /**
     * This method exists to support settings types on "inline" collections
     * (lists, sets, bags) which don't inherently have types. Instead, their
     * types are inferred from the action model. Type information needs to be
     * propagated recursively because of the case where have an action that
     * takes a list<list<bag<foo>>> and any or all of those levels contain
     * inline collections. This method is careful to not overwrite any existing
     * type value that may already be set since some CollectionModel subclasses
     * (MapGetModel, FirstLastModel) infer their own type information at the
     * time they are instantiated.
     * 
     * @param model
     *            The collection model that needs its type set and propagated
     *            down in to its elements recursively.
     * @param type
     *            The top-level type from the action model. As this method
     *            recurses, it will drill down in to this type as appropriate so
     *            that each nested collection gets its proper type.
     */
    public static void setCollectionModelTypes(
            final CompositeTermModel model, 
            final TypeDef type) {
        if (model.getTypeDef() == null)
            model.setTypeDef(type);
        
        int i = 0;
        for (ParameterModel input : model.getInputs()) {
            TypeDef elementType = null;
                    
            if (type instanceof CollectionTypeDef)
                elementType = ((CollectionTypeDef)type).getElementType();
            else if (type instanceof StructDef) {
                if (i == 0)
                    continue;
                elementType = ((StructDef)type).getFieldType(i - 1);
            }
                
            input.setTypeDef(elementType);
            
            final TermModel variable = input.getTerm();
            
            if (variable.getTypeDef() == null)
                variable.setTypeDef(elementType);
        
            if (variable instanceof CompositeTermModel &&
                (input.getTypeDef() instanceof CollectionTypeDef || 
                 input.getTypeDef() instanceof StructDef)) {
                setCollectionModelTypes((CompositeTermModel) variable, input.getTypeDef());
            }
            i++;
        }        
    }

    @Override
    public List<Object> getFancyName() {
        return fancyNameFromActionDefinition(actDef, "fancyName");
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

    @Override
    public ATRTask getBody() {
        return null;
    }
    
    @Override
    public String toString() {
        return "ActionStep: " + getName() + "[" + getInputs() + " "
                + getResults() + "]";
    }

    @Override
    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }

    @Override
    public ATR getInternalSub() {
        return new Wrapper(this);        
    }

}
