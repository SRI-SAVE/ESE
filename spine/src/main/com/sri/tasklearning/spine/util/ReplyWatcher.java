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

// $Id: ReplyWatcher.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.RequestCanceler;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter to store a message with an expected UID for later retrieval. The
 * expected call pattern is:
 *
 * <pre>
 * TransactionUID uid = spine.getNextUid();
 * Message m = new Message(spine.getClientId(), uid);
 * replyWatcher.watchFor(uid);
 * spine.send(m);
 * Message reply = replyWatcher.get(uid);
 * </pre>
 * <p>
 * The ReplyWatcher must be subscribed to the appropriate topic in order to
 * receive the desired messages.
 *
 * @author chris
 *
 * @param <E>
 *            the expected class of reply messages
 */
public class ReplyWatcher<E extends Message>
        implements MessageHandler {
    private final Logger log;
    private final Class<E> type;
    private final Map<TransactionUID, E> watchers;
    private final Map<TransactionUID, CallbackHandler<E>> callbacks;
    private final Spine spine;
    private ExecutorService threadPool;

    /**
     * Builds a new watcher which will handle a particular Java class of reply
     * message.
     *
     * @param messageType
     *            the class of reply messages to handle
     */
    public ReplyWatcher(Class<E> messageType,
                        Spine spine) {
        // Logger name of the form: com.sri.pal.ReplyWatcher<TypeQuery>
        log = LoggerFactory.getLogger(getClass().getName() + "<"
                + messageType.getSimpleName() + ">");
        type = messageType;
        watchers = new HashMap<TransactionUID, E>();
        callbacks = new HashMap<TransactionUID, CallbackHandler<E>>();
        this.spine = spine;
    }

    @Override
    public synchronized void handleMessage(Message message)
            throws MessageHandlerException {
        if (!type.isInstance(message)) {
            log.warn("Unexpected message ({}): {}", message.getClass(), message);
            return;
        }
        if (message.getSender().equals(spine.getClientId())) {
            log.debug("Ignoring message from self: {}", message);
            return;
        }
        @SuppressWarnings("unchecked")
        E msg = (E) message;
        TransactionUID uid = (TransactionUID) msg.getUid();
        if (watchers.containsKey(uid)) {
            log.debug("Got {} -- keeping it", msg);
            watchers.put(uid, msg);
            notifyAll();
        } else if (callbacks.containsKey(uid)) {
            log.debug("Got {} -- sending to callback", msg);
            startCallback(uid, msg);
        } else {
            log.debug("Got {} -- but I don't care", msg);
        }
    }

    /**
     * Tells the watcher to watch for a reply message with a particular UID. By
     * default, all received messages are simply discarded. This method must be
     * called to retain any message.
     *
     * @param uid
     *            the uid of the reply message to retain
     */
    public synchronized void watchFor(TransactionUID uid) {
        watchers.put(uid, null);
        notifyAll();
    }

    /**
     * Block until a desired message is received, then return it. If the message
     * has already been received when this method is called, it will immediately
     * return. Multiple calls to this method can be outstanding at the same
     * time, provided they are waiting for different UIDs. When this method
     * returns, it removes the entry for the requested message, so a subsequent
     * call will fail.
     *
     * @param uid
     *            the UID of the desired reply message
     * @return a message with the requested UID
     * @throws IllegalStateException
     *             if the {@link #watchFor} method was never called for this
     *             UID, or if {@link #stopWatching} is called while this call is
     *             pending
     */
    public synchronized E get(TransactionUID uid)
            throws IllegalStateException {
        while (watchers.get(uid) == null) {
            if (!watchers.containsKey(uid)) {
                throw new IllegalStateException("No entry for " + uid);
            }
            try {
                wait(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            if (!spine.isMasterRunning()) {
                throw new IllegalStateException("Spine has shut down");
            }
        }
        E result = watchers.remove(uid);
        log.debug("Got result {}", result);
        return result;
    }

    /**
     * Removes a particular UID from the watch list. If a call to {@link #get}
     * is pending, that call will immediately throw an exception.
     *
     * @param uid
     *            the UID to stop watching for
     */
    public synchronized void stopWatching(TransactionUID uid) {
        watchers.remove(uid);
        callbacks.remove(uid);
        notifyAll();
    }

    public E sendAndGetReply(Message msg)
            throws SpineException {
        TransactionUID uid = (TransactionUID) msg.getUid();
        watchFor(uid);
        try {
            if (!spine.send(msg)) {
                throw new SpineException("No subscribers");
            }
        } catch (SpineException e) {
            log.warn("sendAndGetReply failed", e);
            stopWatching(uid);
        }
        return get(uid);
    }

    public RequestCanceler sendAndGetReply(CallbackHandler<E> callbackHandler,
                                           Message msg)
            throws SpineException {
        final TransactionUID uid = (TransactionUID) msg.getUid();
        callbacks.put(uid, callbackHandler);
        try {
            // TODO Check the return value from send().
            spine.send(msg);
        } catch (SpineException e) {
            stopWatching(uid);
            throw e;
        }

        return new RequestCanceler() {
            @Override
            public void cancel() {
                stopWatching(uid);
            }
        };
    }

    private void startCallback(TransactionUID uid,
                               E message) {
        // Start the thread pool if necessary.
        if (threadPool == null) {
            String name = getClass().getSimpleName() + "<"
                    + type.getSimpleName() + ">";
            log.debug("Starting thread pool for " + name);
            ThreadFactory tf = new NamedThreadFactory(name);
            threadPool = Executors.newCachedThreadPool(tf);
        }

        // Send the callback job to the thread pool.
        CallbackJob callbackJob = new CallbackJob(uid, message);
        threadPool.execute(callbackJob);
    }

    private class CallbackJob
            implements Runnable {
        private final TransactionUID uid;
        private final E response;

        public CallbackJob(TransactionUID uid,
                           E message) {
            this.uid = uid;
            response = message;
        }

        @Override
        public void run() {
            CallbackHandler<E> handler = callbacks.remove(uid);
            if (handler == null) {
                log.warn("A null handler was registered for {}", uid);
                return;
            }

            try {
                handler.result(response);
            } catch (Exception e) {
                log.warn("Callback handler for " + uid + " threw exception", e);
            }
        }
    }

    public void shutdown() {
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }
}
