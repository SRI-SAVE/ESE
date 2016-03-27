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

// $Id: TypeStorage.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Set;

import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.RequestCanceler;
import com.sri.pal.common.SimpleTypeName;

/**
 * This interface is implemented by a client which wishes to offer mutable
 * storage of types, actions, and procedures. It allows another PAL system
 * component to make requests to add, modify, or remove items from the action
 * model.
 * <p>
 * Use {@link Bridge#setTypeStorage} to register this object with the Bridge.
 * <p>
 * Note that due to a current API limitation, if the Bridge containing the
 * active {@code TypeStorage} instance exits, the remaining task learning
 * processes will be unable to retrieve any types. This will leave the system in
 * an unusable state. Thus, it's important to pay attention to the order in
 * which applications are started: The application which is intended to provide
 * persistence via this interface should start first, and it should be the last
 * to shut down.
 */
public interface TypeStorage {
    /**
     * Requests that the type with the given name be loaded asynchronously. The
     * caller expects a string containing the XML representation of the type, as
     * returned by {@link TypeDef#getXml}.
     *
     * @param callbackHandler
     *            a callback handler to which results will be sent. If the type
     *            loader determines it cannot load the action, it should call
     *            the callback handler with a null argument.
     * @param name
     *            the name of the requested type
     * @return an object which can be used to cancel this load request if it is
     *         no longer needed
     */
    public RequestCanceler loadType(CallbackHandler<String> callbackHandler,
                                    SimpleTypeName name);

    /**
     * (optional) Provides a listing of types, actions, and procedures held by
     * this library. This method only needs to be implemented if any of the
     * following are true:
     * <ul>
     * <li>The (JavaFX) Procedure Editor is in use.
     * <li>The application calls {@link ActionModel#getTypes}.
     * <li>Idioms are in use in the action model (in which case only listing of
     * idioms needs to be supported).
     * </ul>
     *
     * @param type
     *            the subset of the action model to retrieve. If {@code null} or
     *            not specified, all subsets will be retrieved.
     * @return the names of all stored objects in the requested subset(s)
     * @throws PALException
     *             if an error occurs
     */
    public Set<SimpleTypeName> listTypes(Subset... type)
            throws PALException;

    /**
     * Requests that a change be made to the stored types. If the specified
     * object already exists, it should be overwritten.
     *
     * @param name
     *            the name of the type to store or remove
     * @param typeString
     *            the type definition to associate with the given name, or
     *            {@code null} to remove it. This string can be retrieved using
     *            {@link ActionModelDef#getXml}.
     * @throws PALException
     *             if an error occurs in processing the request.
     */
    public void putType(SimpleTypeName name,
                        String typeString)
            throws PALException;

    /**
     * Specifies a subset of the known types.
     */
    // WARNING: The names of these need to be the same as TypeListQuery.Subset.
    public enum Subset {
        /**
         * Refers to non-action types.
         */
        TYPE,
        /**
         * Refers to actions which are not procedures.
         */
        ACTION,
        /**
         * Refers to procedures.
         */
        PROCEDURE,
        /**
         * Refers to constraint declarations.
         */
        CONSTRAINT,
        /**
         * Refers to action families.
         */
        FAMILY,
        /**
         * Refers to idiom definitions.
         */
        IDIOM
    }
}
