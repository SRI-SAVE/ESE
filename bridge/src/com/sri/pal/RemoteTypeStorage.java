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

// $Id: RemoteTypeStorage.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Set;

import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.pal.TypeStorage.Subset;
import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.RequestCanceler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.TypeListQuery;
import com.sri.tasklearning.spine.messages.TypeListResult;
import com.sri.tasklearning.spine.messages.TypeQuery;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.TypeStoreRequest;
import com.sri.tasklearning.spine.messages.TypeStoreResult;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.spine.util.ReplyWatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acts as a proxy for a TypeStorage implementation which lives in another
 * instance of the Bridge. This one accepts local requests and forwards them to
 * the remote for handling.
 */
class RemoteTypeStorage {
    private static final Logger log = LoggerFactory
            .getLogger(RemoteTypeStorage.class);

    private final Bridge bridge;
    private final ErrorFactory errorFactory;

    RemoteTypeStorage(Bridge bridge) {
        this.bridge = bridge;
        errorFactory = new ErrorFactory(bridge.getSpine().getClientId());
    }

    private ActionModelFactory getAmFactory() {
        return bridge.getActionModelFactory();
    }

    private ReplyWatcher<TypeStoreResult> getStorageWatcher() {
        return bridge.getTypeStoreWatcher();
    }

    private ReplyWatcher<TypeListResult> getTypeListWatcher() {
        return bridge.getTypeListWatcher();
    }

    private ReplyWatcher<TypeResult> getTypeQueryWatcher() {
        return bridge.getTypeQueryWatcher();
    }

    RequestCanceler loadType(CallbackHandler<ActionModelDef> callbackHandler,
                             SimpleTypeName name) {
        /* TODO We're turning this asynchronous call into a synchronous one. */
        try {
            ActionModelDef type = loadType(name);
            if (type != null) {
                callbackHandler.result(type);
            } else {
                callbackHandler.result(null);
            }
        } catch (SpineException e) {
            log.warn("Unable to load type " + name, e);
            ErrorInfo error = errorFactory.error(ErrorType.INTERNAL_COMMS);
            callbackHandler.error(error);
        }
        return new RequestCanceler() {
            @Override
            public void cancel() {
                // Ignore.
            }
        };
    }

    private ActionModelDef loadType(SimpleTypeName name)
            throws SpineException {
        ActionModelDef result = null;
        ATRDecl atrType = loadRemoteType(name);
        if (atrType != null) {
            try {
                result = getAmFactory().makeActionModelDef(atrType,
                        name.getVersion(), name.getNamespace());
            } catch (Exception e) {
                String msg = "Unable to load required types for " + name;
                log.warn(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
        return result;
    }

    private ATRDecl loadRemoteType(SimpleTypeName name)
            throws SpineException {
        Spine spine = bridge.getSpine();
        TransactionUID uid = spine.getNextUid();
        TypeQuery query = new TypeQuery(spine.getClientId(), name, uid);
        TypeResult resultMsg = getTypeQueryWatcher().sendAndGetReply(query);
        ATRDecl result = resultMsg.getTypeAtr();
        return result;
    }

    Set<SimpleTypeName> listTypes(Subset... subset)
            throws PALException {
        Spine spine = bridge.getSpine();
        // TypeListQuery.Subset is a mirror enum of TypeCatalog.Subset. Copy
        // values from one to the other:
        TypeListQuery.Subset msgSubset[] = new TypeListQuery.Subset[subset.length];
        for (int i = 0; i < subset.length; i++) {
            msgSubset[i] = TypeListQuery.Subset.valueOf(subset[i].toString());
        }

        TransactionUID uid = spine.getNextUid();
        TypeListQuery query = new TypeListQuery(spine.getClientId(), uid,
                msgSubset);

        TypeListResult result;
        try {
            result = getTypeListWatcher().sendAndGetReply(query);
        } catch (SpineException e) {
            throw new PALRemoteException(e);
        }

        return result.getTypeNames();
    }

    void putType(SimpleTypeName name,
                 String xmlStr)
            throws PALException {
        Spine spine = bridge.getSpine();
        TransactionUID uid = spine.getNextUid();
        TypeStoreRequest req = new TypeStoreRequest(spine.getClientId(), name,
                xmlStr, uid);

        TypeStoreResult result;
        try {
            result = getStorageWatcher().sendAndGetReply(req);
        } catch (SpineException e) {
            throw new PALRemoteException(e);
        }
        Throwable error = result.getError();
        if (error != null) {
            throw new PALRemoteException(error);
        }
    }
}
