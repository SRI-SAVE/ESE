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

import com.sri.pal.training.core.basemodels.ValueConstraintBase;

public class ValueConstraint extends ValueConstraintBase {    

    public ValueConstraint() {
    }

    public ValueConstraint(String parameterName, Collection<Value> values) {
        this.parameter = parameterName;
        this.values = new ArrayList<Value>(values);
    }

    public ValueConstraint(String parameterName, Collection<Value> values, String reason) {
        this(parameterName, values);
        this.reason = reason;
    }

    public ValueConstraint(String parameterName, Value minValue, Value maxValue) {
        this.parameter = parameterName;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }
}
