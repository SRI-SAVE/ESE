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

// $Id: NullableDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Collection;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRConstructor;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.term.ATRNull;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.type.Type;
import com.sri.pal.jaxb.MemberType;
import com.sri.pal.jaxb.NullableType;
import com.sri.pal.jaxb.TypeType;

/**
 * An optional item of a known type. The item may exist, or this may contain
 * {@code null}.
 */
public class NullableDef
        extends CollectionTypeDef {
    protected NullableDef(TypeDef elementType,
                          Bridge bridge) {
        super(elementType, bridge);
    }

    protected NullableDef(ATRTypeDeclaration atr,
                          TypeDef elementType,
                          Bridge bridge)
            throws PALException {
        super(atr, elementType, bridge);
    }

    @Override
    protected void fillInXml(TypeType typeXml) {
        NullableType nullableXml = new NullableType();
        MemberType memberXml = new MemberType();
        memberXml.setTypeRef(getElementType().getName().getFullName());
        nullableXml.setRef(memberXml);
        typeXml.setNullable(nullableXml);
    }

    @Override
    protected Collection<Object> newInstance() {
        return null;
    }

    @Override
    Object stringify(Object value) {
        if (value == null) {
            return null;
        } else {
            return getElementType().stringify(value);
        }
    }

    @Override
    int getStringSize(Object strValue) {
        if (strValue == null) {
            return 0;
        } else {
            return getElementType().getStringSize(strValue);
        }
    }

    @Override
    Object unstringify(Object strValue) {
        if (strValue == null) {
            return null;
        } else {
            return getElementType().unstringify(strValue);
        }
    }

    @Override
    public Object fromAtr(ATRTerm atrValue)
            throws PALException {
        if (atrValue == null || atrValue instanceof ATRNull) {
            return null;
        } else {
            return getElementType().fromAtr(atrValue);
        }
    }

    @Override
    ATRTerm nonNullToAtr(
            ATRConstructor<ATR, ?, ATRTerm, ?, ?, ?, ?, ?, ?, ?, ?> constructor,
            Object value) {
        return getElementType().nonNullToAtr(constructor, value);
    }

    @Override
    protected String getFunctor() {
        return null;
    }

    @Override
    protected String getCollectionName() {
        return Type.Category.NULLABLE.prefix;
    }
}
