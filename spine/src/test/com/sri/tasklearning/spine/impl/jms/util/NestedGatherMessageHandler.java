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

package com.sri.tasklearning.spine.impl.jms.util;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.TypeQuery;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class NestedGatherMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(NestedGatherMessageHandler.class);
    public JmsSpine jmsSpine;
    public Executor threadPool;

    public NestedGatherMessageHandler(JmsSpine jmsSpine) {
        this.jmsSpine = jmsSpine;
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newCachedThreadPool(tf);
    }

    synchronized public void handleMessage(Message message) throws MessageHandlerException {

        if (TypeQuery.class.isInstance(message)) {
            TypeQuery typeQuery = (TypeQuery) message;
            String sender = typeQuery.getSender();
            if (sender.equals(jmsSpine.getClientId())) {
                log.debug("TypeQuery: Ignoring messages from self (" + jmsSpine.getClientId() + ")");
                return;
            }
            threadPool.execute(new NestedTypeQueryGatherHandler(typeQuery));
        }

        else {
            log.error("Received Message that this client is not configured to handle");
            throw new MessageHandlerException("Received Message that this client is unable to handle");
        }

    }

    private class NestedTypeQueryGatherHandler implements Runnable {
        private TypeQuery typeQuery;

        public NestedTypeQueryGatherHandler(TypeQuery typeQuery) {
            this.typeQuery = typeQuery;
        }

        @Override
        synchronized public void run() {
            String sender = typeQuery.getSender();
            // Else This is the message sent by Lumen for the un-nested gather TypeRequest.
            // This must be the Bridge Instance.
            if (sender.equals("Lumen")) {
                // Now that we have received the un-nested gather request we will issue a nested one
                // before responding.
                TypeQuery nestedTypeQuery = new TypeQuery(
                        jmsSpine.getClientId(),
                        (SimpleTypeName) TypeNameFactory
                                .makeName("nestedAction"),
                        jmsSpine.getNextUid());
                try {
                    jmsSpine.gather(nestedTypeQuery, Spine.DEFAULT_TIMEOUT);
                    // Pretend to do something with the returned data
                } catch (SpineException e) {
                    e.printStackTrace();
                }
                // can now respond to the un-nested type request
                SimpleTypeName name = (SimpleTypeName) TypeNameFactory
                        .makeName("ActionDef");
                ATRTypeDeclaration spineType = ATRTestUtil.makeCustomType(name,
                        String.class);
                try {                                                                    // Use uid of query
                    jmsSpine.send(new TypeResult(jmsSpine.getClientId(), name,
                            ATRSyntax.toSource(spineType), typeQuery.getUid()));
                } catch (SpineException e) {
                    e.printStackTrace();
                }
            }
            // Else This is the message sent by Bridge for the nested gather TypeRequest. It is in response to the
            // un-nested gather TypeRequest
            // This must be the Lumen Instance.
            else if (sender.equals("Bridge")) {
                // Now that we have recieved the nested gather request we will issue a response.
                SimpleTypeName name = (SimpleTypeName) TypeNameFactory
                        .makeName("nestedActionDef");
                ATRTypeDeclaration spineType = ATRTestUtil.makeCustomType(name,
                        String.class);
                TypeResult typeResult = new TypeResult(jmsSpine.getClientId(),
                        name, ATRSyntax.toSource(spineType), typeQuery.getUid());
                try {
                    jmsSpine.send(typeResult);
                } catch (SpineException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}