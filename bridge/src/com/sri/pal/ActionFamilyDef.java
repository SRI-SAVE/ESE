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

// $Id: ActionFamilyDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.pal.common.TypeName;
import com.sri.pal.jaxb.ActionFamilyParamType;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.pal.jaxb.FamilyType;
import com.sri.tasklearning.spine.messages.contents.ActionCategory;

public class ActionFamilyDef
        extends AbstractActionDef {
    protected ActionFamilyDef(ATR atr,
                              Bridge bridge)
            throws PALException {
        super(atr, bridge);
    }

    @Override
    public ActionCategory getCategory() {
        return null;
    }

    @Override
    protected void fillInXml(ActionModelType amXml)
            throws PALException {
        /* Build the <family> inside <actionModel>. */
        FamilyType famXml = new FamilyType();
        amXml.getFamily().add(famXml);
        famXml.setId(getName().getFullName());
        famXml.setBenign(isBenign());
        List<ActionFamilyParamType> inputs = famXml.getInputParam();
        List<ActionFamilyParamType> outputs = famXml.getOutputParam();
        for(int i = 0; i < size(); i++) {
            ActionFamilyParamType param = new ActionFamilyParamType();
            param.setRole(getParamName(i));
            if(isInputParam(i)) {
                inputs.add(param);
            } else {
                outputs.add(param);
            }
        }
    }

    @Override
    protected Set<ActionModelDef> getRequiredDefs()
            throws PALException {
        return new HashSet<ActionModelDef>();
    }

    @Override
    public TypeName getParamTypeName(int pos) {
        return null;
    }
}
