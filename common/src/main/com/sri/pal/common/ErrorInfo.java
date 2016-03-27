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

// $Id: ErrorInfo.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ErrorInfo
        implements Serializable {
    private static final long serialVersionUID = 1L;

    // What component originated the error?
    private final String source;

    // ID is unique only for a given source.
    private final int errorId;

    // Possibly parameterized message.
    private final String terseMessage;

    // Possibly parameterized message.
    private final String detailedMessage;

    // Stack trace of PAL procedures, possibly including a CPOF action at the
    // bottom and/or a rule at the top.
    private final List<PALStackFrame> stackInfo;

    public ErrorInfo(String source,
                     int errorId,
                     String terseMsg,
                     String detailMsg,
                     List<PALStackFrame> stackInfo) {
        this.source = source;
        this.errorId = errorId;
        this.terseMessage = terseMsg;
        this.detailedMessage = detailMsg;
        this.stackInfo = new ArrayList<PALStackFrame>();
        if (stackInfo != null) {
            this.stackInfo.addAll(stackInfo);
        }
    }

    public boolean isCancel() {
        if (errorId == 0) {
            return true;
        } else {
            return false;
        }
    }

    public String getSource() {
        return source;
    }

    public int getErrorId() {
        return errorId;
    }

    public String getTerseMessage() {
        return terseMessage;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public List<PALStackFrame> getStackInfo() {
        return stackInfo;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + source + "): "
                + detailedMessage;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((detailedMessage == null) ? 0 : detailedMessage.hashCode());
        result = prime * result + errorId;
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result
                + ((stackInfo == null) ? 0 : stackInfo.hashCode());
        result = prime * result
                + ((terseMessage == null) ? 0 : terseMessage.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ErrorInfo other = (ErrorInfo) obj;
        if (detailedMessage == null) {
            if (other.detailedMessage != null)
                return false;
        } else if (!detailedMessage.equals(other.detailedMessage))
            return false;
        if (errorId != other.errorId)
            return false;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        if (stackInfo == null) {
            if (other.stackInfo != null)
                return false;
        } else if (!stackInfo.equals(other.stackInfo))
            return false;
        if (terseMessage == null) {
            if (other.terseMessage != null)
                return false;
        } else if (!terseMessage.equals(other.terseMessage))
            return false;
        return true;
    }

    public static class PALStackFrame
            implements Serializable {
        private static final long serialVersionUID = 1L;

        // TypeName uniquely identifies an action or procedure definition.
        private final SimpleTypeName actionName;

        // Location within this procedure.
        private final int location;

        public PALStackFrame(SimpleTypeName actionName,
                             int location) {
            this.actionName = actionName;
            this.location = location;
        }

        public SimpleTypeName getActionName() {
            return actionName;
        }

        public int getLocation() {
            return location;
        }

        @Override
        public String toString() {
            return actionName.getFullName() + ":" + location;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((actionName == null) ? 0 : actionName.hashCode());
            result = prime * result + location;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PALStackFrame other = (PALStackFrame) obj;
            if (actionName == null) {
                if (other.actionName != null)
                    return false;
            } else if (!actionName.equals(other.actionName))
                return false;
            if (location != other.location)
                return false;
            return true;
        }
    }
}
