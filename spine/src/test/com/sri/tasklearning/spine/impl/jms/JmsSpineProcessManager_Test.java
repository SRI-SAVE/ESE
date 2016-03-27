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

package com.sri.tasklearning.spine.impl.jms;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import com.sri.tasklearning.spine.ModuleManager;
import com.sri.tasklearning.spine.SpineStartupException;
import com.sri.tasklearning.spine.StartupMode;
import com.sri.tasklearning.spine.impl.jms.util.SpineTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class JmsSpineProcessManager_Test extends SpineTestCase {
     private static final Logger log = LoggerFactory.getLogger("JmsSpineProcessManager_Test");
    private JmsSpineProcessManager spineProcessManager;

    @AfterMethod
    public void tearDown() throws Throwable {
        if (spineProcessManager.getSpine() != null) {
            spineProcessManager.getSpine().shutdown(true);
        }
        spineProcessManager = null;
    }

    @Test
    public void CanConstruct() {
        spineProcessManager = new JmsSpineProcessManager();
        assertNotNull(spineProcessManager);
        assertTrue(JmsSpineProcessManager.class.isInstance(spineProcessManager));
    }

    @Test
    public void CanProvideCorrectStartupFeatures() {
        spineProcessManager = new JmsSpineProcessManager();
        assertEquals(StartupMode.IN_JVM, spineProcessManager.getDefaultStartupMode());
        assertEquals(JmsSpineProcessManager.SPINE_MODULE_NAME, spineProcessManager.getModuleName());
    }

    @Test
    public void CanStartupInJVM() throws SpineStartupException {
        spineProcessManager = new JmsSpineProcessManager();
        ModuleManager moduleManager = new ModuleManager(spineProcessManager);
        moduleManager.startAll();
        assertTrue(spineProcessManager.isRunning());
    }

    @Test
    public void ShutdownBehaviorInJVM() throws SpineStartupException, InterruptedException {
        spineProcessManager = new JmsSpineProcessManager();
        ModuleManager moduleManager = new ModuleManager(spineProcessManager);
        moduleManager.startAll();
        assertTrue(spineProcessManager.isRunning());
        spineProcessManager.stop();
        // Give it time to complete finalization
        Thread.sleep(1000);
        int attempts = 0;
        while (spineProcessManager.isRunning() && attempts++ < 10) {
            log.info("Waiting for Spine to exit");
            Thread.sleep(1000);
        }
        // Make sure it is gone
        assertTrue(!spineProcessManager.isRunning());
    }

}
