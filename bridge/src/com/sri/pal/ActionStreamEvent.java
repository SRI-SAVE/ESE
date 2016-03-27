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

// $Id: ActionStreamEvent.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.ExecutionStatus;
import com.sri.tasklearning.spine.messages.StartExecutionStatus;
import com.sri.tasklearning.spine.messages.SuccessExecutionStatus;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an action that can be observed in the application, or initiated by
 * the execution engine as part of a procedure.
 * <p>
 * One example is an {@link ActionInvocation}, which represents something done
 * by the application. The action in question could be initiated by the user
 * during demonstration, or by the execution engine during procedure execution.
 * <p>
 * Another example is a {@link GestureStart}, which is essentially a container for a
 * group of primitive actions which were performed by the user using a single UI
 * gesture. During procedure execution, the gesture will be "executed" by the
 * execution engine, but the gesture's component actions will be sent to the
 * application for execution as in the normal case.
 */
public abstract class ActionStreamEvent {
    private static final Logger log = LoggerFactory
            .getLogger(ActionStreamEvent.class);

    private final AbstractActionDef def;
    private final Bridge bridge;
    private final ActionStreamEvent caller;
    private long serial; // for cross-application learning
    private final TransactionUID uid;
    private Status status;
    private ErrorInfo lastError;
    private List<Object> values;
    private final ErrorFactory errorFactory;
    private final Set<ActionInvocationStatusListener> listeners;
    private final Set<ActionInvocationStatusListener> internalListeners;
    private final CountDownLatch startNotifyLock;

    protected ActionStreamEvent(AbstractActionDef definition,
                                Bridge myBridge,
                                ActionStreamEvent callingEvent,
                                long serialNum,
                                TransactionUID myUid) {
        def = definition;
        bridge = myBridge;
        caller = callingEvent;
        serial = serialNum;
        uid = myUid;
        status = Status.CREATED;
        lastError = null;
        values = new ArrayList<Object>();
        errorFactory = new ErrorFactory(bridge.getSpine().getClientId());
        listeners = new CopyOnWriteArraySet<ActionInvocationStatusListener>();
        internalListeners = new CopyOnWriteArraySet<ActionInvocationStatusListener>();
        startNotifyLock = new CountDownLatch(1);

        if (caller != null && !(caller.getStatus().equals(Status.RUNNING))) {
            if (caller.getStatus().equals(Status.CREATED)) {
                try {
                    caller.waitUntilRunning(10 * 1000);
                } catch (TimeoutException e) {
                    log.warn("The parent: {} of this action {}"
                            + " never transitioned from CREATED to RUNNING",
                            caller, this);
                }
            }
        }

        /*
         * If this action is handled by a local executor, we should take
         * responsibility for sending out execution status updates to other
         * Spine clients.
         */
        if (isLocallyExecuted()) {
            StateChangeBroadcaster br = new StateChangeBroadcaster();
            addInternalListener(br);
        }
    }

    protected Bridge getBridge() {
        return bridge;
    }

    /**
     * Provides the parent invocation of this invocation. In other words, this
     * method will return the invocation which caused the current invocation to
     * start. Typically the parent will actually be a
     * {@link ProcedureInvocation}. If this method returns null, it indicates
     * the current invocation is a top-level, or user-initiated, action.
     *
     * @return the calling invocation, or null
     */
    public ActionStreamEvent getCaller() {
        return caller;
    }

    TransactionUID getUid() {
        return uid;
    }

    void assignArgs(List<? extends Object> args)
            throws PALException {
        for (int i = 0; i < args.size(); i++) {
            Object arg = args.get(i);
            setValue(i, arg);
        }
    }

    protected void waitUntilStarted() {
        try {
            startNotifyLock.await();
        } catch (InterruptedException e) {
            log.warn("Interrupted", e);
        }
    }

    void finishedStartNotify() {
        startNotifyLock.countDown();
    }

    /**
     * Provides the definition of the action which this is an invocation of.
     *
     * @return the corresponding definition
     */
    public AbstractActionDef getDefinition() {
        return def;
    }

    /**
     * Provides the type of the specified parameter index. Usually this
     * information comes from the action definition and can be retrieved from
     * there as well. However, in the case of idioms there are no static type
     * declarations to retrieve from the idiom definition so you must retrieve
     * the type from the ActionStreamEvent itself. This method encapsulates
     * that requirement.
     *
     * @param i
     *            the index of the parameter to retrieve type information for
     * @return the type of the given parameter, or {@code null}
     * @throws PALException
     *             if the type cannot be retrieved from the action model
     */
    public abstract TypeDef getParamType(int i) throws PALException;

    /**
     * Registers a new status change listener for this invocation. Immediately
     * upon calling this method, the listener will be notified of this
     * invocation's current state. The listener will receive status change
     * notifications until either it is removed by the {@link #removeListener}
     * call, or the invocation transitions to a finished state.
     * <p>
     * If this invocation's state is other than <code>CREATED</code> or
     * <code>RUNNING</code>, this call will only notify the listener of the
     * current state; a finished invocation will have no further events to
     * report.
     *
     * @param listener
     *            the listener to register
     */
    public synchronized void addListener(ActionInvocationStatusListener listener) {
        sendStatus(listener, status);
        if (status != Status.ENDED && status != Status.FAILED) {
            listeners.add(listener);
        }
    }

    /**
     * This is just like {@link #addListener}, but the internal listeners get
     * called after all the others. This means that by the time our internal
     * listener signals the invocation has completed, all other listeners (like
     * the action's own executor) will have been notified. Everything should be
     * in a consistent state.
     *
     * @param listen
     *            the listener to add
     */
    private synchronized void addInternalListener(ActionInvocationStatusListener listen) {
        sendStatus(listen, status);
        if (status != Status.ENDED && status != Status.FAILED) {
            internalListeners.add(listen);
        }
    }

    /**
     * Removes a listener from this invocation's active list.
     *
     * @param listener
     *            the listener to remove
     */
    public synchronized void removeListener(ActionInvocationStatusListener listener) {
        listeners.remove(listener);
    }

    /**
     * Called by the {@link ActionExecutor} to indicate that an error has
     * occurred in execution. This invocation will pass the error on to the
     * appropriate {@link ActionInvocationStatusListener#error}. Calling this
     * method has the side effect of setting the action invocation's status to
     * {@link Status#FAILED}.
     * <p>
     * It is an error to call this method if the invocation's status is other
     * than <code>RUNNING</code>.
     *
     * @param error
     *            the error which occurred
     */
    public void error(ErrorInfo error) {
        if (!isLocallyExecuted()) {
            throw new IllegalStateException(
                    "Can't change state of non-locally executed action " + this);
        }
        handleError(error);
    }

    synchronized void handleError(ErrorInfo error) {
        if (status.equals(Status.CREATED)) {
            updateStatus(Status.RUNNING);
        }
        if (!status.equals(Status.RUNNING) && !status.equals(Status.PAUSED)) {
            if (status.equals(Status.FAILED)) {
                log.info("{} already failed, so cannot (re-)send error {}",
                        this, error);
                return;
            }
            throw new IllegalStateException(
                    "Cannot send error in non-running state (" + status + "): "
                            + error);
        }
        lastError = error;
        for (ActionInvocationStatusListener listener : listeners) {
            sendError(listener, error);
        }
        for (ActionInvocationStatusListener listener : internalListeners) {
            sendError(listener, error);
        }
        updateStatus(Status.FAILED);
    }

    private void sendError(ActionInvocationStatusListener listener,
                           ErrorInfo error) {
        try {
            listener.error(error);
        } catch (Exception e) {
            log.warn("Listener " + listener
                    + " failed to handle error notification for " + error, e);
        }
    }

    /**
     * Returns the last error encountered by this action invocation. The error,
     * if any, was reported by this action's {@link ActionExecutor} calling
     * {@link #error}. It could also result from a called invocation
     * encountering an error.
     *
     * @return the last reported error, or <code>null</code>
     */
    public ErrorInfo getError() {
        return lastError;
    }

    /**
     * Provides the current status of this invocation.
     *
     * @return this invocation's status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Called by the {@link ActionExecutor} to indicate the status of this
     * invocation to all its {@link ActionInvocationStatusListener}s.
     * <p>
     * When the invocation transitions to either of the finish states (
     * <code>ENDED</code> and <code>FAILED</code>), all listeners will be
     * removed.
     * <p>
     * All output parameters must be set before calling this method with
     * {@code ENDED}.
     *
     * @param newStatus
     *            the status to associate with this invocation
     */
    public void setStatus(Status newStatus) {
        if (!isLocallyExecuted()) {
            throw new IllegalStateException(
                    "Can't change state of non-locally executed action " + this);
        }
        updateStatus(newStatus);
    }

    synchronized void updateStatus(Status newStatus) {
        if (newStatus == null) {
            throw new NullPointerException("Cannot set status to null");
        }
        if (!legalStateTransition(status, newStatus)) {
            throw new IllegalStateException("Cannot move from " + status
                    + " to " + newStatus + ": UID " + uid);
        }
        if (log.isDebugEnabled()) {
            log.debug("Invocation {} moving from {} to {}", new Object[] {
                    this, status, newStatus });
        }

        /* Must move from CREATED to RUNNING before finishing. */
        if (status == Status.CREATED
                && (newStatus == Status.ENDED || newStatus == Status.FAILED)) {
            updateStatus(Status.RUNNING);
            // Note that status is now RUNNING.
        }

        /* If it's started running, notify everybody who cares about new events. */
        if (status == Status.CREATED && newStatus != Status.CREATED) {
            bridge.getGlobalNotifier().newInvocation(this);
        }

        /*
         * Check for overly long arguments if it's finished running -- meaning
         * the ActionExecutor has assigned output arguments.
         */
        if (newStatus == Status.ENDED) {
            try {
                checkStringSize();
            } catch (PALSizeException e) {
                log.debug("String size too big", e);
                ErrorInfo errorInfo = errorFactory.error(
                        ErrorType.STRING_TOO_BIG, getUid(),
                        new ArrayList<Integer>());
                error(errorInfo);
                return;
            } catch(PALException e) {
                log.debug("checkStringSize error", e);
                ErrorInfo errorInfo = errorFactory.error(
                        ErrorType.ACTION_MODEL, getDefinition());
                error(errorInfo);
                return;
            }
        }

        /* Actually change our status. */
        status = newStatus;

        /* Notify all listeners. */
        for (ActionInvocationStatusListener listener : listeners) {
            log.debug("Sending status {} to {}", newStatus, listener);
            sendStatus(listener, newStatus);
        }
        for (ActionInvocationStatusListener listener : internalListeners) {
            log.debug("Sending status {} to (internal) {}", newStatus, listener);
            sendStatus(listener, newStatus);
        }

        /*
         * Avoid memory leaks by removing all listeners if we're not going to
         * send any more events.
         */
        if (status.equals(Status.ENDED) || status.equals(Status.FAILED)) {
            listeners.clear();
            internalListeners.clear();
        }
    }

    private void checkStringSize()
            throws PALException {
        AbstractActionDef def = getDefinition();
        if (def == null) {
            /*
             * This is a non-idiom gesture. Its constituent actions and
             * containing procedure will all check their respective string
             * sizes.
             */
            return;
        }
        int stringSize = 0;
        for (int i = 0; i < def.size(); i++) {
            TypeDef type = getParamType(i);
            Object value = getValue(i);
            if (value != null) {
                Object strValue = type.stringify(value);
                stringSize += type.getStringSize(strValue);
            }
        }
        Bridge.checkStringSize(stringSize);
    }

    private void sendStatus(ActionInvocationStatusListener listener,
                            Status newStatus) {
        try {
            listener.newStatus(newStatus);
        } catch (Exception e) {
            log.warn("Listener " + listener + " for invocation " + this
                    + " threw an exception on change to " + newStatus, e);
        }
    }

    /**
     * Identical to {@link #waitUntilRunning()}, except with a timeout.
     *
     * @param timeout
     *            how long, in ms, to wait.
     * @throws InterruptedException
     *             if the timeout expires
     */
    public void waitUntilRunning(long timeout)
            throws TimeoutException {
        Listener listener = new Listener();
        addInternalListener(listener);
        listener.waitForRunning(timeout);
    }

    /**
     * Waits until this invocation is running. Or, more precisely, waits until
     * the invocation's status is other than <code>CREATED</code>. It is
     * possible for status to skip over <code>RUNNING</code> entirely.
     */
    public void waitUntilRunning() {
        Listener listener = new Listener();
        addInternalListener(listener);
        listener.waitForRunning();
    }

    /**
     * Waits until this invocation is finished. A finished invocation will have
     * status of either <code>ENDED</code> or <code>FAILED</code>.
     */
    public void waitUntilFinished() {
        Listener listener = new Listener();
        addInternalListener(listener);
        listener.waitForFinish();
    }

    private boolean legalStateTransition(Status oldStatus,
                                         Status newStatus) {
        boolean result;
        switch (oldStatus) {
        case CREATED:
            result = true;
            break;

        case RUNNING:
            if (newStatus.equals(Status.CREATED)) {
                result = false;
            } else {
                result = true;
            }
            break;

        case PAUSED:
            if (newStatus.equals(Status.CREATED)) {
                result = false;
            } else {
                result = true;
            }
            break;

            case ENDED:
        case FAILED:
            if (oldStatus.equals(newStatus)) {
                result = true;
            } else {
                result = false;
            }
            break;

        default:
            result = false;
        }

        return result;
    }

    /**
     * Provides the serial number of this invocation. Serial numbers are used
     * for cross-application instrumentation in order to provide an ordering for
     * the observed events. The serial numbers are assigned by a single
     * authority associated with the Spine, so there are no duplicates. Note
     * that serial numbers are only assigned for top-level action invocations,
     * meaning those for which {@code getParentInvocation() == null}.
     *
     * @return the serial number of this invocation, or 0
     */
    public long getSerialNumber() {
        return serial;
    }
    
    public void setSerialNumber(long serial) {
        this.serial = serial;
    }

    /**
     * Retrieves the value of a particular parameter to this invocation.
     *
     * @param pos
     *            the number of the field to retrieve
     * @return the current value of the requested field, or {@code null}
     */
    public Object getValue(int pos) {
        if (values.size() > pos) {
            return values.get(pos);
        } else {
            return null;
        }
    }

    /**
     * Retrieves the value of a particular parameter to this invocation, based
     * on the parameter's name in the associated action defintion.
     *
     * @param paramName
     *            the name of the parameter to retrieve
     * @return the value of the requested parameter
     * @see #getValue(int)
     * @see ActionDef#getParamNum
     */
    public Object getValue(String paramName) {
        int pos = getDefinition().getParamNum(paramName);
        return getValue(pos);
    }

    /**
     * Sets an input or output parameter to a given value.
     * <p>
     * Output parameters must all be set before setting the status of this
     * invocation to {@code ENDED}. Input parameters should not be changed from
     * the values initially assigned.
     *
     * @see com.sri.pal.Tuple#setValue(int, java.lang.Object)
     */
    public void setValue(int pos,
                         Object value) {
        AbstractActionDef def = getDefinition();
        Object oldValue = getValue(pos);
        if (status != Status.CREATED && oldValue != null
                && def.isInputParam(pos) && !oldValue.equals(value)) {
            log.warn("Changing input field " + def.getParamName(pos) + " of "
                    + status + " " + this + " from " + oldValue + " to "
                    + value);
        }
        if ((status == Status.ENDED || status == Status.FAILED)
                && !def.isInputParam(pos)) {
            log.warn("Changing output field {} of {} too late -- already {}",
                    new Object[] { def.getParamName(pos), this, status });
        }

        while (values.size() <= pos) {
            values.add(null);
        }
        values.set(pos, value);
    }

    /**
     * Sets the value of a particular parameter to this invocation, based on the
     * parameter's name in the definition of this action.
     *
     * @param paramName
     *            the name of the parameter to set
     * @param value
     *            the value to assign to the given parameter
     * @see #setValue(int, Object)
     * @see ActionDef#getParamNum
     */
    public void setValue(String paramName,
                         Object value) {
        int num = getDefinition().getParamNum(paramName);
        setValue(num, value);
    }

    protected boolean isLocallyExecuted() {
        ActionExecutor exec = getBridge().getActionModel().getExecutor(
                getDefinition().getName());
        if (exec == null) {
            return false;
        }
        if (exec instanceof LumenProcedureExecutor) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String defStr = "";
        String args = "";
        AbstractActionDef def = getDefinition();
        if (def != null) {
            defStr = def.toString();
            StringBuffer argsStr = new StringBuffer();
            for (int i = 0; i < def.size(); i++) {
                argsStr.append(def.getParamName(i));
                argsStr.append(": ");
                Object value = getValue(i);
                String argStr = "";
                if (value != null) {
                    argStr = getValue(i).toString();
                }
                if (argStr.length() > 80) {
                    argStr = argStr.substring(0, 80) + "...";
                }
                argsStr.append(argStr);
                if (i < def.size() - 1) {
                    argsStr.append(", ");
                }
            }
            args = " args: " + argsStr;
        }
        return getClass().getSimpleName() + "[" + defStr + "](" + serial + ")("
                + uid + ")" + args;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bridge == null) ? 0 : bridge.hashCode());
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ActionStreamEvent other = (ActionStreamEvent) obj;
        if (bridge == null) {
            if (other.bridge != null)
                return false;
        } else if (!bridge.equals(other.bridge))
            return false;
        if (uid == null) {
            if (other.uid != null)
                return false;
        } else if (!uid.equals(other.uid))
            return false;
        return true;
    }

    public static enum Status {
        /**
         * The invocation has been created, but it has not yet started
         * executing.
         */
        CREATED,
        /**
         * The invocation is running.
         */
        RUNNING,
        /**
         * The invocation is paused in debugging mode.
         */
        PAUSED,
        /**
         * The invocation has finished running. No errors were encountered.
         * Output parameters may be retrieved via {@link #getValue}.
         */
        ENDED,
        /**
         * The invocation failed to complete successfully. It may have
         * encountered an error, or it may have been canceled.
         */
        FAILED
    }

    private static class Listener
            implements ActionInvocationStatusListener {
        private Status stat;

        public synchronized void waitForRunning() {
            while (stat == Status.CREATED) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }

        public synchronized void waitForRunning(long timeout)
                throws TimeoutException {
            long startTime = System.currentTimeMillis();
            while (stat == Status.CREATED) {
                long timeElapsed = System.currentTimeMillis() - startTime;
                long waitTime = timeout - timeElapsed;
                if (waitTime < 0) {
                    throw new TimeoutException("Never started running after "
                            + timeout + " ms");
                }
                try {
                    wait(waitTime);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }

        public synchronized void waitForFinish() {
            while (stat != Status.ENDED && stat != Status.FAILED) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }

        @Override
        public synchronized void newStatus(Status newStatus) {
            stat = newStatus;
            notifyAll();
        }

        @Override
        public void error(ErrorInfo error) {
            // Ignore
        }
    }

    /**
     * Responsible for sending state change notification messages out to other
     * Spine clients.
     */
    private class StateChangeBroadcaster
            implements ActionInvocationStatusListener {
        @Override
        public void error(ErrorInfo error) {
            /*
             * We can ignore this, because it will be followed by a call to
             * newStatus().
             */
        }

        @Override
        public void newStatus(Status newStatus) {
            if (newStatus == Status.CREATED)
                return;

            TransactionUID parentUid = null;
            if (getCaller() != null) {
                parentUid = getCaller().getUid();
            }
            TransactionUID uid = getUid();
            if (uid == null) {
                log.warn("Never got task UID for invocation {}", this);
            }

            ActionStreamEvent event = ActionStreamEvent.this;
            AbstractActionDef def = getDefinition();

            List<Object> inParams = new ArrayList<Object>();
            String sender = bridge.getSpine().getClientId();

            if (def != null) {
                    for (int i = 0; i < def.numInputParams(); i++) {
                        Object value = getValue(i);
                        try {
                            TypeDef type = event.getParamType(i);
                            Object strValue = type.stringify(value);
                            inParams.add(strValue);
                        } catch (PALException e) {
                            log.warn(
                                    "Unable to stringify type "
                                            + def.getParamTypeName(i) + ", value "
                                            + value, e);
                        }
                    }
            }

            ExecutionStatus statusMsg;
            switch (newStatus) {
            case RUNNING:
                SimpleTypeName actionName = def == null ? null : def.getName();
                statusMsg = new StartExecutionStatus(sender, uid, parentUid,
                        actionName, getSerialNumber(), inParams);
                break;

            case ENDED:
                List<Object> outParams = new ArrayList<Object>();
                if (def != null) {
                        for (int i = def.numInputParams(); i < def.size(); i++) {
                            Object value = getValue(i);
                            try {
                                TypeDef type = event.getParamType(i);
                            Object strValue = type.stringify(value);
                                outParams.add(strValue);
                            } catch (PALException e) {
                                log.warn(
                                        "Unable to unstringify type "
                                                + def.getParamTypeName(i) + ", value "
                                                + value, e);
                            }
                        }
                }

                statusMsg = new SuccessExecutionStatus(sender, uid, parentUid,
                        inParams, outParams);
                break;

            case FAILED:
                ErrorInfo error = getError();
                if (error == null) {
                    log.warn("failed but no failure reason given for {}", this);
                    error = errorFactory.error(ErrorType.UNKNOWN);
                }
                statusMsg = new ErrorExecutionStatus(sender, uid,
                        parentUid, error);
                break;
            default:
                return;
            }

            try {
                getBridge().getSpine().send(statusMsg);
            } catch (Exception e) {
                // This warning is because we may leak memory.
                log.warn("Failed to publish status " + newStatus + " for " + this,
                        e);
            }
        }
    }

}
