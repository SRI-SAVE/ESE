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

// $Id: PALStatusListener_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;

import java.net.URL;
import java.util.Properties;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.util.PALBridgeTestCase;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.JmsClient;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;
import com.sri.tasklearning.spine.messages.PingRequest;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class PALStatusListener_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = "psl";
    private Spine spine;

    @BeforeClass
    public static void load()
            throws Exception {
        // Set the Bridge PAL_BACKEND_TIMEOUT_PROPERTY to 5 seconds, to test the property
        // setting ability but also to ensure we don't have to wait a whole minute for the
        // PALBridge to time out for the timeout test below.
        Properties systemProperties = System.getProperties();
        String customTimeout = "5000";
        systemProperties.put("PAL.ShellPingDelayTimeout", customTimeout);
        System.setProperties(systemProperties);

        URL url = ActionModels.class.getResource(ActionModels.ACTIONS);
        setup(url, NAMESPACE);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        palBridge.shutdown();
        spine.shutdown(true);
    }

    /**
     * We simulate the Shell's watchdog by calling the Bridge's ping once. 5
     * seconds later, it should signal that PAL is merely up, not
     * "up with watchdog."
     */
    @Test
    public void timeout()
            throws Exception {
        spine = new JmsSpine(JmsClient.REMOTE, "PingTest");

        StatusListener listener = new StatusListener();
        assertEquals(null, listener.getStatus());

        PALStatusMonitor.addListener(listener);

        Thread.sleep(200);
        assertEquals(PALStatusListener.Status.UP, listener.getStatus());

        sendPing(spine);
        Thread.sleep(100);
        assertEquals(PALStatusListener.Status.WATCHDOG_UP, listener.getStatus());

        Thread.sleep(4000);
        Assert.assertEquals(listener.getStatus(),
                PALStatusListener.Status.WATCHDOG_UP);

        Thread.sleep(2000);
        assertEquals(PALStatusListener.Status.UP, listener.getStatus());
    }

    private void sendPing(Spine spine)
            throws SpineException {
        TransactionUID uid = spine.getNextUid();
        PingRequest pingMsg = new PingRequest(spine
                .getClientId(), "PALBridge", uid);
        spine.send(pingMsg);
    }

    private class StatusListener
            implements PALStatusListener {
        private Status lastStatus;

        @Override
        public void newStatus(Status status) {
            lastStatus = status;
        }

        public Status getStatus() {
            return lastStatus;
        }
    }
}
