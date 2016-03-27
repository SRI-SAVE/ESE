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

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRSigDecl;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.lumenpal.mock.MockLumen;
import com.sri.tasklearning.lumenpal.util.LumenMediatorTestCase;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.util.MockSpine;
import com.sri.tasklearning.spine.messages.CancelRequest;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.MessageType;
import com.sri.tasklearning.spine.messages.StartExecutionStatus;
import com.sri.tasklearning.spine.messages.SuccessExecutionStatus;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LumenClient_Test extends LumenMediatorTestCase {
    private MockLumen mockLumen;
    private MockSpine mockSpine;
    private TransactionUID uid;
    private TransactionUID parentUid;
    private String bridge = "bridge";
    private List<Object> inParams;
    private List<Object> outParams;
    private SimpleTypeName spineActionTypeName;
    private ATRSigDecl spineAction;

    @BeforeMethod
    public void setUp() throws SpineException {
        mockLumen = new MockLumen();
        mockSpine = new MockSpine();

        uid = new TransactionUID(bridge, 0);
        parentUid = new TransactionUID(bridge, 1);
        inParams = new ArrayList<Object>();
        outParams = new ArrayList<Object>();

        spineActionTypeName = (SimpleTypeName) TypeNameFactory
                .makeName("SpineAction");
        spineAction = ATRTestUtil.makeAction(spineActionTypeName,
                new ATRParameter[0], null, null);
    }

    @Test
    public void canConstruct() throws SpineException {
        LumenClient lumenClient = new LumenClient(mockLumen, mockSpine);
        assertNotNull(lumenClient);
        assertTrue(LumenClient.class.isInstance(lumenClient));
    }

    @Test
    public void successfullySubscribedToInterestingTopics() throws SpineException {
        LumenClient lumenClient = new LumenClient(mockLumen, mockSpine);
        lumenClient.start();

        Set<MessageType> expectedLumenTopics = new HashSet<MessageType>();
        expectedLumenTopics.add(UserMessageType.TYPE_RESULT);
        expectedLumenTopics.add(UserMessageType.EXECUTION_STATUS);
        expectedLumenTopics.add(UserMessageType.CANCEL);
        expectedLumenTopics.add(UserMessageType.EXECUTE_REQUEST);
        expectedLumenTopics.add(UserMessageType.CONSTRAINT_REQUEST);
        expectedLumenTopics.add(SystemMessageType.SERIAL_NUMBER_RESPONSE);
        expectedLumenTopics.add(UserMessageType.CACHE_EXPIRE);
        expectedLumenTopics.add(UserMessageType.BREAKPOINT_RESPONSE);
        expectedLumenTopics.add(UserMessageType.EXPR_EVAL_REQUEST);
        expectedLumenTopics.add(SystemMessageType.SPINE_CLOSING);

        List<MessageType> topics = mockSpine.getSubscribedTopics();
        for (MessageType topic : topics) {
            assertTrue(topic.toString(), expectedLumenTopics.remove(topic));
        }
        assertTrue(expectedLumenTopics.size() == 0);
    }

    @Test
    public void theCorrectHandlersGetCalledForTypeResult() throws SpineException, MessageHandlerException {
        LumenClient lumenClient = new LumenClient(mockLumen, mockSpine);
        lumenClient.start();

        TypeResult typeResult = new TypeResult(bridge, spineActionTypeName,
                ATRSyntax.toSource(spineAction), uid);
        mockSpine.deliver(UserMessageType.TYPE_RESULT, typeResult);
        assertTrue(mockSpine.getLastUid(UserMessageType.TYPE_RESULT).equals(uid));
        assertNull(mockSpine.getLastUid(UserMessageType.EXECUTION_STATUS));
        assertNull(mockSpine.getLastUid(UserMessageType.CANCEL));
        assertNull(mockSpine.getLastUid(UserMessageType.EXECUTE_REQUEST));
    }

    @Test
    public void theCorrectHandlersGetCalledForExecutionStatus() throws SpineException, MessageHandlerException {
        LumenClient lumenClient = new LumenClient(mockLumen, mockSpine);
        lumenClient.start();

        uid = new TransactionUID(bridge, 0);
        StartExecutionStatus startExecutionStatus = new StartExecutionStatus(bridge, uid, parentUid,
                spineActionTypeName, 0, inParams);

        mockSpine.deliver(UserMessageType.EXECUTION_STATUS, startExecutionStatus);
        assertTrue(mockSpine.getLastUid(UserMessageType.EXECUTION_STATUS).equals(uid));
        assertNull(mockSpine.getLastUid(UserMessageType.TYPE_RESULT));
        assertNull(mockSpine.getLastUid(UserMessageType.CANCEL));
        assertNull(mockSpine.getLastUid(UserMessageType.EXECUTE_REQUEST));

        uid = new TransactionUID(bridge, 1);
        SuccessExecutionStatus successExecutionStatus = new SuccessExecutionStatus(bridge, uid, parentUid,
                inParams, outParams);

        mockSpine.deliver(UserMessageType.EXECUTION_STATUS, successExecutionStatus);
        assertTrue(mockSpine.getLastUid(UserMessageType.EXECUTION_STATUS).equals(uid));
        assertNull(mockSpine.getLastUid(UserMessageType.TYPE_RESULT));
        assertNull(mockSpine.getLastUid(UserMessageType.CANCEL));
        assertNull(mockSpine.getLastUid(UserMessageType.EXECUTE_REQUEST));

        uid = new TransactionUID(bridge, 2);
        ErrorInfo error = ErrorFactory.error("test", ErrorType.CANCEL, "foo");
        ErrorExecutionStatus errorExecutionStatus = new ErrorExecutionStatus(bridge, uid, parentUid,
                error);

        mockSpine.deliver(UserMessageType.EXECUTION_STATUS, errorExecutionStatus);
        assertTrue(mockSpine.getLastUid(UserMessageType.EXECUTION_STATUS).equals(uid));
        assertNull(mockSpine.getLastUid(UserMessageType.TYPE_RESULT));
        assertNull(mockSpine.getLastUid(UserMessageType.CANCEL));
        assertNull(mockSpine.getLastUid(UserMessageType.EXECUTE_REQUEST));
    }

    @Test
    public void theCorrectHandlersGetCalledForCancel() throws SpineException, MessageHandlerException {
        LumenClient lumenClient = new LumenClient(mockLumen, mockSpine);
        lumenClient.start();

        CancelRequest cancelRequest = new CancelRequest(bridge, uid);
        mockSpine.deliver(UserMessageType.CANCEL, cancelRequest);
        assertNull(mockSpine.getLastUid(UserMessageType.TYPE_RESULT));
        assertNull(mockSpine.getLastUid(UserMessageType.EXECUTION_STATUS));
        assertTrue(mockSpine.getLastUid(UserMessageType.CANCEL).equals(uid));
        assertNull(mockSpine.getLastUid(UserMessageType.EXECUTE_REQUEST));
    }

    @Test
    public void theCorrectHandlersGetCalledForExecute() throws SpineException, MessageHandlerException {
        LumenClient lumenClient = new LumenClient(mockLumen, mockSpine);
        lumenClient.start();

        ExecuteRequest executeRequest = new ExecuteRequest(bridge, uid,
                parentUid, spineActionTypeName, inParams, false);
        mockSpine.deliver(UserMessageType.EXECUTE_REQUEST, executeRequest);
        assertNull(mockSpine.getLastUid(UserMessageType.TYPE_RESULT));
        assertNull(mockSpine.getLastUid(UserMessageType.EXECUTION_STATUS));
        assertNull(mockSpine.getLastUid(UserMessageType.CANCEL));
        assertTrue(mockSpine.getLastUid(UserMessageType.EXECUTE_REQUEST).equals(uid));
    }

}
