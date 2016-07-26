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

package com.sri.tasklearning.lumenpal.mock;

import java.util.ArrayList;
import java.util.Map;

import com.sri.ai.lumen.agent.SimpleAgent;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.type.Type;
import com.sri.ai.lumen.atr.type.Type.Registry;
import com.sri.ai.lumen.atr.type.impl.RegistryImpl;
import com.sri.ai.lumen.mediator.MediatorException;
import com.sri.ai.lumen.mediator.ServerConnection;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.lumenpal.LumenFacade;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class MockLumen implements LumenFacade {
    private ArrayList<TransactionUID> lastCallUidList;
    private final Type.Registry registry;

    public MockLumen() {
        lastCallUidList = new ArrayList<TransactionUID>();
        registry = new RegistryImpl();
    }

    @Override
    public void cancel(TransactionUID uid) {
        lastCallUidList.add(uid);
    }

    @Override
    public void removeTaskDefinition(SimpleTypeName typeName) throws MediatorException {

    }

    @Override
    public void addTaskDefinition(String source) throws MediatorException {

    }

    @Override
    public void addIfPrimitiveAction(ATRActionDeclaration lumenAction) {

    }

    @Override
    public void setServerConnection(ServerConnection sci) {

    }

    @Override
    public void initialize() {

    }

    /**
     * This method returns the UID of the most recently handled message. Its
     * is used by the tests to check when messages are actually passing through
     * to Lumen.
     * @return
     */
    public TransactionUID getLastCallUid() {
        int size = lastCallUidList.size();
        if (size == 0) {
            return null;
        }
        return lastCallUidList.get(size - 1);
    }

    @Override
    public Registry getTypeRegistry() {
        return registry;
    }

    @Override
    public Object evaluateATRTerm(ATRTerm term,
                                  Map<String, Object> bindings) {
        return null;
    }

    @Override
    public void executeAction(String name, Object[] args) {
        // Do nothing.
    }

    @Override
    public SimpleAgent getAgent() {
        return null;
    }

    @Override
    public void shutdown() {
        // Do nothing.
    }
}
