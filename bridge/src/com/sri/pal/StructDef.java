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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRConstructor;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.Structure;
import com.sri.ai.lumen.atr.term.ATRFunction;
import com.sri.ai.lumen.atr.term.ATRList;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.GeneralizeUnsupportedType;
import com.sri.pal.jaxb.InheritType;
import com.sri.pal.jaxb.StructMemberType;
import com.sri.pal.jaxb.StructType;
import com.sri.pal.jaxb.TypeType;
import com.sri.tasklearning.spine.util.TypeUtil;

/**
 * Definition of a structure type containing an ordered list of typed, named
 * fields.
 */
public class StructDef
        extends TypeDef {
    public static final String OPAQUE = TypeUtil.OPAQUE;

    private final TypeDef[] fieldTypes;
    private final StructDef parentType;

    protected StructDef(Structure atrDecl,
                        Bridge bridge)
            throws PALException {
        super(atrDecl, bridge);
        ATRLiteral parentNameLit = getConcreteAtr().optParentType();
        if (parentNameLit != null) {
            String parentNameStr = parentNameLit.getString();
            TypeName parentName = TypeNameFactory.makeName(parentNameStr);
            parentType = (StructDef) getActionModel().getType(parentName);
            if (parentType == null) {
                throw new PALException("Couldn't retrieve parent type "
                        + parentName + " of struct " + getName());
            }
        } else {
            parentType = null;
        }

        if (getConcreteAtr().getFieldNames().size() != getConcreteAtr()
                .getFieldTypes().size()) {
            throw new IllegalArgumentException(
                    "Field types and names don't match for " + getName());
        }
        fieldTypes = new TypeDef[mySize()];
        for (int i = 0; i < fieldTypes.length; i++) {
            ATRLiteral fieldTypeTerm = (ATRLiteral) getConcreteAtr()
                    .getFieldTypes().get(i);
            String fieldTypeStr = fieldTypeTerm.getString();
            TypeName fieldTypeName = TypeNameFactory.makeName(fieldTypeStr);
            fieldTypes[i] = (TypeDef) getActionModel().getType(fieldTypeName);
            if (fieldTypes[i] == null) {
                throw new PALException("Couldn't retrieve type "
                        + fieldTypeName + " for field " + getFieldName(i)
                        + " of struct " + getName());
            }
        }

        /* Check for duplicate field names. */
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < size(); i++) {
            String name = getFieldName(i);
            if (names.contains(name)) {
                throw new IllegalArgumentException("Duplicate param '" + name
                        + "' in struct " + getName());
            }
            names.add(name);
        }
    }

    StructDef(ATRTypeDeclaration atr,
              Structure concrete,
              Bridge bridge)
            throws PALException {
        super(atr, concrete, bridge);
        ATRLiteral parentNameLit = getConcreteAtr().optParentType();
        if (parentNameLit != null) {
            String parentNameStr = parentNameLit.getString();
            TypeName parentName = TypeNameFactory.makeName(parentNameStr);
            parentType = (StructDef) getActionModel().getType(parentName);
        } else {
            parentType = null;
        }

        if (getConcreteAtr().getFieldNames().size() != getConcreteAtr()
                .getFieldTypes().size()) {
            throw new IllegalArgumentException(
                    "Field types and names don't match for " + getName());
        }
        fieldTypes = new TypeDef[size()];
        for (int i = 0; i < fieldTypes.length; i++) {
            ATRLiteral fieldTypeTerm = (ATRLiteral) getConcreteAtr()
                    .getFieldTypes().get(i);
            String fieldTypeStr = fieldTypeTerm.getString();
            TypeName fieldTypeName = TypeNameFactory.makeName(fieldTypeStr);
            fieldTypes[i] = (TypeDef) getActionModel().getType(fieldTypeName);
        }
    }

    @Override
    public final SimpleTypeName getName() {
        return (SimpleTypeName) super.getName();
    }

    @Override
    public Structure getConcreteAtr() {
        return (Structure) super.getConcreteAtr();
    }

    @Override
    protected void fillInXml(TypeType typeXml) {
        StructType structXml = new StructType();
        int firstLocalField = 0;
        StructDef parent = getParentType();
        if (parent != null) {
            firstLocalField = parent.size();
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
            structXml.setInherit(inheritXml);
        }
        structXml.setOpaque(isOpaque());
        String pref = getGeneralizeUnsupportedPreference();
        if (pref != null) {
            GeneralizeUnsupportedType genUnsupp = new GeneralizeUnsupportedType();
            genUnsupp.setPreference(pref);
            Integer maxInputs = getGeneralizeUnsupportedMaxInputs();
            if (maxInputs != null) {
                genUnsupp.setMaxInputs(BigInteger.valueOf(maxInputs));
            }
            structXml.setGeneralizeUnsupported(genUnsupp);
        }
        List<StructMemberType> fields = structXml.getRef();
        for (int i = firstLocalField; i < size(); i++) {
            StructMemberType field = new StructMemberType();
            field.setName(getFieldName(i));
            field.setTypeRef(getFieldTypeName(i).getFullName());
            fields.add(field);
            if (isFieldDynamic(i)) {
                field.setDynamic(true);
            }
            if (isFieldConstant(i)) {
                field.setConstant(true);
            }
        }
        typeXml.setStruct(structXml);
    }

    @Override
    protected Set<ActionModelDef> getRequiredDefs()
            throws PALException {
        Set<ActionModelDef> result = super.getRequiredDefs();
        if (parentType != null) {
            result.add(parentType);
            result.addAll(parentType.getRequiredDefs());
        }
        for (ActionModelDef type : fieldTypes) {
            result.add(type);
            result.addAll(type.getRequiredDefs());
        }
        return result;
    }

    private StructDef getParentType() {
        return parentType;
    }

    /**
     * Indicates if this structure definition refers to an opaque data
     * structure. If the structure is opaque, learning will not attempt to pull
     * values out of individual fields in this structure; instead, it will only
     * unify complete instances of the structure with other instances of the
     * structure which have all fields identical.
     *
     * @return {@code true} if this is a definition of an opaque structure
     */
    public boolean isOpaque() {
        String flagStr = getMetadata(OPAQUE);
        if (flagStr == null) {
            return false;
        }
        Boolean value = Boolean.valueOf(flagStr);
        return value;
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

    /**
     * Provides the number of fields in structures of this type.
     *
     * @return the size of this structure definition
     */
    public final int size() {
        int parentSize = 0;
        StructDef parent = getParentType();
        if (parent != null) {
            parentSize = parent.size();
        }
        return parentSize + mySize();
    }

    /**
     * Gets just the size of this structure, not including inherited fields.
     */
    private int mySize() {
        return getConcreteAtr().getFieldNames().size();
    }

    /**
     * Maps from the index of a field to its name. If the structure has no
     * declared field names, one will be generated.
     *
     * @param i
     *            the index of the field (starting with zero)
     * @return the name of the field
     */
    public final String getFieldName(int i) {
        int num = i;
        StructDef parent = getParentType();
        if (parent != null && num < parent.size()) {
            return parent.getFieldName(num);
        }
        if (parent != null) {
            num -= parent.size();
        }
        ATRLiteral fieldLit = (ATRLiteral) getConcreteAtr().getFieldNames()
                .get(num);
        return fieldLit.getString();
    }

    /**
     * Maps from the name of a field to its index.
     *
     * @param fieldName
     *            the name of the field
     * @return the field's index (starting with zero), or -1 if the field does
     *         not exist
     */
    public int getFieldNum(String fieldName) {
        for (int i = 0; i < size(); i++) {
            if (getFieldName(i).equals(fieldName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Retrieves the name of the type of a given field.
     *
     * @param i
     *            the index of the field
     * @return the name of the type of objects stored in the given field
     */
    public TypeName getFieldTypeName(int i) {
        int num = i;
        StructDef parent = getParentType();
        if (parent != null && num < parent.size()) {
            return parent.getFieldTypeName(num);
        }
        if (parent != null) {
            num -= parent.size();
        }
        ATRLiteral fieldTerm = (ATRLiteral) getConcreteAtr().getFieldTypes()
                .get(num);
        String fieldStr = fieldTerm.getString();
        TypeName name = TypeNameFactory.makeName(fieldStr);
        return name;
    }

    /**
     * Retrieves the type of the given field in this structure.
     *
     * @param i
     *            the index of the field (starting with zero)
     * @return the type of objects stored in the given field
     */
    public TypeDef getFieldType(int i) {
        int num = i;
        StructDef parent = getParentType();
        if (parent != null) {
            if (num < parent.size()) {
                return parent.getFieldType(num);
            } else {
                num -= parent.size();
            }
        }
        return fieldTypes[num];
    }

    public boolean isFieldDynamic(int i) {
        StructDef parent = getParentType();
        if (parent != null) {
            if (i < parent.size()) {
                return parent.isFieldDynamic(i);
            }
        }
        String name = getFieldName(i);
        ATRMap props = getAtr().optProperties();
        boolean result = false;
        ATRList dynFields = null;
        if (props != null) {
            dynFields = (ATRList) props.get(TypeUtil.STRUCT_DYNAMIC);
        }
        if (dynFields != null) {
            for (ATRTerm term : dynFields.getTerms()) {
                ATRLiteral lit = (ATRLiteral) term;
                if (name.equals(lit.getString())) {
                    result = true;
                }
            }
        }
        return result;
    }

    public boolean isFieldConstant(int i) {
        StructDef parent = getParentType();
        if (parent != null) {
            if (i < parent.size()) {
                return parent.isFieldConstant(i);
            }
        }
        String name = getFieldName(i);
        ATRMap props = getAtr().optProperties();
        boolean result = false;
        ATRList constFields = null;
        if (props != null) {
            constFields = (ATRList) props.get(TypeUtil.STRUCT_CONSTANT);
        }
        if (constFields != null) {
            for (ATRTerm term : constFields.getTerms()) {
                ATRLiteral lit = (ATRLiteral) term;
                if (name.equals(lit.getString())) {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Creates an empty structure of this type. All fields will initially be set
     * to {@code null}.
     *
     * @return a new structure
     */
    public Struct newInstance() {
        return new Struct(this);
    }

    @Override
    List<?> stringify(Object value) {
        if (value == null) {
            return null;
        }
        Struct struct = (Struct) value;
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < size(); i++) {
            TypeDef fieldType = getFieldType(i);
            Object fieldValue = struct.getValue(i);
            Object strFieldValue = fieldType.stringify(fieldValue);
            result.add(strFieldValue);
        }
        return result;
    }

    @Override
    int getStringSize(Object strValue) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) strValue;
        int sum = 0;
        if (strValue != null) {
            for (int i = 0; i < size(); i++) {
                TypeDef fieldType = getFieldType(i);
                Object strFieldValue = list.get(i);
                sum += fieldType.getStringSize(strFieldValue);
            }
        }
        return sum;
    }

    @Override
    Struct unstringify(Object strValue) {
        if (strValue == null) {
            return null;
        }
        Struct struct = newInstance();
        List<?> list;
        try {
            list = (List<?>) strValue;
        } catch (ClassCastException e) {
            throw new RuntimeException("" + strValue, e);
        }
        for (int i = 0; i < size(); i++) {
            TypeDef fieldType = getFieldType(i);
            Object strFieldValue = list.get(i);
            Object fieldValue = fieldType.unstringify(strFieldValue);
            struct.setValue(i, fieldValue);
        }
        return struct;
    }

    // TODO this is defined in Lumen but isn't visible...
    private static final String TUPLE_FUNCTOR = "positionalTupleGen";
    static final String STRUCT_FUNCTOR = "structureGen";

    @Override
    public Struct fromAtr(ATRTerm atrValue)
            throws PALException {
        Struct result = newInstance();
        if (atrValue instanceof ATRFunction) {
            ATRFunction struct = (ATRFunction) atrValue;
            String functor = struct.getFunctor();
            if (functor.equals(TUPLE_FUNCTOR)) {
                List<? extends ATRTerm> elements = struct.getElements();
                for (int i = 0; i < elements.size(); i++) {
                    ATRTerm atrMember = elements.get(i);
                    TypeDef fieldType = getFieldType(i);
                    Object member = fieldType.fromAtr(atrMember);
                    result.setValue(i, member);
                }
            } else if (functor.equals(STRUCT_FUNCTOR)) {
                List<? extends ATRTerm> elements = struct.getElements();
                String typeStr = ((ATRLiteral) elements.get(0)).optType();
                StructDef def = this;
                if (typeStr != null) {
                    TypeName typeName = TypeNameFactory.makeName(typeStr);
                    if (!typeName.equals(getName())) {
                        try {
                            def = (StructDef) getActionModel()
                                    .getType(typeName);
                            result = def.newInstance();
                        } catch (Exception e) {
                            throw new PALException("Fetching subtype "
                                    + typeStr + " of "
                                    + getName().getFullName(), e);
                        }
                    }
                }
                for (int i = 1; i < elements.size(); i++) {
                    ATRTerm atrMember = elements.get(i);
                    TypeDef fieldType = def.getFieldType(i - 1);
                    Object member = fieldType.fromAtr(atrMember);
                    result.setValue(i - 1, member);
                }
            } else {
                throw new IllegalArgumentException("Unknown structure: "
                        + struct);
            }
        } else if (atrValue instanceof ATRList) {
            ATRList atrList = (ATRList) atrValue;
            ATRList valuesList = (ATRList) atrList.get(1);
            for (int i = 0; i < valuesList.size(); i++) {
                ATRTerm atrMember = valuesList.get(i);
                TypeDef fieldType = getFieldType(i);
                Object member = fieldType.fromAtr(atrMember);
                result.setValue(i, member);
            }
        } else {
            throw new IllegalArgumentException("Couldn't parse: "
                    + ATRSyntax.toSource(atrValue) + " (" + atrValue.getClass()
                    + ")");
        }

        return result;
    }

    @Override
    ATRTerm nonNullToAtr(ATRConstructor<ATR, ?, ATRTerm, ?, ?, ?, ?, ?, ?, ?, ?> constructor,
                         Object value) {
        if (!(value instanceof Struct)) {
            throw new RuntimeException("Unexpected type " + value.getClass()
                    + ". Expected " + Struct.class);
        }

        Struct struct = (Struct) value;

        if (!struct.getDefinition().equals(this))
            throw new RuntimeException("Unexpected struct type: "
                    + struct.getDefinition());

        List<ATRTerm> fieldTerms = new ArrayList<ATRTerm>();
        fieldTerms.add(constructor.createLiteral(getName().getFullName(), null));
        for (int i = 0; i < size(); i++)
            fieldTerms.add(getFieldType(i).toAtr(constructor,
                    struct.getValue(i)));

        return constructor.createFunction(STRUCT_FUNCTOR, fieldTerms);
    }
}
