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

package com.sri.pal.training.core.assessment;

import java.util.List;

import com.sri.pal.training.core.basemodels.ArgumentLocationBase;

public class ArgumentLocation extends ArgumentLocationBase {
    public ArgumentLocation() {
        super();
    }
    
    public ArgumentLocation(final int atomIdx, final List<String> accessors) {
        super();
        setAtomIndex(atomIdx);
        getAccessors().addAll(accessors);
    }

    public String getAccessorString() {
        StringBuffer accessor = new StringBuffer();
        for (int i = accessors.size() - 1; i >= 0; i--)
            accessor.append(accessors.get(i) + " of ");
        return accessor.substring(0, accessor.length() - 3);
    }
}
