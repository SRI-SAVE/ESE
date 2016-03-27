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

// $Id: Whitespace_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Whitespace_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = "foo";

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.WHITESPACE);
        setup(url, NAMESPACE);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void descriptions()
            throws Exception {
        SimpleTypeName firstName = (SimpleTypeName) TypeNameFactory.makeName(
                "string1", "1.0", NAMESPACE);
        TypeDef firstType = (TypeDef) actionModel.getType(firstName);
        assertNotNull(firstType);
        String firstDesc = firstType.getMetadata(TypeDef.DESCRIPTION);
        Set<ActionModelDef> types = actionModel.getTypes();

        // Some types get automatically added for Agave. Remove those.
        Set<ActionModelDef> typesClone = new HashSet<ActionModelDef>(types);
        for(ActionModelDef typeDef : typesClone) {
            TypeName name = typeDef.getName();
            if (name instanceof SimpleTypeName
                    && !((SimpleTypeName) name).getNamespace().equals(NAMESPACE)) {
                types.remove(typeDef);
            }
        }

        assertEquals(9, types.size());
        for (ActionModelDef type : types) {
            String desc = type.getMetadata(TypeDef.DESCRIPTION);
            assertEquals(firstDesc, desc);
        }
    }
}
