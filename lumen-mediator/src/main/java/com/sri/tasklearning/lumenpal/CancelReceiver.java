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

// $Id: CancelReceiver.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal;

import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.messages.CancelRequest;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class receives {@link CancelRequest} messages from other Spine clients
 * and relays those requests on to Lumen.
 *
 * @author chris
 */
public class CancelReceiver
        implements MessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final LumenFacade lumen;

    public CancelReceiver(LumenFacade lumen) {
        this.lumen = lumen;
    }

    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (!(message instanceof CancelRequest)) {
            log.warn("Unexpected message ({}): {}", message.getClass(), message);
            return;
        }
        CancelRequest cancelMsg = (CancelRequest) message;

        TransactionUID uid = cancelMsg.getUid();
        log.debug("Received cancel for {}", uid);

        lumen.cancel(uid);
    }
}
