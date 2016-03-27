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

import com.sri.pal.training.core.basemodels.TaskSolutionBase;

public class TaskSolution extends TaskSolutionBase {
    public Step locateStep(String id) {
        return locateStepAux(getOption(), id); 
    }
    
    private Step locateStepAux(Option opt, String id) {
        for (Step step : opt.getSteps()) {
            if (step.getId().equals(id))
                return step;
            else if (step.isOptionSet()) {              
                for (Option subOpt : step.getOptionSet().getOptions()) {
                    Step found = locateStepAux(subOpt, id);
                    if (found != null)
                        return found;
                }
            }
        }
        return null;
    }
}
