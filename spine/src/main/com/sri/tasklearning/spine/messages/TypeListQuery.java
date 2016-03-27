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

// $Id: TypeListQuery.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import java.util.Arrays;

import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Requests a listing of some or all known types. This should result in a
 * {@link TypeListResult}.
 *
 * @author chris
 */
public class TypeListQuery
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final Subset[] subset;

    public TypeListQuery(String sender,
                         TransactionUID uid,
                         Subset... subset) {
        super(sender, uid, UserMessageType.TYPE_LIST_QUERY);
        this.subset = subset;
    }

    /**
     * Indicates what subset of known types should be listed. For instance, the
     * requester may only be interested in procedures.
     *
     * @return the requested subset of types
     */
    public Subset[] getSubset() {
        return subset;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID) super.getUid();
    }

    @Override
    public String toString() {
        return super.toString() + " for " + Arrays.asList(subset);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((subset == null) ? 0 : subset.hashCode());
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
        TypeListQuery other = (TypeListQuery) obj;
        if (subset != other.subset)
            return false;
        return true;
    }

    // WARNING: The names of these need to be the same as TypeStorage.Subet.
    public enum Subset {
        TYPE, ACTION, PROCEDURE, CONSTRAINT, FAMILY, IDIOM
    }
}
