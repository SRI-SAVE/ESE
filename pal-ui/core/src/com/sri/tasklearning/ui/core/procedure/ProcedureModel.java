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

package com.sri.tasklearning.ui.core.procedure;

import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.task.ATRSequence;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.tasklearning.ui.core.BackendInterface;
import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.VariableManager;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.MapModel;

/**
 * Top-level class representing a procedure. Contains references to all
 * steps, inputs, outputs, etc that comprise a procedure through composition. 
 */
public class ProcedureModel extends CommonModel {
    // Functors that are needed to populate a procedure
    public static final String DISPLAY_NAME = "terse_task_description";
    public static final String DESCRIPTION = "description";
    private VariableManager varMgr;
    private SignatureModel signature;
    private final MapModel properties;
    private ProcedureEditController controller; 

    /**
     * Constructor for existing procedures. Utilized by 
     * {@link com.sri.tasklearning.ui.core.CoreUIModelFactory}.
     * 
     * @param sm the signature of the procedure (inputs/outputs & types info.)
     * @param taskExpression the steps of the procedure
     * @param props properties associated with the procedure 
     */
    public ProcedureModel(
            final SignatureModel sm, 
            final ATRTask taskExpression, 
            final MapModel props) {
        super(sm.getFunctor());        
        
        if (taskExpression instanceof ATRSequence) {
            int pos = 0;
            for (ATRTask t : ((ATRSequence) taskExpression).getTasks()) {
                addStep((StepModel) t, pos++);
            }
        } else
            addStep(((StepModel) taskExpression), 0);
        
        name.setValue(sm.getFunctor().substring(sm.getFunctor().lastIndexOf("^") + 1));
        setSignature(sm);
        this.properties = props;

        this.description = properties.getString(DESCRIPTION);
        varMgr = VariableManager.takeNextVariableManager(this);        
       
    }       
    
    /**
     * Constructor for creating new procedures
     */
    public ProcedureModel() {
        super("");

        setSignature(new SignatureModel()); 
        properties = new MapModel();
        varMgr = VariableManager.takeNextVariableManager(this); 
    }

    public void updateDescriptionInProperties() {
            ConstantValueModel descTermModel = new ConstantValueModel(description == null ? "" : description);
            properties.set(DESCRIPTION, descTermModel);
    }
    
    public MapModel getMetadata() {
        return properties; 
    }
    
    public VariableManager getVariableManager() {
        return varMgr; 
    }
    
    public void setName(String name) {
        this.name.setValue(name);
        if (signature != null)
            signature.setFunctor(BackendInterface.PROC_VERSIONED_NAMESPACE + name);
    }
    
    public ProcedureEditController getController() {
        return controller;
    }

    public void setController(ProcedureEditController controller) {
        this.controller = controller;
    }
    
    @Override
    public String getFunctor() {
        return signature.getFunctor(); 
    }

    @Override 
    public String getDescriptionText() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        updateDescriptionInProperties(); 
    }

    public SignatureModel getSignature() {
        return signature;
    }      

    private void setSignature(SignatureModel sm) {
        signature = sm;
        signature.setOwningActionDecl(this);
    }

    public MapModel getProperties() {
        return properties;
    }

    public ATRTask getExecute() {
        return stepsAsSequence();
    }

    public ATRLiteral getExecuteJ() {
        return null;
    }

    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }

    @Override
    public String toString() {
        return ATRSyntax.toSource(this);
    }
}
