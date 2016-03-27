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
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.learning.ATRDemonstratedAction;
import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.ai.lumen.atr.learning.impl.ATRDemonstratedActionImpl;
import com.sri.ai.lumen.atr.learning.impl.ATRDemonstrationImpl;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.util.BridgeJmsSpine;
import com.sri.tasklearning.spine.impl.jms.util.DelayedLocalJmsSpine;
import com.sri.tasklearning.spine.impl.jms.util.MockClientMessageHandler;
import com.sri.tasklearning.spine.impl.jms.util.SpineTestCase;
import com.sri.tasklearning.spine.impl.jms.util.StreamGobbler;
import com.sri.tasklearning.spine.messages.CancelRequest;
import com.sri.tasklearning.spine.messages.CustomBridgeMessage;
import com.sri.tasklearning.spine.messages.CustomEditorMessage;
import com.sri.tasklearning.spine.messages.CustomLapdogMessage;
import com.sri.tasklearning.spine.messages.CustomLumenMessage;
import com.sri.tasklearning.spine.messages.CustomShellMessage;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.LearnRequest;
import com.sri.tasklearning.spine.messages.LearnResult;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.PrivilegedMessageType;
import com.sri.tasklearning.spine.messages.RequestStartWatching;
import com.sri.tasklearning.spine.messages.RequestStopWatching;
import com.sri.tasklearning.spine.messages.SerialNumberRequest;
import com.sri.tasklearning.spine.messages.StartExecutionStatus;
import com.sri.tasklearning.spine.messages.StartWatching;
import com.sri.tasklearning.spine.messages.StopWatching;
import com.sri.tasklearning.spine.messages.SuccessExecutionStatus;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.messages.TypeQuery;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.TypeStoreRequest;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.messages.contents.UID;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JmsSpine_FuncTest extends SpineTestCase {
    private static final Logger log = LoggerFactory.getLogger("JmsSpine_FuncTest");
    private ATRDemonstration demonstration;
    private String adept;
    private String bridge;
    private String lapdog;
    private String lumen;
    private String shell;
    private String editor;
    private SimpleTypeName toLearn;
    private SimpleTypeName actionName;
    private ATRTypeDeclaration spineType;
    private String source;
    private List<Object> inParams;
    private List<Object> outParams;
    private JmsSpine masterJmsSpine;
    private long patience = 500; //milliseconds
    private JmsSpine lumenSpine;
    private JmsSpine lapdogSpine;
    private JmsSpine adeptSpine;
    private JmsSpine editorSpine;

    @BeforeMethod
    public void setUp() {
        adept = "Adept";
        bridge = "Bridge";
        lapdog = "Lapdog";
        lumen = "Lumen";
        shell = "Shell";
        editor = "Editor";
        toLearn = (SimpleTypeName) TypeNameFactory.makeName("LearnToWalk");
        actionName = (SimpleTypeName) TypeNameFactory.makeName("Walk");

        List<ATRDemonstratedAction> actions = new ArrayList<ATRDemonstratedAction>();
        actions.add(new ATRDemonstratedActionImpl(actionName.getFullName()));
        demonstration = new ATRDemonstrationImpl(actions);

        inParams = new ArrayList<Object>();
        outParams = new ArrayList<Object>();

        SimpleTypeName name = (SimpleTypeName) TypeNameFactory.makeName("Stepping");
        spineType = ATRTestUtil.makeCustomType(name, String.class);

        source = "";
    }

    @AfterMethod
    public void tearDown() throws Throwable {
        // Dispose of the REMOTE SPines first as they will want to send a 'closing' message to the others
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

        // Rapid setup and shutdown of the JMS socket 61616 sometimes does not work -
        // this results in an occasional BIND failure - this delay allows the system
        // time to return the port after a test completes and the master spine is closed
        Thread.sleep(1000);
    }

    /**
     * This test is for the request that the Bridge submits on behalf
     * of cpof to get Lapdog to learn a procedure
     * @throws com.sri.tasklearning.spine.SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void learnRequestScenario() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Lapdog message handler and subscribe it to the
        // learn request topic
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.LEARN_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue the learn request from the bridge into the spine
        TransactionUID uid = masterJmsSpine.getNextUid();
        LearnRequest learnRequest = new LearnRequest(bridge, demonstration,
               toLearn, null, null, uid);
        assertTrue(masterJmsSpine.send(learnRequest));

        // Sleep to allow all the messages to propagate
        int counter = 0;
        while (lapdogHandler.getMessageUidListFromSender(bridge) == null && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Lapdog got the message
        UID nextUid = lapdogHandler.getMessageUidListFromSender(bridge).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(bridge, nextUid.getOriginator());
    }

    /**
     * This test is for the request that the Lapdog submits to Bridge
     * as the result of a learn request
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void learnResultScenario() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Bridge message handler and subscribe it to the
        // learn result topic
        MockClientMessageHandler bridgeHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(bridgeHandler, UserMessageType.LEARN_RESULT);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue the learn result from Lapdog into the spine
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        TransactionUID uid = lapdogSpine.getNextUid();
        LearnResult learnResult = new LearnResult(lapdog, uid, source);
        assertTrue(lapdogSpine.send(learnResult));

        // Sleep to allow all the messages to propagate
        int counter = 0;
        while (bridgeHandler.getMessageUidListFromSender(lapdog) == null && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Bridge got the message
        UID nextUid = bridgeHandler.getMessageUidListFromSender(lapdog).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(lapdog, nextUid.getOriginator());
    }

    /**
     * This test is for the request that Lumen and Bridge send to each
     * other to invoke task execution
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void executeRequestScenario() throws Throwable {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Lumen message handler and subscribe it to the
        // execute request topic
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(1000);

        // Create and issue the execute request from bridge into the spine,
        // the bridgeActionInstance holds all the UID and sender information.
        TransactionUID uid = masterJmsSpine.getNextUid();
        ExecuteRequest bridgeExecuteRequest = new ExecuteRequest(bridge, uid,
                null, (SimpleTypeName) TypeNameFactory.makeName("run"),
                inParams, false);
        assertTrue(masterJmsSpine.send(bridgeExecuteRequest));

        // Sleep to allow all the messages to propagate
        int counter = 0;
        while (lumenHandler.getMessageUidListFromSender(bridge) == null && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Lumen got the message
        UID nextUid = lumenHandler.getMessageUidListFromSender(bridge).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(bridge, nextUid.getOriginator());

        // Now try the reverse, sending an execution request from Lumen to Bridge

        // Create a mock Bridge message handler and subscribe it to the
        // execute request topic
        MockClientMessageHandler bridgeHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(bridgeHandler, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue the execute request from lumen into the spine,
        uid = lumenSpine.getNextUid();
        ExecuteRequest lumenExecuteRequest = new ExecuteRequest(lumen, uid,
                null, (SimpleTypeName) TypeNameFactory.makeName("walk"),
                inParams, false);
        assertTrue(lumenSpine.send(lumenExecuteRequest));

        // Sleep to allow all the messages to propagate
        counter = 0;
        while (bridgeHandler.getMessageUidListFromSender(lumen) == null && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Bridge got the message
        nextUid = bridgeHandler.getMessageUidListFromSender(lumen).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(lumen, nextUid.getOriginator());
    }

    /**
     * This test is for the execution status updates that Lumen and Bridge
     * send to each other to inform them about how execution is going
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void executionStatusScenarioLumenToBridge() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        // Create a mock Bridge message handler and subscribe it to the
        // execution status topic
        MockClientMessageHandler bridgeHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(bridgeHandler, UserMessageType.EXECUTION_STATUS);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue the execution status messages from lumen into the spine:
        // 1. Lumen starts a task and finishes with success
        // Note that the TransactionUID being used for status updates is the same as the
        // TransactionUID used by the bridge to request the execution. This allows the
        // Bridge to relate these status messages to a particular execution request.
        TransactionUID uid = new TransactionUID(bridge, 0);
        StartExecutionStatus startStatus = new StartExecutionStatus(lumen, uid,
                null, actionName, 0, inParams);
        assertTrue(lumenSpine.send(startStatus));
        SuccessExecutionStatus successStatus = new SuccessExecutionStatus(lumen, uid, null, inParams, outParams);
        assertTrue(lumenSpine.send(successStatus));

        // 2. Lumen Starts a task and reports back an error with the execution
        uid = new TransactionUID(bridge, 1);
        startStatus = new StartExecutionStatus(lumen, uid, null, actionName, 0, inParams);
        assertTrue(lumenSpine.send(startStatus));
        ErrorInfo error = ErrorFactory.error("test", ErrorType.CANCEL, "foo");
        ErrorExecutionStatus errorStatus = new ErrorExecutionStatus(lumen, uid, null, error);
        assertTrue(lumenSpine.send(errorStatus));

        // 3. Lumen starts a task and someone (the evil cpof?) cancels the execution
        // and lumen reports back the task as cancelled
        uid = new TransactionUID(bridge, 2);
        startStatus = new StartExecutionStatus(lumen, uid, null, actionName, 0, inParams);
        assertTrue(lumenSpine.send(startStatus));
        ErrorInfo cancelError = new ErrorInfo(lumen,
                ErrorType.CANCEL.ordinal(), "cancel", "cancel", null);
        ErrorExecutionStatus cancelStatus = new ErrorExecutionStatus(lumen, uid, null, cancelError);
        assertTrue(lumenSpine.send(cancelStatus));

        // Sleep to allow all the messages to propagate
        int counter = 0;
        while ((bridgeHandler.getMessageUidListFromSender(lumen) == null) ||
               (bridgeHandler.getMessageUidListFromSender(lumen).size() < 6 && counter++ < 10)) {
            Thread.sleep(5);
        }

        // Make sure Bridge got the messages
        UID nextUid = bridgeHandler.getMessageUidListFromSender(lumen).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(bridge, nextUid.getOriginator());
        // The uid of the second message should be the same as the first
        // as it is referring to the status of that same execution
        nextUid = bridgeHandler.getMessageUidListFromSender(lumen).get(1);
        assertEquals(0, nextUid.getId());
        assertEquals(bridge, nextUid.getOriginator());

        nextUid = bridgeHandler.getMessageUidListFromSender(lumen).get(2);
        assertEquals(1, nextUid.getId());
        assertEquals(bridge, nextUid.getOriginator());
        nextUid = bridgeHandler.getMessageUidListFromSender(lumen).get(3);
        assertEquals(1, nextUid.getId());
        assertEquals(bridge, nextUid.getOriginator());

        nextUid = bridgeHandler.getMessageUidListFromSender(lumen).get(4);
        assertEquals(2, nextUid.getId());
        assertEquals(bridge, nextUid.getOriginator());
        nextUid = bridgeHandler.getMessageUidListFromSender(lumen).get(5);
        assertEquals(2, nextUid.getId());
        assertEquals(bridge, nextUid.getOriginator());
    }

    /**
     * This test is for the request that Lumen and Bridge send to each
     * other to cancel task execution
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void cancelExecutionScenario() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Lumen message handler and subscribe it to the
        // cancel request topic
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, UserMessageType.CANCEL);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Allow subscription to propagate
        Thread.sleep(1000);

        // Create and issue the cancel request from bridge into the spine,
        TransactionUID uid = masterJmsSpine.getNextUid();
        CancelRequest cancelRequest = new CancelRequest(bridge, uid);
        assertTrue(masterJmsSpine.send(cancelRequest));

        // Sleep to allow all the message to propagate
        int counter = 0;
        while (lumenHandler.getMessageUidListFromSender(bridge) == null && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Lumen got the message
        UID nextUid = lumenHandler.getMessageUidListFromSender(bridge).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(bridge, nextUid.getOriginator());

        // Now try the reverse, sending the cancel request from Lumen to Bridge

        // Create a mock Bridge message handler and subscribe it to the
        // execute request topic
        MockClientMessageHandler bridgeHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(bridgeHandler, UserMessageType.CANCEL);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue the cancel request from lumen into the spine,
        uid = new TransactionUID(lumen, 0);
        CancelRequest lumenCancelRequest = new CancelRequest(lumen, uid);
        assertTrue(masterJmsSpine.send(lumenCancelRequest));

        // Sleep to allow all the messages to propagate
        counter = 0;
        while (bridgeHandler.getMessageUidListFromSender(lumen) == null && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Bridge got the message
        nextUid = bridgeHandler.getMessageUidListFromSender(lumen).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(lumen, nextUid.getOriginator());
    }

    /**
     * This test is for the request that Lapdog or Lumen send to the
     * Bridge to ask for a types definition
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void typeQueryScenario() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        // Create a mock Bridge message handler and subscribe it to the
        // Type Query topic
        MockClientMessageHandler bridgeHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(bridgeHandler, UserMessageType.TYPE_QUERY);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue the type query from lumen into the spine,
        TransactionUID uid = lumenSpine.getNextUid();
        TypeQuery typeQuery = new TypeQuery(lumen, actionName, uid);
        assertTrue(lumenSpine.send(typeQuery));

        // Sleep to allow all the message to propagate
        int counter = 0;
        while (bridgeHandler.getMessageUidListFromSender(lumen) == null && counter++ < 100) {
            Thread.sleep(50);
        }

        // Make sure Bridge got the message
        UID nextUid = bridgeHandler.getMessageUidListFromSender(lumen).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(lumen, nextUid.getOriginator());
    }

    /**
     * This test is for the result that Bridge sends to Lapdog
     * or Lumen in response to a TypeQuery
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void typeResultScenario() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Lapdog message handler and subscribe it to the
        // Type Result topic
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.TYPE_RESULT);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue the type result from Bridge into the spine,
        TransactionUID uid = masterJmsSpine.getNextUid();
        TypeResult typeResult = new TypeResult(bridge, actionName,
                ATRSyntax.toSource(spineType), uid);
        assertTrue(masterJmsSpine.send(typeResult));

        // Sleep to allow all the message to propagate
        int counter = 0;
        while (lapdogHandler.getMessageUidListFromSender(bridge) == null && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Lapdog got the message
        UID nextUid = lapdogHandler.getMessageUidListFromSender(bridge).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(bridge, nextUid.getOriginator());
    }

    /**
     * This test is for the result that Bridge sends to Lapdog
     * or Lumen in response to a TypeQuery. In this test we want to make sure
     * that the Bridge can communicate the fact that it does not have any types
     * to return. Prior to the code change this call would have hung.
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void noTypeFoundTypeResultScenario() throws Exception {
        final boolean[] success = new boolean[]{false};

        // Create message Broker
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        // Create Bridge Spine & listen for type query
        final JmsSpine bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        MessageHandler typeRequestHandler = new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message.getMessageType().equals(UserMessageType.TYPE_QUERY)) {
                    // Lets pretend we can't find the type and return that status by using null
                    // for the spineType.
                    TypeResult typeResult = new TypeResult(bridge, actionName, null,
                                            (TransactionUID) message.getUid());
                    try {
                        bridgeSpine.send(typeResult);
                    } catch (SpineException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        bridgeSpine.subscribe(typeRequestHandler, UserMessageType.TYPE_QUERY);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create Lapdog Spine and listen for type result
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        MessageHandler typeResultHandler = new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message.getMessageType().equals(UserMessageType.TYPE_RESULT)) {
                    TypeResult typeResult = (TypeResult) message;
                    if (typeResult.getTypeStr() == null) {
                        // expected behavior, we asked for type that was not present.
                        success[0] = true;
                    }
                    else {
                        fail();
                    }
                }
            }
        };
        lapdogSpine.subscribe(typeResultHandler, UserMessageType.TYPE_RESULT);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue the TypeQuery from Lapdog into the spine,
        TransactionUID lapdogUid = lapdogSpine.getNextUid();
        TypeQuery typeQuery = new TypeQuery(lapdog, actionName, lapdogUid);

        assertTrue(lapdogSpine.send(typeQuery));

        // Now wait a few seconds to see if the right thing happens in the inner classes
        // above.
        int tries = 10;
        int attempts = 0;
        while(!success[0] && attempts++ < tries) {
            Thread.sleep(100);
        }
        assertTrue(success[0]);
        bridgeSpine.shutdown(true);
        lapdogSpine.shutdown(true);
    }

    /**
     * This test is for the startWatching request that a spine client will send to the
     * spine to indicate that instrumentation is to be observer.
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void basicStartWatchingScenario() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        MockClientMessageHandler adeptHandler = new MockClientMessageHandler(adeptSpine, false);
        adeptSpine.subscribe(adeptHandler, SystemMessageType.START_WATCHING);
        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, SystemMessageType.START_WATCHING);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        TransactionUID uid = adeptSpine.getNextUid();
        RequestStartWatching requestStartWatching = new RequestStartWatching(adept, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(adeptSpine.send(requestStartWatching));

        // Sleep to allow all the message to propagate
        int counter = 0;
        while (adeptHandler.getMessageUidListFromSender(shell) == null && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Adept and Lumen got the StartWatchingResponse message
        UID nextUid = adeptHandler.getMessageUidListFromSender(shell).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(adept, nextUid.getOriginator());

        counter = 0;
        while (lumenHandler.getMessageUidListFromSender(shell) == null && counter++ < 10) {
            Thread.sleep(5);
        }
        nextUid = lumenHandler.getMessageUidListFromSender(shell).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(adept, nextUid.getOriginator());

        adeptSpine.shutdown(true);
        lumenSpine.shutdown(true);
    }

    /**
     * This test is for the stopWatching request that a spine client will send to the
     * spine to indicate that instrumentation is completed.
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void basicStopWatchingScenario() throws Exception, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        MockClientMessageHandler adeptHandler = new MockClientMessageHandler(adeptSpine, false);
        adeptSpine.subscribe(adeptHandler, SystemMessageType.START_WATCHING, SystemMessageType.STOP_WATCHING);
        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, SystemMessageType.START_WATCHING, SystemMessageType.STOP_WATCHING);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        TransactionUID uid = adeptSpine.getNextUid();
        RequestStartWatching requestStartWatching = new RequestStartWatching(adept, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(adeptSpine.send(requestStartWatching));

        // Now we get Adept to instruct the Spine to stop watching:
        uid = adeptSpine.getNextUid();
        RequestStopWatching requestStopWatching = new RequestStopWatching(adept, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(adeptSpine.send(requestStopWatching));

        // Sleep to allow all the message to propagate
        int counter = 0;
        while (adeptHandler.getMessageUidListFromSender(shell) == null && counter++ < 10) {
            Thread.sleep(20);
        }
        counter = 0;
        while (adeptHandler.getMessageUidListFromSender(shell).size() < 2 && counter++ < 10) {
            Thread.sleep(5);
        }
        // Make sure lumen gets a chance to process the messages too.
        counter = 0;
        while (lumenHandler.getMessageUidListFromSender(shell) == null && counter++ < 10) {
            Thread.sleep(20);
        }
        counter = 0;
        while (lumenHandler.getMessageUidListFromSender(shell).size() < 2 && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Adept and Lumen got the StopWatchingResponse message - the index of 1
        // shows that we are checking the second message to be intercepted (the first was
        // 'start', the second is 'stop')
        UID nextUid = adeptHandler.getMessageUidListFromSender(shell).get(1);
        assertEquals(1, nextUid.getId());
        assertEquals(adept, nextUid.getOriginator());
        nextUid = lumenHandler.getMessageUidListFromSender(shell).get(1);
        assertEquals(1, nextUid.getId());
        assertEquals(adept, nextUid.getOriginator());

        adeptSpine.shutdown(true);
        lumenSpine.shutdown(true);
    }

    /**
     * This test is for the startWatching request that a spine client will send to the
     * spine to indicate that instrumentation is to be observed. This test will ensure
     * that multiple watch requests will ensure the spine waits for the correspoding
     * stop watching requests before actually stopping watching.
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void multipleStartWatchingScenario() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        final boolean[] adeptStartWatching = new boolean[]{false};
        final boolean[] adeptStopWatching = new boolean[]{false};

        adeptSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message instanceof StartWatching) {
                    adeptStartWatching[0] = true;
                } else if (message instanceof StopWatching) {
                    adeptStopWatching[0] = true;
                }
            }
        }, new SystemMessageType[]{SystemMessageType.START_WATCHING, SystemMessageType.STOP_WATCHING});

        final boolean[] lumenStartWatching = new boolean[]{false};
        final boolean[] lumenStopWatching = new boolean[]{false};

        lumenSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message instanceof StartWatching) {
                    lumenStartWatching[0] = true;
                } else if (message instanceof StopWatching) {
                    lumenStopWatching[0] = true;
                }
            }
        }, SystemMessageType.START_WATCHING, SystemMessageType.STOP_WATCHING);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Make the first start watching request...
        TransactionUID uid = adeptSpine.getNextUid();
        RequestStartWatching requestStartWatching = new RequestStartWatching(adept, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(adeptSpine.send(requestStartWatching));
        // Wait for response from Shell Spine
        Thread.sleep(200);
        // Make sure the start watching message was sent
        assertTrue(adeptStartWatching[0]);
        assertTrue(lumenStartWatching[0]);
        // Make sure the stop watching messasge was not sent
        assertFalse(adeptStopWatching[0]);
        assertFalse(lumenStopWatching[0]);

        // Make the second start watching request..
        uid = lumenSpine.getNextUid();
        requestStartWatching = new RequestStartWatching(lumen, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(lumenSpine.send(requestStartWatching));
        // Sleep to allow message propagation
        Thread.sleep(200);
        // Make sure the stop watching messasge was not sent
        assertFalse(adeptStopWatching[0]);
        assertFalse(lumenStopWatching[0]);

        // The first stop watching request...
        uid = adeptSpine.getNextUid();
        RequestStopWatching requestStopWatching = new RequestStopWatching(adept, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(adeptSpine.send(requestStopWatching));
        // Sleep to allow all the message to propagate
        Thread.sleep(200);
        // Make sure the stop watching messasge was not sent, one more request is required
        assertFalse(adeptStopWatching[0]);
        assertFalse(lumenStopWatching[0]);

        // Now issue the Second stop watching request...
        uid = lumenSpine.getNextUid();
        requestStopWatching = new RequestStopWatching(lumen, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(lumenSpine.send(requestStopWatching));
        // Sleep to allow all the message to propagate
        Thread.sleep(200);
        // Make sure the stop watching message was sent
        assertTrue(adeptStopWatching[0]);
        assertTrue(lumenStopWatching[0]);

        adeptSpine.shutdown(true);
        lumenSpine.shutdown(true);
    }

    /**
     * This test makes sure that when a spine closes and sends the spine closing message. If
     * it is listed as one of the watch request spines, the watch count will be decremented,
     * but not otherwise.
     * @throws com.sri.tasklearning.spine.SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void ensureLocalSpineKeepsTrackOfCorrectWatchingSpines() throws Exception, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);

        MockClientMessageHandler adeptHandler = new MockClientMessageHandler(adeptSpine, false);
        adeptSpine.subscribe(adeptHandler, SystemMessageType.START_WATCHING, SystemMessageType.STOP_WATCHING);
        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, SystemMessageType.START_WATCHING, SystemMessageType.STOP_WATCHING);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Make the first start watching request...
        TransactionUID uid = adeptSpine.getNextUid();
        RequestStartWatching requestStartWatching = new RequestStartWatching(adept, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(adeptSpine.send(requestStartWatching));

        // Make the second start watching request..
        uid = lumenSpine.getNextUid();
        requestStartWatching = new RequestStartWatching(lumen, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(lumenSpine.send(requestStartWatching));

        // Sleep to allow message propagation
        Thread.sleep(50);

        // The first stop watching request...
        uid = adeptSpine.getNextUid();
        RequestStopWatching requestStopWatching = new RequestStopWatching(adept, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(adeptSpine.send(requestStopWatching));

        // Sleep to allow all the message to propagate
        Thread.sleep(100);

        // Make sure the spine is still watching by ensuring the stop watching message was not sent:
        assertEquals(1, adeptHandler.getMessageUidListFromSender(shell).size());

        // Here is where this test differs from the one above, Lets stop one of the spines that is
        // not engaged in watching (lapdog) and make sure this is not handled as a stop watching
        // event by the LOCAL Spine (the shell )
        lapdogSpine.shutdown(true);

        // Sleep to allow all the message to propagate
        Thread.sleep(200);

        // Make sure the spine is still watching by ensuring the stop watching message was not sent:
        assertEquals(1, adeptHandler.getMessageUidListFromSender(shell).size());

        // Now issue the Second stop watching request...
        uid = lumenSpine.getNextUid();
        requestStopWatching = new RequestStopWatching(lumen, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(lumenSpine.send(requestStopWatching));

        // Wait for message delivery
        int counter = 0;
        while (adeptHandler.getMessageUidListFromSender(shell) == null &&
               lumenHandler.getMessageUidListFromSender(shell) == null &&
                counter < 1000) {
            counter++;
            Thread.sleep(5);
        }
        if (counter == 500) {
            log.error("Message did not arrive inside of 5 seconds - there is a problem");
            fail();
        }
        counter = 0;
        while (adeptHandler.getMessageUidListFromSender(shell).size() < 2 &&
               lumenHandler.getMessageUidListFromSender(shell).size() < 2 &&
                counter < 1000) {
            counter++;
            Thread.sleep(10);
        }
        if (counter == 500) {
            log.error("Message did not arrive inside of 5 seconds - there is a problem");
            fail();
        }

        // Make sure Adept and Lumen got the StopWatching message - the index of 1
        // shows that we are checking the second message to be intercepted (the first was
        // 'start', the second is 'stop')
        UID nextUid = adeptHandler.getMessageUidListFromSender(shell).get(1);
        assertEquals(1, nextUid.getId());
        // The originator will be Lumen since it is the second stop watching message
        // that causes the watching to be stopped.
        assertEquals(lumen, nextUid.getOriginator());
        nextUid = lumenHandler.getMessageUidListFromSender(shell).get(1);
        assertEquals(1, nextUid.getId());
        assertEquals(lumen, nextUid.getOriginator());

        adeptSpine.shutdown(true);
        lumenSpine.shutdown(true);
        lapdogSpine.shutdown(true);
    }

    /**
     * This test examines the spines ability to handle the SerialNumberRequest
     * appropriately. It should log a warning if the spine is not in watching mode
     * when the request is made.
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void serialNumberRequestScenario() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);

        MockClientMessageHandler adeptHandler = new MockClientMessageHandler(adeptSpine, false);
        adeptSpine.subscribe(adeptHandler, SystemMessageType.SERIAL_NUMBER_RESPONSE);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        TransactionUID uid = adeptSpine.getNextUid();
        SerialNumberRequest serialNumberRequest = new SerialNumberRequest(adept, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(adeptSpine.send(serialNumberRequest));

        // Sleep to allow all the message to propagate
        int counter = 0;
        while (adeptHandler.getMessageUidListFromSender(shell) == null && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Adept got the SerialNumberResponse message, the originator
        // should be adept to ensure the correct client gets the correct serial number
        UID nextUid = adeptHandler.getMessageUidListFromSender(shell).get(0);
        assertEquals(0, nextUid.getId());
        assertEquals(adept, nextUid.getOriginator());

        adeptSpine.shutdown(true);
    }

    /**
     * This test is for the stopWatching request that a spine client will send to the
     * spine to indicate that instrumentation is completed.
     * @throws SpineException if something goes wrong
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void clientExitSeenAsStopWatchingScenario() throws Exception, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        MockClientMessageHandler adeptHandler = new MockClientMessageHandler(adeptSpine, false);
        adeptSpine.subscribe(adeptHandler, SystemMessageType.START_WATCHING, SystemMessageType.STOP_WATCHING);
        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, SystemMessageType.START_WATCHING, SystemMessageType.STOP_WATCHING);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        TransactionUID uid = adeptSpine.getNextUid();
        RequestStartWatching requestStartWatching = new RequestStartWatching(adept, uid);
        // This tells us that the request was handled by the LOCAL Instance of the spine
        assertTrue(adeptSpine.send(requestStartWatching));

        // Now make the Adept Client bugout - this should be treated as a RequestStopWatching
        adeptSpine.shutdown(true);

        // Sleep to allow all the message to propagate
        int counter = 0;
        Thread.sleep(200);
        while (lumenHandler.getMessageUidListFromSender(shell).size() < 2 && counter++ < 10) {
            Thread.sleep(5);
        }

        // Make sure Lumen got the StopWatchingResponse message - the index of 1
        // shows that we are checking the second message to be intercepted (the first was
        // 'start', the second is 'stop')
        UID nextUid = lumenHandler.getMessageUidListFromSender(shell).get(1);
        assertEquals(1, nextUid.getId());
        assertEquals(adept, nextUid.getOriginator());

        lumenSpine.shutdown(true);
    }

    @Test
    public void customBridgeMessageScenario() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        // Create a mock Bridge message handler and subscribe it to the
        // CustomBridgeMessage topic
        MockClientMessageHandler bridgeHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(bridgeHandler, UserMessageType.CUSTOM_BRIDGE_MESSAGE);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue a custom message from lumen into the spine
        TransactionUID uid = lumenSpine.getNextUid();
        String payload = "This is the payload and could be any type of object";
        CustomBridgeMessage customBridgeMessage = new CustomBridgeMessage(lumen, uid, payload);
        assertTrue(lumenSpine.send(customBridgeMessage));

        // Sleep to allow all the messages to propagate
        int counter = 0;
        while (bridgeHandler.getMessageUidListFromSender(lumen) == null && counter++ < 10) {
            Thread.sleep(5);
        }
        if (bridgeHandler.getMessageUidListFromSender(lumen) == null) {
            fail();
        }
    }

    /**
     * This demostrates how to send a custom message to lumen. The Lumen instance should
     * be subscribed to the CUSTOM_LUMEN_MESSAGE topic so any message sent on this topic
     * should be received by Lumen. Lumen is responsible for understanding the payload
     * however, make sure that it is able to isInstance whatever is sent in the payload
     * @throws SpineException if something goes wrong
     * @throws InterruptedException  if something goes wrong
     */
    @Test
    public void customLumenMessageScenario() throws Throwable {
        // (1) The shell starts up and initializes the Broker
        // The LOCAL Jms spine is the one that manages the Jms Broker, there can only
        // be one of these, the current plan is for the Shell, to manage this.
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        // (2) Lumen starts up and creates its Jms Instance which it will use to send
        // and receive messages
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        // (3) The Editor starts up and creates its Jms Instance which it will use to send
        // and receive messages
        editorSpine = new JmsSpine(JmsClient.REMOTE, editor);

        // (4) Lumen creates its handler and subscribes to the custom lumen message
        // topic
        // Create a mock Lumen message handler and subscribe it to the
        // CustomLumenMessage topic. The handler is the code that the Jms
        // Spine calls when a message for that client is received.
        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, UserMessageType.CUSTOM_LUMEN_MESSAGE);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // (5) The Editor creates a custom message type and sends it into
        // the spine using its instance of the JmsSpine.
        TransactionUID uid = editorSpine.getNextUid();
        String payload = "This is the payload and could be any type of object";
        CustomLumenMessage customLumenMessage = new CustomLumenMessage(editor, uid, payload);

        Thread.sleep(25);

        // (6) True is returned if the spine knows at least one client is
        // subscribed to this topic
        assertTrue(editorSpine.send(customLumenMessage));

        // (7) Confirm that the Lumen handler got the message
        // Sleep to allow all the messages to propagate
        int counter = 0;
        while (lumenHandler.getMessageUidListFromSender(editor) == null && counter++ < 10) {
            Thread.sleep(5);
        }
        if (lumenHandler.getMessageUidListFromSender(editor) == null) {
            fail();
        }
        lumenSpine.shutdown(true);
        editorSpine.shutdown(true);
    }

    @Test
    public void customLapdogMessageScenario() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        // Create a mock Lapdog message handler and subscribe it to the
        // CustomLapdogMessage topic
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.CUSTOM_LAPDOG_MESSAGE);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue a custom message from the bridge into the spine
        TransactionUID uid = masterJmsSpine.getNextUid();
        String payload = "This is the payload and could be any type of object";
        CustomLapdogMessage customLapdogMessage = new CustomLapdogMessage(bridge, uid, payload);
        assertTrue(masterJmsSpine.send(customLapdogMessage));

        // Sleep to allow all the messages to propagate
        int counter = 0;
        while (lapdogHandler.getMessageUidListFromSender(bridge) == null && counter++ < 10) {
            Thread.sleep(5);
        }
        if (lapdogHandler.getMessageUidListFromSender(bridge) == null) {
            fail();
        }
    }

    @Test
    public void customShellMessageScenario() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Shell message handler and subscribe it to the
        // CustomShellMessage topic
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, shell);
        MockClientMessageHandler shellHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(shellHandler, UserMessageType.CUSTOM_SHELL_MESSAGE);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue a custom message from the bridge into the spine
        TransactionUID uid = masterJmsSpine.getNextUid();
        String payload = "This is the payload and could be any type of object";
        CustomShellMessage customShellMessage = new CustomShellMessage(bridge, uid, payload);
        assertTrue(masterJmsSpine.send(customShellMessage));

        // Sleep to allow all the messages to propagate
        int counter = 0;
        while (shellHandler.getMessageUidListFromSender(bridge) == null && counter++ < 10) {
            Thread.sleep(5);
        }
        if (shellHandler.getMessageUidListFromSender(bridge) == null) {
            fail();
        }
    }

    @Test
    public void customEditorMessageScenario() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, bridge);

        // Create a mock Editor message handler and subscribe it to the
        // CustomEditorMessage topic
        editorSpine = new JmsSpine(JmsClient.REMOTE, editor);
        MockClientMessageHandler editorHandler = new MockClientMessageHandler(editorSpine, false);
        editorSpine.subscribe(editorHandler, UserMessageType.CUSTOM_EDITOR_MESSAGE);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create and issue a custom message from the bridge into the spine
        TransactionUID uid = masterJmsSpine.getNextUid();
        String payload = "This is the payload and could be any type of object";
        CustomEditorMessage customEditorMessage = new CustomEditorMessage(bridge, uid, payload);
        assertTrue(masterJmsSpine.send(customEditorMessage));

        // Sleep to allow all the messages to propagate
        int counter = 0;
        while (editorHandler.getMessageUidListFromSender(bridge) == null && counter++ < 10) {
            Thread.sleep(5);
        }
        if (editorHandler.getMessageUidListFromSender(bridge) == null) {
            fail();
        }
    }

    // This test goes crackers on Windows, comment out till we find a work around
    // @Test
    public void canReceiveMessagesFromOtherJVM() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        // Create a mock Lumen message handler and subscribe it to the
        // execute request topic
        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(lumenHandler, UserMessageType.EXECUTE_REQUEST);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        boolean noMessages = true;
        long patience = 20 * 1000;
        long wait = 0;
        boolean alreadyStartedOtherTest = false;
        while (noMessages && wait < patience) {
            Thread.sleep(1000);
            wait += 1000;
            if (lumenHandler.getMessageUidListFromSender(bridge) == null) {
                if (!alreadyStartedOtherTest) {
                    String[] args = new String[] {
                            "java",
                            "-cp",
                            System.getProperty("java.class.path"),
                            JmsRemoteSpine_Basic.class.getName()
                    };
                    Runtime rt = Runtime.getRuntime();
                    Process proc = rt.exec(args, new String[0], new File(
                            "../.."));

                    // any output?
                    StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");
                    StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT");
                    errorGobbler.start();
                    outputGobbler.start();
                    int exitVal = proc.waitFor();
                    log.info("ExitValue: " + exitVal);
                    alreadyStartedOtherTest = true;
                }
                log.info("No Messages from the other JVM yet");
            }
            else {
                log.info("Message Received from the other JVM!, originator was {}",
                        lumenHandler.getMessageUidListFromSender(bridge).get(0).getOriginator());
                noMessages = false;
            }
        }
        if (noMessages) {
            fail("No message received from the Remote JmsSpine Instance");
        }
    }

    // This test goes crackers on Windows, comment out till we find a work around
    // @Test
    public void canHandleComplexInteractionWithMultipleSpines() throws Throwable {
        // This is the equivelant of the Shell Spine that is started by the TLModule
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        // Create a mock Lumen message handler and subscribe it to the
        // execute_request and type_result topic through the lumen spine
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, UserMessageType.EXECUTE_REQUEST, UserMessageType.TYPE_RESULT);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Create a mock Lapdog message handler and subscribe it to the
        // execute_request and type_result topic through the lapdog spine
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.LEARN_REQUEST, UserMessageType.TYPE_RESULT);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        Thread otherJvmLauncher = new Thread(new BridgeJmsSpine());
        otherJvmLauncher.start();

        log.info("Pause to allow the Bridge to start and run...");
        Thread.sleep(1000);
        log.info("Ok, lets expect messages from the bridge!");

        // The Bridge will have issued a LearnRequest so get Lapdog to pick it up
        TransactionUID learnRequestUid = null;
        boolean noLearnRequest = true;
        long wait = 0;
        while (noLearnRequest && wait < patience) {
            if (lapdogHandler.getMessageUidListFromSender(bridge) != null) {
                noLearnRequest = false;
                log.info("*******************************Recieved LearnRequest from Bridge");
                learnRequestUid = lapdogHandler.getMessageUidListFromSender(bridge).get(0);
            }
            else {
                log.info("No LearnRequest from Bridge yet...");
            }
            Thread.sleep(100);
            wait += 100;
        }
        if (noLearnRequest) {
            log.error("----------------------------Bridge did not send a LearnRequest.");
            fail();
        }

        // Now Lapdpog looks at the procedure to be learned and needs to request some
        // type information so it sends the Bridge a Type Query
        TransactionUID typeQueryUid = lapdogSpine.getNextUid();
        lapdogSpine.send(new TypeQuery(lapdog, (SimpleTypeName) TypeNameFactory
                .makeName("walk"), typeQueryUid));
        log.info("Posted Type Query, now lets sleep 100..........................");
        Thread.sleep(100);

        // Bridge should now send back a TypeResult with the same Transaction UID
        // as above
        boolean noTypeResult = true;
        wait = 0;
        while (noTypeResult && wait < patience) {
            if (lapdogHandler.getMessageUidListFromSender(bridge).size() == 2) {
                UID uid = lapdogHandler.getMessageUidListFromSender(bridge).get(1);
                if (uid.equals(typeQueryUid)) {
                    noTypeResult = false;
                    log.info("********************Recieved TypeResult from Bridge");
                }
                else {
                    log.warn("Got a response from Bridge but it was not the correct UID! Expected: " + typeQueryUid.getId() + typeQueryUid.getOriginator());
                    log.warn("Got: " + uid.getId() + uid.getOriginator());
                    fail();
                }
            }
            else {
                log.info("No TypeResult from Bridge yet...");
            }
            Thread.sleep(100);
            wait += 100;
        }
        if (noTypeResult) {
            log.error("--------------------Bridge did not reply with a TypeResult.");
            fail();
        }

        // Now Lapdog can learn the procedure and send the Bridge the Learned Result
        lapdogSpine.send(new LearnResult(lapdog, learnRequestUid, "how to walk instructions"));
        Thread.sleep(100);

        // Now that Bridge has the source it will send it to Lumen and ask it to execute it
        TransactionUID executeRequestUID = null;
        boolean noExecuteRequest = true;
        wait = 0;
        while (noExecuteRequest && wait < patience) {
            if (lumenHandler.getMessageUidListFromSender(bridge).size() == 2) {
                executeRequestUID = lumenHandler.getMessageUidListFromSender(bridge).get(1);
                noExecuteRequest = false;
                log.info("*********************Recieved ExecuteRequest from Bridge");
            }
            else {
                log.info("No ExecuteRequest from Bridge yet...");
            }
            Thread.sleep(100);
            wait += 100;
        }
        if (noExecuteRequest) {
            log.error("----------------------Bridge did not send an ExecuteRequest.");
            fail();
        }

        // Lumen looks at the execute request and decides it needs to look up the type info
        typeQueryUid = lumenSpine.getNextUid();
        lumenSpine.send(new TypeQuery(lumen, (SimpleTypeName) TypeNameFactory
                .makeName("walk"), typeQueryUid));
        Thread.sleep(100);

        // Bridge should now send back a TypeResult with the same Transaction UID
        // as above
        noTypeResult = true;
        wait = 0;
        while (noTypeResult && wait < patience) {
            if (lumenHandler.getMessageUidListFromSender(bridge).size() == 3) {
                UID uid = lumenHandler.getMessageUidListFromSender(bridge).get(2);
                if (uid.equals(typeQueryUid)) {
                    noTypeResult = false;
                    log.info("**********************Recieved TypeResult from Bridge");
                }
                else {
                    log.warn("Got a response from Bridge but it was not the correct UID!");
                    System.exit(1);
                }
            }
            else {
                log.info("No TypeResult from Bridge yet...");
            }
            Thread.sleep(100);
            wait += 100;
        }
        if (noTypeResult) {
            log.error("--------------------------------Bridge did not reply with a TypeResult.");
            fail();
        }

        // Now that Lumen has what it needs it can execute the procedure and send status messages
        lumenSpine.send(new StartExecutionStatus(lumen, executeRequestUID,
                null, (SimpleTypeName) TypeNameFactory.makeName("walk"), 0,
                inParams));
        Thread.sleep(100);
        lumenSpine.send(new SuccessExecutionStatus(lumen, executeRequestUID, null, inParams, outParams));


        Thread.sleep(2000);
        lumenSpine.shutdown(true);
        lapdogSpine.shutdown(true);
    }

    /**
     * This test is to ensure we don't fall foul of TLEARN-68. We need to be sure that when CPOF
     * starts up and sends out its subscription notifications and then immediately calls an
     * EXECUTE_REQUEST on Lumen, that Lumen will process the subscription messages first and so
     * will guarantee that when it issues a TYPE_QUERY immediately after getting the
     * EXECUTION_REQUEST that it has already processed the fact that CPOF will handle the request
     * and so the send will not fail.
     * @throws Exception
     */
    // This test goes crackers on Windows, comment out till we find a work around
    // @Test
    public void canShowSubscriptionMessagesAreAlwaysReceivedBeforeRequestMessages() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        // Create te Lumen spine
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        final List<UID> messageIdList = new ArrayList<UID>();

        // Create a mock Lumen message handler and subscribe it to the
        // execute request topic. As soon as we get this message we immediately
        // respond with a type query to see if the subscription notification
        // arrived from the bridge spine in time - if not the send will fail,
        // and that will be our bug
        MessageHandler lumenHandler = new MessageHandler() {

            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (ExecuteRequest.class.isInstance(message)) {
                    // Immediately return the typeQuery, if this fails that kills the test
                    TypeResult typeResult = new TypeResult(
                            bridge,
                            (SimpleTypeName) TypeNameFactory.makeName("a type"),
                            ATRSyntax.toSource(spineType), lumenSpine
                                    .getNextUid());
                    messageIdList.add(message.getUid());
                    try {
                        assertTrue(lumenSpine.send(typeResult));
                    } catch (SpineException e) {
                        e.printStackTrace();
                    }
                }
            }

        };
        // Subscribe to the messages we want to get from the bridge.
        lumenSpine.subscribe(lumenHandler, UserMessageType.EXECUTE_REQUEST, UserMessageType.TYPE_RESULT);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        boolean noMessages = true;
        long patience = 20 * 1000;
        long wait = 0;
        boolean alreadyStartedOtherTest = false;
        while (noMessages && wait < patience) {
            Thread.sleep(1000);
            wait += 1000;
            if (messageIdList.size() < 1) {
                if (!alreadyStartedOtherTest) {
                    String[] args = new String[] {
                            "java",
                            "-cp",
                            System.getProperty("java.class.path"),
                            JmsRemoteSpine_ImmediateExecuteCall.class.getName()
                    };
                    Runtime rt = Runtime.getRuntime();
                    Process proc = rt.exec(args, new String[0], new File(
                            "../.."));

                    // any output?
                    StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");
                    StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT");
                    errorGobbler.start();
                    outputGobbler.start();
                    int exitVal = proc.waitFor();
                    log.info("ExitValue: " + exitVal);
                    alreadyStartedOtherTest = true;
                }
                log.info("No Messages from the other JVM yet");
            }
            else {
                log.info("Message Received from the other JVM!, originator was {}",
                        messageIdList.get(0).getOriginator());
                noMessages = false;
            }
        }
        if (noMessages) {
            fail("No message received from the Remote JmsSpine Instance");
        }
    }

    @Test
    public void canLimitSubscribersToPrivilegedMessages() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        editorSpine = new JmsSpine(JmsClient.REMOTE, editor);

        // Lets test the TypeStoreRequest message subscription - the spine should permit only 1 subscriber
        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        assertTrue(lumenSpine.subscribe(lumenHandler, PrivilegedMessageType.TYPE_STORE_REQUEST));

        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        assertFalse(lapdogSpine.subscribe(lapdogHandler, PrivilegedMessageType.TYPE_STORE_REQUEST));

        MockClientMessageHandler adeptHandler = new MockClientMessageHandler(adeptSpine, false);
        assertFalse(adeptSpine.subscribe(adeptHandler, PrivilegedMessageType.TYPE_STORE_REQUEST));

        MockClientMessageHandler editorHandler = new MockClientMessageHandler(editorSpine, false);
        assertFalse(editorSpine.subscribe(editorHandler, PrivilegedMessageType.TYPE_STORE_REQUEST));

        MockClientMessageHandler shellHandler = new MockClientMessageHandler(masterJmsSpine, false);
        assertFalse(masterJmsSpine.subscribe(shellHandler, PrivilegedMessageType.TYPE_STORE_REQUEST));

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // The lumen message handler should get this message as it was the first to register as a
        // subscriber on the privileged message
        TypeStoreRequest typeStoreRequest = new TypeStoreRequest(shell,
                actionName, "<TaskModel/>",
                masterJmsSpine.getNextUid());
        assertTrue(adeptSpine.send(typeStoreRequest));

        while (lumenHandler.getMessageUidListFromSender(shell) == null) {
            Thread.sleep(20);
        }

        // Check to make sure only the lumenHandler got the message
        assertTrue(lumenHandler.getMessageUidListFromSender(shell).size() == 1);
        assertTrue(lapdogHandler.getMessageUidListFromSender(shell) == null);
        assertTrue(adeptHandler.getMessageUidListFromSender(shell) == null);
        assertTrue(editorHandler.getMessageUidListFromSender(shell) == null);

        // Cleanup
        lumenSpine.shutdown(true);
        lapdogSpine.shutdown(true);
        adeptSpine.shutdown(true);
        editorSpine.shutdown(true);
    }

    /**
     * This tests the ability of the Durable register message. If the LOCAL Spine starts up a
     * little late, we want to make sure the REGISTER messages from the REMOTE spines are there
     * waiting for it - if this doesn't work it will make the REMOTE spines hand foeva
     * @throws Exception
     */
    // This test goes crackers on Windows, comment out till we find a work around
    // @Test
    public void canHandleSeriousDelayInStartingLOCAL() throws Exception {
        // This will start a LOCAL Jms Spine with a nice big delay before it
        // subscribes to the REGISTER topic - this tests the situation when
        // one of the REMOTE Spine gets off a REGISTER message before the
        // LOCAL has an opportunity to listen for it.
        Thread otherJvmLauncher = new Thread(new DelayedLocalJmsSpine());
        otherJvmLauncher.start();

        // If this test fails, the test will block until the test times out. The
        // spine blocks because it waits forever unless the LOCAL spine sends
        // register confirmation.
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        // Cleanup
        lumenSpine.shutdown(true);

        int attempts = 0;
        while (otherJvmLauncher.isAlive() && attempts++ < 10) {
            Thread.sleep(1000);
            log.warn("Waiting for test class to exit...");
        }
        if (otherJvmLauncher.isAlive()) {
            fail();
        }
    }

    @Test
    /**
     * For CPAL-333 we need to be able to tell when the Master Spine goes down.
     */
    public void canTellWhenMasterIsDown() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        assertTrue(lumenSpine.isMasterRunning());

        // Now kill the master spine:
        masterJmsSpine.shutdown(true);

        // wait a bit for it to expire
        Thread.sleep(1000);

        assertFalse(lumenSpine.isMasterRunning());

        try {
            lumenSpine.shutdown(true);
        }
        catch (Exception e) {
            // May cause an exception due to LOCAL being down already.
        }
    }

    /**
     * Make sure we can create multiple test spine clients to allow us to determine
     * the upstate of the master spine. To accomplish this we will use a different
     * identifier to the id each time it is called.
     */
    @Test
    public void canCreateMultipleTestClients() throws Exception, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);

        JmsSpine testSpine1 = new JmsSpine(JmsClient.TEST, "testclient1");
        JmsSpine testSpine2 = new JmsSpine(JmsClient.TEST, "testclient2");
        JmsSpine testSpine3 = new JmsSpine(JmsClient.TEST, "testclient3");

        // ALso call the isMasterRunning repeatedly to make sure that side works as well

        lumenSpine.isMasterRunning();
        Thread.sleep(1010);
        lumenSpine.isMasterRunning();
        Thread.sleep(1010);
        lumenSpine.isMasterRunning();
        Thread.sleep(1010);
        lumenSpine.isMasterRunning();
        Thread.sleep(1010);
        lumenSpine.isMasterRunning();
        Thread.sleep(1010);
        lumenSpine.isMasterRunning();

        testSpine1.shutdown(true);
        testSpine2.shutdown(true);
        testSpine3.shutdown(true);
    }

    @Test
    public void canRemoteSpineShutDownMaster() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);

        // Wait for startup to complete
        Thread.sleep(5000);

        // Initiate the shutdown
        lumenSpine.shutdownMaster();

        // Give each spine an adequate amount of time to wrap up proceedings - especially on slower machines.
        int wait = 0;
        int patience = 100;
        while (lumenSpine.isRunning() && wait++ < patience) {
            Thread.sleep(100);
        }
        assertEquals(false, lumenSpine.isRunning());

        wait = 0;
        while (lapdogSpine.isRunning() && wait++ < patience) {
            Thread.sleep(100);
        }
        assertEquals(false, lapdogSpine.isRunning());

        wait = 0;
        while (masterJmsSpine.isRunning() && wait++ < patience) {
            Thread.sleep(100);
        }
        assertEquals(false, masterJmsSpine.isRunning());
    }

    @Test
    public void canMasterSpineCanShutItselfDownGracefully() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);

        // Wait for startup to complete
        Thread.sleep(3000);

        // Initiate the shutdown
        masterJmsSpine.shutdownMaster();

        // Give each spine an adequate amount of time to wrap up proceedings - especially on slower machines.
        int wait = 0;
        int patience = 200;
        while (lumenSpine.isRunning() && wait++ < patience) {
            Thread.sleep(100);
        }
        assertEquals(false, lumenSpine.isRunning());


        wait = 0;
        while (lapdogSpine.isRunning() && wait++ < patience) {
            Thread.sleep(100);
        }
        assertEquals(false, lapdogSpine.isRunning());

        wait = 0;
        while (masterJmsSpine.isRunning() && wait++ < patience) {
            Thread.sleep(100);
        }
        assertEquals(false, masterJmsSpine.isRunning());
    }

    @Test
    public void isMasterRunningDoesTheRightThingBeforeMasterHasStarted() throws SpineException, InterruptedException {

        Thread thread1 = new Thread(new BridgeMimic("bridge2"));
        thread1.start();

        Thread thread2 = new Thread(new BridgeMimic("bridge1"));
        thread2.start();

        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        Thread.sleep(500);
    }

    /**
     * This test is designed to expose an issue described in CPAL-454 where the system went OOM after a couple of
     * days due to buckets of test spines lieing around in memory that had not been GC'd
     */
    //@Test This test is designed to run over days and so should not be run in the general
    public void isMasterRunningCalledInATightLoopCausingOOM() throws SpineException, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        JmsSpine lumenSpine2 = new JmsSpine(JmsClient.REMOTE, lumen + "2");
        JmsSpine lapdogSpine2 = new JmsSpine(JmsClient.REMOTE, lapdog + "2");
        JmsSpine lumenSpine3 = new JmsSpine(JmsClient.REMOTE, lumen + "3");
        JmsSpine lapdogSpine3 = new JmsSpine(JmsClient.REMOTE, lapdog + "3");
        JmsSpine lumenSpine4 = new JmsSpine(JmsClient.REMOTE, lumen + "4");
        JmsSpine lapdogSpine4 = new JmsSpine(JmsClient.REMOTE, lapdog + "4");
        JmsSpine lumenSpine5 = new JmsSpine(JmsClient.REMOTE, lumen + "5");
        JmsSpine lapdogSpine5 = new JmsSpine(JmsClient.REMOTE, lapdog + "5");
        JmsSpine lumenSpine6 = new JmsSpine(JmsClient.REMOTE, lumen + "6");
        JmsSpine lapdogSpine6 = new JmsSpine(JmsClient.REMOTE, lapdog + "6");

        // Wait for startup to complete
        Thread.sleep(3000);

        for (int i = 0; i < 10000000; i++) {
            lumenSpine.isMasterRunning();
            Thread.sleep(100);
            lapdogSpine.isMasterRunning();
            Thread.sleep(100);
            lumenSpine2.isMasterRunning();
            Thread.sleep(100);
            lapdogSpine2.isMasterRunning();
            Thread.sleep(100);            
			lumenSpine3.isMasterRunning();
            Thread.sleep(100);
            lapdogSpine3.isMasterRunning();
            Thread.sleep(100);            
			lumenSpine4.isMasterRunning();
            Thread.sleep(100);
            lapdogSpine4.isMasterRunning();
            Thread.sleep(100);            
			lumenSpine5.isMasterRunning();
            Thread.sleep(100);
            lapdogSpine5.isMasterRunning();
            Thread.sleep(100);           
			lumenSpine6.isMasterRunning();
            Thread.sleep(100);
            lapdogSpine6.isMasterRunning();
            Thread.sleep(100);       
		}
    }

    private class BridgeMimic implements Runnable {

        String spineName = "";

        public BridgeMimic(String spineName) {
            this.spineName = spineName;
        }

        public void run() {
            while (!isTaskLearningRunning()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.warn("Spine is up! " + Thread.currentThread().getName());
        }

        public boolean isTaskLearningRunning() {
            try {
                Spine spine = new JmsSpine(JmsClient.TEST, "testPALStatus" + "-" + spineName + "-" + System.currentTimeMillis());
                spine.shutdown(true);
                log.debug("PAL is running");
                return true;
            } catch (Exception e) {
                log.debug("PAL is not running");
                return false;
            }
        }
    }
}
