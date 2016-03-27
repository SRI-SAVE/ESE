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

// $Id: ProcedureExecutor.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.RequestCanceler;

/**
 * Facility which can load and execute procedures of a particular type.
 * Procedures must be loaded by one of the {@link #load} methods.
 *
 * @author chris
 */
public interface ProcedureExecutor
        extends ActionExecutor {
    /**
     * Synchronously loads a new procedure from the given source string. The
     * procedure will be registered into the action model as part of this
     * process.
     *
     * @param source
     *            a string containing the textual representation of the script
     *            or program to load
     * @return the corresponding procedure definition
     * @throws PALException
     *             if an error occurs
     */
    public ProcedureDef load(String source)
            throws PALException;

    /**
     * Asynchronously loads a new procedure from the given source string. The
     * procedure will be registered into the action model as part of this
     * process.
     *
     * @param callbackHandler
     *            the callback handler which will receive the procedure
     *            definition once it has been loaded
     * @param source
     *            a string containing the textual representation of the script
     *            or program to load
     * @return an object which may be used to cancel this operation
     */
    public RequestCanceler load(CallbackHandler<ProcedureDef> callbackHandler,
                                String source);

    /*
     * TODO It would be nice to add getNamespace() and getVersion() here, but
     * that would cause a lot of trouble.
     */
}
