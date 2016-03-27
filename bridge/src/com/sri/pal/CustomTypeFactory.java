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

// $Id: CustomTypeFactory.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

/**
 * Implementations of this interface are used by {@link CustomTypeDef} to
 * convert between instances of a representation type and their corresponding
 * string representations.
 */
public interface CustomTypeFactory {
    /**
     * Convert from an instance of the custom type to its string representation.
     *
     * @param value
     *            an instance of the representation class, or {@code null}
     * @return a string unambiguously representing this given value, or
     *         {@code null}
     */
    String makeString(Object value);

    /**
     * Convert from a string representation of the custom type to its object
     * representation.
     *
     * @param strValue
     *            a string representing a value of the corresponding type, or
     *            {@code null}
     * @return an object of the class returned by
     *         {@link CustomTypeDef#getJavaClass}, or {@code null}
     */
    Object makeObject(String strValue);
}
