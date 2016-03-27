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

import java.util.Collection;
import java.util.List;

import com.sri.tasklearning.ui.core.layout.StepLayout;

import javafx.beans.property.SimpleBooleanProperty; 
import javafx.beans.property.SimpleIntegerProperty;

/**
 * A common interface for views that contain step views
 */
public interface IStepViewContainer {
	
	public SimpleBooleanProperty getStepIndexVisibility(); 
    public ContainerStepModel getContainerStepModel();
    public StepLayout getStepLayout(); 
    public void updateIssuesVisualization();
    public StepView findStepView(StepModel step);
    public List<StepView> getSteps(); 
    
}
