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

package com.sri.tasklearning.lapdogController;

/**
 * @author Will Haines
 *
 *         An exception class for {@link TypePublicationFacadeException}.
 */
public class TypePublicationFacadeException extends RuntimeException {

    private static final long serialVersionUID = 6901191693370652531L;

    /**
     * Create new {@link TypePublicationFacadeException}.
     *
     * @param e
     *            the exception causing this one
     */
    public TypePublicationFacadeException(final Exception e) {
        super(e.getMessage(), e);
    }

    /**
     * Create a new {@link TypePublicationFacadeException}.
     *
     * @param message
     *            the reason for the exception
     */
    public TypePublicationFacadeException(final String message) {
        super(message);
    }

    /**
     * Create a new {@link TypePublicationFacadeException}.
     *
     * @param message
     *            the reason for the exception
     * @param e
     *            the exception causing this one
     */
    public TypePublicationFacadeException(final String message, final Exception e) {
        super(message, e);
    }
}
