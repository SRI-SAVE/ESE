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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.decl.ATRFunctionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.Enumerated;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.Structure;
import com.sri.ai.lumen.atr.decl.impl.CTRTypeDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.learning.ATRIdiomTemplateAction;
import com.sri.ai.lumen.atr.learning.impl.ATRIdiomTemplateActionImpl;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRFunction;
import com.sri.ai.lumen.atr.term.ATRList;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRNoEvalTerm;
import com.sri.ai.lumen.atr.term.ATRNull;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.ATRVariable;
import com.sri.ai.lumen.atr.term.impl.CTRLiteral;
import com.sri.ai.lumen.atr.type.Type;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameExpr;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.*;
import com.sri.tasklearning.spine.messages.contents.ActionCategory;
import com.sri.tasklearning.spine.messages.contents.ParamClass;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for creating {@link ActionModelDef}s. The canonical
 * representation of an {@code ActionModelDef} is an {@link ATRDecl}, but they
 * are also represented using the ActionModel.xsd XML schema. This class
 * converts between those representations.
 */
public class ActionModelFactory {
    private static final Logger log = LoggerFactory
            .getLogger(ActionModelFactory.class);

    private final Bridge bridge;
    private final CTRConstructor ctrBuilder;
    private final Comparator<? super ATRTerm> termSorter;

    public ActionModelFactory(Bridge bridge) {
        this.bridge = bridge;
        ctrBuilder = new CTRConstructor();
        termSorter = new ATRTermSorter();
    }

    private ActionModel getActionModel() {
        return bridge.getActionModel();
    }

    /**
     * Given an ATR object representing a type, action, or constraint
     * declaration, construct the corresponding object from the
     * {@code ActionModelDef} hierarchy. Resolve any ambiguous type names
     * relative to the provided version and namespace.
     *
     * @param atr
     *            the ATR structure to translate into Bridge objects
     * @param version
     *            the version to use to resolve ambiguous name references
     * @param namespace
     *            the namespace to use to resolve ambiguous name references
     * @return a new definition created from the provided ATR structure
     * @throws PALException
     *             if the type cannot be created
     */
    public ActionModelDef makeActionModelDef(ATR atr,
                                             String version,
                                             String namespace)
            throws PALException {
        ATR concrete = null;
        if (TypeUtil.isAlias(atr)) {
            ATRList equivTerms = ((ATRTypeDeclaration) atr)
                    .optEquivalentTypes();
            for (ATRTerm equivTerm : equivTerms.getTerms()) {
                String equivStr = ((ATRLiteral) equivTerm).getString();
                TypeName equivName = TypeNameFactory.makeName(equivStr,
                        version, namespace);
                TypeDef equivType = (TypeDef) getActionModel().getType(
                        equivName);
                if (equivType != null && !TypeUtil.isAlias(equivType.getAtr())) {
                    concrete = equivType.getAtr();
                    if (concrete == null) {
                        /* This is true of PrimitiveTypeDef. */
                        concrete = new CTRLiteral(equivType.getName()
                                .getFullName());
                    }
                    break;
                }
            }
            if (concrete == null) {
                throw new PALException("Cannot find equiv types for "
                        + TypeUtil.getName(atr));
            }
        } else {
            concrete = atr;
        }

        TypeDef eleType = null;
        if (TypeUtil.isCollection(concrete)) {
            TypeName eleName = TypeUtil.getElementType(concrete);
            eleType = (TypeDef) getActionModel().getType(eleName);
        }

        if (TypeUtil.isActionFamily(atr)) {
            return new ActionFamilyDef(atr, bridge);
        } else if (TypeUtil.isIdiom(atr)) {
            return new IdiomDef(atr, bridge);
        } else if (TypeUtil.isAction(atr)) {
            if (TypeUtil.isProcedure(atr)) {
                return LumenProcedureDef.newInstance(
                        (ATRActionDeclaration) atr, true, bridge);
            } else {
                return new ActionDef((ATRActionDeclaration) atr, bridge);
            }
        } else if (TypeUtil.isConstraintDecl(atr)) {
            return new ConstraintDef((ATRFunctionDeclaration) atr, bridge);
        } else if (TypeUtil.isStruct(concrete)) {
            if (concrete == atr) {
                return new StructDef((Structure) atr, bridge);
            } else {
                return new StructDef((ATRTypeDeclaration) atr,
                        (Structure) concrete, bridge);
            }
        } else if (TypeUtil.isBag(concrete)) {
            if (concrete == atr) {
                return new BagDef(eleType, bridge);
            } else {
                return new BagDef((ATRTypeDeclaration) atr, eleType, bridge);
            }
        } else if (TypeUtil.isList(concrete)) {
            if (concrete == atr) {
                return new ListDef(eleType, bridge);
            } else {
                return new ListDef((ATRTypeDeclaration) atr, eleType, bridge);
            }
        } else if (TypeUtil.isNullable(concrete)) {
            if (concrete == atr) {
                return new NullableDef(eleType, bridge);
            } else {
                return new NullableDef((ATRTypeDeclaration) atr, eleType,
                        bridge);
            }
        } else if (TypeUtil.isSet(concrete)) {
            if (concrete == atr) {
                return new SetDef(eleType, bridge);
            } else {
                return new SetDef((ATRTypeDeclaration) atr, eleType, bridge);
            }
        } else if (TypeUtil.isCustom(concrete)) {
            if (concrete == atr) {
                return new CustomTypeDef((ATRTypeDeclaration) atr, bridge);
            } else {
                return new CustomTypeDef((ATRTypeDeclaration) atr,
                        (ATRTypeDeclaration) concrete, bridge);
            }
        } else if (TypeUtil.isEnumerated(concrete)) {
            if (concrete == atr) {
                return new EnumeratedTypeDef((Enumerated) atr, bridge);
            } else {
                return new EnumeratedTypeDef((ATRTypeDeclaration) atr,
                        (Enumerated) concrete, bridge);
            }
        } else if (TypeUtil.isPrimitive(concrete)) {
            String nameStr = ((ATRLiteral) concrete).getString();
            PrimitiveTypeDef.Predefined predef = PrimitiveTypeDef.Predefined
                    .valueOf(nameStr.toUpperCase());
            if (concrete == atr) {
                return PrimitiveTypeDef.getPrimitive(predef, bridge);
            } else {
                return new PrimitiveTypeDef((ATRTypeDeclaration) atr, predef,
                        bridge);
            }
        } else {
            throw new RuntimeException("Unknown type declaration: "
                    + ATRSyntax.toSource(atr));
        }
    }

    /**
     * Build a type like {@code list<foo>}.
     *
     * @param outer
     *            the type of collection, such as {@code list}
     * @param innerType
     *            the element type of the collection, such as {@code foo}
     * @return the appropriate collection type
     */
    public CollectionTypeDef makeCollectionType(String outer,
                                                TypeDef innerType) {
        if (innerType == null) {
            throw new IllegalArgumentException("Cannot make " + outer
                    + "<null>");
        }
        if (outer.equals(Type.Category.LIST.prefix)) {
            return new ListDef(innerType, bridge);
        } else if (outer.equals(Type.Category.BAG.prefix)) {
            return new BagDef(innerType, bridge);
        } else if (outer.equals(Type.Category.SET.prefix)) {
            return new SetDef(innerType, bridge);
        } else if (outer.equals(Type.Category.NULLABLE.prefix)) {
            return new NullableDef(innerType, bridge);
        } else {
            throw new IllegalArgumentException("Unknown collection type "
                    + outer);
        }
    }

    /**
     * Converts a JAXB XML structure conforming to the action model XSD into its
     * equivalent ATR representation.
     *
     * @param xml
     *            the JAXB representation of a type declaration
     * @param version
     *            the version of the namespace this type belongs to
     * @param namespace
     *            the namespace this type belongs to
     * @return a new ATR structure expressing the same information as the given
     *         JAXB
     * @throws PALException
     *             if a required type cannot be retrieved
     */
    public ATRTypeDeclaration toAtr(TypeType xml,
                                    String version,
                                    String namespace)
            throws PALException {
        ATRTypeDeclaration result;

        String id = xml.getId();
        SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName(id,
                version, namespace);
        if (TypeNameFactory.isPrimitive(name)) {
            throw new PALException("Cannot duplicate predefined type " + id);
        }
        String nameStr = name.getFullName();
        String description = xml.getDescription();
        description = trimWhitespace(description);

        Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
        ATRTerm descrTerm = ctrBuilder.createLiteral(description, null);
        propMap.put(TypeDef.DESCRIPTION, descrTerm);
        for (MetadataType metadataXml : xml.getMetadata()) {
            String key = metadataXml.getKey();
            String value = metadataXml.getValue();
            value = trimWhitespace(value);
            ATRTerm valueTerm = ctrBuilder.createLiteral(value, null);
            propMap.put(key, valueTerm);
        }

        List<String> equivTypes = new ArrayList<String>();
        for (String str : xml.getEquivalentTo()) {
            TypeName typeName = TypeNameFactory.makeName(str, version,
                    namespace);
            equivTypes.add(typeName.getFullName());
        }

        CustomType customXml = xml.getCustom();
        EnumType enumXml = xml.getEnum();
        NullableType nullableXml = xml.getNullable();
        ListType listXml = xml.getList();
        SetType setXml = xml.getSet();
        BagType bagXml = xml.getBag();
        StructType structXml = xml.getStruct();

        if (customXml != null) {
            String javaType = customXml.getJavaType();
            InheritType inheritXml = customXml.getInherit();
            if (javaType != null) {
                ATRMap props = ctrBuilder.createMap(propMap);
                result = CTRTypeDeclaration.createApplicationType(nameStr,
                        equivTypes, props, javaType);
            } else {
                String parentStr = inheritXml.getParent();
                TypeName parentName = TypeNameFactory.makeName(parentStr,
                        version, namespace);
                String parentQualStr = parentName.getFullName();
                ATRMap props = ctrBuilder.createMap(propMap);
                result = CTRTypeDeclaration.createApplicationSubType(nameStr,
                        equivTypes, props, parentQualStr);
            }
        } else if (enumXml != null) {
            List<String> values = enumXml.getValue();
            SortedSet<String> valueSet = new TreeSet<String>();
            for (String value : values) {
                if (valueSet.contains(value)) {
                    throw new PALException("Duplicate values in enum "
                            + nameStr);
                }
                valueSet.add(value);
            }

            // Re-sort the list of values.
            values.clear();
            values.addAll(valueSet);
            Collections.sort(values);

            // inheritance of enums
            List<String> subTypeNames = new ArrayList<>();
            for (SubTypeType subXml : enumXml.getSubType()) {
                TypeName subName = TypeNameFactory.makeName(subXml.getSub(), version, namespace);
                String subStr = subName.getFullName();
                if (!subTypeNames.contains(subStr)) {
                    subTypeNames.add(subStr);
                }
            }
            Collections.sort(subTypeNames);

            ATRMap props = ctrBuilder.createMap(propMap);
            result = CTRTypeDeclaration.createEnumeratedType(nameStr,
                     equivTypes, props, values, subTypeNames);
        } else if (nullableXml != null) {
            String memberName = nullableXml.getRef().getTypeRef();
            TypeName memberTypeName = TypeNameFactory.makeName(memberName,
                    version, namespace);
            TypeDef memberType = (TypeDef) getActionModel().getType(
                    memberTypeName);
            TypeNameExpr equivType = new TypeNameExpr(
                    Type.Category.NULLABLE.prefix, memberType.getName());
            String equivTypeStr = equivType.getFullName();
            if (!equivTypes.contains(equivTypeStr)) {
                equivTypes.add(equivTypeStr);
            }
            ATRMap props = ctrBuilder.createMap(propMap);
            result = CTRTypeDeclaration.createAliasType(nameStr, equivTypes,
                    props);
        } else if (listXml != null) {
            String memberName = listXml.getRef().getTypeRef();
            if (Boolean.TRUE.equals(listXml.isPermutable())) {
                propMap.put(TypeUtil.PERMUTABLE,
                        ctrBuilder.createLiteral(Boolean.TRUE.toString(), null));
            }
            generalizeSingleton(listXml.getGeneralizeSingleton(), propMap);
            generalizeUnsupported(listXml.getGeneralizeUnsupported(), propMap);
            TypeName memberTypeName = TypeNameFactory.makeName(memberName,
                    version, namespace);
            TypeDef memberType = (TypeDef) getActionModel().getType(
                    memberTypeName);
            if (memberType == null) {
                throw new PALException("Couldn't retrieve member type "
                        + memberTypeName + " of " + name);
            }
            TypeNameExpr equivType = new TypeNameExpr(
                    Type.Category.LIST.prefix, memberType.getName());
            String equivTypeStr = equivType.getFullName();
            if (!equivTypes.contains(equivTypeStr)) {
                equivTypes.add(equivTypeStr);
            }
            ATRMap props = ctrBuilder.createMap(propMap);
            result = CTRTypeDeclaration.createAliasType(nameStr, equivTypes,
                    props);
        } else if (setXml != null) {
            String memberName = setXml.getRef().getTypeRef();
            generalizeUnsupported(setXml.getGeneralizeUnsupported(), propMap);
            TypeName memberTypeName = TypeNameFactory.makeName(memberName,
                    version, namespace);
            TypeDef memberType = (TypeDef) getActionModel().getType(
                    memberTypeName);
            TypeNameExpr equivType = new TypeNameExpr(Type.Category.SET.prefix,
                    memberType.getName());
            String equivTypeStr = equivType.getFullName();
            if (!equivTypes.contains(equivTypeStr)) {
                equivTypes.add(equivTypeStr);
            }
            ATRMap props = ctrBuilder.createMap(propMap);
            result = CTRTypeDeclaration.createAliasType(nameStr, equivTypes,
                    props);
        } else if (bagXml != null) {
            String memberName = bagXml.getRef().getTypeRef();
            generalizeUnsupported(bagXml.getGeneralizeUnsupported(), propMap);
            TypeName memberTypeName = TypeNameFactory.makeName(memberName,
                    version, namespace);
            TypeDef memberType = (TypeDef) getActionModel().getType(
                    memberTypeName);
            TypeNameExpr equivType = new TypeNameExpr(Type.Category.BAG.prefix,
                    memberType.getName());
            String equivTypeStr = equivType.getFullName();
            if (!equivTypes.contains(equivTypeStr)) {
                equivTypes.add(equivTypeStr);
            }
            ATRMap props = ctrBuilder.createMap(propMap);
            result = CTRTypeDeclaration.createAliasType(nameStr, equivTypes,
                    props);
        } else if (structXml != null) {
            generalizeUnsupported(structXml.getGeneralizeUnsupported(), propMap);
            if (Boolean.TRUE.equals(structXml.isOpaque())) {
                propMap.put(TypeUtil.OPAQUE,
                        ctrBuilder.createLiteral(Boolean.TRUE.toString(), null));
            }
            List<String> fieldNames = new ArrayList<String>();
            List<String> fieldTypes = new ArrayList<String>();
            List<ATRTerm> dynFields = new ArrayList<ATRTerm>();
            List<ATRTerm> constFields = new ArrayList<ATRTerm>();
            int fieldNum = 0;
            for (StructMemberType memberXml : structXml.getRef()) {
                String fieldName = memberXml.getName();
                if (fieldName == null || fieldName.length() == 0) {
                    fieldName = "field" + fieldNum++;
                }
                String fieldTypeStr = memberXml.getTypeRef();
                TypeName fieldTypeName = TypeNameFactory.makeName(fieldTypeStr,
                        version, namespace);
                TypeDef memberType = (TypeDef) getActionModel().getType(
                        fieldTypeName);
                if (memberType == null) {
                    throw new PALException("Couldn't retrieve type "
                            + fieldTypeName + " for field " + fieldName
                            + " of tuple " + name);
                }
                fieldNames.add(fieldName);
                fieldTypes.add(fieldTypeName.getFullName());
                if (Boolean.TRUE.equals(memberXml.isDynamic())) {
                    dynFields.add(ctrBuilder.createLiteral(fieldName, null));
                }
                if (Boolean.TRUE.equals(memberXml.isConstant())) {
                    constFields.add(ctrBuilder.createLiteral(fieldName, null));
                }
            }
            Collections.sort(dynFields, termSorter);
            propMap.put(TypeUtil.STRUCT_DYNAMIC, ctrBuilder.createList(dynFields));
            Collections.sort(constFields, termSorter);
            propMap.put(TypeUtil.STRUCT_CONSTANT, ctrBuilder.createList(constFields));
            String parentQualStr = null;
            InheritType inheritXml = structXml.getInherit();
            if (inheritXml != null) {
                String parentStr = inheritXml.getParent();
                TypeName parentName = TypeNameFactory.makeName(parentStr,
                        version, namespace);
                parentQualStr = parentName.getFullName();
            }
            ATRMap props = ctrBuilder.createMap(propMap);
            result = CTRTypeDeclaration.createStructureType(nameStr,
                    equivTypes, props, parentQualStr, fieldNames, fieldTypes);
        } else if (!equivTypes.isEmpty()) {
            /*
             * The type is incomplete, but it references another (hopefully)
             * complete type.
             */
            ATRMap props = ctrBuilder.createMap(propMap);
            result = CTRTypeDeclaration.createAliasType(nameStr, equivTypes,
                    props);
        } else {
            throw new RuntimeException("Type " + name + " is incomplete");
        }

        return result;
    }

    private void generalizeUnsupported(GeneralizeUnsupportedType genUnsupp,
                                       Map<String, ATRTerm> propMap) {
        if (genUnsupp != null) {
            String pref = genUnsupp.getPreference();
            BigInteger maxInputs = genUnsupp.getMaxInputs();
            propMap.put(TypeUtil.GENERALIZE_UNSUPPORTED,
                    ctrBuilder.createLiteral(pref, null));
            if(maxInputs != null) {
                propMap.put(TypeUtil.CONSTRUCT_MAX_INPUTS,
                        ctrBuilder.createLiteral(maxInputs.toString(), null));
            }
        }
    }

    private void generalizeSingleton(GeneralizeSingletonType genSingl,
                                     Map<String, ATRTerm> propMap) {
        if (genSingl != null) {
            String method = genSingl.getMethod();
            propMap.put(TypeUtil.GENERALIZE_SINGLETON,
                    ctrBuilder.createLiteral(method, null));
        }
    }

    /**
     * Constructs an ATR structure to represent the given constraint
     * declaration, given in JAXB XML form.
     *
     * @param xml
     *            the JAXB to convert to ATR
     * @param version
     *            the version of the namespace to which this constraint
     *            declaration belongs
     * @param namespace
     *            the namespace to which this constraint declaration belongs
     * @return a new ATR structure equivalent to the given JAXB XML
     */
    public ATRFunctionDeclaration toAtr(ConstraintDeclarationType xml,
                                        String version,
                                        String namespace) {
        String id = xml.getId();
        SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName(id,
                version, namespace);

        Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
        for (MetadataType metadataXml : xml.getMetadata()) {
            String key = metadataXml.getKey();
            String value = metadataXml.getValue();
            value = trimWhitespace(value);
            ATRTerm valueTerm = ctrBuilder.createLiteral(value, null);
            propMap.put(key, valueTerm);
        }

        List<ConstraintDeclParamType> paramsXml = xml.getParam();
        List<ATRTerm> paramDescrs = new ArrayList<ATRTerm>();
        List<ATRParameter> params = new ArrayList<ATRParameter>();
        for (int i = 0; i < paramsXml.size(); i++) {
            ConstraintDeclParamType paramXml = paramsXml.get(i);

            // Build a parameter for this field.
            ATRVariable var = ctrBuilder.createVariable(paramXml.getId());
            ATRParameter param = ctrBuilder.createParameter(var,
                    Modality.INPUT, null, null);
            params.add(param);

            // Also get the description for this field.
            String descrStr = paramXml.getDescription();
            if (descrStr == null) {
                descrStr = "";
            }
            ATRLiteral descr = ctrBuilder.createLiteral(descrStr, null);
            paramDescrs.add(descr);
        }
        propMap.put(TypeUtil.CONSTRAINT_DESCRIPTIONS,
                ctrBuilder.createList(paramDescrs));

        ATRMap props = ctrBuilder.createMap(propMap);
        ATRSig sig = ctrBuilder.createSignature(name.getFullName(), params);
        ATRNoEvalTerm eval = ctrBuilder.createNoEval(ctrBuilder
                .createSymbol(name.getFullName()));
        ATRFunctionDeclaration atr = ctrBuilder.createFunctionDeclaration(sig,
                eval, null, props);
        return atr;
    }

    /**
     * Converts the given JAXB XML structure, describing an action declaration,
     * into equivalent ATR.
     *
     * @param xml
     *            JAXB representation of an action declaration
     * @param version
     *            the version of the namespace to which this action belongs
     * @param namespace
     *            the namespace to which this action belongs
     * @return a new ATR structure equivalent to the provided JAXB XML
     * @throws PALException
     *             if a required type cannot be retrieved
     */
    public ATRActionDeclaration toAtr(ActionType xml,
                                      String version,
                                      String namespace)
            throws PALException {
        String id = xml.getId();
        SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName(id,
                version, namespace);

        Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
        InheritType inheritXml = xml.getInherit();
        if (inheritXml != null) {
            String parentName = inheritXml.getParent();
            SimpleTypeName parentTypeName = (SimpleTypeName) TypeNameFactory
                    .makeName(parentName, version, namespace);
            ActionModelDef parentRawType = getActionModel().getType(
                    parentTypeName);
            if (parentRawType == null) {
                throw new PALException("Couldn't retrieve parent type "
                        + parentTypeName + " of " + name);
            }
            if (!(parentRawType instanceof ActionDef)) {
                throw new IllegalArgumentException("Action " + id
                        + "'s parent " + parentName + " must be an action");
            }
            propMap.put(TypeUtil.PARENT,
                    ctrBuilder.createLiteral(parentTypeName.getFullName(), null));
        }
        String description = xml.getDescription();
        description = trimWhitespace(description);

        ATRTerm descrTerm = ctrBuilder.createLiteral(description, null);
        propMap.put(TypeDef.DESCRIPTION, descrTerm);
        for (MetadataType metadataXml : xml.getMetadata()) {
            String key = metadataXml.getKey();
            String value = metadataXml.getValue();
            value = trimWhitespace(value);
            ATRTerm valueTerm = ctrBuilder.createLiteral(value, null);
            propMap.put(key, valueTerm);
        }

        List<ActionIdiomFamilyType> familiesXml = xml.getIdiomFamily();
        List<ATRTerm> familiesAtr = new ArrayList<ATRTerm>();
        for (ActionIdiomFamilyType family : familiesXml) {
            String familyStr = family.getFamily();
            SimpleTypeName familyName = (SimpleTypeName) TypeNameFactory
                    .makeName(familyStr, version, namespace);
            ActionModelDef amDef = getActionModel().getType(familyName);
            if (amDef == null) {
                throw new PALException("Couldn't retrieve family " + familyName
                        + " of action " + name);
            }
            if (!(amDef instanceof ActionFamilyDef)) {
                throw new IllegalArgumentException(familyName + " of action "
                        + name + " is not an action family");
            }
            familiesAtr.add(ctrBuilder.createLiteral(familyName.getFullName(), null));
        }
        Collections.sort(familiesAtr, termSorter);
        ATRList familiesTerm = ctrBuilder.createList(familiesAtr);
        propMap.put(TypeUtil.ACTION_FAMILIES, familiesTerm);

        String categoryStr = xml.getCategory();
        ActionCategory category = null;
        if (categoryStr != null) {
            category = ActionCategory.getValueOf(categoryStr);
        }
        if (category == null) {
            category = ActionCategory.EFFECTOR;
        }
        ATRLiteral categoryTerm = ctrBuilder.createLiteral(category.getName(), null);
        propMap.put(TypeUtil.ACTION_CATEGORY, categoryTerm);

        ConstraintsType xmlCons = xml.getConstraints();
        ATRNoEvalTerm atrCons = atrConstraints(xmlCons, version, namespace);
        propMap.put(TypeUtil.CONSTRAINTS, atrCons);

        List<ATRParameter> params = new ArrayList<ATRParameter>();
        paramsToAtr(xml.getInputParam(), Modality.INPUT, version, namespace,
                params, propMap);
        paramsToAtr(xml.getOutputParam(), Modality.OUTPUT, version, namespace,
                params, propMap);

        if (xml.isBenign() != null && xml.isBenign()) {
            propMap.put(TypeUtil.BENIGN, ctrBuilder.createLiteral("true", null));
        }

        /* Deal with collapsibility. */
        CollapsibleType collapse = xml.getCollapsible();
        if (collapse != null) {
            Map<String, ATRTerm> collapseMap = new HashMap<String, ATRTerm>();
            CollapsibleOptionType inside = collapse.getInsideGesture();
            CollapsibleOptionType outside = collapse.getOutsideGesture();
            if (inside == null) {
                inside = CollapsibleOptionType.ALL;
            }
            if (outside == null) {
                outside = CollapsibleOptionType.ALL;
            }
            collapseMap.put(TypeUtil.COLLAPSIBLE_INSIDE_GESTURE,
                    ctrBuilder.createLiteral(inside.value(), null));
            collapseMap.put(TypeUtil.COLLAPSIBLE_OUTSIDE_GESTURE,
                    ctrBuilder.createLiteral(outside.value(), null));

            for (CollapsibleParamType param : collapse.getParam()) {
                String paramName = param.getId();
                /* Does that param exist? */
                boolean found = false;
                for (ATRParameter atrParam : params) {
                    if (atrParam.getVariable().getVariableName()
                            .equals(paramName)) {
                        found = true;
                    }
                }
                if (!found) {
                    throw new PALException(
                            "Collapsible refers to non-existent param "
                                    + paramName);
                }

                String keep = param.getKeep();
                collapseMap
                        .put("$" + paramName, ctrBuilder.createLiteral(keep, null));
            }

            propMap.put(TypeUtil.COLLAPSIBLE, ctrBuilder.createMap(collapseMap));
        }

        ATRSig sig = ctrBuilder.createSignature(name.getFullName(), params);
        ATRMap props = ctrBuilder.createMap(propMap);

        /*
         * EXECUTEJ is used by the Agave actions, noted as a metadata
         * annotation.
         */
        ATRActionDeclaration result;
        if (propMap.containsKey(TypeUtil.EXECUTEJ)) {
            ATRTerm callMethodTerm = propMap.get(TypeUtil.EXECUTEJ);
            String callMethod = ((ATRLiteral) callMethodTerm).getString();
            result = ctrBuilder.createActionDeclaration(sig, callMethod, props);
        } else {
            result = ctrBuilder.createActionDeclaration(sig, (ATRTask) null,
                    props);
        }
        if (log.isDebugEnabled()) {
            log.debug("Built action: {}", ATRSyntax.toSource(result));
        }
        return result;
    }

    /**
     * Converts an action family declaration from XML to ATR.
     *
     * @param familyXml
     *            XML action family declaration
     * @return ATR action family declaration
     */
    ATRActionDeclaration toAtr(FamilyType familyXml,
                               String version,
                               String namespace) {
        String id = familyXml.getId();
        TypeName name = TypeNameFactory.makeName(id, version, namespace);
        List<ActionFamilyParamType> inputsXml = familyXml.getInputParam();
        List<ActionFamilyParamType> outputsXml = familyXml.getOutputParam();
        List<ATRParameter> params = new ArrayList<ATRParameter>();
        for (ActionFamilyParamType inputXml : inputsXml) {
            String role = inputXml.getRole();
            ATRVariable var = ctrBuilder.createVariable(role);
            ATRParameter param = ctrBuilder.createParameter(var,
                    Modality.INPUT, null, null);
            params.add(param);
        }
        for (ActionFamilyParamType outputXml : outputsXml) {
            String role = outputXml.getRole();
            ATRVariable var = ctrBuilder.createVariable(role);
            ATRParameter param = ctrBuilder.createParameter(var,
                    Modality.OUTPUT, null, null);
            params.add(param);
        }
        ATRSig sig = ctrBuilder.createSignature(name.getFullName(), params);

        Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
        propMap.put(TypeUtil.TYPE, ctrBuilder.createLiteral(TypeUtil.TYPE_FAMILY, null));
        ATRMap props = ctrBuilder.createMap(propMap);
        ATRActionDeclaration result = ctrBuilder.createActionDeclaration(sig, (ATRTask) null, props);
        if (log.isDebugEnabled()) {
            log.debug("Built action family: {}", ATRSyntax.toSource(result));
        }
        return result;
    }

    ATRActionDeclaration toAtr(IdiomType it,
                               String version,
                               String namespace)
            throws PALException {
        /* Name */
        String nameStr = it.getId();
        SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName(
                nameStr, version, namespace);

        /* Description and other metadata */
        Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
        String descr = it.getDescription();
        propMap.put(TypeDef.DESCRIPTION, ctrBuilder.createLiteral(descr, null));
        for (MetadataType metaXml : it.getMetadata()) {
            String key = metaXml.getKey();
            String value = metaXml.getValue();
            propMap.put(key, ctrBuilder.createLiteral(value, null));
        }

        /* Signature */
        List<IdiomParamType> inputsXml = it.getInputParam();
        List<IdiomParamType> outputsXml = it.getOutputParam();
        List<ATRParameter> idiomParams = new ArrayList<ATRParameter>();
        for(IdiomParamType input : inputsXml) {
            String paramName = input.getId();
            ATRVariable var = ctrBuilder.createVariable(paramName);
            ATRParameter param = ctrBuilder.createParameter(var, Modality.INPUT, null, null);
            idiomParams.add(param);
            String matchIf = input.getMatchIf();
            if (matchIf == null || matchIf.equals("")) {
                matchIf = "equals";
            }
            Map<String, ATRTerm> paramMap = new HashMap<String, ATRTerm>();
            paramMap.put("matchIf", ctrBuilder.createLiteral(matchIf, null));
            propMap.put("$" + paramName, ctrBuilder.createMap(paramMap));
        }
        for(IdiomParamType output : outputsXml) {
            String paramName = output.getId();
            ATRVariable var = ctrBuilder.createVariable(paramName);
            ATRParameter param = ctrBuilder.createParameter(var, Modality.OUTPUT, null, null);
            idiomParams.add(param);
            String matchIf = output.getMatchIf();
            if (matchIf == null || matchIf.equals("")) {
                matchIf = "equals";
            }
            Map<String, ATRTerm> paramMap = new HashMap<String, ATRTerm>();
            paramMap.put("matchIf", ctrBuilder.createLiteral(matchIf, null));
            propMap.put("$" + paramName, ctrBuilder.createMap(paramMap));
        }
        ATRSig sig = ctrBuilder.createSignature(name.getFullName(), idiomParams);

        /* Templates */
        List<IdiomTemplateType> templatesXml = it.getTemplate();
        List<ATRTerm> templatesList = new ArrayList<ATRTerm>();
        for (IdiomTemplateType templ : templatesXml) {
            /* Each template is an ATRList of [precedence, actions]. */
            List<ATRTerm> actionsTerm = new ArrayList<ATRTerm>();
            for (Object obj : templ.getActionOrNamedAction()) {
                /*
                 * Actions is an ATRList of ATRLiterals. Each literal is the
                 * string representation of an ATRIdiomTemplateAction. Each
                 * action refers either to an action family or a specific
                 * ("named") action.
                 */
                ATRIdiomTemplateAction action;
                if (obj instanceof IdiomTemplateActionType) {
                    IdiomTemplateActionType itat = (IdiomTemplateActionType) obj;
                    String familyStr = itat.getFamily();
                    SimpleTypeName familyName = (SimpleTypeName) TypeNameFactory
                            .makeName(familyStr, version, namespace);
                    ActionModelDef amDef = getActionModel().getType(familyName);
                    if (amDef == null) {
                        throw new PALActionMissingException(
                                "Couldn't retrieve " + familyName
                                        + " for idiom " + name, familyName);
                    }
                    if (!(amDef instanceof ActionFamilyDef)) {
                        throw new RuntimeException(familyName + " of idiom "
                                + name + " is not a family");
                    }
                    /*
                     * When we build the ATRIdiomTemplateActionImpl, we pass a
                     * paramMap where each key is an idiom param name and each
                     * value is a collection of action param names.
                     */
                    List<IdiomTemplateActionParamType> params = itat
                            .getIdiomParam();
                    Map<String, Collection<String>> paramMap = new HashMap<String, Collection<String>>();
                    for (IdiomTemplateActionParamType param : params) {
                        String idiomParam = param.getId();
                        /* Does the idiom param exist? */
                        boolean paramFound = false;
                        for (ATRParameter idiomParamTerm : idiomParams) {
                            String idiomParamStr = idiomParamTerm.getVariable()
                                    .getVariableName();
                            if (idiomParamStr.equals(idiomParam)) {
                                paramFound = true;
                            }
                        }
                        if (!paramFound) {
                            throw new PALException("Template action "
                                    + familyStr + " of idiom " + name
                                    + " references unknown idiom param "
                                    + idiomParam);
                        }

                        String actionParam = param.getRole();
                        /* Does the action family role exist? */
                        ActionFamilyDef famDef = (ActionFamilyDef) amDef;
                        if (famDef.getParamNum(actionParam) == -1) {
                            throw new PALException("Template action "
                                    + familyStr + " of idiom " + name
                                    + " references unknown role " + actionParam
                                    + " of family " + familyName);
                        }

                        Collection<String> actionParams = paramMap
                                .get(idiomParam);
                        if (actionParams == null) {
                            actionParams = new HashSet<String>();
                            paramMap.put(idiomParam, actionParams);
                        }
                        actionParams.add(actionParam);
                    }
                    action = new ATRIdiomTemplateActionImpl(
                            familyName.getFullName(), paramMap);
                } else if (obj instanceof IdiomTemplateNamedActionType) {
                    IdiomTemplateNamedActionType itnat = (IdiomTemplateNamedActionType) obj;
                    String actionStr = itnat.getId();
                    SimpleTypeName actionName = (SimpleTypeName) TypeNameFactory
                            .makeName(actionStr, version, namespace);
                    ActionModelDef amDef = getActionModel().getType(actionName);
                    if (amDef == null) {
                        throw new PALActionMissingException(
                                "Couldn't retrieve " + actionName
                                        + " for idiom " + name, actionName);
                    }
                    if (!(amDef instanceof ActionDef)) {
                        throw new RuntimeException(actionName + " of idiom "
                                + name + " is not an action");
                    }
                    List<IdiomTemplateNamedActionParamType> params = itnat
                            .getIdiomParam();
                    Map<String, Collection<String>> paramMap = new HashMap<String, Collection<String>>();
                    for (IdiomTemplateNamedActionParamType param : params) {
                        String idiomParam = param.getId();
                        /* Does the idiom param exist? */
                        boolean paramFound = false;
                        for (ATRParameter idiomParamTerm : idiomParams) {
                            String idiomParamStr = idiomParamTerm.getVariable()
                                    .getVariableName();
                            if (idiomParamStr.equals(idiomParam)) {
                                paramFound = true;
                            }
                        }
                        if (!paramFound) {
                            throw new PALException("Template action "
                                    + actionStr + " of idiom " + name
                                    + " references unknown idiom param "
                                    + idiomParam);
                        }

                        String actionParam = param.getActionParam();
                        /* Does the action parameter exist? */
                        ActionDef actDef = (ActionDef) amDef;
                        if (actDef.getParamNum(actionParam) == -1) {
                            throw new PALException("Template action "
                                    + actionStr + " of idiom " + name
                                    + " references unknown param "
                                    + actionParam + " of action " + actionName);
                        }

                        Collection<String> actionParams = paramMap
                                .get(idiomParam);
                        if (actionParams == null) {
                            actionParams = new HashSet<String>();
                            paramMap.put(idiomParam, actionParams);
                        }
                        actionParams.add(actionParam);
                    }
                    action = new ATRIdiomTemplateActionImpl(
                            actionName.getFullName(), paramMap);
                } else {
                    throw new IllegalArgumentException("Unknown object "
                            + obj.getClass());
                }
                actionsTerm.add(ctrBuilder.createLiteral(action.toString(), null));
            }

            int precedence = actionsTerm.size();
            if (templ.getPrecedence() != null) {
                precedence = templ.getPrecedence().intValue();
            }
            String templId = templ.getId();
            List<ATRTerm> templateTerm = new ArrayList<ATRTerm>();
            templateTerm.add(ctrBuilder.createLiteral(templId, null));
            templateTerm.add(ctrBuilder.createLiteral(precedence, null));
            templateTerm.add(ctrBuilder.createList(actionsTerm));
            templatesList.add(ctrBuilder.createList(templateTerm));
        }
        Collections.sort(templatesList, termSorter);
        propMap.put(TypeUtil.IDIOM_TEMPLATES, ctrBuilder.createList(templatesList));

        propMap.put(TypeUtil.TYPE,
                ctrBuilder.createLiteral(TypeUtil.TYPE_IDIOM, null));
        ATRMap props = ctrBuilder.createMap(propMap);
        ATRActionDeclaration result = ctrBuilder.createActionDeclaration(sig,
                (ATRTask) null, props);
        if (log.isDebugEnabled()) {
            log.debug("Built idiom: {}", ATRSyntax.toSource(result));
        }
        return result;
    }

    /**
     * Builds the ATR data structures for an action parameter from the
     * corresponding XML structures.
     *
     * @param paramsXml
     *            XML to read
     * @param modality
     *            Is it an input or output parameter?
     * @param version
     *            version of the namespace the action belongs to
     * @param namespace
     *            namespace the action belongs to
     * @param params
     *            list to store ATR parameters into
     * @param propsList
     *            list to store ATR parameter properties into
     */
    private void paramsToAtr(final List<ParamType> paramsXml,
                             final Modality modality,
                             final String version,
                             final String namespace,
                             List<ATRParameter> params,
                             Map<String, ATRTerm> actionPropMap)
            throws PALException {
        for (ParamType paramXml : paramsXml) {
            /* Build the ATRParameter. */
            String name = paramXml.getId();
            ATRVariable var = ctrBuilder.createVariable(name);
            String paramTypeStr = paramXml.getTypeRef().getTypeId();
            TypeName paramTypeName = TypeNameFactory.makeName(paramTypeStr,
                    version, namespace);
            String typeStr = paramTypeName.getFullName();
            ATRTerm defValTerm = null;
            if (modality == Modality.INPUT) {
                String defValStr = paramXml.getDefaultValue();
                if (defValStr != null) {
                    TypeDef type = (TypeDef) getActionModel().getType(
                            paramTypeName);
                    if (type instanceof CustomTypeDef
                            || type instanceof EnumeratedTypeDef
                            || type instanceof PrimitiveTypeDef) {
                        defValTerm = ctrBuilder.createLiteral(defValStr, typeStr);
                    } else if (type instanceof CollectionTypeDef
                            || type instanceof StructDef) {
                        try {
                            defValTerm = ATRSyntax.CTR.termFromSource(defValStr);
                            defValTerm = normalize(defValTerm, version,
                                    namespace);
                        } catch (LumenSyntaxError e) {
                            throw new PALException(
                                    "Cannot parse default value for " + name
                                            + ": " + defValStr, e);
                        }
                    } else {
                        throw new RuntimeException("Unknown type " + type
                                + " of param " + name);
                    }
                }
            }
            ATRParameter param = ctrBuilder.createParameter(var, modality,
                    typeStr, defValTerm);
            params.add(param);

            /*
             * Each parameter has its own map in the action's properties. Start
             * by putting this parameter's class into the map.
             */
            Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
            ParamClassType paramClassXml = paramXml.getClazz();
            ParamClass paramClass;
            Boolean violable = null;
            if (paramClassXml != null) {
                paramClass = ParamClass.getValueOf(paramClassXml.getClazz());
                violable = paramClassXml.isViolable();
            } else {
                if (modality == Modality.INPUT) {
                    paramClass = ParamClass.GENERALIZABLE;
                } else {
                    paramClass = ParamClass.EXTERNAL;
                }
            }
            if (paramClass.isInput()) {
                if (modality != Modality.INPUT) {
                    throw new IllegalArgumentException("Param " + name
                            + " is class " + paramClass
                            + " which must be input");
                }
            } else {
                if (modality != Modality.OUTPUT) {
                    throw new IllegalArgumentException("Param " + name
                            + " is class " + paramClass
                            + " which must be output");
                }
            }
            propMap.put(TypeUtil.PARAM_CLASS,
                    ctrBuilder.createLiteral(paramClass.getName(), null));
            if (violable != null) {
                propMap.put(TypeUtil.PARAM_CLASS_VIOLABLE,
                        ctrBuilder.createLiteral(violable.toString(), null));
            }

            /*
             * Bindings between this parameter and any action families the
             * action belongs to.
             */
            Map<TypeName, Set<String>> bindings = new HashMap<TypeName, Set<String>>();
            for (ActionIdiomParamType binding : paramXml.getIdiomParam()) {
                String familyStr = binding.getFamily();
                String role = binding.getRole();
                SimpleTypeName familyName = (SimpleTypeName) TypeNameFactory
                        .makeName(familyStr, version, namespace);

                /* Does this action belong to that family? */
                boolean found = false;
                ATRList actFams = (ATRList) actionPropMap
                        .get(TypeUtil.ACTION_FAMILIES);
                for (ATRTerm t : actFams.getTerms()) {
                    ATRLiteral actFamLit = (ATRLiteral) t;
                    String actFamStr = actFamLit.getString();
                    SimpleTypeName actFamName = (SimpleTypeName) TypeNameFactory
                            .makeName(actFamStr, version, namespace);
                    if (actFamName.equals(familyName)) {
                        found = true;
                    }
                }
                if (!found) {
                    throw new PALException("Param " + name
                            + " bound to family " + familyName
                            + ", but action isn't bound to " + familyName);
                }

                /* Does that family contain the role we bind to? */
                ActionFamilyDef famDef = (ActionFamilyDef) getActionModel()
                        .getType(familyName);
                if (famDef.getParamNum(role) == -1) {
                    throw new PALException("Param " + name
                            + "bound to non-existent role " + role
                            + " of family " + familyName);
                }

                Set<String> familyBindings = bindings.get(familyName);
                if (familyBindings == null) {
                    familyBindings = new HashSet<String>();
                    bindings.put(familyName, familyBindings);
                }
                familyBindings.add(role);
            }
            /* Convert it to an ATR structure. */
            Map<String, ATRTerm> bindingsMap = new HashMap<String, ATRTerm>();
            for(Entry<TypeName, Set<String>> entry : bindings.entrySet()) {
                TypeName key = entry.getKey();
                Set<String> rolesStr = entry.getValue();
                List<ATRTerm> rolesTerm = new ArrayList<ATRTerm>();
                for (String role : rolesStr) {
                    ATRLiteral roleLit = ctrBuilder.createLiteral(role, null);
                    rolesTerm.add(roleLit);
                }
                Collections.sort(rolesTerm, termSorter);
                bindingsMap.put(key.getFullName(),
                        ctrBuilder.createList(rolesTerm));
            }
            propMap.put(TypeUtil.PARAM_ROLE, ctrBuilder.createMap(bindingsMap));

            /* Parameter description. */
            String descr = paramXml.getDescription();
            if (descr == null) {
                descr = "";
            }
            ATRLiteral descrTerm = ctrBuilder.createLiteral(descr, null);
            propMap.put(TypeUtil.PARAM_DESCRIPTION, descrTerm);

            /* Other metadata. */
            for (MetadataType meta : paramXml.getMetadata()) {
                String key = meta.getKey();
                String value = meta.getValue();
                ATRLiteral valueTerm = ctrBuilder.createLiteral(value, null);
                propMap.put(key, valueTerm);
            }

            /*
             * Finally, add the new properties map for this parameter to the
             * parent action's props.
             */
            ATRMap props = ctrBuilder.createMap(propMap);
            actionPropMap.put("$" + name, props);
        }
    }

    /**
     * Given a value in ATR form, sort its elements if it's an unordered
     * collection. If it contains nested collections, sort those too. This is
     * done so that {@code setGen(a,b)} will compare equal to
     * {@code setGen(b,a)}.
     *
     * @param result
     *            the value to sort
     * @param version
     *            the action model version to use if an unqualified name must be
     *            qualified
     * @param namespace
     *            the action model namespace to use if an unqualified name must
     *            be qualified
     * @return a sorted copy
     */
    private ATRTerm normalize(ATRTerm value,
                              String version,
                              String namespace) {
        ATRTerm result = value;
        /*
         * First sort nested collections, because their sorted state may affect
         * the sort of the containing collection.
         */
        if (result instanceof ATRList) {
            List<ATRTerm> newList = new ArrayList<ATRTerm>();
            for (ATRTerm term : ((ATRList) result).getTerms()) {
                ATRTerm sortedTerm = normalize(term, version, namespace);
                newList.add(sortedTerm);
            }
            result = ctrBuilder.createList(newList);
        }
        if (result instanceof ATRFunction) {
            ATRFunction func = (ATRFunction) result;
            String functor = func.getFunctor();
            List<ATRTerm> newList = new ArrayList<ATRTerm>();
            for (ATRTerm term : func.getElements()) {
                ATRTerm sortedTerm = normalize(term, version, namespace);
                newList.add(sortedTerm);
            }
            result = ctrBuilder.createFunction(functor, newList);
        }

        /* Now sort this thing, if it's an unsorted collection. */
        if (result instanceof ATRFunction) {
            ATRFunction func = (ATRFunction) result;
            String functor = func.getFunctor();
            if (functor.equals("setGen") || functor.equals("bagGen")) {
                List<ATRTerm> list = new ArrayList<ATRTerm>();
                list.addAll(func.getElements());
                Collections.sort(list, termSorter);
                result = ctrBuilder.createFunction(functor, list);
            }

            /*
             * If it's a structure literal, normalize the first argument, which
             * is the name of the type, by making it a fully-qualified name.
             */
            if (functor.equals(StructDef.STRUCT_FUNCTOR)) {
                List<? extends ATRTerm> list = func.getElements();
                ATRTerm nameTerm = list.get(0);
                if (nameTerm instanceof ATRLiteral) {
                    List<ATRTerm> newList = new ArrayList<ATRTerm>();
                    ATRLiteral nameLit = (ATRLiteral) nameTerm;
                    String nameStr = nameLit.getString();
                    TypeName name = TypeNameFactory.makeName(nameStr, version,
                            namespace);
                    nameLit = new CTRLiteral(name.getFullName());
                    newList.add(nameLit);
                    for (int i = 1; i < list.size(); i++) {
                        newList.add(list.get(i));
                    }
                    result = ctrBuilder.createFunction(functor, newList);
                }
            }
        }

        return result;
    }

    /**
     * Translate from an XML representation of constraint binding to the ATRTerm
     * representation. Because the XML can contain unqualified names -- names
     * not prefixed by namespace or version -- and ATR always contains qualified
     * names, this method needs to know how to resolve unqualified names.
     *
     * @param xmlCons
     *            the XML representation of a constraint binding
     * @param version
     *            the version of the action model to use for unqualified
     *            constraints
     * @param namespace
     *            the namespace of the action model to use for unqualified
     *            constraints
     * @return an ATR representation of the same constraint binding
     * @throws PALException
     *             if a referenced constraint can't be retrieved from the action
     *             model
     */
    ATRNoEvalTerm atrConstraints(ConstraintsType xmlCons,
                                 String version,
                                 String namespace)
            throws PALException {
        ATRTerm atrCons;
        if (xmlCons == null) {
            atrCons = ctrBuilder.createNull();
        } else if (xmlCons.getConstraint() != null) {
            atrCons = atrConstraint(xmlCons.getConstraint(), version, namespace);
        } else if (xmlCons.getAnd() != null) {
            atrCons = atrConstraint(xmlCons.getAnd(), version, namespace);
        } else if (xmlCons.getOr() != null) {
            atrCons = atrConstraint(xmlCons.getOr(), version, namespace);
        } else if (xmlCons.getNot() != null) {
            atrCons = atrConstraint(xmlCons.getNot(), version, namespace);
        } else {
            // No nested element means empty constraints.
            atrCons = ctrBuilder.createNull();
        }

        ATRNoEvalTerm result = ctrBuilder.createNoEval(atrCons);
        return result;
    }

    private ATRFunction atrConstraint(ConstraintType xml,
                                      String version,
                                      String namespace)
            throws PALException {
        String name = xml.getName();
        TypeName typeName = TypeNameFactory.makeName(name, version, namespace);
        ConstraintDef consDef = (ConstraintDef) getActionModel().getType(
                typeName);
        if (consDef == null) {
            throw new PALException("Can't find constraint declaration "
                    + typeName);
        }
        ATRTerm[] params = new ATRTerm[consDef.size()];
        for (ConstraintParamType xmlParam : xml.getParam()) {
            String consParamName = xmlParam.getId();
            int consParamPos = consDef.getFieldNum(consParamName);
            ATRTerm paramBinding;
            if (xmlParam.getInputRef() != null) {
                paramBinding = atrParamBinding(xmlParam.getInputRef());
            } else if (xmlParam.getInputList() != null) {
                paramBinding = atrParamBinding(xmlParam.getInputList());
            } else if (xmlParam.getInputUnknown() != null) {
                paramBinding = atrParamBinding(xmlParam.getInputUnknown());
            } else if (xmlParam.getInputConstant() != null) {
                paramBinding = atrParamBinding(xmlParam.getInputConstant());
            } else if (xmlParam.getInputFunc() != null) {
                paramBinding = atrParamBinding(xmlParam.getInputFunc());
            } else {
                throw new IllegalArgumentException("No element in param: "
                        + xmlParam);
            }
            params[consParamPos] = paramBinding;
        }
        ATRFunction result = ctrBuilder.createFunction(typeName.getFullName(),
                Arrays.asList(params));
        return result;
    }

    private ATRFunction atrConstraint(ConstraintAndType xml,
                                      String version,
                                      String namespace)
            throws PALException {
        List<ATRFunction> exprs = new ArrayList<ATRFunction>();
        for (Object o : xml.getAndOrOrOrNot()) {
            if (o instanceof ConstraintType) {
                exprs.add(atrConstraint((ConstraintType) o, version, namespace));
            } else if (o instanceof ConstraintAndType) {
                exprs.add(atrConstraint((ConstraintAndType) o, version,
                        namespace));
            } else if (o instanceof ConstraintOrType) {
                exprs.add(atrConstraint((ConstraintOrType) o, version,
                        namespace));
            } else if (o instanceof ConstraintNotType) {
                exprs.add(atrConstraint((ConstraintNotType) o, version,
                        namespace));
            } else {
                throw new IllegalArgumentException("Unknown element of and: "
                        + o);
            }
        }
        ATRFunction result = ctrBuilder.createFunction("&&", exprs);
        return result;
    }

    private ATRFunction atrConstraint(ConstraintOrType xml,
                                      String version,
                                      String namespace)
            throws PALException {
        List<ATRFunction> exprs = new ArrayList<ATRFunction>();
        for (Object o : xml.getAndOrOrOrNot()) {
            if (o instanceof ConstraintType) {
                exprs.add(atrConstraint((ConstraintType) o, version, namespace));
            } else if (o instanceof ConstraintAndType) {
                exprs.add(atrConstraint((ConstraintAndType) o, version,
                        namespace));
            } else if (o instanceof ConstraintOrType) {
                exprs.add(atrConstraint((ConstraintOrType) o, version,
                        namespace));
            } else if (o instanceof ConstraintNotType) {
                exprs.add(atrConstraint((ConstraintNotType) o, version,
                        namespace));
            } else {
                throw new IllegalArgumentException("Unknown element of or: "
                        + o);
            }
        }
        ATRFunction result = ctrBuilder.createFunction("||", exprs);
        return result;
    }

    private ATRFunction atrConstraint(ConstraintNotType xml,
                                      String version,
                                      String namespace)
            throws PALException {
        List<ATRFunction> exprs = new ArrayList<ATRFunction>();
        if (xml.getConstraint() != null) {
            exprs.add(atrConstraint(xml.getConstraint(), version, namespace));
        } else if (xml.getAnd() != null) {
            exprs.add(atrConstraint(xml.getAnd(), version, namespace));
        } else if (xml.getOr() != null) {
            exprs.add(atrConstraint(xml.getOr(), version, namespace));
        } else if (xml.getNot() != null) {
            exprs.add(atrConstraint(xml.getNot(), version, namespace));
        } else {
            throw new IllegalArgumentException("Missing element in not: " + xml);
        }
        ATRFunction result = ctrBuilder.createFunction("!", exprs);
        return result;
    }

    private ATRTerm atrParamBinding(ConstraintInputRefType inputRef) {
        String actParamName = inputRef.getRef();
        ATRVariable result = ctrBuilder.createVariable(actParamName);
        return result;
    }

    private ATRTerm atrParamBinding(ConstraintInputUnknownType inputUnk) {
        ATRVariable result = ctrBuilder.createVariable("_");
        return result;
    }

    private ATRTerm atrParamBinding(ConstraintInputConstantType inputConst) {
        ATRLiteral result = ctrBuilder.createLiteral(inputConst.getValue(), null);
        return result;
    }

    private ATRTerm atrParamBinding(ConstraintInputListType xmlList) {
        List<?> inputList = xmlList.getInputListOrInputRefOrInputUnknown();
        List<ATRTerm> boundParams = new ArrayList<ATRTerm>();
        for (Object o : inputList) {
            if (o instanceof ConstraintInputRefType) {
                boundParams.add(atrParamBinding((ConstraintInputRefType) o));
            } else if (o instanceof ConstraintInputUnknownType) {
                boundParams
                        .add(atrParamBinding((ConstraintInputUnknownType) o));
            } else if (o instanceof ConstraintInputListType) {
                boundParams.add(atrParamBinding((ConstraintInputListType) o));
            } else if (o instanceof ConstraintInputConstantType) {
                boundParams
                        .add(atrParamBinding((ConstraintInputConstantType) o));
            } else if (o instanceof ConstraintInputFuncType) {
                boundParams.add(atrParamBinding((ConstraintInputFuncType) o));
            } else {
                throw new IllegalArgumentException("Unknown list element: " + o);
            }
        }
        ATRList result = ctrBuilder.createList(boundParams);
        return result;
    }

    private ATRTerm atrParamBinding(ConstraintInputFuncType inputFunc) {
        String functor = inputFunc.getFunction();
        List<ATRTerm> atrArgs = new ArrayList<ATRTerm>();
        if (inputFunc.getInputConstant() != null) {
            atrArgs.add(atrParamBinding(inputFunc.getInputConstant()));
        }
        if (inputFunc.getInputFunc() != null) {
            atrArgs.add(atrParamBinding(inputFunc.getInputFunc()));
        }
        if (inputFunc.getInputList() != null) {
            atrArgs.add(atrParamBinding(inputFunc.getInputList()));
        }
        if (inputFunc.getInputRef() != null) {
            atrArgs.add(atrParamBinding(inputFunc.getInputRef()));
        }
        if (inputFunc.getInputUnknown() != null) {
            atrArgs.add(atrParamBinding(inputFunc.getInputUnknown()));
        }
        ATRFunction result = ctrBuilder.createFunction(functor, atrArgs);
        return result;
    }

    /**
     * Translate from an ATR representation of constraint bindings to the XML
     * representation. Although it's legal for the XML to contain unqualified
     * constraint names, all names produced by this method will be fully
     * qualified.
     *
     * @param atrCons
     *            ATR representation of the constraint bindings
     * @param actDef
     *            the action definition which these constraint bindings apply to
     * @return an XML representation of the same constraint bindings
     * @throws PALException
     *             if a referenced constraint can't be found in the action model
     */
    ConstraintsType xmlConstraints(ATRNoEvalTerm atrCons,
                                   ActionDef actDef)
            throws PALException {
        ConstraintsType result;

        if (atrCons == null) {
            result = null;
        } else {
            result = new ConstraintsType();
            result.setVersion(new BigDecimal(1.0));
            ATRTerm atrInnerTerm = atrCons.getInnerTerm();
            if (atrInnerTerm instanceof ATRNull) {
                // Return the empty Constraints as-is.
            } else {
                ATRFunction atrInnerFunc = (ATRFunction) atrInnerTerm;
                Object xml = xmlConstraint(atrInnerFunc, actDef);

                if (xml instanceof ConstraintType) {
                    result.setConstraint((ConstraintType) xml);
                } else if (xml instanceof ConstraintAndType) {
                    result.setAnd((ConstraintAndType) xml);
                } else if (xml instanceof ConstraintOrType) {
                    result.setOr((ConstraintOrType) xml);
                } else if (xml instanceof ConstraintNotType) {
                    result.setNot((ConstraintNotType) xml);
                } else {
                    throw new RuntimeException("Unknown constraint type " + xml);
                }
            }
        }

        return result;
    }

    private Object xmlConstraint(ATRFunction atrCons,
                                 ActionDef actDef)
            throws PALException {
        Object result;

        String functor = atrCons.getFunctor();
        if (functor.equals("!")) {
            ConstraintNotType not = new ConstraintNotType();
            Object subExpr = xmlConstraint((ATRFunction) atrCons.getElements()
                    .get(0), actDef);
            if (subExpr instanceof ConstraintType) {
                not.setConstraint((ConstraintType) subExpr);
            } else if (subExpr instanceof ConstraintAndType) {
                not.setAnd((ConstraintAndType) subExpr);
            } else if (subExpr instanceof ConstraintOrType) {
                not.setOr((ConstraintOrType) subExpr);
            } else if (subExpr instanceof ConstraintNotType) {
                not.setNot((ConstraintNotType) subExpr);
            } else {
                throw new RuntimeException("Unknown subExpr generated: "
                        + subExpr);
            }
            result = not;
        } else if (functor.equals("||") || functor.equals("&&")) {
            List<Object> subExprs;
            if (functor.equals("||")) {
                ConstraintOrType or = new ConstraintOrType();
                result = or;
                subExprs = or.getAndOrOrOrNot();
            } else {
                ConstraintAndType and = new ConstraintAndType();
                result = and;
                subExprs = and.getAndOrOrOrNot();
            }
            for (ATRTerm subTerm : atrCons.getElements()) {
                ATRFunction subFunc = (ATRFunction) subTerm;
                subExprs.add(xmlConstraint(subFunc, actDef));
            }
        } else {
            ConstraintType cons = new ConstraintType();
            SimpleTypeName typeName = (SimpleTypeName) TypeNameFactory
                    .makeName(functor);
            // Intentionally removing the namespace prefix, to be symmetrical
            // with how our inverse method (atrConstraint) works.
            cons.setName(typeName.getSimpleName());
            ConstraintDef constraintDef = (ConstraintDef) bridge
                    .getActionModel().getType(typeName);
            List<ConstraintParamType> params = cons.getParam();
            List<? extends ATRTerm> terms = atrCons.getElements();
            for (int termNum = 0; termNum < terms.size(); termNum++) {
                ATRTerm term = terms.get(termNum);
                ConstraintParamType param = new ConstraintParamType();
                String paramName = constraintDef.getFieldName(termNum);
                param.setId(paramName); // constraint param name
                Object paramBinding = xmlParamBinding(term, actDef);
                assignParamBinding(param, paramBinding);
                params.add(param);
            }
            result = cons;
        }

        return result;
    }

    private Object xmlParamBinding(ATRTerm term,
                                   ActionDef actDef) {
        Object result;

        if (term instanceof ATRVariable) {
            ATRVariable var = (ATRVariable) term;
            String varName = var.getVariableName();
            if (varName.equals("_")
                    || !actDef.isInputParam(actDef.getParamNum(varName))) {
                // inputUnknown
                result = new ConstraintInputUnknownType();
            } else {
                ConstraintInputRefType ref = new ConstraintInputRefType();
                ref.setRef(var.getVariableName()); // action param name
                result = ref;
            }
        } else if (term instanceof ATRList) {
            ATRList list = (ATRList) term;
            ConstraintInputListType xmlList = new ConstraintInputListType();
            List<Object> innerList = xmlList
                    .getInputListOrInputRefOrInputUnknown();
            for (ATRTerm member : list.getTerms()) {
                innerList.add(xmlParamBinding(member, actDef));
            }
            result = xmlList;
        } else if (term instanceof ATRLiteral) {
            ATRLiteral lit = (ATRLiteral) term;
            ConstraintInputConstantType inputConst = new ConstraintInputConstantType();
            inputConst.setValue(lit.getValue().toString());
            result = inputConst;
        } else if (term instanceof ATRFunction) {
            ATRFunction func = (ATRFunction) term;
            ConstraintInputFuncType inputFunc = new ConstraintInputFuncType();
            inputFunc.setFunction(func.getFunctor());
            ATRTerm atrParam = func.getElements().get(0);
            Object paramObj = xmlParamBinding(atrParam, actDef);
            if (paramObj instanceof ConstraintInputListType) {
                inputFunc.setInputList((ConstraintInputListType) paramObj);
            } else if (paramObj instanceof ConstraintInputRefType) {
                inputFunc.setInputRef((ConstraintInputRefType) paramObj);
            } else if (paramObj instanceof ConstraintInputUnknownType) {
                inputFunc
                        .setInputUnknown((ConstraintInputUnknownType) paramObj);
            } else if (paramObj instanceof ConstraintInputConstantType) {
                inputFunc
                        .setInputConstant((ConstraintInputConstantType) paramObj);
            } else if (paramObj instanceof ConstraintInputFuncType) {
                inputFunc.setInputFunc((ConstraintInputFuncType) paramObj);
            } else {
                throw new RuntimeException("Unknown param binding type: "
                        + paramObj);
            }
            result = inputFunc;
        } else {
            throw new IllegalArgumentException("Unknown param term " + term);
        }

        return result;
    }

    private void assignParamBinding(ConstraintParamType param,
                                    Object binding) {
        if (binding instanceof ConstraintInputListType) {
            param.setInputList((ConstraintInputListType) binding);
        } else if (binding instanceof ConstraintInputRefType) {
            param.setInputRef((ConstraintInputRefType) binding);
        } else if (binding instanceof ConstraintInputUnknownType) {
            param.setInputUnknown((ConstraintInputUnknownType) binding);
        } else if (binding instanceof ConstraintInputConstantType) {
            param.setInputConstant((ConstraintInputConstantType) binding);
        } else if (binding instanceof ConstraintInputFuncType) {
            param.setInputFunc((ConstraintInputFuncType) binding);
        } else {
            throw new RuntimeException("Unknown param binding type: " + binding);
        }
    }

    /**
     * Trim whitespace off the ends and out of the middle of the provided
     * string. Specifically targeted at the case of multi-line text inside XML
     * tags.
     *
     * <pre>
     * &lt;xml&gt;
     *     one two
     *     three
     * &lt;/xml&gt;
     * </pre>
     */
    private static String trimWhitespace(String text) {
        if (text == null) {
            return null;
        }
        String result = text.trim();
        result = result.replaceAll("[\\s]*[\n\r][\\s]*", " ");
        return result;
    }

    private static class ATRTermSorter
            implements Comparator<ATRTerm> {
        @Override
        public int compare(ATRTerm o1,
                           ATRTerm o2) {
            String str1 = ATRSyntax.toSource(o1);
            String str2 = ATRSyntax.toSource(o2);
            return str1.compareTo(str2);
        }
    }
}
