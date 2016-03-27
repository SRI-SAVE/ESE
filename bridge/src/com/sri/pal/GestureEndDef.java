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

// $Id: GestureEndDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Collections;
import java.util.Set;

import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.tasklearning.spine.messages.contents.ActionCategory;
import com.sri.tasklearning.spine.util.TypeUtil;

class GestureEndDef
        extends AbstractActionDef {
    GestureEndDef(Bridge bridge)
            throws PALException {
        super(null, bridge);
    }

    @Override
    public SimpleTypeName getName() {
        return TypeUtil.GESTURE_END_NAME;
    }

    @Override
    public int numInputParams() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
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
    protected void fillInXml(ActionModelType amXml)
            throws PALException {
        throw new RuntimeException("No XML for " + getClass());
    }

    @Override
    public TypeName getParamTypeName(int pos) {
        throw new IllegalArgumentException("No parameters");
    }

    @Override
    public ActionCategory getCategory() {
        return ActionCategory.EFFECTOR;
    }

    @Override
    protected Set<ActionModelDef> getRequiredDefs()
            throws PALException {
        return Collections.emptySet();
    }
}
