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

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTippable;

/**
 * Extension of ImageView that allows the ImageView to be tool-tipped using
 * our 'roll-your-own' tooltips implementation. 
 */
public class ToolTippedImageView extends ImageView implements IToolTippable {
    private IToolTipCallback toolTipCallback = new IToolTipCallback() {
        public String getToolTipText() {
            return null;    
        }
    };

    public ToolTippedImageView() {
        super();
        
        initEventHandlers();
    }
    
    public ToolTippedImageView(Image image) {
        super(image);
        
        initEventHandlers();
    }
    
    @Override
    public IToolTipCallback getToolTipCallback() {
        return toolTipCallback;
    }

    @Override
    public void setToolTipCallback(IToolTipCallback toolTipCallback) {
        this.toolTipCallback = toolTipCallback;
    }
    
    @Override
    public Node getToolTipNode() {
        return this; 
    }
    
    public void refresh() {
    
    }       
    
    private void initEventHandlers() {
        ToolTipper.registerEventHandlers(this); 
    }
}
