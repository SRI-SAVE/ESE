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

// $Id: ParamClass.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages.contents;

/**
 * These are the different classes of action parameters available. The class of
 * a parameter affects how the learning system generalizes actions using this
 * parameter. Each class is only applicable to either input or output
 * parameters.
 */
public enum ParamClass {
    /**
     * Default input class. Always generalized to variable or expression by task
     * learning. Like values are codesignated with the same variable. An
     * unsupported value is made a parameter; the default value is the
     * demonstrated value.
     */
    GENERALIZABLE(true),
    /**
     * Never generalized by task learning.
     */
    CONSTANT(true),
    /**
     * Always generalized to a distinct parameter, even if supportable another
     * way.
     */
    DISTINCT(true),
    /**
     * Supported values are treated like {@code generalizable}. Unsupported
     * values are replaced with {@code null}. during generalization regardless
     * of the value demonstrated. Default is specified on action parameter, not
     * on type.
     */
    LOCAL(true),
    /**
     * Default output class. Will be made a procedure output parameter.
     */
    EXTERNAL(false),
    /**
     * Not made a procedure output parameter. {@link EffectsType#completer} and
     * {@link EffectsType#context} action outputs should probably always be this
     * class.
     */
    INTERNAL(false),
    /**
     * If value supports any other value, treat as internal; otherwise, treat as
     * external.
     */
    INTERMEDIATE(false);

    private final boolean input;

    private ParamClass(boolean isInput) {
        input = isInput;
    }

    public boolean isInput() {
        return input;
    }

    public String getName() {
        return name().toLowerCase();
    }

    public static ParamClass getValueOf(String string) {
        return valueOf(string.toUpperCase());
    }
}
