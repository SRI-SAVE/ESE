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

// $Id: ProcessDemoMessage.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.pal.common.ErrorInfo;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * A request to process a demonstration. This means send it to LAPDOG for
 * preprocessing and idiom recognition.
 */
public class ProcessDemoMessage
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final ATRDemonstration demonstration;
    private final ErrorInfo error;

    public ProcessDemoMessage(String sender,
                              ATRDemonstration demonstration,
                              TransactionUID uid) {
        super(sender, uid, UserMessageType.PROCESS_DEMO);
        this.demonstration = demonstration;
        error = null;
    }

    public ProcessDemoMessage(String sender,
                              ErrorInfo error,
                              TransactionUID uid) {
        super(sender, uid, UserMessageType.PROCESS_DEMO);
        this.error = error;
        demonstration = null;
    }

    public ATRDemonstration getDemonstration() {
        return demonstration;
    }

    public ErrorInfo getError() {
        return error;
    }

    public TransactionUID getUid() {
        return (TransactionUID) uid;
    }

    @Override
    public String toString() {
        return super.toString() + " actions: " + demonstration;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((demonstration == null) ? 0 : demonstration.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProcessDemoMessage other = (ProcessDemoMessage) obj;
        if (demonstration == null) {
            if (other.demonstration != null)
                return false;
        } else if (!demonstration.equals(other.demonstration))
            return false;
        return true;
    }
}
