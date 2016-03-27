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

// $Id: PALRemoteException.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

/**
 * Indicates a communication error between PAL components. The most likely cause
 * of this error is that one of the PAL components has shut down.
 *
 * @author chris
 */
public class PALRemoteException
        extends PALException {
    private static final long serialVersionUID = 1L;

    public PALRemoteException() {
        super();
    }

    public PALRemoteException(String message,
                              Throwable cause) {
        super(message, cause);
    }

    public PALRemoteException(String message) {
        super(message);
    }

    public PALRemoteException(Throwable cause) {
        super(cause);
    }
}
