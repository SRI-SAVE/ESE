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

public enum SizeEnum {
    SMALL("Small", 40),
    MEDIUM("Medium", 60),
    LARGE("Large", 80);
    
    private final String name;
    private final double pixels;
    
    SizeEnum(String name, double pixels) {
        this.name = name;
        this.pixels = pixels;
    }
    
    public String sizeName() {
        return name;
    }  
    
    public double pixels() {
        return pixels; 
    }
    
    public static SizeEnum findByName(String name) {
        for (SizeEnum size : values())
            if (size.sizeName().equals(name))
                return size; 
        
        throw new RuntimeException("Non-existent size: " + name);
    }
}
