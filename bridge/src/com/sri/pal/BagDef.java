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

// $Id: BagDef.java 7750 2016-07-26 16:53:01Z Chris Jones (E24486) $
package com.sri.pal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.type.Type;
import com.sri.pal.jaxb.BagType;
import com.sri.pal.jaxb.GeneralizeUnsupportedType;
import com.sri.pal.jaxb.MemberType;
import com.sri.pal.jaxb.TypeType;

/**
 * An unordered collection of zero or more items of the same type. Duplicate
 * items are allowed.
 */
public class BagDef
        extends CollectionTypeDef {
    protected BagDef(TypeDef elementType,
                     Bridge bridge) {
        super(elementType, bridge);
    }

    protected BagDef(ATRTypeDeclaration atr,
                     TypeDef elementType,
                     Bridge bridge)
            throws PALException {
        super(atr, elementType, bridge);
    }

    @Override
    protected void fillInXml(TypeType typeXml) {
        BagType bagXml = new BagType();
        MemberType memberXml = new MemberType();
        memberXml.setTypeRef(getElementType().getName().getFullName());
        bagXml.setRef(memberXml);
        String pref = getGeneralizeUnsupportedPreference();
        if (pref != null) {
            GeneralizeUnsupportedType genUnsupp = new GeneralizeUnsupportedType();
            genUnsupp.setPreference(pref);
            Integer maxInputs = getGeneralizeUnsupportedMaxInputs();
            if (maxInputs != null) {
                genUnsupp.setMaxInputs(BigInteger.valueOf(maxInputs));
            }
            bagXml.setGeneralizeUnsupported(genUnsupp);
        }
        typeXml.setBag(bagXml);
    }

    @Override
    protected Collection<Object> newInstance() {
        return new ArrayList<Object>();
    }

    @Override
    public boolean isValueOf(Object value)
            throws PALException {
        if (!(value instanceof Collection)) {
            return false;
        }
        Collection<?> coll = (Collection<?>) value;
        TypeDef eleType = getElementType();
        for (Object member : coll) {
            if (!eleType.isValueOf(member)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected String getFunctor() {
        return "bagGen";
    }

    @Override
    protected String getCollectionName() {
        return Type.Category.BAG.prefix;
    }
}
