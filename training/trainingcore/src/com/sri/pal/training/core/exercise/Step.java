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

import javax.xml.bind.annotation.XmlTransient;

import com.sri.pal.training.core.basemodels.StepBase;

public class Step extends StepBase {
    @XmlTransient
    private Option option; // The parent option of this step
    @XmlTransient
    private TaskSolution taskSolution; // non-null if nested directly beneath a TaskSolution

    public Step() {
    }
    
    public Step(String id, Atom atom, String subtaskName, Option option) {
        this.id = id;
        this.atom = atom;
        this.subtask = subtaskName;
        this.option = option;
    }
    
    public Option getOption() {
        return option;
    }

    @Deprecated
    public void setOption(Option option) {
        this.option = option;
    }

    public TaskSolution getTaskSolution() {
        return taskSolution;
    }

    @Deprecated
    public void setTaskSolution(TaskSolution taskSolution) {
        this.taskSolution = taskSolution;
    }

    public boolean isAtom() {
        return getAtom() != null;
    }

    public boolean isOptionSet() {
        return getOptionSet() != null;
    }

    public Option getParentOption() {
        return option;
    }

    public String findSubTask() {
        Step s = this;

        while (true) {
            if (s.getSubtask() != null)
                return s.getSubtask();
            if (s.getOption().getOptionSet() != null)
                s = s.getOption().getOptionSet().getStep();
            else 
                break; 
        }

        return null;
    }
}
