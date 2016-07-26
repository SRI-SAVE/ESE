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

// $Id: ActionModel_Test.java 7750 2016-07-26 16:53:01Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.pal.upgrader.MemoryTypeStorage;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.impl.jms.util.MockSpine;
import com.sri.tasklearning.spine.util.TypeCache;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ActionModel_Test
        extends PALTestCase {
    private IMocksControl mockCtrl;
    private ActionModel actionModel;
    private TypeStorage storage;

    @BeforeMethod
    public void setup()
            throws Exception {
        mockCtrl = EasyMock.createControl();
        Bridge bridge = mockCtrl.createMock(Bridge.class);
        storage = new MemoryTypeStorage();
        EasyMock.expect(bridge.getTypeStorage()).andReturn(storage).anyTimes();
        ActionModelFactory amf = new ActionModelFactory(bridge);
        EasyMock.expect(bridge.getActionModelFactory()).andReturn(amf)
                .anyTimes();
        Spine spine = new MockSpine();
        EasyMock.expect(bridge.getSpine()).andReturn(spine).anyTimes();
        TypeCache tc = new TypeCache(spine);
        EasyMock.expect(bridge.getTypeCache()).andReturn(tc).anyTimes();
        TypeLoaderPublisher tlp = new TypeLoaderPublisher(bridge);
        EasyMock.expect(bridge.getActionLoaderPublisher()).andReturn(tlp)
                .anyTimes();
        actionModel = new ActionModel(bridge);
        EasyMock.expect(bridge.getActionModel()).andReturn(actionModel)
                .anyTimes();
        mockCtrl.replay();

    }

    @Test
    public void loadSimpleFile()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.SIMPLE);
        Set<ActionModelDef> actions = actionModel.load(url, "ns");
        assertEquals(5, actions.size());
        for (ActionModelDef type : actions) {
            ActionModelDef type2 = actionModel.getType(type.getName());
            assertEquals(type, type2);
        }
        ActionModelDef type1 = actionModel.getType(TypeNameFactory.makeName(
                "String", "1.0", "ns"));
        assertNotNull(type1);
        assertTrue(actions.contains(type1));
        assertTrue(type1 instanceof PrimitiveTypeDef);
        assertEquals(1, type1.listMetadataKeys().size());
        assertNotNull(type1.getMetadata(TypeDef.DESCRIPTION));

        mockCtrl.verify();
    }

    @Test
    public void loadSimpleString()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.SIMPLE);
        String amStr = ProcedureLearner.readWholeFile(url);
        Set<ActionModelDef> actions = actionModel.load(amStr, "ns");
        assertEquals(5, actions.size());

        mockCtrl.verify();
    }

    @Test
    public void loadSimpleXml()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.SIMPLE);
        JAXBContext jc = JAXBContext.newInstance(ActionModelType.class
                .getPackage().getName());
        Unmarshaller unmar = jc.createUnmarshaller();
        JAXBElement<?> ele = (JAXBElement<?>) unmar.unmarshal(url);
        ActionModelType amXml = (ActionModelType) ele.getValue();
        assertEquals(1, amXml.getType().size());
        assertEquals(4, amXml.getAction().size());

        Set<ActionModelDef> actions = actionModel.load(amXml, "ns");
        assertEquals(5, actions.size());

        mockCtrl.verify();
    }

    @Test
    public void namespaces()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.SIMPLE);
        String ns1 = "foo";
        String ns2 = "bar";

        Set<ActionModelDef> actions1 = actionModel.load(url, ns1);
        assertEquals(5, actions1.size());
        TypeDef type1 = (TypeDef) actionModel.getType(TypeNameFactory.makeName(
                "String", "1.0", ns1));
        assertNotNull(type1);
        assertTrue(actions1.contains(type1));

        Set<ActionModelDef> actions2 = actionModel.load(url, ns2);
        assertEquals(5, actions2.size());
        TypeDef type2 = (TypeDef) actionModel.getType(TypeNameFactory.makeName(
                "String", "1.0", ns2));
        assertNotNull(type2);
        assertTrue(actions2.contains(type2));

        mockCtrl.verify();
    }

    @Test
    public void loadMetadata()
            throws Exception {
        String ns = "ns";
        SimpleTypeName string0Name = (SimpleTypeName) TypeNameFactory.makeName(
                "string0", "1.0", ns);
        actionModel.registerCustomTypeFactory(string0Name, new ToStringFactory(
                String.class.getName()));
        SimpleTypeName string1Name = (SimpleTypeName) TypeNameFactory.makeName(
                "string1", "1.0", ns);
        actionModel.registerCustomTypeFactory(string1Name, new ToStringFactory(
                String.class.getName()));
        SimpleTypeName string2Name = (SimpleTypeName) TypeNameFactory.makeName(
                "string2", "1.0", ns);
        actionModel.registerCustomTypeFactory(string2Name, new ToStringFactory(
                String.class.getName()));
        URL url = ActionModels.class.getResource(ActionModels.METADATA);
        Set<ActionModelDef> actions = actionModel.load(url, ns);
        assertEquals(6, actions.size());

        Map<String, String> nsMeta = actionModel.getNamespaceMetadata(ns, "1.0");
        assertEquals(2, nsMeta.size());
        assertEquals("value1", nsMeta.get("key1"));
        assertEquals("value2", nsMeta.get("key2"));

        TypeDef string0 = (TypeDef) actionModel.getType(string0Name);
        assertEquals(1, string0.listMetadataKeys().size());
        assertNotNull(string0.getMetadata(TypeDef.DESCRIPTION));

        TypeDef string1 = (TypeDef) actionModel.getType(string1Name);
        assertEquals(2, string1.listMetadataKeys().size());
        assertNotNull(string1.getMetadata(TypeDef.DESCRIPTION));
        assertEquals("bar", string1.getMetadata("foo"));

        TypeDef string2 = (TypeDef) actionModel.getType(string2Name);
        assertEquals(3, string2.listMetadataKeys().size());
        assertNotNull(string2.getMetadata(TypeDef.DESCRIPTION));
        assertEquals("bar", string2.getMetadata("foo"));
        assertEquals("bar2", string2.getMetadata("foo2"));

        ActionDef action0 = (ActionDef) actionModel.getType(TypeNameFactory.makeName(
                "action0", "1.0", ns));
        assertNotNull(action0.getMetadata(TypeDef.DESCRIPTION));
        assertEquals(5, action0.listMetadataKeys().size());
        assertEquals("value1", action0.getParamMetadata(0, "key1"));
        assertEquals("value2", action0.getParamMetadata(0, "key2"));
        assertEquals(2, action0.listParamMetadataKeys(0).size());

        ActionDef action1 = (ActionDef) actionModel.getType(TypeNameFactory
                .makeName("action1", "1.0", ns));
        assertEquals(6, action1.listMetadataKeys().size());
        assertNotNull(action1.getMetadata(TypeDef.DESCRIPTION));
        assertEquals("bob", action1.getMetadata("alice"));

        ActionDef action2 = (ActionDef) actionModel.getType(TypeNameFactory
                .makeName("action2", "1.0", ns));
        assertEquals(8, action2.listMetadataKeys().size());
        assertNotNull(action2.getMetadata(TypeDef.DESCRIPTION));
        assertEquals("value1", action2.getMetadata("key1"));
        assertEquals("value2", action2.getMetadata("key2"));

        mockCtrl.verify();
    }
}
