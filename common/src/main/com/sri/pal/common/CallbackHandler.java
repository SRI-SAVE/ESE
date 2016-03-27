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

// $Id: CallbackHandler.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.common;

/**
 * Provides notification of the results of asynchronous method calls. The caller
 * passes in an instance of this class, which the callee will later notify as
 * errors or results are discovered.
 *
 * @author chris
 *
 * @param <T>
 *            the class of the results expected
 */
public interface CallbackHandler<T> {
    /**
     * Notifies the callback handler of a result from the computation.
     *
     * @param result
     *            the newly discovered result
     */
    public void result(T result);

    /**
     * Notifies the callback handler that an error was encountered.
     *
     * @param error
     *            the error which was encountered
     */
    public void error(ErrorInfo error);
}
