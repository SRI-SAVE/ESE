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

// $Id: EnumeratedTypeDef.java 7750 2016-07-26 16:53:01Z Chris Jones (E24486) $
package com.sri.pal;

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
import com.sri.pal.jaxb.SubTypeType;
import com.sri.pal.jaxb.TypeType;
import com.sri.tasklearning.spine.util.TypeUtil;

import java.util.*;

/**
 * An enumerated type.
 */
public class EnumeratedTypeDef
        extends TypeDef {
    private final Set<EnumeratedTypeDef> subTypes;

    EnumeratedTypeDef(Enumerated atrDecl,
                      Bridge bridge)
            throws PALException {
        super(atrDecl, bridge);

        Enumerated enumDecl = getConcreteAtr();
        subTypes = new HashSet<>();
        for (SimpleTypeName subName : TypeUtil.getSubTypes(enumDecl)) {
            ActionModelDef subDef = getActionModel().getType(subName);
            if (subDef == null) {
                throw new PALException("Unable to find subtype " + subName.getFullName() + " of type " +
                        getName().getFullName());
            }
            if (!(subDef instanceof EnumeratedTypeDef)) {
                throw new PALException(getName().getFullName() + "'s subtype " + subName.getFullName() + " is of type " +
                        subDef.getClass() + " and must be of type " + getClass());
            }
            subTypes.add((EnumeratedTypeDef) subDef);
        }
    }

    EnumeratedTypeDef(ATRTypeDeclaration alias,
                      Enumerated concrete,
                      Bridge bridge)
            throws PALException {
        super(alias, concrete, bridge);

        Enumerated enumDecl = getConcreteAtr();
        subTypes = new HashSet<>();
        for (SimpleTypeName subName : TypeUtil.getSubTypes(enumDecl)) {
            ActionModelDef subDef = getActionModel().getType(subName);
            if (subDef == null) {
                throw new PALException("Unable to find subtype " + subName.getFullName() + " of type " +
                        getName().getFullName());
            }
            if (!(subDef instanceof EnumeratedTypeDef)) {
                throw new PALException(getName().getFullName() + "'s subtype " + subName.getFullName() + " is of type " +
                        subDef.getClass() + " and must be of type " + getClass());
            }
            subTypes.add((EnumeratedTypeDef) subDef);
        }
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

        for (EnumeratedTypeDef sub : subTypes) {
            SubTypeType subXml = new SubTypeType();
            subXml.setSub(sub.getName().getFullName());
            enumXml.getSubType().add(subXml);
        }

        List<String> values = enumXml.getValue();
        SortedSet<String> sortedValues = new TreeSet<String>();
        sortedValues.addAll(getValues());
        values.addAll(sortedValues);
        typeXml.setEnum(enumXml);
    }

    /**
     * Provides access to the sub-types of this enumerated type.
     * @return all of the sub-types of this type
     */
    public Set<EnumeratedTypeDef> getSubTypes() {
        return Collections.unmodifiableSet(subTypes);
    }

    @Override
    public boolean isAssignableTo(TypeDef other) {
        if (super.isAssignableTo(other)) {
            return true;
        }
        if (!(other instanceof EnumeratedTypeDef)) {
            return false;
        }
        EnumeratedTypeDef otherEnum = (EnumeratedTypeDef) other;
        for (EnumeratedTypeDef subType : otherEnum.subTypes) {
            if (isAssignableTo(subType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provides the acceptable values for this enumeration, as directly defined in this type. Values defined in
     * sub-types are not included.
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

    /**
     * Provides all of the acceptable values for this enumeration, including those defined in sub-types.
     * @return a set of enumeration values
     */
    public Set<String> getAllValues() {
        Set<String> result = new HashSet<>();
        result.addAll(getValues());

        for (EnumeratedTypeDef sub : subTypes) {
            result.addAll(sub.getAllValues());
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
    public boolean isValueOf(Object value) {
        return value instanceof String && getAllValues().contains(value);
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
