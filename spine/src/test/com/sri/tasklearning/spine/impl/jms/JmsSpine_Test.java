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
import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jms.JMSException;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.learning.ATRDemonstratedAction;
import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.ai.lumen.atr.learning.impl.ATRDemonstratedActionImpl;
import com.sri.ai.lumen.atr.learning.impl.ATRDemonstrationImpl;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.util.MockClientMessageHandler;
import com.sri.tasklearning.spine.impl.jms.util.SpineTestCase;
import com.sri.tasklearning.spine.messages.BroadcastMessage;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.IgnoredExecutionStatus;
import com.sri.tasklearning.spine.messages.LearnRequest;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.PrivilegedMessageType;
import com.sri.tasklearning.spine.messages.RequestIgnored;
import com.sri.tasklearning.spine.messages.StartExecutionStatus;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.Unsubscribe;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.messages.contents.UID;
import com.sri.tasklearning.spine.util.ATRTestUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JmsSpine_Test extends SpineTestCase {
    private static final Logger log = LoggerFactory.getLogger("JmsSpine_Test");
    private ATRDemonstration demonstration;
    private String adept;
    private String bridge;
    private String otherBridge;
    private String lapdog;
    private String lumen;
    private String shell;
    private SimpleTypeName toLearn;
    private SimpleTypeName actionName;
    private ATRTypeDeclaration spineType;
    private List<Object> inParams;
    private JmsSpine masterJmsSpine;
    private long patience = 500; //milliseconds
    private JmsSpine bridgeSpine;
    private JmsSpine otherBridgeSpine;
    private JmsSpine lumenSpine;
    private JmsSpine lapdogSpine;
    private JmsSpine adeptSpine;
    private JmsSpine editorSpine;

    @BeforeMethod
    public void setUp() {
        adept = "Adept";
        bridge = "Bridge";
        otherBridge = "OtherBridge";
        lapdog = "Lapdog";
        lumen = "Lumen";
        shell = "Shell";
        toLearn = (SimpleTypeName) TypeNameFactory.makeName("LearnToWalk");
        actionName = (SimpleTypeName) TypeNameFactory.makeName("Walk");

        List<ATRDemonstratedAction> actions = new ArrayList<ATRDemonstratedAction>();
        actions.add(new ATRDemonstratedActionImpl(actionName.getFullName()));
        demonstration = new ATRDemonstrationImpl(actions);

        inParams = new ArrayList<Object>();

        SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName("Stepping");
        spineType = ATRTestUtil.makeCustomType(name, String.class);
    }

    @AfterMethod
    public void tearDown() throws Throwable {
        // Dispose of the REMOTE SPines first as they will want to send a 'closing' message to the others
        if (bridgeSpine != null) {
            bridgeSpine.shutdown(true);
        }
        bridgeSpine = null;
        if (otherBridgeSpine != null) {
            otherBridgeSpine.shutdown(true);
        }
        otherBridgeSpine = null;
        if (lumenSpine != null) {
            lumenSpine.shutdown(true);
        }
        lumenSpine = null;
        if (adeptSpine != null) {
            adeptSpine.shutdown(true);
        }
        adeptSpine = null;
        if (editorSpine != null) {
            editorSpine.shutdown(true);
        }
        editorSpine = null;
        if (lapdogSpine != null) {
            lapdogSpine.shutdown(true);
        }
        lapdogSpine = null;
        // Now we can shut down the local that manages the Message Broker
        if (masterJmsSpine != null) {
            masterJmsSpine.shutdown(true);
        }
        masterJmsSpine = null;
    }

    @Test
    public void canConstruct() throws SpineException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);
        assertNotNull(masterJmsSpine);
        assertTrue(JmsSpine.class.isInstance(masterJmsSpine));
    }

    @Test
    public void canTellWhenUpAndRunning() throws SpineException, JMSException, InterruptedException {
        // First lets make sure the REMOTE Spine cant be up unless the LOCAL already is
        boolean isRunning = false;
        int wait = 0;
        try {
            lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
            while (!isRunning && wait < patience) {
                isRunning = lapdogSpine.isRunning();
                Thread.sleep(100);
                wait += 100;
            }
        }
        catch (Exception e) {
            // This is expected behavior - if the Local is not running then the REMOTE
            // Spine constructor should fail and the spine instance will be null
            assertTrue(!isRunning);
        }
        assertTrue(!isRunning);

        // Now lets test the LOCAL Spine isRunning method
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        isRunning = false;
        wait = 0;

        while (!isRunning && wait < patience) {
            isRunning = masterJmsSpine.isRunning();
            Thread.sleep(100);
            wait += 100;
        }
        assertTrue(isRunning);

        // Now lets test the REMOTE Spine isRunning method
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        isRunning = false;
        wait = 0;

        while (!isRunning && wait < patience) {
            isRunning = lumenSpine.isRunning();
            Thread.sleep(100);
            wait += 100;
        }
        assertTrue(isRunning);
    }

    @Test
    public void throwsAnExceptionWhenLocalSpineIsNotReadyYet() throws Exception {
        // Try connect to the LOCAL JmsSpine when it is not yet ready - this should not
        // throw an exception initially as it will keep trying to connect once a second
        // for 20 seconds
        JmsSpine otherSpine = null;
        try {
            otherSpine = new JmsSpine(JmsClient.REMOTE, bridge);
            fail(); // if it didn't throw an exception
        } catch (SpineException e) {
            // Here it will throw an exception but we expected it.
        }

        // Now try again, after the LOCAL is ready, no exception should be thrown in this case
        try {
            masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        } catch (SpineException e) {
            fail("Failed during normal LOCAL spine startup");
        }
        try {
            otherSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        }
        catch (SpineException e) {
            fail("Failed to start normal REMOTE spine");
        }
        otherSpine.shutdown(true);
    }

    @Test
    public void doesNotThrowAnExceptionWhenLocalSpineIsReadyWithinFailureWindow() throws Exception {
        // This will start the LOCAL instance after 5 seconds, that means the REMOTE
        // instance below, the otherSpine, will be in failover mode for 5 seconds and will
        // then be able to connect. This is not considered an exception-able situation.
        Thread spineThread = new Thread(new SpawnLocalSpineInstance());
        spineThread.run();

        // Try connect to the LOCAL JmsSpine when it is not yet ready - this should not
        // throw an exception as long as the connection goes through before the maximum
        // (re)connection attempts are made
        JmsSpine otherSpine = null;
        try {
            otherSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        } catch (SpineException e) {
            fail(); // THere should be no exceptions here.
        }

        otherSpine.shutdown(true);
    }

    private class SpawnLocalSpineInstance implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(5000);
                masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

            } catch (SpineException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    @Test
    public void canStartOnACustomPort() throws SpineException, IOException {
        String prevPort = System.getProperty(JmsSpine.JMS_MESSAGE_BROKER_PORT);
        try {
            String customPort = "8098";
            System.setProperty(JmsSpine.JMS_MESSAGE_BROKER_PORT, customPort);

            ServerSocket serverSocket1 = null;
            ServerSocket serverSocket2;
            try {
                serverSocket1 = new ServerSocket(new Integer(customPort), 1,
                        Inet4Address.getLocalHost());
                serverSocket1.close();
            } catch (IOException e) {
                fail("The port " + customPort
                        + " was not available to start the Spine on");
            }

            masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

            try {
                serverSocket2 = new ServerSocket(new Integer(customPort), 1,
                        Inet4Address.getByName("127.0.0.1"));
                fail("The port " + customPort
                        + " was not assigned to the spine as expected");
                serverSocket2.close();
            } catch (IOException e) {
            }
        } finally {
            System.setProperty(JmsSpine.JMS_MESSAGE_BROKER_PORT, prevPort);
        }
    }

    @Test
    public void canCreateUids() throws SpineException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        JmsSpine lumenJmsSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        TransactionUID uid = masterJmsSpine.getNextUid();
        assertEquals(0, uid.getId());
        assertEquals(shell, uid.getOriginator());

        uid = masterJmsSpine.getNextUid();
        assertEquals(1, uid.getId());
        assertEquals(shell, uid.getOriginator());

        uid = masterJmsSpine.getNextUid();
        assertEquals(2, uid.getId());
        assertEquals(shell, uid.getOriginator());

        uid = lumenJmsSpine.getNextUid();
        assertEquals(0, uid.getId());
        assertEquals(lumen, uid.getOriginator());

        uid = masterJmsSpine.getNextUid();
        assertEquals(3, uid.getId());
        assertEquals(shell, uid.getOriginator());

        uid = lumenJmsSpine.getNextUid();
        assertEquals(1, uid.getId());
        assertEquals(lumen, uid.getOriginator());
    }

    @Test
    public void canGetClientIds() throws Throwable {
        // Simple check to make sure the name set is the one you get back
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, "silly name");
        assertEquals("silly name", masterJmsSpine.getClientId());

        // create another spine with a different name and make the same check
        JmsSpine otherSpine = new JmsSpine(JmsClient.REMOTE, "even sillier name");
        assertEquals("even sillier name", otherSpine.getClientId());

        // Make sure they did not interfere with one another
        assertEquals("silly name", masterJmsSpine.getClientId());
        assertEquals("even sillier name", otherSpine.getClientId());

        otherSpine.shutdown(true);
    }

    @Test (expectedExceptions = SpineException.class)
    public void willThrowAnExceptionWhenSubscribingToNull() throws SpineException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Lapdog message handler and subscribe it a null
        // request topic on the spine
        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(lapdogHandler, (UserMessageType)null);
    }

    @Test (expectedExceptions = SpineException.class)
    public void willThrowAnExceptionWhenSendingUnknownMesageTypes() throws SpineException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);
        masterJmsSpine.send(new NonSenseMessage("noone", new UID("noone", 0)));
    }

    @Test
    public void sendWillReturnFalseIfNoSubscribersPresent() throws SpineException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a new learn request message, this would be created by the bridge
        TransactionUID uid = masterJmsSpine.getNextUid();
        LearnRequest learnRequest = new LearnRequest(bridge, demonstration,
                toLearn, null, null, uid);

        // Send the request into the spine, lapdog should NOT receive it.
        assertFalse(masterJmsSpine.send(learnRequest));
    }

    @Test
    public void canRegisterAndReceiveOneTopicMessage() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Lapdog message handler and subscribe it to the learn
        // request topic on the spine
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.LEARN_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create a new learn request message, this would be created by the bridge
        TransactionUID uid = masterJmsSpine.getNextUid();
        LearnRequest learnRequest = new LearnRequest(bridge, demonstration,
                toLearn, null, null, uid);

        // Send the request into the spine, lapdog should receive it.
        assertTrue(masterJmsSpine.send(learnRequest));

        // Wait for message to propagate and set the history
        int counter = 0;
        while (lapdogHandler.getMessageUidListFromSender(bridge) == null && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Lapdog got the correct message
        assertEquals(uid, lapdogHandler.getMessageUidListFromSender(bridge).get(0));
    }

    @Test
    public void canRegisterAndReceiveOneTopicMessageManyTimesInOrder() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Lapdog message handler and subscribe it to the learn
        // request topic on the spine
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.LEARN_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create new learn request messages, these would be created by the bridge
        for (int i = 0; i < 1000; i++) {
            TransactionUID uid =  masterJmsSpine.getNextUid();
            LearnRequest learnRequest = new LearnRequest(bridge, demonstration,
                    toLearn, null, null, uid);
            // Send the request into the spine, lapdog should receive it.
            assertTrue(masterJmsSpine.send(learnRequest));
        }

        // Sleep to allow all the messages to propagate
        int counter = 0;
        while (lapdogHandler.getMessageUidListFromSender(bridge).size() < 1000 && counter++ < 20) {
            Thread.sleep(300);
        }

        // Make sure Lapdog got the messages in the correct order
        for (int i = 0; i < 1000; i++) {
            UID nextUid = lapdogHandler.getMessageUidListFromSender(bridge).get(i);
            assertEquals(i, nextUid.getId());
            assertEquals(bridge, nextUid.getOriginator());
        }
    }

    @Test(timeOut = 2000)
    public void canRegisterAndReceiveTwoTopicMessagesManyTimesInOrder() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Lapdog message handler and subscribe it to two
        // topics on the spine
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.LEARN_REQUEST);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.TYPE_RESULT);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create the new messages
        for (int i = 0; i < 2000; i++) {
            TransactionUID uid = masterJmsSpine.getNextUid();
            LearnRequest learnRequest = new LearnRequest(bridge, demonstration,
                    toLearn, null, null, uid);
            uid = masterJmsSpine.getNextUid();
            TypeResult typeResult = new TypeResult(bridge, actionName,
                    ATRSyntax.toSource(spineType), uid);
            // Send the messages into the spine, lapdog should receive them.
            assertTrue(masterJmsSpine.send(learnRequest));
            assertTrue(masterJmsSpine.send(typeResult));
        }


        // Sleep to allow all the messages to propagate
        int counter = 0;
        while (lapdogHandler.getMessageUidListFromSender(bridge).size() < 2000) {
            Thread.sleep(10);
        }

        // Make sure Lapdog got the messages is the correct order
        for (int i = 0; i < 2000; i++) {
            UID nextUid = lapdogHandler.getMessageUidListFromSender(bridge).get(i);
            assertEquals(i++, nextUid.getId());
            assertEquals(bridge, nextUid.getOriginator());

            nextUid = lapdogHandler.getMessageUidListFromSender(bridge).get(i);
            assertEquals(i++, nextUid.getId());
            assertEquals(bridge, nextUid.getOriginator());
        }
    }

    @Test(timeOut = 2000)
    public void canSendMessagesToMultipleSubscribers() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Lapdog message handler and subscribe it to two
        // topics on the spine
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.LEARN_REQUEST);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.TYPE_RESULT);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create a mock Lumen message handler and subscribe it to two
        // topics on the spine
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, UserMessageType.LEARN_REQUEST);
        lumenSpine.subscribe(lumenHandler, UserMessageType.TYPE_RESULT);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create the new messages
        for (int i = 0; i < 2000; i++) {
            TransactionUID uid = masterJmsSpine.getNextUid();
            LearnRequest learnRequest = new LearnRequest(bridge, demonstration,
                    toLearn, null, null, uid);
            uid = masterJmsSpine.getNextUid();
            TypeResult typeResult = new TypeResult(bridge, actionName,
                    ATRSyntax.toSource(spineType), uid);
            // Send the messages into the spine, lapdog and lumen both should receive them.
            assertTrue(masterJmsSpine.send(learnRequest));
            assertTrue(masterJmsSpine.send(typeResult));
        }

        // Sleep to allow all the messages to propagate
        int counter = 0;
        while (lapdogHandler.getMessageUidListFromSender(bridge).size() < 2000 ) {
            Thread.sleep(10);
        }

        // Sleep to allow all the messages to propagate
        counter = 0;
        while (lumenHandler.getMessageUidListFromSender(bridge).size() < 2000) {
            Thread.sleep(10);
        }

        // Make sure Lapdog & Lumen got the messages is the correct order
        for (int i = 0; i < 2000;) {
            UID nextUid = lapdogHandler.getMessageUidListFromSender(bridge).get(i);
            assertEquals(i++, nextUid.getId());
            assertEquals(bridge, nextUid.getOriginator());

            nextUid = lapdogHandler.getMessageUidListFromSender(bridge).get(i);
            assertEquals(i++, nextUid.getId());
            assertEquals(bridge, nextUid.getOriginator());

            nextUid = lumenHandler.getMessageUidListFromSender(bridge).get(i - 2);
            assertEquals(i - 2, nextUid.getId());
            assertEquals(bridge, nextUid.getOriginator());

            nextUid = lumenHandler.getMessageUidListFromSender(bridge).get(i - 1);
            assertEquals(i - 1, nextUid.getId());
            assertEquals(bridge, nextUid.getOriginator());
        }
    }

    @Test
    public void canEnsureSpinesAreUnique() throws SpineException {
        // Create master spine which will manage the spine clients
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        // Create the lumen spine as normal
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        // Create a Lumen imposter - this should throw an exception
        try {
            lapdogSpine = new JmsSpine(JmsClient.REMOTE, lumen);
            // Should have thrown an exception, fail.
            fail();
        }
        catch (SpineException e) {

        }
    }

    @Test
    public void canKeepAccurateSpineCount() throws Exception {
        // Create master spine which will manage the spine clients
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        Thread.sleep(100);
        assertEquals(2, masterJmsSpine.getSpineInstanceCount());

        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        Thread.sleep(100);
        assertEquals(3, masterJmsSpine.getSpineInstanceCount());

        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        Thread.sleep(100);
        assertEquals(4, masterJmsSpine.getSpineInstanceCount());

        lumenSpine.shutdown(true);
        Thread.sleep(100);
        assertEquals(3, masterJmsSpine.getSpineInstanceCount());

        lapdogSpine.shutdown(true);
        Thread.sleep(100);
        assertEquals(2, masterJmsSpine.getSpineInstanceCount());

        adeptSpine.shutdown(true);
        Thread.sleep(100);
        assertEquals(1, masterJmsSpine.getSpineInstanceCount());
    }

    /**
     * An example of this is an execution request that for some reason is not accepted
     * for execution by any executor - the requestor would really like to know.
     * @throws com.sri.tasklearning.spine.SpineException if something goes wrong
     * @throws InterruptedException if the sleep is interrupted.
     */
    @Test
    public void canSupportNotificationOfIgnoredExecutionRequest_AcceptedRequest() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lumenSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                    ExecuteRequest executeRequest = (ExecuteRequest) message;
                    // execute request received, lets be nice and accept it.
                    try {
                        StartExecutionStatus startExecutionStatus = new StartExecutionStatus(
                                lumen, executeRequest.getUid(), executeRequest
                                        .getParentUid(),
                                (SimpleTypeName) TypeNameFactory
                                        .makeName("type"), 1l,
                                new ArrayList<Object>());
                        lumenSpine.send(startExecutionStatus);
                    } catch (SpineException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        bridgeSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message instanceof IgnoredExecutionStatus) {
                    fail();
                }
            }
        }, UserMessageType.EXECUTION_STATUS);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        ExecuteRequest executeRequest = new ExecuteRequest(bridge,
                bridgeSpine.getNextUid(), null, actionName, inParams, false);
        bridgeSpine.send(executeRequest);
        // Sleep for a bit to give the Ignored message to propagate, if it is there.
        Thread.sleep(300);
    }

    /**
     * An example of this is an execution request that for some reason is not accepted
     * for execution by any executor - the requestor would really like to know.
     * @throws com.sri.tasklearning.spine.SpineException if something goes wrong
     * @throws InterruptedException if the sleep is interrupted.
     */
    @Test
    public void canSupportNotificationOfIgnoredExecutionRequest_SingleIgnoredRequest() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lumenSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                    ExecuteRequest executeRequest = (ExecuteRequest) message;
                    // execute request received, lets be nasty and ignore it.
                    try {
                        RequestIgnored ignored = new RequestIgnored(lumen, executeRequest.getUid(),
                                executeRequest.getParentUid());
                        lumenSpine.send(ignored);
                    } catch (SpineException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        final boolean[] requestIgnored = new boolean[]{false};

        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        bridgeSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message instanceof IgnoredExecutionStatus) {
                    // Perfect, this is what we expect
                    requestIgnored[0] = true;
                }
                else {
                    fail();
                }
            }
        }, UserMessageType.EXECUTION_STATUS);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        ExecuteRequest executeRequest = new ExecuteRequest(bridge,
                bridgeSpine.getNextUid(), null, actionName, inParams, false);
        bridgeSpine.send(executeRequest);

        // Sleep for a bit to give the Ignored message to propagate, if it is there.
        Thread.sleep(300);
        assertTrue(requestIgnored[0]);
    }

    @Test
    public void canSupportNotificationOfIgnoredExecutionRequest_MultipleIgnoredRequest() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lumenSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                    ExecuteRequest executeRequest = (ExecuteRequest) message;
                    // execute request received, lets be nasty and ignore it.
                    try {
                        RequestIgnored ignored = new RequestIgnored(lumen, executeRequest.getUid(),
                                executeRequest.getParentUid());
                        lumenSpine.send(ignored);
                    } catch (SpineException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        adeptSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                    ExecuteRequest executeRequest = (ExecuteRequest) message;
                    // execute request received, lets be nasty and ignore it.
                    try {
                        RequestIgnored ignored = new RequestIgnored(adept, executeRequest.getUid(),
                                executeRequest.getParentUid());
                        adeptSpine.send(ignored);
                    } catch (SpineException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        otherBridgeSpine = new JmsSpine(JmsClient.REMOTE, otherBridge);
        otherBridgeSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                    ExecuteRequest executeRequest = (ExecuteRequest) message;
                    // execute request received, lets be nasty and ignore it.
                    try {
                        RequestIgnored ignored = new RequestIgnored(otherBridge, executeRequest.getUid(),
                                executeRequest.getParentUid());
                        otherBridgeSpine.send(ignored);
                    } catch (SpineException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        final boolean[] requestIgnored = new boolean[]{false};

        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        bridgeSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message instanceof IgnoredExecutionStatus) {
                    // Perfect, this is what we expect
                    requestIgnored[0] = true;
                }
                else {
                    fail();
                }
            }
        }, UserMessageType.EXECUTION_STATUS);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        ExecuteRequest executeRequest = new ExecuteRequest(bridge,
                bridgeSpine.getNextUid(), null, actionName, inParams, false);
        bridgeSpine.send(executeRequest);

        // Sleep for a bit to give the Ignored message to propagate, if it is there.
        Thread.sleep(300);
        assertTrue(requestIgnored[0]);
    }

    @Test
    public void canSupportNotificationOfIgnoredExecutionRequest_OneAcceptedRequest() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lumenSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                    ExecuteRequest executeRequest = (ExecuteRequest) message;
                    // execute request received, lets be nasty and ignore it.
                    try {
                        RequestIgnored ignored = new RequestIgnored(lumen, executeRequest.getUid(),
                                executeRequest.getParentUid());
                        lumenSpine.send(ignored);
                    } catch (SpineException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        adeptSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                    ExecuteRequest executeRequest = (ExecuteRequest) message;
                    // execute request received, lets be nasty and ignore it.
                    try {
                        RequestIgnored ignored = new RequestIgnored(adept, executeRequest.getUid(),
                                executeRequest.getParentUid());
                        adeptSpine.send(ignored);
                    } catch (SpineException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        otherBridgeSpine = new JmsSpine(JmsClient.REMOTE, otherBridge);
        otherBridgeSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                    ExecuteRequest executeRequest = (ExecuteRequest) message;
                    // execute request received, lets be nice and accept it.
                    try {
                        StartExecutionStatus startExecutionStatus = new StartExecutionStatus(
                                lumen, executeRequest.getUid(), executeRequest
                                        .getParentUid(),
                                (SimpleTypeName) TypeNameFactory
                                        .makeName("type"), 1l,
                                new ArrayList<Object>());
                        otherBridgeSpine.send(startExecutionStatus);
                    } catch (SpineException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        final boolean[] requestIgnored = new boolean[]{false};

        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        bridgeSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message instanceof IgnoredExecutionStatus) {
                    fail();
                    // No, this is not what we expect
                    requestIgnored[0] = true;
                }
            }
        }, UserMessageType.EXECUTION_STATUS);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        ExecuteRequest executeRequest = new ExecuteRequest(bridge,
                bridgeSpine.getNextUid(), null, actionName, inParams, false);
        bridgeSpine.send(executeRequest);

        // Sleep for a bit to give the Ignored message to propagate, if it is there.
        Thread.sleep(300);
        assertFalse(requestIgnored[0]);
    }

    @Test
    public void canUnSubscribeFromAMessageType() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lumenSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                }
            }
        }, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        final boolean[] unsubscribeReceived = new boolean[]{false};
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        lapdogSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (Unsubscribe.class.isInstance(message)) {
                    Unsubscribe unSubMessage = (Unsubscribe)message;
                    assertEquals(lumen, unSubMessage.getUnSubscriber());
                    assertEquals(UserMessageType.EXECUTE_REQUEST, unSubMessage.getUnSubscriptionType());
                    unsubscribeReceived[0] = true;
                }
            }
        }, SystemMessageType.UNSUBSCRIBE);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        assertTrue(lumenSpine.unsubscribe(UserMessageType.EXECUTE_REQUEST));

        Thread.sleep(100);

        assertTrue(unsubscribeReceived[0]);
    }

    @Test
    public void canUnSubscribeFromASystemMessageType() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lumenSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                }
            }
        }, SystemMessageType.START_WATCHING);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        final boolean[] unsubscribeReceived = new boolean[]{false};
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        lapdogSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (Unsubscribe.class.isInstance(message)) {
                    Unsubscribe unSubMessage = (Unsubscribe)message;
                    assertEquals(lumen, unSubMessage.getUnSubscriber());
                    assertEquals( SystemMessageType.START_WATCHING, unSubMessage.getUnSubscriptionType());
                    unsubscribeReceived[0] = true;
                }
            }
        }, SystemMessageType.UNSUBSCRIBE);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        assertTrue(lumenSpine.unsubscribe(SystemMessageType.START_WATCHING));

        Thread.sleep(100);

        // The unsubscribe message will not be sent for system messages
        assertTrue(!unsubscribeReceived[0]);
    }

    @Test
    public void canUnSubscribeFromAPrivilegedMessageType() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lumenSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                }
            }
        }, PrivilegedMessageType.TYPE_STORE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        final boolean[] unsubscribeReceived = new boolean[]{false};
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        lapdogSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (Unsubscribe.class.isInstance(message)) {
                    Unsubscribe unSubMessage = (Unsubscribe)message;
                    assertEquals(lumen, unSubMessage.getUnSubscriber());
                    assertEquals(PrivilegedMessageType.TYPE_STORE_REQUEST, unSubMessage.getUnSubscriptionType());
                    unsubscribeReceived[0] = true;
                }
            }
        }, SystemMessageType.UNSUBSCRIBE);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        assertTrue(lumenSpine.unsubscribe(PrivilegedMessageType.TYPE_STORE_REQUEST));

        Thread.sleep(100);

        assertTrue(unsubscribeReceived[0]);
    }

    @Test
    public void canRejectUnsubscribeFromTopicThatWasNotSubscribedTo() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lumenSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                }
            }
        }, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        final boolean[] unsubscribeReceived = new boolean[]{false};
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        lapdogSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (Unsubscribe.class.isInstance(message)) {
                    Unsubscribe unSubMessage = (Unsubscribe)message;
                    assertEquals(lumen, unSubMessage.getUnSubscriber());
                    assertEquals(UserMessageType.EXECUTE_REQUEST, unSubMessage.getUnSubscriptionType());
                    unsubscribeReceived[0] = true;
                }
            }
        }, SystemMessageType.UNSUBSCRIBE);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        assertFalse(lumenSpine.unsubscribe(UserMessageType.LEARN_REQUEST));

        Thread.sleep(100);

        assertFalse(unsubscribeReceived[0]);
    }

    @Test
    public void willNotProcessMessagesOnUnsubscribedTopic() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        final boolean[] executeRequestReceived = new boolean[]{false};
        lumenSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                    executeRequestReceived[0] = true;
                }
            }
        }, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        assertTrue(lumenSpine.unsubscribe(UserMessageType.EXECUTE_REQUEST));

        masterJmsSpine.send(new ExecuteRequest("CPOF", masterJmsSpine
                .getNextUid(), null, actionName, inParams, false));

        // Allow messages to propagate
        Thread.sleep(100);

        assertFalse(executeRequestReceived[0]);
    }

    @Test
    //TODO: Make sure this happens for shutdown/finalize also
    public void priviligedOwnershipIsRelinquishedWhenUnSubscribed() {

    }

    @Test
    // TODO: Currently the messageTypeToConsumersMap in the spine only remembers the last consumer - there can be many
    public void ensureMultipleListenersOnOneSpineMessageTypeAreAllUnsubscribed() {

    }

    @Test
    public void canDetermineSpineRunning() throws SpineException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, "shell");

        try {
            lumenSpine = new JmsSpine(JmsClient.REMOTE, "Lumen");
            log.debug("Spine running");
        } catch (SpineException e) {
            log.debug("Spine not running");
            fail();
        }

    }

    @Test
    public void canDetermineSpineNotRunning() throws SpineException {
        // First set the reconnect attempts to 1 to avoid a long delay in getting the
        // rejection from the master spine
        Properties systemProperties = System.getProperties();
        String customRetry = "1";
        systemProperties.put("PAL.MaxReconnectAttempts", customRetry);
        System.setProperties(systemProperties);

        try {
            lumenSpine = new JmsSpine(JmsClient.REMOTE, "Lumen");
            log.debug("Spine running");
            fail();
        } catch (SpineException e) {
            log.debug("Spine not running");
        }
        // Remove the custom port property so we don't break other tests.
        systemProperties = System.getProperties();
        systemProperties.remove("PAL.MaxReconnectAttempts");
        System.setProperties(systemProperties);
    }

    /////////////////////////
    // Private Helper classes
    /////////////////////////

    private class NonSenseMessage extends BroadcastMessage {
        private static final long serialVersionUID = 1L;

        public NonSenseMessage(String sender, UID uid) {
            super(sender, uid, null);
        }
    }

}
