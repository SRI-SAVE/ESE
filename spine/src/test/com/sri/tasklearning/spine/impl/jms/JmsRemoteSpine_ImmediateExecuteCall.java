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

package com.sri.tasklearning.spine.impl.jms;

import java.util.ArrayList;
import java.util.List;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.util.SpineTestCase;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.messages.TypeQuery;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.util.LogUtil;

public class JmsRemoteSpine_ImmediateExecuteCall {
    private static String bridge = "Bridge";

    /**
     * This test is not intended to be run by itself.
     * Its main purpose is to support a multi Jms Spine instance test - see the
     * JmsSpine_Test::canShowSubscriptionMessagesAreAlwaysReceivedBeforeRequestMessages()
     * for further details. It is that test which invokes this class in the proper context.
     * @param args any arguments to this psvm
     * @throws com.sri.tasklearning.spine.SpineException if there is a spine problem
     * @throws InterruptedException if the sleep goes wrong
     */
    public static void main(String[] args) throws InterruptedException, SpineException {
        LogUtil.configureLogging(SpineTestCase.LOG_CONFIG_BASE, SpineTestCase.class);

        // First set up the request parameters
        List<Object> inParams = new ArrayList<Object>();
        final ATRTypeDeclaration spineType = ATRTestUtil.makeCustomType(
                (SimpleTypeName) TypeNameFactory
                        .makeName("definition of walking"), String.class);

        // Create the bridge spine
        final JmsSpine bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);

        // Create the handler that will immediate fire back a type result when a type request comes in
        MessageHandler bridgeHandler = new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (TypeQuery.class.isInstance(message)) {
                    // Immediately return the typeResult
                    TypeResult typeResult = new TypeResult(
                            bridge,
                            (SimpleTypeName) TypeNameFactory.makeName("a type"),
                            ATRSyntax.toSource(spineType), bridgeSpine
                                    .getNextUid());
                    try {
                        bridgeSpine.send(typeResult);
                    } catch (SpineException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        // Register for the type query that lumen will immediately send in response to the execute request
        bridgeSpine.subscribe(bridgeHandler, UserMessageType.TYPE_QUERY);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Immediately return the typeResult
        bridgeSpine.send(new ExecuteRequest(bridge, bridgeSpine.getNextUid(),
                null, (SimpleTypeName) TypeNameFactory.makeName("run"),
                inParams, false));

        // Wait to allow the results to come back before exiting
        Thread.sleep(50);

        System.exit(1);
    }

}
