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

// $Id: TypeStorageSpeed_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.HashSet;
import java.util.Set;

import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TypeStorageSpeed_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory
            .getLogger(TypeStorageSpeed_FuncTest.class);

    private static Bridge bridge1;
    private static Bridge bridge2;

    @BeforeClass
    public static void load()
            throws Exception {
        /*
         * This flag lets you run the tests against your procedures and action
         * models which are stored in your machine's user data directory. In
         * other words, this looks at the same data the Editor uses.
         */
        boolean useUserData = false;
        if (useUserData) {
            setupNoStorage();
            System.getProperties().remove(Bridge.STORAGE_DIR_PROP);
        } else {
            setup();
        }
        bridge1 = palBridge;
        bridge1.getTypeStorage(); // side effect
        bridge2 = Bridge.newInstance("bridge2");
    }

    @Test
    public void localTypeListSpeed()
            throws Exception {
        long startTime = System.currentTimeMillis();
        TypeStorage storage = bridge1.getTypeStorage();
        Set<SimpleTypeName> names = storage.listTypes();
        long endTime = System.currentTimeMillis();
        log.info("localTypeListSpeed: {}ms for {} types", endTime - startTime,
                names.size());
    }

    @Test
    public void remoteTypeListSpeed()
            throws Exception {
        long startTime = System.currentTimeMillis();
        TypeStorage storage = bridge2.getTypeStorage();
        Assert.assertNull(storage);
        Set<SimpleTypeName> names = bridge2.getActionModel().listTypes();
        long endTime = System.currentTimeMillis();
        log.info("remoteTypeListSpeed: {}ms for {} types", endTime - startTime,
                names.size());
    }

    @Test
    public void localTypeLoadSpeed()
            throws Exception {
        long startTime = System.currentTimeMillis();
        ActionModel am = bridge1.getActionModel();
        Set<ActionModelDef> types = am.getTypes();
        long endTime = System.currentTimeMillis();
        log.info("localTypeLoadSpeed: {}ms for {} types", endTime - startTime,
                types.size());
    }

    @Test
    public void remoteTypeLoadSpeed()
            throws Exception {
        long startTime = System.currentTimeMillis();
        ActionModel am = bridge2.getActionModel();
        Assert.assertNull(bridge2.getTypeStorage());
        Set<ActionModelDef> types = am.getTypes();
        long endTime = System.currentTimeMillis();
        log.info("remoteTypeLoadSpeed: {}ms for {} types", endTime - startTime,
                types.size());
    }

    @Test
    public void multiLoadSpeed()
            throws Exception {
        long testStartTime = System.currentTimeMillis();
        ActionModel am = bridge2.getActionModel();
        TypeStorage storage = bridge2.getTypeStorage();
        Assert.assertNull(storage);
        Set<ActionModelDef> types = new HashSet<ActionModelDef>();
        for (SimpleTypeName name : am.listTypes()) {
            long typeStartTime = System.currentTimeMillis();
            ActionModelDef type = am.getType(name);
            types.add(type);
            long typeEndTime = System.currentTimeMillis();
            log.info("multiLoadSpeed: {}ms for {}",
                    typeEndTime - typeStartTime, name);
        }
        long testEndTime = System.currentTimeMillis();
        log.info("multiLoadSpeed: {}ms for {} types", testEndTime
                - testStartTime, types.size());
    }
}
