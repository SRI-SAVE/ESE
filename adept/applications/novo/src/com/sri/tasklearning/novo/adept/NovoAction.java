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

package com.sri.tasklearning.novo.adept;


/**
 * NovoAction enumerates all of the actions defined in novo_model.xml. Each
 * of these actions should be instrumented in Controller when the user
 * demonstrates them. Conversely, each of these actions should be supported
 * in NovoExecutor for when adept is running a learned procedure and needs
 * to execute these actions. 
 */
public enum NovoAction {
    FILTER_BY_COLOR("filterByColor"),
    FILTER_BY_SHAPE("filterByShape"),
    COLOR_COPY("colorCopy"),
    SIZE_COPY("sizeCopy"),
    DISPENSE_SHAPE("dispenseShape"),
    DISPENSE_SHAPES("dispenseShapes"),
    ASSEMBLE_TWO("assembleTwo"),
    ASSEMBLE_THREE("assembleThree");

    private String functor; // The identifier for an action
    
    NovoAction(String functor) {
        this.functor = functor;
    }
    
    public String getFunctor() {
        return functor;
    }
    
    public static NovoAction findByName(String name) {
        for (NovoAction act : values())
            if (act.functor.equals(name))
                return act; 
        
        return null;
    }
};

