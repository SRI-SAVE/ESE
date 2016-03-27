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
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;
import com.sri.tasklearning.spine.messages.CancelRequest;
import com.sri.tasklearning.spine.messages.CustomBridgeMessage;
import com.sri.tasklearning.spine.messages.CustomEditorMessage;
import com.sri.tasklearning.spine.messages.CustomLapdogMessage;
import com.sri.tasklearning.spine.messages.CustomLumenMessage;
import com.sri.tasklearning.spine.messages.CustomShellMessage;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.ExecutionStatus;
import com.sri.tasklearning.spine.messages.LearnRequest;
import com.sri.tasklearning.spine.messages.LearnResult;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.StartWatching;
import com.sri.tasklearning.spine.messages.StopWatching;
import com.sri.tasklearning.spine.messages.TypeListQuery;
import com.sri.tasklearning.spine.messages.TypeListResult;
import com.sri.tasklearning.spine.messages.TypeQuery;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.TypeStoreRequest;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MockClientMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(MockClientMessageHandler.class);
    // The origin may not be the sender of this message! The origin is
    // the client that originated the request . All further messages on
    // this request such as status updates will use that same TransactionUID.originator
    private HashMap<String, List<TransactionUID>> senderToUidHistory = new HashMap<String, List<TransactionUID>>();
    private JmsSpine jmsSpine;
    private boolean varyResponseTimes;
    private Random random;

    public MockClientMessageHandler(JmsSpine jmsSpine, boolean varyResponseTimes) {
        this.senderToUidHistory = new HashMap<String, List<TransactionUID>>();
        this.jmsSpine = jmsSpine;
        // This element allows us to introduce unpredictable response times from each of the spines
        // which mimics more closely real system behavior.
        this.varyResponseTimes = varyResponseTimes;
        random = new Random(System.currentTimeMillis());
    }

    synchronized public void handleMessage(Message message) throws MessageHandlerException {
        if (varyResponseTimes) {
            long delay = random.nextLong() % 100;
            try {
                if (delay > 0)
                    Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String sender;
        TransactionUID newUid;
        if (LearnRequest.class.isInstance(message)) {
            LearnRequest learnMessage = (LearnRequest) message;
            sender = learnMessage.getSender();
            newUid = learnMessage.getUid();
        }
        else if (LearnResult.class.isInstance(message)) {
            LearnResult learnResult = (LearnResult) message;
            sender = learnResult.getSender();
            newUid = learnResult.getUid();
        }
        else if (ExecuteRequest.class.isInstance(message)) {
            ExecuteRequest executeRequest = (ExecuteRequest) message;
            sender = executeRequest.getSender();
            newUid = executeRequest.getUid();
        }
        else if (ExecutionStatus.class.isInstance(message)) {
            ExecutionStatus executionStatus = (ExecutionStatus) message;
            sender = executionStatus.getSender();
            newUid = executionStatus.getUid();
        }
        else if (CancelRequest.class.isInstance(message)) {
            CancelRequest cancelRequest = (CancelRequest) message;
            sender = cancelRequest.getSender();
            newUid = cancelRequest.getUid();
        }
        else if (TypeQuery.class.isInstance(message)) {
            TypeQuery typeQuery = (TypeQuery) message;
            sender = typeQuery.getSender();
            newUid = typeQuery.getUid();
            if (sender.equals(jmsSpine.getClientId())) {
                log.debug("Ignoring messages from self");
            }
            else {
                SimpleTypeName name = (SimpleTypeName) TypeNameFactory
                        .makeName("Stepping");
                ATRTypeDeclaration spineType = ATRTestUtil.makeCustomType(name,
                        String.class);
                try {
                    jmsSpine.send(new TypeResult(jmsSpine.getClientId(), name,
                            ATRSyntax.toSource(spineType), newUid));
                } catch (SpineException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (TypeResult.class.isInstance(message)) {
            TypeResult typeResult = (TypeResult) message;
            sender = typeResult.getSender();
            newUid = typeResult.getUid();
        }
        else if (StartWatching.class.isInstance(message)) {
            StartWatching startWatching = (StartWatching) message;
            sender = startWatching.getSender();
            newUid = startWatching.getUid();
        }
        else if (StopWatching.class.isInstance(message)) {
            StopWatching stopWatching = (StopWatching) message;
            sender = stopWatching.getSender();
            newUid = stopWatching.getUid();
        }
        else if (SerialNumberResponse.class.isInstance(message)) {
            SerialNumberResponse serialNumberResponse = (SerialNumberResponse) message;
            sender = serialNumberResponse.getSender();
            newUid = serialNumberResponse.getUid();
        }
        else if (TypeStoreRequest.class.isInstance(message)) {
            TypeStoreRequest typeStoreRequest = (TypeStoreRequest) message;
            sender = typeStoreRequest.getSender();
            newUid = typeStoreRequest.getUid();
        }
        else if (TypeListQuery.class.isInstance(message)) {
            TypeListQuery typeListQuery = (TypeListQuery) message;
            sender = typeListQuery.getSender();
            newUid = typeListQuery.getUid();
            if (sender.equals(jmsSpine.getClientId())) {
                log.debug("Ignoring messages from self");
            }
            else {
                Set<SimpleTypeName> typeNameSet = new HashSet<SimpleTypeName>();
                try {
                    jmsSpine.send(new TypeListResult(jmsSpine.getClientId(), typeNameSet, newUid));
                } catch (SpineException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (CustomBridgeMessage.class.isInstance(message)) {
            CustomBridgeMessage customBridgeMessage = (CustomBridgeMessage) message;
            sender = customBridgeMessage.getSender();
            newUid = customBridgeMessage.getUid();
        }
        else if (CustomLumenMessage.class.isInstance(message)) {
            CustomLumenMessage customLumenMessage = (CustomLumenMessage) message;
            sender = customLumenMessage.getSender();
            newUid = customLumenMessage.getUid();
        }
        else if (CustomLapdogMessage.class.isInstance(message)) {
            CustomLapdogMessage customLapdogMessage = (CustomLapdogMessage) message;
            sender = customLapdogMessage.getSender();
            newUid = customLapdogMessage.getUid();
        }
        else if (CustomShellMessage.class.isInstance(message)) {
            CustomShellMessage customShellMessage = (CustomShellMessage) message;
            sender = customShellMessage.getSender();
            newUid = customShellMessage.getUid();
        }
        else if (CustomEditorMessage.class.isInstance(message)) {
            CustomEditorMessage customEditorMessage = (CustomEditorMessage) message;
            sender = customEditorMessage.getSender();
            newUid = customEditorMessage.getUid();
        }
        else {
            log.error("Received Message that this client is unable to handle");
            throw new MessageHandlerException("Received Message that this client is unable to handle");
        }

        List<TransactionUID> uidList;
        if (senderToUidHistory.containsKey(sender)) {
            uidList = senderToUidHistory.get(sender);
        }
        else {
            uidList = new ArrayList<TransactionUID>();
        }
        uidList.add(newUid);
        senderToUidHistory.put(sender, uidList);
    }

    public List<TransactionUID> getMessageUidListFromSender(String sender) {
        return senderToUidHistory.get(sender);
    }
}
