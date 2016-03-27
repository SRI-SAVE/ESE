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

// $Id: ExprEvalRequest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Sent from the Editor to the Lumen Mediator to request that a set of
 * expressions be evaluated in the context of a currently-paused procedure. Can
 * optionally include new or modified bindings entries for the variables which
 * are in scope in the target procedure.
 *
 * @see ExprEvalResult
 */
public class ExprEvalRequest
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;
    private final TransactionUID procUid;
    private final Map<String, Object> bindingsMods;
    private final List<String> exprs;

    /**
     * Creates a new message to have Lumen evaluate some expressions.
     *
     * @param sender
     *            the Spine client id which is sending this message
     * @param requestUid
     *            a unique UID to represent this message
     * @param procedureUid
     *            the UID of the paused procedure whose context will be used to
     *            evaluate the expressions
     * @param bindingsChanges
     *            new or modified bindings entries to add to the procedure's
     *            context before evaluating the expressions
     * @param expressions
     *            the expressions to evaluate
     */
    public ExprEvalRequest(String sender,
                           TransactionUID requestUid,
                           TransactionUID procedureUid,
                           Map<String, Object> bindingsChanges,
                           List<? extends ATRTerm> expressions) {
        super(sender, requestUid, UserMessageType.EXPR_EVAL_REQUEST);

        procUid = procedureUid;
        bindingsMods = bindingsChanges;
        exprs = new ArrayList<String>();
        for (ATRTerm expr : expressions) {
            String exprStr = ATRSyntax.toSource(expr);
            exprs.add(exprStr);
        }
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID) uid;
    }

    /**
     * Provides the UID of the target procedure within the context of which the
     * expressions will be evaluated.
     *
     * @return the paused procedure's UID
     */
    public TransactionUID getProcedureUid() {
        return procUid;
    }

    /**
     * Provides the bindings entries which will be added to or modified in the
     * procedure's context.
     *
     * @return a map of variable bindings
     */
    public Map<String, Object> getBindingsChanges() {
        return bindingsMods;
    }

    /**
     * Provides the expressions which will be evaluated by Lumen.
     *
     * @return the expressions which will be passed to Lumen for evaluation
     * @throws LumenSyntaxError
     *             if the expressions cannot be parsed from strings into their
     *             ATR representations
     */
    public List<ATRTerm> getExpressions()
            throws LumenSyntaxError {
        List<ATRTerm> result = new ArrayList<ATRTerm>();
        for (String exprStr : exprs) {
            ATRTerm expr = ATRSyntax.CTR.termFromSource(exprStr);
            result.add(expr);
        }
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + " (procUid " + procUid + ", bindings "
                + bindingsMods + ", exprs " + exprs + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((bindingsMods == null) ? 0 : bindingsMods.hashCode());
        result = prime * result + ((exprs == null) ? 0 : exprs.hashCode());
        result = prime * result + ((procUid == null) ? 0 : procUid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExprEvalRequest other = (ExprEvalRequest) obj;
        if (bindingsMods == null) {
            if (other.bindingsMods != null)
                return false;
        } else if (!bindingsMods.equals(other.bindingsMods))
            return false;
        if (exprs == null) {
            if (other.exprs != null)
                return false;
        } else if (!exprs.equals(other.exprs))
            return false;
        if (procUid == null) {
            if (other.procUid != null)
                return false;
        } else if (!procUid.equals(other.procUid))
            return false;
        return true;
    }
}
