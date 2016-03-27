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

// $Id: DependencyFinder.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.mediators;

import java.util.List;

import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.tasklearning.spine.SpineException;

/**
 * Finds (and loads) the things that a given thing depends on. This is context
 * dependent, so multiple implementations exist. For example, Lumen is
 * interested in a different set of dependencies for procedure execution than
 * for constraint coalescing.
 *
 * @param <T>
 *            the type of thing to search for dependencies of
 */
public interface DependencyFinder<T> {
    /**
     * Retrieve dependencies of the given thing.
     *
     * @param object
     *            the thing to retrieve dependencies of
     * @return everything required for a particular operation to succeed on
     *         {@code object}
     * @throws SpineException
     *             if a required object cannot be retrieved
     */
    public List<ATRDecl> getDependencies(T object)
            throws SpineException;
}
