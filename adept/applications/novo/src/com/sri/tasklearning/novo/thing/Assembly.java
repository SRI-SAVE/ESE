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

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import com.sri.pal.Struct;
import com.sri.pal.StructDef;
import com.sri.tasklearning.novo.adept.AdeptWrapper;

public class Assembly extends Thing {
    
    private final List<Thing> things;
    private AssemblyConfigurationEnum configuration;
    private Pane pane;
    
    public Assembly(AssemblyConfigurationEnum configuration, List<Thing> argThings) {       
        this.configuration = configuration;
        this.things = argThings;
        
        init();
    }
    
    public Assembly(AssemblyConfigurationEnum configuration, Thing[] argThings) {       
        this.configuration = configuration;
               
        this.things = new ArrayList<Thing>();
        
        for (Thing thing : argThings)
            things.add(thing);
        
        init();
    }
    
    /**
     * Creates a new Assembly from an Adept Struct instance.
     * 
     * @param struct the Struct to create a new Assembly from
     */
    public Assembly(Struct struct) {        
        @SuppressWarnings("unchecked")
        List<Struct> tuples = (List<Struct>)struct.getValue("things");
        things = new ArrayList<Thing>();
        
        configuration = AssemblyConfigurationEnum.findByName((String) struct
                .getValue("configuration"));
        
        if (configuration == null)
            configuration = AssemblyConfigurationEnum.HORIZONTAL;

        if (tuples != null)
            for (Struct thing : tuples)
                things.add(new Piece(thing));
        
        if (things.size() < 2) {
            for (int i = things.size(); i < 2; i++)
                things.add(new Piece(Piece.DEFAULT_SHAPE));
        }
        
        init();
    }        
    
    /**     
     * @return An Adept Struct instance that reflects the values of this 
     * Assembly
     */
    public Struct toStruct() {
        StructDef tupleDef = (StructDef) AdeptWrapper.getType("Assembly");
        
        if (tupleDef == null)
            return null;
        
        Struct tupleValue = new Struct(tupleDef);
        List<Struct> thingStructs = new ArrayList<Struct>();
        
        for (Thing thing : things) {
            Struct struct = thing.toStruct();
            if (struct != null)
                thingStructs.add(struct);
        }
        
        tupleValue.setValue("things", thingStructs);      
        tupleValue.setValue("configuration", configuration.configurationName());
        
        return tupleValue;
    }
    
    private void init() {
        switch (this.configuration) {
        case HORIZONTAL:
            HBox hbox = new HBox();
            hbox.setAlignment(Pos.CENTER);
            pane = hbox;
            break;
        case VERTICAL: 
            VBox vbox = new VBox();
            vbox.setAlignment(Pos.CENTER);
            pane = vbox;
            break;
        case DIAGONAL_UP:
            DiagonalBox dup = new DiagonalBox(true);
            pane = dup;
            break;
        case DIAGONAL_DOWN:
            DiagonalBox ddown = new DiagonalBox(false);
            pane = ddown;
            break;
        }
        
        for (Thing thing : this.things)
            pane.getChildren().add(thing.getNode());
        
        pane.setStyle("-fx-border-style: solid; -fx-border-color: black; -fx-border-insets: -5;");
    }
    
    @Override
    public Pane getNode() {
        return pane;
    }
    
    public List<Thing> getThings() {
        return things; 
    }
    public AssemblyConfigurationEnum getConfiguration() {
        return configuration;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof Assembly) {
            Assembly otherAss = (Assembly)other;
            if (otherAss.getThings().size() != getThings().size() ||
                otherAss.getConfiguration() != getConfiguration())
                return false;
            
            for (int i = 0; i < getThings().size(); i ++)
                if (!otherAss.getThings().get(i).equals(getThings().get(i)))
                    return false;
            return true;
        }
        return false; 
    }
    
    @Override
    public int hashCode() {
        int hc = 0;
        for (Thing thing : getThings())
                hc += thing.hashCode();
        return hc; 
    }
    
    @Override
    public Assembly clone() {
        Assembly clone = new Assembly(configuration, things); 
        clone.used = used; 
        return clone;
    }
    
    @Override
    public Assembly copy() {
        Assembly copy = new Assembly(configuration, things); 
        return copy;        
    }
    
    @Override
    public String toString() {
        String ret = "Assembly: " + configuration.name() + " [";
        for (Thing thing : things)
            ret += thing.toString() + ", ";
        
        ret = ret.substring(0, ret.length() -2);
        ret += "]";
        
        return ret;
    }

    class DiagonalBox extends Pane {
        private final boolean diagonalUp;
        
        public DiagonalBox(boolean diagonalUp) {
            super();
            
            this.diagonalUp = diagonalUp;
        }
        
        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            
            double x = 0;
            double y = diagonalUp ? getHeight()
                    - getChildren().get(0).getLayoutBounds().getHeight() : 0;
            
            for (Node node : getChildren()) {
                node.relocate(x, y);
                x += node.getLayoutBounds().getWidth();
                y += (diagonalUp ? -1 : 1) * node.getLayoutBounds().getHeight();
            }
        }
        
        @Override
        protected double computePrefWidth(double height) {
            double width = 0;
            for (Node node : getChildren()) {
                width += node.prefWidth(0);
            }
            return width; 
        }
        
        @Override
        protected double computeMinWidth(double height) {
            return computePrefWidth(height);
        }
        
        @Override
        protected double computeMaxWidth(double height) {
            return computePrefWidth(height);    
        }
        
        @Override
        protected double computePrefHeight(double wdith) {
            double height = 0;
            for (Node node : getChildren()) {
                height += node.prefHeight(0);
            }
            return height; 
        }
        @Override
        protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }
        @Override
        protected double computeMaxHeight(double width) {
            return computePrefHeight(width);    
        }
    }
}
