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

package com.sri.tasklearning.novo;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

import com.sri.tasklearning.novo.factory.AssemblyFactory;
import com.sri.tasklearning.novo.factory.ColorFactory;
import com.sri.tasklearning.novo.factory.FilterFactory;
import com.sri.tasklearning.novo.factory.NovoFactory;
import com.sri.tasklearning.novo.factory.ShapeFactory;
import com.sri.tasklearning.novo.factory.SizeFactory;

public final class FactoryViewer extends AnchorPane {
    private static final int TOOLBAR_ICON_SIZE = 24;
    public static final FactoryViewer INSTANCE = new FactoryViewer();
    private NovoFactory currentFactory = ShapeFactory.INSTANCE;
    
    private final HBox toolBar = new HBox(); {
        toolBar.setStyle("-fx-background-color: -novo-SytemGrayDient;");
    }
    private ScrollPane scrollPane = new ScrollPane();
    
    private FactoryViewer() {
        super();
        
        populateToolBar();
        
        scrollPane.setContent(currentFactory);
        
        setPrefWidth(1000);
        
        AnchorPane.setLeftAnchor(toolBar, 0.0);
        AnchorPane.setRightAnchor(toolBar, 0.0);
        AnchorPane.setTopAnchor(toolBar, 0.0);
        
        AnchorPane.setLeftAnchor(scrollPane, 0.0);
        AnchorPane.setRightAnchor(scrollPane, 0.0);
        AnchorPane.setBottomAnchor(scrollPane, 0.0);
        
        getChildren().addAll(toolBar, scrollPane);
    }
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        
        AnchorPane.setTopAnchor(scrollPane, toolBar.getHeight());
    }
    
    private void populateToolBar() {
        final Button shapeFactory = new Button("Shape", ShapeFactory.INSTANCE.getIcon(TOOLBAR_ICON_SIZE));
        shapeFactory.setStyle("-fx-background-color: -novo-SystemDarkGray, -novo-SytemDarkGrayPushedDient;");        
        shapeFactory.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                switchFactory(ShapeFactory.INSTANCE, shapeFactory);
            }
        });
        final Button filter = new Button("Filter", FilterFactory.INSTANCE.getIcon(TOOLBAR_ICON_SIZE));
        filter.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                switchFactory(FilterFactory.INSTANCE, filter);
            }
        });
        final Button colorFactory = new Button("Color", ColorFactory.INSTANCE.getIcon(TOOLBAR_ICON_SIZE));
        colorFactory.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                switchFactory(ColorFactory.INSTANCE, colorFactory);
            }
        });
        final Button sizeFactory = new Button("Size", SizeFactory.INSTANCE.getIcon(TOOLBAR_ICON_SIZE));
        sizeFactory.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                switchFactory(SizeFactory.INSTANCE, sizeFactory);
            }
        });
        final Button assemblyLine = new Button("Assembly", AssemblyFactory.INSTANCE.getIcon(TOOLBAR_ICON_SIZE));
        assemblyLine.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                switchFactory(AssemblyFactory.INSTANCE, assemblyLine);
            }
        });
        
        configButtons(shapeFactory, filter, colorFactory, sizeFactory, assemblyLine);
        
        toolBar.getChildren().addAll(shapeFactory, colorFactory, sizeFactory, filter, assemblyLine);
    }    
    
    private void switchFactory(NovoFactory newFactory, Button button) {        
        for (Node node : toolBar.getChildren())
            if (node != button) {
                ((Button)node).setStyle("-fx-background-color: -novo-SystemDarkGray, -novo-SytemGrayDient;");
            } else {
                ((Button)node).setStyle("-fx-background-color: -novo-SystemDarkGray, -novo-SytemDarkGrayPushedDient;");   
            }
        
        if (newFactory != currentFactory) {           
            scrollPane.setContent(newFactory);
            newFactory.updatePreview();
            currentFactory = newFactory; 
        }
    }
    
    private void configButtons(Button... buttons) {
        for (Button button : buttons) {
            button.setContentDisplay(ContentDisplay.TOP);
            button.getStyleClass().add("toggle");
        }
    }
}
