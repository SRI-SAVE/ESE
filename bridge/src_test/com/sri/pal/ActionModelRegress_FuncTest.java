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

// $Id: ActionModelRegress_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.actionmodels.bad.BadModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.util.PALTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ActionModelRegress_FuncTest
        extends PALTestCase {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    private static ActionModel actionModel;
    private static Bridge bridge;

    @BeforeClass
    public static void setup()
            throws Exception {
        Bridge.startPAL();
        bridge = Bridge.newInstance("amrft");
        actionModel = bridge.getActionModel();
        ProcedureLearner.setStorage(ActionModelRegress_FuncTest.class, bridge);
    }

    @AfterClass
    public static void shutdown() throws PALRemoteException {
        bridge.shutdown();
    }

    /**
     * Builds the test cases to be sent to {@link #checkUrl}.
     *
     * @return an array of URLs and namespaces of action models to load
     */
    @DataProvider(name = "action models")
    public Object[][] loadActionModels() {
        List<Object[]> result = new ArrayList<Object[]>();
        for (String fileName : ActionModels.getAll()) {
            URL url = ActionModels.class.getResource(fileName);
            String namespace = fileName.replaceAll("\\..*", "") + "_"
                    + result.size();
            result.add(new Object[] { url, namespace });
        }
        return result.toArray(new Object[0][]);
    }

    @Test
    public void loadAgaveActionModel()
            throws Exception {
        Assert.assertTrue(actionModel.registerAgave());
    }

    @Test(dataProvider = "action models")
    public void checkUrl(URL url,
                         String namespace)
            throws Exception {
        log.info("Checking {}", url);
        Set<ActionModelDef> actions = actionModel.load(url, namespace);
        Assert.assertTrue(actions.size() > 0);
        for (ActionModelDef typeDef : actions) {
            SimpleTypeName name = (SimpleTypeName) typeDef.getName();
            ActionModelDef td2 = actionModel.getType(name);
            String str = typeDef.getAtrStr() + "\n" + td2.getAtrStr();
            Assert.assertEquals(actionModel.getType(name), typeDef, str);
        }
    }

    @DataProvider(name = "bad action models")
    public Object[][] loadBadActionModels() {
        List<Object[]> result = new ArrayList<Object[]>();
        for (BadModels badModel : BadModels.values()) {
            String fileName = badModel.name().toLowerCase() + ".xml";
            URL url = BadModels.class.getResource(fileName);
            String namespace = fileName.replaceAll("\\..*", "") + "_"
                    + result.size();
            result.add(new Object[] { url, namespace });
        }
        return result.toArray(new Object[0][]);
    }

    @Test(dataProvider = "bad action models", expectedExceptions = PALException.class)
    public void badActionModels(URL url,
                                String namespace)
            throws Exception {
        actionModel.load(url, namespace);
    }
}
