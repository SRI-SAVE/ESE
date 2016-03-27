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

// $Id: ActionInvocationStatusListener.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import com.sri.pal.common.ErrorInfo;

/**
 * Listener which receives status information for a particular action
 * invocation. This listener can be registered via
 * {@link ActionInvocation#addListener}.
 *
 * @author chris
 */
public interface ActionInvocationStatusListener {
    /**
     * Notifies that an error has occurred during the execution of the action.
     * This will be immediately followed by
     * <code>newStatus(Status.FAILED)</code>, which will be the last call made
     * to this listener on behalf of the given <code>ActionInvocation</code>.
     *
     * @param error
     */
    public void error(ErrorInfo error);

    /**
     * Notifies that the status of the action invocation has changed.
     *
     * @param newStatus
     *            the recent status of the action invocation
     */
    public void newStatus(ActionInvocation.Status newStatus);
}
