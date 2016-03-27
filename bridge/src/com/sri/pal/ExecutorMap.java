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

// $Id: ExecutorMap.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ExecutorListQuery;
import com.sri.tasklearning.spine.messages.ExecutorListResult;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ExecutorMap
        implements MessageHandler {
    private static final Logger log = LoggerFactory
            .getLogger(ExecutorMap.class);

    private final Map<SimpleTypeName, ActionExecutor> executors;
    private final Bridge bridge;

    ExecutorMap(Bridge bridge) {
        this.bridge = bridge;
        executors = new HashMap<SimpleTypeName, ActionExecutor>();
    }

    private Spine getSpine() {
        return bridge.getSpine();
    }

    void remove(SimpleTypeName name) {
        String sparkNs = LumenProcedureExecutor.getNamespace();
        if (sparkNs.equals(name.getNamespace())) {
            return;
        }

        synchronized (this) {
            executors.remove(name);
        }
    }

    synchronized void put(SimpleTypeName name,
                          ActionExecutor executor)
            throws PALDuplicateExecutorException,
            SpineException {
        String sparkNs = LumenProcedureExecutor.getNamespace();
        if (sparkNs.equals(name.getNamespace())) {
            return;
        }

        if (executors.containsKey(name)) {
            throw new PALDuplicateExecutorException(name
                    + " is locally registered to " + executors.get(name));
        }

        TransactionUID uid = getSpine().getNextUid();
        ExecutorListQuery execQuery = new ExecutorListQuery(
                getSpine().getClientId(), uid, name);
        Message[] responses = getSpine().gather(execQuery, Spine.DEFAULT_TIMEOUT);

        String spineClientName = null;
        for (Message message : responses) {
            ExecutorListResult result = (ExecutorListResult) message;
            if (result.isExecutor()) {
                spineClientName = result.getSender();
            }
        }
        if (spineClientName != null) {
            throw new PALDuplicateExecutorException("Spine client (" + spineClientName
                    + ") already registered for " + name);
        }

        executors.put(name, executor);
    }

    /**
     * This is a local query; it doesn't go through the Spine.
     */
    ActionExecutor get(SimpleTypeName name) {
        // Check the action name to determine if it belongs to Lumen.
        String ns = name.getNamespace();
        String sparkNs = LumenProcedureExecutor.getNamespace();
        if (sparkNs.equals(ns)) {
            LumenProcedureExecutor sparkExec = (LumenProcedureExecutor) bridge
                    .getPALExecutor();
            return sparkExec;
        }

        synchronized (this) {
            return executors.get(name);
        }
    }

    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (!(message instanceof ExecutorListQuery)) {
            log.warn("Received unexpected message ({}): {}",
                    message.getClass(), message);
            return;
        }

        ExecutorListQuery query = (ExecutorListQuery) message;

        if (query.getSender().equals(getSpine().getClientId())) {
            // Ignore our own query.
            return;
        }

        SimpleTypeName name = query.getActionName();
        boolean result;
        synchronized (this) {
            result = executors.containsKey(name);
        }
        TransactionUID uid = query.getUid();

        ExecutorListResult response = new ExecutorListResult(
                getSpine().getClientId(), uid, result);
        try {
            getSpine().send(response);
        } catch (SpineException e) {
            log.warn("Unable to send response {} to query {}", response, query);
        }
    }

    Set<ActionExecutor> getAll() {
        Set<ActionExecutor> result = new HashSet<ActionExecutor>();
        synchronized (this) {
            result.addAll(executors.values());
        }
        return result;
    }
}
