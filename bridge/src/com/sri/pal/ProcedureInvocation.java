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

// $Id: ProcedureInvocation.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ExprEvalRequest;
import com.sri.tasklearning.spine.messages.ExprEvalResult;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Represents the invocation of a procedure.
 *
 * @author chris
 */
public class ProcedureInvocation
        extends ActionInvocation {
    private static final Logger log = LoggerFactory
            .getLogger(ProcedureInvocation.class);

    ProcedureInvocation(ProcedureDef definition,
                        ActionStreamEvent caller,
                        long serial,
                        TransactionUID uid)
            throws PALException {
        super(definition, caller, serial, uid);
    }

    @Override
    public ProcedureDef getDefinition() {
        return (ProcedureDef) super.getDefinition();
    }

    @Override
    void assignArgs(List<? extends Object> args)
            throws PALException {
        super.assignArgs(args);
    }

    /**
     * May be called when a ProcedureInvocation's status is equal to
     * STATUS.PAUSED (during stepped execution) to evaluate instances of ATRTerm
     * using current runtime values.
     *
     * @param terms the terms to be evaluated
     * @param types the types of the terms to be evaluated. Must be the same
     *        length as terms.
     *
     * @return the results of term evaluation
     */
    public List<Object> evaluateTerms(
            final List<? extends ATRTerm> terms,
            final List<TypeDef> types) {

        if (this.getStatus() != Status.PAUSED)
            return null;

        final Spine spine = getBridge().getSpine();

        ExprEvalRequest msg = new ExprEvalRequest(
                spine.getClientId(), spine.getNextUid(), getUid(), null, terms);

        try {
            ExprEvalResult result =
                getBridge().getExprEvalReceiver().sendAndGetReply(msg);

            if (result == null || result.getValues() == null ||
                result.getValues().size() != types.size()) {
                return null;
            }

            final List<Object> unstringifieds = new ArrayList<Object>();
            int idx = 0;
            for (TypeDef type : types)
                unstringifieds.add(type.unstringify(result.getValues().get(idx++)));

            return unstringifieds;
        } catch (SpineException e) {
            log.error("Failed to evaluate ATRTerm expressions", e);
        }

        return null;
    }
}
