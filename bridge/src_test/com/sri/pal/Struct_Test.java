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

// $Id: Struct_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.Structure;
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

public class Struct_Test
        extends PALTestCase {
    private TypeDef td1;
    private TypeDef td2;
    private IMocksControl mockCtrl;
    private Bridge bridge;

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
        actionModel.storeType(td1Name, td1);
        actionModel.storeType(td2Name, td2);
    }

    @AfterMethod
    public void verify() {
        mockCtrl.verify();
    }

    @Test
    public void twoFieldStruct()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);
        Struct struct = new Struct(structDef);

        assertEquals(structDef, struct.getDefinition());

        assertNull(struct.getValue(0));
        assertNull(struct.getValue("field1"));
        assertNull(struct.getValue(1));
        assertNull(struct.getValue("field2"));
        List<Object> expectedContents = new ArrayList<Object>();
        expectedContents.add(null);
        expectedContents.add(null);
        assertEquals(expectedContents, struct.contents());

        IntType intValue1 = new IntType("5");
        struct.setValue(0, "foo");
        struct.setValue(1, intValue1);
        assertEquals("foo", struct.getValue(0));
        assertEquals("foo", struct.getValue("field1"));
        assertEquals(intValue1, struct.getValue(1));
        assertEquals(intValue1, struct.getValue("field2"));
        expectedContents.clear();
        expectedContents.add("foo");
        expectedContents.add(intValue1);
        assertEquals(expectedContents, struct.contents());

        IntType intValue2 = new IntType("25");
        struct.setValue("field1", "bar");
        struct.setValue("field2", intValue2);
        assertEquals("bar", struct.getValue(0));
        assertEquals("bar", struct.getValue("field1"));
        assertEquals(intValue2, struct.getValue(1));
        assertEquals(intValue2, struct.getValue("field2"));
        expectedContents.clear();
        expectedContents.add("bar");
        expectedContents.add(intValue2);
        assertEquals(expectedContents, struct.contents());
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void getBadFieldName()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);
        Struct struct = new Struct(structDef);
        struct.getValue("foo");
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void setBadFieldName()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);
        Struct struct = new Struct(structDef);
        struct.setValue("foo", "bar");
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void getNegativeFieldNum()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);
        Struct struct = new Struct(structDef);
        struct.getValue(-1);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void getBigFieldNum()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);
        Struct struct = new Struct(structDef);
        struct.getValue(2);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void setNegativeFieldNum()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);
        Struct struct = new Struct(structDef);
        struct.setValue(-1, "foo");
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void setBigFieldNum()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);
        Struct struct = new Struct(structDef);
        struct.setValue(2, "foo");
    }
}
