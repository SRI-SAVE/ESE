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

// $Id: GestureEnd.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SerialNumberRequest;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class GestureEnd
        extends ActionStreamEvent {
    private final GestureStart start;
    /**
     * Creates a new gesture end event for use in a demonstration.
     *
     * @param start
     *            the corresponding gesture start event
     * @return a new gesture suitable for sending to either
     *         {@link Learner#learn} or {@link Learner#recognizeIdiom}
     * @throws PALException
     *             if a communication error occurs
     */
    public static GestureEnd newInstance(GestureStart start)
            throws PALException {
        long serialNum;
        Bridge bridge = start.getBridge();
        Spine spine = bridge.getSpine();
        try {
            TransactionUID uid = spine.getNextUid();
            Message msg = new SerialNumberRequest(spine.getClientId(),
                    uid);
            SerialNumberResponse result = bridge.getSerialGetter()
                    .sendAndGetReply(msg);
            serialNum = result.getSerialNumber();
        } catch (SpineException e) {
            throw new PALException("Unable to get serial number", e);
        }

        TransactionUID endUid = spine.getNextUid();

        return new GestureEnd(start, serialNum, endUid);
    }

    GestureEnd(GestureStart start,
               long serialNum,
               TransactionUID uid)
            throws PALException {
        super(new GestureEndDef(start.getBridge()), start.getBridge(), start.getCaller(),
                serialNum, uid);
        this.start = start;
    }

    @Override
    protected boolean isLocallyExecuted() {
        return GestureStart.isLocallyExecutedStatic(this);
    }

    @Override
    public TypeDef getParamType(int i)
            throws PALException {
        return start.getParamType(i);
    }
}
