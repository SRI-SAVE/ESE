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

/* $Id: VerifiableCallbackHandler.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $ */
package com.sri.pal;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.sri.pal.ActionInvocation.StepCommand;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.PrimitiveTypeDef.Predefined;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.ICalDateTime;
import com.sri.pal.common.ICalDuration;
import com.sri.pal.common.SimpleTypeName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * @author Valerie Wagner Date: Oct 4, 2006
 */
public class VerifiableCallbackHandler
        implements ActionExecutor, GlobalActionListener, ActionInvocationStatusListener {

    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    private List<ActionStreamEvent> actionsSeen;
    private List<ActionInvocation> futureEvents;
    private boolean taskEnded;
    private boolean taskSuccess;
    private boolean error;
    private int subProcsCalled;
    private int numTasksEnded;
    private boolean startCalled;
    private int gestures;

    public VerifiableCallbackHandler() {
        log.debug("Created a new callback handler");
        reset();
    }

    public final void reset() {
        log.info("Resetting everything");
        actionsSeen = new ArrayList<ActionStreamEvent>();
        futureEvents = new Vector<ActionInvocation>();
        taskEnded = false;
        taskSuccess = false;
        error = false;
        subProcsCalled = 0;
        numTasksEnded = 0;
        startCalled = false;
        gestures = 0;
    }

    @Override
    public void execute(ActionInvocation action)
            throws PALException {
        log.debug("performAction called for action " + action);
        if(action.getCaller() == null) {
            Assert.fail("Null procedure for action execution");
        }
        ActionDef def = action.getDefinition();
        for(int i = def.numInputParams(); i < def.size(); i++) {
            String paramName = def.getParamName(i);
            if(action.getValue(i) == null) {
                TypeDef paramType = def.getParamType(i);
                Object value = makeBogusValue(paramType);
                log.info("Bogus value for " + paramName + " is " + value);
                action.setValue(i, value);
            }
        }
        ActionInvocation event = getNextEvent(action);
        if(event != action) {
            ActionDef actDef = action.getDefinition();
            for(int i = actDef.numInputParams(); i < actDef.size(); i++) {
                action.setValue(i, event.getValue(i));
            }
        }
        log.info("Returning {}", action);
        actionWasCalled(action);
        action.setStatus(Status.ENDED);
    }

    public int getNumGesturesSeen() {
        return gestures;
    }

    public static Object makeBogusValue(TypeDef type)
            throws PALException {
        if (type instanceof NullableDef) {
            NullableDef nullType = (NullableDef) type;
            TypeDef subType = nullType.getElementType();
            return makeBogusValue(subType);
        } else if (type instanceof CollectionTypeDef) {
            CollectionTypeDef listType = (CollectionTypeDef) type;
            TypeDef subType = listType.getElementType();
            Collection<Object> result;
            if (type instanceof SetDef) {
                result = new HashSet<Object>();
            } else {
                result = new ArrayList<Object>();
            }
            result.add(makeBogusValue(subType));
            result.add(makeBogusValue(subType));
            return result;
        } else if (type instanceof StructDef) {
            StructDef tType = (StructDef) type;
            Struct result = new Struct(tType);
            for (int i = 0; i < tType.size(); i++) {
                TypeDef memberType = tType.getFieldType(i);
                result.setValue(i, makeBogusValue(memberType));
            }
            return result;
        } else if (type instanceof CustomTypeDef) {
            Object result;
            CustomTypeDef cType = (CustomTypeDef) type;
            Class<?> javaType;
            try {
                javaType = cType.getJavaClass();
            } catch (ClassNotFoundException e) {
                throw new PALException(e);
            }
            if(Number.class.isAssignableFrom(javaType)) {
                result = cType.unstringify("7");
            } else {
                result = cType.unstringify("bogus");
            }
            return result;
        } else if (type instanceof PrimitiveTypeDef) {
            PrimitiveTypeDef primType = (PrimitiveTypeDef) type;
            Predefined kind = primType.getKind();
            if (kind == Predefined.INTEGER) {
                return 7L;
            } else if (kind == Predefined.REAL) {
                return 7.0d;
            } else if (kind == Predefined.STRING) {
                return "bogus";
            } else if (kind == Predefined.BOOLEAN) {
                return true;
            } else if (kind == Predefined.TIMESTAMP) {
                return new ICalDateTime();
            } else if (kind == Predefined.DURATION) {
                return new ICalDuration(10 * 1000);
            } else {
                throw new RuntimeException("Can't handle primitive "
                        + kind.name() + " type");
            }
        } else if (type instanceof EnumeratedTypeDef) {
            EnumeratedTypeDef enumType = (EnumeratedTypeDef) type;
            Set<String> values = enumType.getValues();
            return values.iterator().next();
        } else {
            throw new RuntimeException("Can't handle type " + type.getName()
                    + " of " + type.getClass());
        }
    }

    public boolean sawAction(SimpleTypeName actionName)
            throws RemoteException {
        if (getActionsByType(actionName).size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public List<ActionStreamEvent> getSeenActions() {
        return actionsSeen;
    }

    public List<ActionStreamEvent> getActionsByType(SimpleTypeName type)
            throws RemoteException {
        List<ActionStreamEvent> actions = new ArrayList<ActionStreamEvent>();
        for (ActionStreamEvent event : actionsSeen) {
            AbstractActionDef def = event.getDefinition();
            if (def != null) {
                SimpleTypeName thisId = event.getDefinition().getName();
                if (type.equals(thisId)) {
                    actions.add(event);
                }
            }
        }

        return actions;
    }

    private synchronized void actionWasCalled(ActionInvocation action) {
        actionsSeen.add(action);
        log.info("Saw callback for " + action);
    }

    @Override
    public synchronized void actionStarted(ActionStreamEvent event) {
        if (event instanceof ProcedureInvocation) {
            log.info("Procedure started: {}", event);
            startCalled = true;
            actionsSeen = new ArrayList<ActionStreamEvent>();
            event.addListener(this);
            if (event.getCaller() != null) {
                subProcsCalled++;
            }
        } else if (event instanceof GestureStart) {
            actionsSeen.add(event);
            log.info("Gesture started: {}", event);
            gestures++;
        } else if (event instanceof GestureEnd) {
            actionsSeen.add(event);
            log.info("Gesture ended: {}", event);
        }
    }

    @Override
    public synchronized void newStatus(Status status) {
        if (status == Status.ENDED || status == Status.FAILED) {
            log.info("Procedure ended; success = " + status);
            taskEnded = true;
            numTasksEnded++;
            taskSuccess = (status == Status.ENDED);
            futureEvents = new Vector<ActionInvocation>();
            log.info("Resetting queue of future events.");
        }
    }

    @Override
    public void error(ErrorInfo error) {
        log.info("Procedure error encountered", error);
        this.error = true;
    }

    /**
     * Returns true if any procedure reported an error.
     *
     * @return
     */
    public boolean wasError() {
        return error;
    }

    public boolean isTaskEnded() {
        return taskEnded;
    }

    synchronized public boolean wasTaskSuccessful() {
        return taskSuccess;
    }

    public int subProceduresCalled() {
        return subProcsCalled;
    }

    public int numTasksEnded() {
        return numTasksEnded;
    }

    public boolean wasStartCalled() {
        return startCalled;
    }

    public void addFutureEvent(ActionInvocation event) {
        futureEvents.add(event);
        log.info("Added future event as number " + futureEvents.size() + ": "
                + event);
    }

    /**
     * Search for an event of the same type as the default given. If found,
     * return that. If not, return the given default instead.
     */
    private ActionInvocation getNextEvent(ActionInvocation defaultEvent) {
        ActionInvocation retVal = defaultEvent;

        for (ActionInvocation event : futureEvents) {
            if (event.getDefinition().equals(defaultEvent.getDefinition())) {
                retVal = event;
                break;
            }
        }

        if (retVal != defaultEvent) {
            futureEvents.remove(retVal);
            log.info("Using event from queue: " + retVal);
        }

        return (retVal);
    }

    @Override
    public void cancel(ActionStreamEvent invocation) {
        // Do nothing.
    }

    @Override
    public void executeStepped(ActionInvocation invocation)
            throws PALException {
        execute(invocation);
    }

    @Override
    public void continueStepping(ActionInvocation invocation,
                                 StepCommand command,
                                 List<Object> actionArgs)
            throws PALException {
        throw new PALException("Not implemented.");
    }
}
