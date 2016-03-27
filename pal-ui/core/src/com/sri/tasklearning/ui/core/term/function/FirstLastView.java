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

package com.sri.tasklearning.ui.core.term.function;

import javafx.scene.text.Text;

import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ParameterView;
import com.sri.tasklearning.ui.core.term.TypeUtilities;

/**
 * View for {@link FirstLastModel}. Consists of a label that indicates first
 * or last, followed by the element type of the collection (list), followed
 * by a TermView. 
 */
public class FirstLastView extends AccessorFunctionView {
    public FirstLastView(final FunctionModel argFuncModel,
                         final ParameterModel argParam,
                         final StepView argStepView,
                         final CommonView argProcView) {
        super(argFuncModel, argParam, argStepView, argProcView);
    }
    
    @Override 
    protected void createTermViews() {
        Text lbl = new Text("the " + funcModel.getFunctor() + " "
                + TypeUtilities.getName(termModel.getTypeDef()) + " in");
        lbl.setFont(LABEL_FONT);
        
        hbox.getChildren().add(lbl);
        
        for (ParameterModel pm : funcModel.getInputs()) {
            ParameterView pv = new ParameterView(pm, stepView, procView, true, false, null);
            hbox.getChildren().add(pv);
        }
    }
}
