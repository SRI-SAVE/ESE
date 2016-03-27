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

package com.sri.pal.events;

import static com.sri.pal.ProcedureLearner.NAMESPACE;
import static org.testng.AssertJUnit.fail;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Valerie Wagner
 *         Date: Oct 23, 2008
 */
public class Parameter_FuncTest extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    @BeforeClass
    public static void init()
            throws Exception {
        setup();
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void testParameterNameCollision() throws RemoteException, PALException {
        List<ActionInvocation> eventList = new ArrayList<ActionInvocation>();
        String item1 = "entityID1";


        for (int i = 0; i < 9; i++) {
            ActionInvocation action202Event = ((ActionDef) actionModel
                    .getType(TypeNameFactory.makeName("action202", "1.0", NAMESPACE)))
                    .invoke(null, item1, "title", "the-title-name" + i);
            eventList.add(action202Event);
        }

        ProcedureDef proc = ProcedureLearner.learnProcedure(eventList, "testParameterNameCollision");

        eventList.clear();

        ActionInvocation task1 = proc.invoke(null);

        int i = 100;
        for(int pNum = 0; pNum < proc.size(); pNum++) {
            task1.setValue(pNum, "foo" + (i++));
        }

        eventList.add(task1);
        ActionInvocation action202Event = ((ActionDef) actionModel
                .getType(TypeNameFactory.makeName("action202", "1.0", NAMESPACE))).invoke(
                null, "entityID2", "title", "the-title-name");

        eventList.add(action202Event);
        log.info("proc = " + proc);

        try {
            ProcedureDef proc2 = ProcedureLearner.learnProcedure(eventList, "nestedProcedure");
            log.info("proc2 = " + proc2);
        }
        catch (PALException e) {
            fail(e.getMessage());
        }

    }

}
