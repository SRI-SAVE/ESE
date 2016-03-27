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

import javax.xml.bind.annotation.XmlTransient;

import com.sri.pal.training.core.basemodels.QueryIssueBase;
import com.sri.pal.training.core.exercise.QueryConstraint;

public class QueryIssue extends QueryIssueBase {
    @XmlTransient
    private final QueryConstraint constraint;
    
    @Deprecated
    public QueryIssue() {
        constraint = null; 
    }
    
    public QueryIssue(QueryConstraint constraint) {
        this.constraint = constraint;     
    }
    
    public QueryConstraint getConstraint() {
        return constraint;
    }
}