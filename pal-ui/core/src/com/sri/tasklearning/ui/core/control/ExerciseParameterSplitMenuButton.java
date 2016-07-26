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

package com.sri.tasklearning.ui.core.control;

import java.util.List;

import com.sri.tasklearning.ui.core.term.ExerciseStepParameter;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameterView;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

/**
 * A SplitMenuButton that is associated with a {@code TermView}. 
 */
public class ExerciseParameterSplitMenuButton extends Pane {

	private TermSplitMenuButton button;	
	private HBox borderRect; 
	private boolean changedFromOriginal = false; 

	public ExerciseParameterSplitMenuButton(ExerciseStepParameterView tv) {
    	
        super();        
        
        button = new TermSplitMenuButton(tv);
        button.getStyleClass().clear();
        
        button.getStyleClass().add("exercise-var");
        
        if (((ExerciseStepParameter) tv.getTermModel()).getChangeFromOriginalProperty().getValue()) {
			button.getStyleClass().add("changed");
			changedFromOriginal = true; 
        }
	   
        ChangeListener<Boolean> arg0 = new ChangeListener<Boolean>() {
		
		
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0,
					Boolean arg1, Boolean arg2) {
				  
				changedFromOriginal = arg2; 
				
				if (arg2) 
					button.getStyleClass().add("changed");				  
				else 
					button.getStyleClass().remove("changed"); 
			}        	
        };
        
		((ExerciseStepParameter) tv.getTermModel()).getChangeFromOriginalProperty().addListener(arg0);
                
        borderRect = new HBox();        
        borderRect.getChildren().add(button);        
        borderRect.setVisible(true);
        
       
        unhighlightBorder(); 
        
        this.getChildren().addAll(borderRect);
                
    }
    
    public void highlightBorder() {
    	/* borderRect.getStyleClass().clear(); 
    	borderRect.getStyleClass().add("exercise-var-border-rect-highlighted"); */ 
    	//button.getStyleClass().clear();
    	button.getStyleClass().clear();          
        button.getStyleClass().add("exercise-var");
    	button.getStyleClass().add("highlighted");    	
    
    }  
    
    public void unhighlightBorder() {
    	/* borderRect.getStyleClass().clear(); 
    	borderRect.getStyleClass().add("exercise-var-border-rect"); */
    	
    	//button.getStyleClass().clear();
    	button.getStyleClass().clear();          
        button.getStyleClass().add("exercise-var"); 
    	if (changedFromOriginal) 
			button.getStyleClass().add("changed");		

    }  
 
    @Override
    protected double computeMinWidth(double height) {
        return button.computeMinWidth(USE_COMPUTED_SIZE) ;
    }
    
    public TermSplitMenuButton getNode() {
    	return button;    	
    }   
    
	public void show() {
		button.show();
	}

	public List<MenuItem> getItems() {
		return button.getItems();
	}

	public void hide() {
		button.hide();
	}

	public void setOnAction(EventHandler<ActionEvent> eventHandler) {
		button.setOnAction(eventHandler);
	}

	public String getText() {
		return button.getText();
	}

	public void setText(String text) {	
		button.setText(text);	
	}
	
}
