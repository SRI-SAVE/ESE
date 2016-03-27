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

import java.util.LinkedList;
import java.util.List;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.SelectionManager;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.control.PanelHeader;
import com.sri.tasklearning.ui.core.control.ToolTipper;

/**
 * The annotation panel. Shows Annotations for user to select and to apply to actions and parameters. 
 */

public class AnnotationPanel extends Pane {
	
    public static final double DEF_WIDTH = 300;   
    protected static final double HEADER_HEIGHT = 28.0;
    
    private ExerciseEditController controller;   

	private final VBox groupList = new VBox();
    private final PanelHeader header;
	
    private AnnotationMenuEntry rearrange;
	
	private List<AnnotationMenuEntry> menuEntries = new LinkedList<AnnotationMenuEntry>(); 
    
    private static AnnotationPanel instance;
    
    private AnnotationMenuEntry selectedItem; 
       
    public AnnotationPanel(ExerciseEditController controller) {
    	
    	VBox layout = new VBox();
    	
    	controller.setAnnotationPanel(this); 
    	
    	this.controller = controller; 
    	    	
        this.setPrefWidth(DEF_WIDTH);      
                
        Label title = new Label("Annotations");
        title.setFont(Fonts.LARGE);         
		title.setPrefWidth(DEF_WIDTH);        		
        
        header = new PanelHeader(title);
                    
        getStyleClass().add("annotation-panel");
        groupList.setStyle("-fx-border-color: transparent; -fx-border-width: 0;");
                
        populateGroupList();
        
        layout.getChildren().addAll(header, groupList); 
        
        this.getChildren().addAll(layout); 
        
        layoutChildren();
        
        instance = this; 

    }
    
    @Override
    protected void layoutChildren() {
        double widest_group = 0;
        for (Node node : groupList.getChildren()) {
            double width = ((AnnotationMenuEntry)node).computeWidth();
            if (width > widest_group)
                widest_group = width; 
        }
        
        for (Node node : groupList.getChildren())
            ((AnnotationMenuEntry)node).setPrefWidth(widest_group);
        
        groupList.setPrefWidth(widest_group);

        super.layoutChildren();     
    }
     
    private void populateGroupList() {
   	
        AnnotationMenuEntry actions = new AnnotationMenuEntry(this, "Steps", Utilities.getImage("library.png"));               
        groupList.getChildren().add(actions);

        rearrange = new AnnotationMenuEntry(this, "Rearrange", null);
        rearrange.setLeftIndent(32);
        //groupList.getChildren().add(rearrange);
        rearrange.setOnMousePressed(createGroupEventHandler(rearrange));
        
        AnnotationMenuEntry makeOptional = new ToggleStepOptionalMenuEntry(this);
        makeOptional.setLeftIndent(32);
        groupList.getChildren().add(makeOptional);
        makeOptional.setOnMousePressed(createGroupEventHandler(makeOptional));
        
        AnnotationMenuEntry groupSequence = new GroupUngroupSequenceMenuEntry(this);
        groupSequence.setLeftIndent(32);
        groupList.getChildren().add(groupSequence);
        groupSequence.setOnMousePressed(createGroupEventHandler(groupSequence));
     
        AnnotationMenuEntry inAnyOrder = new InAnyOrGivenOrderMenuEntry(this);
        inAnyOrder.setLeftIndent(32);
        groupList.getChildren().add(inAnyOrder);
        inAnyOrder.setOnMousePressed(createGroupEventHandler(inAnyOrder));   
     
        //
        //
        // 
        
        AnnotationMenuEntry parameters = new AnnotationMenuEntry(this, "Parameters", Utilities.getImage("library.png"));
        groupList.getChildren().add(parameters);   
        
        AnyOfThisTypeMenuEntry anyOfThisType = new AnyOfThisTypeMenuEntry(this);
        anyOfThisType.setLeftIndent(32);
        groupList.getChildren().add(anyOfThisType);
        anyOfThisType.setOnMousePressed(createGroupEventHandler(anyOfThisType));
        
        AnyOfTheseValuesMenuEntry anyOfTheseValues = new AnyOfTheseValuesMenuEntry(this);
        anyOfTheseValues.setLeftIndent(32);
        groupList.getChildren().add(anyOfTheseValues);
        anyOfTheseValues.setOnMousePressed(createGroupEventHandler(anyOfTheseValues));
      
        AnyInThatRangeMenuEntry anyInThatRange = new AnyInThatRangeMenuEntry(this);
        anyInThatRange.setLeftIndent(32);
        groupList.getChildren().add(anyInThatRange);
        anyInThatRange.setOnMousePressed(createGroupEventHandler(anyInThatRange));
      
        
        //
        //
        //
        
        menuEntries.add(makeOptional); 
        menuEntries.add(groupSequence); 
        menuEntries.add(inAnyOrder); 
        menuEntries.add(anyOfThisType); 
        menuEntries.add(anyOfTheseValues); 
        menuEntries.add(anyInThatRange); 
             
    }   
    
    public void unselectAll() {
      	
    	controller.unhighlightSteps();
    	controller.unhighlightParameters();
    	selectedItem = null; 
    	
    	for (AnnotationMenuEntry entry : menuEntries) {
    		entry.setSelected(false);    
    		entry.owner.getExerciseViewSelectionManager().registerSelectionEventCallback(rearrange);	
    	}
    	
    }
    
    private EventHandler<MouseEvent> createGroupEventHandler(final AnnotationMenuEntry item) {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
            	
            	e.consume();
            	
                ToolTipper.hideTooltip();

                for (AnnotationMenuEntry item2 : menuEntries) {
                	if (item2 != item)
                		item2.setSelected(false); 
                	else                
                		item.setSelected(! item.isSelected());
                }
                            	
            	controller.unhighlightSteps();
            	controller.unhighlightParameters();
            	
            	if (item.isSelected()) {           
            		selectedItem = item; 
                	item.invokeCommand();                 
                } else {                	
                	item.owner.getExerciseViewSelectionManager().registerSelectionEventCallback(rearrange);	
                }
              
            }
        };
    }

	public AnnotationPanel getInstance() {
		return instance;
	}

	public ExerciseEditController getController() {
		return controller;
	}     

	public SelectionManager getExerciseViewSelectionManager() { 
		return getController().getView().getSelectionManager(); 		
	}

	public AnnotationMenuEntry getSelectedItem() {
		return selectedItem;
	}

}
