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

// $Id: CustomTypeDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRConstructor;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.Application;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.ApplicationSubType;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRNull;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.CustomType;
import com.sri.pal.jaxb.InheritType;
import com.sri.pal.jaxb.TypeType;
import com.sri.tasklearning.spine.util.TypeUtil;

/**
 * A custom, application-defined type. The type must have a representation class
 * with an unambiguous string representation of its non-null values. An instance
 * of this class also requires a {@link CustomTypeFactory} which can construct
 * instances of the class from their string representation and produce the
 * string representation from instances. {@link ToStringFactory} is provided,
 * which will work for many representation classes.
 */
public class CustomTypeDef
        extends TypeDef {
    private final CustomTypeDef parentType;
    private final CustomTypeFactory factory;

    /**
     * Builds a custom type definition object from its ATR description.
     *
     * @param atrDecl
     *            the ATR declaration which this object will wrap
     * @param bridge
     *            the Bridge instance this definition belongs to
     * @throws PALException
     *             if a required type cannot be found
     */
    public CustomTypeDef(ATRTypeDeclaration atrDecl,
                         Bridge bridge)
            throws PALException {
        super(atrDecl, bridge);
        Application app = getConcreteAtr();
        SimpleTypeName concreteName = TypeUtil.getName(app);
        if (TypeUtil.isCustomSubType(app)) {
            ApplicationSubType sub = (ApplicationSubType) app;
            String parentNameStr = sub.getParentType().getString();
            TypeName parentName = TypeNameFactory.makeName(parentNameStr);
            ActionModelDef def = getActionModel().getType(parentName);
            if (!(def instanceof CustomTypeDef)) {
                throw new RuntimeException("Parent type " + def.getName()
                        + " of " + getName() + " is " + def.getClass()
                        + " and must be " + getClass());
            }
            parentType = (CustomTypeDef) def;
            factory = parentType.factory;
        } else {
            parentType = null;
            factory = getActionModel().getCustomTypeFactory(concreteName);
        }
    }

    CustomTypeDef(ATRTypeDeclaration atr,
                  ATRTypeDeclaration concrete,
                  Bridge bridge)
            throws PALException {
        super(atr, concrete, bridge);
        Application app = getConcreteAtr();
        SimpleTypeName concreteName = TypeUtil.getName(app);
        if (TypeUtil.isCustomSubType(app)) {
            ApplicationSubType sub = (ApplicationSubType) app;
            String parentNameStr = sub.getParentType().getString();
            TypeName parentName = TypeNameFactory.makeName(parentNameStr);
            ActionModelDef def = getActionModel().getType(parentName);
            if (!(def instanceof CustomTypeDef)) {
                throw new RuntimeException("Parent type " + def.getName()
                        + " of " + getName() + " is " + def.getClass()
                        + " and must be " + getClass());
            }
            parentType = (CustomTypeDef) def;
            factory = parentType.factory;
        } else {
            parentType = null;
            factory = getActionModel().getCustomTypeFactory(concreteName);
        }
    }

    @Override
    public final Application getConcreteAtr() {
        return (Application) super.getConcreteAtr();
    }

    @Override
    public final SimpleTypeName getName() {
        return (SimpleTypeName) super.getName();
    }

    @Override
    protected Set<ActionModelDef> getRequiredDefs()
            throws PALException {
        Set<ActionModelDef> result = super.getRequiredDefs();
        CustomTypeDef parent = getParentType();
        if (parent != null) {
            result.add(parent);
            result.addAll(parent.getRequiredDefs());
        }
        return result;
    }

    /**
     * @return The parent type of this custom application type if one exists
     */
    public CustomTypeDef getParentType() {
        return parentType;
    }

    @Override
    protected void fillInXml(TypeType typeXml) {
        CustomType customXml = new CustomType();
        CustomTypeDef parent = getParentType();
        if (parent != null) {
            SimpleTypeName parentName = parent.getName();
            SimpleTypeName myName = getName();
            String nameStr;
            if (myName.getNamespace().equals(parentName.getNamespace())
                    && myName.getVersion().equals(parentName.getVersion())) {
                nameStr = parentName.getSimpleName();
            } else {
                nameStr = parentName.getFullName();
            }
            InheritType inheritXml = new InheritType();
            inheritXml.setParent(nameStr);
            customXml.setInherit(inheritXml);
        } else {
            customXml.setJavaType(getJavaClassName());
        }
        typeXml.setCustom(customXml);
    }

    /**
     * Gets the name of the Java class which is used as a representation class
     * by this custom type. Note that the named class may not be accessible on
     * this JVM's classpath, if the type was defined in another JVM. In such
     * cases, all instances of this type will be represented by strings.
     *
     * @return the name of this type's Java representation class
     */
    public String getJavaClassName() {
        CustomTypeDef parent = getParentType();
        if (parent != null) {
            return parent.getJavaClassName();
        }
        ATRLiteral lit = getConcreteAtr().getRepresentationType();
        return lit.getString();
    }

    /**
     * Gets the Java class used as to represent values of this type.
     *
     * @return the Java representation class of this type
     * @throws ClassNotFoundException
     *             if the requested class cannot be found on this JVM's
     *             classpath. This can be a common occurrence.
     * @see #getJavaClassName
     */
    public Class<?> getJavaClass()
            throws ClassNotFoundException {
        String className = getJavaClassName();
        Class<?> clazz = Class.forName(className);
        return clazz;
    }

    @Override
    Object stringify(Object value) {
        if (factory == null) {
            return value;
        } else {
            return factory.makeString(value);
        }
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
        if (factory == null) {
            return strValue;
        } else {
            return factory.makeObject((String) strValue);
        }
    }

    @Override
    public Object fromAtr(ATRTerm atrValue) {
        Object result;
        if (atrValue instanceof ATRLiteral) {
            ATRLiteral literal = (ATRLiteral) atrValue;
            result = unstringify(literal.getValue());
        } else if (atrValue instanceof ATRNull) {
            result = null;
        } else {
            throw new IllegalArgumentException("Don't know how to decode "
                    + atrValue.getClass() + ": " + atrValue);
        }
        return result;
    }

    @Override
    ATRTerm nonNullToAtr(
            ATRConstructor<ATR, ?, ATRTerm, ?, ?, ?, ?, ?, ?, ?, ?> constructor,
            Object value) {
        return constructor.createLiteral(stringify(value), getName().getFullName());
    }
}
