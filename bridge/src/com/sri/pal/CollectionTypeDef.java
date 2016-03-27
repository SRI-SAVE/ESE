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

// $Id: CollectionTypeDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRConstructor;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.term.ATRFunction;
import com.sri.ai.lumen.atr.term.ATRList;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameExpr;
import com.sri.tasklearning.spine.util.TypeUtil;

/**
 * Superclass of collection types. A collection type contains other objects, all
 * of which are the same type (or subclasses).
 */
public abstract class CollectionTypeDef
        extends TypeDef {
    private final TypeDef elementType;

    protected CollectionTypeDef(TypeDef elementType,
                                Bridge bridge) {
        super(null, bridge);
        this.elementType = elementType;
        if (elementType == null) {
            throw new IllegalArgumentException("Null element type not allowed");
        }
    }

    protected CollectionTypeDef(ATRTypeDeclaration atr,
                                TypeDef elementType,
                                Bridge bridge)
            throws PALException {
        super(atr, bridge);
        this.elementType = elementType;
    }

    @Override
    public TypeName getName() {
        if (getAtr() != null) {
            return super.getName();
        } else {
            return new TypeNameExpr(getCollectionName(), elementType.getName());
        }
    }

    @Override
    protected Set<ActionModelDef> getRequiredDefs()
            throws PALException {
        Set<ActionModelDef> result = super.getRequiredDefs();
        result.add(getElementType());
        result.addAll(getElementType().getRequiredDefs());
        return result;
    }

    @Override
    public String getAtrStr() {
        if (getAtr() == null) {
            return getName().getFullName();
        } else {
            return super.getAtrStr();
        }
    }

    /**
     * Returns the type of the element which this collection contains.
     *
     * @return the element type of this collection
     */
    public TypeDef getElementType() {
        return elementType;
    }

    /**
     * Indicates how to generalize the construction of this compound type.
     * Should it be taken as one intact argument to the procedure
     * (parameterize)? Should it be constructed from its component elements
     * (construct)?
     *
     * @return one of the strings {@code parameterize} or {@code construct}, or
     *         {@code null}
     */
    public String getGeneralizeUnsupportedPreference() {
        return TypeUtil.getGeneralizeUnsupportedPreference(getAtr());
    }

    /**
     * If this type prefers to be constructed (see
     * {@link #getGeneralizeUnsupportedPreference}), how many inputs should be
     * considered for construction?
     *
     * @return the maximum number of input to use to construct new instances of
     *         this type
     */
    public Integer getGeneralizeUnsupportedMaxInputs() {
        return TypeUtil.getGeneralizeUnsupportedMaxInputs(getAtr());
    }

    protected abstract Collection<Object> newInstance();

    @Override
    Object stringify(Object value) {
        if (value == null) {
            return null;
        }
        Collection<?> coll = (Collection<?>) value;
        Collection<Object> result = newInstance();
        for (Object item : coll) {
            result.add(elementType.stringify(item));
        }

        return result;
    }

    @Override
    int getStringSize(Object strValue) {
        int sum = 0;
        if (strValue != null) {
            Collection<?> coll = (Collection<?>) strValue;
            for (Object member : coll) {
                sum += elementType.getStringSize(member);
            }
        }
        return sum;
    }

    @Override
    Object unstringify(Object strValue) {
        if (strValue == null) {
            return null;
        }
        Collection<?> coll;
        try {
            coll = (Collection<?>) strValue;
        } catch (ClassCastException e) {
            throw new RuntimeException("" + strValue, e);
        }
        Collection<Object> result = newInstance();
        for (Object member : coll) {
            result.add(elementType.unstringify(member));
        }
        return result;
    }

    @Override
    public Object fromAtr(ATRTerm atrValue)
            throws PALException {
        Collection<Object> result = newInstance();
        TypeDef type = getElementType();
        if (atrValue instanceof ATRFunction) {
            ATRFunction func = (ATRFunction) atrValue;
            String functor = func.getFunctor();
            if (!getFunctor().equals(functor)) {
                throw new IllegalArgumentException("Expected " + getFunctor()
                        + "; got " + functor);
            }
            for (ATRTerm atrTerm : func.getElements()) {
                Object member = type.fromAtr(atrTerm);
                result.add(member);
            }
        } else if (atrValue instanceof ATRList) {
            ATRList coll = (ATRList) atrValue;
            for (ATRTerm lapdogMember : coll.getTerms()) {
                Object member = type.fromAtr(lapdogMember);
                result.add(member);
            }
        } else {
            throw new IllegalArgumentException("Object is not an ATRList: "
                    + atrValue);
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    ATRTerm nonNullToAtr(
            ATRConstructor<ATR, ?, ATRTerm, ?, ?, ?, ?, ?, ?, ?, ?> constructor,
            Object value) {
        Collection dummy = newInstance();

        if (!dummy.getClass().isInstance(value)) {
            throw new RuntimeException("Unexpected type " + value.getClass()
                    + " where " + dummy.getClass() + " was expected");
        }

        TypeDef elementType = getElementType();
        List<ATRTerm> atrElements = new ArrayList<ATRTerm>();

        for (Object elementValue : ((Collection)value))
            atrElements.add(elementType.toAtr(constructor, elementValue));

        ATRTerm term;

        if (this instanceof ListDef)
            term = constructor.createList(atrElements);
        else
            term = constructor.createFunction(getFunctor(), atrElements);

        return term;
    }

    protected abstract String getFunctor();

    protected abstract String getCollectionName();
}
