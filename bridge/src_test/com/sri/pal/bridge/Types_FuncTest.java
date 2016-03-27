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

// $Id: Types_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.bridge;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.Bridge_FuncTest;
import com.sri.pal.CustomTypeDef;
import com.sri.pal.ListDef;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureExecutor;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.Struct;
import com.sri.pal.StructDef;
import com.sri.pal.TypeDef;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Types_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory.getLogger("TestSourceLogger");
    private static final String ACTION_MODEL = ActionModels.ACTIONS;
    private static final String TEST_NAMESPACE = Bridge_FuncTest.NAMESPACE;

    private static SimpleTypeName createPathName;
    private static SimpleTypeName getPathName;
    private static SimpleTypeName drawPointName;

    @BeforeClass
    public static void setup() throws RemoteException, PALException {
        setup(ActionModels.class.getResource(ACTION_MODEL), TEST_NAMESPACE);

        createPathName = (SimpleTypeName) TypeNameFactory.makeName(
                "createPath", "1.0", TEST_NAMESPACE);
        getPathName = (SimpleTypeName) TypeNameFactory.makeName("getPath",
                "1.0", TEST_NAMESPACE);
        drawPointName = (SimpleTypeName) TypeNameFactory.makeName("drawPoint",
                "1.0", TEST_NAMESPACE);
        actionModel.registerExecutor(createPathName, callbackHandler);
        actionModel.registerExecutor(getPathName, callbackHandler);
        actionModel.registerExecutor(drawPointName, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void nestedTypes()
            throws PALException, IOException, ClassNotFoundException {
        List<ActionInvocation> demoEvents = new Vector<ActionInvocation>();

        // Build the data structure of a list of tuples of floats.
        StructDef structDef = (StructDef) actionModel.getType(TypeNameFactory.makeName(
                "point", "1.0", TEST_NAMESPACE));
        List<Struct> points = new ArrayList<Struct>();
        Struct struct = new Struct(structDef);
        struct.setValue(0, 5.5f);
        struct.setValue(1, 2f);
        struct.setValue(2, 37.73f);
        points.add(struct);
        struct = new Struct(structDef);
        struct.setValue(0, 3.141527f);
        struct.setValue(1, 2.71828f);
        struct.setValue(2, -1f);
        points.add(struct);

        ActionInvocation event1 = ((ActionDef) actionModel.getType(createPathName))
                .invoke(null, "mypath", points);
        demoEvents.add(event1);

        ActionInvocation event2 = ((ActionDef) actionModel.getType(getPathName))
                .bindAll(null, "mypath", points);
        demoEvents.add(event2);

        ProcedureDef task1 = learningBridge.learn("nestedTypes", null,
                demoEvents.toArray(new ActionInvocation[0]));

        // Now that we've learned the procedure, we serialize it and
        // de-serialize it again.
        String taskStr = task1.getSource();
        log.info("Reconstituting procedure from string: " + taskStr);
        ProcedureExecutor sparkExec = palBridge.getPALExecutor();
        ProcedureDef task2 = sparkExec.load(taskStr);

        // Make sure we can read the individual data values from the default
        // values of the deserialized procedure.
        boolean atomSeen = false;
        boolean listSeen = false;
        for(int i = 0; i < task1.numInputParams(); i++) {
            String paramName = task1.getParamName(i);
            TypeDef type = task1.getParamType(i);
            Object paramValue = task2.getDefaultValue(paramName);
            log.info("Value for " + paramName + " is " + paramValue);
            if(type instanceof ListDef) {
                listSeen = true;
                assertTrue("Wrong object type", paramValue instanceof List<?>);
                List<?> list = (List<?>) paramValue;
                Object member0 = list.get(0);
                TypeDef memberType = ((ListDef) type).getElementType();
                assertEquals("Wrong member type of list", StructDef.class,
                        memberType.getClass());
                assertEquals("Wrong type ID of list members", TypeNameFactory.makeName(
                        "point", "1.0", TEST_NAMESPACE), memberType.getName());
                assertTrue("Wrong object type for list member",
                        member0 instanceof Struct);
                Struct memberStruct = (Struct) member0;
                Object tupleMember0 = memberStruct.getValue(0);
                assertEquals("Wrong member type of tuple", CustomTypeDef.class,
                        ((StructDef) memberType).getFieldType(0).getClass());
                assertTrue("Wrong member object type of tuple" + tupleMember0.getClass(),
                        tupleMember0 instanceof Float);
                Float tupleMemberFloat = (Float) tupleMember0;
                assertEquals("Wrong value for tuple member", 5.5,
                        tupleMemberFloat.floatValue(), 0.01);
            } else {
                // Assume it's an atom
                atomSeen = true;
                assertEquals("Wrong value for name", "mypath", paramValue
                        .toString());
            }
        }
        assertTrue(atomSeen);
        assertTrue(listSeen);

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
        List<ActionStreamEvent> callbackEvents = callbackHandler.getSeenActions();
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

    @Test
    public void simpleTupleParam()
            throws Exception {
        StructDef structDef = (StructDef) actionModel
                .getType(TypeNameFactory.makeName("point", "1.0",
                        TEST_NAMESPACE));
        Struct point = new Struct(structDef);
        point.setValue(0, 2f);
        point.setValue(1, 4f);
        point.setValue(2, 8f);
        ActionInvocation action = ((ActionDef) actionModel
                .getType(drawPointName)).bindAll(null, point);

        ProcedureDef task = learningBridge.learn("simpleTupleParam", null, action);

        ProcedureInvocation proc = task.invoke(null);
        proc.start();
        proc.waitUntilFinished();
    }
}
