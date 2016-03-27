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

// $Id: SuccessExecutionStatus.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import java.util.List;

import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class SuccessExecutionStatus
        extends ExecutionStatus {
    private static final long serialVersionUID = 1L;

    private final List<Object> inParams;
    private final List<Object> outParams;

    public SuccessExecutionStatus(String sender,
                                  TransactionUID uid,
                                  TransactionUID parentUid,
                                  List<Object> inParams,
                                  List<Object> outParams) {
        super(sender, uid, parentUid);
        this.inParams = inParams;
        this.outParams = outParams;
    }

    public List<Object> getInParams() {
        return inParams;
    }

    public List<Object> getOutParams() {
        return outParams;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + outParams + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((inParams == null) ? 0 : inParams.hashCode());
        result = prime * result
                + ((outParams == null) ? 0 : outParams.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SuccessExecutionStatus other = (SuccessExecutionStatus) obj;
        if (inParams == null) {
            if (other.inParams != null) {
                return false;
            }
        } else if (!inParams.equals(other.inParams)) {
            return false;
        }
        if (outParams == null) {
            if (other.outParams != null) {
                return false;
            }
        } else if (!outParams.equals(other.outParams)) {
            return false;
        }
        return true;
    }
}
