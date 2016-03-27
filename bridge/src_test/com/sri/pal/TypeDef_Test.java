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

// $Id: TypeDef_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRConstructor;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.decl.impl.CTRTypeDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.term.ATRList;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.impl.CTRLiteral;
import com.sri.pal.jaxb.TypeType;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.spine.util.TypeUtil;

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
public class TypeDef_Test
        extends PALTestCase {
    private IMocksControl mockCtrl;
    private Bridge bridge;
    private CTRConstructor ctrBuilder;

    @BeforeMethod
    public void setup() {
        ctrBuilder = new CTRConstructor();
        mockCtrl = EasyMock.createNiceControl();
        bridge = mockCtrl.createMock(Bridge.class);
        mockCtrl.replay();
    }

    @AfterMethod
    public void verify() {
        mockCtrl.verify();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyName()
            throws Exception {
        new CustomTypeDef(ATRTestUtil.makeCustomType("", String.class), bridge);
    }

    @Test
    public void metadata() {
        Map<String, ATRTerm> origMeta = new HashMap<String, ATRTerm>();
        origMeta.put("key1", new CTRLiteral("value1"));
        origMeta.put("key2", new CTRLiteral("value2"));
        origMeta.put(TypeUtil.DESCRIPTION, new CTRLiteral("test type"));
        ATRMap props = ctrBuilder.createMap(origMeta);
        List<String> equivs = new ArrayList<String>();
        equivs.add("string");
        ATRTypeDeclaration ctr = CTRTypeDeclaration.createAliasType("td",
                equivs, props);
        TypeDef td1 = new MyTypeDef(ctr, bridge);
        assertEquals(3, td1.listMetadataKeys().size());
        for (String key : td1.listMetadataKeys()) {
            String value = td1.getMetadata(key);
            if (key.equals("key1")) {
                assertEquals("value1", value);
            } else if (key.equals("key2")) {
                assertEquals("value2", value);
            } else if (key.equals(TypeUtil.DESCRIPTION)) {
                /* Do nothing. */
            } else {
                fail();
            }
        }
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullMetaKey() {
        Map<String, ATRTerm> meta = new HashMap<String, ATRTerm>();
        meta.put(null, new CTRLiteral("foo"));
        ATRMap props = ctrBuilder.createMap(meta);
        List<String> equivs = new ArrayList<String>();
        equivs.add("string");
        ATRTypeDeclaration atr = CTRTypeDeclaration.createAliasType("td",
                equivs, props);
        new MyTypeDef(atr, bridge);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void listMetadata()
            throws Exception {
        Map<String, ATRTerm> meta = new HashMap<String, ATRTerm>();
        List<ATRTerm> list = new ArrayList<ATRTerm>();
        list.add(ctrBuilder.createLiteral("val1", null));
        list.add(ctrBuilder.createLiteral("val2", null));
        ATRList atrList = ctrBuilder.createList(list);
        meta.put("foo", atrList);
        ATRMap props = ctrBuilder.createMap(meta);
        List<String> equivs = new ArrayList<String>();
        equivs.add("string");
        ATRTypeDeclaration atr = CTRTypeDeclaration.createAliasType("td",
                equivs, props);
        new MyTypeDef(atr, bridge);
    }

    private static class MyTypeDef
            extends TypeDef {

        protected MyTypeDef(ATRTypeDeclaration atrDecl,
                            Bridge bridge) {
            super(atrDecl, bridge);
        }

        @Override
        protected void fillInXml(TypeType typeXml) {
            throw new RuntimeException("unimplemented");
        }

        @Override
        Object stringify(Object value) {
            throw new RuntimeException("unimplemented");
        }

        @Override
        int getStringSize(Object strValue) {
            throw new RuntimeException("unimplemented");
        }

        @Override
        Object unstringify(Object strValue) {
            throw new RuntimeException("unimplemented");
        }

        @Override
        public Object fromAtr(ATRTerm atrValue) {
            throw new RuntimeException("unimplemented");
        }

        @Override
        ATRTerm nonNullToAtr(
                ATRConstructor<ATR, ?, ATRTerm, ?, ?, ?, ?, ?, ?, ?, ?> constructor,
                Object value) {
            throw new RuntimeException("unimplemented");
        }
    }
}
