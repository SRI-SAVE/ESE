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

// $Id: StructDef_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration.Structure;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
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

public class StructDef_Test
        extends PALTestCase {
    private CustomTypeDef td1;
    private CustomTypeDef td2;
    private CustomTypeDef td3;
    private CustomTypeDef td4;
    private CustomTypeDef td5;
    private IMocksControl mockCtrl;
    private Bridge bridge;
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

        SimpleTypeName td1Name = (SimpleTypeName) TypeNameFactory
                .makeName("String");
        SimpleTypeName td2Name = (SimpleTypeName) TypeNameFactory
                .makeName("String2");
        SimpleTypeName td3Name = (SimpleTypeName) TypeNameFactory
                .makeName("Int");
        SimpleTypeName td4Name = (SimpleTypeName) TypeNameFactory
                .makeName("type1");
        SimpleTypeName td5Name = (SimpleTypeName) TypeNameFactory
                .makeName("type2");
        actionModel.registerCustomTypeFactory(td1Name, new ToStringFactory(
                String.class.getName()));
        actionModel.registerCustomTypeFactory(td2Name, new ToStringFactory(
                String.class.getName()));
        actionModel.registerCustomTypeFactory(td3Name, new ToStringFactory(
                IntType.class.getName()));
        actionModel.registerCustomTypeFactory(td4Name, new ToStringFactory(
                Type1.class.getName()));
        actionModel.registerCustomTypeFactory(td5Name, new ToStringFactory(
                Type2.class.getName()));
        td1 = new CustomTypeDef(ATRTestUtil.makeCustomType(td1Name,
                String.class), bridge);
        td2 = new CustomTypeDef(ATRTestUtil.makeCustomType(td2Name,
                String.class), bridge);
        td3 = new CustomTypeDef(ATRTestUtil.makeCustomType(td3Name,
                IntType.class), bridge);
        td4 = new CustomTypeDef(
                ATRTestUtil.makeCustomType(td4Name, Type1.class), bridge);
        td5 = new CustomTypeDef(
                ATRTestUtil.makeCustomType(td5Name, Type2.class), bridge);
        actionModel.storeType(td1Name, td1);
        actionModel.storeType(td2Name, td2);
        actionModel.storeType(td3Name, td3);
        actionModel.storeType(td4Name, td4);
        actionModel.storeType(td5Name, td5);
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
        assertEquals(td1, structDef.getFieldType(0));
        assertEquals(td2, structDef.getFieldType(1));
        assertEquals("field1", structDef.getFieldName(0));
        assertEquals("field2", structDef.getFieldName(1));
        assertEquals(2, structDef.size());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void mismatchedFields()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName(), td3.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        new StructDef(atr, bridge);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullFieldNames()
            throws Exception {
        String[] fieldNames = null;
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        new StructDef(atr, bridge);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullFieldTypes()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = null;
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        new StructDef(atr, bridge);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullFieldName()
            throws Exception {
        String[] fieldNames = new String[] { "field1", null };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        new StructDef(atr, bridge);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullFieldType()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(), null };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        new StructDef(atr, bridge);
    }

    @Test
    public void zeroFields()
            throws Exception {
        String[] fieldNames = new String[0];
        String[] fieldTypes = new String[0];
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);
        assertEquals(0, structDef.size());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void duplicateFieldName()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field1" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        new StructDef(atr, bridge);
    }

    @Test
    public void duplicateFieldType()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td1.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);
        assertNotNull(structDef);
    }

    @Test
    public void inheritOneField()
            throws Exception {
        String[] superFieldNames = new String[] { "field1" };
        String[] superFieldTypes = new String[] { td1.getName().getFullName() };
        Structure superAtr = ATRTestUtil.makeStruct("tuple", null,
                superFieldNames, superFieldTypes, false);
        StructDef superDef = new StructDef(superAtr, bridge);
        assertNotNull(superDef);
        actionModel.storeType(superDef.getName(), superDef);

        String[] subFieldNames = new String[0];
        String[] subFieldTypes = new String[0];
        Structure subAtr = ATRTestUtil.makeStruct("tuple", superDef.getName()
                .getFullName(), subFieldNames, subFieldTypes, false);
        StructDef subDef = new StructDef(subAtr, bridge);
        assertNotNull(subDef);

        assertEquals(1, subDef.size());
        assertEquals("field1", subDef.getFieldName(0));
        assertEquals(0, subDef.getFieldNum("field1"));
        assertEquals(td1.getName(), subDef.getFieldTypeName(0));
        assertEquals(td1, subDef.getFieldType(0));
    }

    @Test
    public void inheritOneFieldWithOne()
            throws Exception {
        String[] superFieldNames = new String[] { "field1" };
        String[] superFieldTypes = new String[] { td1.getName().getFullName() };
        Structure superAtr = ATRTestUtil.makeStruct("tuple", null,
                superFieldNames, superFieldTypes, false);
        StructDef superDef = new StructDef(superAtr, bridge);
        assertNotNull(superDef);
        actionModel.storeType(superDef.getName(), superDef);

        String[] subFieldNames = new String[] { "field2" };
        String[] subFieldTypes = new String[] { td2.getName().getFullName() };
        Structure subAtr = ATRTestUtil.makeStruct("tuple", superDef.getName()
                .getFullName(), subFieldNames, subFieldTypes, false);
        StructDef subDef = new StructDef(subAtr, bridge);
        assertNotNull(subDef);

        assertEquals(2, subDef.size());
        assertEquals("field1", subDef.getFieldName(0));
        assertEquals("field2", subDef.getFieldName(1));
        assertEquals(0, subDef.getFieldNum("field1"));
        assertEquals(1, subDef.getFieldNum("field2"));
        assertEquals(td1.getName(), subDef.getFieldTypeName(0));
        assertEquals(td2.getName(), subDef.getFieldTypeName(1));
        assertEquals(td1, subDef.getFieldType(0));
        assertEquals(td2, subDef.getFieldType(1));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void inheritDuplicateFieldName()
            throws Exception {
        String[] superFieldNames = new String[] { "field1" };
        String[] superFieldTypes = new String[] { td1.getName().getFullName() };
        Structure superAtr = ATRTestUtil.makeStruct("tuple", null,
                superFieldNames, superFieldTypes, false);
        StructDef superDef = new StructDef(superAtr, bridge);
        assertNotNull(superDef);
        actionModel.storeType(superDef.getName(), superDef);

        String[] subFieldNames = new String[] { "field1" };
        String[] subFieldTypes = new String[] { td1.getName().getFullName() };
        Structure subAtr = ATRTestUtil.makeStruct("tuple", superDef.getName()
                .getFullName(), subFieldNames, subFieldTypes, false);
        new StructDef(subAtr, bridge);
    }

    @Test
    public void equals()
            throws Exception {
        String[] fieldNames1 = new String[] { "field1", "field2" };
        String[] fieldNames2 = new String[] { "field1", "field2", "field3" };
        String[] fieldNames3 = new String[] { "field2", "field1" };
        String[] fieldTypes1 = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        String[] fieldTypes2 = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName(), td3.getName().getFullName() };
        String[] fieldTypes3 = new String[] { td2.getName().getFullName(),
                td1.getName().getFullName() };
        String[] fieldTypes4 = new String[] { td4.getName().getFullName(),
                td4.getName().getFullName() };
        String[] fieldTypes5 = new String[] { td5.getName().getFullName(),
                td5.getName().getFullName() };
        Structure atr1 = ATRTestUtil.makeStruct("tuple", null, fieldNames1,
                fieldTypes1, false);
        StructDef structDef1 = new StructDef(atr1, bridge);
        Structure atr2 = ATRTestUtil.makeStruct("tuple", null, fieldNames1,
                fieldTypes1, false);
        StructDef structDef2 = new StructDef(atr2, bridge);
        Structure atr3 = ATRTestUtil.makeStruct("foo", null, fieldNames1,
                fieldTypes1, false);
        StructDef structDef3 = new StructDef(atr3, bridge);
        Structure atr4 = ATRTestUtil.makeStruct("tuple", null, fieldNames2,
                fieldTypes2, false);
        StructDef structDef4 = new StructDef(atr4, bridge);
        Structure atr5 = ATRTestUtil.makeStruct("tuple", null, fieldNames3,
                fieldTypes1, false);
        StructDef structDef5 = new StructDef(atr5, bridge);
        Structure atr6 = ATRTestUtil.makeStruct("tuple", null, fieldNames1,
                fieldTypes3, false);
        StructDef structDef6 = new StructDef(atr6, bridge);
        Structure atr7 = ATRTestUtil.makeStruct("tuple", null, fieldNames3,
                fieldTypes3, false);
        StructDef structDef7 = new StructDef(atr7, bridge);
        Structure atr8 = ATRTestUtil.makeStruct("tuple", null, fieldNames1,
                fieldTypes4, false);
        StructDef structDef8 = new StructDef(atr8, bridge);
        Structure atr9 = ATRTestUtil.makeStruct("tuple", null, fieldNames1,
                fieldTypes5, false);
        StructDef structDef9 = new StructDef(atr9, bridge);

        assertTrue(structDef1.equals(structDef2));
        assertTrue(structDef2.equals(structDef1));
        assertFalse(structDef1.equals(structDef3));
        assertFalse(structDef3.equals(structDef1));
        assertFalse(structDef1.equals(structDef4));
        assertFalse(structDef4.equals(structDef1));
        assertFalse(structDef1.equals(structDef5));
        assertFalse(structDef5.equals(structDef1));
        assertFalse(structDef1.equals(structDef6));
        assertFalse(structDef6.equals(structDef1));
        assertFalse(structDef1.equals(structDef7));
        assertFalse(structDef7.equals(structDef1));
        assertFalse(structDef5.equals(structDef6));
        assertFalse(structDef6.equals(structDef5));
        assertFalse(structDef5.equals(structDef7));
        assertFalse(structDef7.equals(structDef5));
        assertFalse(structDef6.equals(structDef7));
        assertFalse(structDef7.equals(structDef6));
        assertFalse(structDef8.equals(structDef9));
        assertFalse(structDef9.equals(structDef8));
    }

    @Test
    public void newInstance()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td2.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);
        Struct struct1 = structDef.newInstance();
        assertNotNull(struct1);
    }

    @Test
    public void stringify()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2", "field3" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td3.getName().getFullName(), td4.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);

        String value1 = "foo";
        IntType value2 = new IntType("42");
        Type1 value3 = new Type1("1");
        Struct struct = new Struct(structDef);
        struct.setValue(0, value1);
        struct.setValue(1, value2);
        struct.setValue(2, value3);
        List<?> strTuple = structDef.stringify(struct);
        assertNotNull(strTuple);
        assertEquals("foo", strTuple.get(0));
        assertEquals("42", strTuple.get(1));
        assertEquals("1", strTuple.get(2));
    }

    @Test
    public void unstringify()
            throws Exception {
        String[] fieldNames = new String[] { "field1", "field2", "field3" };
        String[] fieldTypes = new String[] { td1.getName().getFullName(),
                td3.getName().getFullName(), td4.getName().getFullName() };
        Structure atr = ATRTestUtil.makeStruct("tuple", null, fieldNames,
                fieldTypes, false);
        StructDef structDef = new StructDef(atr, bridge);

        List<Object> strTuple = new ArrayList<Object>();
        strTuple.add("bar");
        strTuple.add("-1");
        strTuple.add("99");
        Struct tuple = (Struct) structDef.unstringify(strTuple);
        assertNotNull(tuple);
        assertEquals("bar", tuple.getValue(0));
        assertEquals(new IntType("-1"), tuple.getValue(1));
        assertEquals(new Type1("99"), tuple.getValue(2));
    }
}
