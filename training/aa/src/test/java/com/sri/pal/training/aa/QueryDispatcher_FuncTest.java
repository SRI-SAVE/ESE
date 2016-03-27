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

// $Id: QueryDispatcher_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.training.aa;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sri.pal.ActionModel;
import com.sri.pal.Bridge;
import com.sri.pal.Struct;
import com.sri.pal.StructDef;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.training.mock.MockApplication;
import com.sri.pal.upgrader.MemoryTypeStorage;
import com.sri.pal.util.PALTestCase;

/**
 * Checking if the Predicate Evaluator works.
 */
public class QueryDispatcher_FuncTest
        extends PALTestCase {
    private static QueryDispatcher eval;
    private static Bridge bridge;
    private static MockApplication mockApp;
    private static ActionModel am;
    private static StructDef latlonDef;

    @BeforeClass
    public static void setup()
            throws Exception {
        Bridge.startPAL();
        mockApp = new MockApplication();
        bridge = Bridge.newInstance(QueryDispatcher_FuncTest.class.getSimpleName());
        bridge.setTypeStorage(new MemoryTypeStorage());
        am = bridge.getActionModel();
        eval = new QueryDispatcher(bridge);

        latlonDef = (StructDef) am.getType(MockApplication.LATLON);
    }

    @AfterClass
    public static void shutdown()
            throws Exception {
        mockApp.shutdown();
        bridge.shutdown();
    }

    @Test(timeOut = 10000)
    public void simpleEval()
            throws Exception {
        SimpleTypeName geoBoxPred = MockApplication.GEO_BOX_PRED;
        Struct latlon = latlonDef.newInstance();
        latlon.setValue(0, 46.594);
        latlon.setValue(1, -112.0381);
        String boxName = "Western Hemisphere";
        Object result = eval.evaluate(geoBoxPred, latlon, boxName);
        Assert.assertEquals(Boolean.TRUE, result);

        latlon.setValue(0, -37.8163);
        latlon.setValue(1, 144.9641);
        result = eval.evaluate(geoBoxPred, latlon, boxName);
        Assert.assertEquals(Boolean.FALSE, result);
    }
}
