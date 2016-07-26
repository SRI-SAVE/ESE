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

package com.sri.tasklearning.ui.core.exercise;

import com.sri.tasklearning.ui.core.SelectionEventCallback;

import javafx.scene.layout.Pane;

public class AbstractCommand implements IInvokableCommand, SelectionEventCallback<AbstractCommand> {

	protected Pane owner;
	protected ExerciseEditController controller; 
	
	public AbstractCommand(ExerciseEditController controller, Pane owner) {
		
		this.controller = controller; 
		this.owner = owner; 
		
	}	

	@Override
	public void invokeCommand() {	
	}

	@Override
	public void invoke(AbstractCommand selectedItem) {
		invokeCommand();
		
	}

	@Override
	public void invokeCommand(Pane selectedItem) {
		invokeCommand(); 
		
	}	

}
