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

package com.sri.pal.actionmodels;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionModel;
import com.sri.pal.ActionModelDef;
import com.sri.pal.Bridge;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class RelativePath_FuncTest
        extends PALBridgeTestCase {
        private static final Logger log = LoggerFactory.getLogger("TestSourceLogger");

    public static final String ACTIONS_SUBDIR_FILE = "subdir/actions_test.xml";
    public static final String NAMESPACE = "testApp";

    @AfterMethod
    public void shutdown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void loadActionsSubdir()
            throws Exception {
        Bridge.startPAL();
        palBridge = Bridge.newInstance("rpft");
        ProcedureLearner.setStorage(getClass(), palBridge);
        ActionModel am = palBridge.getActionModel();
        URL url = this.getClass().getResource(ACTIONS_SUBDIR_FILE);
        assertNotNull("Actions file not found", url);

        am.load(url, NAMESPACE);
        Set<ActionModelDef> allTypes = am.getTypes();
        Set<ActionModelDef> types = new HashSet<ActionModelDef>();
        Set<ActionDef> actions = new HashSet<ActionDef>();
        for(ActionModelDef type : allTypes) {
            TypeName name = type.getName();
            if (name instanceof SimpleTypeName
                    && !((SimpleTypeName) name).getNamespace().equals(NAMESPACE)) {
                continue;
            }
            if(type instanceof ActionDef) {
                ActionDef action = (ActionDef) type;
                actions.add(action);
            } else {
                types.add(type);
            }
        }
        log.debug("Found " + actions.size() + " actions.");
        assertEquals(1, actions.size());
        assertEquals(11, types.size());

        for(ActionModelDef paramType : types) {
            String name = paramType.toString();
            assertNotNull(name, paramType);
            assertNotNull(name, paramType.getName());
            assertNotNull(name, paramType.getName().getFullName());
        }
    }
}
