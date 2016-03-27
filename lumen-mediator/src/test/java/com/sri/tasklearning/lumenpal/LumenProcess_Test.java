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

package com.sri.tasklearning.lumenpal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import com.sri.tasklearning.lumenpal.util.LumenMediatorTestCase;
import com.sri.tasklearning.spine.StartupMode;
import com.sri.tasklearning.spine.impl.jms.JmsClient;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;

import org.testng.annotations.Test;

public class LumenProcess_Test extends LumenMediatorTestCase {

    @Test
    public void CanConstruct() {
        LumenProcess lumenProcess = new LumenProcess();
        assertNotNull(lumenProcess);
        assertTrue(LumenProcess.class.isInstance(lumenProcess));
    }

    @Test
    public void canProvideCorrectStartupMode() {
        LumenProcess lumenProcess = new LumenProcess();
        assertEquals(StartupMode.IN_JVM, lumenProcess.getDefaultStartupMode());
    }

    @Test
    public void isRunningWorksAsExpected() throws Exception {
        JmsSpine jmsSpine = new JmsSpine(JmsClient.LOCAL, "test shell");

        LumenProcess lumenProcess = new LumenProcess();
        assertFalse(lumenProcess.isRunning());

        lumenProcess.startInJvm();
        assertTrue(lumenProcess.isRunning());

        jmsSpine.shutdown(true);
        lumenProcess.stop();
        assertFalse(lumenProcess.isRunning());
    }

    @Test
    public void startsAndStopsProperly() throws Exception {
        JmsSpine jmsSpine = new JmsSpine(JmsClient.LOCAL, "test shell");
        LumenProcess lumenProcess = new LumenProcess();
        lumenProcess.startInJvm();

        assertTrue(lumenProcess.isRunning());
        lumenProcess.stop();
        assertFalse(lumenProcess.isRunning());

        jmsSpine.shutdown(true);
    }
}
