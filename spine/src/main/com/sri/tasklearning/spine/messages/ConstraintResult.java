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

// $Id: ConstraintResult.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.pal.common.ErrorInfo;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class ConstraintResult
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final String constraintStr;
    private final ErrorInfo error;

    public ConstraintResult(String sender,
                            String constraintStr,
                            TransactionUID uid) {
        super(sender, uid, UserMessageType.CONSTRAINT_RESULT);
        this.constraintStr = constraintStr;
        error = null;
    }

    public ConstraintResult(String sender,
                            ErrorInfo error,
                            TransactionUID uid) {
        super(sender, uid, UserMessageType.CONSTRAINT_RESULT);
        this.error = error;
        constraintStr = null;
    }

    public String getConstraintStr() {
        return constraintStr;
    }

    public ErrorInfo getError() {
        return error;
    }

    public TransactionUID getUid() {
        return (TransactionUID) uid;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((constraintStr == null) ? 0 : constraintStr.hashCode());
        result = prime * result + ((error == null) ? 0 : error.hashCode());
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
        ConstraintResult other = (ConstraintResult) obj;
        if (constraintStr == null) {
            if (other.constraintStr != null)
                return false;
        } else if (!constraintStr.equals(other.constraintStr))
            return false;
        if (error == null) {
            if (other.error != null)
                return false;
        } else if (!error.equals(other.error))
            return false;
        return true;
    }
}
