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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.ActionExecutor;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionInvocation.StepCommand;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.PALException;
import com.sri.pal.Struct;
import com.sri.pal.common.ErrorInfo;
import com.sri.tasklearning.novo.Controller;
import com.sri.tasklearning.novo.thing.AssemblyConfigurationEnum;
import com.sri.tasklearning.novo.thing.ColorEnum;
import com.sri.tasklearning.novo.thing.Piece;
import com.sri.tasklearning.novo.thing.ShapeEnum;
import com.sri.tasklearning.novo.thing.SizeEnum;

/**
 * NovoExecutor is a singleton class that listens to action invocation messages 
 * from the adept/task learning system and calls the appropriate Controller 
 * methods to execute those actions in the application. 
 */
public final class NovoExecutor implements ActionExecutor {
    private static final Logger log = LoggerFactory
            .getLogger(NovoExecutor.class);
    
    private static final NovoExecutor instance = new NovoExecutor();

    private NovoExecutor() { }    
    
    /**
     * @return the instance of this singleton class. 
     */
    public static NovoExecutor getInstance() {
        return instance;
    } 

    /**
     * This is the most important method that must be implemented by an 
     * ActionExecutor. This method takes the ActionInvocation passed in from 
     * Adept and translates it in to the appropriate calls in the application.
     * In Novo, we do this via the Controller.
     * 
     * @param invocation The action invocation we need to execute
     * @throws PALException
     */
    @Override
    public void execute(final ActionInvocation invocation) throws PALException {
        try {
         /* It is very important to disable action instrumentation when we're 
            executing an ActionInvocation from the Adept system. Otherwise,
            if the user is executing a procedure during a demonstration we 
            will instrument the action even though it should be 
            encapsulated by the procedure that's being executed */            
            Controller.endInstrumentation();
            
            innerExecute(invocation);
        } catch (Exception e) {
            log.warn("Execution of " + invocation + " failed", e);
            ErrorInfo error = new ErrorInfo(AdeptWrapper.NAMESPACE, 1,
                    "execution failed", e.toString(), null);
            invocation.error(error);
        } finally {
         /* Be sure to re-enable instrumentation after the action has been
            executed */
            Controller.startInstrumentation();
        }
    }

    /**
     * The actual workhorse method for executing and action invocation. 
     * Figures out which action is being executed, extracts its arguments and
     * makes the appropriate call to Controller. 
     * 
     * @param invocation The action invocation we need to execute
     * @throws PALException
     */
    private void innerExecute(ActionInvocation invocation) throws PALException {
        int size = invocation.getDefinition().size();        
        ArrayList<Object> items = new ArrayList<Object>();
        
        for (int i = 0; i < size; i++) 
            items.add(invocation.getValue(i));        
        
        String actionName = invocation.getDefinition().getName().getSimpleName();
        NovoAction action = NovoAction.findByName(actionName);
        
        if (action == null) {
            log.error("Cannot execute unknown action: '" + actionName + "'");
            return;
        }
        
     /* Figure out which action we're automating and then make the 
        appropriate changes in the application. A few tips on extracting/passing
        arguments:
        
        - list type arguments are passed as instances of Java.util.List
        - set type arguments are passed as instances of Java.util.Set
        - bag type arguments are passed as instances of Java.util.List
        - struct type arguments are passed as instances of import com.sri.pal.Struct;
        -
      */
        switch (action) {
            case FILTER_BY_COLOR: {
                if (items.get(0) == null) {
                    invocation.setStatus(Status.FAILED);
                    return; 
                }
                
                ColorEnum color = ColorEnum.findByName((String) items.get(0));

                invocation.setStatus(Status.RUNNING);

                List<Struct> filteredPieces = Controller.filterByColor(color);
                invocation.setValue(1, filteredPieces);

                invocation.setStatus(Status.ENDED);
                break;
            } case FILTER_BY_SHAPE: {
                if (items.get(0) == null) {
                    invocation.setStatus(Status.FAILED);                   
                    return; 
                }
                
                ShapeEnum shape = ShapeEnum.findByName((String) items.get(0));

                invocation.setStatus(Status.RUNNING);

                List<Struct> filteredPieces = 
                    Controller.filterByShape(shape);
                invocation.setValue(1, filteredPieces);

                invocation.setStatus(Status.ENDED);
                break;
            } case COLOR_COPY: {
                if (items.get(0) == null || items.get(1) == null) {
                    invocation.setStatus(Status.FAILED);                   
                    return; 
                }
                
                Piece piece = new Piece((Struct) items.get(0));
                ColorEnum color = ColorEnum.findByName((String) items.get(1));

                invocation.setStatus(Status.RUNNING);
                Struct colored_copy = Controller.colorCopy(piece, color, true);
                if (colored_copy != null
                        && color.colorName().equals(
                                colored_copy.getValue("color"))) {
                    invocation.setValue(2, colored_copy);
                    invocation.setStatus(Status.ENDED);
                } else {
                    invocation.setStatus(Status.FAILED);
                }
                break;
            } case SIZE_COPY: {
                if (items.get(0) == null || items.get(1) == null) {
                    invocation.setStatus(Status.FAILED);                   
                    return; 
                }
                
                Piece piece = new Piece((Struct) items.get(0));
                SizeEnum pieceSize = SizeEnum.findByName((String) items.get(1));

                invocation.setStatus(Status.RUNNING);
                Struct copy = Controller.sizeCopy(piece, pieceSize, true);
                if (copy != null
                        && pieceSize.sizeName().equals(copy.getValue("size"))) {
                    invocation.setValue(2, copy);
                    invocation.setStatus(Status.ENDED);
                } else
                    invocation.setStatus(Status.FAILED);
                break;
            } case DISPENSE_SHAPE: {
                if (items.get(0) == null) {
                    invocation.setStatus(Status.FAILED);                   
                    return; 
                }
                
                ShapeEnum shapeEnum = ShapeEnum.findByName((String) items
                        .get(0));

                invocation.setStatus(Status.RUNNING);
                Struct result = Controller.dispenseShape(shapeEnum);
                invocation.setValue(1, result);
                invocation.setStatus(Status.ENDED);
                break;
            } case DISPENSE_SHAPES: {
                if (items.get(0) == null || items.get(1) == null) {
                    invocation.setStatus(Status.FAILED);                   
                    return; 
                }
                
                ShapeEnum shapeEnum = ShapeEnum.findByName((String) items.get(1));
                Integer n = (Integer) items.get(0);

                invocation.setStatus(Status.RUNNING);
                List<Struct> shapesList = Controller.dispenseShapes(shapeEnum,
                        n);
                invocation.setValue(2, shapesList);
                invocation.setStatus(Status.ENDED);
                break;
            } case ASSEMBLE_TWO: {
                if (items.get(0) == null || 
                        items.get(1) == null ||
                        items.get(2) == null) {
                        invocation.setStatus(Status.FAILED);                   
                        return; 
                    }
                
                Piece p1 = new Piece((Struct) items.get(0));
                Piece p2 = new Piece((Struct) items.get(1));

                AssemblyConfigurationEnum conf = AssemblyConfigurationEnum
                        .findByName((String) items.get(2));

                invocation.setStatus(Status.RUNNING);
                
                Struct assembly = Controller.assembleTwo(conf, p1, p2, true);
                
                invocation.setValue(3, assembly);
                invocation.setStatus(Status.ENDED);
                break;
            } case ASSEMBLE_THREE: {
                if (items.get(0) == null || 
                    items.get(1) == null ||
                    items.get(2) == null ||
                    items.get(3) == null) {
                    invocation.setStatus(Status.FAILED);                   
                    return; 
                }
                Piece p1 = new Piece((Struct) items.get(0));
                Piece p2 = new Piece((Struct) items.get(1));
                Piece p3 = new Piece((Struct) items.get(2));

                AssemblyConfigurationEnum conf = AssemblyConfigurationEnum
                        .findByName((String) items.get(3));

                invocation.setStatus(Status.RUNNING);
                
                Struct assembly = Controller.assembleThree(conf, p1, p2, p3, true);
                invocation.setValue(4, assembly);
                invocation.setStatus(Status.ENDED);
                break;
            } default: {
                log.warn("Action {} ignored.", invocation.getDefinition().getName());

                // This will get picked up by the caller and turned into a call
                // to ActionInvocation.error().
                throw new IllegalArgumentException(
                        "Don't know how to execute action " + invocation);
            }
        }
    }

    @Override
    public void cancel(ActionStreamEvent arg0) {
        // Intentionally left empty. Novo actions aren't long enough to merit
        // support for canceling an action. 
    }
    
    @Override
    public void continueStepping(ActionInvocation invocation,
                                 StepCommand command,
                                 List<Object> actionArgs)
            throws PALException {
        // Intentionally left empty
    }

    @Override
    public void executeStepped(ActionInvocation arg0) throws PALException {
        // Intentionally left empty
    }    
}
