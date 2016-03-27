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

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;

import com.sri.pal.Struct;
import com.sri.tasklearning.novo.adept.AdeptWrapper;
import com.sri.tasklearning.novo.adept.NovoAction;
import com.sri.tasklearning.novo.thing.Assembly;
import com.sri.tasklearning.novo.thing.AssemblyConfigurationEnum;
import com.sri.tasklearning.novo.thing.ColorEnum;
import com.sri.tasklearning.novo.thing.Piece;
import com.sri.tasklearning.novo.thing.ShapeEnum;
import com.sri.tasklearning.novo.thing.SizeEnum;
import com.sri.tasklearning.novo.thing.Thing;



/**
 * The Controller of Novo's Model-View-Controller implementation. In addition
 * to performing the usual duties of a Controller in MVC, the controller
 * also must make the necessary calls to instrument actions that are understood
 * by task learning for purposes of demonstrating and learning procedures.
 */

public class Controller {
    private static boolean instrumenting = true;
    private static SimpleIntegerProperty numSteps = new SimpleIntegerProperty(0);
    private static final PartsViewer usedParts = Novo.getInstance().getUsedPartsViewer();
    private static final PartsViewer unusedParts = Novo.getInstance().getUnusedPartsViewer();    
    
    /**
     * Turns off adept instrumentation. This is necessary so that we don't 
     * instrument actions when a procedure is being played back because that
     * could potentially happen during the demosntration of a new procedure.
     */
    public static void endInstrumentation() {
        instrumenting = false;
    }

    /**
     * Enables/re-enables adept instrumentation
     */
    
    public static void startInstrumentation() {
        instrumenting = true;
    }
    
    /**
     * @return if instrumentation is enabled and the app is connected to the 
     * Adept/Task Learning backend. 
     */
    public static boolean isInstrumenting() {
        return instrumenting && AdeptWrapper.isConnectedToTaskLearning();
    }
    
    public static SimpleIntegerProperty numStepsProperty() {
        return numSteps; 
    }
    
    private static void incrementNumSteps() {
        numSteps.setValue(numSteps.getValue() + 1);
    }
    
    private static boolean executingEvent = false;
    
    private static void execute(final Runnable run) {
        if (Platform.isFxApplicationThread())
            run.run();
        else {
            executingEvent = true; 
            // Run it on the FX application thread...synchronously
            Runnable wrapper = new Runnable() {
                public void run() {
                    synchronized (run) {
                        run.run();
                        executingEvent = false; 
                        run.notifyAll();
                    }
                }
            };
            
            synchronized (run) {
                Platform.runLater(wrapper);
                
                while (executingEvent) {
                    try {
                        run.wait(); 
                    } catch (InterruptedException e) {
                        
                    }
                }
            }
        }
    }
    
    public static List<Struct> dispenseShapes(ShapeEnum shape, int n) {        
        ArrayList<Struct> shapes = new ArrayList<Struct>();
        
        for (int index = 0; index < n; index++) {
            final Piece clone = new Piece(shape, new Integer(index + 1).toString());
            
            execute(new Runnable() {
                public void run() {
                    incrementNumSteps();
                    unusedParts.addThing(clone);       
                }
            });
            if (AdeptWrapper.isConnectedToTaskLearning())
                shapes.add(clone.toStruct());
        }
        
        if (isInstrumenting())
            AdeptWrapper.instrumentAction(NovoAction.DISPENSE_SHAPES,
                    new Integer(n), shape.shapeName(), shapes);
        
        return shapes; 
    }
    
    public static Struct dispenseShape(ShapeEnum shape) {
        final Piece piece = new Piece(shape);
        
        execute(new Runnable() {
            public void run() {
                incrementNumSteps();
                unusedParts.addThing(piece);
            }
        }); 
        
        Struct tuple = null;
        if (AdeptWrapper.isConnectedToTaskLearning())
            tuple = piece.toStruct();
        
        if (isInstrumenting()) 
            AdeptWrapper.instrumentAction(NovoAction.DISPENSE_SHAPE,
                    shape.shapeName(), tuple);
        
        return tuple;
    }    
    
    public static Struct colorCopy(
            final Piece piece, 
            final ColorEnum color,
            final boolean automation) {
        final Piece copy = piece.getColoredCopy(color);
        
        piece.use(); 
        
        execute(new Runnable() {
            public void run() {
                incrementNumSteps();
                
                if (automation)
                    unusedParts.removeThing(piece);
                
                unusedParts.addThing(copy);
                usedParts.addThing(piece);
            }
        }); 

        Struct result = null;
        if (AdeptWrapper.isConnectedToTaskLearning())
                result = copy.toStruct();        
        
        if (isInstrumenting())
            AdeptWrapper.instrumentAction(NovoAction.COLOR_COPY, piece.toStruct(),
                    color.colorName(), result);     
        
        return result;
    }
    
    public static Struct sizeCopy(
            final Piece piece, 
            final SizeEnum size,
            final boolean automation) {
        final Piece copy = piece.getResizedCopy(size);       
        
        piece.use(); 
        
        execute(new Runnable() {
            public void run() {
                incrementNumSteps();
                
                if (automation)
                    unusedParts.removeThing(piece);
                
                unusedParts.addThing(copy);
                usedParts.addThing(piece);
            }
        }); 

        Struct result = null;
        if (AdeptWrapper.isConnectedToTaskLearning())
                result = copy.toStruct();        
        
        if (isInstrumenting())
            AdeptWrapper.instrumentAction(NovoAction.SIZE_COPY, piece.toStruct(),
                    size.sizeName(), result);   
        
        return result;
    }
    
    public static Struct assembleTwo(final AssemblyConfigurationEnum conf, 
                                     final Piece p1, 
                                     final Piece p2,
                                     final boolean automation) {
        final List<Thing> pieces = new ArrayList<Thing>();
        pieces.add(p1.copy());
        pieces.add(p2.copy());
        
        p1.use();
        p2.use();
        
        final Assembly ass = new Assembly(conf, pieces);
        
        execute(new Runnable() {
            public void run() {
                incrementNumSteps();

                if (automation) {
                    unusedParts.removeThing(p1);
                    unusedParts.removeThing(p2);
                }
                
                unusedParts.addThing(ass);                
                
                usedParts.addThing(p1);
                usedParts.addThing(p2);
            }
        });
        
        Struct result = null;
        if (AdeptWrapper.isConnectedToTaskLearning())
            result = ass.toStruct();
        
        if (isInstrumenting())
            AdeptWrapper.instrumentAction(NovoAction.ASSEMBLE_TWO, p1.toStruct(),
                    p2.toStruct(), conf.configurationName(), result);
        
        return result; 
    }
    
    public static Struct assembleThree(final AssemblyConfigurationEnum conf, 
                                       final Piece p1, 
                                       final Piece p2, 
                                       final Piece p3,
                                       final boolean automation) {
        final List<Thing> pieces = new ArrayList<Thing>();
        pieces.add(p1.copy());
        pieces.add(p2.copy());
        pieces.add(p3.copy());
        
        p1.use();
        p2.use();
        p3.use(); 
        
        final Assembly ass = new Assembly(conf, pieces);
        
        execute(new Runnable() {
            public void run() {
                incrementNumSteps();
                
                if (automation) {
                    unusedParts.removeThing(p1);
                    unusedParts.removeThing(p2);
                    unusedParts.removeThing(p3);
                }
                
                unusedParts.addThing(ass);
                
                usedParts.addThing(p1);
                usedParts.addThing(p2);
                usedParts.addThing(p3); 
            }
        });
        
        Struct result = null;
        if (AdeptWrapper.isConnectedToTaskLearning())
            result = ass.toStruct();
        
        if (isInstrumenting())
            AdeptWrapper.instrumentAction(NovoAction.ASSEMBLE_THREE, p1.toStruct(),
                    p2.toStruct(), p3.toStruct(), conf.configurationName(),
                    result);
        
        return result;
    }
    
    public static List<Struct> filterByColor(
            final ColorEnum color) {
        final List<Piece> savedPieces = new ArrayList<Piece>();        
        final List<Piece> pieces = new ArrayList<Piece>();
        
        for (Thing thing : unusedParts.getThings())
            if (thing instanceof Piece)
                pieces.add((Piece)thing);
        
        for (Piece piece : pieces)
            if (piece.getColor() == color)
                savedPieces.add(piece);
        
        execute(new Runnable() {
            public void run() {
                incrementNumSteps();
                for (Piece piece : pieces)
                    if (piece.getColor() != color) {
                        unusedParts.removeThing(piece);
                        piece.use();
                        usedParts.addThing(piece);
                    }
            }
        });
        
        List<Struct> result = null;
        
        if (AdeptWrapper.isConnectedToTaskLearning()) {
            result = new ArrayList<Struct>();
            for (Piece saved : savedPieces)
                result.add(saved.toStruct());
            
            if (isInstrumenting()) {                
                AdeptWrapper.instrumentAction(NovoAction.FILTER_BY_COLOR, 
                        color.colorName(), result);
            } 
        }             
        
        return result; 
    }
    
    public static List<Struct> filterByShape(
            final ShapeEnum shape) {
        final List<Piece> savedPieces = new ArrayList<Piece>();
        
        final List<Piece> pieces = new ArrayList<Piece>();
        for (Thing thing : unusedParts.getThings())
            if (thing instanceof Piece)
                pieces.add((Piece)thing);        
        
        for (Piece piece : pieces)
            if (piece.getShape() == shape)
                savedPieces.add(piece);

        execute(new Runnable() {
            public void run() {
                incrementNumSteps();
                for (Piece piece : pieces)
                    if (piece.getShape() != shape) {
                        unusedParts.removeThing(piece);
                        piece.use();
                        usedParts.addThing(piece);
                    }
            }
        });
                
        List<Struct> result = null;
        
        if (AdeptWrapper.isConnectedToTaskLearning()) {
            result = new ArrayList<Struct>();
            for (Piece saved : savedPieces)
                result.add(saved.toStruct());            
            
            if (isInstrumenting()) {                
                AdeptWrapper.instrumentAction(NovoAction.FILTER_BY_SHAPE,
                        shape.shapeName(), result);
            }                    
        }
        
        return result; 
    }
}
