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

// $Id: PALSizeException.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

/**
 * Indicates that an object or parameter exceeds the size restrictions for the
 * PAL system. Most likely, a parameter was passed in which is larger than
 * specified by {@link Bridge#setMaxStringSize}.
 *
 * @author chris
 */
public class PALSizeException
        extends PALException {
    private static final long serialVersionUID = 1L;

    public PALSizeException() {
        super();
    }

    public PALSizeException(String message,
                            Throwable cause) {
        super(message, cause);
    }

    public PALSizeException(String message) {
        super(message);
    }

    public PALSizeException(Throwable cause) {
        super(cause);
    }
}
