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
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;

import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTippable;

/**
 * A tool-tippable rectangle that uses our 'roll-your-own' tooltips 
 * implementation. 
 */
public class ToolTippedRectangle extends Rectangle implements IToolTippable {
    private IToolTipCallback toolTipCallback = new IToolTipCallback() {
        public String getToolTipText() {
            return null;
        }
    };
    
    public ToolTippedRectangle() {
        setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                ToolTipper.hideTooltip();
            }
        });
        setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                ToolTipper.showTooltip(ToolTippedRectangle.this, event);
            }
        });
        setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                ToolTipper.hideTooltip();
            }
        });
    }
    
    public IToolTipCallback getToolTipCallback() {
        return toolTipCallback;
    }

    public void setToolTipCallback(IToolTipCallback toolTipCallback) {
        this.toolTipCallback = toolTipCallback;
    }

    public void refresh() {
    }
    
    @Override
    public Node getToolTipNode() {
        return this; 
    }    
}
