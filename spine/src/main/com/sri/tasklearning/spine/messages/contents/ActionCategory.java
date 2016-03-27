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

// $Id: ActionCategory.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages.contents;

/**
 * These are the different effects types that an action can have, as far as
 * LAPDOG is concerned.
 */
public enum ActionCategory {
    /**
     * action may change the state of the application. Added to demonstration by
     * application.
     */
    EFFECTOR,
    /**
     * Execution does not change the state of the application. May be inserted
     * in demonstration by the application or by task learning. Removable if its
     * outputs provide no supports. Inclusion in the action model may make
     * learning slower.
     */
    COMPLETER,
    /**
     * Execution does not change the state of the application. Maybe inserted in
     * demonstration by application, but not during learning. Removable if its
     * outputs provide no supports.
     */
    CONTEXT,
    /**
     * Like {@link #COMPLETER}, but may be inserted by application or during
     * learning.
     */
    SUPPORTER,
    /**
     * Execution does not change the state of the application. Used by the
     * automated assessment module to ask the application to evaluate
     * domain-specific expressions.
     */
    QUERY;

    public String getName() {
        return name().toLowerCase();
    }

    public static ActionCategory getValueOf(String string) {
        return valueOf(string.toUpperCase());
    }
}
