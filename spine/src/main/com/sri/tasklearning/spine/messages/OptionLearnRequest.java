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
package com.sri.tasklearning.spine.messages;

import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class OptionLearnRequest
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final ATRDemonstration demonstration;

    public OptionLearnRequest(String sender,
                              TransactionUID uid,
                              ATRDemonstration demonstration) {
        super(sender, uid, UserMessageType.LEARN_OPTION_REQUEST);
        this.demonstration = demonstration;
    }

    public ATRDemonstration getDemonstration() {
        return demonstration;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID) uid;
    }

    @Override
    public String toString() {
        return super.toString() + " " + demonstration.getActions().size()
                + " actions";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((demonstration == null) ? 0 : demonstration.hashCode());
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
        OptionLearnRequest other = (OptionLearnRequest) obj;
        if (demonstration == null) {
            if (other.demonstration != null)
                return false;
        } else if (!demonstration.equals(other.demonstration))
            return false;
        return true;
    }
}
