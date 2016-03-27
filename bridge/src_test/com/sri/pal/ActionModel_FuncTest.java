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

// $Id: ActionModel_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.upgrader.MemoryTypeStorage;
import com.sri.pal.util.PALTestCase;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ActionModel_FuncTest
        extends PALTestCase {
    private static final String NAMESPACE = "ns";
    private static Bridge palBridge;
    private static ActionModel actionModel;
    private static MemoryTypeStorage ttc;

    @BeforeClass
    public static void setup()
            throws RemoteException,
            PALException {
        Bridge.startPAL();
        palBridge = Bridge.newInstance("amft");
        actionModel = palBridge.getActionModel();

        ttc = new MemoryTypeStorage();
        Assert.assertTrue(palBridge.setTypeStorage(ttc));
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * Creates a TypeCatalog, registers it, and then tries to query it using
     * ActionModel.listTypes().
     */
    @Test
    public void listTypes()
            throws Exception {
        VerifiableCallbackHandler callbackHandler = new VerifiableCallbackHandler();
        palBridge.addActionListener(callbackHandler);
        URL url = ActionModels.class.getResource(ActionModels.TUPLES);
        actionModel.load(url, NAMESPACE);

        Set<SimpleTypeName> expectedSet = new HashSet<SimpleTypeName>();
        SimpleTypeName tuple1 = (SimpleTypeName) TypeNameFactory.makeName(
                "tuple1", "1.0", NAMESPACE);
        expectedSet.add(tuple1);
        SimpleTypeName tuple2 = (SimpleTypeName) TypeNameFactory.makeName(
                "tuple2", "1.0", NAMESPACE);
        expectedSet.add(tuple2);
        SimpleTypeName tuple3 = (SimpleTypeName) TypeNameFactory.makeName(
                "tuple3", "1.0", NAMESPACE);
        expectedSet.add(tuple3);
        SimpleTypeName tuple4 = (SimpleTypeName) TypeNameFactory.makeName(
                "tuple4", "1.0", NAMESPACE);
        expectedSet.add(tuple4);
        SimpleTypeName tuple5 = (SimpleTypeName) TypeNameFactory.makeName(
                "tuple5", "1.0", NAMESPACE);
        expectedSet.add(tuple5);
        SimpleTypeName tuple6 = (SimpleTypeName) TypeNameFactory.makeName(
                "tuple6", "1.0", NAMESPACE);
        expectedSet.add(tuple6);
        SimpleTypeName tuple7 = (SimpleTypeName) TypeNameFactory.makeName(
                "tuple7", "1.0", NAMESPACE);
        expectedSet.add(tuple7);
        SimpleTypeName enum1 = (SimpleTypeName) TypeNameFactory.makeName(
                "enum1", "1.0", NAMESPACE);
        expectedSet.add(enum1);
        SimpleTypeName string = (SimpleTypeName) TypeNameFactory.makeName(
                "String", "1.0", NAMESPACE);
        expectedSet.add(string);
        SimpleTypeName action0 = (SimpleTypeName) TypeNameFactory.makeName(
                "action0", "1.0", NAMESPACE);
        expectedSet.add(action0);
        SimpleTypeName action1 = (SimpleTypeName) TypeNameFactory.makeName(
                "action1", "1.0", NAMESPACE);
        expectedSet.add(action1);
        SimpleTypeName action2 = (SimpleTypeName) TypeNameFactory.makeName(
                "action2", "1.0", NAMESPACE);
        expectedSet.add(action2);
        SimpleTypeName action3 = (SimpleTypeName) TypeNameFactory.makeName(
                "action3", "1.0", NAMESPACE);
        expectedSet.add(action3);
        SimpleTypeName action4 = (SimpleTypeName) TypeNameFactory.makeName(
                "action4", "1.0", NAMESPACE);
        expectedSet.add(action4);
        SimpleTypeName action5 = (SimpleTypeName) TypeNameFactory.makeName(
                "action5", "1.0", NAMESPACE);
        expectedSet.add(action5);
        SimpleTypeName action6 = (SimpleTypeName) TypeNameFactory.makeName(
                "action6", "1.0", NAMESPACE);
        expectedSet.add(action6);
        SimpleTypeName action7 = (SimpleTypeName) TypeNameFactory.makeName(
                "action7", "1.0", NAMESPACE);
        expectedSet.add(action7);
        SimpleTypeName action8 = (SimpleTypeName) TypeNameFactory.makeName(
                "action8", "1.0", NAMESPACE);
        expectedSet.add(action8);

        Assert.assertEquals(expectedSet, ttc.listTypes());
        Assert.assertEquals(expectedSet, actionModel.listTypes());

        /* Now remove a type from the action model and make sure it took. */
        actionModel.storeType(action4, null);
        expectedSet.remove(action4);

        Assert.assertNull(actionModel.getType(action4));
        Assert.assertEquals(expectedSet, ttc.listTypes());
        Assert.assertEquals(expectedSet, actionModel.listTypes());
    }
}
