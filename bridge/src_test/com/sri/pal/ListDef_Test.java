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

// $Id: ListDef_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;
import java.util.Vector;

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

public class ListDef_Test
        extends PALTestCase {
    private IMocksControl mockCtrl;
    private Bridge bridge;
    private TypeDef td1;
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
    public void stringList()
            throws Exception {
        ListDef ld = new ListDef(td1, bridge);
        assertEquals(td1, ld.getElementType());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullElementType()
            throws Exception {
        new ListDef((TypeDef) null, bridge);
    }

    @Test
    public void stringify()
            throws Exception {
        ListDef ld = new ListDef(td2, bridge);
        List<IntType> palList = new Vector<IntType>();
        palList.add(new IntType(1));
        palList.add(new IntType(2));
        palList.add(new IntType(3));
        palList.add(new IntType(5));
        palList.add(new IntType(7));
        List<?> strList = ld.stringify(palList);
        assertEquals(String.class, strList.get(0).getClass());
        assertEquals(String.class, strList.get(1).getClass());
        assertEquals("1", strList.get(0));
        assertEquals("2", strList.get(1));
        assertEquals("3", strList.get(2));
        assertEquals("5", strList.get(3));
        assertEquals("7", strList.get(4));
    }

    @Test
    public void unstringify()
            throws Exception {
        ListDef ld = new ListDef(td2, bridge);
        List<String> strList = new Vector<String>();
        strList.add(new IntType(11).toString());
        strList.add(new IntType(13).toString());
        strList.add(new IntType(17).toString());
        Object palObj = ld.unstringify(strList);
        assertTrue(palObj instanceof List<?>);
        List<?> palList = (List<?>) palObj;
        assertEquals(3, palList.size());
        assertEquals(IntType.class, palList.get(0).getClass());
        assertEquals("11", palList.get(0).toString());
        assertEquals("13", palList.get(1).toString());
        assertEquals("17", palList.get(2).toString());
    }
}
