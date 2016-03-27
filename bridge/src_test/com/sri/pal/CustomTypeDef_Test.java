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

// $Id: CustomTypeDef_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.ApplicationSubType;
import com.sri.ai.lumen.atr.decl.impl.CTRTypeDeclaration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.types.BadType1;
import com.sri.pal.types.BadType2;
import com.sri.pal.types.BadType3;
import com.sri.pal.types.BadType4;
import com.sri.pal.types.BadType5;
import com.sri.pal.types.IntType;
import com.sri.pal.types.Type1;
import com.sri.pal.types.Type2;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.util.ATRTestUtil;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link TypeDef}.
 *
 * @author chris
 */
public class CustomTypeDef_Test
        extends PALTestCase {
    private IMocksControl mockCtrl;
    private Bridge bridge;
    private SimpleTypeName stringName;
    private SimpleTypeName intName;
    private SimpleTypeName integerName;
    private SimpleTypeName type1Name;
    private SimpleTypeName type2Name;
    private ActionModel actionModel;

    @BeforeMethod
    public void setup()
            throws Exception {
        mockCtrl = EasyMock.createNiceControl();
        bridge = mockCtrl.createMock(Bridge.class);
        actionModel = new MockActionModel(bridge);
        EasyMock.expect(bridge.getActionModel()).andReturn(actionModel)
                .anyTimes();
        mockCtrl.replay();

        stringName = (SimpleTypeName) TypeNameFactory.makeName("String");
        actionModel.registerCustomTypeFactory(stringName, new ToStringFactory(
                String.class.getName()));
        intName = (SimpleTypeName) TypeNameFactory.makeName("Int");
        actionModel.registerCustomTypeFactory(intName, new ToStringFactory(
                IntType.class.getName()));
        integerName = (SimpleTypeName) TypeNameFactory.makeName("Integer");
        actionModel.registerCustomTypeFactory(integerName, new ToStringFactory(
                Integer.class.getName()));
        type1Name = (SimpleTypeName) TypeNameFactory.makeName("type1");
        actionModel.registerCustomTypeFactory(type1Name, new ToStringFactory(
                Type1.class.getName()));
        type2Name = (SimpleTypeName) TypeNameFactory.makeName("type2");
        actionModel.registerCustomTypeFactory(type2Name, new ToStringFactory(
                Type2.class.getName()));
    }

    @AfterMethod
    public void verify() {
        mockCtrl.verify();
    }

    @Test
    public void stringType()
            throws Exception {
        TypeDef td = new CustomTypeDef(ATRTestUtil.makeCustomType(stringName,
                String.class), bridge);
        assertNotNull(td);
        assertEquals(stringName, td.getName());
    }

    @Test
    public void integerType()
            throws Exception {
        TypeDef td = new CustomTypeDef(ATRTestUtil.makeCustomType(integerName,
                Integer.class), bridge);
        assertNotNull(td);
        assertEquals(integerName, td.getName());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullClass()
            throws Exception {
        ATRTypeDeclaration type = CTRTypeDeclaration.createApplicationType(
                "String", null, null, null);
        new CustomTypeDef(type, bridge);
    }

    @Test
    public void stringConstructor()
            throws Exception {
        new CustomTypeDef(ATRTestUtil.makeCustomType(type1Name, Type1.class),
                bridge);
    }

    @Test
    public void staticValueOf()
            throws Exception {
        new CustomTypeDef(ATRTestUtil.makeCustomType(type2Name, Type2.class),
                bridge);
    }

    /**
     * The parent type's class is a parent of this type's class. In other words:
     *
     * <code>parent.getJavaClass().isAssignableFrom(this.getJavaClass()) == true</code>
     */
    @Test
    public void parentClassParent()
            throws Exception {
        CustomTypeDef td1 = new CustomTypeDef(ATRTestUtil.makeCustomType(
                type1Name, Type1.class), bridge);
        actionModel.storeType(type1Name, td1);
        ApplicationSubType atr2 = CTRTypeDeclaration.createApplicationSubType(
                type2Name.getFullName(), null, null, td1.getName()
                        .getFullName());
        new CustomTypeDef(atr2, bridge);
    }

// @Test(expected = IllegalArgumentException.class)
// public void parentClassNotParent()
// throws Exception {
// CustomTypeDef td1 = new CustomTypeDef(ATRTestUtil.makeCustomType("td1",
// String.class), bridge);
// ApplicationSubType atr2 = CTRTypeDeclaration.createApplicationSubType(
// "td2", null, null, Integer.class, td1.getName().getFullName());
// new CustomTypeDef(atr2, bridge);
// }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void noGoodConstructor()
            throws Exception {
        SimpleTypeName bad1Name = (SimpleTypeName) TypeNameFactory
                .makeName("bad1");
        actionModel.registerCustomTypeFactory(bad1Name, new ToStringFactory(
                BadType1.class.getName()));
        new CustomTypeDef(ATRTestUtil.makeCustomType(bad1Name, BadType1.class),
                bridge);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void wrongTypeConstructor()
            throws Exception {
        SimpleTypeName bad2Name = (SimpleTypeName) TypeNameFactory
                .makeName("bad2");
        actionModel.registerCustomTypeFactory(bad2Name, new ToStringFactory(
                BadType2.class.getName()));
        new CustomTypeDef(ATRTestUtil.makeCustomType(bad2Name, BadType2.class),
                bridge);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void wrongArgTypeValueOf()
            throws Exception {
        SimpleTypeName bad3Name = (SimpleTypeName) TypeNameFactory
                .makeName("bad3");
        actionModel.registerCustomTypeFactory(bad3Name, new ToStringFactory(
                BadType3.class.getName()));
        new CustomTypeDef(ATRTestUtil.makeCustomType(bad3Name, BadType3.class),
                bridge);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void wrongReturnTypeValueOf()
            throws Exception {
        SimpleTypeName bad4Name = (SimpleTypeName) TypeNameFactory
                .makeName("bad4");
        actionModel.registerCustomTypeFactory(bad4Name, new ToStringFactory(
                BadType4.class.getName()));
        new CustomTypeDef(ATRTestUtil.makeCustomType(bad4Name, BadType4.class),
                bridge);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nonStaticValueOf()
            throws Exception {
        SimpleTypeName bad5Name = (SimpleTypeName) TypeNameFactory
                .makeName("bad5");
        actionModel.registerCustomTypeFactory(bad5Name, new ToStringFactory(
                BadType5.class.getName()));
        new CustomTypeDef(ATRTestUtil.makeCustomType(bad5Name, BadType5.class),
                bridge);
    }

    @Test
    public void overloadedConstructor()
            throws Exception {
        new CustomTypeDef(ATRTestUtil.makeCustomType(intName, IntType.class),
                bridge);
    }

    @Test
    public void stringify()
            throws Exception {
        CustomTypeDef td = new CustomTypeDef(ATRTestUtil.makeCustomType(
                type1Name, Type1.class), bridge);
        Type1 realObj1 = new Type1("foo");
        Object strObj1 = td.stringify(realObj1);
        Type1 realObj2 = (Type1) td.unstringify(strObj1);
        Object strObj2 = td.stringify(realObj2);
        assertNotNull(strObj1);
        assertNotNull(realObj2);
        assertNotNull(strObj2);
        assertEquals(strObj1, strObj2);
    }

    @Test(expectedExceptions = ClassCastException.class, enabled = false)
    public void stringifyWrongClass()
            throws Exception {
        CustomTypeDef td1 = new CustomTypeDef(ATRTestUtil.makeCustomType(
                type1Name, Type1.class), bridge);
        IntType obj = new IntType(13);
        td1.stringify(obj);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void unstringifyBadValue()
            throws Exception {
        CustomTypeDef td = new CustomTypeDef(ATRTestUtil.makeCustomType(
                intName, IntType.class), bridge);
        String value = "2.71828";
        td.unstringify(value);
    }

    @Test
    public void stringifyNull()
            throws Exception {
        CustomTypeDef td1 = new CustomTypeDef(ATRTestUtil.makeCustomType(
                type1Name, Type1.class), bridge);
        CustomTypeDef td2 = new CustomTypeDef(ATRTestUtil.makeCustomType(
                intName, IntType.class), bridge);
        Object str1 = td1.stringify(null);
        Object str2 = td2.stringify(null);
        assertNull(str1);
        assertNull(str2);
    }

    @Test
    public void unstringifyNull()
            throws Exception {
        CustomTypeDef td1 = new CustomTypeDef(ATRTestUtil.makeCustomType(
                type1Name, Type1.class), bridge);
        CustomTypeDef td2 = new CustomTypeDef(ATRTestUtil.makeCustomType(
                intName, IntType.class), bridge);
        Object str1 = td1.stringify(null);
        Object str2 = td2.stringify(null);
        assertNull(str1);
        assertNull(str2);
    }
}
