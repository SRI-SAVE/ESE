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

// $Id: StartExecutionStatus.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import java.util.List;

import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class StartExecutionStatus
        extends ExecutionStatus {
    private static final long serialVersionUID = 1L;

    private final List<Object> inParams;
    private final SimpleTypeName actionName;
    private final long serialNumber;

    public StartExecutionStatus(String sender,
                                TransactionUID uid,
                                TransactionUID parentUid,
                                SimpleTypeName actionName,
                                long serialNumber,
                                List<Object> inParams) {
        super(sender, uid, parentUid);
        this.actionName = actionName;
        this.inParams = inParams;
        this.serialNumber = serialNumber;
    }

    public SimpleTypeName getActionName() {
        return actionName;
    }

    public List<Object> getInParams() {
        return inParams;
    }

    public long getSerialNumber() {
        return serialNumber;
    }
    
    @Override
    public String toString() {
        return super.toString() + " " + actionName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        StartExecutionStatus that = (StartExecutionStatus) o;

        if (actionName != null ? !actionName.equals(that.actionName) : that.actionName != null)
            return false;
        if (inParams != null ? !inParams.equals(that.inParams) : that.inParams != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (inParams != null ? inParams.hashCode() : 0);
        result = 31 * result + (actionName != null ? actionName.hashCode() : 0);
        return result;
    }
}
