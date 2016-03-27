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

package com.sri.tasklearning.lumenpal.mock;

import com.sri.ai.lumen.mediator.MediatorException;
import com.sri.ai.lumen.mediator.TaskExecutionListener;
import com.sri.ai.lumen.mediator.TaskExecutor;

import java.util.List;
import java.util.Map;

public class MockTaskExecutor implements TaskExecutor {

    boolean wasCalled = false;

    @Override
    public void startTask(TaskExecutionListener taskListener,
                          String s,
                          String s1,
                          List<Object> objects,
                          Map<String, Object> stringObjectMap,
                          TaskExecutionListener parentTaskListener,
                          boolean stepped)
            throws MediatorException {
        wasCalled = true;
    }

    public boolean wasCalled() {
        return wasCalled;
    }
}
