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

// $Id: MockSpine.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.impl.jms.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.GatherCallback;
import com.sri.tasklearning.spine.messages.CancelRequest;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.MessageType;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.messages.PrivilegedMessageType;
import com.sri.tasklearning.spine.messages.SerialNumberRequest;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.messages.TypeListQuery;
import com.sri.tasklearning.spine.messages.TypeListResult;
import com.sri.tasklearning.spine.messages.TypeQuery;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.messages.contents.UID;

public class MockSpine
        implements Spine {
    private int nextUid = 0;
    private final Map<String, ATRDecl> actions;
    private final Map<MessageType, List<MessageHandler>> messageHandlers;
    private final Map<MessageType, Message> lastMessages;
    private ArrayList<Message> sentMessagesList;
    private Message[] gatherResults;

    public MockSpine() throws SpineException {
        actions = new HashMap<String, ATRDecl>();
        messageHandlers = new HashMap<MessageType, List<MessageHandler>>();
        lastMessages = new HashMap<MessageType, Message>();
        sentMessagesList = new ArrayList<Message>();
        MessageHandler handler = new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message instanceof TypeResult) {

                }
            }
        };
        subscribe(handler, UserMessageType.TYPE_RESULT);
    }

    @Override
    public String getClientId() {
        return "testLumen";
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public boolean isMasterRunning() {
        return true;
    }

    @Override
    public TransactionUID getNextUid() {
        return new TransactionUID(getClientId(), nextUid++);
    }

    @Override
    public void shutdown(boolean loud) throws Exception {

    }

    @Override
    public void shutdownMaster() throws Exception {

    }

    @Override
    public boolean send(Message message)
            throws SpineException {
        System.out.println("Sending " + message);
        sentMessagesList.add(message);

        if(message instanceof TypeQuery) {
            handleTypeQuery((TypeQuery) message);
        } else if (message instanceof TypeListQuery) {
            handleTypeListQuery((TypeListQuery) message);
        } else if(message instanceof SerialNumberRequest) {
            handleSerialNumberRequest((SerialNumberRequest) message);
        }

        return true;
    }

    @Override
    public Message[] gather(Message message, long timeout) throws SpineException {
        gatherResults = new Message[0];
        if(message instanceof TypeQuery) {
            List<MessageHandler> handlerList = messageHandlers.get(UserMessageType.TYPE_RESULT);
            handlerList.add(new MessageHandler() {
                @Override
                public void handleMessage(Message message) throws MessageHandlerException {
                    if (message.getMessageType().equals(UserMessageType.TYPE_RESULT)) {
                            gatherResults = new Message[1];
                            gatherResults[0] = message;
                    }
                }
            });
            handleTypeQuery((TypeQuery) message);
        }
        return gatherResults;
    }

    @Override
    public void gatherAsynchronous(Message message, long timeout, GatherCallback callback) throws SpineException {

    }

    @Override
    public final void subscribe(
            MessageHandler messageHandler,
            UserMessageType... messageTypes)
            throws SpineException {
        for (UserMessageType type : messageTypes) {
            List<MessageHandler> handlerList = messageHandlers.get(type);
            if (handlerList == null) {
                handlerList = new ArrayList<MessageHandler>();
                messageHandlers.put(type, handlerList);
            }
            handlerList.add(messageHandler);
        }
    }

    @Override
    public void subscribe(MessageHandler messageHandler, SystemMessageType... messageTypes) throws SpineException {
        for (SystemMessageType type : messageTypes) {
            List<MessageHandler> handlerList = messageHandlers.get(type);
            if (handlerList == null) {
                handlerList = new ArrayList<MessageHandler>();
                messageHandlers.put(type, handlerList);
            }
            handlerList.add(messageHandler);
        }
    }

    @Override
    public boolean subscribe(MessageHandler messageHandler, PrivilegedMessageType messageType) throws SpineException {
        List<MessageHandler> handlerList = messageHandlers.get(messageType);
        if (handlerList == null) {
            handlerList = new ArrayList<MessageHandler>();
            messageHandlers.put(messageType, handlerList);
        }
        handlerList.add(messageHandler);
        return true;
    }

    @Override
    public boolean unsubscribe(MessageType messageType) throws SpineException {
        return false;
    }

    public void addType(String taskName,
                        ATRDecl typeDef) {
        System.out.println("Adding " + taskName + ": " + typeDef);
        actions.put(taskName, typeDef);
    }

    public void addType(SimpleTypeName typeName,
                        ATRDecl typeDef) {
        addType(typeName.getFullName(), typeDef);
    }

    public void execute(SimpleTypeName testTaskName,
                        List<Object> inParams)
            throws MessageHandlerException {
        String sender = "testClient";
        TransactionUID uid = new TransactionUID(sender, nextUid++);
        ExecuteRequest execReq = new ExecuteRequest(sender, uid, null,
                testTaskName, inParams, false);
        deliver(UserMessageType.EXECUTE_REQUEST, execReq);
    }

    public void cancel(TransactionUID uid)
            throws MessageHandlerException {
        String sender = "testClient";
        CancelRequest cancelMsg = new CancelRequest(sender, uid);
        deliver(UserMessageType.CANCEL, cancelMsg);
    }

    public void deliver(MessageType messageType,
                        Message message)
            throws MessageHandlerException {
        lastMessages.put(messageType, message);
        List<MessageHandler> handlers = messageHandlers.get(messageType);
        if (handlers != null) {
            for (MessageHandler handler : handlers) {
                handler.handleMessage(message);
            }
        }

    }

    public Message getLastMessage(UserMessageType type) {
        return lastMessages.get(type);
    }

    public UID getLastUid(UserMessageType type) {
        if (getLastMessage(type) != null) {
            return getLastMessage(type).getUid();
        }
        return null;
    }

    public List<MessageType> getSubscribedTopics() {
        List<MessageType> topicList = new ArrayList<MessageType>();
        for (MessageType messageType : messageHandlers.keySet()) {
            topicList.add(messageType);
        }
        return topicList;
    }

    public ArrayList<Message> getSentMessagesList() {
        return sentMessagesList;
    }

    private void handleTypeQuery(TypeQuery query) {
        SimpleTypeName name = query.getTypeName();
        TransactionUID uid = query.getUid();
        ATRDecl type = actions.get(name.getFullName());
        String typeSrc = null;
        if (type != null) {
            typeSrc = ATRSyntax.toSource(type);
        }
        TypeResult result = new TypeResult("mockspine", name, typeSrc, uid);
        try {
            deliver(UserMessageType.TYPE_RESULT, result);
        } catch (MessageHandlerException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleTypeListQuery(TypeListQuery query) {
        TransactionUID uid = query.getUid();
        Set<SimpleTypeName> types = new HashSet<SimpleTypeName>();
        TypeListResult result = new TypeListResult("mockspine", types, uid);
        try {
            deliver(UserMessageType.TYPE_LIST_RESULT, result);
        } catch (MessageHandlerException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleSerialNumberRequest(SerialNumberRequest query) {
        TransactionUID uid = query.getUid();
        long serial = 0;
        SerialNumberResponse response = new SerialNumberResponse("mockspine",
                uid, serial);
        try {
            deliver(SystemMessageType.SERIAL_NUMBER_RESPONSE, response);
        } catch (MessageHandlerException e) {
            throw new RuntimeException(e);
        }
    }
}
