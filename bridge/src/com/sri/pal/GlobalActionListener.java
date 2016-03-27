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

// $Id: GlobalActionListener.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

/**
 * Receives notification of all actions and procedures that occur in the system.
 * An application should implement this class if it is interested in monitoring
 * overall PAL system activity. The implementing class should then be registered
 * using {@link Bridge#addActionListener}.
 *
 * @author chris
 */
public interface GlobalActionListener {
    /**
     * Notifies that an action or procedure has begun executing. Depending on
     * the action's speed, this notification may be received after it has
     * completed.
     *
     * @param action
     *            the recently started action
     */
    public void actionStarted(ActionStreamEvent action);
}
