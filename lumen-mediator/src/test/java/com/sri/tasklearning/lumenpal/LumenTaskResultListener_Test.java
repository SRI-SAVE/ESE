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

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.lumenpal.mock.MockExecutionHandler;
import com.sri.tasklearning.lumenpal.mock.MockLumen;
import com.sri.tasklearning.lumenpal.util.LumenMediatorTestCase;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.mediators.TypeFetcher;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.util.MockSpine;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.SuccessExecutionStatus;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.spine.util.ReplyWatcher;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LumenTaskResultListener_Test extends LumenMediatorTestCase {
    private SimpleTypeName actionName;
    private TransactionUID uid;
    private TransactionUID parentUid;
    private List<Object> inParams;
    private Spine spineFacade;
    private MockExecutionHandler execHandler;
    private LockingActionModel actionModel;
    private SimpleTypeName spineActionTypeName;
    private ReplyWatcher<SerialNumberResponse> serialGetter;

    @BeforeMethod
    public void setup() throws SpineException {
        spineActionTypeName = (SimpleTypeName) TypeNameFactory.makeName(
                "SpineAction", "1.0", ExecutionHandler.NAMESPACE);
        uid = new TransactionUID("Lumen", 0);
        parentUid = null;
        inParams = new ArrayList<Object>();
        spineFacade = new MockSpine();
        MockLumen lumen = new MockLumen();
        LumenTypeAdder adder = new LumenTypeAdder(lumen);
        actionModel = new LockingActionModel(adder);
        ReplyWatcher<SerialNumberResponse> serialGetter = new ReplyWatcher<SerialNumberResponse>(
                SerialNumberResponse.class, spineFacade);
        ReplyWatcher<TypeResult> typeResultGetter = new ReplyWatcher<TypeResult>(
                TypeResult.class, spineFacade);
        serialGetter = new ReplyWatcher<SerialNumberResponse>(
                SerialNumberResponse.class, spineFacade);
        TypeFetcher typeFetcher = new TypeFetcher(spineFacade, actionModel,
                typeResultGetter);
        ProcedureDependencyFinder procDepFinder = new ProcedureDependencyFinder(
                actionModel, typeFetcher);
        execHandler = new MockExecutionHandler(actionModel, new TypeFetcher(
                spineFacade, actionModel, typeResultGetter), serialGetter,
                spineFacade, procDepFinder);
    }

    @Test
    public void canConstruct() {
        Runnable cleanupTask = new Runnable() {
            @Override
            public void run() {
            }
        };
        LumenTaskResultListener listener = new LumenTaskResultListener(
                actionName, uid, parentUid, 0, inParams, cleanupTask,
                spineFacade, execHandler, actionModel, serialGetter);
        assertNotNull(listener);
        assertTrue(LumenTaskResultListener.class.isInstance(listener));
    }

    @Test
    public void sendsSuccessMessageOnSuccessWithSolidTypeLock() throws Exception{
        // Get a readlock for the type
        final Lock lock = actionModel.getReadLock(spineActionTypeName);
        Runnable cleanupTask = new Runnable() {
            @Override
            public void run() {
                lock.unlock();
                actionModel.maybeRemove(spineActionTypeName);
            }
        };

        // Create the listener
        LumenTaskResultListener listener = new LumenTaskResultListener(
                spineActionTypeName, uid, parentUid, 0, inParams, cleanupTask,
                spineFacade, execHandler, actionModel, serialGetter);

        // Create the spine action that the listener is waiting on an outcome from
        ATRActionDeclaration spineAction = ATRTestUtil.makeAction(
                spineActionTypeName, new ATRParameter[0], null, null);
        // Add the spine action to the action model
        actionModel.add(spineAction);

        // Make sure the message queue is empty before we invoke the message delivery
        assertTrue(((MockSpine)spineFacade).getSentMessagesList().size() == 0);

        // Make sure readlock is in place - we should not be allowed to clobber this
        // type once it has a readlock
        assertFalse(actionModel.maybeRemove(spineActionTypeName));

        // Trigger the listener behavior
        listener.taskSucceeded("SpineAction", Collections.emptyList());

        // Make sure readlock is released after the message has been handled
        boolean successfullyRemoved = actionModel.maybeRemove(spineActionTypeName);
        if (!successfullyRemoved) {
            // If we could not remove it, the listener may have already cleaned it up - lets
            // check to be sure
            ATRDecl type = actionModel.getRaw(spineActionTypeName);
            int wait = 0;
            int patience = 1000;
            while (type != null && wait < patience) {
                wait++;
                Thread.sleep(1000);
                type = actionModel.getRaw(spineActionTypeName);
            }
            if (type != null)
                fail("Could not remove type even though it is still present and should be unlocked.");
        }

        // Check the messages to make sure we got what was inspected
        List<Message> messageList = ((MockSpine)spineFacade).getSentMessagesList();
        assertTrue(messageList.size() == 1);
        assertTrue(SuccessExecutionStatus.class.isInstance(messageList.get(0)));
    }

    @Test
    public void sendsFailureMessageOnFailureWithSolidTypeLock() throws Exception{
        // Get a readlock for the type
        final Lock lock = actionModel.getReadLock(spineActionTypeName);
        Runnable cleanupTask = new Runnable() {
            @Override
            public void run() {
                lock.unlock();
                actionModel.maybeRemove(spineActionTypeName);
            }
        };

        // Create the listener
        LumenTaskResultListener listener = new LumenTaskResultListener(
                spineActionTypeName, uid, parentUid, 0, inParams, cleanupTask,
                spineFacade, execHandler, actionModel, serialGetter);

        // Create the spine action that the listener is waiting on an outcome from
        ATRActionDeclaration spineAction = ATRTestUtil.makeAction(
                spineActionTypeName, new ATRParameter[0], null, null);
        // Add the spine action to the action model
        actionModel.add(spineAction);

        // Make sure the message queue is empty before we invoke the message delivery
        assertTrue(((MockSpine)spineFacade).getSentMessagesList().size() == 0);

        // Make sure readlock is in place - we should not be allowed to clobber this
        // type once it has a readlock
        assertFalse(actionModel.maybeRemove(spineActionTypeName));

        // Trigger the listener behavior
        // Value for throwable parameter should indicate failure, not cancelation, not failure
        try {
            listener.taskFailed("SpineAction", new Throwable(), null);
        } catch(Exception e) {

        }

        // Make sure readlock is released after the message has been handled
        boolean successfullyRemoved = actionModel.maybeRemove(spineActionTypeName);
        if (!successfullyRemoved) {
            // If we could not remove it, the listener may have already cleaned it up - lets
            // check to be sure
            ATRDecl type = actionModel.getRaw(spineActionTypeName);
            int wait = 0;
            int patience = 10;
            while (type != null && wait < patience) {
                wait++;
                Thread.sleep(10);
                type = actionModel.getRaw(spineActionTypeName);
            }
            if (type != null)
                fail("Could not remove type even though it is still present and should be unlocked.");
        }

        // Check the messages to make sure we got what was inspected
        List<Message> messageList = ((MockSpine)spineFacade).getSentMessagesList();
        assertTrue(messageList.size() == 1);
        assertTrue(ErrorExecutionStatus.class.isInstance(messageList.get(0)));

    }
}
