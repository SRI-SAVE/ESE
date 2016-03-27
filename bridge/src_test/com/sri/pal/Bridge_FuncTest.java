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

// $Id: Bridge_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.JmsClient;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.RequestIgnored;
import com.sri.tasklearning.spine.messages.TypeQuery;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.UserMessageType;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Bridge_FuncTest
        extends PALTestCase {
    public static final String NAMESPACE = "namespace";

    private static Bridge bridge;

    @BeforeClass
    public static void buildBridge()
            throws Exception {
        Bridge.startPAL();
        bridge = Bridge.newInstance("bft");
        ProcedureLearner.setStorage(Bridge_FuncTest.class, bridge);
    }

    @AfterClass
    public static void shutdown() throws PALRemoteException {
        bridge.shutdown();
    }

    @Test
    public void actionModel()
            throws Exception {
        ActionModel actionModel = bridge.getActionModel();
        URL url = ActionModels.class.getResource(ActionModels.METADATA);
        Set<ActionModelDef> types = actionModel.load(url, "ns");
        assertEquals(6, types.size());
    }

    /**
     * Originally, asking for an unknown type would cause the caller to wait indefinitely
     * as the responders only responded if they had information. Now all subscribers to the
     * TYPE_QUERY topic will respond.
     * @throws Exception
     */
    @Test
    public void willReturnAnEmptyTypeResultIfAskedForUnknownType() throws Exception {
        final boolean[] success = new boolean[1];
        JmsSpine lumenSpine = new JmsSpine(JmsClient.REMOTE, "Lumen2");
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
        lumenSpine.subscribe(typeResultHandler, UserMessageType.TYPE_RESULT);

        TypeQuery typeQuery = new TypeQuery("Lumen2",
                (SimpleTypeName) TypeNameFactory.makeName("bigfaketypename",
                        "1.0", "FAKE"), lumenSpine.getNextUid());
        assertTrue(lumenSpine.send(typeQuery));
        // Now wait a few seconds to see if the right thing happens in the inner classes
        // above.
        int tries = 10;
        int attempts = 0;
        while(!success[0] && attempts++ < tries) {
            Thread.sleep(100);
        }
        assertTrue(success[0]);
        lumenSpine.shutdown(true);
    }

    @Test
    public void sendingBadExecutionRequestReturnsIgnoreNotification() throws SpineException, InterruptedException {
        JmsSpine bridgeSpine = new JmsSpine(JmsClient.REMOTE, "fake_bridge");

        final boolean[] requestIgnored = new boolean[1];
        requestIgnored[0] = false;
        bridgeSpine.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessageHandlerException {
                if (message instanceof RequestIgnored) {
                    requestIgnored[0] = true;
                }
            }
        }, UserMessageType.REQUEST_IGNORED);

        ExecuteRequest executeRequest = new ExecuteRequest("fake_bridge",
                bridgeSpine.getNextUid(), null,
                (SimpleTypeName) TypeNameFactory.makeName("badaction", "1.0",
                        "worsenamespace"), new ArrayList<Object>(), false);

        bridgeSpine.send(executeRequest);

        // Allow messages to propagate through the system
        Thread.sleep(1000);

        assertTrue(requestIgnored[0]);
    }

    // FIXME: Re-enable tests when desired API is reached.
//    /**
//     * If the Editor isn't on the classpath, we should get an
//     * exception when we try to start it.
//     */
//    @Test(expected = JFXProcessLauncherException.class)
//    public void startEditor()
//            throws Exception {
//        TypeName typeName = new TypeName("foo", NAMESPACE);
//        bridge.startAdeptEditor(typeName, "/foo/", null);
//    }
//
//    /**
//     * If the Executor isn't on the classpath, we should get an
//     * exception when we try to start it.
//     */
//    @Test(expected = JFXProcessLauncherException.class)
//    public void startExecutor()
//            throws Exception {
//        TypeName typeName = new TypeName("foo", NAMESPACE);
//        bridge.startAdeptEditor(typeName, "/foo/", null);
//    }
}
