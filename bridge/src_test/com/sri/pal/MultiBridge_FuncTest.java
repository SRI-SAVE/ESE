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

// $Id: MultiBridge_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test interactions when multiple applications (multiple Bridge clients) are
 * operating concurrently. Note that ActionDef and similar objects are tied to
 * the Bridge instance used to create them. These tests need to be careful to
 * use the correct instances of objects.
 */
public class MultiBridge_FuncTest
        extends PALBridgeTestCase {
    private static final String NS = "MBFT";
    private static final String VERS = "1.0";

    private static SimpleTypeName makeStringId1;
    private static ActionDef makeStringDef1;
    private static SimpleTypeName concatId1;
    private static Bridge bridge2;

    @BeforeClass
    public static void load()
            throws Exception {
        URL url = ActionModels.class.getResource("debug.xml");
        setup(url, NS);

        bridge2 = Bridge.newInstance("bridge2");

        makeStringId1 = (SimpleTypeName) TypeNameFactory.makeName("makeString",
                VERS, NS);
        makeStringDef1 = (ActionDef) actionModel.getType(makeStringId1);
        actionModel.registerExecutor(makeStringId1, callbackHandler);
        concatId1 = (SimpleTypeName) TypeNameFactory
                .makeName("concat", VERS, NS);
        actionModel.registerExecutor(concatId1, callbackHandler);
    }

    /**
     * Bridge1 reports some demonstrated actions. Bridge2 should see them.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test(timeOut = 10000)
    public void demoVisibility()
            throws Exception {
        Listener listener2 = new Listener();
        bridge2.addActionListener(listener2);

        ActionInvocation action1 = makeStringDef1.invoke(null);
        action1.setStatus(Status.RUNNING);
        action1.setValue(0, "foo");
        long serNum = action1.getSerialNumber();
        ActionStreamEvent action2 = listener2.waitForAction(serNum, 1000);
        Assert.assertEquals(action1.getDefinition(), action2.getDefinition());
        Assert.assertEquals(action1.getStatus(), action2.getStatus());
        action1.setStatus(Status.ENDED);
        action2.waitUntilFinished();
        Assert.assertEquals(action1.getValue(0), action2.getValue(0));
    }

    private static class Listener
            implements GlobalActionListener {
        private final Map<Long, ActionStreamEvent> events;

        public Listener() {
            events = new HashMap<Long, ActionStreamEvent>();
        }

        @Override
        public void actionStarted(ActionStreamEvent action) {
            synchronized(events) {
                events.put(action.getSerialNumber(), action);
                events.notifyAll();
            }
        }

        public ActionStreamEvent waitForAction(long serialNum,
                                               long timeout)
                throws TimeoutException {
            ActionStreamEvent result;
            long endTime = System.currentTimeMillis() + timeout;
            synchronized (events) {
                long timeRemaining = endTime - System.currentTimeMillis();
                while (!events.containsKey(serialNum) && timeRemaining > 0) {
                    try {
                        events.wait(timeRemaining);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    timeRemaining = endTime - System.currentTimeMillis();
                }
                result = events.get(serialNum);
            }
            if (result == null) {
                throw new TimeoutException();
            } else {
                return result;
            }
        }
    }
}
