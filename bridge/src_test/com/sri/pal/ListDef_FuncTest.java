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

// $Id: ListDef_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Miscellaneous functional tests of lists.
 */
public class ListDef_FuncTest
        extends PALBridgeTestCase {
    private static final String ACTION_MODEL = "lists.xml";
    private static final String VERS = "1.0";
    private static final String NS = "ldft";

    private static ListDef list1Def;
    private static ListDef list2Def;
    private static ListDef list3Def;
    private static ListDef list4Def;
    private static ListDef list5Def;
    private static ListDef list6Def;
    private static ListDef list7Def;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTION_MODEL), NS);

        SimpleTypeName list1Name = (SimpleTypeName) TypeNameFactory.makeName(
                "list1", VERS, NS);
        list1Def = (ListDef) actionModel.getType(list1Name);

        SimpleTypeName list2Name = (SimpleTypeName) TypeNameFactory.makeName(
                "list2", VERS, NS);
        list2Def = (ListDef) actionModel.getType(list2Name);

        SimpleTypeName list3Name = (SimpleTypeName) TypeNameFactory.makeName(
                "list3", VERS, NS);
        list3Def = (ListDef) actionModel.getType(list3Name);

        SimpleTypeName list4Name = (SimpleTypeName) TypeNameFactory.makeName(
                "list4", VERS, NS);
        list4Def = (ListDef) actionModel.getType(list4Name);

        SimpleTypeName list5Name = (SimpleTypeName) TypeNameFactory.makeName(
                "list5", VERS, NS);
        list5Def = (ListDef) actionModel.getType(list5Name);

        SimpleTypeName list6Name = (SimpleTypeName) TypeNameFactory.makeName(
                "list6", VERS, NS);
        list6Def = (ListDef) actionModel.getType(list6Name);

        SimpleTypeName list7Name = (SimpleTypeName) TypeNameFactory.makeName(
                "list7", VERS, NS);
        list7Def = (ListDef) actionModel.getType(list7Name);
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    /**
     * Ensure that different list types don't consider themselves equal to each
     * other.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void differentTypesNotEquals()
            throws Exception {
        TypeName implicit1Name = TypeNameFactory.makeName("list<string>");
        ListDef implicit1 = (ListDef) actionModel.getType(implicit1Name);
        TypeName implicit2Name = TypeNameFactory.makeName("list<struct1>",
                VERS, NS);
        ListDef implicit2 = (ListDef) actionModel.getType(implicit2Name);

        ListDef[] defs = { list1Def, list2Def, list3Def, list4Def, list5Def,
                list6Def, list7Def, implicit1, implicit2 };
        for (int i = 0; i < defs.length; i++) {
            for (int j = 0; j < defs.length; j++) {
                ListDef ld1 = defs[i];
                ListDef ld2 = defs[j];
                if (i == j) {
                    Assert.assertTrue(ld1.equals(ld2), ld1.getName()
                            + ".equals(" + ld2.getName() + ")");
                } else {
                    Assert.assertFalse(ld1.equals(ld2), ld1.getName()
                            + ".equals(" + ld2.getName() + ")");
                }
            }
        }
    }
}
