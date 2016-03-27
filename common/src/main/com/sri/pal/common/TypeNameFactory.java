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

// $Id: TypeNameFactory.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.common;

import java.util.HashSet;
import java.util.Set;

// TODO Write unit tests for this class.
/**
 * Creates {@link TypeName} instances of the appropriate class.
 */
public class TypeNameFactory {
    private static final Set<String> predefs;
    static {
        predefs = new HashSet<String>();
        predefs.add("integer");
        predefs.add("real");
        predefs.add("string");
        predefs.add("boolean");
        predefs.add("timestamp");
        predefs.add("duration");
    }

    /**
     * Equivalent to calling {@link #makeName(String, String, String)} with
     * {@code null} for the second and third parameters.
     *
     * @param str
     *            the string to parse
     * @return a new type name object
     */
    public static TypeName makeName(String str) {
        return makeName(str, null, null);
    }

    /**
     * Use the provided version and namespace for context only if necessary --
     * if name is fully qualified, just use it.
     *
     * @param name
     * @param version
     * @param namespace
     * @return
     */
    /**
     * Builds a new type name object by parsing the given strings. If
     * {@code name} is fully qualified, it will be parsed and used without
     * reference to {@code version} and {@code namespace}. {@code name} may be a
     * string like any of the following: {@code string}, {@code MyType},
     * {@code myApp^1.3^MyType}, {@code list<MyType>},
     * {@code list<myApp^1.3^MyType>}.
     *
     * @param name
     *            the string to parse into a new name object
     * @param version
     *            the namespace version to use if one is required, or
     *            {@code null}
     * @param namespace
     *            the namespace to use if one is required, or {@code null}
     * @return a new name object unambiguously identifying a type or action
     */
    public static TypeName makeName(String name,
                                    String version,
                                    String namespace) {
        if (predefs.contains(name)) {
            return new SimpleTypeName(name, null, null);
        } else if (name.matches(".+<.+>")) {
            int pos = name.indexOf('<');
            String outer = name.substring(0, pos);
            String innerStr = name.substring(pos + 1, name.length() - 1);
            TypeName inner = makeName(innerStr, version, namespace);
            return new TypeNameExpr(outer, inner);
        } else {
            String newName;
            String newVersion;
            String newNamespace;
            int pos = name.lastIndexOf(SimpleTypeName.PACKAGE_NAME_SEPARATOR);
            if (pos == -1) {
                newName = name;
                newVersion = version;
                newNamespace = namespace;
            } else {
                newName = name.substring(pos
                        + SimpleTypeName.PACKAGE_NAME_SEPARATOR.length());
                String remainder = name.substring(0, pos);
                pos = remainder
                        .lastIndexOf(SimpleTypeName.PACKAGE_NAME_SEPARATOR);
                if (pos == -1) {
                    newVersion = version;
                    newNamespace = remainder;
                } else {
                    newVersion = remainder.substring(pos
                            + SimpleTypeName.PACKAGE_NAME_SEPARATOR.length());
                    newNamespace = remainder.substring(0, pos);
                }
            }
            if (newName.length() == 0) {
                throw new IllegalArgumentException("Name cannot be empty");
            }
            return new SimpleTypeName(newName, newVersion, newNamespace);
        }
    }

    /**
     * Indicates if the given name identifies a predefined primitive type.
     *
     * @param name
     *            the name to check
     * @return {@code true} if the provided name belongs to one of the
     *         predefined primitive types; {@code false} otherwise
     */
    public static boolean isPrimitive(TypeName name) {
        return predefs.contains(name.getFullName());
    }
}
