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

// $Id: ATRTestUtil.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRFunctionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.Structure;
import com.sri.ai.lumen.atr.decl.impl.CTRTypeDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRNoEvalTerm;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.ATRVariable;
import com.sri.ai.lumen.atr.term.impl.CTRLiteral;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameExpr;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.messages.contents.ActionCategory;
import com.sri.tasklearning.spine.messages.contents.ParamClass;

public class ATRTestUtil {
    public static ATRTypeDeclaration makeCustomType(SimpleTypeName name,
                                                    Class<?> baseClass) {
        ATRTypeDeclaration type = CTRTypeDeclaration.createApplicationType(
                name.getFullName(), null, null, baseClass.getCanonicalName());
        return type;
    }

    public static ATRTypeDeclaration makeCustomType(String name,
                                                    Class<?> baseClass) {
        SimpleTypeName typeName = (SimpleTypeName) TypeNameFactory
                .makeName(name);
        return makeCustomType(typeName, baseClass);
    }

    public static ATRTypeDeclaration makeList(SimpleTypeName name,
                                              ATR elementType) {
        TypeName elementName = TypeUtil.getName(elementType);
        String equivStr = new TypeNameExpr("list", elementName).getFullName();
        List<String> equivs = new ArrayList<String>();
        equivs.add(equivStr);
        ATRTypeDeclaration type = CTRTypeDeclaration.createAliasType(
                name.getFullName(), equivs, null);
        return type;
    }

    public static ATRActionDeclaration makeAction(SimpleTypeName name,
                                                  ATRParameter[] params,
                                                  ATRMap props,
                                                  ATRActionDeclaration parent) {
        CTRConstructor ctrBuilder = new CTRConstructor();
        ATRSig sig = ctrBuilder.createSignature(name.getFullName(),
                Arrays.asList(params));
        if (props == null) {
            props = ctrBuilder.createMap(new HashMap<String, ATRTerm>());
        }
        @SuppressWarnings("unchecked")
        Map<String, ATRTerm> propMap = ((ATRMap.Mutable<ATRTerm>) props)
                .getMap();
        propMap.put(TypeUtil.CONSTRAINTS,
                ctrBuilder.createNoEval(ctrBuilder.createNull()));
        ATRLiteral effectsTerm = ctrBuilder
                .createLiteral(ActionCategory.EFFECTOR.getName(), null);
        propMap.put(TypeUtil.ACTION_CATEGORY, effectsTerm);
        ATRLiteral transientTerm = ctrBuilder.createLiteral(Boolean.TRUE
                .toString(), null);
        propMap.put(TypeUtil.TRANSIENT, transientTerm);
        for (int i = 0; i < params.length; i++) {
            String paramName = params[i].getVariable().getVariableName();
            ATRMap paramProps = (ATRMap) propMap.get("$" + paramName);
            if(paramProps == null) {
                paramProps = ctrBuilder.createMap(new HashMap<String, ATRTerm>());
                propMap.put("$" + paramName, paramProps);
            }
            @SuppressWarnings("unchecked")
            Map<String, ATRTerm> paramMap = ((ATRMap.Mutable<ATRTerm>) paramProps)
                    .getMap();
            paramMap.put(TypeUtil.PARAM_DESCRIPTION,
                    ctrBuilder.createLiteral("description " + i, null));
            if (params[i].getMode() == Modality.INPUT) {
                paramMap.put(TypeUtil.PARAM_CLASS, ctrBuilder
                        .createLiteral(ParamClass.GENERALIZABLE.getName(), null));
            } else {
                paramMap.put(TypeUtil.PARAM_CLASS,
                        ctrBuilder.createLiteral(ParamClass.EXTERNAL.getName(), null));
            }
        }
        if (parent != null) {
            TypeName parentName = TypeUtil.getName(parent);
            String parentStr = parentName.getFullName();
            propMap.put(TypeUtil.PARENT, new CTRLiteral(parentStr));
        }
        return ctrBuilder.createActionDeclaration(sig, (ATRTask) null, props);
    }

    public static ATRActionDeclaration makeAction(String name,
                                                  ATRParameter[] params,
                                                  ATRMap props,
                                                  ATRActionDeclaration parent) {
        SimpleTypeName typeName = (SimpleTypeName) TypeNameFactory
                .makeName(name);
        return makeAction(typeName, params, props, parent);
    }

    public static ATRActionDeclaration makeProcedure(SimpleTypeName name,
                                                     ATRActionDeclaration parent) {
        CTRConstructor ctrBuilder = new CTRConstructor();
        ATRActionDeclaration action = makeAction(name, new ATRParameter[0],
                null, parent);
        ATRSig sig = action.getSignature();
        ATRMap props = action.getProperties();
        ATRTask task = ctrBuilder.createAction(name.getFullName(),
                new ArrayList<ATRTerm>(), null);
        return ctrBuilder.createActionDeclaration(sig, task, props);
    }

    public static ATRTypeDeclaration makeAliasType(String name,
                                                   String equivName,
                                                   ATRMap props) {
        List<String> equivList = new ArrayList<String>();
        equivList.add(equivName);
        ATRTypeDeclaration type = CTRTypeDeclaration.createAliasType(name,
                equivList, props);
        return type;
    }

    public static Structure makeStruct(String name,
                                       String parentName,
                                       String[] fieldNames,
                                       String[] fieldTypes,
                                       boolean opaque) {
        CTRConstructor ctrBuilder = new CTRConstructor();
        List<String> names = Arrays.asList(fieldNames);
        List<String> types = Arrays.asList(fieldTypes);
        Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
        propMap.put(TypeUtil.OPAQUE,
                ctrBuilder.createLiteral(Boolean.toString(opaque), null));
        propMap.put(TypeUtil.DESCRIPTION,
                ctrBuilder.createLiteral("test struct type", null));
        ATRMap props = ctrBuilder.createMap(propMap);
        return CTRTypeDeclaration.createStructureType(name, null, props,
                parentName, names, types);
    }

    public static ATRFunctionDeclaration makeConstraintDef(String name,
                                                           String[] paramNames,
                                                           String[] paramDescs) {
        CTRConstructor ctrBuilder = new CTRConstructor();

        List<ATRParameter> params = new ArrayList<ATRParameter>();
        for (String paramName : paramNames) {
            ATRVariable var = ctrBuilder.createVariable(paramName);
            ATRParameter param = ctrBuilder.createParameter(var,
                    Modality.INPUT, null, null);
            params.add(param);
        }

        List<ATRTerm> paramDescrs = new ArrayList<ATRTerm>();
        for (String paramDesc : paramDescs) {
            if (paramDesc == null) {
                paramDesc = "";
            }
            ATRLiteral lit = ctrBuilder.createLiteral(paramDesc, null);
            paramDescrs.add(lit);
        }
        Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
        propMap.put(TypeUtil.CONSTRAINT_DESCRIPTIONS,
                ctrBuilder.createList(paramDescrs));

        ATRMap props = ctrBuilder.createMap(propMap);
        ATRSig sig = ctrBuilder.createSignature(name, params);
        ATRNoEvalTerm eval = ctrBuilder.createNoEval(ctrBuilder
                .createSymbol(name));
        ATRFunctionDeclaration atr = ctrBuilder.createFunctionDeclaration(sig,
                eval, null, props);
        return atr;
    }
}
