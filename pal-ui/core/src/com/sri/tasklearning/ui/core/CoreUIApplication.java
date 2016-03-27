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

import javafx.application.Application;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;


/**
 * Base class for (JavaFX 2.x) PAL UI applications that want logic for displaying
 * a 'loading' overlay. All PAL UI applications are currently 2.x.  
 */

public abstract class CoreUIApplication extends Application {
    protected Scene scene;
    protected Stage stage; 
       
    // overlay to show while a file is being loaded
    private final Group overlay = new Group();
    
    protected void initOverlay(String message) {
        final Text label = new Text(message);
        label.setFont(Font.font(Fonts.FONTFACE, FontWeight.BOLD, FontPosture.REGULAR, 48));
        label.setFill(Color.WHITE);
        label.setTextOrigin(VPos.TOP);
        label.layoutXProperty().bind(scene.widthProperty().divide(2).subtract(label.getLayoutBounds().getWidth() / 2));
        label.layoutYProperty().bind(scene.heightProperty().divide(2).subtract(label.getLayoutBounds().getHeight() / 2));
        DropShadow dropshadow = new DropShadow();
        dropshadow.setOffsetX(2.0);
        dropshadow.setOffsetY(2.0);
        dropshadow.setColor(Colors.SystemDarkGray);
        dropshadow.setRadius(10.0);
        label.setEffect(dropshadow);
        Rectangle rectangle2 = new Rectangle();
        rectangle2.setFill(Colors.SystemDarkGray);
        rectangle2.widthProperty().bind(scene.widthProperty());
        rectangle2.heightProperty().bind(scene.heightProperty());
        rectangle2.setLayoutY(-1.0);
        rectangle2.setOpacity(Colors.DimOpacity);
        overlay.getChildren().setAll(rectangle2, label);
        overlay.setCursor(Cursor.WAIT);
    }

    public void showOverlay() {
    	if (! ((Pane)scene.getRoot()).getChildren().contains(overlay)) {
    		((Pane)scene.getRoot()).getChildren().add(overlay);
    		overlay.setVisible(true);
    	}
    }

    public void hideOverlay() {
    	if (((Pane)scene.getRoot()).getChildren().contains(overlay)) {
    		((Pane)scene.getRoot()).getChildren().remove(overlay);
    		overlay.setVisible(false);
    	}
    }
    
}
