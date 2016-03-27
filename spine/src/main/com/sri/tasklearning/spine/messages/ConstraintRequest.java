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

// $Id: ConstraintRequest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class ConstraintRequest
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final String procSrc;

    public ConstraintRequest(String sender,
                             String procSrc,
                             TransactionUID uid) {
        super(sender, uid, UserMessageType.CONSTRAINT_REQUEST);
        this.procSrc = procSrc;
    }

    public String getProcSrc() {
        return procSrc;
    }

    public TransactionUID getUid() {
        return (TransactionUID) uid;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((procSrc == null) ? 0 : procSrc.hashCode());
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
        ConstraintRequest other = (ConstraintRequest) obj;
        if (procSrc == null) {
            if (other.procSrc != null)
                return false;
        } else if (!procSrc.equals(other.procSrc))
            return false;
        return true;
    }

}
