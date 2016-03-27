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

import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public abstract class CustomMessage extends BroadcastMessage {
    private static final long serialVersionUID = 1L;
    private Object payload;

    public CustomMessage(String sender, TransactionUID uid, Object payload, UserMessageType messageType) {
        super(sender, uid, messageType);
        this.payload = payload;
    }

    public TransactionUID getUid() {
        return (TransactionUID) uid;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        CustomMessage that = (CustomMessage) o;

        if (payload != null ? !payload.equals(that.payload) : that.payload != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (payload != null ? payload.hashCode() : 0);
        return result;
    }
}