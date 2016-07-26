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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameExpr;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.lumenpal.mock.MockComplexTypeFetcher;
import com.sri.tasklearning.lumenpal.mock.MockLumen;
import com.sri.tasklearning.lumenpal.mock.MockSimpleTypeFetcher;
import com.sri.tasklearning.lumenpal.mock.MockTaskExecutor;
import com.sri.tasklearning.lumenpal.util.LumenMediatorTestCase;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.util.MockSpine;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ReplyWatcher;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ExecutionHandler_Test extends LumenMediatorTestCase {
    private MockLumen mockLumen;
    private LockingActionModel lockingActionModel;
    private MockSpine mockSpine;
    private MockSimpleTypeFetcher simpleTypeFetcher;
    private MockComplexTypeFetcher complexTypeFetcher;

    private String bridge = "Bridge";
    private TransactionUID parentUid;
    private TransactionUID uid;
    private TypeNameExpr listOfStringSpineType;
    private SimpleTypeName nestedActionSpineType;
    private SimpleTypeName nestedActionSpineType2;
    private List<Object> inParams;
    private ReplyWatcher<SerialNumberResponse> serialGetter;

    @BeforeMethod
    public void setUp() throws SpineException {
        mockLumen = new MockLumen();
        LumenTypeAdder adder = new LumenTypeAdder(mockLumen);
        lockingActionModel = new LockingActionModel(adder);
        mockSpine = new MockSpine();
        simpleTypeFetcher = new MockSimpleTypeFetcher(
                new ReplyWatcher<TypeResult>(TypeResult.class, mockSpine),
                mockSpine, lockingActionModel);
        complexTypeFetcher = new MockComplexTypeFetcher(
                new ReplyWatcher<TypeResult>(TypeResult.class, mockSpine),
                mockSpine, lockingActionModel);
        serialGetter = new ReplyWatcher<SerialNumberResponse>(
                SerialNumberResponse.class, mockSpine);

        parentUid = null;
        uid = new TransactionUID(bridge, 0);
        listOfStringSpineType = (TypeNameExpr) TypeNameFactory
                .makeName("list<string>");
        nestedActionSpineType = (SimpleTypeName) TypeNameFactory
                .makeName("SpineAction");
        nestedActionSpineType2 = (SimpleTypeName) TypeNameFactory.makeName(
                "SpineAction2", "1.0", "SPARK");
        inParams = new ArrayList<Object>();
    }

    @Test
    public void canConstruct() {
        ProcedureDependencyFinder procDepFinder = new ProcedureDependencyFinder(
                lockingActionModel, simpleTypeFetcher);
        RunOnce runOnce = new RunOnce(mockLumen, lockingActionModel, procDepFinder);
        ExecutionHandler executionHandler = new ExecutionHandler(runOnce,
                lockingActionModel, simpleTypeFetcher, serialGetter, mockSpine,
                procDepFinder);
        assertNotNull(executionHandler);
        assertTrue(ExecutionHandler.class.isInstance(executionHandler));
    }

    @Test
    public void implicitDeclaration()
            throws Exception {
        ProcedureDependencyFinder procDepFinder = new ProcedureDependencyFinder(
                lockingActionModel, simpleTypeFetcher);
        List<ATRDecl> spineTypeList = procDepFinder
                .getRequiredTypes(listOfStringSpineType, true);

        assertEquals(0, spineTypeList.size());
    }

    @Test
    public void canGetComplexRequiredType() throws MessageHandlerException, SpineException {
        ProcedureDependencyFinder procDepFinder = new ProcedureDependencyFinder(
                lockingActionModel, complexTypeFetcher);
        List<ATRDecl> spineTypeList = procDepFinder
                .getRequiredTypes(nestedActionSpineType, true);

        assertTrue(spineTypeList.size() == 2);
        assertEquals("SpineActionParent", TypeUtil.getName(spineTypeList.get(0)).getFullName());
        assertEquals("SpineAction", TypeUtil.getName(spineTypeList.get(1)).getFullName());
    }

    @Test
    public void canAddAndRemoveExecutors() {
        ProcedureDependencyFinder procDepFinder = new ProcedureDependencyFinder(
                lockingActionModel, complexTypeFetcher);
        RunOnce runOnce = new RunOnce(mockLumen, lockingActionModel, procDepFinder);
        ExecutionHandler executionHandler = new ExecutionHandler(runOnce,
                lockingActionModel, complexTypeFetcher, serialGetter,
                mockSpine, procDepFinder);

        MockTaskExecutor mockTaskExecutor = new MockTaskExecutor();
        executionHandler.addExecutor(mockTaskExecutor, "SpineAction");

        executionHandler.removeExecutor("SpineAction");
    }

    //@Test
    public void aSimpleExecutionCallGoesThrough() throws MessageHandlerException {
        MockTaskExecutor mockTaskExecutor = new MockTaskExecutor();
        ProcedureDependencyFinder procDepFinder = new ProcedureDependencyFinder(
                lockingActionModel, complexTypeFetcher);
        RunOnce runOnce = new RunOnce(mockLumen, lockingActionModel, procDepFinder);
        ExecutionHandler executionHandler = new ExecutionHandler(runOnce,
                lockingActionModel, complexTypeFetcher, serialGetter,
                mockSpine, procDepFinder);

        executionHandler.addExecutor(mockTaskExecutor, "SPARK.SpineAction2");
        ExecuteRequest executeRequest = new ExecuteRequest(bridge, uid,
                parentUid, nestedActionSpineType2, inParams, false);

        executionHandler.handleMessage(executeRequest);

        assertTrue(mockTaskExecutor.wasCalled());
    }


}
