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

// $Id: TypeStoreResult.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Response to a {@link TypeStoreRequest}. Indicates whether the request was
 * fulfilled, or whether an error was encountered.
 *
 * @author chris
 */
public class TypeStoreResult
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final Throwable error;

    public TypeStoreResult(String sender,
                           Throwable error,
                           TransactionUID uid) {
        super(sender, uid, UserMessageType.TYPE_STORE_RESULT);
        this.error = error;
    }

    /**
     * Provides the error encountered during type storage, if any.
     *
     * @return an error, or {@code null}
     */
    public Throwable getError() {
        return error;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID) super.getUid();
    }
}
