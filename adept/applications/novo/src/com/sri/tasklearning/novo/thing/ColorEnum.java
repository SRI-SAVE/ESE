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

package com.sri.tasklearning.novo.thing;

import javafx.scene.paint.Color;

public enum ColorEnum {
    BLACK ("Black", Color.BLACK),
    BLUE ("Blue", Color.rgb(0, 174, 239)),
    GREEN ("Green", Color.GREEN),
    RED ("Red", Color.rgb(237, 28, 36)),
    WHITE ("White", Color.WHITE),
    YELLOW ("Yellow", Color.rgb(245, 238, 48));  
    
    private String name;
    private Color color;
    
    ColorEnum(String name, Color color) {
        this.name = name;
        this.color = color;
    }
    
    public String colorName() {
        return name;
    }
    
    public Color color() {
        return color; 
    }
    
    public static ColorEnum findByName(String name) {
        for (ColorEnum color : values())
            if (color.colorName().equals(name))
                return color; 
        
        throw new RuntimeException("Non-existent color: " + name);
    }
}
