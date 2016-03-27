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

// $Id: TypeNameExpr.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.common;

/**
 * Type name expression of the form {@code set<string>} or
 * {@code list<app^1.1^name>}. Expressions can be nested arbitrarily deeply, as
 * in {@code list<set<bag<integer>>>}.
 */
public class TypeNameExpr
        implements TypeName {
    private static final long serialVersionUID = 1L;
    private final String outer;
    private final TypeName inner;

    /**
     * Creates a new type name representing a type such as {@code list<MyType>}.
     *
     * @param outer
     *            the kind of container, such as {@code list}
     * @param inner
     *            the name of the contained type, such as {@code MyType}
     */
    public TypeNameExpr(String outer,
                        TypeName inner) {
        this.outer = outer;
        this.inner = inner;
    }

    /**
     * Retrieves the outer portion of this type name expression. In the case of
     * {@code list<MyType>}, this method would return {@code list}.
     *
     * @return the kind of the container referenced by this name
     */
    public String getOuter() {
        return outer;
    }

    /**
     * Retrieves the inner portion of this type name expression. In the case of
     * {@code list<MyType>}, this method would return {@code MyType}.
     *
     * @return the element type of the container referenced by this name
     */
    public TypeName getInner() {
        return inner;
    }

    @Override
    public String getFullName() {
        return outer + "<" + inner.getFullName() + ">";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getFullName() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((inner == null) ? 0 : inner.hashCode());
        result = prime * result + ((outer == null) ? 0 : outer.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TypeNameExpr other = (TypeNameExpr) obj;
        if (inner == null) {
            if (other.inner != null)
                return false;
        } else if (!inner.equals(other.inner))
            return false;
        if (outer == null) {
            if (other.outer != null)
                return false;
        } else if (!outer.equals(other.outer))
            return false;
        return true;
    }
}
