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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.util.MockClientMessageHandler;
import com.sri.tasklearning.spine.impl.jms.util.SpineTestCase;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.IgnoredExecutionStatus;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.RequestIgnored;
import com.sri.tasklearning.spine.messages.StartExecutionStatus;
import com.sri.tasklearning.spine.messages.SuccessExecutionStatus;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ExecutionAcceptanceWatcher_Test extends SpineTestCase {
    private String shell = "Shell";
    private String bridge = "Bridge";
    private String lumen = "Lumen";
    private String otherBridge = "OtherBridge";
    private JmsSpine masterSpine;
    private JmsSpine bridgeSpine;
    private JmsSpine otherBridgeSpine;
    private JmsSpine lumenSpine;
    private SimpleTypeName actionName;
    private List<Object> inParams;
    private List<Object> outParams;
    private ErrorInfo errorInfo;

    @BeforeMethod
    public void setUp() {
        actionName = (SimpleTypeName) TypeNameFactory.makeName("type");
        inParams = new ArrayList<Object>();
        outParams = new ArrayList<Object>();
        errorInfo = new ErrorInfo("error", -1, "error", "error", new ArrayList<ErrorInfo.PALStackFrame>());
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (bridgeSpine != null) {
            bridgeSpine.shutdown(true);
        }
        if (otherBridgeSpine != null) {
            otherBridgeSpine.shutdown(true);
        }
        if (lumenSpine != null) {
            lumenSpine.shutdown(true);
        }
        if (masterSpine != null) {
            masterSpine.shutdown(true);
        }
    }

    @Test
    public void canCreateInstance() throws SpineException {
        masterSpine = new JmsSpine(JmsClient.LOCAL, shell);
        ExecutionAcceptanceWatcher executionWatcher = new ExecutionAcceptanceWatcher(masterSpine);
        assertNotNull(executionWatcher);
        assertTrue(ExecutionAcceptanceWatcher.class.isInstance(executionWatcher));
    }

    @Test
    public void canAcceptExecutionRequestToMonitor() throws SpineException, MessageHandlerException {
        masterSpine = new JmsSpine(JmsClient.LOCAL, shell);
        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        TransactionUID uid = bridgeSpine.getNextUid();

        ExecuteRequest executeRequest = new ExecuteRequest(bridge, uid, null,
                actionName, inParams, false);

        ExecutionAcceptanceWatcher executionWatcher = new ExecutionAcceptanceWatcher(masterSpine);
        executionWatcher.addWatch(executeRequest);

        assertEquals(1, executionWatcher.watchCount());
    }

    @Test
    public void willClearExecutionWatch_ForStartExecution() throws SpineException, MessageHandlerException {
        masterSpine = new JmsSpine(JmsClient.LOCAL, shell);
        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        TransactionUID uid = bridgeSpine.getNextUid();

        ExecuteRequest executeRequest = new ExecuteRequest(bridge, uid, null,
                actionName, inParams, false);

        ExecutionAcceptanceWatcher executionWatcher = new ExecutionAcceptanceWatcher(masterSpine);
        executionWatcher.addWatch(executeRequest);

        assertEquals(1, executionWatcher.watchCount());

        StartExecutionStatus start = new StartExecutionStatus(lumen, uid, null, actionName, 1l, inParams);
        executionWatcher.handleMessage(start);

        assertEquals(0, executionWatcher.watchCount());
    }

    @Test
    public void willClearExecutionWatch_ForSuccessExecution() throws SpineException, MessageHandlerException {
        masterSpine = new JmsSpine(JmsClient.LOCAL, shell);
        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        TransactionUID uid = bridgeSpine.getNextUid();

        ExecuteRequest executeRequest = new ExecuteRequest(bridge, uid, null,
                actionName, inParams, false);

        ExecutionAcceptanceWatcher executionWatcher = new ExecutionAcceptanceWatcher(masterSpine);
        executionWatcher.addWatch(executeRequest);

        assertEquals(1, executionWatcher.watchCount());

        SuccessExecutionStatus successExecutionStatus = new SuccessExecutionStatus(
                lumen, uid, null, inParams, outParams);
        executionWatcher.handleMessage(successExecutionStatus);

        assertEquals(0, executionWatcher.watchCount());
    }

    @Test
    public void willClearExecutionWatch_ForErrorExecution() throws SpineException, MessageHandlerException {
        masterSpine = new JmsSpine(JmsClient.LOCAL, shell);
        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        TransactionUID uid = bridgeSpine.getNextUid();

        ExecuteRequest executeRequest = new ExecuteRequest(bridge, uid, null,
                actionName, inParams, false);

        ExecutionAcceptanceWatcher executionWatcher = new ExecutionAcceptanceWatcher(masterSpine);
        executionWatcher.addWatch(executeRequest);

        assertEquals(1, executionWatcher.watchCount());

        ErrorExecutionStatus error = new ErrorExecutionStatus(lumen, uid, null, errorInfo);
        executionWatcher.handleMessage(error);

        assertEquals(0, executionWatcher.watchCount());
    }

    @Test
    public void willIssueIgnoredExecutionStatus_ForOneSubscriber() throws SpineException, MessageHandlerException, InterruptedException {
        masterSpine = new JmsSpine(JmsClient.LOCAL, shell);

        final boolean[] informedRequestIgnored = new boolean[]{false};
        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        bridgeSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message instanceof IgnoredExecutionStatus) {
                    informedRequestIgnored[0] = true;
                }
            }
        }, UserMessageType.EXECUTION_STATUS);

        // Allow subscriptions to propagate.
        Thread.sleep(100);

        TransactionUID uid = bridgeSpine.getNextUid();

        ExecuteRequest executeRequest = new ExecuteRequest(bridge, uid, null,
                actionName, inParams, false);

        ExecutionAcceptanceWatcher executionWatcher = new ExecutionAcceptanceWatcher(masterSpine);
        executionWatcher.addWatch(executeRequest);

        assertEquals(1, executionWatcher.watchCount());

        RequestIgnored requestIgnored = new RequestIgnored(lumen, uid, null);
        executionWatcher.handleMessage(requestIgnored);

        assertEquals(0, executionWatcher.watchCount());
        // Sleep for a tad to allow any ignore messages to propagate
        Thread.sleep(500);

        assertTrue(informedRequestIgnored[0]);
    }

    @Test
    public void willNotIssueIgnoredExecutionStatus_ForMultipleSubscribers_WithOneAccepting() throws SpineException, MessageHandlerException, InterruptedException {
        masterSpine = new JmsSpine(JmsClient.LOCAL, shell);
        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);

        // Set up the executors
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lumenSpine.subscribe(new MockClientMessageHandler(lumenSpine, false), UserMessageType.EXECUTE_REQUEST);
        otherBridgeSpine = new JmsSpine(JmsClient.REMOTE, otherBridge);
        otherBridgeSpine.subscribe(new MockClientMessageHandler(otherBridgeSpine, false), UserMessageType.EXECUTE_REQUEST);

        // Allow subscription notifications to be delivered.
        Thread.sleep(100);

        final boolean[] informedRequestIgnored = new boolean[]{false};
        bridgeSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message instanceof IgnoredExecutionStatus) {
                    informedRequestIgnored[0] = true;
                }
            }
        }, UserMessageType.EXECUTION_STATUS);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        TransactionUID uid = bridgeSpine.getNextUid();
        ExecuteRequest executeRequest = new ExecuteRequest(bridge, uid, null,
                actionName, inParams, false);

        ExecutionAcceptanceWatcher executionWatcher = new ExecutionAcceptanceWatcher(masterSpine);
        executionWatcher.addWatch(executeRequest);
        assertEquals(1, executionWatcher.watchCount());

        // Lumen Ignores the Request
        RequestIgnored requestIgnored = new RequestIgnored(lumen, uid, null);
        executionWatcher.handleMessage(requestIgnored);

        // Make sure watcher didn't ditch the watch , still need to hear from OtherBridge
        assertEquals(1, executionWatcher.watchCount());
        // Sleep for a tad to allow any ignore messages to propagate
        Thread.sleep(500);
        assertFalse(informedRequestIgnored[0]);

        // Other Bridge Accepts the Execute Request, now we can pull the watch
        StartExecutionStatus start = new StartExecutionStatus(otherBridge, uid, null, actionName, 1l, inParams);
        executionWatcher.handleMessage(start);
        assertEquals(0, executionWatcher.watchCount());
        // Sleep for a tad to allow any ignore messages to propagate
        Thread.sleep(500);
        assertFalse(informedRequestIgnored[0]);
    }

    public void willIssueIgnoredExecutionStatus_ForMultipleSubscribers_WhenAllIgnore() throws SpineException, MessageHandlerException, InterruptedException {
        masterSpine = new JmsSpine(JmsClient.LOCAL, shell);
        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);

        // Set up the executors
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lumenSpine.subscribe(new MockClientMessageHandler(lumenSpine, false), UserMessageType.EXECUTE_REQUEST);
        otherBridgeSpine = new JmsSpine(JmsClient.REMOTE, otherBridge);
        otherBridgeSpine.subscribe(new MockClientMessageHandler(otherBridgeSpine, false), UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        final boolean[] informedRequestIgnored = new boolean[]{false};
        bridgeSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message instanceof IgnoredExecutionStatus) {
                    informedRequestIgnored[0] = true;
                }
            }
        }, UserMessageType.EXECUTION_STATUS);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        TransactionUID uid = bridgeSpine.getNextUid();
        ExecuteRequest executeRequest = new ExecuteRequest(bridge, uid, null,
                actionName, inParams, false);

        ExecutionAcceptanceWatcher executionWatcher = new ExecutionAcceptanceWatcher(masterSpine);
        executionWatcher.addWatch(executeRequest);
        assertEquals(1, executionWatcher.watchCount());

        // Lumen Ignores the Request
        RequestIgnored requestIgnored = new RequestIgnored(lumen, uid, null);
        executionWatcher.handleMessage(requestIgnored);

        // Make sure watcher didn't ditch the watch, still need to hear from OtherBridge
        assertEquals(1, executionWatcher.watchCount());
        // Sleep for a tad to allow any ignore messages to propagate
        Thread.sleep(500);
        assertFalse(informedRequestIgnored[0]);

        // Other Bridge Ignores the Execute Request as well, now we can pull the watch and
        // it should issue an IgnoredExecutionStatus
        requestIgnored = new RequestIgnored(otherBridge, uid, null);
        executionWatcher.handleMessage(requestIgnored);
        assertEquals(0, executionWatcher.watchCount());

        // Sleep for a tad to allow any ignore messages to propagate
        Thread.sleep(500);
        assertTrue(informedRequestIgnored[0]);
    }

}
