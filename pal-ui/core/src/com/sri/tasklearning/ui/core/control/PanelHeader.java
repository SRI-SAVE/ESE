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

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * Used as the visual header of the Library and Value Panel. 
 */
public class PanelHeader extends BorderPane {
    public static final double DEF_HEIGHT = 28.0;
    
    public PanelHeader (Node left, Node right) {
        getStyleClass().add("panel-header");     
        setPrefHeight(DEF_HEIGHT);      
        
        if (left != null) {
            this.setLeft(left);
            BorderPane.setAlignment(left, Pos.CENTER);
        }
        if (right != null) {
            this.setRight(right);
            BorderPane.setAlignment(right, Pos.CENTER) ;
        }
    }
    
    
    public PanelHeader (Node left) {
        getStyleClass().add("panel-header");     
        setPrefHeight(DEF_HEIGHT);      
              
        this.setRight(left);
        BorderPane.setAlignment(left, Pos.CENTER) ;
        
    }
}
