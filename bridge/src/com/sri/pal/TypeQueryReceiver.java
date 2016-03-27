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

// $Id: TypeQueryReceiver.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.TypeListQuery;
import com.sri.tasklearning.spine.messages.TypeListResult;
import com.sri.tasklearning.spine.messages.TypeQuery;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.TypeStoreRequest;
import com.sri.tasklearning.spine.messages.TypeStoreResult;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives type queries from remote clients and sends responses based on types
 * known to our local {@link TypeStorage}.
 *
 * @author chris
 */
public class TypeQueryReceiver
        implements MessageHandler {
    private static final Logger log = LoggerFactory
            .getLogger(TypeQueryReceiver.class);

    private final Spine spine;
    private final TypeLoaderPublisher loaderPublisher;
    private final Executor threadPool;

    public TypeQueryReceiver(Spine spine,
                             TypeLoaderPublisher loaderPublisher) {
        this.spine = spine;
        this.loaderPublisher = loaderPublisher;

        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newCachedThreadPool(tf);
    }

    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (!loaderPublisher.isLocal()) {
            log.debug("No local loader; ignoring {}", message);
            return;
        }

        if (spine.getClientId().equals(message.getSender())) {
            log.warn("Message from self: {}", message);
        }

        log.debug("Spine client {} handling message: {}", spine.getClientId(),
                message);
        if (message instanceof TypeQuery) {
            threadPool.execute(new TypeQueryHandler((TypeQuery) message));
        } else if (message instanceof TypeListQuery) {
            threadPool.execute(new TypeListHandler((TypeListQuery) message));
        } else if (message instanceof TypeStoreRequest) {
            threadPool.execute(new StoreTask((TypeStoreRequest) message));
        } else {
            log.warn("Unexpected message ({}): {}", message.getClass(), message);
        }
    }

    private class TypeQueryHandler implements Runnable {
        private final TypeQuery typeMsg;

        public TypeQueryHandler(TypeQuery typeQuery) {
            this.typeMsg = typeQuery;
        }

        @Override
        public void run() {
            SimpleTypeName typeName = typeMsg.getTypeName();
            TransactionUID uid = typeMsg.getUid();

            ActionModelDef type;
            if (TypeNameFactory.isPrimitive(typeName)) {
                log.error("{} asked to load primitive type {}", uid, typeName);
                type = null;
            } else {
                /*
                 * If it's an idiom, we need to load the base name of the idiom
                 * rather than the idiom+template name. If it's not an idiom,
                 * this shouldn't hurt anything.
                 */
                typeName = typeName.getIdiomBaseName();
                try {
                    type = loaderPublisher.getLocalType(typeName);
                } catch (PALException e) {
                    log.warn("Error loading " + typeName, e);
                    type = null;
                }
            }

            String atrTypeStr;
            if(type == null) {
                atrTypeStr = null;
            } else {
                log.debug("Found {}", type);
                ATR atrType = type.getAtr();
                atrTypeStr = ATRSyntax.toSource(atrType);
            }
            TypeResult responseMsg = new TypeResult(spine.getClientId(),
                    typeName, atrTypeStr, uid);
            try {
                spine.send(responseMsg);
            } catch (SpineException e) {
                log.warn("Unable to send response to type query for "
                        + typeName, e);
            }
        }
    }

    private class TypeListHandler implements Runnable {
        private final TypeListQuery msg;

        public TypeListHandler(TypeListQuery typeListQuery) {
            this.msg = typeListQuery;
        }

        @Override
        public void run() {
            TransactionUID uid = msg.getUid();
            TypeListQuery.Subset msgSubset[] = msg.getSubset();

            // TypeListQuery.Subset is a different enum than TypeCatalog.Subset. We
            // have to convert one to the other.
            TypeStorage.Subset subset[] = new TypeStorage.Subset[msgSubset.length];
            for (int i = 0; i < msgSubset.length; i++) {
                subset[i] = TypeStorage.Subset.valueOf(msgSubset[i].toString());
            }

            TypeListResult resultMsg;
            try {
                Set<SimpleTypeName> result = loaderPublisher.listLocal(subset);
                if (result == null) {
                    result = Collections.emptySet();
                }
                resultMsg = new TypeListResult(spine.getClientId(), result, uid);
            } catch (Exception e) {
                log.warn("Failed to list for " + Arrays.toString(subset), e);
                ErrorInfo error = ErrorFactory.error(spine.getClientId(),
                        ErrorType.STORAGE);
                resultMsg = new TypeListResult(spine.getClientId(), error, uid);
            }
            try {
                spine.send(resultMsg);
            } catch (SpineException e) {
                log.warn(
                        "Unable to send response to list query for "
                                + Arrays.toString(msgSubset), e);
            }
        }
    }

    private class StoreTask
            implements Runnable {
        private final TypeStoreRequest request;

        public StoreTask(TypeStoreRequest req) {
            request = req;
        }

        @Override
        public void run() {
            SimpleTypeName name = request.getName();
            TransactionUID uid = request.getUid();
            TypeStoreResult reply;
            try {
                String typeXmlStr = request.getTypeStr();
                loaderPublisher.put(name, typeXmlStr);
                reply = new TypeStoreResult(spine.getClientId(), null, uid);
            } catch (Exception e) {
                reply = new TypeStoreResult(spine.getClientId(), e, uid);
            }
            try {
                spine.send(reply);
            } catch (SpineException e) {
                log.warn("Unable to send " + reply, e);
            }
        }
    }
}
