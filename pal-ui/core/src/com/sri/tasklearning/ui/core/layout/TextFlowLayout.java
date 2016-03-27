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

package com.sri.tasklearning.ui.core.layout;


import java.util.LinkedList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.step.ExerciseGroupOfStepsModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ParameterView;

/**
 * A specialization of FlowPane that knows how to visualize instances of
 * ParameterModel, String and StringProperty in a consistent way. Most notably
 * used for step headers. 
 */
public final class TextFlowLayout extends FlowPane {
    
	public static final double VGAP = 2;
    public static final double HGAP = 5;
    
    // for texteditor simple editable string properties 
    private static final double TEXTEDITOR_WIDTH = 200;
        
    private static final Logger log = LoggerFactory
            .getLogger(TextFlowLayout.class);
	
    private Paint textColor = Colors.DefaultText;
    private Font font = Fonts.TITLE;
    private StepView stepView;
    private boolean containsVariable = false;
    private boolean nested = false;
    private boolean createTextFieldEditorsForStringProperties = false; 
    
    private SimpleBooleanProperty readOnly = new SimpleBooleanProperty(false);
    
    public TextFlowLayout(List<Object> contents) {
        super(HGAP, VGAP);
        
        setStyle("-fx-background-color: transparent;");
        
        setContents(contents);
    }
       
    public TextFlowLayout(double argHgap, double argVgap) {
        super(argHgap, argVgap);        
     }
    
    public TextFlowLayout(StepView argStepView) {
        super(HGAP, VGAP);
        
        this.stepView = argStepView;                
        setContents(stepView.getStepModel().getFancyName());           
    }
    
    public TextFlowLayout(String label, StepView argStepView) {
        super(HGAP, VGAP);
        
        this.createTextFieldEditorsForStringProperties = true;    
        this.stepView = argStepView;                
        
        List<Object> name = new LinkedList<Object>(); 
        
        name.add(label);
        name.add(stepView.getStepModel().nameProperty()); 
        
        setContents(name); 
        
        this.createTextFieldEditorsForStringProperties = false;    
    }
    
    public TextFlowLayout(StepView argStepView, double argHgap, double argVgap, boolean nested) {
       super(argHgap, argVgap);
       
       this.nested = nested;
       this.stepView = argStepView;                
       setContents(stepView.getStepModel().getFancyName());   
    }

    public void setTextColor(Paint color) {
        this.textColor = color;
        
        for (Node node : this.getChildren())
            if (node instanceof Text)
                ((Text)node).setFill(color);
    }    
    
    public Font getFont() {
        return font;
    }
    
    public void setReadOnly(boolean ro) {
        readOnly.setValue(ro);
    }
    
    public SimpleBooleanProperty readOnlyProperty() {
        return readOnly;
    }

    public void setFont(Font font) {
        this.font = font;
        for (Node node : this.getChildren())
            if (node instanceof Text)
                ((Text)node).setFont(font);
    }    
    
    public boolean containsVariable() {
        return containsVariable; 
    }
    
    public void setContents(List<Object> contents) {
        getChildren().clear();

        for (final Object item : contents) {
        	
            if (item instanceof ParameterModel) {
        
            	ParameterModel pm = (ParameterModel)item;
                ParameterView pv = new ParameterView(pm, stepView,
                        stepView == null ? null : stepView.getView(),
                        true, nested, readOnly);
                
                getChildren().add(pv);
                
            } else if (item instanceof String) {
                String text = (String) item;
                String[] words = text.split(" ");

                for (String word : words) {
                    if (word.length() > 0) {
                        final Text txt = new Text(word);
                        txt.setFill(textColor);
                        if (font != null)
                            txt.setFont(font);

                        getChildren().add(txt);                        
                    }
                }               
            } else if (item instanceof StringProperty) {
            	
            	if (createTextFieldEditorsForStringProperties) {
            		            		
            		 final TextField editor = new TextField();
            		 //editor.setStyle(".text-field");
            		 //editor.setStyle(".subtask-name-text-field");
            		 
            		 editor.promptTextProperty().bind((StringProperty) item);
            		 // we can't bind here, because it's a read only property... 
            		 editor.textProperty().setValue(((StringProperty) item).getValue());
            		 
            		editor.setOnKeyReleased(new EventHandler<KeyEvent>() {
            	            public void handle(KeyEvent e) {
           	            	
            	               ((StringProperty) item).setValue(editor.getText());

            	            }
            	        }); 
            		 
            		 editor.setMinWidth(TEXTEDITOR_WIDTH);       
            		 editor.setPrefWidth(TEXTEDITOR_WIDTH);
            		 editor.setAlignment(Pos.CENTER_LEFT);
            		 editor.setEditable(true);
            		          		             		 
            		 // for some obscure reason, I have to also add an empty label to make it work
                     getChildren().addAll(editor, new Label(""));
            		
            	} else {
            	
            		final Text txt = new Text();
            		txt.textProperty().bind((StringProperty)item);
            		txt.setFill(textColor);
            		if (font != null)
            			txt.setFont(font);
            		getChildren().add(txt);
            	}
            	
            } else if (item instanceof Node) {
                  getChildren().add((Node)item);               
            } else {
                log.error("TextFlowLayout error: unhandled content object\n{}", item.getClass());        
            }
        }
    }       
}
