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

public class RequestStopWatching
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    public RequestStopWatching(String sender,
                          TransactionUID uid) {
        super(sender, uid, UserMessageType.REQUEST_STOP_WATCHING);
    }

    public TransactionUID getUid() {
        return (TransactionUID)uid;
    }

}