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

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.impl.CTRActionDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.learning.ATRDemonstratedAction;
import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.ai.lumen.atr.learning.impl.ATRDemonstratedActionImpl;
import com.sri.ai.lumen.atr.learning.impl.ATRDemonstrationImpl;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRNull;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.impl.CTRLiteral;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.RequestCanceler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.training.core.exercise.Option;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ConstraintRequest;
import com.sri.tasklearning.spine.messages.ConstraintResult;
import com.sri.tasklearning.spine.messages.LearnRequest;
import com.sri.tasklearning.spine.messages.LearnResult;
import com.sri.tasklearning.spine.messages.OptionLearnRequest;
import com.sri.tasklearning.spine.messages.OptionLearnResult;
import com.sri.tasklearning.spine.messages.ProcessDemoMessage;
import com.sri.tasklearning.spine.messages.contents.ActionCategory;
import com.sri.tasklearning.spine.messages.contents.ParamClass;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.spine.util.ReplyWatcher;
import com.sri.tasklearning.spine.util.TypeUtil;

/**
 * Provides access to the learning capabilities of the PAL system. Given a
 * demonstrated sequence of action invocations, a new procedure can be learned
 * which performs those actions, intelligently substituting variables for
 * certain parameters.
 *
 * @author chris
 */
public class Learner {
    private static final Logger log = LoggerFactory.getLogger(Learner.class);

    private static final Object PARAM_DESCR_STR = "LAPDOG-learned parameter";

    private final Bridge bridge;
    private ErrorFactory errorFactory;
    private int stringSize;

    Learner(Bridge bridge)
            throws PALRemoteException {
        this.bridge = bridge;
    }

    private ActionModel getActionModel() {
        return bridge.getActionModel();
    }

    private Spine getSpine() {
        return bridge.getSpine();
    }

    private ReplyWatcher<LearnResult> getLearnReceiver() {
        return bridge.getLearnReceiver();
    }

    private ReplyWatcher<OptionLearnResult> getLearnOptionReceiver() {
        return bridge.getLearnOptionReceiver();
    }

    private ReplyWatcher<ConstraintResult> getConstraintReceiver() {
        return bridge.getConstraintReceiver();
    }

    private ReplyWatcher<ProcessDemoMessage> getIdiomReceiver() {
        return bridge.getIdiomReceiver();
    }

    private LumenProcedureExecutor getExecutor() {
        return (LumenProcedureExecutor) bridge.getPALExecutor();
    }

    private ErrorFactory getErrorFactory() {
        if (errorFactory == null) {
            errorFactory = new ErrorFactory(getSpine().getClientId());
        }
        return errorFactory;
    }

    private LearnRequest buildRequest(String name,
                                      Properties learnProps,
                                      Set<TypeName> extraTypes,
                                      ActionStreamEvent... actions)
            throws PALException {
        if (learnProps == null) {
            learnProps = new Properties();
        }

       // Construct the canonical name of the procedure to be learned.
        SimpleTypeName typeName = (SimpleTypeName) TypeNameFactory.makeName(
                name, LumenProcedureDef.SERIALIZATION_FORMAT_VERSION,
                LumenProcedureExecutor.getNamespace());

        // Convert the demonstration to a form we can transmit.
        ATRDemonstration demo = demoToAtr(actions);

        // Make the learn request.
        final TransactionUID uid = getSpine().getNextUid();
        LearnRequest learnMsg = new LearnRequest(getSpine().getClientId(),
                demo, typeName, learnProps, extraTypes, uid);

        log.debug("Built learn request: {}", learnMsg);

        return learnMsg;
    }    

    public synchronized ATRDemonstration demoToAtr(ActionStreamEvent... actions)
            throws PALException {
        List<ATRDemonstratedAction> atrActions = new ArrayList<ATRDemonstratedAction>();
        stringSize = 0;
        List<ATRDemonstratedAction> gestureContents = null;
        for (ActionStreamEvent invoc : actions) {
            if (invoc instanceof GestureStart) {
                if (gestureContents != null) {
                    throw new PALException("Nested gestures not allowed");
                }
                gestureContents = new ArrayList<ATRDemonstratedAction>();
            } else if (invoc instanceof GestureEnd) {
                if (gestureContents == null) {
                    throw new PALException("Unmatched GestureEnd");
                }
                ATRDemonstratedAction gesture = new ATRDemonstratedActionImpl(
                        gestureContents);
                atrActions.add(gesture);
                gestureContents = null;
            } else if (invoc instanceof ActionInvocation) {
                ATRDemonstratedAction atrAction = actionToAtr((ActionInvocation) invoc);
                if (gestureContents == null) {
                    atrActions.add(atrAction);
                } else {
                    gestureContents.add(atrAction);
                }
            } else {
                throw new IllegalArgumentException("Unrecognized event ("
                        + invoc.getClass() + ") " + invoc);
            }
        }
        ATRDemonstration demo = new ATRDemonstrationImpl(atrActions);

        // Verify that we're not sending too much data.
        Bridge.checkStringSize(stringSize);

        return demo;
    }

    public List<ActionStreamEvent> demoFromAtr(List<ATRDemonstratedAction> atrActions)
            throws PALException {
        List<ActionStreamEvent> result = new ArrayList<ActionStreamEvent>();
        for (ATRDemonstratedAction atrAct : atrActions) {
            /*
             * Each item could be an action call, a non-idiom gesture, or an
             * idiom gesture. Gestures have opt.body() non-null. Actions and
             * idioms have a valid name. Idioms have non-null
             * optParameterTypes().
             */

            List<ATRDemonstratedAction> body = atrAct.optBody();
            if (body == null) {
                /* Simple action. */
                result.add(actionFromAtr(atrAct));
            } else {
                /* Gesture, possibly an idiom. */
                String actNameStr = atrAct.getName();
                SimpleTypeName actName = null;
                if (actNameStr != null && !actNameStr.equals("")) {
                    actName = (SimpleTypeName) TypeNameFactory
                            .makeName(actNameStr);
                }

                IdiomDef idiom;
                List<TypeName> paramTypes;
                List<Object> args;
                if (actName == null) {
                    idiom = null;
                    paramTypes = null;
                    args = null;
                } else {
                    idiom = (IdiomDef) getActionModel().getType(actName);
                    /* Null check in case the idiom can't be found. */
                    if (idiom == null) {
                        throw new PALException("Couldn't load idiom " + actName);
                    }

                    /*
                     * Assign param types and values. We have to unstringify the
                     * values.
                     */
                    List<String> typeStrs = atrAct.optParameterTypes();
                    List<Object> strArgs = atrAct.getActionArguments();
                    paramTypes = new ArrayList<TypeName>();
                    args = new ArrayList<Object>();
                    for (int i = 0; i < typeStrs.size(); i++) {
                        String nameStr = typeStrs.get(i);
                        Object strArg = strArgs.get(i);
                        TypeName name = TypeNameFactory.makeName(nameStr);
                        paramTypes.add(name);
                        TypeDef typeDef = (TypeDef) getActionModel().getType(
                                name);
                        Object arg = typeDef.unstringify(strArg);
                        args.add(arg);
                    }
                }

                /*
                 * Add the gesture start, followed by all the contained actions,
                 * followed by the gesture end.
                 */
                GestureStart start = GestureStart.newInstance(bridge, idiom,
                        actName != null ? actName.getIdiomTemplateName() : null,
                                paramTypes);
                if (idiom != null) {
                    for (int i = 0; i < idiom.size(); i++) {
                        start.setValue(i, args.get(i));
                    }
                }
                result.add(start);
                result.addAll(demoFromAtr(body));
                GestureEnd end = GestureEnd.newInstance(start);
                result.add(end);
            }
        }

        return result;
    }

    /**
     * Does more than parse. Adds extra properties to the ATR structure.
     */
    private LumenProcedureDef parse(String procBody,
                                    String constraintStr)
            throws LumenSyntaxError,
            PALException {
        CTRConstructor builder = new CTRConstructor();

        // Parse the string into an ActionDeclaration.
        log.debug("LAPDOG returned: {}", procBody);
        CTRActionDeclaration sparklProc = ATRSyntax.CTR.declFromSource(
                CTRActionDeclaration.class, procBody);

        @SuppressWarnings("unchecked")
        Map<String, ATRTerm> propMap = (Map<String, ATRTerm>) sparklProc
                .getProperties().getMap();

        // Add the constraint term.
        ATRTerm constraintTerm = ATRSyntax.CTR.termFromSource(constraintStr);
        if (!(constraintTerm instanceof ATRNull)) {
            propMap.put(TypeUtil.CONSTRAINTS, constraintTerm);
        }

        // Set other properties so this can be handled by accessors in ActionDef etc.
        propMap.put(TypeUtil.ACTION_CATEGORY, new CTRLiteral(
                ActionCategory.EFFECTOR.getName()));
        for (int i = 0; i < sparklProc.getSignature().getElements().size(); i++) {
            ATRParameter param = sparklProc.getSignature().getElements().get(i);
            String paramName = param.getVariable().getVariableName();
            Map<String, ATRTerm> paramPropMap = new HashMap<String, ATRTerm>();
            if (param.getMode() == Modality.INPUT) {
                paramPropMap.put(TypeUtil.PARAM_CLASS,
                        builder.createLiteral(ParamClass.GENERALIZABLE.getName(), null));
            } else {
                paramPropMap.put(TypeUtil.PARAM_CLASS,
                        builder.createLiteral(ParamClass.EXTERNAL.getName(), null));
            }
            paramPropMap.put(TypeUtil.PARAM_DESCRIPTION,
                    builder.createLiteral(PARAM_DESCR_STR, null));
            ATRMap paramProps = builder.createMap(paramPropMap);
            propMap.put("$" + paramName, paramProps);
        }

        // Build a SparkProcedureDef out of the ActionDeclaration.
        LumenProcedureDef palProc = getExecutor().load(sparklProc);

        log.debug("Built proc: {}", palProc);

        return palProc;
    }

    public ATRDemonstratedAction actionToAtr(ActionInvocation action)
            throws PALException {
        ATRDemonstratedAction result;
        AbstractActionDef actionDef = action.getDefinition();
        SimpleTypeName name = actionDef.getName();
        List<Object> params = new ArrayList<Object>();
        for (int i = 0; i < actionDef.size(); i++) {
            Object paramValue = action.getValue(i);
            TypeDef paramType = actionDef.getParamType(i);
            Object strValue = paramType.stringify(paramValue);
            stringSize += paramType.getStringSize(strValue);
            params.add(strValue);
        }

        result = new ATRDemonstratedActionImpl(name.getFullName(), params);
        return result;
    }

    private ActionStreamEvent actionFromAtr(ATRDemonstratedAction atr)
            throws PALException {
        String actNameStr = atr.getName();
        SimpleTypeName actName = (SimpleTypeName) TypeNameFactory
                .makeName(actNameStr);
        ActionDef def = (ActionDef) getActionModel().getType(actName);
        if (def == null) {
            throw new PALActionMissingException(actName);
        }
        ActionInvocation event = def.invoke(null);
        List<Object> atrParams = atr.getActionArguments();
        for (int i = 0; i < def.size(); i++) {
            Object strValue = atrParams.get(i);
            TypeDef type = def.getParamType(i);
            Object value = type.unstringify(strValue);
            event.setValue(i, value);
        }
        return event;
    }

    /**
     * Synchronously learn a new procedure which performs the specified sequence
     * of actions. The new procedure will automatically be marked as transient
     * and made available via the action model.
     *
     * @param name
     *            the name of the new procedure
     * @param learnProps
     *            properties to control behavior for this call to learn. See
     *            {@link com.sri.ai.tasklearning.lapdog.LapdogConfiguration} for more detail.
     * @param extraTypes
     *            additional types which LAPDOG should be made aware of before
     *            learning happens. Currently, this is only useful for sending
     *            completer actions to LAPDDOG. May be {@code null}.
     * @param actions
     *            the demonstration
     * @return a procedure which performs the requested actions
     * @throws PALException
     *             if learning failed
     * @see ActionDef#isTransient
     */
    public ProcedureDef learn(String name,
                              Properties learnProps,
                              Set<TypeName> extraTypes,
                              ActionStreamEvent... actions)
            throws PALException {
        // Build the learn request.
        LearnRequest request = buildRequest(name, learnProps, extraTypes, actions);

        // Send it to LAPDOG, and wait for the response.
        LearnResult learnResult;
        try {
            learnResult = getLearnReceiver().sendAndGetReply(request);
        } catch (SpineException e) {
            throw new PALException("Unable to send request " + request, e);
        }

        log.debug("LAPDOG returned: {}", learnResult);

        // Was it an error?
        ErrorInfo error = learnResult.getError();
        if (error != null) {
            throw new PALException("Learning failed: " + error);
        }
        String procBody = learnResult.getProcedureSource();

        // Coalesce constraints.
        ConstraintRequest constraintReq = new ConstraintRequest(getSpine()
                .getClientId(), procBody, getSpine().getNextUid());
        ConstraintResult constraintRes;
        try {
            constraintRes = getConstraintReceiver().sendAndGetReply(
                    constraintReq);
        } catch (SpineException e) {
            throw new PALException("Unable to send request " + constraintReq, e);
        }

        // Error?
        error = constraintRes.getError();
        if (error != null) {
            throw new PALException("Constraint coalescing failed: " + error);
        }
        String constraintStr = constraintRes.getConstraintStr();

        // Parse it.
        LumenProcedureDef proc;
        try {
            proc = parse(procBody, constraintStr);
        } catch (LumenSyntaxError e) {
            throw new PALException("Unable to parse result from LAPDOG: "
                    + procBody, e);
        }

        return proc;
    }

    public ProcedureDef learn(String name,
                              Set<TypeName> extraTypes,
                              ActionStreamEvent... actions)
        throws PALException {
        return learn(name, null, extraTypes, actions);                             
    }
    
    /**
     * Asynchronously learn a new procedure which performs the specified
     * sequence of actions.
     * <p>
     * Note that there is a race condition possible when canceling this
     * operation via the returned <code>RequestCanceler</code>. If this task
     * finishes, and another starts immediately after it, and this one is
     * canceled at the wrong time, it's possible for the next learn task to get
     * canceled instead of this one.
     *
     * @param callbackHandler
     *            the callback handler which will receive the newly learned
     *            procedure
     * @param name
     *            the name to be assigned to the new procedure
     * @param learnProps
     *            properties to control behavior for this call to learn. See
     *            {@link com.sri.ai.tasklearning.lapdog.LapdogConfiguration} for more detail.
     * @param extraTypes
     *            additional types which LAPDOG should be made aware of before
     *            learning happens. Currently, this is only useful for sending
     *            completer actions to LAPDDOG. May be {@code null}.
     * @param actions
     *            the demonstration
     * @return an object which may be used to request the learn operation to
     *         stop
     */
    public synchronized RequestCanceler learn(CallbackHandler<ProcedureDef> callbackHandler,
                                              String name,
                                              Properties learnProps,
                                              Set<TypeName> extraTypes,
                                              ActionStreamEvent... actions) {
        // Synchronously build the request.
        LearnRequest learnRequest;
        try {
            learnRequest = buildRequest(name, learnProps, extraTypes, actions);
        } catch (PALSizeException e) {
            ErrorInfo error = getErrorFactory().error(ErrorType.STRING_TOO_BIG);
            callbackHandler.error(error);
            return new RequestCanceler() {
                @Override
                public void cancel() {
                }
            };
        } catch (PALException e) {
            log.debug("Couldn't build request", e);
            ErrorInfo error = getErrorFactory().error(ErrorType.ACTION_MODEL,
                    name);
            callbackHandler.error(error);
            return new RequestCanceler() {
                @Override
                public void cancel() {
                }
            };
        }

        // This callback handler receives LAPDOG's result and sends it to Lumen
        // for constraint coalescing.
        Chain1 chain1 = new Chain1(callbackHandler);

        try {
            getLearnReceiver().sendAndGetReply(chain1, learnRequest);
        } catch (SpineException e) {
            log.warn("Couldn't send learn request " + learnRequest, e);
            ErrorInfo error = getErrorFactory().error(ErrorType.INTERNAL_COMMS);
            callbackHandler.error(error);
        }

        /*
         * TODO Implement cancel for chain1. It should send a cancel message to
         * the LAPDOG Mediator.
         */
        return chain1;
    }

    /**
     * Attempt to match a gesture against the known idiom patterns, thus
     * identifying the idiom in use. The provided actions should begin with
     * {@link GestureStart} and end with {@link GestureEnd}. If these actions
     * match a known idiom pattern, the idiom will be identified by the
     * {@link GestureStart#getDefinition} method in the returned demonstration.
     * Otherwise, the provided demonstration will be returned unchanged.
     *
     * @param demonstration
     *            the demonstration of a gesture to be matched against known
     *            idioms
     * @return a possibly modified copy of the provided gesture
     * @throws PALException
     *             if an error occurs
     */
    public List<ActionStreamEvent> recognizeIdiom(ActionStreamEvent... demonstration)
            throws PALException {
        ATRDemonstration demo = demoToAtr(demonstration);
        TransactionUID uid = getSpine().getNextUid();
        ProcessDemoMessage req = new ProcessDemoMessage(getSpine()
                .getClientId(), demo, uid);

        // Send it to LAPDOG, and wait for the response.
        ProcessDemoMessage idiomResult;
        try {
            idiomResult = getIdiomReceiver().sendAndGetReply(req);
        } catch (SpineException e) {
            throw new PALException("Unable to send request " + req, e);
        }

        log.debug("LAPDOG returned: {}", idiomResult);

        // Was it an error?
        ErrorInfo error = idiomResult.getError();
        if (error != null) {
            throw new PALException("Learning failed: " + error);
        }

        ATRDemonstration newDemo = idiomResult.getDemonstration();
        List<ActionStreamEvent> result;
        if (newDemo == null) {
            result = Arrays.asList(demonstration);
        } else {
            result = demoFromAtr(newDemo.getActions());
        }
        return result;
    }

    /**
     * Given the source for a procedure, asynchronously apply the constraint
     * coalescing algorithm to it, resulting in a procedure with constraints.
     *
     * @param callbackHandler
     *            the callback handler which will receive the constrained
     *            procedure
     * @param procBody
     *            a string containing the source of the procedure to constrain
     * @return a request canceler which can be used to cancel this activity
     */
    public RequestCanceler constrainProcedure(CallbackHandler<ProcedureDef> callbackHandler,
                                              String procBody) {
        Chain2 chain2 = new Chain2(callbackHandler, procBody);

        // Send the constraint coalesce request to Lumen.
        ConstraintRequest req = new ConstraintRequest(getSpine()
                .getClientId(), procBody, getSpine().getNextUid());
        try {
            getConstraintReceiver().sendAndGetReply(chain2, req);
        } catch (SpineException e) {
            log.warn("Couldn't send constraint request " + req, e);
            ErrorInfo error = getErrorFactory().error(ErrorType.INTERNAL_COMMS);
            callbackHandler.error(error);
        }

        return chain2;
    }

    /**
     * Synchronously learn an Option from a demonstration. The Option is one
     * part of a task solution (AKA gold standard), for use with the DEFT
     * training software's automated assessment. The result of this call will
     * probably require additional editing to make a good task solution.
     *
     * @param actions
     *            the demonstrated actions
     * @return a new option for use in a task solution
     * @throws PALException
     *             if learning failed
     */
    public Option learnOption(ActionStreamEvent... actions)
            throws PALException {
        // Convert the demonstration to a form we can transmit.
        ATRDemonstration demo = demoToAtr(actions);

        // Build the request.
        final TransactionUID uid = getSpine().getNextUid();
        OptionLearnRequest request = new OptionLearnRequest(getSpine()
                .getClientId(), uid, demo);

        // Send it to LAPDOG, and wait for the response.
        OptionLearnResult result;
        try {
            result = getLearnOptionReceiver().sendAndGetReply(request);
        } catch (SpineException e) {
            throw new PALException("Unable to send request " + request, e);
        }

        log.debug("LAPDOG returned: {}", result);

        // Was it an error?
        ErrorInfo error = result.getError();
        if (error != null) {
            throw new PALException("Learning failed: " + error);
        }

        try {
            return result.getOption();
        } catch (Exception e) {
            log.info("Failed to unmarshal {}", result.getOptionSrc());
            throw new PALException("unmarshal failed", e);
        }
    }

    private class Chain1
            extends AsyncChain<LearnResult, ProcedureDef> {
        public Chain1(CallbackHandler<ProcedureDef> subCallbackHandler) {
            super(subCallbackHandler);
        }

        @Override
        public void results(LearnResult result) {
            log.debug("LAPDOG returned: {}", result);

            // Get the procedure and the error. One of these will be null.
            String procBody = result.getProcedureSource();
            ErrorInfo error = result.getError();

            // Was it an error?
            if (error != null) {
                subCH.error(error);
                return;
            }

            RequestCanceler rc = constrainProcedure(subCH, procBody);
            addCanceler(rc);
        }
    }

    private class Chain2
            extends AsyncChain<ConstraintResult, ProcedureDef> {
        private final String procBody;

        public Chain2(CallbackHandler<ProcedureDef> subCallbackHandler,
                      String procedureBody) {
            super(subCallbackHandler);
            procBody = procedureBody;
        }

        @Override
        public void results(ConstraintResult result) {
            log.debug("Constraint coalescing returned: {}", result);

            // Get the constraint string and the error. One will be null.
            String constraintStr = result.getConstraintStr();
            ErrorInfo error = result.getError();

            // Was it an error?
            if (error != null) {
                subCH.error(error);
                return;
            }

            // Parse the response.
            try {
                LumenProcedureDef proc = parse(procBody, constraintStr);
                subCH.result(proc);
            } catch (Exception e) {
                log.warn("Failed to load learned procedure into lumen: "
                        + procBody, e);
                error = getErrorFactory().error(ErrorType.INTERNAL_PARSE,
                        procBody);
                subCH.error(error);
                return;
            }
        }
    }
}
