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

// $Id: MockTaskExecutionListener.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal.mock;

import java.util.List;

import com.sri.ai.lumen.mediator.MediatorException;
import com.sri.ai.lumen.mediator.TaskExecutionListener;
import com.sri.ai.lumen.runtime.LumenStackTraceElement;

public class MockTaskExecutionListener
        implements TaskExecutionListener {
    boolean wasCalled = false;
    private boolean wasCalledWithSuccess = false;
    private boolean wasCalledWithFailure = false;

    @Override
    public String getTaskMessageUID() {
        return null;
    }

    @Override
    public boolean taskFailed(String taskid,
                              Object reason,
                              List<LumenStackTraceElement> stack)
            throws MediatorException {
        wasCalled = true;
        wasCalledWithFailure = true;
        return false;
    }

    @Override
    public boolean taskSucceeded(String taskid,
                                 List<Object> resultList)
            throws MediatorException {
        wasCalled = true;
        wasCalledWithSuccess = true;
        return false;
    }

    public boolean wasCalled() {
        return wasCalled;
    }

    public boolean wasCalledWithSuccess() {
        return wasCalledWithSuccess;
    }

    public boolean wasCalledWithFailure() {
        return wasCalledWithFailure;
    }
}
