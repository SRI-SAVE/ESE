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

package com.sri.tasklearning.ui.core.library;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Polygon;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;

/**
 * Visualization for a 'group' in the library. 
 */
public class LibraryRowGroup extends LibraryRowBasicUI {
    public LibraryRowGroup (String argText, Image argIcon) {
        super(argText, argIcon); 
        
        if (argIcon == null) {
            icon = Utilities.getImage("gear.png");
            ((ImageView)label.getGraphic()).setImage(icon);
        }       
        
        getChildren().add(triangle); 
        
        setToolTipCallback(new IToolTipCallback() {
            public String getToolTipText() {
                return "Click to show the actions for \"" + text + "\"";
            }
        });
    }    
    
    // ************************** UI Components *******************************

    private static final double ARROW_SIZE = 6;
    private final Polygon triangle = new Polygon(new double[]{0.0, ARROW_SIZE * 0.8, (ARROW_SIZE - 2), 0.0, 0.0, -ARROW_SIZE * 0.8}); {
        triangle.translateXProperty().bind(widthProperty().subtract(8));
        triangle.translateYProperty().bind(heightProperty().divide(2));
        triangle.setFill(Colors.SystemDarkGray);
        triangle.setStroke(null);
        triangle.visibleProperty().bind(selected);       
    }
}
