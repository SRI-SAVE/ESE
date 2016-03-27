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

// $Id: TypeStoreRequest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Requests that a type be added, modified, or removed.
 *
 * @author chris
 */
public class TypeStoreRequest
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final SimpleTypeName name;
    private final String typeStr;

    public TypeStoreRequest(String sender,
                            SimpleTypeName name,
                            String typeXml,
                            TransactionUID uid) {
        super(sender, uid, PrivilegedMessageType.TYPE_STORE_REQUEST);
        this.name = name;
        typeStr = typeXml;
    }

    /**
     * Provides the ID of the type to be modified.
     *
     * @return the target type's ID
     */
    public SimpleTypeName getName() {
        return name;
    }

    /**
     * The new type to store under the given name. If a type already exists with
     * the given name, it should be replaced with the new one. If this value is
     * {@code null}, the indicated type should be removed.
     *
     * @return the new type, or {@code null}
     */
    public String getTypeStr() {
        return typeStr;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID) super.getUid();
    }

    @Override
    public String toString() {
        return super.toString() + " for " + name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((typeStr == null) ? 0 : typeStr.hashCode());
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
        TypeStoreRequest other = (TypeStoreRequest) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (typeStr == null) {
            if (other.typeStr != null)
                return false;
        } else if (!typeStr.equals(other.typeStr))
            return false;
        return true;
    }
}
