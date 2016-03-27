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

// $Id: TypeName.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.common;

import java.io.Serializable;

/**
 * Identifies a type or action, providing type safety and separating the type's
 * name from its namespace.
 *
 * @author chris
 */
public interface TypeName
        extends Serializable {
    /**
     * The full name of the type including namespace, separator, and local name.
     * If the type is a constructed collection type such as
     * {@code list<integer>} then that type expression string will be
     * returned. This is the name by which the type is known internally to the
     * task learning system.
     *
     * @return the full name of the type
     */
    public String getFullName();
}
