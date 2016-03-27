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

import com.sri.pal.training.core.basemodels.TaskAssessmentBase;

public class TaskAssessment extends TaskAssessmentBase {
    @XmlTransient
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean hasProblems() {
    	if (getEqualityIssues().size() > 0 || getValueIssues().size() > 0 ||
    	    getOrderingIssues().size() > 0 || getQueryIssues().size() > 0 ||
    	    getStateIssues().size() > 0    || getMissingAtomIssues().size() > 0)
    		return true;
    	
    	return false;
    }
}
