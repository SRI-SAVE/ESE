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

// $Id: Event3_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.events;

import static com.sri.pal.ProcedureLearner.NAMESPACE;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.Learner;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureExecutor;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.Struct;
import com.sri.pal.StructDef;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Event3_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");
    static final String ACTION_MODEL = "xmltypes_action_model.xml";

    @BeforeClass
    public static void setup()
            throws RemoteException,
            PALException {
        setup(Event3_FuncTest.class.getResource(ACTION_MODEL), NAMESPACE);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void testXmlTuple()
            throws Exception {
        Vector<ActionInvocation> events = new Vector<ActionInvocation>();

        List<String> list1 = new Vector<String>();
        list1.add("0x080e0000011afe20b2350af1045880e5");
        list1.add("1248");

        StructDef structDef = (StructDef) ((ActionDef) actionModel
                .getType(TypeNameFactory
                        .makeName("action163", "1.0", NAMESPACE)))
                .getParamType(2);
        Struct tuple = new Struct(structDef);
        tuple.setValue(0, "50");
        tuple.setValue(1, "234.768");

        ActionInvocation ev1 = ((ActionDef) actionModel.getType(TypeNameFactory.makeName(
                "action163", "1.0", NAMESPACE))).invoke(null,
                "0x080e0000011ae96662630af104588011", list1, tuple);
        events.add(ev1);

        ProcedureDef task = ProcedureLearner
                .learnProcedure(events, "action163");
        assertNotNull("Unable to create initial task", task);

        ProcedureExecutor sparkExec = palBridge.getPALExecutor();
        task = sparkExec.load(task.getSource());
        ProcedureDef task_action145 = sparkExec.load(task.getSource());
        assertEquals("Copy generated from task not equal to original ", task,
                task_action145);
        assertEquals("XML generated from copied task not equal to original ",
                task.getSource().trim(), task_action145.getSource().trim());
    }

    @Test
    public void testFirstLastList()
            throws Exception {
        List<String> layers = new ArrayList<String>();
        layers.add("0x080e00000119875b2e340af1046181d2");
        ActionInvocation action159Event = ((ActionDef) actionModel
                .getType(TypeNameFactory.makeName("action159", "1.0", NAMESPACE))).invoke(null,
                "0x080e0000011995cc2e410af1045389d2", layers);
        ActionInvocation action209Event = ((ActionDef) actionModel
                .getType(TypeNameFactory.makeName("action209", "1.0", NAMESPACE))).invoke(null,
                "0x080e00000119347fd4290af10461c892",
                "0x080e00000119875b2e340af1046181d2");

        Vector<ActionInvocation> events = new Vector<ActionInvocation>();
        events.add(action159Event);
        events.add(action209Event);

        Learner learningBridge = palBridge.getLearner();

        ProcedureDef task = learningBridge.learn(
                "0x080e00000110c18e73f70af53b048888", null,
                events.toArray(new ActionInvocation[0]));

        assertNotNull("Procedure not learned", task);

        Set<String> action159Params = new HashSet<String>();
        Set<String> action209Params = new HashSet<String>();
        for (int i = 0; i < action159Event.getDefinition().size(); i++) {
            action159Params.add(action159Event.getDefinition().getParamName(i));
        }
        for (int i = 0; i < action209Event.getDefinition().size(); i++) {
            action209Params.add(action209Event.getDefinition().getParamName(i));
        }
        for (int i = 0; i < task.numInputParams(); i++) {
            assertNotNull("Display name is null for " + i, task
                    .getParamDescription(i));
            assertNotNull("Default value is null for " + i, task
                    .getDefaultValue(i));

            String actionParamName = task.getParamName(i).replaceAll("_[0-9]$",
                    "");
            if (action159Params.contains(actionParamName)) {
                assertEquals("Default value incorrect: " + actionParamName,
                        task.getDefaultValue(i), action159Event
                                .getValue(actionParamName));
            } else if (action209Params.contains(actionParamName)) {
                assertEquals("Default value incorrect: " + actionParamName,
                        task.getDefaultValue(i), action209Event
                                .getValue(actionParamName));
            } else {
                fail("Parameter name " + actionParamName
                        + " does not have a value");
            }
        }
    }

    @Test
    public void testUngeneralizableTuple()
            throws Exception {
        int paramCount = 0;

        StructDef structDef = (StructDef) ((ActionDef) actionModel
                .getType(TypeNameFactory.makeName("ungeneralizableTuple",
                        "1.0", NAMESPACE))).getParamType(0);
        Struct tuple = new Struct(structDef);
        tuple.setValue(0, 5);
        tuple.setValue(1, 0x0046181d2);

        ActionInvocation ungeneralizableTupleEvent = ((ActionDef) actionModel
                .getType(TypeNameFactory.makeName("ungeneralizableTuple", "1.0", NAMESPACE)))
                .invoke(null, tuple);

        Vector<ActionInvocation> events = new Vector<ActionInvocation>();
        events.add(ungeneralizableTupleEvent);

        Learner learningBridge = palBridge.getLearner();

        ProcedureDef task = learningBridge.learn("0x080", null,
                events.toArray(new ActionInvocation[0]));

        assertNotNull("Procedure not learned", task);
        assertNotNull("ActionDefinition null for task");

        assertEquals("Number of parameters incorrect", paramCount, task.size());

        for (int i = 0; i < task.numInputParams(); i++) {
            String paramName = task.getParamName(i);
            log.info("paramName: " + paramName);
            assertNotNull("Display name is null for " + paramName, task
                    .getParamDescription(i));
            assertNotNull("Default value is null for " + paramName, task
                    .getDefaultValue(paramName));

            String actionParamName = paramName.replaceAll("[0-9]$", "");
            if (ungeneralizableTupleEvent.getValue(actionParamName) != null) {
                assertEquals("Default value incorrect: " + actionParamName,
                        task.getDefaultValue(paramName),
                        ungeneralizableTupleEvent.getValue(actionParamName));
            } else {
                fail("Parameter name " + actionParamName
                        + " does not have a value");
            }
        }
    }
}
