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

// $Id: BreakpointNotify.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class BreakpointNotify
        extends ExecutionStatus {
    private static final long serialVersionUID = 1L;

    private final SimpleTypeName actionName;
    private final int preorderIndex;
    private final SimpleTypeName subActionName;

    public BreakpointNotify(String sender,
                            SimpleTypeName actionName,
                            int preorderIndex,
                            SimpleTypeName subActionName,
                            TransactionUID uid) {
        super(sender, uid, null);
        this.actionName = actionName;
        this.preorderIndex = preorderIndex;
        this.subActionName = subActionName;
    }

    public SimpleTypeName getActionName() {
        return actionName;
    }

    public int getPreorderIndex() {
        return preorderIndex;
    }

    public SimpleTypeName getSubActionName() {
        return subActionName;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID)uid;
    }

    @Override
    public String toString() {
        return super.toString() + " for " + actionName + "@" + preorderIndex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((actionName == null) ? 0 : actionName.hashCode());
        result = prime * result + preorderIndex;
        result = prime * result
                + ((subActionName == null) ? 0 : subActionName.hashCode());
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
        BreakpointNotify other = (BreakpointNotify) obj;
        if (actionName == null) {
            if (other.actionName != null)
                return false;
        } else if (!actionName.equals(other.actionName))
            return false;
        if (preorderIndex != other.preorderIndex)
            return false;
        if (subActionName == null) {
            if (other.subActionName != null)
                return false;
        } else if (!subActionName.equals(other.subActionName))
            return false;
        return true;
    }
}
