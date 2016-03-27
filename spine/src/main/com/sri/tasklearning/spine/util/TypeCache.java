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

// $Id: TypeCache.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.pal.common.TypeName;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.CacheExpire;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.UserMessageType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches types to minimize chattiness when retrieving types via the Spine.
 */
public class TypeCache
        implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(TypeCache.class);

    private final Map<TypeName, Reference<ATR>> map;
    private final Map<Reference<ATR>, TypeName> refMap;
    private final ReferenceQueue<ATR> refQueue;
    private final Set<ATR> allTypes;

    public TypeCache(Spine spine)
            throws SpineException {
        map = new HashMap<TypeName, Reference<ATR>>();
        refMap = new HashMap<Reference<ATR>, TypeName>();
        refQueue = new ReferenceQueue<ATR>();
        allTypes = new HashSet<ATR>();

        if(spine != null) {
            spine.subscribe(this, UserMessageType.CACHE_EXPIRE);
            spine.subscribe(this, UserMessageType.TYPE_RESULT);
        }
    }

    /**
     * Adds the given type to the cache. If the type is a transient action, it
     * will not be added.
     *
     * @param atr
     *            the object to be cached
     */
    public void add(ATR atr) {
        /*
         * We could skip transient actions here, but instead TypeLoaderPublisher
         * sends a CacheExpire message whenever they're modified. That allows us
         * to cache procedures, giving a nice performance boost.
         */

        TypeName name = TypeUtil.getName(atr);
        log.debug("Cache add: {}", name);
        SoftReference<ATR> ref = new SoftReference<ATR>(atr,
                refQueue);
        synchronized (this) {
            map.put(name, ref);
            refMap.put(ref, name);
        }

        /* This is a deliberate memory leak: */
        allTypes.add(atr);
    }

    /**
     * Removes the named item from the cache. This is not necessary from a
     * perspective of avoiding memory leaks. It's only needed for the sake of
     * data consistency.
     *
     * @param name
     *            the name of the item to remove
     */
    public void remove(TypeName name) {
        log.debug("Cache remove: {}", name);
        synchronized(this) {
            Reference<ATR> ref = map.remove(name);
            if (ref != null) {
                refMap.remove(ref);
            }
        }
    }

    /**
     * Retrieves a type from the cache.
     *
     * @param name
     *            the name of the type to retrieve
     * @return the requested type, or {@code null} if none is currently cached
     */
    public ATR get(TypeName name) {
        ATR result = null;
        Reference<ATR> ref;
        synchronized(this) {
            ref = map.get(name);
        }
        if (ref != null) {
            result = ref.get();
        }
        log.debug("Cache {}: {}", result == null ? "miss" : "hit", name);

        cleanCache();

        return result;
    }

    private void cleanCache() {
        Reference<? extends ATR> ref;
        while ((ref = refQueue.poll()) != null) {
            TypeName name = refMap.get(ref);
            log.debug("Cache clean: {}", name);
            synchronized (this) {
                map.remove(name);
                refMap.remove(ref);
            }
        }
    }

    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (message instanceof CacheExpire) {
            CacheExpire ce = (CacheExpire) message;
            TypeName name = ce.getTypeName();
            log.debug("Expiring {}", name);
            remove(name);
        } else if (message instanceof TypeResult) {
            TypeResult tr = (TypeResult) message;
            ATRDecl decl = tr.getTypeAtr();
            if (decl != null) {
                add(decl);
            }
        } else {
            log.warn("Unexpected message ({}): {}", message.getClass(), message);
            return;
        }
    }

    public void shutdown() {
        map.clear();
        refMap.clear();
        cleanCache();
    }
}
