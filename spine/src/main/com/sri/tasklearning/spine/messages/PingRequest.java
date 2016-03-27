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

// $Id: PingRequest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Requests that another module confirm its operational status by sending a
 * corresponding {@link PingReply}.
 *
 * @author chris
 */
public class PingRequest
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final String targetModule;

    public PingRequest(String sender,
                       String targetModule,
                       TransactionUID uid) {
        super(sender, uid, UserMessageType.PING_REQUEST);
        this.targetModule = targetModule;
    }

    /**
     * Returns the name of the module whose status is being queried. Only the
     * named module should respond. This name should match the client ID given
     * to the JmsSpine constructor.
     *
     * @return the name of the module whose status is being queried
     */
    public String getTargetModule() {
        return targetModule;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID) super.getUid();
    }

    @Override
    public String toString() {
        return super.toString() + " for " + targetModule;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((targetModule == null) ? 0 : targetModule.hashCode());
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
        PingRequest other = (PingRequest) obj;
        if (targetModule == null) {
            if (other.targetModule != null)
                return false;
        } else if (!targetModule.equals(other.targetModule))
            return false;
        return true;
    }
}
