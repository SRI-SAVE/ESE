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

// $Id: ConstraintHandler.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.decl.impl.CTRActionDeclaration;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.ai.lumen.util.ConstraintChecker;
import com.sri.pal.common.ErrorInfo;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.mediators.TypeFetcher;
import com.sri.tasklearning.mediators.WithLockedTypes;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ConstraintRequest;
import com.sri.tasklearning.spine.messages.ConstraintResult;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles requests to coalesce constraints on newly learned procedures. Given
 * an unconstrained procedure, we retrieve all the actions which it calls so we
 * can look at their constraints. Then we ask Lumen to coalesce all of those
 * into a new set of constraint bindings for this procedure.
 */
public class ConstraintHandler
        implements MessageHandler {
    private static final Logger log = LoggerFactory
            .getLogger(ConstraintHandler.class);

    private final Spine spine;
    private final ExecutorService threadPool;
    private final WithLockedTypes withLockedTypes;
    private final ProcedureDependencyFinder finder;

    public ConstraintHandler(TypeFetcher typeFetcher,
                             LockingActionModel actionModel,
                             Spine spine) {
        this.spine = spine;
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newCachedThreadPool(tf);
        withLockedTypes = new WithLockedTypes(actionModel);
        finder = new ProcedureDependencyFinder(actionModel, typeFetcher);
    }

    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (!(message instanceof ConstraintRequest)) {
            log.warn("Unexpected message ({}): {}", message.getClass(), message);
            return;
        }

        ConstraintRequest req = (ConstraintRequest) message;

        ConstraintJob job = new ConstraintJob(req);
        threadPool.execute(job);
    }

    void shutdown() {
        threadPool.shutdown();
    }

    /**
     * Thread to handle getting constraints on a procedure.
     */
    private class ConstraintJob
            implements Runnable {
        private final ConstraintRequest req;
        private final TransactionUID uid;

        public ConstraintJob(ConstraintRequest req) {
            this.req = req;
            uid = req.getUid();
        }

        @Override
        public void run() {
            ErrorInfo error;
            String result = null;

            String procSrc = req.getProcSrc();
            log.debug("Coalescing constraints for {}", procSrc);
            ATRActionDeclaration proc;
            try {
                proc = ATRSyntax.CTR.declFromSource(CTRActionDeclaration.class, procSrc);

                ConstraintAction action = new ConstraintAction();
                withLockedTypes.lockedAction(action, proc, finder);
                error = action.getError();
                result = action.result();
            } catch (LumenSyntaxError e) {
                log.warn("Unable to parse: " + procSrc, e);
                error = ErrorFactory.error(spine.getClientId(),
                        ErrorType.INTERNAL_PARSE, procSrc);
            } catch (Exception e) {
                log.warn("Failed coalescing constraints: " + procSrc, e);
                error = ErrorFactory.error(spine.getClientId(),
                        ErrorType.CONSTRAINT_COALESCE, procSrc);
            }

            // Send the result.
            ConstraintResult reply;
            if (error != null) {
                reply = new ConstraintResult(spine.getClientId(), error, uid);
            } else {
                reply = new ConstraintResult(spine.getClientId(), result, uid);
            }
            try {
                spine.send(reply);
            } catch (SpineException e) {
                log.warn("Unable to send result: " + reply, e);
            }
        }
    }

    /**
     * This action is called by {@link WithLockedTypes} to handle the details of
     * calling Lumen to coalesce constraints.
     */
    private class ConstraintAction
            extends WithLockedTypes.Action<String, ATRActionDeclaration> {
        private String result;
        private ErrorInfo error;

        @Override
        public void run(ATRActionDeclaration proc,
                        List<ATRDecl> requiredTypes,
                        Runnable cleanupTask) {
            try {
                ConstraintChecker checker = new ConstraintChecker();
                for(ATRDecl decl : requiredTypes) {
                    if (decl instanceof ATRActionDeclaration) {
                        ATRActionDeclaration action = (ATRActionDeclaration) decl;
                        checker.add(action);
                    }
                }

                // Get the coalesced constraints for this procedure.
                ATRTerm constraints = checker.calculateConstraints(proc);
                result = ATRSyntax.toSource(constraints);
            } catch (RuntimeException e) {
                log.warn("Constraint coalesce failed for procedure "
                        + ATRSyntax.toSource(proc), e);
                error = ErrorFactory.error(spine.getClientId(),
                        ErrorType.CONSTRAINT_COALESCE, proc);
            } finally {
                cleanupTask.run();
            }
        }

        @Override
        public ErrorInfo getError() {
            return error;
        }

        @Override
        public String result() {
            return result;
        }
    }
}
