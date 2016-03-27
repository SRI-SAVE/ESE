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
package com.sri.pal;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.CancelRequest;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Represents a single invocation of a given {@link ActionDef}. This invocation
 * will be associated with an {@link ActionExecutor} and 0 or more
 * {@link ActionInvocationStatusListener}s.
 *
 * @author chris
 */
public class ActionInvocation
        extends ActionStreamEvent {
    private static final Logger log = LoggerFactory
            .getLogger(ActionInvocation.class);

    private int location;
    private SimpleTypeName subActionName;

    ActionInvocation(ActionDef definition,
                     ActionStreamEvent caller,
                     long serialNum,
                     TransactionUID uid) {
        super(definition, definition.getBridge(), caller, serialNum, uid);

        for (int i = 0; i < definition.numInputParams(); i++) {
            Object value = definition.getDefaultValue(i);
            if (value != null) {
                setValue(i, value);
            }
        }
    }

    @Override
    void assignArgs(List<? extends Object> args)
            throws PALException {
        int numInputs = getDefinition().numInputParams();
        if (!args.isEmpty() && args.size() != numInputs) {
            throw new IllegalArgumentException("Must have either 0 or "
                    + numInputs + " args; got " + args.size());
        }
        super.assignArgs(args);
    }

    /**
     * Starts the execution of this invocation. The invocation runs
     * asynchronously.
     *
     * @throws PALException
     *             if an error occurs
     */
    public void start()
            throws PALException {
        checkArgs();
        if (getCaller() != null) {
            getCaller().waitUntilStarted();
        }
        ActionDef def = getDefinition();
        ActionExecutor exec = def.getExecutor();
        if (exec == null) {
            try {
                /* There's no local executor. Is there a remote one? */
                if (!def.getActionModel().hasRemoteExecutor(def.getName())) {
                    throw new PALException("No local or remote executor for "
                            + this);
                }
                log.debug("Sending remote execute request for {}", this);
                Spine spine = def.getSpine();
                List<Object> args = new ArrayList<Object>();
                for (int i = 0; i < def.numInputParams(); i++) {
                    TypeDef type = def.getParamType(i);
                    Object value = type.stringify(getValue(i));
                    args.add(value);
                }
                ExecuteRequest req = new ExecuteRequest(spine.getClientId(),
                        getUid(), getCaller() == null ? null : getCaller().getUid(), def.getName(), args, false);
                def.getBridge().getInvocationCache().add(this);
                spine.send(req);
            } catch (SpineException e) {
                throw new PALRemoteException(e);
            }
        } else {
            log.debug("Calling {} to run {}", exec, this);
            exec.execute(this);
        }
    }

    /**
     * Perform pre-execution checks if necessary. This implementation looks for
     * non-nullable arguments that are null, and logs a warning for them.
     *
     * @throws PALException
     *             if an argument error is detected.
     */
    protected void checkArgs()
            throws PALException {
        ActionDef def = getDefinition();
        for (int i = 0; i < def.numInputParams(); i++) {
            TypeDef type = def.getParamType(i);
            Object value = getValue(i);
            if (!(type instanceof NullableDef) && value == null) {
                log.warn("Param {} of {} is null but not nullable: type {}",
                        new Object[] { def.getParamName(i), this, type });
            }
        }
    }
    
    @Override
    public TypeDef getParamType(int i)
            throws PALException {
        return getDefinition().getParamType(i);
    }

    /**
     * Starts the execution of this invocation in stepped mode. Not all
     * executors support stepped mode. Stepped mode is another name for
     * debugging.
     *
     * @throws PALException
     *             if an error occurs
     */
    public void startStepping()
            throws PALException {
        if (getCaller() != null) {
            getCaller().waitUntilStarted();
        }
        ActionExecutor exec = getDefinition().getExecutor();
        log.debug("Calling {} to run {} in stepped mode", exec, this);
        exec.executeStepped(this);
    }

    /**
     * If the invocation is in state {@link Status.PAUSED}, this method will
     * cause it to resume execution.
     *
     * @param command
     *            the way in which execution should be resumed
     * @throws PALException
     *             if an error occurs
     */
    public void continueStepping(StepCommand command)
            throws PALException {
        continueStepping(command, null);
    }

    /**
     * If the invocation is in state {@link Status.PAUSED}, this method will
     * cause it to resume execution. In addition, the next action called by this
     * one will have its input parameters replaced.
     *
     * @param command
     *            the way in which execution should be resumed
     * @param actionArgs
     *            new input parameters for the next called (sub-)action
     * @throws PALException
     *             if an error occurs
     */
    public void continueStepping(StepCommand command,
                                 List<Object> actionArgs)
            throws PALException {
        ActionExecutor exec = getDefinition().getExecutor();
        exec.continueStepping(this, command, actionArgs);
    }

    void setLocation(int preorderIndex) {
        location = preorderIndex;
    }

    /**
     * If the invocation is {@link Status.PAUSED}, this method returns the
     * location of the execution counter within this invocation.
     *
     * @return a value which is interpreted in an implementation-dependent way
     */
    public int getLocation() {
        if (getStatus() == Status.PAUSED) {
            return location;
        } else {
            return -1;
        }
    }

    void setSubAction(SimpleTypeName name) {
        subActionName = name;
    }

    /**
     * If the invocation is {@link Status.PAUSED}, this method returns the name
     * of the sub-action which is preparing to execute.
     *
     * @return the name of the next action which this invocation will execute
     */
    public SimpleTypeName getSubAction() {
        if (getStatus() == Status.PAUSED) {
            return subActionName;
        } else {
            return null;
        }
    }

    @Override
    public ActionDef getDefinition() {
        return (ActionDef) super.getDefinition();
    }

    /**
     * Requests that this invocation stop. Not all invocations are capable of
     * being canceled. Even for those which are, it is possible this invocation
     * will have completed by the time the <code>cancel</code> call is received
     * and processed. If the cancel is successful in altering the behavior of
     * this invocation, its status will change to {@link Status#FAILED}.
     * <p>
     * If this invocation is not in the <code>RUNNING</code> or
     * <code>PAUSED</code> state, this call will do nothing. Otherwise, it will
     * call {@link ActionExecutor#cancel} of every executor in the system to
     * attempt to cancel the action.
     * <p>
     * After this method is called, the contents of any output parameters are
     * undefined.
     *
     * @return {@code true} if the cancel request was received by the
     *         appropriate executor.
     */
    public boolean cancel() {
        Status stat = getStatus();
        if (stat.equals(Status.RUNNING) ||
            stat.equals(Status.PAUSED)) {
            Spine spine = getDefinition().getSpine();
            CancelRequest msg = new CancelRequest(spine.getClientId(), getUid());
            try {
                return spine.send(msg);
            } catch (SpineException e) {
                log.warn("Unable to send cancel msg for " + this, e);
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public Object getValue(int pos) {
        if (pos >= getDefinition().size()) {
            throw new IllegalArgumentException("Cannot retrieve value " + pos
                    + " (max " + getDefinition().size() + ") from " + this
                    + " with " + getDefinition().size() + " parameters");
        }
        return super.getValue(pos);
    }

    public static enum StepCommand {
        /**
         * Step into the indicated sub-action and pause.
         */
        STEP_INTO,
        /**
         * Execute the next sub-action and pause.
         */
        STEP_OVER,
        /**
         * Execute sub-actions until this action is done, then pause.
         */
        STEP_OUT,
        /**
         * Continue running without pausing again, unless a breakpoint is hit.
         */
        CONTINUE
    }
}
