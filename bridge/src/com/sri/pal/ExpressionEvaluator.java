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

// $Id: ExpressionEvaluator.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ExprEvalRequest;
import com.sri.tasklearning.spine.messages.ExprEvalResult;

/**
 * Tracks a set of variable bindings and allows for evaluation of
 * expressions using those bindings. 
 * 
 * @author mcintyre
 */
public final class ExpressionEvaluator {
    private final Map<String, Object> bindings = new HashMap<String, Object>();
    private final Bridge bridge;
    private final Spine spine;

    public ExpressionEvaluator(Bridge bridge) {
        this.bridge = bridge;
        this.spine = bridge.getSpine();
    }

    /**
     * Adds a binding to the expression evaluator
     * 
     * @param var the variable name to be bound
     * @param type the type of the variable being bound
     * @param value the value to be bound
     */
    public void addBinding(String var, TypeDef type, Object value) {
        bindings.put(var, type.stringify(value));
    }
    
    /**
     * Removes a binding from the expression evaluator
     * 
     * @param var the variable to be unbound
     */
    public void removeBinding(String var) {
        bindings.remove(var);
    }

    /**
     * Evaluates a list of ATRTerm expressions against the current bindings
     * 
     * @param exprs a list of ATRTerm expressions
     * @return the results of expression evaluation
     */
    public List<Object> evaluateExpressions(List<ATRTerm> exprs) 
            throws PALException {
        try {
            ExprEvalRequest msg = new ExprEvalRequest(
                    spine.getClientId(), spine.getNextUid(), 
                    null, bindings, exprs);

            ExprEvalResult result = 
                    bridge.getExprEvalReceiver().sendAndGetReply(msg);
            return result.getValues();
        } catch (SpineException e) {
            throw new PALException("Error while evaluating expressions", e);
        }
    }
}
