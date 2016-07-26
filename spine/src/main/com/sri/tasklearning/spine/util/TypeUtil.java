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

// $Id: TypeUtil.java 7750 2016-07-26 16:53:01Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.decl.ATRFunctionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRSigDecl;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.Application;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.ApplicationSubType;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.Structure;
import com.sri.ai.lumen.atr.learning.ATRDemonstratedAction;
import com.sri.ai.lumen.atr.learning.ATRIdiomTemplateAction;
import com.sri.ai.lumen.atr.learning.impl.IdiomTemplateFactory;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRList;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.impl.CTRLiteral;
import com.sri.ai.lumen.atr.type.Type;
import com.sri.ai.lumen.atr.type.Type.Category;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameExpr;
import com.sri.pal.common.TypeNameFactory;

public class TypeUtil {
    public static final String DESCRIPTION = "description";

    /* To distinguish actions, action families, and idioms: */
    public static final String TYPE = "PAL type";
    public static final String TYPE_FAMILY = "family";
    public static final String TYPE_IDIOM = "idiom";
    public static final SimpleTypeName GESTURE_END_NAME = new SimpleTypeName(
            "GestureEnd", "_", "_");

    // For actions:
    public static final String ACTION_FAMILIES = "actionFamilies";
    public static final String PARENT = "parent";
    public static final String CONSTRAINTS = "constraints";
    public static final String ACTION_CATEGORY = "category";
    public static final String BENIGN = "benign";
    public static final String TRANSIENT = "isTransient";
    public static final String NEEDS_PRELOAD = "needsPreload";
    public static final String PARAM_CLASS = "class";
    public static final String PARAM_CLASS_VIOLABLE = "violable";
    public static final String PARAM_DESCRIPTION = "description";
    public static final String PARAM_ROLE = "role";
    public static final String EXECUTEJ = "executeJ";
    private static final String[] ACTION_RESERVED_KEYS_ARRAY = {
            ACTION_FAMILIES, PARENT, CONSTRAINTS, ACTION_CATEGORY, TRANSIENT,
            PARAM_CLASS, PARAM_CLASS_VIOLABLE, PARAM_DESCRIPTION, PARAM_ROLE };
    public static final List<String> ACTION_RESERVED_KEYS = Arrays
            .asList(ACTION_RESERVED_KEYS_ARRAY);
    public static final String COLLAPSIBLE = "collapsible";
    public static final String COLLAPSIBLE_INSIDE_GESTURE = "insideGesture";
    public static final String COLLAPSIBLE_OUTSIDE_GESTURE = "outsideGesture";

    // For idioms:
    public static final String IDIOM_TEMPLATES = "idiomTemplates";

    // For constraints:
    public static final String CONSTRAINT_DESCRIPTIONS = "descriptions";

    // For structs:
    public static final String OPAQUE = "opaque";
    public static final String STRUCT_DYNAMIC = "dynamic";
    public static final String STRUCT_CONSTANT = "constantFields";

    // For lists:
    public static final String PERMUTABLE = "permutable";

    // For collection types (list, set, bag, struct):
    public static final String GENERALIZE_SINGLETON = "generalizeSingleton";
    public static final String GENERALIZE_UNSUPPORTED = "generalizeUnsupported";
    public static final String CONSTRUCT_MAX_INPUTS = "constructMaxInputs";
    private static final String[] TYPE_RESERVED_KEYS_ARRAY = {
            GENERALIZE_SINGLETON, GENERALIZE_UNSUPPORTED, CONSTRUCT_MAX_INPUTS,
            PERMUTABLE, OPAQUE, STRUCT_DYNAMIC, STRUCT_CONSTANT, DESCRIPTION,
            BENIGN };
    public static final List<String> TYPE_RESERVED_KEYS = Arrays
            .asList(TYPE_RESERVED_KEYS_ARRAY);

    /**
     * If the string contains an XML action model which contains at least one
     * action definition.
     */
    public static boolean isActionString(String typeStr) {
        if (isIdiomString(typeStr)) {
            return false;
        }
        return typeStr.matches("(?s).*<actionModel\\b.*<action\\b.*");
    }

    /**
     * If the string contains an XML TaskModel element (defined in
     * ActionModel.xsd).
     */
    public static boolean isProcedureString(String typeStr) {
        return typeStr.matches("(?s).*<TaskModel\\b.*");
    }

    public static boolean isActionFamilyString(String typeStr) {
        return typeStr.matches("(?s).*<actionModel\\b.*<family\\b.*");
    }

    public static boolean isIdiomString(String typeStr) {
        return typeStr.matches("(?s).*<actionModel\\b.*<idiom\\b.*");
    }

    public static boolean isConstraintString(String typeStr) {
        return typeStr.matches("(?s).*<actionModel\\b.*<constraintDecl\\b.*");
    }

    /**
     * Including ATRLiterals like {@code integer} or {@code list<string>}.
     */
    public static boolean isType(ATR atr) {
        if (atr instanceof ATRTypeDeclaration) {
            return true;
        } else if (atr instanceof ATRLiteral) {
            /*
             * We could try to decide if it's a known primitive type or a valid
             * collection name.
             */
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if the given object represents an action, as opposed to a data
     * type declaration. Procedures are actions.
     *
     * @param type
     *            a type or action declaration
     * @return true iff the given object is an action or procedure declaration
     */
    public static boolean isAction(ATR type) {
        if (isActionFamily(type)) {
            return false;
        } else if (isIdiom(type)) {
            return false;
        } else {
            return type instanceof ATRActionDeclaration;
        }
    }

    public static boolean isTransient(ATRActionDeclaration type) {
        ATRMap props = type.getProperties();
        ATRLiteral transTerm = (ATRLiteral) props.get(TRANSIENT);
        if (transTerm == null) {
            return true;
        } else {
            String transStr = transTerm.getString();
            boolean isTransient = Boolean.parseBoolean(transStr);
            return isTransient;
        }
    }

    public static boolean needsPreload(ATRActionDeclaration type) {
        ATRMap props = type.getProperties();
        ATRLiteral preloadTerm = (ATRLiteral) props.get(NEEDS_PRELOAD);
        if (preloadTerm == null) {
            return false;
        } else {
            String preloadStr = preloadTerm.getString();
            boolean needsPreload = Boolean.parseBoolean(preloadStr);
            return needsPreload;
        }
    }

    public static boolean isProcedure(ATR type) {
        if (!isAction(type)) {
            return false;
        }

        ATRActionDeclaration act = (ATRActionDeclaration) type;
        ATRTask task = act.getExecute();
        if (task == null) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean isActionFamily(ATR type) {
        if (!(type instanceof ATRActionDeclaration)) {
            return false;
        }
        ATRActionDeclaration act = (ATRActionDeclaration) type;
        ATRMap props = act.getProperties();
        ATRTerm term = props.get(TYPE);
        if (term == null) {
            return false;
        }
        ATRLiteral lit = (ATRLiteral) term;
        String str = lit.getString();
        if(str.equals(TYPE_FAMILY)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isIdiom(ATR type) {
        if (!(type instanceof ATRActionDeclaration)) {
            return false;
        }
        ATRActionDeclaration act = (ATRActionDeclaration) type;
        ATRMap props = act.getProperties();
        ATRTerm term = props.get(TYPE);
        if (term == null) {
            return false;
        }
        ATRLiteral lit = (ATRLiteral) term;
        String str = lit.getString();
        if(str.equals(TYPE_IDIOM)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Maybe it's an ATRLiteral like {@code list<myType>}. Maybe it's an
     * ATRTypeDeclaration which claims to be equivalent to {@code list<myType>}.
     */
    public static boolean isCollection(ATR type) {
        if (type instanceof ATRLiteral) {
            TypeName typeName = getName(type);
            return typeName instanceof TypeNameExpr;
        } else if (type instanceof ATRTypeDeclaration) {
            ATRTypeDeclaration typeDecl = (ATRTypeDeclaration) type;
            ATRList equivs = typeDecl.optEquivalentTypes();
            if (equivs != null) {
                for (ATRTerm equivTerm : equivs.getTerms()) {
                    if (isCollection(equivTerm)) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public static boolean isBag(ATR atr) {
        return isCollection(atr)
                && getCollectionType(atr).equals(Type.Category.BAG.prefix);
    }

    public static boolean isList(ATR atr) {
        return isCollection(atr)
                && getCollectionType(atr).equals(Type.Category.LIST.prefix);
    }

    public static boolean isPermutable(ATRTypeDeclaration atr) {
        if (isList(atr)) {
            ATRMap props = atr.optProperties();
            if (props != null) {
                ATRLiteral permTerm = (ATRLiteral) props.get(PERMUTABLE);
                if (permTerm != null) {
                    return Boolean.parseBoolean(permTerm.getString());
                }
            }
        }
        return false;
    }

    public static boolean isSet(ATR atr) {
        return isCollection(atr)
                && getCollectionType(atr).equals(Type.Category.SET.prefix);
    }

    public static boolean isNullable(ATR atr) {
        return isCollection(atr)
                && getCollectionType(atr).equals(Type.Category.NULLABLE.prefix);
    }

    public static boolean isStruct(ATR atr) {
        if (atr instanceof ATRTypeDeclaration) {
            ATRTypeDeclaration decl = (ATRTypeDeclaration) atr;
            return decl.getTypeCategory() == Category.STRUCTURE_TYPE;
        } else {
            return false;
        }
    }

    public static boolean isAlias(ATR atr) {
        if (atr instanceof ATRTypeDeclaration) {
            ATRTypeDeclaration decl = (ATRTypeDeclaration) atr;
            return decl.getTypeCategory() == Category.INCOMPLETE;
        } else {
            return false;
        }
    }

    public static boolean isConstraintDecl(ATR atr) {
        return atr instanceof ATRFunctionDeclaration;
    }

    /**
     * Is it either an application type or an application sub type?
     */
    public static boolean isCustom(ATR atr) {
        if (atr instanceof ATRTypeDeclaration) {
            ATRTypeDeclaration decl = (ATRTypeDeclaration) atr;
            Category cat = decl.getTypeCategory();
            return cat == Category.PRIMITIVE_BASE
                    || cat == Category.PRIMITIVE_SUBTYPE;
        } else {
            return false;
        }
    }

    public static boolean isCustomSubType(ATR atr) {
        if (atr instanceof ATRTypeDeclaration) {
            ATRTypeDeclaration decl = (ATRTypeDeclaration) atr;
            return decl.getTypeCategory() == Category.PRIMITIVE_SUBTYPE;
        } else {
            return false;
        }
    }

    public static boolean isEnumerated(ATR atr) {
        if (atr instanceof ATRTypeDeclaration) {
            ATRTypeDeclaration decl = (ATRTypeDeclaration) atr;
            return decl.getTypeCategory() == Category.ENUMERATED_TYPE;
        } else {
            return false;
        }
    }

    public static boolean isPrimitive(ATR atr) {
        if (atr instanceof ATRLiteral) {
            TypeName name = getName(atr);
            return TypeNameFactory.isPrimitive(name);
        }
        return false;
    }

    public static boolean isGesture(ATRDemonstratedAction action) {
        return action.optBody() != null;
    }

    public static TypeName getName(ATR type) {
        if (type instanceof ATRSigDecl) {
            ATRSigDecl sigDecl = (ATRSigDecl) type;
            String name = sigDecl.getSignature().getFunctor();
            return (SimpleTypeName) TypeNameFactory.makeName(name);
        } else if (type instanceof ATRTypeDeclaration) {
            ATRTypeDeclaration typeDecl = (ATRTypeDeclaration) type;
            String nameStr = typeDecl.getTypeName().getString();
            return (SimpleTypeName) TypeNameFactory.makeName(nameStr);
        } else if (type instanceof ATRLiteral) {
            String nameStr = ((ATRLiteral) type).getString();
            return TypeNameFactory.makeName(nameStr);
        } else {
            throw unknown(type);
        }
    }

    public static SimpleTypeName getName(ATRDecl type) {
        return (SimpleTypeName) getName((ATR) type);
    }

    /**
     * Fetches the names of all the types which the given type directly depends
     * on. Does not fetch transitive dependencies. Also does not fetch
     * constraint definitions. In many cases, the caller will want to filter out
     * TypeNameExpr items after building the full set of transitive
     * dependencies.
     *
     * @param type
     *            the type for which to fetch dependencies. This could be a
     *            type, action, or ATRLiteral such as a primitive type or
     *            collection.
     * @return types which the given type depends on in order to be
     *         instantiated.
     */
    public static Set<TypeName> getRequiredTypes(ATRDecl type) {
        Set<TypeName> result = new HashSet<TypeName>();

        // Add parent type
        if (isAction(type)) {
            SimpleTypeName parent = getParent((ATRActionDeclaration) type);
            if (parent != null) {
                result.add(parent);
            }
        } else if (isStruct(type)) {
            SimpleTypeName parent = getParent((Structure) type);
            if (parent != null) {
                result.add(parent);
            }
        } else if (isCustomSubType(type)) {
            SimpleTypeName parent = getParent((ApplicationSubType) type);
            if (parent != null) {
                result.add(parent);
            }
        }

        // Add sub-type
        if (isEnumerated(type)) {
            ATRTypeDeclaration.Enumerated enumType = (ATRTypeDeclaration.Enumerated) type;
            result.addAll(getSubTypes(enumType));
        }

        if (isProcedure(type)) {
            // Add nested actions.
            result.addAll(SubTasksFinder
                    .findSubTasks((ATRActionDeclaration) type));
        }

        // Add field/parameter types.
        if (isAction(type)) {
            result.addAll(getParamTypes((ATRActionDeclaration) type));
        } else if (isStruct(type)) {
            result.addAll(getFieldTypes((Structure) type));
        }

        /* Add action families. */
        if (isAction(type)) {
            result.addAll(getActionFamilies((ATRActionDeclaration) type));
        }

        if (isCollection(type)) {
            // Add element type.
            result.add(getElementType(type));
        }

        if (isIdiom(type)) {
            // Add actions or families in its templates.
            result.addAll(getTemplateActions((ATRActionDeclaration) type));
        }

        return result;
    }

    public static Set<SimpleTypeName> getTemplateActions(ATRActionDeclaration idiom) {
        Set<SimpleTypeName> result = new HashSet<SimpleTypeName>();
        ATRMap props = idiom.getProperties();
        ATRList templatesTerm = (ATRList) props.get(TypeUtil.IDIOM_TEMPLATES);
        for (ATRTerm templateTerm : templatesTerm.getTerms()) {
            ATRList templateList = (ATRList) templateTerm;
            ATRList actionList = (ATRList) templateList.get(2);
            for (ATRTerm actionTerm : actionList.getTerms()) {
                ATRLiteral actionLit = (ATRLiteral) actionTerm;
                String actionStr = actionLit.getString();
                ATRIdiomTemplateAction action;
                try {
                    action = (ATRIdiomTemplateAction) IdiomTemplateFactory
                            .parseStatement(actionStr);
                } catch (LumenSyntaxError e) {
                    throw new IllegalArgumentException(
                            "Cannot parse as ATRIdiomTemplateAction: "
                                    + actionStr);
                }
                String nameStr = action.getActionReference();
                SimpleTypeName actionName = (SimpleTypeName) TypeNameFactory
                        .makeName(nameStr);
                result.add(actionName);
            }
        }
        return result;
    }

    public static Set<SimpleTypeName> getActionFamilies(ATRActionDeclaration action) {
        Set<SimpleTypeName> result = new HashSet<SimpleTypeName>();
        ATRMap props = action.getProperties();
        ATRList list = (ATRList) props.get(ACTION_FAMILIES);
        if (list == null) {
            return result;
        }
        for (ATRTerm term : list.getTerms()) {
            ATRLiteral lit = (ATRLiteral) term;
            String str = lit.getString();
            SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName(str);
            result.add(name);
        }
        return result;
    }

    public static List<TypeName> getParamTypes(ATRActionDeclaration act) {
        List<TypeName> result = new ArrayList<TypeName>();
        ATRSig sig = act.getSignature();
        for (ATRParameter param : sig.getElements()) {
            ATRLiteral typeLit = param.getType();
            TypeName typeName = getName(typeLit);
            result.add(typeName);
        }
        return result;
    }

    public static List<TypeName> getFieldTypes(Structure struct) {
        List<TypeName> result = new ArrayList<TypeName>();
        for (ATRTerm typeTerm : struct.getFieldTypes().getTerms()) {
            result.add(getName(typeTerm));
        }
        return result;
    }

    public static Set<TypeName> getEquivalentTypeNames(ATRTypeDeclaration type) {
        Set<TypeName> result = new HashSet<TypeName>();
        ATRList equivList = type.optEquivalentTypes();
        if (equivList != null) {
            for (ATRTerm term : equivList.getTerms()) {
                result.add(getName(term));
            }
        }
        return result;
    }

    /**
     * Gets the name of the parent action, from which this one inherits.
     */
    public static SimpleTypeName getParent(ATRActionDeclaration atr) {
        SimpleTypeName result = null;
        ATRMap props = atr.getProperties();
        if (props != null) {
            ATRLiteral parentTerm = (ATRLiteral) props.get(PARENT);
            if (parentTerm != null) {
                String parentStr = parentTerm.getString();
                result = (SimpleTypeName) TypeNameFactory.makeName(parentStr);
            }
        }
        return result;
    }

    public static SimpleTypeName getParent(Structure atr) {
        ATRLiteral lit = atr.optParentType();
        if (lit != null) {
            return (SimpleTypeName) getName(lit);
        } else {
            return null;
        }
    }

    /*
     * TODO It would be nice if this argument could be a common ancestor of both
     * Application and ApplicationSubType.
     */
    public static SimpleTypeName getParent(ApplicationSubType atr) {
        ATRLiteral lit = atr.getParentType();
        return (SimpleTypeName) getName(lit);
    }

    public static List<SimpleTypeName> getSubTypes(ATRTypeDeclaration.Enumerated enumType) {
        // Get version and namespace of the parent type, to use in case the child names aren't fully qualified.
        String parentNameStr = enumType.getTypeName().getString();
        SimpleTypeName parentName = (SimpleTypeName) TypeNameFactory.makeName(parentNameStr);
        String version = parentName.getVersion();
        String namespace = parentName.getNamespace();

        List<SimpleTypeName> result = new ArrayList<>();
        for (ATRTerm childTerm : enumType.getChildTypes().getTerms()) {
            ATRLiteral childLit = (ATRLiteral) childTerm;
            String childStr = childLit.getString();
            TypeName childName = TypeNameFactory.makeName(childStr, version, namespace);
            SimpleTypeName childSimpleName = (SimpleTypeName) childName;
            result.add(childSimpleName);
        }
        return result;
    }

    /**
     * Only valid for collections. The collection is either an ATRLiteral
     * {@code list<myType>} or an ATRTypeDeclaration which is equivalent to an
     * ATRLiteral {@code list<myType>}.
     */
    public static TypeName getElementType(ATR atr) {
        if (atr instanceof ATRLiteral) {
            TypeNameExpr name = (TypeNameExpr) getName(atr);
            return name.getInner();
        } else if (atr instanceof ATRTypeDeclaration) {
            ATRTypeDeclaration typeDecl = (ATRTypeDeclaration) atr;
            for (ATRTerm equivTerm : typeDecl.optEquivalentTypes().getTerms()) {
                if (equivTerm instanceof ATRLiteral) {
                    return getElementType(equivTerm);
                }
            }
            throw new IllegalArgumentException("Can't find equiv for "
                    + ATRSyntax.toSource(atr));
        }
        throw unknown(atr);
    }

    /**
     * list, set, bag, nullable
     */
    public static String getCollectionType(ATR atr) {
        if (atr instanceof ATRLiteral) {
            TypeNameExpr name = (TypeNameExpr) getName(atr);
            return name.getOuter();
        } else if (atr instanceof ATRTypeDeclaration) {
            ATRTypeDeclaration typeDecl = (ATRTypeDeclaration) atr;
            for (ATRTerm equivTerm : typeDecl.optEquivalentTypes().getTerms()) {
                if (equivTerm instanceof ATRLiteral) {
                    return getCollectionType(equivTerm);
                }
            }
            throw new IllegalArgumentException("Can't find equiv for "
                    + ATRSyntax.toSource(atr));
        }
        throw unknown(atr);
    }

    /**
     * Gets the representation class for a custom application type.
     */
    public static String getRepresentationClassName(ATRTypeDeclaration atr) {
        if (atr instanceof Application) {
            Application app = (Application) atr;
            return app.getRepresentationType().getString();
        } else if (atr instanceof ApplicationSubType) {
            throw new IllegalArgumentException(
                    "application sub types don't have explicit representation classes");
        } else {
            throw new IllegalArgumentException("Can't handle class "
                    + atr.getClass());
        }
    }

    public static String getGeneralizeUnsupportedPreference(ATRTypeDeclaration atr) {
        String result = null;
        if (atr != null) {
            ATRMap props = atr.optProperties();
            if (props != null) {
                ATRLiteral lit = (ATRLiteral) props
                        .get(TypeUtil.GENERALIZE_UNSUPPORTED);
                if (lit != null) {
                    result = lit.getString();
                }
            }
        }
        return result;
    }

    public static Integer getGeneralizeUnsupportedMaxInputs(ATRTypeDeclaration atr) {
        Integer result = null;
        if (atr != null) {
            ATRMap props = atr.optProperties();
            if (props != null) {
                ATRLiteral lit = (ATRLiteral) props
                        .get(TypeUtil.CONSTRUCT_MAX_INPUTS);
                if(lit != null) {
                    result = Integer.valueOf(lit.getString());
                }
            }
        }
        return result;
    }

    public static ATR makeCollection(TypeNameExpr nameExpr) {
        return new CTRLiteral(nameExpr.getFullName());
    }

    private static IllegalArgumentException unknown(ATR obj) {
        return new IllegalArgumentException("Unknown object (" + obj.getClass()
                + "): " + ATRSyntax.toSource(obj));
    }
}
