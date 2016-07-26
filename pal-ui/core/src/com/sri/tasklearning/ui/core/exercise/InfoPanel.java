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

import java.util.HashMap;
import java.util.Map;

import com.sri.tasklearning.ui.core.EditSession;
import com.sri.tasklearning.ui.core.EditSessionManager;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.control.PanelHeader;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * The info panel. Shows Infos for user and instructions.
 */

public class InfoPanel extends Pane {
	
    public static final double DEF_WIDTH = 300;   
    protected static final double HEADER_HEIGHT = 28.0;
    
    private ExerciseEditController controller;
    private static Map<ExerciseModel, InfoPanel> infoPanels = new HashMap<ExerciseModel, InfoPanel>();  
    
    private final PanelHeader header;
	
    public InfoPanel(ExerciseEditController controller, ExerciseModel model, EventHandler<ActionEvent> doneHandler ) {

    	this.controller = controller;  	    
        this.setPrefWidth(DEF_WIDTH);       
        
        infoPanels.put(model,  this);  
        
    	// controller.setAnnotationPanel(this);     
        
    	VBox layout = new VBox();
    	
    		       
    	Label title = new Label(" Have you");
    	title.setFont(Fonts.HUGE);         
    	title.setPrefWidth(DEF_WIDTH);       
    	title.setStyle("-fx-text-fill: red; -fx-border-color: white; -fx-background-color: white; -fx-border-width: 0; -fx-padding: 0"); 
    	
        header = new PanelHeader(title);
        header.setStyle("-fx-border-color: white; -fx-background-color: white;"); 
  	    	
    	VBox textPanel = new VBox();
    	textPanel.setStyle("-fx-text-fill: black; -fx-border-color: white; -fx-background-color: white; -fx-border-width: 0; -fx-padding: 0"); 
    	textPanel.setSpacing(20);
    	textPanel.setPadding(new Insets(5, 5, 5, 5));
    	
    	textPanel.setMaxWidth(AnnotationPanel.DEF_WIDTH - 20);
    	textPanel.setMinWidth(AnnotationPanel.DEF_WIDTH - 20);
    	textPanel.setPrefWidth(AnnotationPanel.DEF_WIDTH - 20);
    
    	CheckBox cb1 = new CheckBox("  Specified all allowable variations\n  on the ingredients and tools?"); 
    	CheckBox cb2 = new CheckBox("  Specified which actions\n  can be done in any order?"); 
    	CheckBox cb3 = new CheckBox("  Marked which steps are optional?\n");
    	
    	cb1.setFont(Fonts.LARGE);
    	cb2.setFont(Fonts.LARGE);
    	cb3.setFont(Fonts.LARGE);
    	
    	Button done = new Button("Done"); 
    	done.setFont(Fonts.LARGE);    	
    	done.setDisable(true);
    	
    	done.disableProperty().bind(			
    			cb1.selectedProperty().and(cb2.selectedProperty().and(cb3.selectedProperty())).not());
    	
    	done.setOnAction(doneHandler); 

    	textPanel.getChildren().addAll(cb1, cb2, cb3, done);
    	
    	VBox.setMargin(textPanel, new Insets(5,5,5,5));
      	                    
        getStyleClass().add("annotation-panel");
        textPanel.setStyle("-fx-border-color: transparent; -fx-border-width: 0;");
                     
       layout.getChildren().addAll(header, textPanel); 
        
       this.getChildren().addAll(layout); 
            
    }
    
    @Override
    protected void layoutChildren() {
     
        super.layoutChildren();     
    }

	public static void makeVisible(ExerciseModel model) {
		
		infoPanels.get(model).setVisible(true);
	
	}
     
}
