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

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.pal.CollectionTypeDef;
import com.sri.pal.PALException;
import com.sri.pal.StructDef;
import com.sri.pal.TypeDef;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.VariableManager;
import com.sri.tasklearning.ui.core.step.ActionStepModel;

/**
 * A model that represents a parameter to an action. In our case, the action
 * will always be a procedure thus we're dealing with procedure parameters, 
 * which may be inputs or outputs (published results). 
 */
public class ActionDeclarationParameterModel extends ParameterModel 
    implements ATRParameter {

    private TermModel defaultValue;
    private static final Logger log = LoggerFactory
            .getLogger(ActionDeclarationParameterModel.class);

    public ActionDeclarationParameterModel (final VariableModel argVar, 
                                            final Modality md, 
                                            final String typeName, 
                                            final TermModel defaultValue, 
                                            final boolean fromFactory) {
        VariableModel vm = argVar;

        try {
            type = (TypeDef) BackendFacade.getInstance().getType(TypeNameFactory.makeName(typeName));
            
            if (defaultValue instanceof CompositeTermModel &&
                    (type instanceof CollectionTypeDef || 
                     type instanceof StructDef))
                ActionStepModel.setCollectionModelTypes((CompositeTermModel)defaultValue, type);
            
            if (defaultValue != null)
                defaultValue.setTypeDef(type); 
            
            vm.setTypeDef(type);
        } catch (PALException e) {
            log.error("Exception occurred while retreiving type for" +
                      "procedure argument", e);
        }
        
        String modal;
        
        if (md.equals(Modality.INPUT)) {
            modal = INPUT_FUNCTOR;
            // If this is an input, it needs to be made into a procedure input 
            // variable model
            if (fromFactory) {
                VariableManager varMgr = VariableManager.getNextVariableManager();
                varMgr.unmanageVariable(vm);                
                vm = new ProcedureInputVariableModel(vm.getVariableName(),
                        defaultValue);
                vm.setTypeDef(type);
                varMgr.manageVariable(vm);
            }
        } 
        else if (md.equals(Modality.OUTPUT))
            modal = RESULT_FUNCTOR;        
        else
            modal = "?";      
        
        this.modality = modal;        
        this.setTerm(vm);
        this.defaultValue = defaultValue;
    }
    
    public String toString() {
        return "$" + getTerm().getDisplayString() + "=" + defaultValue;
    }

    @Override
    public VariableModel getVariable() {
        return (VariableModel)super.getTerm();
    }
    
    public Modality getMode() {
        if ( modality.equals(INPUT_FUNCTOR))
            return Modality.INPUT;
        else if ( modality.equals(RESULT_FUNCTOR))
            return Modality.OUTPUT;
        
        return Modality.UNKNOWN;
    }
    
    public boolean isProcedureInput() {
        return modality.equals(INPUT_FUNCTOR);
    }
    
    public boolean isExerciseInput() {
        return modality.equals(INPUT_FUNCTOR);
    }
    
    @Override
    public TermModel getDefaultValue() {
        return defaultValue;
    }
    
    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }
    
    public ATR getInternalSub() {
        return null;
    }
    
    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }
}
