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

// $Id: CacheExpire.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.TypeListQuery.Subset;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Message sent to indicate that a given type has been modified or removed, and
 * thus should be removed from any caches.
 */
public class CacheExpire
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final SimpleTypeName typeName;
    private Subset type;

    /**
     * Creates a cache expiry message.
     *
     * @param sender
     *            the client id of the sender
     * @param uid
     *            a new transaction uid
     * @param typeName
     *            the identifier of the type which should be flushed from caches
     */
    public CacheExpire(String sender,
                       TransactionUID uid,
                       SimpleTypeName typeName,
                       TypeListQuery.Subset type) {
        super(sender, uid, UserMessageType.CACHE_EXPIRE);
        this.typeName = typeName;
        this.type = type;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID) uid;
    }

    /**
     * Provides the identifier of the type which should be flushed from caches.
     */
    public SimpleTypeName getTypeName() {
        return typeName;
    }

    /**
     * Provides the type of the thing being modified, or {@code null} if it's
     * being removed.
     *
     * @return a type
     */
    public Subset getType() {
            return type;
    }

    @Override
    public String toString() {
        return super.toString() + " for " + type + " " + typeName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result
                + ((typeName == null) ? 0 : typeName.hashCode());
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
        CacheExpire other = (CacheExpire) obj;
        if (type != other.type)
            return false;
        if (typeName == null) {
            if (other.typeName != null)
                return false;
        } else if (!typeName.equals(other.typeName))
            return false;
        return true;
    }
}
