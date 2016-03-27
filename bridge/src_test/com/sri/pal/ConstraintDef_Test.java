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

// $Id: ConstraintDef_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import com.sri.ai.lumen.atr.decl.ATRFunctionDeclaration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.util.ATRTestUtil;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ConstraintDef_Test
        extends PALTestCase {
    private static final String VERSION = "1.0";
    private static final String NAMESPACE = "cdt";

    private Bridge bridge;
    private IMocksControl mockCtrl;

    @BeforeMethod
    public void setup() {
        mockCtrl = EasyMock.createNiceControl();
        bridge = mockCtrl.createMock(Bridge.class);
        mockCtrl.replay();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullFieldNames()
            throws Exception {
        String[] fieldDescs = new String[] { "foo", "bar" };
        ATRFunctionDeclaration atr = ATRTestUtil.makeConstraintDef("con", null,
                fieldDescs);
        new ConstraintDef(atr, bridge);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullFieldDescs()
            throws Exception {
        String[] fieldNames = new String[] { "foo", "bar" };
        ATRFunctionDeclaration atr = ATRTestUtil.makeConstraintDef("con",
                fieldNames, null);
        new ConstraintDef(atr, bridge);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void mismatchedArrays()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2", "field3" };
        String[] fieldDescs = new String[] { "desc1", "desc2" };
        ATRFunctionDeclaration atr = ATRTestUtil.makeConstraintDef("con",
                fieldNames, fieldDescs);
        new ConstraintDef(atr, bridge);
    }

    @Test
    public void noArgs()
            throws Exception {
        ATRFunctionDeclaration atr = ATRTestUtil.makeConstraintDef("con",
                new String[0], new String[0]);
        new ConstraintDef(atr, bridge);
    }

    /**
     * Take a ConstraintDef, convert it to ATR, convert that back to a
     * ConstraintDef, and ensure they're equal.
     */
    @Test
    public void roundTripAtr()
            throws Exception {
        SimpleTypeName name1 = (SimpleTypeName) TypeNameFactory.makeName(
                "constraint1", VERSION, NAMESPACE);
        String[] fieldNames1 = new String[] { "arg1", "arg2" };
        String[] fieldDescs1 = new String[] { "descr1", "descr2" };
        ATRFunctionDeclaration atr1 = ATRTestUtil.makeConstraintDef(
                name1.getFullName(), fieldNames1, fieldDescs1);
        ConstraintDef cons1 = new ConstraintDef(atr1, bridge);

        ATRFunctionDeclaration atr2 = cons1.getAtr();
        ConstraintDef constraint = new ConstraintDef(atr2, bridge);
        Assert.assertEquals(cons1, constraint);
    }
}
