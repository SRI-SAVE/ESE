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
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import com.sri.tasklearning.novo.Controller;
import com.sri.tasklearning.novo.Novo;
import com.sri.tasklearning.novo.thing.ColorEnum;
import com.sri.tasklearning.novo.thing.ShapeEnum;

public final class FilterFactory extends NovoFactory {
public static final FilterFactory INSTANCE = new FilterFactory();        
    private FilterMode selectedMode;
    private Object selectedValue;
    
    private FilterFactory() {
        super();
        
        Label titleLabel = new Label("Filter Pieces");
        titleLabel.setFont(TITLE_FONT);
        title.getChildren().addAll(getIcon(ICON_SIZE),titleLabel);
        
        final SplitMenuButton filterValue = new SplitMenuButton();
        
        final SplitMenuButton filterBy = new SplitMenuButton();
        MenuItem shape = new MenuItem("Shape"); 
        shape.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                filterBy.setText("Shape");
                selectedMode = FilterMode.SHAPE;
                filterValue.getItems().clear();
                for (ShapeEnum shapeEnum : ShapeEnum.values()) {
                    final ShapeEnum finalShape = shapeEnum; 
                    MenuItem mi = new MenuItem(shapeEnum.shapeName());
                    mi.setOnAction(new EventHandler<ActionEvent>() {
                        public void handle(ActionEvent e) {
                            filterValue.setText(finalShape.shapeName());
                            selectedValue = finalShape; 
                        }
                    });
                    filterValue.getItems().add(mi);
                }
                filterValue.getItems().get(0).fire();  
            }
        });
        MenuItem color = new MenuItem("Color"); 
        color.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                filterBy.setText("Color");
                selectedMode = FilterMode.COLOR;
                filterValue.getItems().clear();
                for (ColorEnum colorEnum : ColorEnum.values()) {
                    final ColorEnum finalColor = colorEnum; 
                    MenuItem mi = new MenuItem(colorEnum.colorName());
                    mi.setOnAction(new EventHandler<ActionEvent>() {
                        public void handle(ActionEvent e) {
                            filterValue.setText(finalColor.colorName());
                            selectedValue = finalColor; 
                        }
                    });
                    filterValue.getItems().add(mi);
                }
                filterValue.getItems().get(0).fire();
            }
        });
        filterBy.getItems().addAll(shape, color);
        
        shape.fire();
        
        HBox filterByBox = new HBox(5);
        filterByBox.setAlignment(Pos.CENTER_LEFT);
        filterByBox.getChildren().addAll(new Label("Filter by "), filterBy);
        
        HBox valueBox = new HBox(5);
        valueBox.setAlignment(Pos.CENTER_LEFT);
        valueBox.getChildren().addAll(new Label("Keep the " ), filterValue, new Label(" pieces"));
        
        buildButton.setText("Filter Pieces");
        buildButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {                
                if (selectedMode == FilterMode.COLOR) {
                    Controller.filterByColor((ColorEnum)selectedValue);
                } else {
                    Controller.filterByShape((ShapeEnum)selectedValue);
                }
            }
        });

        getChildren().addAll(title, new Spacer(), filterByBox, valueBox, new Spacer(), buildButton);
    }
       
    @Override
    public void updatePreview() {
    }
        
    public ImageView getIcon(int size) {
        return Novo.getImageView("images/" + size + "/filter.png", size);
    }
    
    enum FilterMode {
        SHAPE,
        COLOR
    }
}
