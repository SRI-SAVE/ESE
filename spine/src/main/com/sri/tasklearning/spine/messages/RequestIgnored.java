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

/**
 * This message is sent from a Spine Client that is registered on a particular Topic and
 * has received and rejected a request on that said topic. This is to aid the requesting
 * Spine Clients Spine Instance to determine when a request will not be handled by
 * anyone.
 */
public class RequestIgnored extends BroadcastMessage {
    private static final long serialVersionUID = 1L;
    private TransactionUID parentUid;

    public RequestIgnored(String sender, TransactionUID uid, TransactionUID parentUid) {
        super(sender, uid, UserMessageType.REQUEST_IGNORED);
        this.parentUid = parentUid;
    }

    public TransactionUID getUid() {
        return (TransactionUID)uid;
    }

    public TransactionUID getParentUid() {
        return parentUid;
    }

}
