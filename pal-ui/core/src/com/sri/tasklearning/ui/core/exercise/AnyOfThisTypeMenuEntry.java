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

import javafx.scene.layout.Pane;


public class AnyOfThisTypeMenuEntry extends AnnotationMenuEntry {

	public AnyOfThisTypeMenuEntry(AnnotationPanel panel) {

		super(panel, new AnyOfThisTypeCommand(panel.getController(), panel), "Any of this type"); 

	}
	
	  @Override
		public void invokeCommand(Pane selectedItem) {    	    
		  
		}

	@Override
	public void invokeCommand() {

		command.controller.highlightParameters();
		
	}    

}
