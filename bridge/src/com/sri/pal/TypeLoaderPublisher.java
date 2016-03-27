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

// $Id: TypeLoaderPublisher.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.sri.pal.TypeStorage.Subset;
import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.RequestCanceler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.upgrader.MemoryTypeStorage;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.CacheExpire;
import com.sri.tasklearning.spine.messages.TypeListQuery;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes a batch of type load requests the type loader.
 * <p>
 * Requirements for type loading:
 * <ol>
 * <li>We use {@link CallbackHandler} and {@link RequestCanceler} for
 * asynchronous calling semantics.
 * <li>This class sends requests to only one type loader.
 * <li>The request is for a set of types, not just one type.
 * <li>For each type in the set, this class receives a result from the loader.
 * <li>The loader implementation may run asynchronously or synchronously. If
 * async, then our call to it will immediately return and leave us with a
 * RequestCanceler. Callbacks will come in afterwards. If sync, the callbacks
 * will all come in before our call to the loader returns, and the resultant
 * RequestCanceler will be useless.
 * <li>The load request we receive will have a RequestCanceler associated with
 * it. If that's called, we need to propagate it to the RequestCanceler of the
 * loader that's handling the request.
 * <li>It's common for a loader implementation we call to incorrectly implement
 * its handling of negative results (type not found). It may return a Result
 * with a null TypeName, or it may return a null Result. Or it may never call
 * our callback handler. We need to watch for all these conditions and log them.
 * <li>We may receive multiple load requests at the same time, for possibly
 * overlapping sets of TypeNames.
 * </ol>
 *
 * @author chris
 */
class TypeLoaderPublisher {
    private static final Logger log = LoggerFactory
            .getLogger(TypeLoaderPublisher.class);

    private static final long TIMEOUT = 30 * 1000;
    private static final String AM_LOG_DIR = "PAL.actionModelLogDir";
    private static final String LOG_BASE_NAME = "load";

    private ErrorFactory errorFactory;
    private final Timer timer;
    private final Bridge bridge;
    private TypeStorage typeStorage;
    private RemoteTypeStorage remoteStorage;
    private boolean storageAssigned = false;
    private final MessageDigest md5;

    TypeLoaderPublisher(Bridge bridge) {
        this.bridge = bridge;
        timer = new Timer(true);
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Used for the offline ActionModel, which in turn is used for upgrades when
     * the whole Task Learning system isn't running.
     */
    TypeLoaderPublisher(MemoryTypeStorage memoryTypeStorage) {
        bridge = null;
        timer = new Timer(true);
        typeStorage = memoryTypeStorage;
        storageAssigned = true;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private ErrorFactory getErrorFactory() {
        if (errorFactory == null) {
            if (bridge == null) {
                errorFactory = new ErrorFactory("offline");
            } else {
                errorFactory = new ErrorFactory(bridge.getSpine().getClientId());
            }
        }
        return errorFactory;
    }

    /**
     * Lazy initialization of our type storage. This is a memory-based cache
     * wrapping the underlying persistent storage.
     * @throws PALException
     */
    private synchronized void initStorage() {
        if (!storageAssigned) {
            try {
                typeStorage = bridge.getTypeStorage();
                if(typeStorage == null) {
                    remoteStorage = new RemoteTypeStorage(bridge);
                }
                storageAssigned = true;
            } catch (PALException e) {
                throw new RuntimeException("Failed to initialize storage", e);
            }
        }
    }

    private Spine getSpine() {
        if (bridge != null) {
            return bridge.getSpine();
        } else {
            return null;
        }
    }

    /**
     * Asynchronously load the requested type.
     */
    RequestCanceler load(CallbackHandler<ActionModelDef> callbackHandler,
                         SimpleTypeName desiredType) {
        initStorage();
        if(desiredType.equals(TypeUtil.GESTURE_END_NAME)) {
            try {
                callbackHandler.result(new GestureEndDef(bridge));
            } catch (PALException e) {
                log.warn("Impossible exception", e);
            }
            return new RequestCanceler() {
                @Override
                public void cancel() {
                }
            };
        }

        if (typeStorage != null) {
            LoadChain chain = new LoadChain(callbackHandler, desiredType);
            RequestCanceler canceler = localLoad(chain, desiredType);
            return canceler;
        } else {
            return remoteStorage.loadType(callbackHandler, desiredType);
        }
    }

    /**
     * Synchronously load a type, using only local loaders. This is used by
     * {@link TypeQueryReceiver}, which is handling a remote request for type
     * loading. So we need to make sure we don't reflect the same type loading
     * request back out into the cloud.
     *
     * @param desiredActions
     * @return the desired type, or {@code null}
     */
    ActionModelDef getLocalType(SimpleTypeName desiredType)
            throws PALException {
        initStorage();
        if (typeStorage == null) {
            return null;
        }
        return getType(desiredType);
    }

    /**
     * Synchronously load a single type. This is a shortcut for the strange
     * calling conventions of {@link #load}.
     *
     * @param name
     * @return null if not found
     * @throws PALException
     */
    ActionModelDef getType(SimpleTypeName name)
            throws PALException {
        SynchronousCallbackHandler<ActionModelDef> sch = new SynchronousCallbackHandler<ActionModelDef>();
        load(sch, name);
        try {
            return sch.waitForResult();
        } catch (PALException e) {
            if (sch.getError().getErrorId() == ErrorType.NOT_ALL_LOADED.ordinal()) {
                return null;
            } else {
                throw e;
            }
        }
    }

    /**
     * Does the work of actually loading the requested type's string
     * representation. Called by the other entry point methods.
     */
    private RequestCanceler localLoad(CallbackHandler<String> callbackHandler,
                                      SimpleTypeName desiredType) {
        AsyncChain<String, String> chain = new AsyncChain<String, String>(
                callbackHandler) {
            @Override
            public void results(String result) {
                subCH.result(result);
            }
        };
        TimeoutTask tt = new TimeoutTask(chain, desiredType);
        timer.schedule(tt, TIMEOUT);
        log.debug("Calling {} to load {}", typeStorage, desiredType);
        RequestCanceler rc = typeStorage.loadType(chain, desiredType);
        chain.addCanceler(rc);
        return chain;
    }

    /**
     * Calls {@link #list} only if the authoritative information is local.
     */
    Set<SimpleTypeName> listLocal(Subset... subsets)
            throws PALException {
        initStorage();
        if (typeStorage == null) {
            return null;
        } else {
            return list(subsets);
        }
    }

    /**
     * Call {@link TypeStorage#listTypes}.
     */
    Set<SimpleTypeName> list(Subset... subsets)
            throws PALException {
        initStorage();
        if (typeStorage != null) {
            return typeStorage.listTypes(subsets);
        } else {
            return remoteStorage.listTypes(subsets);
        }
    }

    /**
     * Call {@link TypeStorage#putType}.
     */
    void put(SimpleTypeName name,
                    String typeStr)
            throws PALException {
        initStorage();

        /*
         * Always send a cache expire message, because we may be overwriting something.
         */
        Spine spine = getSpine();
        if (spine != null) {
            TypeListQuery.Subset type;
            if (typeStr == null || typeStr.equals("")) {
                type = null;
            } else if (TypeUtil.isProcedureString(typeStr)) {
                type = TypeListQuery.Subset.PROCEDURE;
            } else if (TypeUtil.isActionString(typeStr)) {
                type = TypeListQuery.Subset.ACTION;
            } else if (TypeUtil.isActionFamilyString(typeStr)) {
                type = TypeListQuery.Subset.FAMILY;
            } else if (TypeUtil.isIdiomString(typeStr)) {
                type = TypeListQuery.Subset.IDIOM;
            } else if (TypeUtil.isConstraintString(typeStr)) {
                type = TypeListQuery.Subset.CONSTRAINT;
            } else {
                type = TypeListQuery.Subset.TYPE;
            }
            CacheExpire msg = new CacheExpire(spine.getClientId(),
                    spine.getNextUid(), name, type);
            try {
                spine.send(msg);
            } catch (SpineException e) {
                log.warn("Unable to send cache expiry message " + msg, e);
            }
        }

        if (typeStorage != null) {
            try {
                typeStorage.putType(name, typeStr);
            } catch (Exception e) {
                throw new PALException(typeStorage + " failed to store " + name
                        + " as " + typeStr, e);
            }
        } else {
            remoteStorage.putType(name, typeStr);
        }
    }

    boolean isLocal() {
        initStorage();
        return typeStorage != null;
    }

    /**
     * If action model logging is enabled, log the just-loaded action into a
     * file in the specified directory.
     *
     * @param result
     *            result of a load request, which will be logged to a file
     */
    private void maybeLogResult(String result) {
        String logDirStr = System.getProperty(AM_LOG_DIR);
        if (logDirStr != null) {
            File logDir = new File(logDirStr);
            String filename;
            try {
                synchronized (md5) {
                    md5.reset();
                    md5.update(result.getBytes("UTF-8"));
                    byte[] hash = md5.digest();
                    BigInteger hashInt = new BigInteger(1, hash);
                    String hashStr = hashInt.toString(16);
                    hashStr = hashStr.substring(0, 12);
                    filename = LOG_BASE_NAME + hashStr + ".xml";
                }
            } catch (Exception e) {
                log.warn("Unable to generate hash from " + result, e);
                return;
            }
            File logFile = new File(logDir, filename);
            Writer out = null;
            try {
                out = new FileWriter(logFile);
                out.append(result);
            } catch (IOException e) {
                log.warn("Unable to log action model fragment to " + logFile, e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        log.warn("Unable to close file " + logFile, e);
                    }
                }
            }
        }
    }

    private class LoadChain extends AsyncChain<String, ActionModelDef>{
        private final SimpleTypeName desiredType;

        public LoadChain(CallbackHandler<ActionModelDef> subCallbackHandler,
                         SimpleTypeName desiredType) {
            super(subCallbackHandler);
            this.desiredType = desiredType;
        }

        @Override
        public void results(String typeStr) {
            if (typeStr == null) {
                ErrorInfo err = getErrorFactory().error(
                        ErrorType.NOT_ALL_LOADED, desiredType.getFullName());
                subCH.error(err);
                return;
            }
            maybeLogResult(typeStr);
            if (TypeUtil.isProcedureString(typeStr)) {
                CallbackHandler<?> ch = subCH;
                @SuppressWarnings("unchecked")
                RequestCanceler canceler = bridge.getPALExecutor().load(
                        (CallbackHandler<ProcedureDef>) ch, typeStr);
                addCanceler(canceler);
                return;
            } else {
                Set<ActionModelDef> types;
                try {
                    types = bridge.getActionModel().read(typeStr,
                            desiredType.getNamespace());
                } catch (PALException e) {
                    log.info("Error building type " + desiredType
                            + " from string", e);
                    ErrorInfo err = getErrorFactory().error(
                            ErrorType.ACTION_MODEL, desiredType);
                    subCH.error(err);
                    return;
                }
                for (ActionModelDef maybeType : types) {
                    if (maybeType.getName().equals(desiredType)) {
                        subCH.result(maybeType);
                        return;
                    }
                }
                ErrorInfo err = getErrorFactory().error(
                        ErrorType.NOT_ALL_LOADED, desiredType);
                subCH.error(err);
                return;
            }
        }
    }

    /**
     * Be on the lookout for loaders that never give us an answer at all. We
     * can't know for certain that the loader is broken, because CPOF at least
     * could wait a very long time before giving us our answer. However, it's
     * probably an error, and we can log it in hopes that somebody will notice.
     */
    private class TimeoutTask
            extends TimerTask {
        private final AsyncChain<?, ?> chain;
        private final SimpleTypeName desiredType;

        public TimeoutTask(AsyncChain<?, ?> chain,
                           SimpleTypeName typeName) {
            this.chain = chain;
            desiredType = typeName;
        }

        @Override
        public void run() {
            if(chain.activityCount() == 0) {
                log.warn("{} never answered query for {}",
                        typeStorage, desiredType);
            }
        }
    }
}
