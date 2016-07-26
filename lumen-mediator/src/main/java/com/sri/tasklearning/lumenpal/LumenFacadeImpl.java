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

// $Id: LumenFacadeImpl.java 7750 2016-07-26 16:53:01Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal;

import java.util.Map;

import com.sri.ai.lumen.agent.SimpleAgent;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.type.Type.Registry;
import com.sri.ai.lumen.atr.type.TypeUtil;
import com.sri.ai.lumen.mediator.MediatorException;
import com.sri.ai.lumen.mediator.ServerConnection;
import com.sri.ai.lumen.runtime.LumenConnection;
import com.sri.ai.lumen.runtime.SteppingSupport;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the &quot;real&quot; implementation of {@link LumenFacade}. This is
 * backed by an actual instance of Lumen which will service requests.
 *
 * @author chris
 */
public class LumenFacadeImpl
        implements LumenFacade {
    private static final Logger log = LoggerFactory
            .getLogger(LumenFacadeImpl.class);

    private final LumenConnection lumen;

    public LumenFacadeImpl(LumenConnection lumen) {
        this.lumen = lumen;
    }

    @Override
    public void removeTaskDefinition(SimpleTypeName typeName)
            throws MediatorException {
        lumen.removeTaskDefinition(typeName.getFullName());
    }

    @Override
    public void addTaskDefinition(String source)
            throws MediatorException {
        lumen.addTaskDefinition(source, null);
    }

    @Override
    public void addIfPrimitiveAction(ATRActionDeclaration lumenAction) {
        if (lumen.addIfPrimitiveAction(lumenAction) == null) {
            log.warn("Failed to add {}", ATRSyntax.toSource(lumenAction));
        }
    }

    @Override
    public void setServerConnection(ServerConnection sci) {
        lumen.setServerConnection(sci);
    }

    @Override
    public void initialize() {
        lumen.initialize();
        LumenConnection.setStepper(lumen.getAgent(), SteppingSupport.STEPPER);
    }

    @Override
    public Registry getTypeRegistry() {
        return TypeUtil.getDefaultTypeRegistry();
    }

    @Override
    public void cancel(TransactionUID uid) {
        lumen.cancelProcedure(uid.toString());
    }

    @Override
    public Object evaluateATRTerm(ATRTerm term,
                                  Map<String, Object> bindings) {
        return lumen.evaluateATRTerm(term, bindings);
    }

    @Override
    public void shutdown() {
        lumen.disconnectFromLumen();
    }

    @Override
    public void executeAction(String actName,
                              Object[] args) {
        lumen.executeAction(actName, args);
    }

    @Override
    public SimpleAgent getAgent() {
        return lumen.getAgent();
    }
}
