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

// $Id: ProcedureDef_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Set;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.impl.CTRActionDeclaration;
import com.sri.ai.lumen.atr.impl.CTRParameter;
import com.sri.ai.lumen.atr.impl.CTRSig;
import com.sri.ai.lumen.atr.task.impl.CTRPass;
import com.sri.ai.lumen.atr.term.impl.CTRMap;
import com.sri.ai.lumen.atr.term.impl.CTRVariable;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.types.IntType;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.impl.jms.util.MockSpine;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.spine.util.ReplyWatcher;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ProcedureDef_Test
        extends PALTestCase {
    private TypeDef td1;
    private TypeDef td3;
    private IMocksControl mockCtrl;
    private Bridge bridge;
    private CTRActionDeclaration atr1;
    private ActionModel actionModel;

    @BeforeMethod
    public void setup()
            throws Exception {
        mockCtrl = EasyMock.createNiceControl();
        MockSpine spine = new MockSpine();
        ReplyWatcher<SerialNumberResponse> serialGetter = new ReplyWatcher<SerialNumberResponse>(
                SerialNumberResponse.class, spine);
        spine.subscribe(serialGetter, SystemMessageType.SERIAL_NUMBER_RESPONSE);
        bridge = mockCtrl.createMock(Bridge.class);
        EasyMock.expect(bridge.getSerialGetter()).andReturn(serialGetter)
                .anyTimes();
        EasyMock.expect(bridge.getSpine()).andReturn(spine).anyTimes();
        actionModel = new MockActionModel(bridge);
        EasyMock.expect(bridge.getActionModel()).andReturn(actionModel)
                .anyTimes();
        ActionModelFactory actionModelFactory = new ActionModelFactory(bridge);
        EasyMock.expect(bridge.getActionModelFactory())
                .andReturn(actionModelFactory).anyTimes();
        mockCtrl.replay();

        CustomTypeFactory factory1 = new ToStringFactory(String.class.getName());
        CustomTypeFactory factory3 = new ToStringFactory(
                IntType.class.getName());
        SimpleTypeName td1Name = (SimpleTypeName) TypeNameFactory
                .makeName("String");
        SimpleTypeName td3Name = (SimpleTypeName) TypeNameFactory
                .makeName("Int");
        actionModel.registerCustomTypeFactory(td1Name, factory1);
        actionModel.registerCustomTypeFactory(td3Name, factory3);
        td1 = new CustomTypeDef(ATRTestUtil.makeCustomType(td1Name,
                String.class), bridge);
        td3 = new CustomTypeDef(ATRTestUtil.makeCustomType(td3Name,
                IntType.class), bridge);
        actionModel.storeType(td1Name, td1);
        actionModel.storeType(td3Name, td3);

        ATRParameter params[] = new ATRParameter[2];
        params[0] = new CTRParameter(new CTRVariable("input1"), Modality.INPUT,
                td1.getName().getFullName());
        params[1] = new CTRParameter(new CTRVariable("output1"),
                Modality.OUTPUT, td3.getName().getFullName());
        TypeName procName = TypeNameFactory.makeName("proc1",
                LumenProcedureDef.SERIALIZATION_FORMAT_VERSION,
                LumenProcedureExecutor.getNamespace());
        ATRSig sig = new CTRSig(procName.getFullName(), params);
        atr1 = new CTRActionDeclaration(sig, new CTRPass(), new CTRMap());
    }

    @AfterMethod
    public void verify() {
        mockCtrl.verify();
    }

    @Test
    public void twoArgProc()
            throws Exception {
        ProcedureDef pd = new TestProcedureDef(atr1, bridge);
        assertEquals(2, pd.size());
        assertTrue(pd.isInputParam(0));
        assertEquals("input1", pd.getParamName(0));
        assertFalse(pd.isInputParam(1));
        assertEquals("output1", pd.getParamName(1));
        assertTrue(pd.isTransient());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullSource()
            throws Exception {
        new TestProcedureDef(null, bridge);
    }

    @Test
    public void defaultValue()
            throws Exception {
        ProcedureDef pd1 = new TestProcedureDef(atr1, bridge);
        assertNull(pd1.getDefaultValue(0));
        ProcedureDef pd2 = pd1.newDefaultValue(0, "foo");
        assertEquals("foo", pd2.getDefaultValue(0));
        ProcedureDef pd3 = pd2.newDefaultValue(0, null);
        assertNull(pd3.getDefaultValue(0));
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void setDefaultOutput()
            throws Exception {
        ProcedureDef pd1 = new TestProcedureDef(atr1, bridge);
        pd1.newDefaultValue(1, null);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void getDefaultOutput()
            throws Exception {
        ProcedureDef pd1 = new TestProcedureDef(atr1, bridge);
        pd1.getDefaultValue(1);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void setDefaultUnknown()
            throws Exception {
        ProcedureDef pd1 = new TestProcedureDef(atr1, bridge);
        pd1.newDefaultValue(2, null);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void getDefaultUnknown()
            throws Exception {
        ProcedureDef pd1 = new TestProcedureDef(atr1, bridge);
        pd1.getDefaultValue(2);
    }

    private static class TestProcedureDef
            extends ProcedureDef {
        protected TestProcedureDef(ATRActionDeclaration atr,
                                   Bridge bridge)
                throws PALException {
            super(atr, bridge);
        }

        @Override
        public String getXml() {
            throw new RuntimeException("unimplemented");
        }

        @Override
        protected Set<ActionModelDef> getRequiredDefs()
                throws PALException {
            throw new RuntimeException("unimplemented");
        }

        @Override
        public ProcedureDef copyAndRename(SimpleTypeName newName)
                throws PALException {
            throw new RuntimeException("unimplemented");
        }
    }
}
