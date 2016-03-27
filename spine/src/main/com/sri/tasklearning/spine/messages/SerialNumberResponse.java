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

public class SerialNumberResponse
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;
    private long serialNumber;

    public SerialNumberResponse(String sender,
                                TransactionUID uid,
                                long serialNumber) {
        super(sender, uid, SystemMessageType.SERIAL_NUMBER_RESPONSE);
        this.serialNumber = serialNumber;
    }

    public TransactionUID getUid() {
        return (TransactionUID) uid;
    }

    public long getSerialNumber() {
        return serialNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        SerialNumberResponse that = (SerialNumberResponse) o;

        if (serialNumber != that.serialNumber)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (serialNumber ^ (serialNumber >>> 32));
        return result;
    }
}