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

// $Id: TypeAdder.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.mediators;

import com.sri.ai.lumen.atr.decl.ATRDecl;

/**
 * Responsible for doing the actual work of adding or removing types and actions
 * from LAPDOG or Lumen.
 */
public interface TypeAdder {
    /**
     * Add the given type to the system. The &quot;type&quot; could actually be
     * a type, action, family, idiom, or constraint.
     *
     * @param decl
     *            the thing to add
     * @throws MediatorsException
     *             if an error occurs
     */
    public void add(ATRDecl decl)
            throws MediatorsException;

    /**
     * Attempt to remove the given type from the system. The caller of this
     * method guarantees that the indicated type is not currently in use.
     * However, the implementor is not required to actually remove it. For
     * instance, we never remove types from LAPDOG's action model; only actions.
     *
     * @param decl
     *            the thing to remove
     * @return {@code true} if the type was actually removed
     * @throws MediatorsException
     *             if an error occurs
     */
    public boolean remove(ATRDecl decl)
            throws MediatorsException;
}
