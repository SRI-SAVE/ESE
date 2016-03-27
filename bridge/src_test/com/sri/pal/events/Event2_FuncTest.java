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
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.net.ServerSocketFactory;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.Learner;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureExecutor;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.VerifiableCallbackHandler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Valerie Wagner Date: Feb 12, 2007
 */
public class Event2_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory.getLogger("TestSourceLogger");

    private static SimpleTypeName act144Name;
    private static SimpleTypeName act170Name;
    private static SimpleTypeName act202Name;

    protected ActionInvocation addRelationMemberEvent;
    protected ActionInvocation action144Event;

    @BeforeClass
    public static void initialize() throws Exception {
        setup();

        act144Name = (SimpleTypeName) TypeNameFactory.makeName("action144",
                "1.0", NAMESPACE);
        actionModel.registerExecutor(act144Name, callbackHandler);
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
    public void testCreateitem2()
            throws Exception {

        action144Event = ((ActionDef) actionModel.getType(act144Name)).bindAll(
                null, "item5item2");

        List<String> singleitem2List = new ArrayList<String>();
        singleitem2List.add("theitem1");
        addRelationMemberEvent = ((ActionDef) actionModel.getType(act170Name))
                .bindAll(null, "theitem7", "members", singleitem2List);

        Vector<ActionInvocation> events = new Vector<ActionInvocation>();
        events.add(action144Event);
        events.add(addRelationMemberEvent);

        Learner learningBridge = palBridge.getLearner();

        ProcedureDef task = learningBridge.learn(
                "0x080e00000110c18e73f70af53b048079", null,
                events.toArray(new ActionInvocation[0]));

        assertNotNull("Procedure not learned", task);
        for(int i = 0; i < task.numInputParams(); i++) {
            log.info("Procedure input type ["
                    + task.getParamType(i)
                    + "] default value [" + task.getDefaultValue(i)
                    + "]");
        }
        for(int i = task.numInputParams(); i < task.size(); i++) {
            log.info("Procedure output type ["
                    + task.getParamType(i) + "]");
        }

        // All inputs should have display name and default values
        for(int i = 0; i < task.numInputParams(); i++) {
            assertNotNull("Display name is null for " + i, task.getParamDescription(i));
            assertNotNull("Default value is null for " + i, task
                    .getDefaultValue(i));
        }
    }

    @Test
    public void testMissingCallbacks()
            throws Exception {

        Vector<ActionInvocation> events = new Vector<ActionInvocation>();

        String entity = "entity-1234";
        String nameAtt = "name";

        ActionDef actDef = (ActionDef) actionModel.getType(act202Name);
        ActionInvocation event1 = actDef.invoke(null, entity, nameAtt, "name1");
        events.add(event1);

        ActionInvocation event2 = actDef.invoke(null, entity, nameAtt, "name1");
        events.add(event2);

        ActionInvocation event3 = actDef.invoke(null, entity, nameAtt, "name1");
        events.add(event3);

        ActionInvocation event4 = actDef.invoke(null, entity, nameAtt, "name1");
        events.add(event4);

        ActionInvocation event5 = actDef.invoke(null, entity, nameAtt, "name1");
        events.add(event5);

        ActionInvocation event6 = actDef.invoke(null, entity, nameAtt, "name1");
        events.add(event6);

        ActionInvocation event7 = actDef.invoke(null, entity, nameAtt, "name1");
        events.add(event7);

        ActionInvocation event8 = actDef.invoke(null, entity, nameAtt, "name1");
        events.add(event8);

        ActionInvocation event9 = actDef.invoke(null, entity, nameAtt, "name1");
        events.add(event9);

        ProcedureLearner
                .learnAndInvokeProcedure(events, "testMissingCallbacks");
    }

    // Make sure that default values are learned properly and
    // preserved by LAPDOG/lumen. Default values should be learned
    // based on the original input parameters. We test that those
    // values are preserved by invoking the learned procedure with no
    // input values -- lumen should fill in the defaults. Then we can
    // check what those values are by looking at the callbacks.
    @Test
    public void testDefaultValues()
            throws PALException, IOException, ClassNotFoundException {
        List<ActionInvocation> demoEvents = new Vector<ActionInvocation>();

        ActionInvocation event1 = ((ActionDef) actionModel
                .getType(TypeNameFactory.makeName("action144", "1.0", NAMESPACE))).bindAll(
                null, "item4ID1", "entity0001");
        demoEvents.add(event1);

        List<String> list1 = new Vector<String>();
        list1.add("member1");
        list1.add("42");
        list1.add("28.0");
        list1.add("1, 2, 3");
        log.info("List is " + list1.toString());
        ActionInvocation event2 = ((ActionDef) actionModel
                .getType(TypeNameFactory.makeName("action170", "1.0", NAMESPACE))).bindAll(
                null, "testEntityID1", "item44", list1);
        demoEvents.add(event2);

        ProcedureDef task1 = ProcedureLearner.learnProcedure(demoEvents,
                "testDefaultValues");

        // Now that we've learned the procedure, we serialize it and
        // de-serialize it again.

        String taskStr = task1.getSource();
        log.info("Reconstituting procedure from string: ");
        log.info(taskStr);

        ProcedureExecutor sparkExec = palBridge.getPALExecutor();
        ProcedureDef task2 = sparkExec.load(taskStr);

        // First compare default values for the whole procedure.
        for(int i = 0; i < task1.numInputParams(); i++) {
            Object default1 = task1.getDefaultValue(i);
            Object default2 = task2.getDefaultValue(i);
            assertEquals("Default values for " + i + " don't match",
                    default1.toString(), default2.toString());
        }

        // Run the procedure with no params, so it will use the default values.
        ProcedureInvocation proc = task2.invoke(null);
        proc.start();
        proc.waitUntilFinished();

        // Now we check the observed events' input parameters.
        VerifiableCallbackHandler vch = ProcedureLearner.getCallbackHandler();
        List<ActionStreamEvent> callbackEvents = vch.getSeenActions();
        assertEquals("Number of callbacks doesn't match number of events",
                demoEvents.size(), callbackEvents.size());
        for (int i = 0; i < demoEvents.size(); i++) {
            // Each observed callback event needs to match its
            // corresponding demo'd event in terms of its input
            // parameters.
            ActionInvocation demoEvent = demoEvents.get(i);
            ActionStreamEvent callbackEvent = callbackEvents.get(i);
            assertEquals("Callback event #" + i + " is the wrong type",
                    demoEvent.getDefinition().getName(), callbackEvent
                            .getDefinition().getName());
            for(int j = 0; j < demoEvent.getDefinition().numInputParams(); j++) {
                assertEquals("Callback event #" + i + " ("
                        + callbackEvent.getDefinition().getName() + ") param "
                        + j + " has unexpected value", demoEvent.getValue(j),
                        callbackEvent.getValue(j));
            }
        }
    }

    //@Test
    public void portTest() throws Exception {

        // Set the port to the usual default value then run a client
        // connection startup/shutdown.
        Properties p = new Properties(System.getProperties());
        p.setProperty("RMI_MX_PORT", "1099");
        System.setProperties(p);
        ProcedureLearner.buildBridge();

        ServerSocket socket;
        try {
            socket = ServerSocketFactory.getDefault().createServerSocket(1099);
            fail("Port "+1099+"not bound to sever.");
            socket.close();
        } catch (Exception e) {
            //do nothing
        }

        // Run it again with different ports to ensure it cleanly
        // starts up, connects, and shuts down
        p = new Properties(System.getProperties());
        p.setProperty("RMI_MX_PORT", "1999");
        System.setProperties(p);
        ProcedureLearner.buildBridge();

        try {
            socket = ServerSocketFactory.getDefault().createServerSocket(1999);
            fail("Port "+1999+"not bound to sever.");
            socket.close();
        } catch (Exception e) {
            //do nothing
        }

        p = new Properties(System.getProperties());
        p.setProperty("RMI_MX_PORT", "1899");
        System.setProperties(p);
        ProcedureLearner.buildBridge();

        try {
            socket = ServerSocketFactory.getDefault().createServerSocket(1899);
            fail("Port "+1899+"not bound to sever.");
            socket.close();
        } catch (Exception e) {
            //do nothing
        }

        // Restoring default port setting.
        p = new Properties(System.getProperties());
        p.setProperty("RMI_MX_PORT", "1099");
        System.setProperties(p);
    }
}
