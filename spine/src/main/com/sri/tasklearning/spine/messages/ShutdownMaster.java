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

// $Id: Shutdown.java 3360 2010-09-14 23:50:17Z dhartnett $
package com.sri.tasklearning.spine.messages;

import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Request that the Master Spine shutdown
 *
 * @author daragh
 */
public class ShutdownMaster
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    public ShutdownMaster(String sender,
                          TransactionUID uid) {
        super(sender, uid, SystemMessageType.SHUTDOWN_MASTER);
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID) super.getUid();
    }

    @Override
    public String toString() {
        return super.toString() + " from " + sender;
    }

}
