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

package com.sri.tasklearning.novo.factory;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.image.ImageView;

import com.sri.tasklearning.novo.Controller;
import com.sri.tasklearning.novo.Novo;
import com.sri.tasklearning.novo.thing.Piece;
import com.sri.tasklearning.novo.thing.ShapeEnum;

public final class ShapeFactory extends NovoFactory {    
    public static final ShapeFactory INSTANCE = new ShapeFactory();     
    
    private ShapeEnum selectedShape = ShapeEnum.CIRCLE;
    private int numShapes = 1; 
    
    private ShapeFactory() {
        super();
        
        Label titleLabel = new Label("Shape Factory");
        titleLabel.setFont(TITLE_FONT);
        title.getChildren().addAll(getIcon(ICON_SIZE),titleLabel);
        
        final SplitMenuButton shapeMenu = new SplitMenuButton();

        for (ShapeEnum shape : ShapeEnum.values()) {
            final ShapeEnum finalShape = shape;
            MenuItem mi = new MenuItem(shape.shapeName());
            mi.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    shapeMenu.setText(finalShape.shapeName());
                    selectedShape = finalShape;
                    updatePreview();
                }
            });
            shapeMenu.getItems().add(mi);
        }
        
        shapeMenu.getItems().get(0).fire();
        
        final SplitMenuButton numMenu = new SplitMenuButton();
        for (int i = 1; i <= 10; i++) {
            MenuItem mi = new MenuItem("" + i);
            final int finalI = i;
            mi.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    numMenu.setText(finalI + "");
                    numShapes = finalI;
                    updatePreview();
                }
            });
            numMenu.getItems().add(mi);
        }
        
        numMenu.getItems().get(0).fire();
        
        Label inputsLabel = new Label("Factory Inputs:");
        inputsLabel.setFont(INPUT_FONT);
        
        Label shapeLbl = new Label("Choose a shape:");             
        Label numLbl = new Label("Number to Produce:");
        
        inputs.add(shapeLbl, 0, 0);
        inputs.add(shapeMenu, 1, 0);
        inputs.add(numLbl, 0, 1);
        inputs.add(numMenu, 1, 1);
        
        buildButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                if (numShapes > 1)
                    Controller.dispenseShapes(selectedShape, numShapes);
                else
                    Controller.dispenseShape(selectedShape);                
            }
        });
        
        Label outLbl = new Label("Factory Output Preview:");
        outLbl.setFont(INPUT_FONT);
        
        getChildren().addAll(title, new Spacer(), inputsLabel, inputs,
                new Spacer(), outLbl, preview,
                new Spacer(), buildButton);
    }
    
    @Override
    public void updatePreview() {
        preview.getChildren().clear();
        if (numShapes == 1) {
            preview.getChildren().add(new Piece(selectedShape).getNode());
            return;
        }
        
        for (int i = 0; i < numShapes; i++) {
            preview.getChildren().add(
                    new Piece(selectedShape, new Integer(i + 1).toString()).getNode());
        }
    }
    
    public ImageView getIcon(int size) {
        return Novo.getImageView("images/" + size + "/shapes_color_outline.png", size);
    }
}
