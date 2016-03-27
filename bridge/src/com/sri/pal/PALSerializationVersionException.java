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

// $Id: PALSerializationVersionException.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

/**
 * This exception indicates that a procedure's source is serialized by an
 * incompatible (probably old) version of the Bridge. Upgrading the procedure
 * will probably be required.
 *
 * @see com.sri.pal.upgrader.ProcedureUpgrader
 */
public class PALSerializationVersionException
        extends PALException {
    private static final long serialVersionUID = 1L;

    public PALSerializationVersionException() {
        super();
    }

    public PALSerializationVersionException(String message,
                                            Throwable cause) {
        super(message, cause);
    }

    public PALSerializationVersionException(String message) {
        super(message);
    }

    public PALSerializationVersionException(Throwable cause) {
        super(cause);
    }
}
