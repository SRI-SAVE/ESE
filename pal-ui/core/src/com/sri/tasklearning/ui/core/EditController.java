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

package com.sri.tasklearning.ui.core;

import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Scene;
import javafx.util.Callback;

import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;

public abstract class EditController implements IUndoWatcher {

	@Override
	public void onUndoChanged(IUndoable undoable) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUndoCleared() {
		// TODO Auto-generated method stub

	}

	public void setController(EditController editController) {
		// TODO Auto-generated method stub
		
	}

	public CommonModel getModel() {
		// TODO Auto-generated method stub
		return null;
	}

	public UndoManager getUndoManager() {
		// TODO Auto-generated method stub
		return null;
	}

	public CommonView getView() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isUnsavedChanges() {
		// TODO Auto-generated method stub
		return false;
	}

	public SimpleIntegerProperty numStepsWithErrorsProperty() {
		// TODO Auto-generated method stub
		return null;
	}

	public SimpleIntegerProperty numStepsWithWarningsProperty() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<StepModel> getStepsWithIssues() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean selectStep(StepModel target) {
		return false;
		// TODO Auto-generated method stub
		
	}

	public IUndoable deleteStep_impl(StepView theStep) {
		return null;
		// TODO Auto-generated method stub
		
	}

	public VariableManager getVariableManager() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean renameReplaceVariable(VariableModel variable, String newName) {
		return false;
		// TODO Auto-generated method stub
		
	}

	public boolean renameVariable(VariableModel variable, String newName) {
		return false;
		// TODO Auto-generated method stub
		
	}

	public List<FunctionModel> getSuggestedFunctionCalls(StepModel stepModel,
			ParameterModel input) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void deleteStep(StepView addedStep) {
		// TODO Auto-generated method stub
		
	}

	public void addStep(StepView addedStep, StepLayout stepLayout, int newIndex) {
		// TODO Auto-generated method stub
		
	}

	public void validate(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public IUndoable addStep_impl(StepView ghost, StepLayout dropLayout,
			int dropIndex) {
				return null;
		// TODO Auto-generated method stub
		
	}

	public void deleteSteps(List<StepView> delSteps) {
		// TODO Auto-generated method stub
		
	}

	public void moveSteps(List<StepView> multiDragStepViews,
			StepLayout origLayout, StepLayout dropLayout, int dropIndex) {
		// TODO Auto-generated method stub
		
	}

	public void moveStep(StepView originalStep, StepLayout origLayout,
			StepLayout dropLayout, int dropIndex) {
		// TODO Auto-generated method stub
		
	}

	public void changeArgument(ParameterModel argument, ConstantValueModel cvm,
			StepModel stepModel, boolean nested) {
		// TODO Auto-generated method stub
		
	}

	public boolean changeArgument(ParameterModel argument, TermModel atrValue,
			StepModel step, boolean nested) {
				return nested;
		// TODO Auto-generated method stub
		
	}	
	
	public void unhighlightVariable(VariableModel varModel) {
		// TODO Auto-generated method stub
		
	}

	public void highlightVariable(VariableModel varModel) {
		// TODO Auto-generated method stub
		
	}

	public SimpleBooleanProperty unsavedChangesProperty() {
		// TODO Auto-generated method stub
		return null;
	}

	public void attemptSave(boolean allowSaveAs, boolean forceSaveAs,
			Callback<CommonModel, Void> onSuccess, Scene scene) {
		// TODO Auto-generated method stub
		
	}

	public void unhighlightSteps() {
		// TODO Auto-generated method stub
		
	}

	public void attemptRename(Callback<CommonModel, Void> onRenameSuccess,
			Scene scene) {
		// TODO Auto-generated method stub
		
	}

}
