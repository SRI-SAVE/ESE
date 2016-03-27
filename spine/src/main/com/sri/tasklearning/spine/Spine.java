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

// $Id: Spine.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine;

import com.sri.tasklearning.spine.impl.jms.GatherCallback;
import com.sri.tasklearning.spine.messages.*;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public interface Spine {

    public final long DEFAULT_TIMEOUT = 60 * 1000; // One Minute

    public TransactionUID getNextUid();

    public String getClientId();

    public boolean isRunning();

    public boolean isMasterRunning();

    public void subscribe(
            MessageHandler handler,
            UserMessageType... messageTypes) throws SpineException;

    public void subscribe(
            MessageHandler handler,
            SystemMessageType... messageTypes) throws SpineException;

    public boolean subscribe(
            MessageHandler handler,
            PrivilegedMessageType messageType) throws SpineException;

    public boolean unsubscribe(MessageType messageType) throws SpineException;

    public boolean send(Message message) throws SpineException;

    public Message[] gather(Message message, long timeout) throws SpineException;

    public void gatherAsynchronous(Message message, long timeout, GatherCallback callback) throws SpineException;

    public void shutdown(boolean loud) throws Exception;

    public void shutdownMaster() throws Exception;

}
