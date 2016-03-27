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
import javafx.scene.control.ScrollPane;

/**
 * An extension of SCrollPane that provides some convenience methods for 
 * scaling and scrolling a node in to view. Hopefully the ScrollPane control
 * will evolve to the point where it provides this functionality for us. 
 */
public class ScrollPanePlus extends ScrollPane {  
    private static final double SCOOCH = 12; 
    
    public ScrollPanePlus() {
        super();        
        prefViewportHeightProperty().bind(this.prefHeightProperty().add(500));
    }
    
    private void setScale() {
        final Node scrollNode = getContent();
        scrollNode.autosize();
        
        double oldPos = getVvalue();
        double oldSize = getVmax();
        
       // setVmax(scrollNode.getLayoutBounds().getHeight() - getHeight());       
        setVvalue((getVmax() / oldSize) * oldPos);
    }
    
    // Utility methods
    public void scrollIntoView(Node node) {
        Node scrollNode = getContent();
        // Set the ScrollPane proportions so that scrollbar position matches
        // the Y position of nodes within the node being scrolled. 
        setScale();
                
        double y = scrollNode.sceneToLocal(node.localToScene(0, 0)).getY();        
        // Only scroll if necessary
        if (y < getVvalue() || (y + node.getLayoutBounds().getHeight()) > getVvalue() + getHeight()) {
            // Prefer to position 1/3rd of the way down
            y = y - (getHeight() * .333);
            if (y < 0) y = 0;
            if (y > getVmax()) 
                y = getVmax();

            setVvalue(y);
            layout();  
        }              
    }
    
    public void scrollToTop() {
        setVvalue(0.00);
    }
    
    public void scoochUp() {
        // setScale();
        double v = getVvalue();
        double newV = Math.max(0, v - SCOOCH);
        setVvalue(newV);
    }
    
    public void scoochDown() {
        //setScale();
        double v = getVvalue();
        double newV = Math.min(getVmax(), v + SCOOCH);
        setVvalue(newV);
    }
}
