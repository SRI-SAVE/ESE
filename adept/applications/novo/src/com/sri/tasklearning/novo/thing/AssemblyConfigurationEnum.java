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

public enum AssemblyConfigurationEnum {
    HORIZONTAL("Horizontal", "Horizontal - "),
    VERTICAL("Vertical", "Vertical |"),
    DIAGONAL_UP("Diagonal-Up", "Diagonal Up /"),
    DIAGONAL_DOWN("Diagonal-Down", "Diagonal Down \\");
    
    private String name;
    private String displayName;
    
    AssemblyConfigurationEnum(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }
    
    public String configurationName() {
        return name;
    }
    
    public String configurationDisplayName() {
        return displayName; 
    }
    
    public static AssemblyConfigurationEnum findByName(String name) {
        for (AssemblyConfigurationEnum conf : values())
            if (conf.configurationName().equals(name))
                return conf; 
        
        throw new RuntimeException("Non-existent assembly configuration: " + name);
    }
}
