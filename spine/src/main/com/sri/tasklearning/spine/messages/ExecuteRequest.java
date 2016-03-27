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

// $Id: ExecuteRequest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import java.util.List;

import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class ExecuteRequest
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;
    private final TransactionUID parentUid;
    private final SimpleTypeName actionName;
    private final List<Object> inParams;
    private final boolean stepped;

    public ExecuteRequest(String sender,
                          TransactionUID uid,
                          TransactionUID parentUid,
                          SimpleTypeName actionName,
                          List<Object> inParams,
                          boolean stepped) {
        super(sender, uid, UserMessageType.EXECUTE_REQUEST);
        this.parentUid = parentUid;
        this.actionName = actionName;
        this.inParams = inParams;
        this.stepped = stepped;
    }

    public TransactionUID getUid() {
        return (TransactionUID)uid;
    }

    public TransactionUID getParentUid() {
        return parentUid;
    }

    public SimpleTypeName getActionName() {
        return actionName;
    }

    public List<Object> getInParams() {
        return inParams;
    }

    public boolean isStepped() {
        return stepped;
    }

    @Override
    public String toString() {
        String steppedStr = " normal";
        if (isStepped()) {
            steppedStr = " stepped";
        }
        return super.toString() + " (parentUid " + parentUid + ") for "
                + actionName + steppedStr;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((actionName == null) ? 0 : actionName.hashCode());
        result = prime * result
                + ((inParams == null) ? 0 : inParams.hashCode());
        result = prime * result
                + ((parentUid == null) ? 0 : parentUid.hashCode());
        result = prime * result + (stepped ? 1231 : 1237);
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
        ExecuteRequest other = (ExecuteRequest) obj;
        if (actionName == null) {
            if (other.actionName != null)
                return false;
        } else if (!actionName.equals(other.actionName))
            return false;
        if (inParams == null) {
            if (other.inParams != null)
                return false;
        } else if (!inParams.equals(other.inParams))
            return false;
        if (parentUid == null) {
            if (other.parentUid != null)
                return false;
        } else if (!parentUid.equals(other.parentUid))
            return false;
        if (stepped != other.stepped)
            return false;
        return true;
    }
}
