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

// $Id: EnumeratedTypeDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRConstructor;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.Enumerated;
import com.sri.ai.lumen.atr.term.ATRList;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.jaxb.EnumType;
import com.sri.pal.jaxb.TypeType;

/**
 * An enumerated type.
 */
public class EnumeratedTypeDef
        extends TypeDef {

    EnumeratedTypeDef(Enumerated atrDecl,
                      Bridge bridge) {
        super(atrDecl, bridge);
    }

    EnumeratedTypeDef(ATRTypeDeclaration alias,
                      Enumerated concrete,
                      Bridge bridge) {
        super(alias, concrete, bridge);
    }

    @Override
    public SimpleTypeName getName() {
        return (SimpleTypeName) super.getName();
    }

    @Override
    public Enumerated getConcreteAtr() {
        return (Enumerated) super.getConcreteAtr();
    }

    @Override
    protected Set<ActionModelDef> getRequiredDefs() {
        return Collections.emptySet();
    }

    @Override
    protected void fillInXml(TypeType typeXml) {
        EnumType enumXml = new EnumType();
        List<String> values = enumXml.getValue();
        SortedSet<String> sortedValues = new TreeSet<String>();
        sortedValues.addAll(getValues());
        values.addAll(sortedValues);
        typeXml.setEnum(enumXml);
    }

    /**
     * Provides all of the acceptable values for this enumeration.
     *
     * @return a set of enumeration values
     */
    public Set<String> getValues() {
        Set<String> result = new HashSet<String>();
        ATRList atrValues = getConcreteAtr().getValues();
        for (ATRTerm term : atrValues.getTerms()) {
            ATRLiteral lit = (ATRLiteral) term;
            result.add(lit.getString());
        }
        return result;
    }

    @Override
    Object stringify(Object value) {
        /* Values are all strings. Force that to be true with a cast. */
        return (String) value;
    }

    @Override
    int getStringSize(Object strValue) {
        if (strValue == null) {
            return 0;
        } else {
            return strValue.toString().length();
        }
    }

    @Override
    Object unstringify(Object strValue) {
        try {
            return (String) strValue;
        } catch (ClassCastException e) {
            throw new RuntimeException("" + strValue, e);
        }
    }

    @Override
    public Object fromAtr(ATRTerm atrValue) {
        if (!(atrValue instanceof ATRLiteral)) {
            throw new RuntimeException("Unexpected type " + atrValue.getClass()
                    + " of " + ATRSyntax.toSource(atrValue));
        }
        ATRLiteral lit = (ATRLiteral) atrValue;
        return lit.getString();
    }

    @Override
    public ATRTerm nonNullToAtr(
            ATRConstructor<ATR, ?, ATRTerm, ?, ?, ?, ?, ?, ?, ?, ?> constructor,
            Object value) {
        if (!(value instanceof String)) {
            throw new RuntimeException("Unexpected type " + value.getClass() +
                    " provided as a value to enum type " + this.toString());
        }

        return constructor.createLiteral(value, getName().getFullName());
    }
}
