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

package com.sri.tasklearning.ui.core.common;

import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;

public abstract class CommonModel extends ContainerStepModel implements ATRActionDeclaration {

	public CommonModel(String functor) {
		super(functor);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ATRMap getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	 public SignatureModel getSignature() {
	        return null;
	}      
	 
	@Override
	public ATRTask getExecute() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ATRLiteral getExecuteJ() {
		// TODO Auto-generated method stub
		return null;
	}


	public void setController(EditController editController) {
		// TODO Auto-generated method stub
		
	}

}
