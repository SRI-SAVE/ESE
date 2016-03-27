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

import java.util.List;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;

import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.SelectionManager;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;
import com.sri.tasklearning.ui.core.step.IStepViewContainer;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;

public abstract class CommonView extends Pane implements IStepViewContainer {

	protected SimpleBooleanProperty stepIndexVisibilityProperty = new SimpleBooleanProperty(true);
	protected SimpleIntegerProperty relativeIndex = new SimpleIntegerProperty(0);  
	protected SimpleIntegerProperty index = new SimpleIntegerProperty(0);  

	@Override
	public ContainerStepModel getContainerStepModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StepLayout getStepLayout() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateIssuesVisualization() {
		// TODO Auto-generated method stub

	}

	@Override
	public StepView findStepView(StepModel step) {
		// TODO Auto-generated method stub
		return null;
	}

	public EditController getController() {
		// TODO Auto-generated method stub
		return null;
	}

	public SelectionManager getSelectionManager() {
		// TODO Auto-generated method stub
		return null;
	}

	public ScrollPanePlus getScrollPane() {
		// TODO Auto-generated method stub
		return null;
	}

	public ContainerStepModel getModel() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isReadOnly() {
		// TODO Auto-generated method stub
		return false;
	}

	public void prepareToClose() {
		// TODO Auto-generated method stub

	}

	public void setReadOnly(Boolean newVal) {
		// TODO Auto-generated method stub

	}

	public ReadOnlyBooleanProperty readOnlyProperty() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean clearSelectionRect() {
		// TODO Auto-generated method stub
		return false;

	}

	public void moveSelectionRect(Point2D point2d) {
		// TODO Auto-generated method stub

	}

	public boolean startSelectionRect(Point2D point2d) {
		// TODO Auto-generated method stub
		return false;		
	}

	public void setReadOnly(boolean ro) {
		// TODO Auto-generated method stub

	}

	public void refreshResults() {
		// TODO Auto-generated method stub

	}

	public SimpleBooleanProperty getStepIndexVisibility() {		
		return stepIndexVisibilityProperty;		
	}

	public int getRelativeIndex() {
		return relativeIndex.getValue();
	}

	public SimpleIntegerProperty relativeIndexProperty() {    	
		return relativeIndex; 
	}

	public int getIndex() {
		return index.getValue();
	}

	public SimpleIntegerProperty indexProperty() {    	
		return index; 
	}

	public List<StepView> getSteps() {  
		StepLayout layout = getStepLayout(); 
		if (layout != null)
			return getStepLayout().getStepViews();
		else return null;
	}

}
