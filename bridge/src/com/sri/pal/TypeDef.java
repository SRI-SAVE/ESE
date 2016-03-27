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

// $Id: TypeDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRConstructor;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.term.ATRList;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRNull;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameExpr;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.pal.jaxb.MetadataType;
import com.sri.pal.jaxb.ObjectFactory;
import com.sri.pal.jaxb.RequireType;
import com.sri.pal.jaxb.TypeType;
import com.sri.tasklearning.spine.util.TypeUtil;

/**
 * The superclass of all type definition objects.
 */
public abstract class TypeDef
        extends ActionModelDef {
    private static final Logger log = LoggerFactory.getLogger(TypeDef.class);

    private String cachedXml = null;
    private final ATRTypeDeclaration concreteAtr;

    protected TypeDef(ATRTypeDeclaration atrDecl,
                      Bridge bridge) {
        super(atrDecl, bridge);
        concreteAtr = null;
        checkMetadata();
    }

    /**
     * alias is a type alias declaration which refers to concrete. We want to
     * take our actual type definition from concrete, but metadata from alias.
     */
    protected TypeDef(ATRTypeDeclaration alias,
                      ATRTypeDeclaration concrete,
                      Bridge bridge) {
        super(alias, bridge);
        concreteAtr = concrete;
        checkMetadata();
    }

    /**
     * Verify that metadata values are all legal: They should be non-null
     * strings.
     */
    private void checkMetadata() {
        for (String key : listMetadataKeys()) {
            String val = getMetadata(key);
            if (val == null) {
                throw new IllegalArgumentException("Metadata value for " + key
                        + " of " + getName() + " is null");
            }
        }
    }

    @Override
    public ATRTypeDeclaration getAtr() {
        return (ATRTypeDeclaration) super.getAtr();
    }

    protected ATRTypeDeclaration getConcreteAtr() {
        if (concreteAtr != null) {
            return concreteAtr;
        } else {
            return getAtr();
        }
    }

    @Override
    public TypeName getName() {
        ATRLiteral atrName = getAtr().getTypeName();
        String strName = atrName.getString();
        TypeName name = TypeNameFactory.makeName(strName);
        return name;
    }

    @Override
    public String getXml()
            throws PALException {
        if (cachedXml != null) {
            return cachedXml;
        }

        TypeName tn = getName();
        if (!(tn instanceof SimpleTypeName)) {
            /*
             * If it's not represented by a SimpleTypeName, then it doesn't have
             * an XML representation.
             */
            return null;
        }
        SimpleTypeName name = (SimpleTypeName) tn;

        /* Build the <actionModel> element */
        ActionModelType amXml = new ActionModelType();
        amXml.setVersion(name.getVersion());
        List<RequireType> requiresXml = amXml.getRequire();
        for (ActionModelDef amDef : getRequiredDefs()) {
            TypeName reqName = amDef.getName();
            if (reqName instanceof TypeNameExpr
                    || TypeNameFactory.isPrimitive(reqName)) {
                /* Filter out list<myType> and predefined primitives. */
                continue;
            }
            RequireType requireXml = new RequireType();
            requireXml.setName(reqName.getFullName());
            requiresXml.add(requireXml);
        }

        /* Build the <type> element inside the <actionModel>. */
        TypeType typeXml = new TypeType();
        amXml.getType().add(typeXml);
        typeXml.setId(name.getSimpleName());
        typeXml.setDescription(getMetadata(DESCRIPTION));
        List<MetadataType> metadata = typeXml.getMetadata();
        for (String key : listMetadataKeys()) {
            if (!TypeUtil.TYPE_RESERVED_KEYS.contains(key)) {
                String value = getMetadata(key);
                MetadataType metaItem = new MetadataType();
                metaItem.setKey(key);
                metaItem.setValue(value);
                metadata.add(metaItem);
            }
        }
        List<String> equivTypes = typeXml.getEquivalentTo();
        for (TypeName eqvName : getEquivalentTypeNames()) {
            equivTypes.add(eqvName.getFullName());
        }

        fillInXml(typeXml);

        ObjectFactory objectFactory = new ObjectFactory();
        JAXBElement<ActionModelType> jaxb = objectFactory
                .createActionModel(amXml);
        try {
            cachedXml = marshal(jaxb);
            return cachedXml;
        } catch (JAXBException e) {
            throw new PALException(
                    "Unable to marshal " + jaxb + " for " + this, e);
        }
    }

    protected abstract void fillInXml(TypeType typeXml);

    protected Set<ActionModelDef> getRequiredDefs()
            throws PALException {
        Set<ActionModelDef> result = new HashSet<ActionModelDef>();
        for (TypeName equivName : getEquivalentTypeNames()) {
            ActionModelDef equivDef = getActionModel().getType(equivName);
            if (equivDef == null) {
                log.debug("Can't retrieve {} which {} is equivalent to",
                        equivName, getName());
            } else if(!result.contains(equivDef)) {
                result.add(equivDef);
            }
        }
        return result;
    }

    /**
     * Computes the names of equivalent types to this one. Does not recursively
     * get transitive equivalencies.
     *
     * @return the names of directly equivalent types
     */
    protected Set<? extends TypeName> getEquivalentTypeNames() {
        ATRTypeDeclaration atr = getAtr();
        if (atr == null) {
            return Collections.emptySet();
        }
        ATRList equivAtr = atr.optEquivalentTypes();
        if (equivAtr == null) {
            return Collections.emptySet();
        }
        Set<TypeName> result = new HashSet<TypeName>();
        for (ATRTerm term : equivAtr.getTerms()) {
            ATRLiteral lit = (ATRLiteral) term;
            TypeName name = TypeNameFactory.makeName(lit.getString());
            result.add(name);
        }
        return result;
    }

    /**
     * Indicates whether two TypeDef instances are equivalent to each other.
     * Note that being equivalent and being equal are two separate concepts
     * and that this method only looks for equivalence, not equality.
     *
     * @param other The TypeDef to check equivalence against
     * @return true if other is equivalent to this TypeDef
     */
    public boolean isEquivalentTo(final TypeDef other) {
        Set<? extends TypeName> myNames = getEquivalentTypeNames();

        if (myNames.contains(other.getName()))
            return true;

        Set<? extends TypeName> otherNames = other.getEquivalentTypeNames();
        if (otherNames.contains(getName()))
            return true;

        return false;
    }

    /**
     * Indicates if one type is assignable to another. In other words, can a
     * value of type other be assigned to variable or parameter of this type?
     * @param other the TypeDef to check assignability against
     * @return true if values of type other are assignable to this type
     */
    public boolean isAssignableTo(final TypeDef other) {
        if (equals(other) || isEquivalentTo(other))
            return true;

        if (this instanceof CustomTypeDef) {
            CustomTypeDef aCust = (CustomTypeDef)this;
            CustomTypeDef parent = aCust;
            while ((parent = parent.getParentType()) != null) {
                if (parent.equals(other) || parent.isEquivalentTo(other))
                    return true;
            }
        }

        return false;
    }

    @Override
    public String getMetadata(String key) {
        ATRTypeDeclaration atr = getAtr();
        if (atr == null) {
            return null;
        }
        ATRMap props = atr.optProperties();
        if (props == null) {
            return null;
        }
        ATRTerm value = props.get(key);
        if (value == null || value instanceof ATRNull) {
            return null;
        } else if (value instanceof ATRLiteral) {
            return ((ATRLiteral) value).getString();
        } else {
            throw new IllegalArgumentException("Metadata value for " + key
                    + " of " + getName() + " is " + value.getClass()
                    + " not string");
        }
    }

    @Override
    public Set<String> listMetadataKeys() {
        ATRTypeDeclaration atr = getAtr();
        if (atr == null) {
            return Collections.emptySet();
        }
        ATRMap props = atr.optProperties();
        if (props == null) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<String>();
        /* Filter out reserved keys. */
        for (String key : props.getMap().keySet()) {
            if (TypeUtil.TYPE_RESERVED_KEYS.contains(key)) {
                continue;
            }
            result.add(key);
        }
        /* ...but add description back in. */
        result.add(TypeUtil.DESCRIPTION);
        return result;
    }

    abstract Object stringify(Object value);

    /**
     * Assuming the given object is a stringified instance of this type, what is
     * its total string length?
     *
     * @param strValue
     *            a stringified object of this type, possibly null
     * @return the sum of the lengths of the strings making up the object
     */
    abstract int getStringSize(Object strValue);

    abstract Object unstringify(Object strValue);

    /**
     * Although this method is exposed publicly, it is intended for internal
     * SRI use only.
     * @throws PALException
     */
    public abstract Object fromAtr(ATRTerm atrValue) throws PALException;

    /**
     * This API is intended for internal use only!
     *
     * @param value the bridge data value to be converted to ATRTerm
     * @return the ATR representation of the value
     */
    public ATRTerm toAtr(final Object value) {
        return toAtr(new CTRConstructor(), value);
    }

    /**
     * This API is intended for internal use only!
     *
     * @param constructor An ATR Factory
     * @param value the bridge data value to be converted to ATRTerm
     * @return the ATR representation of the value
     */
    public ATRTerm toAtr(
            ATRConstructor<ATR, ?, ATRTerm, ?, ?, ?, ?, ?, ?, ?, ?> constructor,
            Object value) {
        // TODO: Ultimately this check for null should only exist in
        // NullableDef, in which case toAtr becomes abstract and nonNullToAtr
        // implementations are refactored to be concrete toAtr implementations.
        // We aren't doing this currently because we aren't enforcing
        // non-null types yet.
        if (value == null)
            return constructor.createNull();

        return nonNullToAtr(constructor, value);
    }

    abstract ATRTerm nonNullToAtr(
            ATRConstructor<ATR, ?, ATRTerm, ?, ?, ?, ?, ?, ?, ?, ?> constructor,
            Object value);
}
