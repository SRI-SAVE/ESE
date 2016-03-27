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

import java.util.HashSet;
import java.util.Map;

import com.sri.tasklearning.spine.messages.contents.UID;

public class ExistingSubscriptions extends BroadcastMessage {
    private static final long serialVersionUID = 1L;
    private Map<MessageType, HashSet<String>> subscriptionTypes;
    private String requestingClient;

    public ExistingSubscriptions(String sender, UID uid, Map<MessageType, HashSet<String>> messageTypes, String requestingClient) {
        super(sender, uid, SystemMessageType.EXISTING_SUBSCRIPTIONS);
        this.subscriptionTypes = messageTypes;
        this.requestingClient = requestingClient;
    }

    public Map<MessageType, HashSet<String>> getSubscriptionTypes() {
        return subscriptionTypes;
    }

    public String getRequestingClient() {
        return requestingClient;
    }
}
