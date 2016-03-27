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

import java.util.ArrayList;
import java.util.List;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.lumenpal.mock.MockTaskExecutionListener;
import com.sri.tasklearning.lumenpal.util.LumenMediatorTestCase;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.impl.jms.util.MockSpine;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.SuccessExecutionStatus;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ExecutionWatcher_Test extends LumenMediatorTestCase {

    private String bridge = "Bridge";
    private TransactionUID uid;
    private List<Object> inParams;
    private List<Object> outParams;
    private ATRActionDeclaration spineAction;
    private SimpleTypeName spineActionTypeName;
    private List<Object> argList;
    private Spine spine;

    @BeforeMethod
    public void setUp()
            throws Exception {
        uid = new TransactionUID(bridge, 0);
        inParams = new ArrayList<Object>();
        outParams = new ArrayList<Object>();

        spineActionTypeName = (SimpleTypeName) TypeNameFactory
                .makeName("SpineAction");
        spineAction = ATRTestUtil.makeAction(spineActionTypeName,
                new ATRParameter[0], null, null);
        argList = new ArrayList<Object>();
        spine = new MockSpine();
    }

    @Test
    public void canConstruct() {
        ExecutionWatcher executionWatcher = new ExecutionWatcher(spine);
        assertNotNull(executionWatcher);
        assertTrue(ExecutionWatcher.class.isInstance(executionWatcher));
    }

    @Test
    public void doesNothingWithUnRegisteredUid() throws MessageHandlerException {
        ExecutionWatcher executionWatcher = new ExecutionWatcher(spine);
        MockTaskExecutionListener watcher = new MockTaskExecutionListener();
        executionWatcher.watch(uid, watcher, spineAction, argList);

        TransactionUID differentUid = new TransactionUID(bridge, 100);
        SuccessExecutionStatus successMessage = new SuccessExecutionStatus(bridge, differentUid, null,
                                                                           inParams, outParams);
        executionWatcher.handleMessage(successMessage);

        assertFalse(watcher.wasCalled());
    }

    @Test
    public void callsWatcherOnSuccess() throws MessageHandlerException {
        ExecutionWatcher executionWatcher = new ExecutionWatcher(spine);
        MockTaskExecutionListener watcher = new MockTaskExecutionListener();
        executionWatcher.watch(uid, watcher, spineAction, argList);

        SuccessExecutionStatus successMessage = new SuccessExecutionStatus(bridge, uid, null,
                                                                           inParams, outParams);
        executionWatcher.handleMessage(successMessage);

        assertTrue(watcher.wasCalledWithSuccess());
    }

    @Test
    public void callsWatcherOnError() throws MessageHandlerException {
        ExecutionWatcher executionWatcher = new ExecutionWatcher(spine);
        MockTaskExecutionListener watcher = new MockTaskExecutionListener();
        executionWatcher.watch(uid, watcher, spineAction, argList);

        ErrorInfo error = ErrorFactory.error("test", ErrorType.CANCEL, "test");
        ErrorExecutionStatus errorMessage = new ErrorExecutionStatus(bridge, uid, null, error);
        executionWatcher.handleMessage(errorMessage);

        assertTrue(watcher.wasCalledWithFailure());
    }
}
