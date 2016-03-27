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

package com.sri.tasklearning.novo;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.layout.FlowPane;

import com.sri.tasklearning.novo.thing.ColorEnum;
import com.sri.tasklearning.novo.thing.Piece;
import com.sri.tasklearning.novo.thing.ShapeEnum;
import com.sri.tasklearning.novo.thing.SizeEnum;
import com.sri.tasklearning.novo.thing.Thing;

public final class PartsViewer extends FlowPane {
    private final List<Thing> things = new ArrayList<Thing>();
    private Thing[] solution = null;
    private final boolean stack;
    
    public PartsViewer(String title, boolean stack) {
        super(12, 12);
        
        setPadding(new Insets(10, 10, 10, 10));
        
        this.stack = stack; 
    }
    
    public void addThing(Thing thing) {
        things.add(thing);
        
        if (stack)
            getChildren().add(0, thing.getNode());
        else
            getChildren().add(thing.getNode());
        
        if (solution != null) {            
            ArrayList<Integer> matched = new ArrayList<Integer>();
            if (solution.length == things.size()) {
                boolean success = true; 
                
                for (int i = 0; i < solution.length; i++) {
                    int j; 
                    for (j = 0; j < things.size(); j++)
                        if (!matched.contains(j) && solution[i].equals(things.get(j))) {
                            matched.add(j);
                            break;
                        }
                    if (j == things.size()) {
                        success = false;
                        break; 
                    }                        
                }
                
                if (success) {
                    Novo.getInstance().solvePuzzle();
                }
            }
        }
    }
    
    public List<Thing> getThings() {
        return things;
    }
    
    public void removeThing(Thing thing) {
        if (things.contains(thing)) {
            int idx = things.indexOf(thing);
            Thing actualThing = things.get(idx);
            
            things.remove(actualThing);
            getChildren().remove(actualThing.getNode());
        }
    }    

    public void clear() {
        getChildren().clear();
        things.clear(); 
    }
    
    public void testThings() {        
        for (ShapeEnum shape : ShapeEnum.values())
            for (SizeEnum size : SizeEnum.values())                
                for (ColorEnum color : ColorEnum.values())
                    addThing(new Piece(shape, color, size, null));                   
    }
    
    public void registerSolution(Thing[] solution) {
        this.solution = solution; 
    }
}
