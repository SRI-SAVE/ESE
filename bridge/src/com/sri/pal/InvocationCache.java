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

// $Id: InvocationCache.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.common.ErrorInfo;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.messages.contents.UID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache of known running invocations, indexed by UID. The UIDs are used by the
 * Spine to uniquely identify invocations. As we receive updates for
 * {@link ActionInvocation} objects, we need to find those objects and/or their
 * parents in order to apply the updates. This is necessary because incoming
 * messages are handled by thread pools, so race conditions are possible where,
 * for example, a called invocation might be processed before its caller. This
 * class provides a point of synchronization for those situations.
 */
class InvocationCache {
    private static final Logger log = LoggerFactory
            .getLogger(InvocationCache.class);

    /**
     * For a given UID, what's the corresponding invocation?
     */
    private final Map<TransactionUID, ActionStreamEvent> map;
    /**
     * This map is used to coordinate the adding of an Invocation when Lumen
     * sends an StartExecutionStatus message and any subsequent subtasks that
     * have this uid as its parent. A problem arises if the invocation has not
     * been added for the parent before an execution request arrives for its
     * child as the child will want to get a handle to its parents invocation.
     * When a StartExecutionStatus message arrives in the ITLExecutionListener,
     * it adds the uid of that task to this watcher list by calling watchFor
     * below. When the invocation for that new task is added, the uid will be
     * removed from the watchers list. If, before then, a task execution request
     * comes in from Lumen with this uid set as its parent it will consult this
     * watchers object and if the uid is present it will wait until it is
     * notified that the invocation has been fully added.
     */
    private final Map<TransactionUID, ActionStreamEvent> watchers;

    /**
     * Builds the cache. The cache will always retain every live invocation,
     * plus a specified number of recently dead invocations.
     *
     * @param size the number of dead invocations to retain
     */
    InvocationCache() {
        map = new HashMap<TransactionUID, ActionStreamEvent>();
        watchers = new HashMap<TransactionUID, ActionStreamEvent>();
    }

    /**
     * Get the requested UID from the cache.
     *
     * @param uid
     *            the UID of the event to retrieve
     * @param timeout
     *            time to wait, in ms. 0 means don't wait; just check the cache
     *            and throw the exception if not found
     * @return the requested event, or {@code null} if {@code uid} is
     *         {@code null}
     * @throws TimeoutException
     *             if the requested UID can't be found in the time allotted
     */
    synchronized ActionStreamEvent get(UID uid,
                                       long timeout)
            throws TimeoutException {
        log.debug("Requesting live uid: " + uid);
        if (uid == null) {
            return null;
        }

        boolean waited = false;
        ActionStreamEvent result = null;
        long startTime = System.currentTimeMillis();
        while (result == null) {
            result = map.get(uid);
            if (result == null) {
                log.debug("Nothing (yet) for {}", uid);
                long timeElapsed = System.currentTimeMillis() - startTime;
                long timeRemaining = timeout - timeElapsed;
                if (timeRemaining <= 0) {
                    break;
                }
                waited = true;
                try {
                    wait(timeRemaining);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }

        if(waited) {
            long waitTime = System.currentTimeMillis() - startTime;
            log.warn("Waited {} ms for UID {}, and got {}", new Object[] {
                    waitTime, uid, result });
        }

        if (result == null) {
            throw new TimeoutException("Didn't find UID " + uid + " after "
                    + timeout + " ms");
        }

        return result;
    }

    synchronized void add(ActionStreamEvent invocation) {
        TransactionUID uid = invocation.getUid();
        log.debug("Adding live uid: " + uid);
        map.put(uid, invocation);
        /*
         * Inform any watchers that the invocation they have been waiting on is
         * ready.
         */
        if (watchers.keySet().contains(uid)) {
            watchers.put(uid, invocation);
        }
        Listener listener = new Listener(invocation);
        invocation.addListener(listener);
        notifyAll();
    }

    private synchronized void remove(ActionStreamEvent invocation) {
        TransactionUID uid = invocation.getUid();
        log.debug("Removing dead uid: " + uid);
        map.remove(uid);
        watchers.remove(uid);
    }

    /**
     * This method is called by the ITLExecutionListener when a StartExecutionStatus message
     * arrives. It is used to synchronize with any incoming subtasks that are using this uid
     * as their parent. They will wait until the invocation has been added before continuing.
     * @param uid The uid of the task mentioned in the StartExecutionStatus message
     */
    synchronized void watchFor(TransactionUID uid) {
        // If the InvocationCache has already added this invocation there is no need to
        // make the ActionExecAdapter wait.
        if (map.containsKey(uid)) {
            return;
        }
        if (watchers.size() > 5000) {
            // This condition should never be hit, if it is, we likely have a memory leak
            if (uid.getId() % 1000 == 0) {
                log.warn("Watchers Size: {}", watchers.size());
            }
        }
        watchers.put(uid, null);
    }

    /**
     * This method is called by the ITLExecutionStatusListener when a procedure completes.
     * At that point there is no need to keep the UID in the watchlist as all child
     * procedures must have completed in order for this procedure to complete. Failure to
     * remove the UID from the watch list would result in leaked memory as these objects
     * would never get GC'd
     * @param uid the uid of the invocation that just completed (success, failure or cancel)
     */
    synchronized void endWatch(TransactionUID uid) {
        watchers.remove(uid);
        notifyAll();
    }

    /**
     * This method is called from the ActionExecAdapter to see if the parent invocation
     * is in the process of being prepared. If so, it will wait until it has been added
     * by the add method in this class. If it is not on the list of UIDs being prepared
     * the method will return immediately.
     * @param uid The UID if the parent invocation of the task that is getting the execution request
     */
    synchronized void getParentInvocationReady(TransactionUID uid) {
        while (watchers.get(uid) == null) {
            // Return if there is no need to wait
            if (!watchers.containsKey(uid)) {
                return;
            }

            try {
                log.debug("About to wait on uid: {}", uid);
                wait();
            } catch (InterruptedException e) {
                log.debug("Wait Interrupted");
            }
        }
        log.debug("Invocation Added for Uid: {}", uid);
    }

    /**
     * When an invocation dies, we remove it from the cache.
     */
    private class Listener
            implements ActionInvocationStatusListener {
        private final ActionStreamEvent invoc;

        public Listener(ActionStreamEvent invocation) {
            invoc = invocation;
        }

        @Override
        public void error(ErrorInfo error) {
            // Ignore.
        }

        @Override
        public void newStatus(Status newStatus) {
            if (newStatus == Status.ENDED || newStatus == Status.FAILED) {
                remove(invoc);
            }
        }
    }
}
