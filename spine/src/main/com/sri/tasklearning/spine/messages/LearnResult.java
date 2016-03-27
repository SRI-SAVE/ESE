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

// $Id: LearnResult.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.pal.common.ErrorInfo;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class LearnResult
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;
    private String procedureSource = null;
    private ErrorInfo error = null;

    public LearnResult(String sender,
                       TransactionUID uid,
                       String procedureSource) {
        super(sender, uid, UserMessageType.LEARN_RESULT);
        this.procedureSource = procedureSource;
    }

    public LearnResult(String sender,
                       TransactionUID uid,
                       ErrorInfo error) {
        super(sender, uid, UserMessageType.LEARN_RESULT);
        this.error = error;
    }

    public TransactionUID getUid() {
        return (TransactionUID)uid;
    }

    public String getProcedureSource() {
        return procedureSource;
    }

    public ErrorInfo getError() {
        return error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        LearnResult that = (LearnResult) o;

        if (procedureSource != null ? !procedureSource.equals(that.procedureSource) : that.procedureSource != null)
            return false;
        if (error != null ? !error.equals(that.error) : that.error != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (procedureSource != null ? procedureSource.hashCode() : 0);
        result = 31 * result + (error != null ? error.hashCode() : 0);
        return result;
    }
}
