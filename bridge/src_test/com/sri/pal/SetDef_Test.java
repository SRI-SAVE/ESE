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

// $Id: SetDef_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.HashSet;
import java.util.Set;

import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.types.IntType;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.util.ATRTestUtil;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SetDef_Test
        extends PALTestCase {
    private IMocksControl mockCtrl;
    private Bridge bridge;
    private CustomTypeDef td1;
    private CustomTypeDef td2;

    @BeforeMethod
    public void setup()
            throws Exception {
        mockCtrl = EasyMock.createNiceControl();
        bridge = mockCtrl.createMock(Bridge.class);
        ActionModel actionModel = new MockActionModel(bridge);
        EasyMock.expect(bridge.getActionModel()).andReturn(actionModel)
                .anyTimes();
        mockCtrl.replay();

        SimpleTypeName td1Name = (SimpleTypeName) TypeNameFactory
                .makeName("String");
        SimpleTypeName td2Name = (SimpleTypeName) TypeNameFactory
                .makeName("Int");
        actionModel.registerCustomTypeFactory(td1Name, new ToStringFactory(
                String.class.getName()));
        actionModel.registerCustomTypeFactory(td2Name, new ToStringFactory(
                IntType.class.getName()));
        td1 = new CustomTypeDef(ATRTestUtil.makeCustomType(td1Name,
                String.class), bridge);
        td2 = new CustomTypeDef(ATRTestUtil.makeCustomType(td2Name,
                IntType.class), bridge);
    }

    @AfterMethod
    public void verify() {
        mockCtrl.verify();
    }

    @Test
    public void stringSet() {
        SetDef sd = new SetDef(td1, bridge);
        assertEquals(td1, sd.getElementType());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullElementType() {
        new SetDef((TypeDef) null, bridge);
    }

    @Test
    public void stringify() {
        SetDef sd = new SetDef(td2, bridge);
        Set<IntType> palSet = new HashSet<IntType>();
        palSet.add(new IntType(31));
        palSet.add(new IntType(37));
        palSet.add(new IntType(41));
        Set<?> strSet = sd.stringify(palSet);
        assertEquals(String.class, strSet.iterator().next().getClass());
        assertTrue(strSet.contains("31"));
        assertTrue(strSet.contains("37"));
        assertTrue(strSet.contains("41"));
    }

    @Test
    public void unstringify()
            throws Exception {
        SetDef sd = new SetDef(td2, bridge);
        Set<String> strSet = new HashSet<String>();
        strSet.add(new IntType(43).toString());
        strSet.add(new IntType(47).toString());
        strSet.add(new IntType(53).toString());
        Object palObj = sd.unstringify(strSet);
        assertTrue(palObj instanceof Set<?>);
        Set<?> palSet = (Set<?>) palObj;
        assertEquals(3, palSet.size());
        assertEquals(IntType.class, palSet.iterator().next().getClass());
        assertTrue(palSet.contains(new IntType(43)));
        assertTrue(palSet.contains(new IntType(47)));
        assertTrue(palSet.contains(new IntType(53)));
    }
}
