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

// $Id: AbstractActionDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRSigDecl;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRNull;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameExpr;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.pal.jaxb.ObjectFactory;
import com.sri.pal.jaxb.RequireType;
import com.sri.tasklearning.spine.messages.contents.ActionCategory;
import com.sri.tasklearning.spine.util.TypeUtil;

/**
 * Ancestor class of actions (and procedures) and idioms.
 */
public abstract class AbstractActionDef
        extends ActionModelDef {
    private String cachedXml = null;

    protected AbstractActionDef(ATR atr,
                                Bridge bridge)
            throws PALException {
        super(atr, bridge);

        /* Check for duplicate param names. */
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < localSize(); i++) {
            String name = localGetParamName(i);
            if (names.contains(name)) {
                throw new PALException("Duplicate param '" + name + "' in "
                        + localGetName());
            }
            names.add(name);
        }
    }

    @Override
    public SimpleTypeName getName() {
        return localGetName();
    }

    private SimpleTypeName localGetName() {
        String strName = getAtr().getSignature().getFunctor();
        SimpleTypeName name = (SimpleTypeName) TypeNameFactory
                .makeName(strName);
        return name;
    }

    @Override
    public ATRActionDeclaration getAtr() {
        return (ATRActionDeclaration) super.getAtr();
    }

    @Override
    public String getXml()
            throws PALException {
        if (cachedXml != null) {
            return cachedXml;
        }

        /* Build the <actionModel> element */
        ActionModelType amXml = new ActionModelType();
        amXml.setVersion(getName().getVersion());
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

        fillInXml(amXml);

        ObjectFactory objectFactory = new ObjectFactory();
        JAXBElement<ActionModelType> jaxb = objectFactory
                .createActionModel(amXml);
        try {
            cachedXml = marshal(jaxb);
            return cachedXml;
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to marshal " + jaxb + " for "
                    + this, e);
        }
    }

    protected abstract void fillInXml(ActionModelType amXml)
            throws PALException;

    /**
     * Provides the number of parameters, both input and output, that this
     * action takes.
     *
     * @return the number of parameters for this action
     * @see #numInputParams
     */
    public int size() {
        return localSize();
    }

    private int localSize() {
        ATRActionDeclaration atr = getAtr();
        if (atr == null) {
            return 0;
        } else {
            return atr.getSignature().getElements().size();
        }
    }

    /**
     * Returns the name of the requested parameter. Parameters are ordered, so
     * each has a number and a name.
     *
     * @param pos
     *            the position or number (starting at zero) of the parameter to
     *            retrieve the name of
     * @return the name of the requested parameter
     * @see #getParamNum
     */
    public String getParamName(int pos) {
        return localGetParamName(pos);
    }

    private String localGetParamName(int pos) {
        ATRParameter param = getAtr().getSignature().getElements().get(pos);
        return param.getVariable().getVariableName();
    }

    /**
     * Retrieves the type name of the indicated parameter.
     *
     * @param pos
     *            the number (starting at zero) of the parameter to retrieve
     *            type information for
     * @return the name of the type of the indicated parameter
     * @see #getParamType
     */
    public abstract TypeName getParamTypeName(int pos);

    /**
     * Returns the type of the indicated parameter to this action definition.
     *
     * @param pos
     *            the index of the desired parameter
     * @return the type of the indicated parameter
     * @throws PALException
     *             if the type can't be retrieved
     * @see #getParamTypeName
     */
    public TypeDef getParamType(int pos)
            throws PALException {
        TypeName name = getParamTypeName(pos);
        TypeDef result = null;
        if (name != null) {
            result = (TypeDef) getActionModel().getType(name);
        }
        return result;
    }

    /**
     * Returns the number of the parameter with the given name.
     *
     * @param name
     *            the name of the parameter to search for in this action's
     *            parameter list
     * @return the number of the requested parameter, or -1
     * @see #getParamName
     */
    public int getParamNum(String name) {
        for (int i = 0; i < size(); i++) {
            if (getParamName(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getMetadata(String key) {
        ATRMap props = getAtr().getProperties();
        if (props == null) {
            return null;
        }
        ATRTerm value = props.get(key);
        if (value == null || value instanceof ATRNull) {
            return null;
        } else if (value instanceof ATRLiteral) {
            return ((ATRLiteral) value).getString();
        } else {
            return value.toString();
        }
    }

    @Override
    public Set<String> listMetadataKeys() {
        ATRMap props = getAtr().getProperties();
        if (props == null) {
            return Collections.emptySet();
        }
        return props.getMap().keySet();
    }

    /**
     * Returns the number of input parameters which the action definition takes.
     *
     * @return the number of input parameters
     * @see #size
     */
    public int numInputParams() {
        ATRSigDecl atr = getAtr();
        ATRSig sig = atr.getSignature();
        List<? extends ATRParameter> atrParams = sig.getElements();
        int inputs = 0;
        for (ATRParameter atrParam : atrParams) {
            if (atrParam.getMode() == Modality.INPUT) {
                inputs++;
            }
        }
        return inputs;
    }

    /**
     * Indicates if the specified field is an input or output parameter to this
     * action.
     *
     * @param pos
     *            the position in the argument list. The first argument is 0.
     * @return <code>true</code> if an input parameter, <code>false</code>if an
     *         output parameter
     */
    public boolean isInputParam(int pos) {
        return pos < numInputParams();
    }

    /**
     * Returns the type of effects caused by running this procedure. This
     * information is used by LAPDOG when it learns new procedures that include
     * this action.
     *
     * @return the effects type of this action
     */
    public abstract ActionCategory getCategory();

    /**
     * Indicates if the action is typically benign with respect to modifying the
     * intent of a procedure within an exercise solution.
     *
     * @return {@code true} if this action defaults to benign
     */
    public boolean isBenign() {
        boolean result = false;
        String benignStr = getMetadata(TypeUtil.BENIGN);
        if (benignStr != null) {
            result = Boolean.parseBoolean(benignStr);
        }
        return result;
    }

    /**
     * Returns the description of this field, as provided in the action model
     * XML file
     *
     * @param pos
     *            the field for which to fetch the description
     * @return the description of the given field
     */
    public String getParamDescription(int pos) {
        String result = null;
        ATRMap actionProps = getAtr().getProperties();
        ATRMap paramProps = (ATRMap) actionProps.get("$" + getParamName(pos));
        ATRLiteral lit = null;
        if (paramProps != null) {
            lit = (ATRLiteral) paramProps.get(TypeUtil.PARAM_DESCRIPTION);
        }
        if (lit != null) {
            result = lit.getString();
        }
        return result;
    }

    /**
     * Provides metadata for a given parameter of this action.
     *
     * @param pos
     *            the position (starting at 0) of the parameter to retrieve
     *            metadata for
     * @param key
     *            the key to look up in the parameter's metadata
     * @return the value associated with the given key, or <code>null</code>
     * @see #listFieldMetadataKeys
     */
    public String getParamMetadata(int pos,
                                   String key) {
        String result = null;
        ATRMap actionProps = getAtr().getProperties();
        ATRMap paramProps = (ATRMap) actionProps.get("$" + getParamName(pos));
        ATRLiteral valueTerm = null;
        if (paramProps != null) {
            valueTerm = (ATRLiteral) paramProps.get(key);
        }
        if (valueTerm != null) {
            result = valueTerm.getString();
        }
        return result;
    }

    /**
     * Provides all keys which are present in a given parameter's metadata.
     *
     * @param pos
     *            the position (starting at 0) of the parameter to retrieve
     *            metadata for
     * @return the given parameter's metadata keys
     * @see #getFieldMetadata
     */
    public Set<String> listParamMetadataKeys(int pos) {
        Set<String> result = new HashSet<String>();
        ATRMap actionProps = getAtr().getProperties();
        ATRMap paramProps = (ATRMap) actionProps.get("$" + getParamName(pos));
        if (paramProps != null) {
            for (String key : paramProps.getMap().keySet()) {
                if (!TypeUtil.ACTION_RESERVED_KEYS.contains(key)) {
                    result.add(key);
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
