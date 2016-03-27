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

// $Id: WithLockedTypes.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.mediators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.util.TypeUtil;

/**
 * The LAPDOG and Lumen Mediators both need to perform certain operations after
 * having locked all relevant types and actions. This class abstracts out the
 * dependency finding, locking, and unlocking.
 */
public class WithLockedTypes {
    private final LockingActionModel actionModel;

    public WithLockedTypes(LockingActionModel am) {
        actionModel = am;
    }

    /**
     * Perform an action on an object after having locked all types relevant to
     * the object. Before the action is called, dependencies will be discovered
     * and locked.
     *
     * @param <R>
     *            the result type which the action will return
     * @param <T>
     *            the type of the object to operate on
     * @param innerAction
     *            the action to perform after all locking has been done
     * @param object
     *            the object to operate on
     * @param finder
     *            finds other objects (types and actions) that the object
     *            depends on
     */
    public <R, T> void lockedAction(Action<R, T> innerAction,
                                    T object,
                                    DependencyFinder<T> finder)
            throws SpineException,
            MediatorsException {
        final Collection<Lock> locks = new ArrayList<Lock>();
        final List<ATRDecl> requiredTypes = new ArrayList<ATRDecl>();
        Runnable cleanupTask = new Runnable() {
            @Override
            public void run() {
                for(Lock lock : locks) {
                    lock.unlock();
                }
                // Remove any types we can.
                actionModel.maybeRemove(requiredTypes);
            }
        };

        try {
            requiredTypes.addAll(finder.getDependencies(object));

            // Lock all the required types.
            for (ATRDecl type : requiredTypes) {
                SimpleTypeName typeName = TypeUtil.getName(type);
                Lock lock = actionModel.getReadLock(typeName);
                locks.add(lock);
                actionModel.add(type);
            }
        } catch(Exception e) {
            cleanupTask.run();
            throw new MediatorsException("Unable to start " + innerAction, e);
        }

        /*
         * Do the learning or idiom recognition now that everything we need is
         * locked against removal.
         */
        innerAction.run(object, requiredTypes, cleanupTask);
    }

    /**
     * Encapsulates the action to be performed after all necessary locks and
     * types have been acquired.
     *
     * @param <R>
     *            the return type of the action
     * @param <T>
     *            the input type of the action
     */
    public abstract static class Action<R, T> {
        /**
         * Run the action, and make sure the cleanup task is called at the end.
         *
         * @param object
         *            the object to perform the action on
         * @param requiredTypes
         *            the types and actions which this one depends upon
         * @param cleanupTask
         *            a task that must get run afterwards, whether the action
         *            was successful or not
         */
        public abstract void run(T object,
                                 List<ATRDecl> requiredTypes,
                                 Runnable cleanupTask);

        /**
         * If the action encountered an error during execution, it will be
         * available via this method.
         *
         * @return an error encountered during {@link #run}, or {@code null}
         */
        public abstract ErrorInfo getError();

        /**
         * Provides the result of running the action on the object
         *
         * @return the return value for this action
         */
        public abstract R result();
    }
}
