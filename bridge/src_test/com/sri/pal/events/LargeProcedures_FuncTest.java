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

/* $Id: LargeProcedures_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $ */
package com.sri.pal.events;

import static com.sri.pal.ProcedureLearner.NAMESPACE;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * More event tests, just separated out so Event_FuncTest doesn't become too
 * large
 *
 * @author Valerie Wagner Date: Jan 3, 2007
 */
public class LargeProcedures_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    private static final String WAR_AND_PEACE = "../test/data/WarAndPeace.txt";

    private static ActionInvocation action144Event;
    private static SimpleTypeName act144Name;
    private static SimpleTypeName act170Name;
    private static SimpleTypeName act202Name;

    @BeforeClass
    public static void initialize() throws Exception {
        setup();

        // New action model events
        act144Name = (SimpleTypeName) TypeNameFactory.makeName("action144",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(act144Name, callbackHandler);
        String item1 = "item1-test-1234";
        action144Event = ((ActionDef) actionModel.getType(act144Name)).bindAll(
                null, "item5item2", item1);

        act170Name = (SimpleTypeName) TypeNameFactory.makeName("action170",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(act170Name, callbackHandler);

        act202Name = (SimpleTypeName) TypeNameFactory.makeName("action202",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(act202Name, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void testTenOfSameEventSameValues()
            throws Exception {
        Vector<ActionInvocation> events = new Vector<ActionInvocation>();
        for (int i = 0; i < 10; i++) {
            events.add(action144Event);
        }
        ProcedureLearner.learnAndInvokeProcedure(events,
                "testTenOfSameEventSameValues");
    }

    @Test
    public void testTenOfSameEventDifferentValues()
            throws Exception {
        Vector<ActionInvocation> events = new Vector<ActionInvocation>();
        for (int i = 0; i < 9; i++) {
            ActionInvocation ceEvent = ((ActionDef) actionModel
                    .getType(act144Name)).bindAll(null, "item5item2",
                    "entityid" + i);
            events.add(ceEvent);
        }
        ProcedureLearner.learnAndInvokeProcedure(events,
                "testTenOfSameEventDifferentValues");
    }

    @Test
    public void testEmptyListInput()
            throws Exception {
        // Pass an empty list as an input parameter, and then make
        // sure it doesn't get listed as an input.
        // 6/18/2008 VW this is a change in behavior.  Previously, empty lists were generalized.
        // however it was decided that LAPDOG will not generalize empty lists, so the empty list
        // will not be an input to a procedure
        List<String> list = new ArrayList<String>();
        ActionInvocation arme = ((ActionDef) actionModel.getType(act170Name))
                .bindAll(null, "entity1", "members", list);

        long time1 = System.currentTimeMillis();
        ProcedureDef task = ProcedureLearner.learnProcedure(arme, "testEmptyListInput");
        long time2 = System.currentTimeMillis();

        ProcedureLearner.invokeTask(task);
        long time3 = System.currentTimeMillis();

        log.info("Learning took " + (time2 - time1) + " ms.");
        log.info("Executing took " + (time3 - time2) + " ms.");

        assertEquals("Wrong number of inputs", 1, task.numInputParams());
        assertEquals("Wrong number of outputs", 0, task.size() - task.numInputParams());
    }

    // Some versions of OAA can't handle IclStr objects larger than some size
    // (65k?). Since we store a Task as an IclStr, this prevents large
    // procedures from being learned.
    @Test
    public void checkIclSizeLimits()
            throws Exception {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 1024; i++) {
            sb.append("A");
        }
        String kByte = sb.toString();
        sb = new StringBuffer();
        for (int i = 0; i < 257; i++) {
            sb.append(kByte);
        }
        sb.append("RGH!");
        String bigString = sb.toString();
        ActionInvocation action = ((ActionDef) actionModel.getType(act202Name))
                .bindAll(null, "entityId", "attr", bigString);
        ProcedureLearner.learnAndInvokeProcedure(action, "checkIclSizeLimits");
    }

    /**
     * See https://jira.esd.sri.com/browse/CPAL-183 . Large strings were causing
     * Lumen's regex parser to hit a StackOverflowException. This is likely
     * influenced by the content of the string and the number of potential
     * matches.
     */
    @Test
    public void warAndPeace()
            throws Exception {
        File file = new File(WAR_AND_PEACE);
        String text = ProcedureLearner.readWholeFile(file);
        ActionInvocation action = ((ActionDef) actionModel.getType(act202Name))
                .bindAll(null, "entityId", "attr", text);
        ProcedureLearner.learnAndInvokeProcedure(action, "warAndPeace");
    }
}
