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

// $Id: NullyExecutor.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.List;

import com.sri.pal.ActionInvocation.StepCommand;
import com.sri.pal.ActionStreamEvent.Status;

public class NullyExecutor
        implements ActionExecutor {

    @Override
    public void execute(ActionInvocation invocation)
            throws PALException {
        invocation.setStatus(Status.RUNNING);
        ActionDef def = invocation.getDefinition();
        for (int i = def.numInputParams(); i < def.size(); i++) {
            invocation.setValue(i, null);
        }
        invocation.setStatus(Status.ENDED);
    }

    @Override
    public void executeStepped(ActionInvocation invocation)
            throws PALException {
        execute(invocation);
    }

    @Override
    public void cancel(ActionStreamEvent event) {
        // Do nothing.
    }

    @Override
    public void continueStepping(ActionInvocation invocation,
                                 StepCommand command,
                                 List<Object> actionArgs)
            throws PALException {
        // Do nothing.
    }
}
