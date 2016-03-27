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

// $Id: OfflineActionModel_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.impl.jms.JmsClient;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;
import com.sri.tasklearning.spine.util.ATRTestUtil;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * CPOF at least needs to perform certain operations using the action model when
 * the PAL system is not online. (This is done for certain types of CPOF
 * upgrades.) These tests ensure that the action model can be created when PAL
 * is not running, and that the action model can support certain minimal
 * functionality.
 *
 * @author chris
 */
public class OfflineActionModel_FuncTest
        extends PALTestCase {
    private static final String ns = "ns";

    private ActionModel actionModel;
    private Bridge bridge;

    @BeforeMethod
    public void ensureOffline()
            throws Exception {
        // If we can create a local spine, it means there's already one running;
        // therefore, PAL is (probably) running. If this throws an exception, it
        // means PAL is running.
        Spine spine = new JmsSpine(JmsClient.LOCAL, "foo");
        spine.shutdown(true);
    }

    @BeforeMethod
    public void setup()
            throws Exception {
        actionModel = ActionModel.offlineInstance();

        // Private accessor pattern to grab the bridge field from ActionModel.
        for(Field field : actionModel.getClass().getDeclaredFields()) {
            if(field.getName().equals("bridge")) {
                field.setAccessible(true);
                bridge = (Bridge) field.get(actionModel);
            }
        }
        Assert.assertNotNull(bridge);
    }

    @Test
    public void load()
            throws Exception {
        URL url = ActionModels.class.getResource(ActionModels.METADATA);
        Set<ActionModelDef> types = actionModel.load(url, ns);
        Assert.assertEquals(6, types.size());
    }

    @Test
    public void getCachedType()
            throws Exception {
        load();

        TypeDef string0Def = (TypeDef) actionModel.getType(TypeNameFactory
                .makeName("string0", "1.0", ns));
        Assert.assertNotNull(string0Def);
        TypeDef string1Def = (TypeDef) actionModel.getType(TypeNameFactory
                .makeName("string1", "1.0", ns));
        Assert.assertNotNull(string1Def);
        TypeDef string2Def = (TypeDef) actionModel.getType(TypeNameFactory
                .makeName("string2", "1.0", ns));
        Assert.assertNotNull(string2Def);
        ActionDef action0Def = (ActionDef) actionModel
                .getType(TypeNameFactory.makeName("action0", "1.0", ns));
        Assert.assertNotNull(action0Def);
        ActionDef action1Def = (ActionDef) actionModel
                .getType(TypeNameFactory.makeName("action1", "1.0", ns));
        Assert.assertNotNull(action1Def);
        ActionDef action2Def = (ActionDef) actionModel
                .getType(TypeNameFactory.makeName("action2", "1.0", ns));
        Assert.assertNotNull(action2Def);
    }

    @Test
    public void getNamespaceMetadata()
            throws Exception {
        load();

        Map<String, String> md = actionModel.getNamespaceMetadata(ns, "1.0");
        Assert.assertEquals("value1", md.get("key1"));
        Assert.assertEquals("value2", md.get("key2"));
    }

    @Test
    public void addType()
            throws Exception {
        SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName("foo",
                "1.0", ns);
        CustomTypeFactory fact = new ToStringFactory(String.class.getName());
        actionModel.registerCustomTypeFactory(name, fact);
        TypeDef in = new CustomTypeDef(ATRTestUtil.makeCustomType(name,
                String.class), bridge);
        actionModel.storeType((SimpleTypeName) in.getName(), in);
        TypeDef out = (TypeDef) actionModel.getType(name);
        Assert.assertNotNull(out);
        Assert.assertEquals(in, out);
    }

    @Test
    public void getTypes()
            throws Exception {
        load();
        Set<ActionModelDef> types = actionModel.getTypes();
        Assert.assertEquals(6, types.size());
    }

    @Test
    public void removeType()
            throws Exception {
        load();
        Assert.assertEquals(6, actionModel.getTypes().size());
        SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName(
                "action2", "1.0", ns);
        actionModel.storeType(name, null);
        Assert.assertEquals(5, actionModel.getTypes().size());
        Assert.assertEquals(null, actionModel.getType(name));
    }


}
