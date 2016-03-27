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
import static org.testng.AssertJUnit.assertTrue;

import com.sri.tasklearning.lumenpal.mock.MockExecutionHandler;
import com.sri.tasklearning.lumenpal.mock.MockLumen;
import com.sri.tasklearning.lumenpal.util.LumenMediatorTestCase;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.mediators.TypeFetcher;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.util.MockSpine;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.util.ReplyWatcher;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ServerConnectionImpl_Test extends LumenMediatorTestCase {
    private MockExecutionHandler executionHandler;
    private TypeFetcher typeFetcher;
    private ExecutionWatcher executionWatcher;
    private LockingActionModel lockingActionModel;
    private MockSpine mockSpine;
    private ReplyWatcher<SerialNumberResponse> serialGetter;

    @BeforeMethod
    public void setup() throws SpineException {
        MockLumen mockLumen = new MockLumen();
        LumenTypeAdder adder = new LumenTypeAdder(mockLumen);
        lockingActionModel = new LockingActionModel(adder);
        mockSpine = new MockSpine();
        ReplyWatcher<TypeResult> typeQueryWatcher = new ReplyWatcher<TypeResult>(
                TypeResult.class, mockSpine);
        executionWatcher = new ExecutionWatcher(mockSpine);
        typeFetcher = new TypeFetcher(mockSpine, lockingActionModel, typeQueryWatcher);
        serialGetter = new ReplyWatcher<SerialNumberResponse>(
                SerialNumberResponse.class, mockSpine);
        ProcedureDependencyFinder procDepFinder = new ProcedureDependencyFinder(
                lockingActionModel, typeFetcher);
        executionHandler = new MockExecutionHandler(lockingActionModel,
                typeFetcher, serialGetter, mockSpine, procDepFinder);
    }

    @Test
    public void canConstruct() {
        ServerConnectionImpl serverConn = new ServerConnectionImpl(
                executionHandler, executionWatcher, lockingActionModel,
                mockSpine, serialGetter);
        assertNotNull(serverConn);
        assertTrue(ServerConnectionImpl.class.isInstance(serverConn));
    }

    @Test
    public void canRegisterAsTaskExecutor() {
        new ServerConnectionImpl(executionHandler, executionWatcher,
                lockingActionModel, mockSpine, serialGetter);
    }
}
