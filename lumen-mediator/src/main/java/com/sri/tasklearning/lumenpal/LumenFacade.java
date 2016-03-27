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

// $Id: LumenFacade.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal;

import java.util.Map;

import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.type.Type.Registry;
import com.sri.ai.lumen.mediator.MediatorException;
import com.sri.ai.lumen.mediator.ServerConnection;
import com.sri.ai.lumen.runtime.LumenConnection;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * This interface abstracts out the operations which we ask Lumen to perform. It
 * allows us to easily use a mock lumen in place of the actual Lumen.
 *
 * @author chris
 */
public interface LumenFacade {
    /**
     * Calls {@link LumenConnection#removeTaskDefinition}.
     *
     * @param typeName
     * @throws MediatorException
     */
    void removeTaskDefinition(SimpleTypeName typeName)
            throws MediatorException;

    /**
     * Calls {@link LumenConnection#addTaskDefinition}.
     *
     * @param source
     * @throws MediatorException
     */
    void addTaskDefinition(String source)
            throws MediatorException;

    /**
     * Calls {@link LumenConnection#addIfPrimitiveAction}.
     *
     * @param lumenAction
     */
    void addIfPrimitiveAction(ATRActionDeclaration lumenAction);

    /**
     * Calls {@link LumenConnection#setServerConnection}.
     *
     * @param sci
     */
    void setServerConnection(ServerConnection sci);

    /**
     * Calls {@link LumenConnection#initialize}.
     */
    void initialize();

    Registry getTypeRegistry();

    /**
     * Calls {@link LumenConnection#cancelProcedure}.
     *
     * @param uid
     *            the UID of the procedure to cancel
     */
    void cancel(TransactionUID uid);

    /**
     * Calls {@link LumenConnection#evaluateATRTerm}.
     */
    Object evaluateATRTerm(ATRTerm term,
                           Map<String, Object> bindings);

    void shutdown();
}
