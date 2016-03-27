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

// $Id: MemoryTypeStorage.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.upgrader;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sri.pal.PALException;
import com.sri.pal.TypeStorage;
import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.RequestCanceler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.util.TypeUtil;

/**
 * This is a non-persistent TypeStorage implementation, used only for upgrading
 * and for some tests.
 */
public class MemoryTypeStorage
        implements TypeStorage {
    private final Map<SimpleTypeName, String> types;

    public MemoryTypeStorage() {
        types = new HashMap<SimpleTypeName, String>();
    }

    @Override
    public RequestCanceler loadType(CallbackHandler<String> callbackHandler,
                                    SimpleTypeName name) {
        String typeStr = types.get(name);
        callbackHandler.result(typeStr);

        return new RequestCanceler() {
            @Override
            public void cancel() {
                // Ignore
            }
        };
    }

    @Override
    public Set<SimpleTypeName> listTypes(Subset... subsets)
            throws PALException {
        Set<SimpleTypeName> result = new HashSet<SimpleTypeName>();

        Set<Subset> subsetsSet = new HashSet<Subset>();
        if (subsets == null || subsets.length == 0) {
            for (Subset item : Subset.values()) {
                subsetsSet.add(item);
            }
        } else {
            subsetsSet.addAll(Arrays.asList(subsets));
        }

        for(SimpleTypeName name : types.keySet()) {
            String typeStr = types.get(name);
            if(typeStr == null) {
                // Ignore
            } else if (TypeUtil.isProcedureString(typeStr)) {
                if (subsetsSet.contains(Subset.PROCEDURE)) {
                    result.add(name);
                }
            } else if (TypeUtil.isActionString(typeStr)) {
                if (subsetsSet.contains(Subset.ACTION)) {
                    result.add(name);
                }
            } else if (TypeUtil.isActionFamilyString(typeStr)) {
                if (subsetsSet.contains(Subset.FAMILY)) {
                    result.add(name);
                }
            } else if (TypeUtil.isIdiomString(typeStr)) {
                if (subsetsSet.contains(Subset.IDIOM)) {
                    result.add(name);
                }
            } else if (TypeUtil.isConstraintString(typeStr)) {
                if (subsetsSet.contains(Subset.CONSTRAINT)) {
                    result.add(name);
                }
            } else {
                if (subsetsSet.contains(Subset.TYPE)) {
                    result.add(name);
                }
            }
        }

        return result;
    }

    @Override
    public void putType(SimpleTypeName name,
                        String typeStr)
            throws PALException {
        types.put(name, typeStr);
    }
}
