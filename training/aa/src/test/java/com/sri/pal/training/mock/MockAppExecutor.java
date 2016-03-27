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

// $Id: MockAppExecutor.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.training.mock;

import java.util.List;

import com.sri.pal.ActionExecutor;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionInvocation.StepCommand;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.Bridge;
import com.sri.pal.PALException;
import com.sri.pal.Struct;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;

public class MockAppExecutor
        implements ActionExecutor {
    private final Bridge bridge;

    MockAppExecutor(Bridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public void execute(ActionInvocation invocation)
            throws PALException {
        SimpleTypeName name = invocation.getDefinition().getName();
        if (name.equals(MockApplication.GEO_BOX_PRED)) {
            isInGeoBox(invocation);
        } else {
            ErrorInfo err = new ErrorInfo(bridge.getSpine().getClientId(), 42,
                    "Unknown action", "Unknown action " + name, null);
            invocation.error(err);
        }
    }

    @Override
    public void executeStepped(ActionInvocation invocation)
            throws PALException {
        // Do nothing.
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

    private void isInGeoBox(ActionInvocation invoc) {
        Boolean result = null;
        ErrorInfo error = null;
        invoc.setStatus(Status.RUNNING);
        Struct latlon = (Struct) invoc.getValue(0);
        // double lat = (Double) latlon.getValue(0);
        double lon = (Double) latlon.getValue(1);
        String region = (String) invoc.getValue(1);
        if (region.equals("Western Hemisphere")) {
            if (lon < 0) {
                result = true;
            } else {
                result = false;
            }
        } else {
            error = new ErrorInfo(bridge.getSpine().getClientId(), 43,
                    "bad region", "Unknown region " + region, null);
        }
        if (error != null) {
            invoc.error(error);
        } else {
            invoc.setValue(2, result);
            invoc.setStatus(Status.ENDED);
        }
    }
}
