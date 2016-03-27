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

// $Id: ExecutorListResult.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class ExecutorListResult
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;
    private final boolean isExecutor;

    public ExecutorListResult(String sender,
                              TransactionUID uid,
                              boolean isExecutor) {
        super(sender, uid, UserMessageType.EXECUTOR_LIST_RESULT);
        this.isExecutor = isExecutor;
    }

    public TransactionUID getUid() {
        return (TransactionUID)uid;
    }

    public boolean isExecutor() {
        return isExecutor;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + isExecutor + ")";
    }
}
