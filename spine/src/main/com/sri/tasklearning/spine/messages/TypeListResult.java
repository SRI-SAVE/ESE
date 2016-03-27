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

// $Id: TypeListResult.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import java.util.Set;

import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Response from a {@link TypeListQuery}.
 *
 * @author chris
 */
public class TypeListResult
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final Set<SimpleTypeName> names;
    private final ErrorInfo error;

    public TypeListResult(String sender,
                          Set<SimpleTypeName> typeNames,
                          TransactionUID uid) {
        super(sender, uid, UserMessageType.TYPE_LIST_RESULT);
        names = typeNames;
        error = null;
    }

    public TypeListResult(String sender,
                          ErrorInfo error,
                          TransactionUID uid) {
        super(sender, uid, UserMessageType.TYPE_LIST_RESULT);
        names = null;
        this.error = error;
    }

    /**
     * Returns the set of type names corresponding to the request. This set may
     * be empty, but not {@code null}.
     *
     * @return the requested type names
     */
    public Set<SimpleTypeName> getTypeNames() {
        return names;
    }

    /**
     * Returns the error, if one occurred.
     *
     * @return the error or {@code null}
     */
    public ErrorInfo getError() {
        return error;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID) super.getUid();
    }

    @Override
    public String toString() {
        return super.toString() + " with: " + names;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((names == null) ? 0 : names.hashCode());
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
        TypeListResult other = (TypeListResult) obj;
        if (names == null) {
            if (other.names != null)
                return false;
        } else if (!names.equals(other.names))
            return false;
        return true;
    }
}
