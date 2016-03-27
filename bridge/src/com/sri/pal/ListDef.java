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

// $Id: ListDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.type.Type;
import com.sri.pal.jaxb.GeneralizeSingletonType;
import com.sri.pal.jaxb.GeneralizeUnsupportedType;
import com.sri.pal.jaxb.ListType;
import com.sri.pal.jaxb.MemberType;
import com.sri.pal.jaxb.TypeType;
import com.sri.tasklearning.spine.util.TypeUtil;

/**
 * An ordered collection of zero or more items of the same type. Duplicate items
 * are allowed. The behavior of this data structure during learning can be
 * altered via the {@code permutable} flag.
 */
public class ListDef
        extends CollectionTypeDef {
    public static final String PERMUTABLE = TypeUtil.PERMUTABLE;

    protected ListDef(TypeDef elementType,
                      Bridge bridge) {
        super(elementType, bridge);
    }

    protected ListDef(ATRTypeDeclaration atr,
                      TypeDef elementType,
                      Bridge bridge)
            throws PALException {
        super(atr, elementType, bridge);
    }

    @Override
    protected void fillInXml(TypeType typeXml) {
        ListType listXml = new ListType();
        MemberType memberXml = new MemberType();
        memberXml.setTypeRef(getElementType().getName().getFullName());
        listXml.setRef(memberXml);
        listXml.setPermutable(isPermutable());
        String pref = getGeneralizeUnsupportedPreference();
        if (pref != null) {
            GeneralizeUnsupportedType genUnsupp = new GeneralizeUnsupportedType();
            genUnsupp.setPreference(pref);
            Integer maxInputs = getGeneralizeUnsupportedMaxInputs();
            if (maxInputs != null) {
                genUnsupp.setMaxInputs(BigInteger.valueOf(maxInputs));
            }
            listXml.setGeneralizeUnsupported(genUnsupp);
        }
        String method = getGeneralizeSingletonMethod();
        if (method != null) {
            GeneralizeSingletonType genSingl = new GeneralizeSingletonType();
            genSingl.setMethod(method);
            listXml.setGeneralizeSingleton(genSingl);
        }
        typeXml.setList(listXml);
    }

    /**
     * Indicates if this list is permutable. In general, lists should not be
     * permutable. A permutable list is an ordered collection which is treated
     * as unordered by the learning components of the system for purposes of
     * determining if a loop has been demonstrated.
     *
     * @return {@code true} if this list definition is a permutable list
     */
    public boolean isPermutable() {
        String flagStr = getMetadata(PERMUTABLE);
        if (flagStr == null) {
            return false;
        }
        Boolean value = Boolean.valueOf(flagStr);
        return value;
    }

    /**
     * Indicates how to generalize the construction of a singleton from a
     * compound type. Should we take the first element, take the last element,
     * or require a size of 1 and take the only element?
     *
     * @return one of the strings {@code first}, {@code last}, or {@code only},
     *         or {@code null}
     */
    public String getGeneralizeSingletonMethod() {
        String result = null;
        ATRTypeDeclaration atr = getAtr();
        if (atr != null) {
            ATRMap props = atr.optProperties();
            if (props != null) {
                ATRLiteral lit = (ATRLiteral) props
                        .get(TypeUtil.GENERALIZE_SINGLETON);
                if (lit != null) {
                    result = lit.getString();
                }
            }
        }
        return result;
    }

    @Override
    protected Collection<Object> newInstance() {
        return new ArrayList<Object>();
    }

    @Override
    @SuppressWarnings("unchecked")
    List<Object> stringify(Object value) {
        return (List<Object>) super.stringify(value);
    }

    @Override
    protected String getFunctor() {
        return "listGen";
    }

    @Override
    protected String getCollectionName() {
        return Type.Category.LIST.prefix;
    }
}
