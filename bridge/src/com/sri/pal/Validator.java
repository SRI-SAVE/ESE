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

// $Id: Validator.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.decl.Declarations;
import com.sri.ai.lumen.editorsupport.ProcedureInfo;
import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.RequestCanceler;
import com.sri.pal.common.TypeName;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.spine.util.TypeUtil;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper class to call Lumen for procedure validation. This is particularly
 * useful to a procedure editor.
 */
public class Validator {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Bridge bridge;
    private final ActionModel actionModel;
    private ExecutorService threadPool;

    /**
     * Construct a new Validator which will be able to validate different
     * procedures.
     *
     * @param bridge
     *            a Bridge which will be used to retrieve required types and
     *            actions
     */
    public Validator(Bridge bridge) {
        this.bridge = bridge;
        actionModel = bridge.getActionModel();
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newCachedThreadPool(tf);
    }

    /**
     * Constructs a ProcedureInfo object which contains validation results for a
     * particular procedure.
     *
     * @param act
     *            the procedure to validate
     * @return an object which can be interrogated to discover warnings, errors,
     *         and suggestions
     * @throws PALException
     *             if a required type cannot be retrieved
     */
    public ProcedureInfo makeProcedureInfo(ATRActionDeclaration act)
            throws PALException {
        TypeFetcher decls = new TypeFetcher(act);
        decls.fetchAll();
        return ProcedureInfo.getProcedureInfo(act, decls);
    }

    /**
     * Asynchronously constructs a ProcedureInfo which contains validation
     * results for a particular procedure.
     *
     * @param callbackHandler
     *            will receive the ProcedureInfo containing validation results
     * @param act
     *            the procedure to validate
     * @return can be used to cancel the request if type retrieval is taking too
     *         long
     */
    public RequestCanceler makeProcedureInfo(CallbackHandler<ProcedureInfo> callbackHandler,
                                             final ATRActionDeclaration act) {
        TypeFetcher decls = new TypeFetcher(act);
        AsyncChain<Declarations, ProcedureInfo> chain = new AsyncChain<Declarations, ProcedureInfo>(
                callbackHandler) {
            @Override
            public void results(Declarations decls) {
                ProcedureInfo pi = ProcedureInfo.getProcedureInfo(act, decls);
                subCH.result(pi);
            }
        };
        return decls.fetchAll(chain);
    }

    /**
     * Does the work of retrieving dependent types, either synchronously or
     * asynchronously, for a given procedure. The types must all be fetched
     * before the ProcedureInfo can be created. Lumen's getProcedureInfo method
     * needs an object (like this one) which implements Declarations, meaning
     * it's effectively a map of type name -> definition.
     */
    private class TypeFetcher
            implements Declarations {
        private final ATRActionDeclaration proc;
        private final Map<String, ATRDecl> declMap;

        private TypeFetcher(ATRActionDeclaration act) {
            proc = act;
            declMap = new HashMap<String, ATRDecl>();
        }

        /**
         * Synchronously fetch all the required types.
         *
         * @throws PALException
         *             if something can't be retrieved
         */
        private void fetchAll()
                throws PALException {
            for (TypeName name : TypeUtil.getRequiredTypes(proc)) {
                ActionModelDef amDef = actionModel.getType(name);
                if (amDef == null) {
                    throw new PALException("Couldn't get action/type " + name);
                }
                add(amDef);
                for (ActionModelDef def : amDef.getRequiredDefs()) {
                    add(def);
                }
            }
        }

        /**
         * Asynchronously fetch all the required types, allowing for a cancel to
         * occur in the middle.
         *
         * @param callbackHandler
         *            where to send this after we're populated with all the
         *            required types
         * @return a request canceler
         */
        private RequestCanceler fetchAll(final CallbackHandler<Declarations> callbackHandler) {
            InnerFetcher job = new InnerFetcher(callbackHandler);
            threadPool.execute(job);
            return job;
        }

        /**
         * Adds the given type or action to this object's map. Doesn't add
         * things that Lumen isn't looking for, like constraints.
         *
         * @param amDef
         *            the type or action to add
         */
        private void add(ActionModelDef amDef) {
            ATR atr = amDef.getAtr();
            if (atr instanceof ATRDecl) {
                declMap.put(amDef.getName().getFullName(), (ATRDecl) atr);
            }
        }

        @Override
        public ATRDecl get(String name) {
            return declMap.get(name);
        }

        @Override
        public void preFetch(Collection<String> name) {
            /* No action necessary. */
        }

        /**
         * Does the crazy asynchronous stuff for the asynchronous fetchAll
         * method.
         */
        private class InnerFetcher
                implements Runnable, RequestCanceler {
            private boolean keepRunning = true;
            private final CallbackHandler<Declarations> callbackHandler;

            private InnerFetcher(CallbackHandler<Declarations> ch) {
                callbackHandler = ch;
            }

            @Override
            public void run() {
                try {
                    for (TypeName name : TypeUtil.getRequiredTypes(proc)) {
                        if (keepRunning) {
                            ActionModelDef amDef = actionModel.getType(name);
                            add(amDef);
                            for (ActionModelDef def : amDef.getRequiredDefs()) {
                                add(def);
                            }
                        }
                    }
                } catch (Exception e) {
                    String actStr = "null";
                    if (proc != null) {
                        actStr = ATRSyntax.toSource(proc);
                    }
                    log.info("Unable to get required types for " + actStr, e);
                    ErrorInfo err = ErrorFactory.error(bridge.getSpine()
                            .getClientId(), ErrorType.NOT_ALL_LOADED, actStr);
                    callbackHandler.error(err);
                }
                if (keepRunning) {
                    callbackHandler.result(TypeFetcher.this);
                }
            }

            @Override
            public void cancel() {
                keepRunning = false;
            }
        }
    }
}
