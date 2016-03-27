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

import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.step.StepView;

/**
 * View for a {@link NullValueModel}. Appears like a constant value with a little
 * warning icon to inform the user they may want to specify a value where this
 * term is located. 
 * 
 * TODO: Once we finish nullable types, I think we should only display the 
 * warning icon if we encounter a null value on a non-nullable type. 
 */
public class NullValueView extends MenuButtonTermView {
    
    public NullValueView(
            final NullValueModel argVarModel, 
            final ParameterModel argParamModel,
            final StepView argStepView,
            final CommonView argProcView) {       
        super(argVarModel, argParamModel, argStepView, argProcView);
        configureButton();
    }
    
    @Override
    protected void configureButton() {
        super.configureButton();
        
        button.getStyleClass().clear(); 
        button.getStyleClass().add("constant-var");

        /*
        ImageView iv = Utilities.getImageView("warning_small.png");
        iv.setPreserveRatio(true);
        iv.setFitHeight(DEFAULT_HEIGHT);
        iv.setFitWidth(DEFAULT_HEIGHT);
        
        button.setGraphic(iv);*/
    }
}
