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

import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import com.sri.tasklearning.ui.core.Cursors;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.resources.ResourceLoader;

/**
 * A control that consist of a knurling image and appropriate tooltips/cursors. 
 * 'Knurling' refers to the horizontal lines that make it look like you can 
 * interact with something, such as a step.
 */
public final class Knurling extends Group {
    private final String tooltip;
    public static final double WIDTH = 16;
    public static final double HEIGHT = 10; 
    
    public Knurling(String tooltip) {
        super();
        
        this.tooltip = tooltip; 
        
        getChildren().addAll(knurling, knurlingBg);
    }
    
    final ToolTippedImageView knurling = new ToolTippedImageView();
    {
        knurling.setToolTipCallback(new IToolTipCallback() {
            public String getToolTipText() {
                return tooltip;
            }
        });
        knurling.setImage(new Image(ResourceLoader.getStream("knurling.png")));
        knurling.setManaged(false);
        knurling.setCursor(Cursors.OPEN_HAND);       
    }

    final ToolTippedRectangle knurlingBg = new ToolTippedRectangle();
    {
        knurlingBg.setToolTipCallback(new IToolTipCallback() {
            public String getToolTipText() {
                return tooltip;
            }
        });
        
        knurlingBg.setWidth(WIDTH);
        knurlingBg.setHeight(HEIGHT);
        knurlingBg.setFill(Color.TRANSPARENT);
        knurlingBg.setCursor(Cursors.OPEN_HAND);
        knurlingBg.setOnMouseDragged(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {             
                knurling.setCursor(Cursors.CLOSED_HAND);                
            }
        });
        knurlingBg.setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                knurling.setCursor(Cursors.OPEN_HAND);                
            }
        });
    }
}
