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
package com.sri.pal.training.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.Bridge;
import com.sri.pal.TypeDef;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.training.core.exercise.Value;

public class ValueUtil {
    private static final Logger log = LoggerFactory.getLogger(ValueUtil.class);

    public static TypeDef getTypeDef(Value value,
                                     Bridge bridge) {
        try {
            TypeName name = TypeNameFactory.makeName(value.getType());
            TypeDef type = (TypeDef) bridge.getActionModel().getType(name);
            return type;
        } catch (Exception e) {
            log.error("Failed to deserialize value", e);
            return null;
        }
    }

    public static Object getObject(Value value,
                                   Bridge bridge) {
        if (!value.isDeserialized()) {
            try {
                TypeDef type = getTypeDef(value, bridge);
                ATRTerm term = ATRSyntax.CTR.termFromSource(value.getCtrs());
                value.setValue(type.fromAtr(term));
            } catch (Exception e) {
                log.error("Failed to deserialize value", e);
            }
        }

        return value.getValue();
    }

    public static void setObject(Value value,
                                 Object obj,
                                 Bridge bridge) {
        value.setValue(obj);

        try {
            TypeDef type = getTypeDef(value, bridge);
            ATRTerm term = type.toAtr(obj);
            value.setCtrs(ATRSyntax.toSource(term));
        } catch (Exception e) {
            Class<?> cl = null;
            if (obj != null) {
                cl = obj.getClass();
            }
            log.error("Failed to serialized value " + obj + " (" + cl + ")",
                    e);
        }
    }
}
