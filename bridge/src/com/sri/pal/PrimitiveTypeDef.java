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
package com.sri.pal;

import java.util.Collections;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRConstructor;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRNull;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.common.ICalDateTime;
import com.sri.pal.common.ICalDuration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.TypeType;

/**
 * Definition of a primitive, predefined type.
 */
public class PrimitiveTypeDef
        extends TypeDef {
    public static PrimitiveTypeDef getPrimitive(Predefined predef,
                                                Bridge bridge) {
        return new PrimitiveTypeDef(predef, bridge);
    }

    private final Predefined predef;
    private final ToStringFactory stringer;

    private PrimitiveTypeDef(Predefined predef,
                             Bridge bridge) {
        super(null, bridge);
        this.predef = predef;
        stringer = new ToStringFactory(predef.getRepresentationClass()
                .getCanonicalName());
    }

    PrimitiveTypeDef(ATRTypeDeclaration atr,
                     Predefined predef,
                     Bridge bridge)
            throws PALException {
        super(atr, null, bridge);
        this.predef = predef;
        stringer = new ToStringFactory(predef.getRepresentationClass()
                .getCanonicalName());
    }

    @Override
    public ATRTypeDeclaration getConcreteAtr() {
        /*
         * There is no such thing as an ATR declaration of a primitive type.
         * Something like ATRLiteral("string") is a reference to a predefined
         * primitive type named string.
         */
        return null;
    }

    @Override
    public String getAtrStr() {
        return predef.getName();
    }

    @Override
    public String getXml()
            throws PALException {
        if (getAtr() == null) {
            /* There is no XML representation of the predefined types. */
            return null;
        } else {
            return super.getXml();
        }
    }

    @Override
    protected void fillInXml(TypeType typeXml) {
    }

    @Override
    public SimpleTypeName getName() {
        if (getAtr() == null) {
            return (SimpleTypeName) TypeNameFactory.makeName(predef.getName(),
                    null, null);
        } else {
            return (SimpleTypeName) super.getName();
        }
    }

    /**
     * Returns the kind of primitive this declaration represents. The primitive
     * types are all predefined.
     *
     * @return this declaration's kind
     */
    public Predefined getKind() {
        return predef;
    }

    @Override
    public String getMetadata(String key) {
        if (getAtr() == null) {
            return null;
        } else {
            return super.getMetadata(key);
        }
    }

    @Override
    public Set<String> listMetadataKeys() {
        if (getAtr() == null) {
            return Collections.emptySet();
        } else {
            return super.listMetadataKeys();
        }
    }

    @Override
    protected Set<ActionModelDef> getRequiredDefs()
            throws PALException {
        if (getAtr() == null) {
            return Collections.emptySet();
        } else {
            return super.getRequiredDefs();
        }
    }

    @Override
    Object stringify(Object value) {
        return value;
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
        return strValue;
    }

    @Override
    public Object fromAtr(ATRTerm atrValue) {
        if (atrValue instanceof ATRNull) {
            return null;
        }
        if (!(atrValue instanceof ATRLiteral)) {
            throw new RuntimeException("Unexpected type " + atrValue.getClass()
                    + " of " + ATRSyntax.toSource(atrValue));
        }
        ATRLiteral lit = (ATRLiteral) atrValue;

        Object value = lit.getValue();

        // Sometimes the ATR for a boolean looks like a string.
        Class<?> clazz = predef.getRepresentationClass();
        if (value instanceof String && !clazz.equals(String.class)) {
            value = stringer.makeObject(value.toString());
        }
        return value;
    }

    @Override
    ATRTerm nonNullToAtr(
            ATRConstructor<ATR, ?, ATRTerm, ?, ?, ?, ?, ?, ?, ?, ?> constructor,
            Object value) {
        /*
         * Values of primitives are encoded without any explicit semantic type
         * information.
         */
        return constructor.createLiteral(value, null);
    }

    public enum Predefined {
        INTEGER(Long.class), REAL(Double.class), STRING(String.class), BOOLEAN(
                Boolean.class), TIMESTAMP(ICalDateTime.class), DURATION(
                ICalDuration.class);

        private final Class<?> clazz;

        private Predefined(Class<?> clazz) {
            this.clazz = clazz;
        }

        private String getName() {
            return name().toLowerCase();
        }

        /**
         * Provides the Java class used to represent objects of this type.
         *
         * @return this type's Java representation class
         */
        public Class<?> getRepresentationClass() {
            return clazz;
        }
    }
}
