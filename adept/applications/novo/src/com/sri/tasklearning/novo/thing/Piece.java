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

import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import com.sri.pal.Struct;
import com.sri.pal.StructDef;
import com.sri.tasklearning.novo.adept.AdeptWrapper;

public class Piece extends Thing {
    public static final ColorEnum DEFAULT_COLOR = ColorEnum.BLACK;
    public static final SizeEnum DEFAULT_SIZE = SizeEnum.MEDIUM;
    public static final ShapeEnum DEFAULT_SHAPE = ShapeEnum.CIRCLE;
    
    private ShapeEnum novoShape;
    private ColorEnum novoColor;
    private SizeEnum novoSize;
    private String label; 
    
    
    private Shape shape;
    private final Text lblText = new Text(); {
        lblText.setFont(new Font("Tahoma", 30));
    }
    private final StackPane pane = new StackPane(); {
        pane.setMaxHeight(Region.USE_PREF_SIZE);
    }
        
    public Piece(ShapeEnum shape, String label) {
        this.novoShape = shape;
        this.novoColor = DEFAULT_COLOR;
        this.novoSize = DEFAULT_SIZE;
        this.label = label; 
        
        init();
    }
    
    public Piece(ShapeEnum shape) {
        this.novoShape = shape;
        this.novoColor = DEFAULT_COLOR;
        this.novoSize = DEFAULT_SIZE;
        
        init();
    }
    
    public Piece(ShapeEnum novoShape, ColorEnum novoColor, SizeEnum novoSize, String label) {
        this.novoShape = novoShape;
        this.novoColor = novoColor;
        this.novoSize = novoSize;
        this.label = label;
        
        init();
    }
    
    public Piece(ShapeEnum novoShape, ColorEnum novoColor, SizeEnum novoSize) {
        this.novoShape = novoShape;
        this.novoColor = novoColor;
        this.novoSize = novoSize;
        
        init();
    }
    
    public Piece getColoredCopy(ColorEnum color) {
        return new Piece(novoShape, color, novoSize, label);
    }
    
    public Piece getResizedCopy(SizeEnum size) {
        return new Piece(novoShape, novoColor, size, label);
    }   
        
    /**
     * Creates a Piece from an Adept Struct
     */
    public Piece(final Struct struct) {
        novoShape = ShapeEnum.findByName((String)struct.getValue("shape"));
        novoColor = ColorEnum.findByName((String)struct.getValue("color"));
        novoSize = SizeEnum.findByName((String)struct.getValue("size"));
        
        label = (String)struct.getValue("label");
        
        if (novoShape == null)
            novoShape = DEFAULT_SHAPE;
        
        if (novoColor == null)
            novoColor = DEFAULT_COLOR;
        
        if (novoSize == null)
            novoSize = DEFAULT_SIZE;
        
        init();
    }
    
    /**     
     * @return An Adept Struct instance that reflects the values of this Piece 
     */
    @Override
    public Struct toStruct() {
        StructDef structDef = (StructDef) AdeptWrapper.getType("Piece");
        
        if (structDef == null)
            return null;
        
        Struct struct = new Struct(structDef);
               
        struct.setValue("shape", novoShape.shapeName());
        struct.setValue("color", novoColor.colorName());
        struct.setValue("size",  novoSize.sizeName());
        
        if (label != null)
            struct.setValue("label", label);
        
        return struct;
    }
    
    @Override
    public String toString() {
        return "Piece: [" + novoSize.name() + ", " + novoColor.name() + ", " + 
               novoShape.name() + ", " + label + "]";
    }
    
    private void init() {
        switch (novoShape) {
        case SQUARE:
            shape = new Rectangle(novoSize.pixels(), novoSize.pixels(),
                    novoColor.color());
            break;
        case CIRCLE:
            shape = new Circle(novoSize.pixels() / 2, novoColor.color());
            break;
        case TRIANGLE:
            double size = novoSize.pixels();
            shape = new Polygon(new double[] { 0, 0, size, 0, size / 2, -size });
            shape.setFill(novoColor.color());
            break;
        default:
            throw new RuntimeException("Unsupported NovoShape! " + novoShape);
        }

        registerEvents(pane);
        
        if (label != null)
            lblText.setText(label);
        
        if (novoColor == ColorEnum.BLACK) {
            lblText.setStroke(Color.WHITE);
            lblText.setFill(Color.WHITE);
        }         
        
        pane.getChildren().addAll(shape, lblText);
        
        DropShadow ds = new DropShadow();
        ds.setOffsetY(3.0f);
        ds.setColor(Color.color(0.4f, 0.4f, 0.4f));

        shape.setEffect(ds);
    }

    @Override
    public Pane getNode() {
        return pane;
    }

    public ShapeEnum getShape() {
        return novoShape;
    }

    public ColorEnum getColor() {
        return novoColor;
    }

    public SizeEnum getSize() {
        return novoSize;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Piece) {
            Piece otherPiece = (Piece)other;
            if (otherPiece.getShape() == getShape() &&
                otherPiece.getColor() == getColor() &&
                otherPiece.getSize() == getSize() &&
                (otherPiece.label == null ||
                 otherPiece.label.equals(label)))
                return true;
        }
        return false; 
    }
    
    @Override
    public int hashCode() {
        return getShape().hashCode() + 
               getSize().hashCode() +
               getColor().hashCode();
    }
    
    @Override
    public Piece clone() {
        Piece clone = new Piece(novoShape, novoColor, novoSize, label);
        clone.used = used; 
        return clone;
    }
    
    public Piece copy() {
        Piece copy = new Piece(novoShape, novoColor, novoSize, label);
        return copy;
    }    
}
