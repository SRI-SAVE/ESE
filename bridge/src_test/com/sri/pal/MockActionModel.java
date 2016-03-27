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

// $Id: MockActionModel.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.HashMap;
import java.util.Map;

import com.sri.pal.PrimitiveTypeDef.Predefined;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.SpineException;

public class MockActionModel
        extends ActionModel {
    private final Map<SimpleTypeName, ActionModelDef> types;
    private final Bridge bridge;

    MockActionModel(Bridge bridge)
            throws SpineException {
        super(bridge);
        this.bridge = bridge;
        types = new HashMap<SimpleTypeName, ActionModelDef>();
    }

    @Override
    public void storeType(SimpleTypeName name,
                          ActionModelDef type) {
        types.put(name, type);
    }

    @Override
    public ActionModelDef getSimpleType(SimpleTypeName name) {
        // If it's a predefined primitive, return it.
        if (TypeNameFactory.isPrimitive(name)) {
            Predefined predef = Predefined.valueOf(name.getFullName()
                    .toUpperCase());
            return PrimitiveTypeDef.getPrimitive(predef, bridge);
        }

        return types.get(name);
    }
}
