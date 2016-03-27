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

// $Id: RecursiveExecutor.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.stress;

import java.util.List;

import com.sri.pal.ActionExecutor;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionInvocation.StepCommand;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.PALException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.common.SimpleTypeName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecursiveExecutor
        implements ActionExecutor {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    private ProcedureDef proc;
    private boolean keepRunning = true;

    public void setProcedure(ProcedureDef proc) {
        this.proc = proc;
    }

    public void stop() {
        keepRunning = false;
    }

    @Override
    public void cancel(ActionStreamEvent invocation) {
        log.warn("Not implemented");
    }

    @Override
    public void execute(ActionInvocation invocation)
            throws PALException {
        SimpleTypeName name = invocation.getDefinition().getName();
        log.debug("Called {}", name);
        if("action1".equals(name.getSimpleName()) && proc != null && keepRunning) {
            log.debug("Calling {}", proc.getName());
            ActionInvocation newProc = proc.invoke(null);
            newProc.start();
        }
        invocation.setStatus(Status.ENDED);
    }

    @Override
    public void executeStepped(ActionInvocation invocation)
            throws PALException {
        execute(invocation);
    }

    @Override
    public void continueStepping(ActionInvocation invocation,
                                 StepCommand command,
                                 List<Object> actionArgs)
            throws PALException {
        throw new PALException("Not implemented.");
    }
}
