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

package com.sri.tasklearning.ui.core.procedure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.task.ATRSequence;
import com.sri.ai.lumen.atr.task.ATRTask;

/**
 * PUTR implementation of ATRSequence. The PAL UI generally uses lists of
 * {@code StepModel} to track steps and therefore doesn't do much with 
 * this class. However, we need this class to complete our ATR implementation. 
 */
public class StepSequence implements ATRSequence {
    protected List<ATRTask> tasks;

    public StepSequence(Iterable<? extends ATRTask> s) {
        tasks = new ArrayList<ATRTask>();
        Iterator<?extends ATRTask> itr = s.iterator();
        while(itr.hasNext()) {
            ATRTask element = (ATRTask)itr.next();
            tasks.add(element);
        }
    }

    @Override
    public ATRCat getCategory() {
        return ATRCat.SEQUENCE;
    }

    @Override
    public List<? extends ATRTask> getTasks() {
        return tasks;
    }

    @Override
    public ATR getInternalSub() {
        return null;
    }
}