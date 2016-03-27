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

// $Id: SynchronousCallbackHandler.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.List;

import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.util.ErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This adapter class allows us to easily wrap an asynchronous method with
 * synchronous behavior. This can be passed as the callback handler for an async
 * method, then the caller can call {@link #waitForResult} and return its
 * result.
 *
 * @author chris
 *
 * @param <T>
 *            the class of the expected results
 */
class SynchronousCallbackHandler<T>
        implements CallbackHandler<T> {
    private static final Logger log = LoggerFactory
            .getLogger(SynchronousCallbackHandler.class);

    private static int serial = 0;
    private final int mySerial = serial++;

    private ErrorInfo error = null;
    private final List<T> results;

    public SynchronousCallbackHandler() {
        results = new ArrayList<T>();
    }

    @Override
    public synchronized void error(ErrorInfo error) {
        log.debug("{} Got error {}", mySerial, error);
        this.error = error;
        this.notifyAll();
    }

    @Override
    public synchronized void result(T result) {
        log.debug("{} Got result: {}", mySerial, result);
        results.add(result);
        this.notifyAll();
    }

    /**
     * Wait until a result or error is delivered to this callback handler, then
     * return (or throw) it.
     *
     * @return the result which was delivered to this callback handler
     * @throws PALException
     *             which wraps any error delivered to this callback handler's
     *             <code>error</code> method.
     */
    public synchronized T waitForResult()
            throws PALException {
        log.debug("{} Waiting for result", mySerial);
        while (error == null && results.isEmpty()) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        if (!results.isEmpty()) {
            T result = results.get(results.size() - 1);
            log.debug("{} Returning most recent result {}", mySerial, result);
            return result;
        } else {
            log.debug("{} Throwing error", mySerial, error);
            if (error.getErrorId() == ErrorType.NOT_ALL_LOADED.ordinal()) {
                String nameStr = error.getDetailedMessage();
                TypeName name = TypeNameFactory.makeName(nameStr);
                throw new PALActionMissingException(error.toString(), name);
            } else {
                throw new PALException(error.toString());
            }
        }
    }

    /**
     * Wait until {@code size} results have been delivered to this callback
     * handler, or an error. Once that has occurred, either return the list of
     * results of throw the error.
     *
     * @param size
     *            the number of results to wait for
     * @return all of the received results, in order
     * @throws PALException
     *             if an error was signed via this object's
     *             {@link #error(ErrorInfo)} method.
     */
    public synchronized List<T> waitForResults(int size)
            throws PALException {
        while (error == null && results.size() < size) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        if (results.size() < size) {
            log.debug("{} Throwing error", mySerial, error);
            throw new PALException(error.toString());
        } else {
            log.debug("{} Returning {} results", mySerial, results.size());
            return results;
        }
    }

    /**
     * Provides the error that was delivered to this callback handler, or
     * {@code null} if none was delivered.
     *
     * @return the error, or {@code null}
     */
    public synchronized ErrorInfo getError() {
        return error;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + mySerial;
    }
}
