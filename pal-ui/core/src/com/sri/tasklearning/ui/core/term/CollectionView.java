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
 * Term view for inline collections and structs. Inline refers collections/
 * structs that are declared inline in a procedure and are not being 
 * referenced through a variable. They are basically constant values for 
 * collections and structs. Their menu buttons appear similarly to instances
 * of {@link ConstantValueView} since they both represent constant values. 
 */
public class CollectionView extends MenuButtonTermView {
    protected CompositeTermModel collModel;
    
    public CollectionView(
            final CompositeTermModel argModel,
            final ParameterModel argParamModel,
            final StepView argStepView,
            final CommonView argProcView) {
        super(argModel, argParamModel, argStepView, argProcView);
        
        collModel = argModel;
        
        configureButton();
    }
    
    @Override
    protected void configureButton() {
        button.setText(collModel.getPreviewText());
        super.configureButton();
        
        button.getStyleClass().clear(); 
        button.getStyleClass().add("constant-var");        
    }       
}
