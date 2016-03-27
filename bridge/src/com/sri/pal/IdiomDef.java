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

// $Id: IdiomDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.learning.ATRIdiomTemplateAction;
import com.sri.ai.lumen.atr.learning.impl.IdiomTemplateFactory;
import com.sri.ai.lumen.atr.term.ATRList;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.pal.jaxb.IdiomParamType;
import com.sri.pal.jaxb.IdiomTemplateActionParamType;
import com.sri.pal.jaxb.IdiomTemplateActionType;
import com.sri.pal.jaxb.IdiomTemplateNamedActionParamType;
import com.sri.pal.jaxb.IdiomTemplateNamedActionType;
import com.sri.pal.jaxb.IdiomTemplateType;
import com.sri.pal.jaxb.IdiomType;
import com.sri.pal.jaxb.MetadataType;
import com.sri.tasklearning.spine.messages.contents.ActionCategory;
import com.sri.tasklearning.spine.util.TypeUtil;

public class IdiomDef
        extends AbstractActionDef {
    protected IdiomDef(ATR atr,
                       Bridge bridge)
            throws PALException {
        super(atr, bridge);
    }

    @Override
    public ActionCategory getCategory() {
        return ActionCategory.EFFECTOR;
    }

    @Override
    protected void fillInXml(ActionModelType amXml)
            throws PALException {
        /* Build the <idiom> inside the <actionModel> */
        IdiomType idiom = new IdiomType();
        amXml.getIdiom().add(idiom);
        idiom.setId(getName().getFullName());

        /* Description and metadata */
        idiom.setDescription(getMetadata(DESCRIPTION));
        idiom.setBenign(isBenign());
        List<MetadataType> metadata = idiom.getMetadata();
        for (String key : listMetadataKeys()) {
            String value = getMetadata(key);
            if (key.equals(DESCRIPTION)) {
                continue;
            }
            MetadataType metaItem = new MetadataType();
            metaItem.setKey(key);
            metaItem.setValue(value);
            metadata.add(metaItem);
        }

        /* Input and output params */
        List<IdiomParamType> inputs = idiom.getInputParam();
        List<IdiomParamType> outputs = idiom.getOutputParam();
        for (int i = 0; i < size(); i++) {
            IdiomParamType param = new IdiomParamType();
            param.setId(getParamName(i));
            String matchIf = getParamMatchIf(i);
            param.setMatchIf(matchIf);
            if (isInputParam(i)) {
                inputs.add(param);
            } else {
                outputs.add(param);
            }
        }

        List<IdiomTemplateType> templates = idiom.getTemplate();
        for (int templateNum = 0; templateNum < numTemplates(); templateNum++) {
            IdiomTemplateType template = new IdiomTemplateType();
            template.setId(getTemplateId(templateNum));
            template.setPrecedence(BigInteger
                    .valueOf(getTemplatePrecedence(templateNum)));
            List<Object> actions = template.getActionOrNamedAction();
            for (int actionNum = 0; actionNum < templateLength(templateNum); actionNum++) {
                Object action;
                if (isNamedTemplateAction(templateNum, actionNum)) {
                    IdiomTemplateNamedActionType act = new IdiomTemplateNamedActionType();
                    ActionDef calledAction = (ActionDef) getActionModel()
                            .getType(getTemplateActionName(templateNum,
                                    actionNum));
                    act.setId(calledAction.getName().getFullName());
                    List<IdiomTemplateNamedActionParamType> params = act
                            .getIdiomParam();
                    for (int paramNum = 0; paramNum < size(); paramNum++) {
                        String idiomParam = getParamName(paramNum);
                        for (String actionParam : getBoundTemplateParams(
                                templateNum, actionNum, paramNum)) {
                            IdiomTemplateNamedActionParamType param = new IdiomTemplateNamedActionParamType();
                            param.setActionParam(actionParam);
                            param.setId(idiomParam);
                            params.add(param);
                        }
                    }
                    action = act;
                } else {
                    IdiomTemplateActionType act = new IdiomTemplateActionType();
                    ActionFamilyDef calledAction = (ActionFamilyDef) getActionModel()
                            .getType(getTemplateActionName(templateNum,
                                    actionNum));
                    act.setFamily(calledAction.getName().getFullName());
                    List<IdiomTemplateActionParamType> params = act
                            .getIdiomParam();
                    for (int paramNum = 0; paramNum < size(); paramNum++) {
                        String idiomParam = getParamName(paramNum);
                        for (String actionParam : getBoundTemplateParams(
                                templateNum, actionNum, paramNum)) {
                            IdiomTemplateActionParamType param = new IdiomTemplateActionParamType();
                            param.setRole(actionParam);
                            param.setId(idiomParam);
                            params.add(param);
                        }
                    }
                    action = act;
                }
                actions.add(action);
            }
            templates.add(template);
        }
    }

    @Override
    protected Set<ActionModelDef> getRequiredDefs()
            throws PALException {
        Set<ActionModelDef> result = new HashSet<ActionModelDef>();
        for (int templateNum = 0; templateNum < numTemplates(); templateNum++) {
            for (int actionNum = 0; actionNum < templateLength(templateNum); actionNum++) {
                if (isNamedTemplateAction(templateNum, actionNum)) {
                    ActionDef calledAction = (ActionDef) getActionModel()
                            .getType(getTemplateActionName(templateNum,
                                    actionNum));
                    result.add(calledAction);
                } else {
                    ActionFamilyDef calledAction = (ActionFamilyDef) getActionModel()
                            .getType(getTemplateActionName(templateNum,
                                    actionNum));
                    result.add(calledAction);
                }
            }
        }
        return result;
    }

    @Override
    public TypeName getParamTypeName(int pos) {
        return null;
    }

    /**
     * Retrieves the {@code matchIf} attribute for the given parameter. This
     * parameter describes under what conditions values of the parameter will
     * match each other in demonstrations containing multiple candidate actions.
     *
     * @param paramNum
     *            the parameter number to retrieve {@code matchIf} for
     * @return {@code equals} (the default) or {@code singleton}
     */
    public String getParamMatchIf(int paramNum) {
        String paramName = getParamName(paramNum);
        ATRMap props = getAtr().getProperties();
        ATRMap paramMap = (ATRMap) props.get("$" + paramName);
        ATRLiteral matchLit = (ATRLiteral) paramMap.get("matchIf");
        return matchLit.getString();
    }

    /**
     * Provides the number of templates which this idiom defines.
     *
     * @return the number of action templates defined by this idiom
     */
    public int numTemplates() {
        ATRMap props = getAtr().getProperties();
        ATRList templatesTerm = (ATRList) props.get(TypeUtil.IDIOM_TEMPLATES);
        return templatesTerm.size();
    }

    /**
     * Provides the identifier of the given template for this idiom. Each
     * template must have a unique identifier within the context of its idiom
     * definition.
     *
     * @param templateNum
     *            the template to retrieve the identifier of
     * @return the identifier of the requested template
     */
    public String getTemplateId(int templateNum) {
        ATRMap props = getAtr().getProperties();
        ATRList templatesTerm = (ATRList) props.get(TypeUtil.IDIOM_TEMPLATES);
        ATRList templateTerm = (ATRList) templatesTerm.get(templateNum);
        ATRLiteral idLit = (ATRLiteral) templateTerm.get(0);
        return idLit.getString();
    }

    /**
     * Given the name of an idiom template, retrieve the corresponding template
     * number.
     *
     * @param templateID
     *            the name of the desired template, as specified in the action
     *            model XML
     * @return a number suitable for use by the other template-related methods
     *         of this class, or {@code -1} if the requested template is not
     *         found
     */
    public int getTemplateNum(String templateID) {
        ATRMap props = getAtr().getProperties();
        ATRList templates = (ATRList) props.get(TypeUtil.IDIOM_TEMPLATES);
        int num = 0;
        for (ATRTerm term : templates.getTerms()) {
            ATRList template = (ATRList)term;
            ATRLiteral idLit = (ATRLiteral)template.get(0);
            if (idLit.getString().equals(templateID))
                return num;
            num++;
        }

        return -1;
    }

    /**
     * Provides the precedence of a given template for this idiom. Higher
     * precedence numbers indicate that the template will be chosen over
     * lower-valued ones.
     *
     * @param templateNum
     *            the template to retrieve precedence of
     * @return the precedence of the indicated template
     */
    public int getTemplatePrecedence(int templateNum) {
        ATRMap props = getAtr().getProperties();
        ATRList templatesTerm = (ATRList) props.get(TypeUtil.IDIOM_TEMPLATES);
        ATRList templateTerm = (ATRList) templatesTerm.get(templateNum);
        ATRLiteral precLit = (ATRLiteral) templateTerm.get(1);
        return precLit.getInt();
    }

    /**
     * Provides the length of the indicated action template of this idiom.
     *
     * @param templateNum
     *            the template to retrieve the length of
     * @return the number of actions or action families in the indicated
     *         template
     */
    public int templateLength(int templateNum) {
        ATRMap props = getAtr().getProperties();
        ATRList templatesTerm = (ATRList) props.get(TypeUtil.IDIOM_TEMPLATES);
        ATRList templateTerm = (ATRList) templatesTerm.get(templateNum);
        ATRList actionsTerm = (ATRList) templateTerm.get(2);
        return actionsTerm.size();
    }

    /**
     * Provides the name of the indicated action reference in the indicated
     * template of this idiom.
     *
     * @param templateNum
     *            the template number in which to find the indicated action
     * @param actionNum
     *            the action reference to retrieve the name of
     * @return the name of the indicated action
     * @throws PALException
     *             if a parse error occurs
     */
    public SimpleTypeName getTemplateActionName(int templateNum,
                                                int actionNum)
            throws PALException {
        ATRMap props = getAtr().getProperties();
        ATRList templatesTerm = (ATRList) props.get(TypeUtil.IDIOM_TEMPLATES);
        ATRList templateTerm = (ATRList) templatesTerm.get(templateNum);
        ATRList actionsTerm = (ATRList) templateTerm.get(2);
        ATRLiteral actionLit = (ATRLiteral) actionsTerm.get(actionNum);
        String actionStr = actionLit.getString();
        ATRIdiomTemplateAction action;
        try {
            action = (ATRIdiomTemplateAction) IdiomTemplateFactory
                    .parseStatement(actionStr);
        } catch (LumenSyntaxError e) {
            throw new PALException("Parse error in '" + actionStr + "' for "
                    + this, e);
        }
        String actionNameStr = action.getActionReference();
        SimpleTypeName actionName = (SimpleTypeName) TypeNameFactory
                .makeName(actionNameStr);
        return actionName;
    }

    /**
     * Indicates if the specified template action is a reference to an action
     * family or a specific action.
     *
     * @param templateNum
     *            the template of the action in question
     * @param actionNum
     *            the action number of the indicated template
     * @return {@code true} if the indicated action is a reference to a specific
     *         action rather than an action family
     * @throws PALException
     *             if a relevant action definition cannot be retrieved, or a
     *             parse error occurs
     */
    public boolean isNamedTemplateAction(int templateNum,
                                         int actionNum)
            throws PALException {
        SimpleTypeName actionName = getTemplateActionName(templateNum, actionNum);
        ActionModelDef amDef = getActionModel().getType(actionName);
        if (amDef instanceof ActionDef) {
            return true;
        } else if (amDef instanceof ActionFamilyDef) {
            return false;
        } else {
            throw new PALException("Couldn't retrieve " + actionName
                    + " for template of " + this);
        }
    }

    /**
     * Provides the bindings for a given idiom parameter to an action reference
     * inside a template. For a given template, find the referenced action (or
     * action family) call. Find all parameters to that action call which are
     * bound to the indicated idiom parameter.
     *
     * @param templateNum
     *            the template in which to find the requested action
     * @param actionNum
     *            the action call to retrieve bindings for
     * @param paramNum
     *            the parameter of this idiom to retrieve bindings for
     * @return the names of the action parameters which are bound to the
     *         indicated idiom parameter
     * @throws PALException
     *             if a parse error occurs
     */
    public Set<String> getBoundTemplateParams(int templateNum,
                                              int actionNum,
                                              int paramNum) throws PALException {
        Set<String> result = new HashSet<String>();
        String idiomParamName = getParamName(paramNum);
        ATRMap props = getAtr().getProperties();
        ATRList templatesTerm = (ATRList) props.get(TypeUtil.IDIOM_TEMPLATES);
        ATRList templateTerm = (ATRList) templatesTerm.get(templateNum);
        ATRList actionsTerm = (ATRList) templateTerm.get(2);
        ATRLiteral actionLit = (ATRLiteral) actionsTerm.get(actionNum);
        String actionStr = actionLit.getString();
        ATRIdiomTemplateAction action;
        try {
            action = (ATRIdiomTemplateAction) IdiomTemplateFactory
                    .parseStatement(actionStr);
        } catch (LumenSyntaxError e) {
            throw new PALException("Parse error in '" + actionStr + "' for "
                    + this, e);
        }
        Map<String, Collection<String>> paramMap = action.getIdiomParamMap();
        Collection<String> bindings = paramMap.get(idiomParamName);
        if (bindings != null) {
            result.addAll(bindings);
        }
        return result;
    }
}
