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

// $Id: ConstraintDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.decl.ATRFunctionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRSigDecl;
import com.sri.ai.lumen.atr.term.ATRList;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRVariable;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.pal.jaxb.ConstraintDeclParamType;
import com.sri.pal.jaxb.ConstraintDeclarationType;
import com.sri.pal.jaxb.ObjectFactory;
import com.sri.tasklearning.spine.util.TypeUtil;

/**
 * Declaration of a constraint which may be bound to actions.
 *
 * @author chris
 */
public class ConstraintDef
        extends ActionModelDef {
    private String cachedXml = null;

    protected ConstraintDef(ATRFunctionDeclaration atr,
                            Bridge bridge)
            throws PALException {
        super(atr, bridge);

        ATRSig sig = atr.getSignature();
        List<? extends ATRParameter> atrParams = sig.getElements();
        ATRMap props = atr.getProperties();
        ATRList descrs = (ATRList) props.get(TypeUtil.CONSTRAINT_DESCRIPTIONS);
        if (atrParams.size() != descrs.size()) {
            throw new IllegalArgumentException(
                    "Params and descriptions don't match length");
        }

        /* Check for duplicate param names. */
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < size(); i++) {
            String name = getFieldName(i);
            if (names.contains(name)) {
                throw new PALException("Duplicate param '" + name + "' in "
                        + getName());
            }
            names.add(name);
        }
    }

    @Override
    public final ATRFunctionDeclaration getAtr() {
        return (ATRFunctionDeclaration) super.getAtr();
    }

    /**
     * Returns the number of parameters which this constraint declaration takes.
     *
     * @return the number of parameters for this constraint
     */
    public final int size() {
        ATRSigDecl atr = getAtr();
        ATRSig sig = atr.getSignature();
        List<? extends ATRParameter> atrParams = sig.getElements();
        return atrParams.size();
    }

    /**
     * Maps a field name to its positional index in the constraint definition's
     * argument list.
     *
     * @param fieldName
     *            the name of the argument
     * @return the corresponding position (starting with zero)
     */
    public int getFieldNum(String fieldName) {
        ATRSigDecl atr = getAtr();
        ATRSig sig = atr.getSignature();
        List<? extends ATRParameter> atrParams = sig.getElements();
        for (int i = 0; i < atrParams.size(); i++) {
            ATRParameter atrParam = atrParams.get(i);
            ATRVariable var = atrParam.getVariable();
            String varName = var.getVariableName();
            if (varName.equals(fieldName)) {
                return i;
            }
        }
        throw new NoSuchElementException("No field of constraint declaration "
                + getName() + " named " + fieldName);
    }

    /**
     * Inverse operation from {@link #getFieldNum}. Maps an index to a name in
     * the constraint definition's argument list.
     *
     * @param fieldNum
     *            the index of the argument
     * @return the corresponding argument name
     */
    public final String getFieldName(int fieldNum) {
        ATRSigDecl atr = getAtr();
        ATRSig sig = atr.getSignature();
        List<? extends ATRParameter> atrParams = sig.getElements();
        ATRParameter atrParam = atrParams.get(fieldNum);
        ATRVariable var = atrParam.getVariable();
        return var.getVariableName();
    }

    /**
     * Retrieves the description of a parameter to this constraint declaration.
     *
     * @param fieldNum
     *            the number of the field to retrieve a description for
     * @return the requested description, as encoded in the action model XML
     *         file
     */
    public String getFieldDescription(int fieldNum) {
        String result = null;
        ATRMap propMap = getAtr().getProperties();
        ATRList paramDescrs = (ATRList) propMap
                .get(TypeUtil.CONSTRAINT_DESCRIPTIONS);
        if (paramDescrs != null && paramDescrs.size() > fieldNum) {
            ATRLiteral descrTerm = (ATRLiteral) paramDescrs.get(fieldNum);
            result = descrTerm.getString();
        }
        return result;
    }

    @Override
    public final SimpleTypeName getName() {
        String strName = getAtr().getSignature().getFunctor();
        TypeName name = TypeNameFactory.makeName(strName);
        return (SimpleTypeName) name;
    }

    @Override
    public String getMetadata(String key) {
        return null;
    }

    @Override
    public Set<String> listMetadataKeys() {
        return Collections.emptySet();
    }

    @Override
    protected Set<ActionModelDef> getRequiredDefs() {
        return Collections.emptySet();
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

        /* Build the <constraintDecl> element inside the <actionModel>. */
        ConstraintDeclarationType constraint = new ConstraintDeclarationType();
        constraint.setId(getName().getSimpleName());
        List<ConstraintDeclParamType> params = constraint.getParam();
        for (int i = 0; i < size(); i++) {
            ConstraintDeclParamType param = new ConstraintDeclParamType();
            param.setId(getFieldName(i));
            String descr = getFieldDescription(i);
            if (descr != null) {
                param.setDescription(descr);
            }
            params.add(param);
        }
        amXml.getConstraintDecl().add(constraint);

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
}
