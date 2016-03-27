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

// $Id: ActionDef_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.types.IntType;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.impl.jms.util.MockSpine;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.messages.contents.ParamClass;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.spine.util.ReplyWatcher;
import com.sri.tasklearning.spine.util.TypeCache;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ActionDef_Test
        extends PALTestCase {
    private TypeDef td1;
    private TypeDef td2;
    private TypeDef td3;
    private IMocksControl mockCtrl;
    private Bridge bridge;
    private ActionModel actionModel;
    private CTRConstructor ctrBuilder;

    @BeforeMethod
    public void setup()
            throws Exception {
        ctrBuilder = new CTRConstructor();
        mockCtrl = EasyMock.createNiceControl();
        MockSpine spine = new MockSpine();
        ReplyWatcher<SerialNumberResponse> serialGetter = new ReplyWatcher<SerialNumberResponse>(
                SerialNumberResponse.class, spine);
        spine.subscribe(serialGetter, SystemMessageType.SERIAL_NUMBER_RESPONSE);
        bridge = mockCtrl.createMock(Bridge.class);
        EasyMock.expect(bridge.getSerialGetter()).andReturn(serialGetter)
                .anyTimes();
        EasyMock.expect(bridge.getSpine()).andReturn(spine).anyTimes();
        ExecutorMap executorMap = new ExecutorMap(bridge);
        EasyMock.expect(bridge.getExecutorMap()).andReturn(executorMap).anyTimes();
        actionModel = new MockActionModel(bridge);
        EasyMock.expect(bridge.getActionModel()).andReturn(actionModel)
                .anyTimes();
        ActionModelFactory amf = new ActionModelFactory(bridge);
        EasyMock.expect(bridge.getActionModelFactory()).andReturn(amf)
                .anyTimes();
        TypeCache tc = new TypeCache(spine);
        EasyMock.expect(bridge.getTypeCache()).andReturn(tc).anyTimes();
        mockCtrl.replay();

        CustomTypeFactory factory1 = new ToStringFactory(String.class.getName());
        CustomTypeFactory factory3 = new ToStringFactory(
                IntType.class.getName());
        SimpleTypeName td1Name = (SimpleTypeName) TypeNameFactory
                .makeName("a^1^String");
        SimpleTypeName td2Name = (SimpleTypeName) TypeNameFactory
                .makeName("String2");
        SimpleTypeName td3Name = (SimpleTypeName) TypeNameFactory
                .makeName("Int");
        actionModel.registerCustomTypeFactory(td1Name, factory1);
        actionModel.registerCustomTypeFactory(td2Name, factory1);
        actionModel.registerCustomTypeFactory(td3Name, factory3);
        td1 = new CustomTypeDef(ATRTestUtil.makeCustomType(td1Name,
                String.class), bridge);
        td2 = new CustomTypeDef(ATRTestUtil.makeCustomType(td2Name,
                String.class), bridge);
        td3 = new CustomTypeDef(ATRTestUtil.makeCustomType(td3Name,
                IntType.class), bridge);
        actionModel.storeType(td1Name, td1);
        actionModel.storeType(td2Name, td2);
        actionModel.storeType(td3Name, td3);
    }

    @AfterMethod
    public void verify() {
        mockCtrl.verify();
    }

    @Test
    public void twoArgAction()
            throws Exception {
        ATRParameter[] params = new ATRParameter[2];
        params[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        params[1] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.OUTPUT, td3.getName()
                .getFullName(), null);
        ATRActionDeclaration atr = ATRTestUtil.makeAction("action1", params,
                null, null);
        ActionDef ad = new ActionDef(atr, bridge);
        assertEquals(2, ad.size());
        assertTrue(ad.isInputParam(0));
        assertEquals("input1", ad.getParamName(0));
        assertFalse(ad.isInputParam(1));
        assertEquals("output1", ad.getParamName(1));
        Assert.assertEquals(ParamClass.GENERALIZABLE, ad.getParamClass(0));
        Assert.assertEquals(ParamClass.EXTERNAL, ad.getParamClass(1));
        assertTrue(ad.isTransient());
    }

    @Test
    public void transientAction()
            throws Exception {
        ATRParameter[] params = new ATRParameter[2];
        params[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        params[1] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.OUTPUT, td3.getName()
                .getFullName(), null);
        Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
        propMap.put(TypeUtil.TRANSIENT, ctrBuilder.createLiteral("true", null));
        ATRMap props = ctrBuilder.createMap(propMap);
        ATRActionDeclaration atr = ATRTestUtil.makeAction("action1", params,
                props, null);
        ActionDef ad = new ActionDef(atr, bridge);
        assertTrue(ad.isTransient());
    }

    @Test
    public void zeroParams()
            throws Exception {
        ATRParameter[] params = new ATRParameter[0];
        ATRActionDeclaration atr = ATRTestUtil.makeAction("nullAction", params,
                null, null);
        ActionDef ad = new ActionDef(atr, bridge);
        assertEquals(0, ad.size());
    }

    @Test
    public void equals()
            throws Exception {
        Map<String, String> meta = new HashMap<String, String>();
        meta.put("key", "value");
        ATRParameter param1 = ctrBuilder.createParameter(ctrBuilder
                .createVariable("field"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        ATRParameter param2 = ctrBuilder.createParameter(ctrBuilder
                .createVariable("foo"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        ATRParameter param3 = ctrBuilder.createParameter(ctrBuilder
                .createVariable("field"), Modality.INPUT, td2.getName()
                .getFullName(), null);
        ATRParameter param4 = ctrBuilder.createParameter(ctrBuilder
                .createVariable("field"), Modality.OUTPUT, td1.getName()
                .getFullName(), null);
        ATRParameter param6 = ctrBuilder.createParameter(ctrBuilder
                .createVariable("field"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        ATRActionDeclaration atr1 = ATRTestUtil.makeAction("action",
                new ATRParameter[] { param1 }, null, null);
        ActionDef ad1 = new ActionDef(atr1, bridge);
        ATRActionDeclaration atr2 = ATRTestUtil.makeAction("foo",
                new ATRParameter[] { param1 }, null, null);
        ActionDef ad2 = new ActionDef(atr2, bridge);
        ATRActionDeclaration atr3 = ATRTestUtil.makeAction("action",
                new ATRParameter[] { param2 }, null, null);
        ActionDef ad3 = new ActionDef(atr3, bridge);
        ATRActionDeclaration atr4 = ATRTestUtil.makeAction("action",
                new ATRParameter[] { param3 }, null, null);
        ActionDef ad4 = new ActionDef(atr4, bridge);
        ATRActionDeclaration atr5 = ATRTestUtil.makeAction("action",
                new ATRParameter[] { param4 }, null, null);
        ActionDef ad5 = new ActionDef(atr5, bridge);
        ATRActionDeclaration atr7 = ATRTestUtil.makeAction("action",
                new ATRParameter[] { param6 }, null, null);
        ActionDef ad7 = new ActionDef(atr7, bridge);
        ATRActionDeclaration atr8 = ATRTestUtil.makeAction("action",
                new ATRParameter[] { param1 }, null, null);
        ActionDef ad8 = new ActionDef(atr8, bridge);

        assertFalse(ad1.equals(ad2));
        assertFalse(ad2.equals(ad1));
        assertFalse(ad1.equals(ad3));
        assertFalse(ad3.equals(ad1));
        assertFalse(ad1.equals(ad4));
        assertFalse(ad4.equals(ad1));
        assertFalse(ad1.equals(ad5));
        assertFalse(ad5.equals(ad1));

        assertTrue(ad1.equals(ad7));
        assertTrue(ad7.equals(ad1));
        assertTrue(ad1.equals(ad8));
        assertTrue(ad8.equals(ad1));
    }

    @Test
    public void newInstance()
            throws Exception {
        ATRParameter[] params = new ATRParameter[2];
        params[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        params[1] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.OUTPUT, td3.getName()
                .getFullName(), null);
        ATRActionDeclaration atr = ATRTestUtil.makeAction("action1", params,
                null, null);
        ActionDef ad = new ActionDef(atr, bridge);
        ActionInvocation ai1 = ad.invoke(null);
        assertNotNull(ai1);
        assertTrue(ai1 instanceof ActionInvocation);
        ActionInvocation ai2 = ad.invoke(null, "foo");
        ai2.setValue(1, new IntType("5"));
        assertNotNull(ai2);
        assertTrue(ai2 instanceof ActionInvocation);
        List<Object> values = new ArrayList<Object>();
        values.add("foo");
        ActionInvocation ai3 = ad.invoke(null, values);
        assertNotNull(ai3);
        assertTrue(ai3 instanceof ActionInvocation);
        assertEquals(ai1.getDefinition(), ad);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void newInstanceArrayWrongSize()
            throws Exception {
        ATRParameter[] params = new ATRParameter[2];
        params[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        params[1] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.OUTPUT, td3.getName()
                .getFullName(), null);
        ATRActionDeclaration atr = ATRTestUtil.makeAction("action1", params,
                null, null);
        ActionDef ad = new ActionDef(atr, bridge);
        ad.invoke(null, "foo", new IntType("17"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void newInstanceListWrongSize()
            throws Exception {
        ATRParameter[] params = new ATRParameter[2];
        params[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        params[1] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.OUTPUT, td3.getName()
                .getFullName(), null);
        ATRActionDeclaration atr = ATRTestUtil.makeAction("action1", params,
                null, null);
        ActionDef ad = new ActionDef(atr, bridge);
        List<Object> args = new ArrayList<Object>();
        args.add("foo");
        args.add(new IntType("19"));
        args.add(null);
        ad.invoke(null, args);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void newInstanceWrongArgType()
            throws Exception {
        ATRParameter[] params = new ATRParameter[2];
        params[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        params[1] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.OUTPUT, td3.getName()
                .getFullName(), null);
        ATRActionDeclaration atr = ATRTestUtil.makeAction("action1", params,
                null, null);
        ActionDef ad = new ActionDef(atr, bridge);
        ad.invoke(null, "foo", "bar");
    }

    @Test
    public void newInstanceNullArg()
            throws Exception {
        ATRParameter[] params = new ATRParameter[2];
        params[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        params[1] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.OUTPUT, td3.getName()
                .getFullName(), null);
        ATRActionDeclaration atr = ATRTestUtil.makeAction("action1", params,
                null, null);
        ActionDef ad = new ActionDef(atr, bridge);
        ActionInvocation ai = ad.invoke(null, (Object) null);
        assertNotNull(ai);
    }

    @Test
    public void inheritOneInput()
            throws Exception {
        ATRParameter[] superParams = new ATRParameter[1];
        superParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        ATRActionDeclaration superAtr = ATRTestUtil.makeAction("super",
                superParams, null, null);
        ActionDef superDef = new ActionDef(superAtr, bridge);
        actionModel.storeType(superDef.getName(), superDef);

        ATRParameter[] subParams = new ATRParameter[0];
        ATRActionDeclaration subAtr = ATRTestUtil.makeAction("sub", subParams,
                null, superDef.getAtr());
        ActionDef subDef = new ActionDef(subAtr, bridge);

        assertNotNull(subDef);
        assertEquals(1, subDef.size());
        assertEquals(1, subDef.numInputParams());
        assertEquals("input1", subDef.getParamName(0));
        assertEquals(0, subDef.getParamNum("input1"));
        assertTrue(subDef.isInputParam(0));
        assertEquals(td1.getName(), subDef.getParamTypeName(0));
        assertEquals(td1, subDef.getParamType(0));
    }

    @Test
    public void inheritInWithIn()
            throws Exception {
        ATRParameter[] superParams = new ATRParameter[1];
        superParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        ATRActionDeclaration superAtr = ATRTestUtil.makeAction("super",
                superParams, null, null);
        ActionDef superDef = new ActionDef(superAtr, bridge);
        actionModel.storeType(superDef.getName(), superDef);

        ATRParameter[] subParams = new ATRParameter[1];
        subParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input2"), Modality.INPUT, td2.getName()
                .getFullName(), null);
        ATRActionDeclaration subAtr = ATRTestUtil.makeAction("sub", subParams,
                null, superDef.getAtr());
        ActionDef subDef = new ActionDef(subAtr, bridge);

        assertNotNull(subDef);
        assertEquals(2, subDef.size());
        assertEquals(2, subDef.numInputParams());
        assertEquals("input1", subDef.getParamName(0));
        assertEquals("input2", subDef.getParamName(1));
        assertEquals(0, subDef.getParamNum("input1"));
        assertEquals(1, subDef.getParamNum("input2"));
        assertTrue(subDef.isInputParam(0));
        assertTrue(subDef.isInputParam(1));
        assertEquals(td1.getName(), subDef.getParamTypeName(0));
        assertEquals(td2.getName(), subDef.getParamTypeName(1));
        assertEquals(td1, subDef.getParamType(0));
        assertEquals(td2, subDef.getParamType(1));
    }

    @Test
    public void inheritOneOutput()
            throws Exception {
        ATRParameter[] superParams = new ATRParameter[1];
        superParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.OUTPUT, td1.getName()
                .getFullName(), null);
        ATRActionDeclaration superAtr = ATRTestUtil.makeAction("super",
                superParams, null, null);
        ActionDef superDef = new ActionDef(superAtr, bridge);
        actionModel.storeType(superDef.getName(), superDef);

        ATRParameter[] subParams = new ATRParameter[0];
        ATRActionDeclaration subAtr = ATRTestUtil.makeAction("sub", subParams,
                null, superDef.getAtr());
        ActionDef subDef = new ActionDef(subAtr, bridge);

        assertNotNull(subDef);
        assertEquals(1, subDef.size());
        assertEquals(0, subDef.numInputParams());
        assertEquals("output1", subDef.getParamName(0));
        assertEquals(0, subDef.getParamNum("output1"));
        assertFalse(subDef.isInputParam(0));
        assertEquals(td1.getName(), subDef.getParamTypeName(0));
        assertEquals(td1, subDef.getParamType(0));
    }

    @Test
    public void inheritOutWithOut()
            throws Exception {
        ATRParameter[] superParams = new ATRParameter[1];
        superParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.OUTPUT, td1.getName()
                .getFullName(), null);
        ATRActionDeclaration superAtr = ATRTestUtil.makeAction("super",
                superParams, null, null);
        ActionDef superDef = new ActionDef(superAtr, bridge);
        actionModel.storeType(superDef.getName(), superDef);

        ATRParameter[] subParams = new ATRParameter[1];
        subParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output2"), Modality.OUTPUT, td2.getName()
                .getFullName(), null);
        ATRActionDeclaration subAtr = ATRTestUtil.makeAction("sub", subParams,
                null, superDef.getAtr());
        ActionDef subDef = new ActionDef(subAtr, bridge);

        assertNotNull(subDef);
        assertEquals(2, subDef.size());
        assertEquals(0, subDef.numInputParams());
        assertEquals("output1", subDef.getParamName(0));
        assertEquals("output2", subDef.getParamName(1));
        assertEquals(0, subDef.getParamNum("output1"));
        assertEquals(1, subDef.getParamNum("output2"));
        assertFalse(subDef.isInputParam(0));
        assertFalse(subDef.isInputParam(1));
        assertEquals(td1.getName(), subDef.getParamTypeName(0));
        assertEquals(td2.getName(), subDef.getParamTypeName(1));
        assertEquals(td1, subDef.getParamType(0));
        assertEquals(td2, subDef.getParamType(1));
    }

    @Test
    public void inheritInWithOut()
            throws Exception {
        ATRParameter[] superParams = new ATRParameter[1];
        superParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        ATRActionDeclaration superAtr = ATRTestUtil.makeAction("super",
                superParams, null, null);
        ActionDef superDef = new ActionDef(superAtr, bridge);
        actionModel.storeType(superDef.getName(), superDef);

        ATRParameter[] subParams = new ATRParameter[1];
        subParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output2"), Modality.OUTPUT, td2.getName()
                .getFullName(), null);
        ATRActionDeclaration subAtr = ATRTestUtil.makeAction("sub", subParams,
                null, superDef.getAtr());
        ActionDef subDef = new ActionDef(subAtr, bridge);

        assertNotNull(subDef);
        assertEquals(2, subDef.size());
        assertEquals(1, subDef.numInputParams());
        assertEquals("input1", subDef.getParamName(0));
        assertEquals("output2", subDef.getParamName(1));
        assertEquals(0, subDef.getParamNum("input1"));
        assertEquals(1, subDef.getParamNum("output2"));
        assertTrue(subDef.isInputParam(0));
        assertFalse(subDef.isInputParam(1));
        assertEquals(td1.getName(), subDef.getParamTypeName(0));
        assertEquals(td2.getName(), subDef.getParamTypeName(1));
        assertEquals(td1, subDef.getParamType(0));
        assertEquals(td2, subDef.getParamType(1));
    }

    @Test
    public void inheritOutWithIn()
            throws Exception {
        ATRParameter[] superParams = new ATRParameter[1];
        superParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.OUTPUT, td1.getName()
                .getFullName(), null);
        ATRActionDeclaration superAtr = ATRTestUtil.makeAction("super",
                superParams, null, null);
        ActionDef superDef = new ActionDef(superAtr, bridge);
        actionModel.storeType(superDef.getName(), superDef);

        ATRParameter[] subParams = new ATRParameter[1];
        subParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input2"), Modality.INPUT, td2.getName()
                .getFullName(), null);
        ATRActionDeclaration subAtr = ATRTestUtil.makeAction("sub", subParams,
                null, superDef.getAtr());
        ActionDef subDef = new ActionDef(subAtr, bridge);

        assertNotNull(subDef);
        assertEquals(2, subDef.size());
        assertEquals(1, subDef.numInputParams());
        assertEquals("input2", subDef.getParamName(0));
        assertEquals("output1", subDef.getParamName(1));
        assertEquals(0, subDef.getParamNum("input2"));
        assertEquals(1, subDef.getParamNum("output1"));
        assertTrue(subDef.isInputParam(0));
        assertFalse(subDef.isInputParam(1));
        assertEquals(td2.getName(), subDef.getParamTypeName(0));
        assertEquals(td1.getName(), subDef.getParamTypeName(1));
        assertEquals(td2, subDef.getParamType(0));
        assertEquals(td1, subDef.getParamType(1));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void inheritInWithDupIn()
            throws Exception {
        ATRParameter[] superParams = new ATRParameter[1];
        superParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        ATRActionDeclaration superAtr = ATRTestUtil.makeAction("super",
                superParams, null, null);
        ActionDef superDef = new ActionDef(superAtr, bridge);
        actionModel.storeType(superDef.getName(), superDef);

        ATRParameter[] subParams = new ATRParameter[1];
        subParams[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        ATRActionDeclaration subAtr = ATRTestUtil.makeAction("sub", subParams,
                null, superDef.getAtr());
        new ActionDef(subAtr, bridge);
    }

    @Test
    public void paramMetadata()
            throws Exception {
        ATRParameter[] params = new ATRParameter[2];
        params[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), null);
        params[1] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.OUTPUT, td3.getName()
                .getFullName(), null);
        Map<String, ATRTerm> param0Map = new HashMap<String, ATRTerm>();
        param0Map.put("key1", ctrBuilder.createLiteral("value1", null));
        param0Map.put("key2", ctrBuilder.createLiteral("value2", null));
        ATRMap param0Props = ctrBuilder.createMap(param0Map);
        Map<String, ATRTerm> param1Map = new HashMap<String, ATRTerm>();
        param1Map.put("key3", ctrBuilder.createLiteral("value3", null));
        ATRMap param1Props = ctrBuilder.createMap(param1Map);
        Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
        propMap.put("$input1", param0Props);
        propMap.put("$output1", param1Props);
        ATRMap props = ctrBuilder.createMap(propMap);
        ATRActionDeclaration atr = ATRTestUtil.makeAction("action1", params,
                props, null);
        ActionDef ad = new ActionDef(atr, bridge);

        assertEquals(2, ad.listParamMetadataKeys(0).size());
        assertEquals(1, ad.listParamMetadataKeys(1).size());
        assertEquals("value1", ad.getParamMetadata(0, "key1"));
        assertEquals("value2", ad.getParamMetadata(0, "key2"));
        assertEquals("value3", ad.getParamMetadata(1, "key3"));
    }

    /**
     * Build an action with default values. Verify them. Convert to ATR and
     * back, verify again. Convert to XML and back, verify again
     *
     * @throws Exception
     */
    @Test
    public void defaultValues()
            throws Exception {
        String defVal = "Default Value #1";
        ATRTerm defValTerm = ctrBuilder.createLiteral(defVal, td1.getName().getFullName());
        ATRParameter[] params = new ATRParameter[1];
        params[0] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("input1"), Modality.INPUT, td1.getName()
                .getFullName(), defValTerm);
        ATRActionDeclaration atr = ATRTestUtil.makeAction("a^1^action1", params,
                null, null);
        ActionDef ad = new ActionDef(atr, bridge);
        SimpleTypeName name = ad.getName();
        Assert.assertEquals(defVal, ad.getDefaultValue(0));
        Assert.assertEquals(defVal, ad.getDefaultValue("input1"));

        atr = ad.getAtr();
        ad = new ActionDef(atr, bridge);
        Assert.assertEquals(defVal, ad.getDefaultValue(0));
        Assert.assertEquals(defVal, ad.getDefaultValue("input1"));

        String xmlStr = ad.getXml();
        actionModel.storeType((SimpleTypeName) td1.getName(), td1);
        Set<ActionModelDef> defs = actionModel
                .read(xmlStr, name.getNamespace());
        ad = null;
        for (ActionModelDef def : defs) {
            if (def.getName().equals(name)) {
                ad = (ActionDef) def;
            }
        }
        if (ad == null) {
            Assert.fail();
        }
        Assert.assertEquals(defVal, ad.getDefaultValue(0));
        Assert.assertEquals(defVal, ad.getDefaultValue("input1"));
    }
}
