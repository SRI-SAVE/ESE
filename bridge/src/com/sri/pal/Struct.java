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

// $Id: Struct.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An instance of a structure as defined by {@link StructDef}.
 */
public class Struct {
    private final StructDef structDef;
    private final Object[] values;

    /**
     * Constructs a structure of a given type. All fields will initially be set
     * to {@code null}.
     *
     * @param definition
     *            the type of structure to create
     */
    public Struct(StructDef definition) {
        structDef = definition;
        values = new Object[structDef.size()];
    }

    /**
     * Retrieves the type definition of which this structure is an instance.
     *
     * @return this structure's definition
     */
    public StructDef getDefinition() {
        return structDef;
    }

    /**
     * Retrieves the value at a given field position in this structure. The
     * first field is zero.
     *
     * @param i
     *            the field number to retrieve the value of
     * @return the value at the indicated position, or {@code null}
     */
    public Object getValue(int i) {
        return values[i];
    }

    /**
     * Retrieves a field value by name rather than by number. Names are
     * available from the associated structure type definition.
     *
     * @param fieldName
     *            the name of the field to retrieve
     * @return the value at the requested location
     * @see StructDef#getFieldNum
     * @see #getValue(int)
     */
    public Object getValue(String fieldName) {
        int pos = structDef.getFieldNum(fieldName);
        if (pos == -1) {
            throw new NoSuchElementException("No field " + fieldName
                    + " in struct " + getDefinition().getName());
        }
        return getValue(pos);
    }

    /**
     * Assigns a value to a given field in this structure. Fields are numbered
     * starting with zero.
     *
     * @param i
     *            the field number to assign
     * @param fieldValue
     *            the value to store in the field
     */
    public void setValue(int i,
                         Object fieldValue) {
        values[i] = fieldValue;
    }

    /**
     * Assigns a value to a structure field by field name rather than field
     * number. Field names are available from the associated structure type
     * definition.
     *
     * @param fieldName
     *            the name of the field to retrieve
     * @param value
     *            the value to store in the field
     * @see StructDef#getFieldNum
     * @see #setValue(int, Object)
     */
    public void setValue(String fieldName,
                         Object value) {
        int pos = structDef.getFieldNum(fieldName);
        if (pos == -1) {
            throw new NoSuchElementException("No field " + fieldName
                    + " in struct " + getDefinition().getName());
        }
        setValue(pos, value);
    }

    /**
     * Convenience method which returns a list of structure contents which can
     * be iterated over.
     *
     * @return an untyped list which contains all the values stored in this
     *         structure's fields
     */
    public List<?> contents() {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        int size = getDefinition().size();
        for (int i = 0; i < size; i++) {
            sb.append(getValue(i));
            if (i < size - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((structDef == null) ? 0 : structDef.hashCode());
        result = prime * result + Arrays.hashCode(values);
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
        Struct other = (Struct) obj;
        if (structDef == null) {
            if (other.structDef != null)
                return false;
        } else if (!structDef.equals(other.structDef))
            return false;
        if (!Arrays.equals(values, other.values))
            return false;
        return true;
    }
}
