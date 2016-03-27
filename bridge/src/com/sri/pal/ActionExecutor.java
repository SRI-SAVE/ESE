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

// $Id: ActionExecutor.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.List;

import com.sri.pal.ActionInvocation.StepCommand;

/**
 * Facility which can execute one or more user-level actions. Typically the
 * implementor of this interface will be a module of an application.
 *
 * @author chris
 */
public interface ActionExecutor {
    /**
     * Requests that a particular action be executed. The executor is expected
     * to use {@link ActionInvocation#setStatus} and
     * {@link ActionInvocation#error} to signal status information, and
     * {@link ActionInvocation#setValue} to assign output parameters.
     * <p>
     * The implementing class can handle this request either synchronously or
     * asynchronously; it is the caller's responsibility to watch the status of
     * the action invocation.
     *
     * @param invocation
     *            the action invocation to execute, with input parameters
     *            assigned
     * @throws PALException
     *             if the execution cannot be started; errors during execution
     *             will be reported via
     *             {@link ActionInvocationStatusListener#error}
     */
    public void execute(ActionInvocation invocation)
            throws PALException;

    /**
     * Requests that an action be executed in stepped mode -- in other words,
     * debugging. Implementing stepping is optional; this method may behave
     * identically to {@link #execute}.
     */
    public void executeStepped(ActionInvocation invocation)
            throws PALException;

    /**
     * Requests that a particular action or procedure invocation -- and all
     * actions called by it -- be canceled. That may be an action invocation
     * currently being executed by this implementor. However, typically the
     * event will be a procedure invocation, and this implementor is responsible
     * for checking all of its current and future action invocations via
     * {@link ActionStreamEvent#getCaller} to see if they were called by it.
     * Implementors should be aware that invocations they are running may be
     * called indirectly by the event to be canceled; it may be necessary to
     * check {@code getCaller().getCaller()} and so on. If the implementor finds
     * an action invocation which should be canceled, it can simply call
     * {@link ActionInvocation#error} or set its status to {@code FAILED}.
     * <p>
     * It is suggested that the implementor add a listener to {@code event}'s
     * status changes, because the implementor's responsibility is over once
     * {@code event} completes.
     *
     * @param event
     *            the invocation to cancel
     */
    public void cancel(ActionStreamEvent event);

    /**
     * Requests that a stepped invocation be continued. If the implementor does
     * not support stepped execution, this method should never be called; it may
     * throw an exception or do nothing.
     *
     * @param invocation
     *            the invocation to continue
     * @param command
     *            the nature of the continuation
     * @param actionArgs
     *            if non-null, these arguments will replace the ones that would
     *            otherwise be given to the next action this one calls.
     * @throws PALException
     *             if the invocation cannot be continued
     */
    public void continueStepping(ActionInvocation invocation,
                                 StepCommand command,
                                 List<Object> actionArgs)
            throws PALException;
}
