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
package com.sri.pal.training.aa;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionModel;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.Bridge;
import com.sri.pal.PALException;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.contents.ActionCategory;

/**
 * Glue code to call an application and ask it to evaluate a predicate. In this
 * context, predicate means an action which doesn't change the application's
 * state and which can help us evaluate some expression in the application's
 * domain. Examples: "Is this geo coord in the U.S.?" or
 * "What color is this object?"
 */
public class QueryDispatcher {
    private final Bridge bridge;
    private final ActionModel actionModel;
    private final QueryCache cache;

    /**
     * Creates a new predicate evaluator.
     *
     * @param bridge
     *            will be used to communicate with other parts of the system --
     *            notably, the application which evaluates these predicates
     */
    public QueryDispatcher(Bridge bridge) {
        this.bridge = bridge;
        actionModel = this.bridge.getActionModel();
        cache = new QueryCache();
    }

    /**
     * Evaluate the named predicate action in the appropriate application.
     *
     * @param predName
     *            the name of the predicate action to invoke
     * @param args
     *            one argument for every input parameter of the predicate action
     * @return the value of the first output parameter of the predicate action
     * @throws PALException
     *             if an error occurs executing the action
     * @throws IllegalArgumentException
     *             the number of arguments is not equal to the number of input
     *             parameters of the named predicate, or the requested action is
     *             not a predicate
     * @throws InterruptedException
     *             if the thread is interrupted while waiting for another thread
     *             to perform the query
     */
    public Object evaluate(SimpleTypeName predName,
                           Object... args)
            throws PALException,
            IllegalArgumentException, InterruptedException {
        /* If it's in the cache, just return that. */
        Object result = cache.getOrLock(predName, args);
        if (result != null) {
            return result;
        }

        /* Get the action definition. */
        ActionDef ad = (ActionDef) actionModel.getType(predName);
        ActionCategory category = ad.getCategory();
        if (category != ActionCategory.QUERY) {
            throw new IllegalArgumentException("Not a "
                    + ActionCategory.QUERY.name() + ": " + predName);
        }

        /* Create an action invocation. */
        ActionInvocation invoc = ad.invoke(null);

        /* Check and assign args. */
        if (ad.numInputParams() != args.length) {
            throw new IllegalArgumentException(
                    "Wrong number of arguments for " + predName + ": Expected "
                            + ad.numInputParams() + ", got " + args.length);
        }
        for(int i = 0; i < args.length; i++) {
            invoc.setValue(i, args[i]);
        }

        /* Start the invocation and wait for it to finish. */
        invoc.start();
        invoc.waitUntilFinished();

        /* Was it successful? */
        if (invoc.getStatus() != Status.ENDED) {
            throw new PALException("Predicate " + predName + " failed: "
                    + invoc.getError());
        }

        result = invoc.getValue(ad.numInputParams());
        cache.add(predName, args, result);
        return result;
    }
}
