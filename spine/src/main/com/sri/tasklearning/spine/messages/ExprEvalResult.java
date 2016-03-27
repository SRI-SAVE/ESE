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

// $Id: ExprEvalResult.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import java.util.List;

import com.sri.pal.common.ErrorInfo;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.messages.contents.UID;

/**
 * Sent from the Lumen Mediator back to the Editor in response to a
 * {@link ExprEvalRequest}. This message contains the results of expression
 * evaluation in the context of a paused procedure.
 */
public class ExprEvalResult
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;
    private final List<Object> exprValues;
    private final ErrorInfo errorInfo;

    /**
     * Creates a message containing the results of expression evaluation.
     *
     * @param sender
     *            the Spine client ID which will send this message
     * @param uid
     *            the UID of the {@link ExprEvalRequest} to which this message
     *            corresponds
     * @param values
     *            the values which result from expression evaluation
     */
    public ExprEvalResult(String sender,
                          UID uid,
                          List<Object> values) {
        super(sender, uid, UserMessageType.EXPR_EVAL_RESULT);
        exprValues = values;
        errorInfo = null;
    }

    /**
     * Creates a message containing an error which resulted from an attempt at
     * expression evaluation.
     *
     * @param clientId
     *            the Spine client ID which will send this message
     * @param uid
     *            the UID of the {@link ExprEvalRequest} to which this message
     *            corresponds
     * @param error
     *            the error which was encountered during processing
     */
    public ExprEvalResult(String sender,
                          TransactionUID uid,
                          ErrorInfo error) {
        super(sender, uid, UserMessageType.EXPR_EVAL_RESULT);
        errorInfo = error;
        exprValues = null;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID) uid;
    }

    /**
     * Provides the (stringified) values which result from expression
     * evaluation. Each member of this list corresponds to the expression at the
     * same index of {@link ExprEvalRequest#getExpressions}.
     *
     * @return the results of expression evaluation, or {@code null} if an error
     *         occurred
     */
    public List<Object> getValues() {
        return exprValues;
    }

    /**
     * Indicates if an error occurred while processing the expressions.
     *
     * @return the error which occurred, or {@code null} if processing was
     *         successful
     */
    public ErrorInfo getError() {
        return errorInfo;
    }

    @Override
    public String toString() {
        return super.toString() + " (values " + exprValues + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((exprValues == null) ? 0 : exprValues.hashCode());
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
        ExprEvalResult other = (ExprEvalResult) obj;
        if (exprValues == null) {
            if (other.exprValues != null)
                return false;
        } else if (!exprValues.equals(other.exprValues))
            return false;
        return true;
    }
}
