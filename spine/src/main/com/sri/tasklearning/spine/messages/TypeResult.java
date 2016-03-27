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

// $Id: TypeResult.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class TypeResult
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final SimpleTypeName typeName;
    private final String typeStr;

    /**
     * @param sender
     *            who's sending the answer to the question
     * @param typeName
     *            the requested type name
     * @param type
     *            the string representation of the ATR object(s)
     * @param uid
     *            copied UID from the question to the answer
     */
    public TypeResult(String sender,
                      SimpleTypeName typeName,
                      String type,
                      TransactionUID uid) {
        super(sender, uid, UserMessageType.TYPE_RESULT);
        this.typeName = typeName;
        typeStr = type;
    }

    public SimpleTypeName getTypeName() {
        return typeName;
    }

    public String getTypeStr() {
        return typeStr;
    }

    public ATRDecl getTypeAtr()
            throws RuntimeException {
        if (typeStr == null) {
            return null;
        }
        CTRConstructor ctrCon = new CTRConstructor();
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ATRSyntax atrUtil = new ATRSyntax(ctrCon);
        Object result;
        try {
            result = atrUtil.declFromSource(typeStr);
        } catch (LumenSyntaxError e) {
            throw new RuntimeException("Unable to parse ATR string: " + typeStr, e);
        }
        return (ATRDecl) result;
    }

    public TransactionUID getUid() {
        return (TransactionUID)uid;
    }

    @Override
    public String toString() {
        String not;
        if (typeStr == null) {
            not = " not";
        } else {
            not = " ";
        }
        return super.toString() + " " + typeName.getFullName() + not
                + " found";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((typeName == null) ? 0 : typeName.hashCode());
        result = prime * result + ((typeStr == null) ? 0 : typeStr.hashCode());
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
        TypeResult other = (TypeResult) obj;
        if (typeName == null) {
            if (other.typeName != null) {
                return false;
            }
        } else if (!typeName.equals(other.typeName)) {
            return false;
        }
        if (typeStr == null) {
            if (other.typeStr != null) {
                return false;
            }
        } else if (!typeStr.equals(other.typeStr)) {
            return false;
        }
        return true;
    }
}
