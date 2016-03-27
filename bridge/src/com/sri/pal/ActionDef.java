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

// $Id: ActionDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRList;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRNoEvalTerm;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.ActionIdiomFamilyType;
import com.sri.pal.jaxb.ActionIdiomParamType;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.pal.jaxb.ActionType;
import com.sri.pal.jaxb.CollapsibleOptionType;
import com.sri.pal.jaxb.CollapsibleParamType;
import com.sri.pal.jaxb.CollapsibleType;
import com.sri.pal.jaxb.ConstraintsType;
import com.sri.pal.jaxb.InheritType;
import com.sri.pal.jaxb.MetadataType;
import com.sri.pal.jaxb.ObjectFactory;
import com.sri.pal.jaxb.ParamClassType;
import com.sri.pal.jaxb.ParamType;
import com.sri.pal.jaxb.TypeRef;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SerialNumberRequest;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.contents.ActionCategory;
import com.sri.tasklearning.spine.messages.contents.ParamClass;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an action that can be performed in the workspace. This could be a
 * user-level action, such as &quot;open a new window,&quot; or it could be a
 * higher-level action such as a procedure represented by a {@link ProcedureDef}
 * . It should not be a low-level action below the level of the vocabulary used
 * by the user in interacting with the system, such as &quot;send a network
 * packet.&quot;
 *
 * @author chris
 */
public class ActionDef
        extends AbstractActionDef {
    private static final Logger log = LoggerFactory.getLogger(ActionDef.class);

    private final ActionDef parentAct;
    private final TypeDef[] paramTypes;

    /**
     * Builds a new action definition from the specified ATR declaration,
     * belonging to the specified Bridge instance.
     *
     * @param atr
     *            the ATR declaration
     * @param bridge
     *            the Bridge instance to which this action definition will
     *            belong
     * @throws PALException
     *             if a required type cannot be loaded
     */
    public ActionDef(ATRActionDeclaration atr,
                     Bridge bridge)
            throws PALException {
        super(atr, bridge);
        TypeName parentName = TypeUtil.getParent(atr);
        if (parentName == null) {
            parentAct = null;
        } else {
            parentAct = (ActionDef) getActionModel().getType(parentName);
            if (parentAct == null) {
                throw new PALActionMissingException("Couldn't get parent "
                        + parentName + " of action " + getName(), parentName);
            }
        }

        List<? extends ATRParameter> params = atr.getSignature().getElements();
        this.paramTypes = new TypeDef[mySize()];
        for (int i = 0; i < paramTypes.length; i++) {
            ATRParameter param = params.get(i);
            ATRLiteral paramTypeTerm = param.getType();
            String paramTypeStr = paramTypeTerm.getString();
            TypeName paramTypeName = TypeNameFactory.makeName(paramTypeStr);
            paramTypes[i] = (TypeDef) getActionModel().getType(paramTypeName);
            if (paramTypes[i] == null) {
                throw new PALActionMissingException("Unable to retrieve type "
                        + paramTypeName + " for parameter to action "
                        + getName(), paramTypeName);
            }
        }

        /* Check for duplicate param names. */
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < localSize(); i++) {
            String name = localGetParamName(i);
            if (names.contains(name)) {
                throw new IllegalArgumentException("Duplicate param '" + name
                        + "' with " + getName() + " and parent "
                        + getParentDef().getName());
            }
            names.add(name);
        }
    }

    @Override
    protected void fillInXml(ActionModelType amXml)
            throws PALException {
        /* Build the <action> element inside the <actionModel>. */
        ActionType actionXml = new ActionType();
        SimpleTypeName name = getName();
        actionXml.setId(name.getSimpleName());
        actionXml.setConstraints(getXmlConstraints());
        actionXml.setCategory(getCategory().getName());
        actionXml.setBenign(isBenign());
        ActionDef parent = getParentDef();
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
            actionXml.setInherit(inheritXml);
        }
        List<ParamType> inputsXml = new ArrayList<ParamType>();
        List<ParamType> outputsXml = new ArrayList<ParamType>();
        for (int i = 0; i < size(); i++) {
            String paramName = getParamName(i);
            if (getParentParamNum(i) != -1) {
                /* Don't include inherited parameters. */
                continue;
            }
            ParamType paramXml = new ParamType();
            paramXml.setDescription(getParamDescription(i));
            paramXml.setId(paramName);
            TypeRef typeRef = new TypeRef();
            typeRef.setTypeId(getParamTypeName(i).getFullName());
            paramXml.setTypeRef(typeRef);
            ParamClassType paramClass = new ParamClassType();
            paramClass.setClazz(getParamClass(i).getName());
            if (isParamClassViolable(i)) {
                paramClass.setViolable(true);
            }
            paramXml.setClazz(paramClass);
            if (isInputParam(i)) {
                Object defVal = getDefaultValue(i);
                if (defVal != null) {
                    TypeDef paramType = getParamType(i);
                    ATRTerm atrDefVal = paramType.toAtr(defVal);
                    String defValStr;
                    if (paramType instanceof CustomTypeDef
                            || paramType instanceof EnumeratedTypeDef
                            || paramType instanceof PrimitiveTypeDef) {
                        ATRLiteral lit = (ATRLiteral) atrDefVal;
                        defValStr = lit.getValue().toString();
                    } else if (paramType instanceof CollectionTypeDef
                            || paramType instanceof StructDef) {
                        defValStr = ATRSyntax.toSource(atrDefVal);
                    } else {
                        throw new RuntimeException("Unknown type ("
                                + paramType.getClass() + ") of default value "
                                + i + " of " + this);
                    }
                    paramXml.setDefaultValue(defValStr);
                }
            }
            List<ActionIdiomParamType> idiomParams = paramXml.getIdiomParam();
            for (SimpleTypeName familyName : getFamilies()) {
                for (String role : getParamRoles(i, familyName)) {
                    ActionIdiomParamType binding = new ActionIdiomParamType();
                    binding.setFamily(familyName.getFullName());
                    binding.setRole(role);
                    idiomParams.add(binding);
                }
            }
            List<MetadataType> metadataXml = new ArrayList<MetadataType>();
            for (String key : listParamMetadataKeys(i)) {
                if (TypeUtil.ACTION_RESERVED_KEYS.contains(key)) {
                    continue;
                }
                String value = getParamMetadata(i, key);
                MetadataType metaItem = new MetadataType();
                metaItem.setKey(key);
                metaItem.setValue(value);
                metadataXml.add(metaItem);
            }
            paramXml.getMetadata().addAll(metadataXml);
            if (i < numInputParams()) {
                inputsXml.add(paramXml);
            } else {
                outputsXml.add(paramXml);
            }
        }
        actionXml.getInputParam().addAll(inputsXml);
        actionXml.getOutputParam().addAll(outputsXml);
        String descr = getMetadata(DESCRIPTION);
        if (descr == null) {
            descr = "";
        }
        actionXml.setDescription(descr);
        List<MetadataType> metadata = actionXml.getMetadata();
        for (String key : listMetadataKeys()) {
            /* Don't include reserved metadata keys. */
            if (TypeUtil.ACTION_RESERVED_KEYS.contains(key)) {
                continue;
            }
            /* Don't include metadata maps for the parameters. */
            if (key.startsWith("$")) {
                String var = key.substring(1);
                if(getParamNum(var) != -1) {
                    continue;
                }
            }
            String value = getMetadata(key);
            MetadataType metaItem = new MetadataType();
            metaItem.setKey(key);
            metaItem.setValue(value);
            metadata.add(metaItem);
        }

        /* Is it collapsible? */
        if (isCollapsible()) {
            CollapsibleType collapseXml = new CollapsibleType();
            CollapsibleOptionType inside = getCollapsibilityInsideGesture();
            CollapsibleOptionType outside = getCollapsibilityOutsideGesture();
            collapseXml.setInsideGesture(inside);
            collapseXml.setOutsideGesture(outside);
            List<CollapsibleParamType> params = collapseXml.getParam();
            for (int i = 0; i < size(); i++) {
                String keep = getParamCollapsibility(i);
                if (keep != null) {
                    CollapsibleParamType paramXml = new CollapsibleParamType();
                    paramXml.setId(getParamName(i));
                    paramXml.setKeep(keep);
                    params.add(paramXml);
                }
            }
            actionXml.setCollapsible(collapseXml);
        }

        /* Add any action families it's a member of. */
        List<ActionIdiomFamilyType> families = actionXml.getIdiomFamily();
        for (SimpleTypeName family : getFamilies()) {
            ActionIdiomFamilyType familyXml = new ActionIdiomFamilyType();
            familyXml.setFamily(family.getFullName());
            families.add(familyXml);
        }

        amXml.getAction().add(actionXml);
    }

    @Override
    public int size() {
        return localSize();
    }

    private int localSize() {
        int parentSize = 0;
        ActionDef parent = getParentDef();
        if (parent != null) {
            parentSize = parent.size();
        }
        return parentSize + mySize();
    }

    /**
     * The size of just this ActionDef, ignoring its parent.
     */
    private int mySize() {
        return super.size();
    }

    /**
     * If this action definition inherits from another, this method returns the
     * parent.
     *
     * @return the parent of this action definition, or {@code null}
     */
    public final ActionDef getParentDef() {
        return parentAct;
    }

    /**
     * Given a (positional) parameter, determine if it's inherited from the
     * parent or part of this action's definition. If inherited, determine the
     * parameter index in the context of the parent definition.
     *
     * @param pos
     *            the index of the parameter in this definition
     * @return the index of the parameter in the parent's definition, or -1
     */
    private final int getParentParamNum(int pos) {
        int numParentInputs;
        int numMyInputs;
        int numParentOutputs;
        ActionDef parent = getParentDef();
        if (parent == null) {
            numParentInputs = 0;
            numMyInputs = numInputParams();
            numParentOutputs = 0;
        } else {
            numParentInputs = parent.numInputParams();
            numMyInputs = numInputParams() - numParentInputs;
            numParentOutputs = parent.size() - numParentInputs;
        }
        if (pos < numParentInputs) {
            return pos;
        } else if (pos < numParentInputs + numMyInputs) {
            return -1;
        } else if (pos < numParentInputs + numMyInputs + numParentOutputs) {
            return pos - numMyInputs;
        } else {
            return -1;
        }
    }

    /**
     * Determine if the given parameter index represents a parameter that is
     * inherited from the parent action definition or local to this one. If
     * local, determine the index of this parameter within this definition's ATR
     * structures.
     *
     * @param pos
     *            a parameter index for this action, accounting for inherited
     *            parameters
     * @return a parameter index local to this action, ignoring inherited
     *         parameters. If the parameter isn't local, return -1.
     */
    private int getLocalParamNum(int pos) {
        int numParentInputs;
        int numMyInputs;
        int numParentOutputs;
        ActionDef parent = getParentDef();
        if (parent == null) {
            numParentInputs = 0;
            numMyInputs = numInputParams();
            numParentOutputs = 0;
        } else {
            numParentInputs = parent.numInputParams();
            numMyInputs = numInputParams() - numParentInputs;
            numParentOutputs = parent.size() - numParentInputs;
        }
        if (pos < numParentInputs) {
            return -1;
        } else if (pos < numParentInputs + numMyInputs) {
            return pos - numParentInputs;
        } else if (pos < numParentInputs + numMyInputs + numParentOutputs) {
            return -1;
        } else {
            return pos - numParentInputs - numParentOutputs;
        }
    }

    @Override
    public String getParamName(int pos) {
        return localGetParamName(pos);
    }

    private String localGetParamName(int pos) {
        int parentPos = getParentParamNum(pos);
        if (parentPos == -1) {
            int localPos = getLocalParamNum(pos);
            return super.getParamName(localPos);
        } else {
            return getParentDef().getParamName(parentPos);
        }
    }

    @Override
    public TypeName getParamTypeName(int pos) {
        int parentPos = getParentParamNum(pos);
        if (parentPos != -1) {
            return getParentDef().getParamTypeName(parentPos);
        }
        int localPos = getLocalParamNum(pos);
        ATRParameter param = getAtr().getSignature().getElements().get(localPos);
        String typeStr = param.getType().getString();
        return TypeNameFactory.makeName(typeStr);
    }

    @Override
    public TypeDef getParamType(int pos) {
        int parentPos = getParentParamNum(pos);
        if (parentPos != -1) {
            return getParentDef().getParamType(parentPos);
        }
        int localPos = getLocalParamNum(pos);
        return paramTypes[localPos];
    }

    @Override
    protected Set<ActionModelDef> getRequiredDefs()
            throws PALException {
        Set<ActionModelDef> result = new HashSet<ActionModelDef>();
        if (getParentDef() != null) {
            result.add(getParentDef());
            result.addAll(getParentDef().getRequiredDefs());
        }
        for (ActionModelDef def : paramTypes) {
            result.add(def);
            result.addAll(def.getRequiredDefs());
        }
        for (SimpleTypeName name : getFamilies()) {
            ActionFamilyDef def = (ActionFamilyDef) getActionModel().getType(
                    name);
            if (def == null) {
                throw new PALActionMissingException("Couldn't retrieve family "
                        + name, name);
            }
            result.add(def);
            result.addAll(def.getRequiredDefs());
        }
        return result;
    }

    protected ActionInvocation newInstance(ActionInvocation parent,
                                           long serial)
            throws PALException {
        TransactionUID uid = getSpine().getNextUid();
        return new ActionInvocation(this, parent, serial, uid);
    }

    /**
     * Creates a new invocation of this action definition. Don't forget to call
     * {@link ActionInvocation#start} on the result, if you want it to actually
     * run.
     *
     * @param args
     *            arguments to assign to the action's input parameters
     * @param parent
     *            the invocation which called this action, or <code>null</code>
     *            if this is a top-level invocation
     * @return a new invocation
     * @throws PALException
     *             if the invocation cannot be created
     */
    public ActionInvocation invoke(ActionInvocation parent,
                                   Object... args)
            throws PALException {
        return invoke(parent, Arrays.asList(args));
    }

    /**
     * Creates a new invocation of this action definition, initializing both
     * input and output arguments. Used primarily for testing, as in typical
     * usage there is no point in setting the outputs of an
     * {@link ActionInvocation} prior to calling {@link ActionInvocation#start}
     * on it.
     *
     * @param allArgs
     *            ordered input and output arguments to assign to the action's
     *            parameters
     * @param parent
     *            the invocation which called this action, or <code>null</code>
     *            if this is a top-level invocation
     * @return a new invocation
     * @throws PALException
     *             if the invocation cannot be created, or if |allArgs| <
     *             |inputs|
     */
    public ActionInvocation bindAll(ActionInvocation parent,
                                    Object... allArgs)
            throws PALException {
        final int numArgs = allArgs.length;
        // TODO handle wrong number of allArgs more gracefully, but need to
        // clean up many tests
// if (numArgs != fields.size())
// throw new PALException("bindAll argument count mismatch " + numArgs + " " +
// fields.size());
        if (numArgs < numInputParams())
            throw new PALException("Not all inputs were specified: " + this
                    + " args= " + Arrays.toString(allArgs));

        // Create the invocation and bind its inputs
        ArrayList<Object> inArgs = new ArrayList<Object>();
        for (int j = 0; j < numArgs; j++) {
            if (isInputParam(j))
                inArgs.add(allArgs[j]);
        }
        ActionInvocation result = invoke(parent, inArgs);

        // Bind outputs of the invocation
        for (int j = 0; j < numArgs; j++) {
            if (!isInputParam(j))
                result.setValue(j, allArgs[j]);
        }

        return result;
    }

    /**
     * Creates a new invocation of this action definition. Don't forget to call
     * {@link ActionInvocation#start} on the result, if you want it to actually
     * run.
     *
     * @param args
     *            arguments to assign to the action's input parameters
     * @param parent
     *            the invocation which called this action, or <code>null</code>
     *            if this is a top-level invocation
     * @return a new invocation
     * @throws PALException
     *             if the invocation cannot be created
     */ 
    public ActionInvocation invoke(ActionInvocation parent,
                                   List<? extends Object> args)
            throws PALException {
        // It only needs a serial number if it's going to be used in a
        // demonstration sent to LAPDOG. That's only going to happen if it's a
        // top-level, meaning user-initiated, action. That's only true if it has
        // no parent.
        long serial = 0;
        if (parent == null) {
            try {
                TransactionUID uid = getSpine().getNextUid();
                Message msg = new SerialNumberRequest(getSpine().getClientId(),
                        uid);
                SerialNumberResponse result = getBridge().getSerialGetter()
                        .sendAndGetReply(msg);
                serial = result.getSerialNumber();
            } catch (SpineException e) {
                throw new PALException("Unable to get serial number", e);
            }
        }
        ActionInvocation result = newInstance(parent, serial);
        result.assignArgs(args);
        return result;
    }

    ActionInvocation invoked(ActionStreamEvent parent,
                             TransactionUID uid,
                             long serialNumber)
            throws PALException {
        return new ActionInvocation(this, parent, serialNumber, uid);
    }

    @Override
    public int numInputParams() {
        int parentInputs = 0;
        ActionDef parent = getParentDef();
        if (parent != null) {
            parentInputs = parent.numInputParams();
        }
        return super.numInputParams() + parentInputs;
    }

    @Override
    public ActionCategory getCategory() {
        ATRMap propMap = getAtr().getProperties();
        ATRLiteral effectsTerm = (ATRLiteral) propMap
                .get(TypeUtil.ACTION_CATEGORY);
        String effectsStr = effectsTerm.getString();
        return ActionCategory.getValueOf(effectsStr);
    }

    /**
     * Provides the action families which this action belongs to. Action
     * families are used to group related action definitions together for
     * purposes of idiom or gesture recognition.
     *
     * @return the names of the action families which this action belongs to
     */
    public Set<SimpleTypeName> getFamilies() {
        return TypeUtil.getActionFamilies(getAtr());
    }

    /**
     * Provides the roles which a given parameter plays in an action family. The
     * named action family must be one to which this action definition belongs,
     * and the indicated parameter must be bound to a role in that action
     * family.
     *
     * @param familyName
     *            the name of the action family for which to retrieve the
     *            binding of a particular parameter
     * @return the roles which this parameter plays in the indicated action
     *         family
     */
    public List<String> getParamRoles(int pos,
                                      TypeName familyName) {
        int parentPos = getParentParamNum(pos);
        if (parentPos != -1) {
            return getParentDef().getParamRoles(parentPos, familyName);
        }
        List<String> result = new ArrayList<String>();
        ATRMap actionProps = getAtr().getProperties();
        ATRMap paramProps = (ATRMap) actionProps.get("$" + getParamName(pos));
        ATRMap roleProps = (ATRMap) paramProps.get(TypeUtil.PARAM_ROLE);
        if (roleProps != null) {
            ATRList roles = (ATRList) roleProps.get(familyName.getFullName());
            if (roles != null) {
                for (ATRTerm roleTerm : roles.getTerms()) {
                    ATRLiteral roleLit = (ATRLiteral) roleTerm;
                    result.add(roleLit.getString());
                }
            }
        }

        return result;
    }

    /**
     * What is the class of the given parameter? The parameter class has an
     * effect on learning generalization.
     *
     * @param pos
     *            the parameter to retrieve the class of
     * @return the class of the parameter
     */
    public ParamClass getParamClass(int pos) {
        int parentPos = getParentParamNum(pos);
        if (parentPos != -1) {
            return getParentDef().getParamClass(parentPos);
        }
        ParamClass result = null;
        ATRMap actionProps = getAtr().getProperties();
        ATRMap paramProps = (ATRMap) actionProps.get("$" + getParamName(pos));
        ATRLiteral classLit = null;
        if (paramProps != null) {
            classLit = (ATRLiteral) paramProps.get(TypeUtil.PARAM_CLASS);
        }
        if (classLit == null) {
            if (isInputParam(pos)) {
                result = ParamClass.GENERALIZABLE;
            } else {
                result = ParamClass.EXTERNAL;
            }
        } else {
            result = ParamClass.getValueOf(classLit.getString());
        }
        return result;
    }

    public boolean isParamClassViolable(int pos) {
        int parentPos = getParentParamNum(pos);
        if (parentPos != -1) {
            return getParentDef().isParamClassViolable(parentPos);
        }
        boolean result = false;
        ATRMap actionProps = getAtr().getProperties();
        ATRMap paramProps = (ATRMap) actionProps.get("$" + getParamName(pos));
        ATRLiteral classLit = null;
        if (paramProps != null) {
            classLit = (ATRLiteral) paramProps
                    .get(TypeUtil.PARAM_CLASS_VIOLABLE);
        }
        if (classLit != null) {
            result = Boolean.valueOf(classLit.getString());
        }
        return result;
    }

    @Override
    public String getParamDescription(int pos) {
        int parentPos = getParentParamNum(pos);
        if (parentPos == -1) {
            return super.getParamDescription(pos);
        } else {
            return getParentDef().getParamDescription(parentPos);
        }
    }

    @Override
    public String getParamMetadata(int pos,
                                   String key) {
        int parentPos = getParentParamNum(pos);
        if (parentPos == -1) {
            return super.getParamMetadata(pos, key);
        } else {
            return getParentDef().getParamMetadata(parentPos, key);
        }
    }

    @Override
    public Set<String> listParamMetadataKeys(int pos) {
        int parentPos = getParentParamNum(pos);
        if (parentPos == -1) {
            return super.listParamMetadataKeys(pos);
        } else {
            return getParentDef().listParamMetadataKeys(parentPos);
        }
    }

    /**
     * Returns the executor which is responsible for handling this action.
     *
     * @return this action's executor
     */
    public ActionExecutor getExecutor() {
        return getActionModel().getExecutor(getName());
    }

    /**
     * Indicates if this action is subject to automatic garbage collection. Such
     * an action will be automatically removed from the action model and deleted
     * from the vocabularies of LAPDOG and LUMEN when it is no longer
     * referenced. Its visibility in {@link ActionModel} will also be modified;
     * see that class for details.
     *
     * @return <code>true</code> iff this action is transient
     */
    public boolean isTransient() {
        return TypeUtil.isTransient(getAtr());
    }

    /**
     * Indicates if this action is collapsible in any context.
     *
     * @return {@code true} if instances of this action can be collapsed as part
     *         of idiom recognition or procedure learning
     */
    public boolean isCollapsible() {
        ATRMap props = getAtr().getProperties();
        return props.get(TypeUtil.COLLAPSIBLE) != null;
    }

    /**
     * Indicates how instances of this action are collapsed inside gestures,
     * during idiom recognition.
     *
     * @return the collapsibility of this action inside gestures
     */
    public CollapsibleOptionType getCollapsibilityInsideGesture() {
        ATRMap props = getAtr().getProperties();
        ATRMap collapse = (ATRMap) props.get(TypeUtil.COLLAPSIBLE);
        String value = collapse.getString(TypeUtil.COLLAPSIBLE_INSIDE_GESTURE);
        return CollapsibleOptionType.fromValue(value);
    }

    /**
     * Indicates how instances of this action are collapsed outside of a
     * gesture, during procedure learning.
     *
     * @return the collapsibility of this action outside of gestures
     */
    public CollapsibleOptionType getCollapsibilityOutsideGesture() {
        ATRMap props = getAtr().getProperties();
        ATRMap collapse = (ATRMap) props.get(TypeUtil.COLLAPSIBLE);
        String value = collapse.getString(TypeUtil.COLLAPSIBLE_OUTSIDE_GESTURE);
        return CollapsibleOptionType.fromValue(value);
    }

    /**
     * Indicates if the specified parameter is collapsible, and if so, what
     * value is kept. When actions are collapsed, the indicated parameter will
     * either retain the first, last, or merged value of the observed values for
     * this parameter on collapsed actions.
     *
     * @param i
     *            the parameter to retrieve collapsibility information for
     * @return one of the strings {@code first}, {@code last}, or {@code merge},
     *         or {@code null}
     */
    public String getParamCollapsibility(int i) {
        ATRMap props = getAtr().getProperties();
        ATRMap collapse = (ATRMap) props.get(TypeUtil.COLLAPSIBLE);
        String paramName = getParamName(i);
        return collapse.getString("$" + paramName);
    }

    /**
     * Provides the native ATR representation of the constraints on this action.
     * {@code ATRNoEvalTerm} wrapping an {@code ATRNull} indicates empty
     * constraints.
     *
     * @return the constraints for this action
     */
    public ATRNoEvalTerm getATRConstraints() {
        ATRMap propMap = getAtr().getProperties();
        ATRNoEvalTerm constraints = (ATRNoEvalTerm) propMap
                .get(TypeUtil.CONSTRAINTS);
        return constraints;
    }

    /**
     * Provides the XML string representing the constraints on this action. If
     * there are no constraints (action is unconstrained), the top XML element
     * will have no child elements. If the constraints are unknown, {@code null}
     * will be returned.
     *
     * @return the constraints for this action, or {@code null}
     * @throws PALException
     */
    public String getConstraints()
            throws PALException {
        ConstraintsType xmlObj = getXmlConstraints();
        if (xmlObj == null) {
            return null;
        }
        ObjectFactory objectFactory = new ObjectFactory();
        JAXBElement<ConstraintsType> jaxbObj = objectFactory
                .createConstraints(xmlObj);

        try {
            return marshal(jaxbObj).trim();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to marshal XML string for "
                    + xmlObj, e);
        }
    }

    /**
     * Returns the JAXB XML representation of this action's constraints, if it
     * has any. The constraints are things that must be true before this action
     * can be executed. Examples include permissions the user must have or data
     * values that must be present.
     *
     * @return the JAXB representation of this action's constraints, or
     *         {@code null}
     * @throws PALException
     *             if the constraints can't be created
     */
    public ConstraintsType getXmlConstraints()
            throws PALException {
        return getActionModelFactory()
                .xmlConstraints(getATRConstraints(), this);
    }

    /**
     * Retrieves the default value associated with the named parameter. If a
     * value is not specified when the procedure is invoked, this value will be
     * used.
     *
     * @param pos
     *            the index of the parameter to examine
     * @return the default for the requested parameter, or <code>null</code>
     * @see ProcedureInvocation#getValue(String)
     */
    public Object getDefaultValue(int pos) {
        if (pos >= numInputParams()) {
            throw new IndexOutOfBoundsException("Field #" + pos
                    + " is not an input or doesn't exist");
        }
        int parentPos = getParentParamNum(pos);
        if (parentPos != -1) {
            return getParentDef().getDefaultValue(parentPos);
        }
        int localPos = getLocalParamNum(pos);

        ATRSig sig = getAtr().getSignature();
        ATRParameter param = sig.getElements().get(localPos);
        ATRTerm defaultTerm = param.getDefaultValue();
        if (defaultTerm == null) {
            return null;
        } else {
            TypeDef paramType = getParamType(localPos);
            try {
                return paramType.fromAtr(defaultTerm);
            } catch (PALException e) {
                log.error(
                        "Failed parsing default value ("
                                + ATRSyntax.toSource(defaultTerm)
                                + ") for arg " + pos + " of " + getName(), e);
                return null;
            }
        }
    }

    /**
     * Retrieves a default value by parameter name rather than parameter
     * position.
     *
     * @param fieldName
     *            the name of the parameter to retrieve a default value for
     * @return the associated default value
     * @throws PALException
     *             if an error occurs
     * @see #getDefaultValue(int)
     */
    public Object getDefaultValue(String fieldName)
            throws PALException {
        int pos = getParamNum(fieldName);
        return getDefaultValue(pos);
    }

    /**
     * Copies this action and assigns a new default value to the indicated
     * parameter.
     *
     * @param pos
     *            the index of the parameter to alter
     * @param value
     *            the new default value
     * @return a new action with a modified default value
     * @see #getDefaultValue
     */
    public ActionDef newDefaultValue(int pos,
                                     Object value) {
        if (pos >= numInputParams()) {
            throw new IndexOutOfBoundsException("Field #" + pos
                    + " is not an input or doesn't exist");
        }
        int parentPos = getParentParamNum(pos);
        if (parentPos != -1) {
            throw new IndexOutOfBoundsException("Field #" + pos
                    + " is inherited; cannot change default value");
        }
        int localPos = getLocalParamNum(pos);

        /* Convert the value to ATR. */
        TypeDef type = getParamType(pos);
        ATRTerm atrValue = type.toAtr(value);

        /*
         * We have to rebuild the ATRActionDeclaration from scratch. Start by
         * getting all its fields.
         */
        ATRActionDeclaration atr = getAtr();
        ATRSig sig = atr.getSignature();
        ATRTask exec = atr.getExecute();
        ATRLiteral execJ = atr.getExecuteJ();
        ATRMap props = atr.getProperties();

        /* Rebuild the sig. */
        CTRConstructor ctrBuilder = new CTRConstructor();
        List<ATRParameter> params = new ArrayList<ATRParameter>();
        params.addAll(sig.getElements());
        ATRParameter param = params.get(localPos);
        ATRParameter newParam = ctrBuilder.createParameter(param.getVariable(),
                param.getMode(), param.getType().getString(), atrValue);
        params.set(localPos, newParam);
        ATRSig newSig = ctrBuilder.createSignature(sig.getFunctor(), params);

        /*
         * Now we have to figure out which ATRActionDeclaration constructor to
         * use.
         */
        ATRActionDeclaration newAtr;
        if (exec == null) {
            newAtr = ctrBuilder.createActionDeclaration(newSig,
                    execJ.getString(), props);
        } else {
            newAtr = ctrBuilder.createActionDeclaration(newSig, exec, props);
        }

        /* Create the new ActionDef. */
        SimpleTypeName name = getName();
        ActionDef result;
        try {
            result = (ActionDef) getActionModelFactory().makeActionModelDef(
                    newAtr, name.getVersion(), name.getNamespace());
        } catch (PALException e) {
            log.warn("Cannot copy " + this + " using ATR "
                    + ATRSyntax.toSource(newAtr));
            throw new RuntimeException("Cannot copy " + getName(), e);
        }
        return result;
    }
}
