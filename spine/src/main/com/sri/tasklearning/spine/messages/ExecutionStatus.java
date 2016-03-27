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

// $Id: ExecutionStatus.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public abstract class ExecutionStatus
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;
    private final TransactionUID parentUid;

    public ExecutionStatus(String sender,
                           TransactionUID uid,
                           TransactionUID parentUid) {
        super(sender, uid, UserMessageType.EXECUTION_STATUS);
        this.parentUid = parentUid;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID)uid;
    }

    public TransactionUID getParentUid() {
        return parentUid;
    }

    @Override
    public String toString() {
        return super.toString() + " (parentUid " + parentUid + ")";
    }

    @Override
     public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((parentUid == null) ? 0 : parentUid.hashCode());
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ExecutionStatus other = (ExecutionStatus) obj;
        if (parentUid == null) {
            if (other.parentUid != null) {
                return false;
            }
        } else if (!parentUid.equals(other.parentUid)) {
            return false;
        }
        if (uid == null) {
            if (other.uid != null) {
                return false;
            }
        } else if (!uid.equals(other.uid)) {
            return false;
        }
        return true;
    }
}
