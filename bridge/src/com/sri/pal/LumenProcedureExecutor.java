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

// $Id: LumenProcedureExecutor.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.List;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRLocator;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.task.ATRAction;
import com.sri.pal.ActionInvocation.StepCommand;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.RequestCanceler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.upgrader.ProcedureUpgrader;
import com.sri.tasklearning.lumenpal.ExecutionHandler;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.BreakpointResponse;
import com.sri.tasklearning.spine.messages.BreakpointResponse.Command;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fills the role of the thing that executes Lumen procedures. In reality, this
 * is a proxy for Lumen itself, which may reside in a different JVM and which
 * this class communicates with via the Spine.
 */
public class LumenProcedureExecutor
        implements ProcedureExecutor {
    private static final Logger log = LoggerFactory
            .getLogger(LumenProcedureExecutor.class);

    static final String NAMESPACE = ExecutionHandler.NAMESPACE;

    private final Bridge bridge;
    private final ErrorFactory errorFactory;

    /**
     * Provides the namespace used for all Lumen procedures. Only Lumen
     * procedures are permitted to be in this namespace, and all Lumen
     * procedures must be.
     *
     * @return the Lumen namespace
     */
    public static String getNamespace() {
        return NAMESPACE;
    }

    /**
     * Provides the version of the namespace used for Lumen procedures. This
     * will increment as the procedure format changes.
     *
     * @return Lumen's namespace version.
     */
    public static String getVersion() {
        return LumenProcedureDef.SERIALIZATION_FORMAT_VERSION;
    }

    LumenProcedureExecutor(Bridge bridge) {
        this.bridge = bridge;
        errorFactory = new ErrorFactory(bridge.getSpine().getClientId());
    }

    private InvocationCache getInvocCache() {
        return bridge.getInvocationCache();
    }

    private Spine getSpine() {
        return bridge.getSpine();
    }

    @Override
    public LumenProcedureDef load(String xmlSource)
            throws PALException {
        SynchronousCallbackHandler<ProcedureDef> sch = new SynchronousCallbackHandler<ProcedureDef>();
        load(sch, xmlSource);
        try {
            return (LumenProcedureDef) sch.waitForResult();
        } catch (PALException e) {
            log.warn("Unable to load procedure, the action model corresponding to " +
                    "the procedure may be missing", e);
            throw(e);
        }
    }

    /**
     * Provides for loading a procedure based on Lumen's CTR representation of
     * that procedure. This is intended to be used by other learning components,
     * such as the procedure editor.
     *
     * @param sparklProc
     *            CTR representation of the procedure to load
     * @return the loaded procedure
     * @throws PALException
     *             if the procedure can't be loaded
     * @throws PALSerializationVersionException
     *             if the XML can be parsed, but is using an incompatible
     *             version of the PAL serialization format. If this occurs,
     *             upgrading is required using {@link ProcedureUpgrader}.
     */
    public LumenProcedureDef load(ATRActionDeclaration sparklProc)
            throws PALException {
        SynchronousCallbackHandler<ProcedureDef> sch = new SynchronousCallbackHandler<ProcedureDef>();
        load(sch, sparklProc);
        try {
            return (LumenProcedureDef) sch.waitForResult();
        } catch (PALException e) {
            log.warn("Unable to load procedure, the action model corresponding to " +
                    "the procedure may be missing", e);
            throw(e);
        }
    }

    /**
     * Asynchronous load method. Uses Lumen's CTR factory
     * {@link CTRElementConstructor}.
     *
     * @see com.sri.pal.ProcedureExecutor#load(com.sri.pal.common.CallbackHandler,
     *      java.lang.String)
     */
    @Override
    public RequestCanceler load(final CallbackHandler<ProcedureDef> callbackHandler,
                                String xmlSource) {
        StoreTypeTask chain = new StoreTypeTask(callbackHandler);
        RequestCanceler rc = LumenProcedureDef.newInstance(chain, xmlSource,
                true, bridge);
        chain.addCanceler(rc);
        return chain;
    }

    public RequestCanceler load(final CallbackHandler<ProcedureDef> callbackHandler,
                                ATRActionDeclaration decl) {
        StoreTypeTask chain = new StoreTypeTask(callbackHandler);
        RequestCanceler rc = LumenProcedureDef.newInstance(chain, decl, true,
                bridge);
        chain.addCanceler(rc);
        return chain;
    }

    @Override
    public void cancel(ActionStreamEvent invocation) {
        /*
         * Do nothing. This is because procedures are canceled by having a
         * called action fail. If somebody wants to cancel a procedure, they'll
         * call ActionInvocation#cancel, and that will broadcast a request to
         * all ActionExecutors to cancel any subtasks.
         */
    }

    private void execute(ActionInvocation invocation,
                         boolean stepped)
            throws PALException {
        if (!(invocation instanceof ProcedureInvocation)) {
            throw new IllegalArgumentException("Not a procedure invocation: "
                    + invocation);
        }
        ProcedureInvocation procInvoc = (ProcedureInvocation) invocation;
        if (!this.equals(procInvoc.getDefinition().getExecutor())) {
            throw new IllegalArgumentException(
                    "Doesn't belong to this executor: " + procInvoc);
        }
        if (!Status.CREATED.equals(procInvoc.getStatus())) {
            throw new IllegalStateException("Invocation has already started: "
                    + procInvoc);
        }

        LumenProcedureDef def = (LumenProcedureDef) procInvoc.getDefinition();
        SimpleTypeName actionName = def.getName();
        TransactionUID parentUid = null;
        List<Object> inParams = new ArrayList<Object>();
        int stringSize = 0;
        for (int i = 0; i < def.numInputParams(); i++) {
            TypeDef type = def.getParamType(i);
            Object value = procInvoc.getValue(i);
            Object strValue = type.stringify(value);
            inParams.add(strValue);
            stringSize += type.getStringSize(strValue);
        }
        Bridge.checkStringSize(stringSize);

        ExecuteRequest execMsg = new ExecuteRequest(getSpine().getClientId(),
                procInvoc.getUid(), parentUid, actionName, inParams, stepped);
        getInvocCache().add(procInvoc);
        log.debug("startTask({}, {})", procInvoc, execMsg);
        try {
            getSpine().send(execMsg);
        } catch (SpineException e) {
            throw new PALException("Failed to start task " + procInvoc, e);
        }
    }

    /**
     * Starts a procedure. This implementation is asynchronous; the call will
     * return quickly, and the procedure will run in another thread.
     *
     * @see com.sri.pal.ActionExecutor#execute(com.sri.pal.ActionInvocation)
     */
    @Override
    public void execute(ActionInvocation invocation)
            throws PALException {
        execute(invocation, false);
    }

    @Override
    public void executeStepped(ActionInvocation invocation)
            throws PALException {
        execute(invocation, true);
    }

    @Override
    public void continueStepping(ActionInvocation invocation,
                                 StepCommand command,
                                 List<Object> actionArgs)
            throws PALException {
        /*
         * If actionArgs is given, check it for correct length and stringify its
         * contents.
         */
        List<Object> strArgs;
        if (actionArgs == null) {
            strArgs = null;
        } else {
            /*
             * We need to stringify the arguments. In order to do that, we need
             * to know their types. The types are defined by the action
             * definition. We get that by inspecting the ATR procedure source
             * and looking for the action call at the location where this
             * procedure is currently paused.
             */
            LumenProcedureDef procDef = (LumenProcedureDef) invocation
                    .getDefinition();
            ATRActionDeclaration procAtr = procDef.getAtr();
            ATRLocator<ATR> locator = new ATRLocator<ATR>(procAtr);
            int preorderIndex = invocation.getLocation();
            ATR atrActionCall = locator.getATR(preorderIndex);
            ATRAction actionCall = (ATRAction) atrActionCall;
            String actionNameStr = actionCall.getFunctor();
            SimpleTypeName actionName = (SimpleTypeName) TypeNameFactory
                    .makeName(actionNameStr);
            ActionDef actDef = (ActionDef) bridge.getActionModel().getType(
                    actionName);

            /* Now we have the action definition. */
            if (actionArgs.size() != actDef.numInputParams()) {
                throw new PALException("Expecting null or "
                        + actDef.numInputParams() + " args; got "
                        + actionArgs.size());
            }
            strArgs = new ArrayList<Object>();
            for (int i = 0; i < actionArgs.size(); i++) {
                TypeDef type = actDef.getParamType(i);
                Object strValue = type.stringify(actionArgs.get(i));
                strArgs.add(strValue);
            }
        }

        Command cmd = Command.valueOf(command.name());
        TransactionUID uid = invocation.getUid();
        BreakpointResponse msg = new BreakpointResponse(getSpine().getClientId(),
                cmd, uid, strArgs);
        try {
            getSpine().send(msg);
        } catch (SpineException e) {
            throw new PALException("Unable to send message " + msg, e);
        }
    }

    private class StoreTypeTask
            extends AsyncChain<LumenProcedureDef, ProcedureDef> {
        public StoreTypeTask(CallbackHandler<ProcedureDef> subCallbackHandler) {
            super(subCallbackHandler);
        }

        @Override
        public void results(LumenProcedureDef result) {
            try {
                // TODO Make this call use the async pattern.
                bridge.getActionModel().storeType(result.getName(), result);
                subCH.result(result);
            } catch (Exception e) {
                String msg = "Failed to add: " + result.getName();
                log.error(msg, e);
                ErrorInfo error = errorFactory.error(ErrorType.ACTION_MODEL,
                        result);
                subCH.error(error);
            }
        }
    }
}
