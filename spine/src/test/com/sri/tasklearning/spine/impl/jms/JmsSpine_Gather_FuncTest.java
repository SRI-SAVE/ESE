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
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.util.MockClientMessageHandler;
import com.sri.tasklearning.spine.impl.jms.util.MultiNestedGatherMessageHandler;
import com.sri.tasklearning.spine.impl.jms.util.NestedGatherMessageHandler;
import com.sri.tasklearning.spine.impl.jms.util.SpineTestCase;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.TypeListQuery;
import com.sri.tasklearning.spine.messages.TypeQuery;
import com.sri.tasklearning.spine.messages.UserMessageType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JmsSpine_Gather_FuncTest extends SpineTestCase {

    private static final Logger log = LoggerFactory.getLogger("JmsSpine_Test");
    private String adept;
    private String bridge;
    private String lapdog;
    private String lumen;
    private String shell;
    private String editor;
    private SimpleTypeName actionName1;
    private JmsSpine masterJmsSpine;
    private JmsSpine lumenSpine;
    private JmsSpine lapdogSpine;
    private JmsSpine adeptSpine;
    private JmsSpine editorSpine;
    private JmsSpine bridgeSpine;

    @BeforeMethod
    public void setUp() {
        adept = "Adept";
        bridge = "Bridge";
        lapdog = "Lapdog";
        lumen = "Lumen";
        shell = "Shell";
        editor = "Editor";
        actionName1 = (SimpleTypeName) TypeNameFactory.makeName("Walk");

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
        if (bridgeSpine != null) {
            bridgeSpine.shutdown(true);
        }
        bridgeSpine = null;
        // Now we can shut down the local that manages the Message Broker
        if (masterJmsSpine != null) {
            masterJmsSpine.shutdown(true);
        }
        masterJmsSpine = null;
    }

    /**
     * The Gather method is used to make a request to all the Spines and then return with
     * the combined results. In this test we will call it with the TypeListQuery as as
     * parameter. The gather method is a blocking/synchronous call
     * @throws com.sri.tasklearning.spine.SpineException if something goes horribly, horribly wrong
     */
    @Test
    public void canHandleBasicGatherCommand() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        editorSpine = new JmsSpine(JmsClient.REMOTE, editor);

        MockClientMessageHandler shellHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(shellHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler adeptHandler = new MockClientMessageHandler(adeptSpine, false);
        adeptSpine.subscribe(adeptHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler editorHandler = new MockClientMessageHandler(editorSpine, false);
        editorSpine.subscribe(editorHandler, UserMessageType.TYPE_LIST_QUERY);

		// Allow subscriptions to disperse
		Thread.sleep(500);

        TypeListQuery typeListQuery = new TypeListQuery(editor, editorSpine.getNextUid(), TypeListQuery.Subset.ACTION);
        Message[] responseMessages = editorSpine.gather(typeListQuery, Spine.DEFAULT_TIMEOUT);

        assertEquals(4, responseMessages.length);

        // Cleanup
        lumenSpine.shutdown(true);
        lapdogSpine.shutdown(true);
        adeptSpine.shutdown(true);
        editorSpine.shutdown(true);
    }

    @Test
    public void canUseGatherForTypeQuery() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        editorSpine = new JmsSpine(JmsClient.REMOTE, editor);

        MockClientMessageHandler shellHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(shellHandler, UserMessageType.TYPE_QUERY);

        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, UserMessageType.TYPE_QUERY);

        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.TYPE_QUERY);

        MockClientMessageHandler adeptHandler = new MockClientMessageHandler(adeptSpine, false);
        adeptSpine.subscribe(adeptHandler, UserMessageType.TYPE_QUERY);

        MockClientMessageHandler editorHandler = new MockClientMessageHandler(editorSpine, false);
        editorSpine.subscribe(editorHandler, UserMessageType.TYPE_QUERY);

		// Allow subscriptions to disperse
		Thread.sleep(500);

		TypeQuery typeQuery = new TypeQuery(editor, actionName1, editorSpine.getNextUid());
        Message[] responseMessages = editorSpine.gather(typeQuery, Spine.DEFAULT_TIMEOUT);

        assertEquals(4, responseMessages.length);

        // Cleanup
        lumenSpine.shutdown(true);
        lapdogSpine.shutdown(true);
        adeptSpine.shutdown(true);
        editorSpine.shutdown(true);
    }

    /**
     * This tests the scenario where the Bridge issues an execute request to Lumen, Lumen
     * asks the Bridge for a type definition, the Bridge in turn needs to ask Lumen (or
     * the other Bridge) for a nested type in order to satisfy the original type query.
     * This had been resulting in deadlock but when it works, nested gather commmands will
     * allow the system to unwind. Note: All the real magic is going on in the
     * NestedGatherMessageHandler
     * @throws Exception if something goes wrong
     */
    @Test
    public void canHandleNestedGatherCommands() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);

        NestedGatherMessageHandler lumenHandler = new NestedGatherMessageHandler(lumenSpine);
        lumenSpine.subscribe(lumenHandler, UserMessageType.TYPE_QUERY);

        NestedGatherMessageHandler bridgeHandler = new NestedGatherMessageHandler(bridgeSpine);
        bridgeSpine.subscribe(bridgeHandler, UserMessageType.TYPE_QUERY);

		// Allow subscriptions to disperse
		Thread.sleep(500);

        TypeQuery typeQuery = new TypeQuery(lumen, actionName1, lumenSpine.getNextUid());
        Message[] responseMessages = lumenSpine.gather(typeQuery, Spine.DEFAULT_TIMEOUT);

        assertEquals(1, responseMessages.length);

        // Cleanup
        lumenSpine.shutdown(true);
        bridgeSpine.shutdown(true);
        masterJmsSpine.shutdown(true);
    }

    @Test
    public void canTellWhenAnotherSpineGoesAway() throws Exception {
        // Create master spine which will manage the spine clients
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lumenSpine.subscribe(new MockClientMessageHandler(lumenSpine, false), UserMessageType.TYPE_QUERY);
        // Allow subscription messages to propagate.
        Thread.sleep(100);
        assertEquals(2, masterJmsSpine.getSpineInstanceCount());
        assertEquals(1, masterJmsSpine.calculateExpectedResponderCount(UserMessageType.TYPE_QUERY));

        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        lapdogSpine.subscribe(new MockClientMessageHandler(lapdogSpine, false), UserMessageType.TYPE_QUERY);
        // Allow subscription messages to propagate.
        Thread.sleep(100);
        assertEquals(3, masterJmsSpine.getSpineInstanceCount());
        assertEquals(2, masterJmsSpine.calculateExpectedResponderCount(UserMessageType.TYPE_QUERY));

        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        adeptSpine.subscribe(new MockClientMessageHandler(adeptSpine, false), UserMessageType.TYPE_QUERY);
        // Allow subscription messages to propagate.
        Thread.sleep(100);
        assertEquals(4, masterJmsSpine.getSpineInstanceCount());
        assertEquals(3, masterJmsSpine.calculateExpectedResponderCount(UserMessageType.TYPE_QUERY));

        lumenSpine.shutdown(true);
        // Allow subscription messages to propagate.
        Thread.sleep(100);
        assertEquals(3, masterJmsSpine.getSpineInstanceCount());
        assertEquals(2, masterJmsSpine.calculateExpectedResponderCount(UserMessageType.TYPE_QUERY));

        lapdogSpine.shutdown(true);
        // Allow subscription messages to propagate.
        Thread.sleep(100);
        assertEquals(2, masterJmsSpine.getSpineInstanceCount());
        assertEquals(1, masterJmsSpine.calculateExpectedResponderCount(UserMessageType.TYPE_QUERY));

        adeptSpine.shutdown(true);
        // Allow subscription messages to propagate.
        Thread.sleep(100);
        assertEquals(1, masterJmsSpine.getSpineInstanceCount());
        assertEquals(0, masterJmsSpine.calculateExpectedResponderCount(UserMessageType.TYPE_QUERY));
    }

    /**
     * This tests the scenario where the Bridge issues an execute request to Lumen, Lumen
     * asks the Bridge for a type definition, the Bridge in turn needs to ask Lumen (or
     * the other Bridge) for a nested type in order to satisfy the original type query.
     * This will go on for a few iterations to see if there is any deadlocks possible if
     * this level of nesting is ever required. Note: All the real magic is going on in
     * the MultiNestedGatherMessageHandler
     * @throws Exception if something goes wrong
     */
    //@Test
    //TODO: This functionality is not currently supported. Decide if we need it.
    public void canHandleMultiNestedGatherCommands() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);

        MultiNestedGatherMessageHandler lumenHandler = new MultiNestedGatherMessageHandler(lumenSpine);
        lumenSpine.subscribe(lumenHandler, UserMessageType.TYPE_QUERY);

        MultiNestedGatherMessageHandler bridgeHandler = new MultiNestedGatherMessageHandler(bridgeSpine);
        bridgeSpine.subscribe(bridgeHandler, UserMessageType.TYPE_QUERY);

		// Allow subscriptions to disperse
		Thread.sleep(500);

        TypeQuery typeQuery = new TypeQuery(lumen, actionName1, lumenSpine.getNextUid());
        log.debug("{} Waiting for permission to gather", lumenSpine.getClientId());
        Message[] responseMessages = lumenSpine.gather(typeQuery, Spine.DEFAULT_TIMEOUT);

        assertEquals(1, responseMessages.length);

        // Cleanup
        lumenSpine.shutdown(true);
        bridgeSpine.shutdown(true);
        masterJmsSpine.shutdown(true);
    }

    @Test
    public void canHandleGatherConfusion_CPAL_338() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        editorSpine = new JmsSpine(JmsClient.REMOTE, editor);
        bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);

        MockClientMessageHandler shellHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(shellHandler, UserMessageType.TYPE_QUERY);

        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, UserMessageType.TYPE_QUERY);

        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.TYPE_QUERY);

        MockClientMessageHandler adeptHandler = new MockClientMessageHandler(adeptSpine, false);
        adeptSpine.subscribe(adeptHandler, UserMessageType.TYPE_QUERY);

        MockClientMessageHandler editorHandler = new MockClientMessageHandler(editorSpine, false);
        editorSpine.subscribe(editorHandler, UserMessageType.TYPE_QUERY);

        MockClientMessageHandler bridgeHandler = new MockClientMessageHandler(bridgeSpine, false);
        editorSpine.subscribe(bridgeHandler, UserMessageType.TYPE_LIST_QUERY);

		// Allow subscriptions to disperse
		Thread.sleep(500);

        TypeQuery typeQuery = new TypeQuery(editor, actionName1, editorSpine.getNextUid());

        // Right between the gather permission and the actual gather, issue another gather-able
        // message type to force a gather confusion situation. If this test passes we do not need
        // to be concerned about this bug.
        editorSpine.send(new TypeListQuery(editor, editorSpine.getNextUid(), TypeListQuery.Subset.PROCEDURE));

        Message[] responseMessages = editorSpine.gather(typeQuery, Spine.DEFAULT_TIMEOUT);

        assertEquals(4, responseMessages.length);

        // Cleanup
        lumenSpine.shutdown(true);
        lapdogSpine.shutdown(true);
        adeptSpine.shutdown(true);
        editorSpine.shutdown(true);
    }

    /**
     * For a while we have wanted to be able to issue multiple simultaneous gather requests from the one
     * spine. This test verifies that this is now possible with the added GatherInstance code.
     * @throws SpineException
     */
    @Test
    public void canHandleSingleAsynchronousGatherRequests() throws Exception, InterruptedException {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        editorSpine = new JmsSpine(JmsClient.REMOTE, editor);

        MockClientMessageHandler shellHandler = new MockClientMessageHandler(masterJmsSpine, false);
        masterJmsSpine.subscribe(shellHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, false);
        lumenSpine.subscribe(lumenHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, false);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler adeptHandler = new MockClientMessageHandler(adeptSpine, false);
        adeptSpine.subscribe(adeptHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler editorHandler = new MockClientMessageHandler(editorSpine, false);
        editorSpine.subscribe(editorHandler, UserMessageType.TYPE_LIST_QUERY);

		// Allow subscriptions to disperse
		Thread.sleep(500);
        final Message[][] responseMessages = {new Message[0]};
        final boolean[] resultsReceived = new boolean[]{false};
        GatherCallback callback = new GatherCallback() {
            @Override
            public void result(Message[] messages) {
                resultsReceived[0] = true;
                responseMessages[0] = messages;
            }
            @Override
            public void warning(com.sri.tasklearning.spine.messages.GatherIssues warningType) {
                fail();
            }

        };

        TypeListQuery typeListQuery = new TypeListQuery(editor, editorSpine.getNextUid(), TypeListQuery.Subset.ACTION);
        editorSpine.gatherAsynchronous(typeListQuery, Spine.DEFAULT_TIMEOUT, callback);

        // This is an asynchronous call so it will not block and may take a few ms to complete
        Thread.sleep(500);

        assertEquals(4, responseMessages[0].length);

        // Cleanup
        lumenSpine.shutdown(true);
        lapdogSpine.shutdown(true);
        adeptSpine.shutdown(true);
        editorSpine.shutdown(true);
    }

    @Test
    public void canHandleMultipleAsynchronousGatherRequests() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);
        lumenSpine = new JmsSpine(JmsClient.REMOTE, lumen);
        lapdogSpine = new JmsSpine(JmsClient.REMOTE, lapdog);
        adeptSpine = new JmsSpine(JmsClient.REMOTE, adept);
        editorSpine = new JmsSpine(JmsClient.REMOTE, editor);

        MockClientMessageHandler shellHandler = new MockClientMessageHandler(masterJmsSpine, true);
        masterJmsSpine.subscribe(shellHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler lumenHandler = new MockClientMessageHandler(lumenSpine, true);
        lumenSpine.subscribe(lumenHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler lapdogHandler = new MockClientMessageHandler(lapdogSpine, true);
        lapdogSpine.subscribe(lapdogHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler adeptHandler = new MockClientMessageHandler(adeptSpine, true);
        adeptSpine.subscribe(adeptHandler, UserMessageType.TYPE_LIST_QUERY);

        MockClientMessageHandler editorHandler = new MockClientMessageHandler(editorSpine, true);
        editorSpine.subscribe(editorHandler, UserMessageType.TYPE_LIST_QUERY);

		// Allow subscriptions to disperse
		Thread.sleep(500);

        final Message[][] responseMessages1 = {new Message[0]};
        final boolean[] resultsReceived1 = new boolean[]{false};
        GatherCallback callback1 = new GatherCallback() {
            @Override
            public void result(Message[] messages) {
                resultsReceived1[0] = true;
                responseMessages1[0] = messages;
            }
            @Override
            public void warning(com.sri.tasklearning.spine.messages.GatherIssues warningType) {
                fail();
            }

        };

        final Message[][] responseMessages2 = {new Message[0]};
        final boolean[] resultsReceived2 = new boolean[]{false};
        GatherCallback callback2 = new GatherCallback() {
            @Override
            public void result(Message[] messages) {
                resultsReceived2[0] = true;
                responseMessages2[0] = messages;
            }
            @Override
            public void warning(com.sri.tasklearning.spine.messages.GatherIssues warningType) {
                fail();
            }

        };

        final Message[][] responseMessages3 = {new Message[0]};
        final boolean[] resultsReceived3 = new boolean[]{false};
        GatherCallback callback3 = new GatherCallback() {
            @Override
            public void result(Message[] messages) {
                resultsReceived3[0] = true;
                responseMessages3[0] = messages;
            }
            @Override
            public void warning(com.sri.tasklearning.spine.messages.GatherIssues warningType) {
                fail();
            }

        };

        final Message[][] responseMessages4 = {new Message[0]};
        final boolean[] resultsReceived4 = new boolean[]{false};
        GatherCallback callback4 = new GatherCallback() {
            @Override
            public void result(Message[] messages) {
                resultsReceived4[0] = true;
                responseMessages4[0] = messages;
            }
            @Override
            public void warning(com.sri.tasklearning.spine.messages.GatherIssues warningType) {
                fail();
            }

        };

        final Message[][] responseMessages5 = {new Message[0]};
        final boolean[] resultsReceived5 = new boolean[]{false};
        GatherCallback callback5 = new GatherCallback() {
            @Override
            public void result(Message[] messages) {
                resultsReceived5[0] = true;
                responseMessages5[0] = messages;
            }
            @Override
            public void warning(com.sri.tasklearning.spine.messages.GatherIssues warningType) {
                fail();
            }

        };

        TypeListQuery typeListQuery1 = new TypeListQuery(editor, editorSpine.getNextUid(), TypeListQuery.Subset.ACTION);
        TypeListQuery typeListQuery2 = new TypeListQuery(editor, editorSpine.getNextUid(), TypeListQuery.Subset.ACTION);
        TypeListQuery typeListQuery3 = new TypeListQuery(editor, editorSpine.getNextUid(), TypeListQuery.Subset.ACTION);
        TypeListQuery typeListQuery4 = new TypeListQuery(editor, editorSpine.getNextUid(), TypeListQuery.Subset.ACTION);
        TypeListQuery typeListQuery5 = new TypeListQuery(editor, editorSpine.getNextUid(), TypeListQuery.Subset.ACTION);


        editorSpine.gatherAsynchronous(typeListQuery1, Spine.DEFAULT_TIMEOUT, callback1);
        editorSpine.gatherAsynchronous(typeListQuery2, Spine.DEFAULT_TIMEOUT, callback2);
        editorSpine.gatherAsynchronous(typeListQuery3, Spine.DEFAULT_TIMEOUT, callback3);
        editorSpine.gatherAsynchronous(typeListQuery4, Spine.DEFAULT_TIMEOUT, callback4);
        editorSpine.gatherAsynchronous(typeListQuery5, Spine.DEFAULT_TIMEOUT, callback5);

        // This is an asynchronous call so it will not block and may take a few ms to complete
        Thread.sleep(500);

        assertTrue(resultsReceived1[0]);
        assertEquals(4, responseMessages1[0].length);
        assertTrue(resultsReceived1[0]);
        assertEquals(4, responseMessages2[0].length);
        assertTrue(resultsReceived1[0]);
        assertEquals(4, responseMessages3[0].length);
        assertTrue(resultsReceived1[0]);
        assertEquals(4, responseMessages4[0].length);
        assertTrue(resultsReceived1[0]);
        assertEquals(4, responseMessages5[0].length);

        // Cleanup
        lumenSpine.shutdown(true);
        lapdogSpine.shutdown(true);
        adeptSpine.shutdown(true);
        editorSpine.shutdown(true);
    }

    /*
    @Test
    public void canSupportMultipleSpinesCallingListTypes() throws Exception {
        masterJmsSpine = new JmsSpine(JmsClient.LOCAL, shell);

        Thread.sleep(100);
        Thread thread = new Thread(new BridgeRunner());
        thread.start();

        Thread.sleep(100);
        Thread thread2 = new Thread(new BridgeRunner());
        thread2.start();

        Thread.sleep(100);
        Thread thread3 = new Thread(new BridgeRunner());
        thread3.start();

        Thread.sleep(100);
        Thread thread4 = new Thread(new BridgeRunner());
        thread4.start();

        Thread.sleep(100);
        Thread thread5 = new Thread(new BridgeRunner());
        thread5.start();

        // Keep the process running, like an application window
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
        }
    }

    private class BridgeRunner implements Runnable {

        public void run() {
            TypeListQuery.Subset actionSubset = TypeListQuery.Subset.ACTION;
            TypeListQuery.Subset subsetProcedure = TypeListQuery.Subset.PROCEDURE;

            com.sri.pal.Bridge bridge = null;
            try {
                bridge = com.sri.pal.Bridge.newInstance("test-bridge-" + System.currentTimeMillis());
            } catch (PALException e) {
                e.printStackTrace();
            }

            Set types = null;
            try {
                types = bridge.getTypeStorage().listTypes(TypeStorage.Subset.ACTION, TypeStorage.Subset.PROCEDURE);
            } catch (PALException e) {
                e.printStackTrace();
            }
            log.warn("Got {} types back :)", types.size());

            // Keep the process running, like an application window
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {}
            }

        }
    }
       */
}
