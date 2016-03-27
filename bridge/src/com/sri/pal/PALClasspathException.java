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

// $Id: PALClasspathException.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

/**
 * This exception indicates that the classpath doesn't contain a necessary
 * entry. Typically, the underlying representation type for a {@link TypeDef}
 * couldn't be loaded. This may indicate a bug in the PAL system, as instances
 * can be encoded as strings in many cases.
 *
 * @author chris
 */
public class PALClasspathException
        extends PALException {
    private static final long serialVersionUID = 1L;

    public PALClasspathException() {
        super();
    }

    public PALClasspathException(String message,
                                 Throwable cause) {
        super(message, cause);
    }

    public PALClasspathException(String message) {
        super(message);
    }

    public PALClasspathException(Throwable cause) {
        super(cause);
    }
}
