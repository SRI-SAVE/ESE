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

/**
 * View for {@link StructureGetModel}. Visualizes the struct being accessed as 
 * well as the specific field in that struct from which to return a value. 
 */
public class StructureGetView extends AccessorFunctionView {
    public StructureGetView(
            final FunctionModel argFuncModel,            
            final ParameterModel argParam,
            final StepView argStepView, 
            final CommonView argProcView) {
        super(argFuncModel, argParam, argStepView, argProcView);
    }
    
    @Override 
    protected void createTermViews() {
        Text lbl1 = new Text("the");
        Text lbl2 = new Text("of");
        lbl1.setFont(LABEL_FONT);
        lbl2.setFont(LABEL_FONT);
        ParameterView keyView = new ParameterView(funcModel.getInputs().get(
                StructureGetModel.KEY_IDX), stepView, procView, true, false, null);
        ParameterView mapView = new ParameterView(funcModel.getInputs().get(
                StructureGetModel.TERM_IDX), stepView, procView, true, false, null);
        
        hbox.getChildren().addAll(lbl1, keyView, lbl2, mapView);
    }
}
