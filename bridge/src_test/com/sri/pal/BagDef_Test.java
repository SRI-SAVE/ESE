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

// $Id: BagDef_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
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

public class BagDef_Test
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
    public void stringBag()
            throws Exception {
        BagDef bd = new BagDef(td1, bridge);
        assertEquals(td1, bd.getElementType());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullElementType() {
        new BagDef((TypeDef) null, bridge);
    }

    @Test
    public void sameBagParent()
            throws Exception {
        BagDef bd1 = new BagDef(td1, bridge);
        ATRTypeDeclaration atr = ATRTestUtil.makeAliasType("bag2", "bag<"
                + td1.getName().getFullName() + ">", null);
        BagDef bd2 = new BagDef(atr, bd1, bridge);
        assertFalse(bd1.equals(bd2));
    }

    @Test
    public void stringify()
            throws Exception {
        BagDef bd = new BagDef(td2, bridge);
        Collection<IntType> palBag = new ArrayList<IntType>();
        palBag.add(new IntType(31));
        palBag.add(new IntType(37));
        palBag.add(new IntType(41));
        Collection<?> strBag = (Collection<?>) bd.stringify(palBag);
        assertEquals(String.class, strBag.iterator().next().getClass());
        assertTrue(strBag.contains("31"));
        assertTrue(strBag.contains("37"));
        assertTrue(strBag.contains("41"));
    }

    @Test
    public void unstringify()
            throws Exception {
        BagDef bd = new BagDef(td2, bridge);
        Collection<String> strBag = new ArrayList<String>();
        strBag.add(new IntType(43).toString());
        strBag.add(new IntType(47).toString());
        strBag.add(new IntType(53).toString());
        Object palObj = bd.unstringify(strBag);
        assertTrue(palObj instanceof Collection<?>);
        Collection<?> palBag = (Collection<?>) palObj;
        assertEquals(3, palBag.size());
        assertEquals(IntType.class, palBag.iterator().next().getClass());
        assertTrue(palBag.contains(new IntType(43)));
        assertTrue(palBag.contains(new IntType(47)));
        assertTrue(palBag.contains(new IntType(53)));
    }
}
