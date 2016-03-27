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

// $Id: TypeFetcher.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.mediators;

import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.lumenpal.ExecutionHandler;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ExecutorListQuery;
import com.sri.tasklearning.spine.messages.ExecutorListResult;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.TypeQuery;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ReplyWatcher;
import com.sri.tasklearning.spine.util.TypeCache;

/**
 * Synchronously fetches types via the Spine.
 *
 * @author chris
 */
public class TypeFetcher {
    private final Spine spine;
    private final LockingActionModel actionModel;
    private final TypeCache cache;
    private final ReplyWatcher<TypeResult> watcher;

    public TypeFetcher(Spine spine,
                       LockingActionModel actionModel,
                       ReplyWatcher<TypeResult> typeQueryWatcher)
            throws SpineException {
        this.spine = spine;
        this.actionModel = actionModel;
        cache = new TypeCache(spine);
        watcher = typeQueryWatcher;
    }

    /**
     * Request the given type from any type listeners which are connected to the
     * Spine. This method will block indefinitely until the requested answer
     * comes back.
     *
     * @param typeName
     *            the name of the type to fetch
     * @return the ATR representation of the requested type
     * @throws SpineException
     *             if a communication error occurs
     */
    /*
     * TODO Exclude any other threads from calling this method to fetch the same
     * type. When this thread finishes, the desired item will be in the cache
     * and any blocked threads can pick it up from there.
     */
    public ATRDecl getType(SimpleTypeName typeName)
            throws SpineException {
        if (TypeNameFactory.isPrimitive(typeName)) {
            throw new RuntimeException("Can't load primitive type " + typeName);
        }
        ATRDecl result = actionModel.getRaw(typeName);

        if (result == null) {
            result = (ATRDecl) cache.get(typeName);
        }

        if (result == null) {
            TransactionUID uid = spine.getNextUid();
            TypeQuery query = new TypeQuery(spine.getClientId(), typeName, uid);
            TypeResult resultMsg = watcher.sendAndGetReply(query);
            result = resultMsg.getTypeAtr();
        }

        if (result != null) {
            cache.add(result);
        }

        return result;
    }

    /**
     * Checks to see if the indicated action has a registered executor somewhere
     * in the system. If no executor is registered, then Lumen won't be able to
     * successfully run any procedure that calls the action.
     *
     * @param actName
     *            the action to check
     * @return {@code true} iff an executor is registered
     * @throws SpineException
     *             if a communication error occurs
     */
    public boolean hasExecutor(SimpleTypeName actName)
            throws SpineException {
        if (ExecutionHandler.NAMESPACE.equals(actName.getNamespace())) {
            return true;
        }

        TransactionUID uid = spine.getNextUid();
        ExecutorListQuery elq = new ExecutorListQuery(spine.getClientId(), uid,
                actName);
        Message[] responses = spine.gather(elq, Spine.DEFAULT_TIMEOUT);
        for (Message msg : responses) {
            ExecutorListResult result = (ExecutorListResult) msg;
            if (result.isExecutor()) {
                return true;
            }
        }
        return false;
    }

    public void shutdown() {
        cache.shutdown();
    }
}
