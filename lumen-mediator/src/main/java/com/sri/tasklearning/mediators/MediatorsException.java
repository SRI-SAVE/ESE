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

// $Id: MediatorsException.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.mediators;

/**
 * Indicates that something went wrong with a {@link TypeAdder}.
 */
public class MediatorsException
        extends Exception {
    private static final long serialVersionUID = 1L;

    public MediatorsException() {
        super();
    }

    public MediatorsException(String message,
                          Throwable cause) {
        super(message, cause);
    }

    public MediatorsException(String message) {
        super(message);
    }

    public MediatorsException(Throwable cause) {
        super(cause);
    }
}
