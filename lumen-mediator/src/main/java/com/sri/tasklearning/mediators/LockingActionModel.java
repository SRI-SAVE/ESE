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

// $Id: LockingActionModel.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.mediators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.mediator.MediatorException;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameExpr;
import com.sri.tasklearning.lumenpal.locks.DebugReadWriteLock;
import com.sri.tasklearning.lumenpal.locks.MultiThreadReadWriteLock;
import com.sri.tasklearning.spine.util.TypeUtil;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an abstraction of an action model which includes advisory locks.
 * Before making use of a type (or action), the user should get a read lock on
 * it. This can be done before the type has been added. When the type is no
 * longer in use, its lock should be released.
 * <p>
 * In practice, two instances of this class exist: One for the LAPDOG Mediator,
 * and another for the Lumen Mediator. New types are added to this, and it calls
 * the appropriate {@link TypeAdder} to actually add the types to LAPDOG or
 * Lumen.
 *
 * @author chris
 * @see com.sri.tasklearning.lapdogController.LockingActionModel
 */
public class LockingActionModel {
    private static final Logger log = LoggerFactory
            .getLogger(LockingActionModel.class);
    private static final boolean debugLocks = false;

    private final TypeAdder typeAdder;
    private final Map<SimpleTypeName, ReadWriteLock> locks;
    private final Map<SimpleTypeName, ATRDecl> types;
    private final Map<SimpleTypeName, Collection<Lock>> dependentLocks;
    private final ExecutorService threadPool;
    private final Object addMonitor = new Object();

    public LockingActionModel(TypeAdder adder) {
        typeAdder = adder;
        locks = new HashMap<SimpleTypeName, ReadWriteLock>();
        types = new HashMap<SimpleTypeName, ATRDecl>();
        dependentLocks = new HashMap<SimpleTypeName, Collection<Lock>>();
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newCachedThreadPool(tf);
    }

    /**
     * Acquires a read lock on a given type. The type doesn't have to exist in
     * the action model in order to acquire a lock on it. This is a read lock,
     * so multiple locks can exist simultaneously and be held by different
     * threads. This lock prevents the type from being removed from the action
     * model.
     *
     * @param typeName
     * @return a {@code Lock} which has already been locked. This object should
     *         not be locked by the caller, as this method has already locked
     *         it. However, the lock should be unlocked (via {@link Lock#unlock}
     *         ) when the lock is no longer needed.
     */
    public Lock getReadLock(SimpleTypeName typeName) {
        ReadWriteLock rwl;
        synchronized (this) {
            rwl = locks.get(typeName);
            if (rwl == null) {
                if (debugLocks) {
                    rwl = new DebugReadWriteLock(typeName);
                } else {
                    rwl = new MultiThreadReadWriteLock();
                }
                locks.put(typeName, rwl);
            }
        }
        Lock readLock = rwl.readLock();
        log.debug("Got readlock for {}", typeName);
        readLock.lock();
        return readLock;
    }

    /**
     * Adds the definition of the given type to the action model. The type
     * should already be locked before this is called. This method also adds the
     * type to Lumen itself.
     *
     * @throws MediatorException
     */
    public void add(ATRDecl type)
            throws MediatorsException {
        SimpleTypeName typeName = TypeUtil.getName(type);
        synchronized (addMonitor) {
            if (types.get(typeName) == null) {
                if (TypeUtil.isAction(type)) {
                    ATRActionDeclaration inhAction = inheritAction((ATRActionDeclaration) type);
                    typeAdder.add(inhAction);
                } else {
                    typeAdder.add(type);
                }
                types.put(typeName, type);

                /*
                 * We lock dependent types so we don't accidentally remove a
                 * required type while this one is still around. We don't need
                 * to lock transitive dependencies because we're locking direct
                 * dependencies, and they lock the transitive ones.
                 */
                Collection<Lock> locks = new HashSet<Lock>();
                for (TypeName dependent : TypeUtil.getRequiredTypes(type)) {
                    /*
                     * If it's something like list<Thing>, pull out the Thing
                     * and lock it.
                     */
                    while (dependent instanceof TypeNameExpr) {
                        TypeNameExpr expr = (TypeNameExpr) dependent;
                        dependent = expr.getInner();
                    }
                    SimpleTypeName toLock = (SimpleTypeName) dependent;
                    Lock lock = getReadLock(toLock);
                    locks.add(lock);
                }
                dependentLocks.put(typeName, locks);
            }
        }
    }

    /**
     * Try to remove the named type, but don't remove it if it's in use, and
     * don't block waiting for it to become unused.
     *
     * @return {@code true} if the type was removed.
     */
    public boolean maybeRemove(SimpleTypeName typeName) {
        ATRDecl type = getInherited(typeName);
        if (type == null) {
            log.debug("Nothing known about {}", typeName);
            return false;
        }

        ReadWriteLock rwl = locks.get(typeName);
        if (rwl == null) {
            log.debug("No lock exists for {}", typeName);
            return false;
        }
        Lock lock = rwl.writeLock();
        if (lock.tryLock()) {
            try {
                log.debug("Removing {}", typeName);
                if (typeAdder.remove(type)) {
                    types.remove(typeName);

                    /*
                     * Now that it's removed, release its read locks on
                     * dependent types.
                     */
                    Collection<Lock> depLocks = dependentLocks.remove(typeName);
                    for (Lock depLock : depLocks) {
                        depLock.unlock();
                    }

                    log.debug("Removed {}", typeName);
                    return true;
                } else {
                    log.debug("Didn't remove {}", typeName);
                    return false;
                }
            } catch (Exception e) {
                log.warn("Failed to remove " + typeName + " from Lumen", e);
                return false;
            } finally {
                // TODO This is a tiny memory leak. However, if we actually
                // remove the entry from locks, we get NPEs sometimes when
                // LumenTaskResultListener.cleanup() calls releaseReadLock().
                // The NPEs don't seem to cause any harm, but they're worrying.
                // locks.remove(typeName);
                lock.unlock();
            }
        } else {
            log.debug("Couldn't get lock for {} -- not removing", typeName);
            return false;
        }
    }

    /**
     * Asynchronously removes actions from Lumen.
     *
     * @param targets
     *            the actions to be removed
     */
    public void maybeRemove(Collection<ATRDecl> targets) {
        if (targets == null || targets.isEmpty()) {
            return;
        }

        TypeRemover remover = new TypeRemover(targets);
        threadPool.execute(remover);
    }

    /**
     * @return the requested type, or {@code null} if it can't be found. The
     *         caller should already have a read lock on the requested type.
     */
    public ATRDecl getRaw(SimpleTypeName typeName) {
        return types.get(typeName);
    }

    /**
     * @return the requested type. If it's an action that inherits from another
     *         action, rewrite its signature to include its parent's parameters.
     */
    public ATRDecl getInherited(SimpleTypeName typeName) {
        ATRDecl decl = types.get(typeName);
        if (TypeUtil.isAction(decl)) {
            decl = inheritAction((ATRActionDeclaration) decl);
        }
        return decl;
    }

    private ATRActionDeclaration inheritAction(ATRActionDeclaration action) {
        SimpleTypeName parentName = TypeUtil.getParent(action);
        if (parentName == null) {
            return action;
        }
        SimpleTypeName name = TypeUtil.getName(action);
        ATRActionDeclaration origParent = (ATRActionDeclaration) getRaw(parentName);
        if (origParent == null) {
            throw new RuntimeException("Unable to retrieve parent "
                    + parentName + " of " + name);
        }
        ATRActionDeclaration inhParent = inheritAction(origParent);

        CTRConstructor ctrBuilder = new CTRConstructor();
        Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
        propMap.putAll(inhParent.getProperties().getMap());
        propMap.putAll(action.getProperties().getMap());

        /* Build the param list. */
        List<ATRParameter> params = new ArrayList<ATRParameter>();
        /* Parent's inputs first. */
        for (ATRParameter param : inhParent.getSignature().getElements()) {
            if (param.getMode() == Modality.INPUT) {
                params.add(param);
            }
        }
        /* This action's inputs. */
        for (ATRParameter param : action.getSignature().getElements()) {
            if (param.getMode() == Modality.INPUT) {
                params.add(param);
            }
        }
        /* Parent's outputs. */
        for (ATRParameter param : inhParent.getSignature().getElements()) {
            if (param.getMode() == Modality.OUTPUT) {
                params.add(param);
            }
        }
        /* This action's outputs. */
        for (ATRParameter param : action.getSignature().getElements()) {
            if (param.getMode() == Modality.OUTPUT) {
                params.add(param);
            }
        }

        ATRSig sig = ctrBuilder.createSignature(name.getFullName(), params);
        ATRMap props = ctrBuilder.createMap(propMap);
        ATRActionDeclaration result;
        if (propMap.containsKey(TypeUtil.EXECUTEJ)) {
            ATRTerm callMethodTerm = propMap.get(TypeUtil.EXECUTEJ);
            String callMethod = ((ATRLiteral) callMethodTerm).getString();
            result = ctrBuilder.createActionDeclaration(sig, callMethod, props);
        } else {
            result = ctrBuilder.createActionDeclaration(sig, (ATRTask) null,
                    props);
        }

        return result;
    }

    public class TypeRemover
            implements Runnable {
        private final Set<ATRDecl> removalSet;

        public TypeRemover(Collection<ATRDecl> targets) {
            removalSet = new CopyOnWriteArraySet<ATRDecl>();
            removalSet.addAll(targets);
        }

        @Override
        public void run() {
            /*
             * Actions have dependencies on other actions. So try to remove
             * them, and if anything was successfully removed then try the whole
             * lot again.
             */
            boolean wasRemoved;
            do {
                wasRemoved = false;
                for (ATRDecl decl : removalSet) {
                    SimpleTypeName name = TypeUtil.getName(decl);
                    if (maybeRemove(name)) {
                        wasRemoved = true;
                        removalSet.remove(decl);
                    }
                }
            } while (wasRemoved);
        }
    }

    public void shutdown() {
        threadPool.shutdown();
    }
}
