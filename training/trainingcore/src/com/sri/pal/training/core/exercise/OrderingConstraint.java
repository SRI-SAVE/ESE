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

import com.sri.pal.training.core.basemodels.OrderingConstraintBase;

public class OrderingConstraint extends OrderingConstraintBase {

    public OrderingConstraint() {
    }

    public OrderingConstraint(String predecessorStepName, String successorStepName) {
        this.predecessor = predecessorStepName;
        this.successor = successorStepName;
    }

    public OrderingConstraint(String predecessorStepName, String successorStepName, String reason) {
        this(predecessorStepName, successorStepName);
        this.reason = reason;
    }
    
    @Override
    public boolean equals(Object o) {
    	if (!(o instanceof OrderingConstraint)) return false;
    	OrderingConstraint that = (OrderingConstraint) o;
    	return this.predecessor.equals(that.predecessor) && this.successor.equals(that.successor); 
    }
    
    @Override
    public int hashCode() {
    	return (((null == predecessor) ? "NULL" : predecessor) + "<" +
    			((null == successor) ? "NULL" : successor)).hashCode();
    }
    
}
