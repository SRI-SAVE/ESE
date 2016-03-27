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
package com.sri.pal.training.core.exercise;

import java.util.Collection;
import java.util.ArrayList;

import com.sri.pal.training.core.basemodels.EqualityConstraintBase;

public class EqualityConstraint extends EqualityConstraintBase {

    public EqualityConstraint() {
    }

    public EqualityConstraint(Collection<String> parameterNames) {
        this.parameters = new ArrayList<String>(parameterNames);
    }

    public EqualityConstraint(Collection<String> parameterNames, String reason) {
        this(parameterNames);
        this.reason = reason;
    }

}
