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

// $Id: SetDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.type.Type;
import com.sri.pal.jaxb.GeneralizeUnsupportedType;
import com.sri.pal.jaxb.MemberType;
import com.sri.pal.jaxb.SetType;
import com.sri.pal.jaxb.TypeType;

/**
 * An unordered collection of zero or more items of the same type. Duplicate
 * items are suppressed.
 */
public class SetDef
        extends CollectionTypeDef {
    protected SetDef(TypeDef elementType,
                     Bridge bridge) {
        super(elementType, bridge);
    }

    protected SetDef(ATRTypeDeclaration atr,
                     TypeDef elementType,
                     Bridge bridge)
            throws PALException {
        super(atr, elementType, bridge);
    }

    @Override
    protected void fillInXml(TypeType typeXml) {
        SetType setXml = new SetType();
        MemberType memberXml = new MemberType();
        memberXml.setTypeRef(getElementType().getName().getFullName());
        setXml.setRef(memberXml);
        String pref = getGeneralizeUnsupportedPreference();
        if (pref != null) {
            GeneralizeUnsupportedType genUnsupp = new GeneralizeUnsupportedType();
            genUnsupp.setPreference(pref);
            Integer maxInputs = getGeneralizeUnsupportedMaxInputs();
            if (maxInputs != null) {
                genUnsupp.setMaxInputs(BigInteger.valueOf(maxInputs));
            }
            setXml.setGeneralizeUnsupported(genUnsupp);
        }
        typeXml.setSet(setXml);
    }

    @Override
    protected Collection<Object> newInstance() {
        return new HashSet<Object>();
    }

    @Override
    @SuppressWarnings("unchecked")
    Set<Object> stringify(Object value) {
        return (Set<Object>) super.stringify(value);
    }

    @Override
    protected String getFunctor() {
        return "setGen";
    }

    @Override
    protected String getCollectionName() {
        return Type.Category.SET.prefix;
    }
}
