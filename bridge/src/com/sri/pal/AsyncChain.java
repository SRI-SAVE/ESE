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

// $Id: AsyncChain.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.RequestCanceler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class which allows one asynchronous call to be chained onto the
 * results of another.
 *
 * @author chris
 *
 * @param <T>
 *            the class of results expected for this callback handler
 * @param <T1>
 *            the class of results which this callback handler will send to the
 *            next callback handler in the chain
 */
abstract class AsyncChain<T, T1>
        implements CallbackHandler<T>, RequestCanceler {
    private static final Logger log = LoggerFactory.getLogger(AsyncChain.class);

    protected final CallbackHandler<T1> subCH;
    private final Set<RequestCanceler> cancelers;
    private int numResults = 0;
    private int numErrors = 0;

    public AsyncChain(CallbackHandler<T1> subCallbackHandler) {
        log.debug("Chain {} result to {}", this, subCallbackHandler);
        cancelers = new CopyOnWriteArraySet<RequestCanceler>();
        subCH = subCallbackHandler;
    }

    @Override
    public void error(ErrorInfo error) {
        numErrors++;
        subCH.error(error);
    }

    @Override
    public final void result(T result) {
        numResults++;
        results(result);
    }

    public abstract void results(T result);

    @Override
    public void cancel() {
        for (RequestCanceler rc : cancelers) {
            rc.cancel();
        }
    }

    /**
     * Adds a canceler which will be called if this object ever receives a
     * cancel request.
     *
     * @param rc
     *            the subsidiary canceler to call
     * @see RequestCanceler#cancel
     */
    public void addCanceler(RequestCanceler rc) {
        cancelers.add(rc);
    }

    public int activityCount() {
        return numResults + numErrors;
    }
}
