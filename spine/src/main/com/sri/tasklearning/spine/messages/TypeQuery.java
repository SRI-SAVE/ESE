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

// $Id: TypeQuery.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/*
 * TODO For a huge performance improvement, we should allow TypeQuery to contain
 * Set<TypeName> instead of a single TypeName. TypeResult would be modified to
 * suit.
 */
public class TypeQuery
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final SimpleTypeName actionName;

    public TypeQuery(String sender,
                     SimpleTypeName typeName,
                     TransactionUID uid) {
        super(sender, uid, UserMessageType.TYPE_QUERY);
        this.actionName = typeName;
    }

    public SimpleTypeName getTypeName() {
        return actionName;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID)uid;
    }

    @Override
    public String toString() {
        return super.toString() + " for " + actionName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((actionName == null) ? 0 : actionName.hashCode());
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
        TypeQuery other = (TypeQuery) obj;
        if (actionName == null) {
            if (other.actionName != null) {
                return false;
            }
        } else if (!actionName.equals(other.actionName)) {
            return false;
        }
        return true;
    }
}
