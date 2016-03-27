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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.common.InputVariableModel;
import com.sri.tasklearning.ui.core.term.ActionDeclarationParameterModel;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableModel;

/**
 * The signature of a procedure including the names and types of its inputs and
 * outputs. Provides change detection through {@code isSignatureChanged} for
 * purposes of detecting breaking changes in procedure dependencies. 
 */
public class SignatureModel extends com.sri.tasklearning.ui.core.common.SignatureModel {
    
	public SignatureModel() {
		 super();
	 }
	 
	 public SignatureModel(String f,
	            Collection<ActionDeclarationParameterModel> params) {
		 super(f, params);
	 }
	        	 
    public ProcedureModel getOwningActionDecl() {
        return (ProcedureModel) owningActionDecl;
    }

    public void setOwningActionDecl(ProcedureModel owningActionDecl) {
        this.owningActionDecl = owningActionDecl;
    }
    

  
}
