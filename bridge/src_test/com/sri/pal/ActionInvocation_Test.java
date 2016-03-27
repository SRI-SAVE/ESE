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

// $Id: ActionInvocation_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.pal.ActionInvocation.StepCommand;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.types.IntType;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.impl.jms.util.MockSpine;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ATRTestUtil;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ActionInvocation_Test
        extends PALTestCase {
    private ActionDef ad1;
    private IMocksControl mockCtrl;
    private TransactionUID uid1;
    private TestExecutor executor;

    @BeforeMethod
    public void setup()
            throws Exception {
        CTRConstructor ctrBuilder = new CTRConstructor();
        uid1 = new TransactionUID("bogus", 1);
        SimpleTypeName actionName = (SimpleTypeName) TypeNameFactory
                .makeName("action1");
        mockCtrl = EasyMock.createControl();
        Bridge bridge = mockCtrl.createMock(Bridge.class);
        ActionModel actionModel = new MockActionModel(bridge);
        EasyMock.expect(bridge.getActionModel()).andReturn(actionModel)
                .anyTimes();
        GlobalActionNotifier notifier = mockCtrl
                .createMock(GlobalActionNotifier.class);
        EasyMock.expect(bridge.getGlobalNotifier()).andReturn(notifier)
                .anyTimes();
        notifier.newInvocation(EasyMock.isA(ActionInvocation.class));
        EasyMock.expectLastCall().anyTimes();
        ExecutorMap executorMap = new ExecutorMap(bridge);
        EasyMock.expect(bridge.getExecutorMap()).andReturn(executorMap)
                .anyTimes();
        Spine spine = new MockSpine();
        EasyMock.expect(bridge.getSpine()).andReturn(spine).anyTimes();
        mockCtrl.replay();
        executor = new TestExecutor();
        actionModel.registerExecutor(actionName, executor);

        SimpleTypeName td1Name = (SimpleTypeName) TypeNameFactory
                .makeName("String");
        SimpleTypeName td2Name = (SimpleTypeName) TypeNameFactory
                .makeName("Int");
        actionModel.registerCustomTypeFactory(td1Name, new ToStringFactory(
                String.class.getName()));
        actionModel.registerCustomTypeFactory(td2Name, new ToStringFactory(
                IntType.class.getName()));
        CustomTypeDef td1 = new CustomTypeDef(ATRTestUtil.makeCustomType(
                td1Name, String.class), bridge);
        CustomTypeDef td2 = new CustomTypeDef(ATRTestUtil.makeCustomType(
                td2Name, IntType.class), bridge);
        actionModel.storeType(td1.getName(), td1);
        actionModel.storeType(td2.getName(), td2);
        ATRParameter[] params = new ATRParameter[2];
        params[0] = ctrBuilder.createParameter(
                ctrBuilder.createVariable("input1"), Modality.INPUT,
                td1.getName().getFullName(), null);
        params[1] = ctrBuilder.createParameter(ctrBuilder
                .createVariable("output1"), Modality.INPUT, td2.getName()
                .getFullName(), null);
        ATRActionDeclaration atr = ATRTestUtil.makeAction("action1", params,
                null, null);
        ad1 = new ActionDef(atr, bridge);
        actionModel.storeType(ad1.getName(), ad1);
        System.gc();
        Thread.sleep(100);
        TestListener.resetFinalized();
    }

    @AfterMethod
    public void verify() {
        mockCtrl.verify();
    }

    @Test
    public void newInvocation() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        assertEquals(ad1, ai.getDefinition());
        assertEquals(ActionDef.class, ai.getDefinition().getClass());
        assertNull(ai.getCaller());
        assertEquals(Status.CREATED, ai.getStatus());
    }

    @Test
    public void setValue() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.setStatus(Status.RUNNING);

        ai.setValue("input1", "foo");
        assertEquals("foo", ai.getValue("input1"));
        assertEquals("foo", ai.getValue(0));
        assertNull(ai.getValue(1));
        assertNull(ai.getValue("output1"));

        ai.setValue("input1", "bar");
        assertEquals("bar", ai.getValue("input1"));
        assertEquals("bar", ai.getValue(0));
        assertNull(ai.getValue(1));
        assertNull(ai.getValue("output1"));

        IntType value = new IntType(1);
        ai.setValue("output1", value);
        assertEquals("bar", ai.getValue("input1"));
        assertEquals("bar", ai.getValue(0));
        assertEquals(value, ai.getValue(1));
        assertEquals(value, ai.getValue("output1"));

        value = new IntType(2);
        ai.setValue("output1", value);
        assertEquals("bar", ai.getValue("input1"));
        assertEquals("bar", ai.getValue(0));
        assertEquals(value, ai.getValue(1));
        assertEquals(value, ai.getValue("output1"));
    }

    @Test
    public void equals() {
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ActionInvocation ai2 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.RUNNING);
        assertTrue(ai1.equals(ai2));
        assertTrue(ai2.equals(ai1));
        assertTrue(ai1.equals(ai1));
        assertTrue(ai2.equals(ai2));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullDefinition() {
        new ActionInvocation(null, null, 0, uid1);
    }

    @Test
    public void nullParent() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        assertNull(ai.getCaller());
    }

    // TODO We expect this to hang, but I don't know how to specify that easily.
    // The child invocation should wait until the parent becomes RUNNING.
// @Test(expected = IllegalStateException.class)
    public void parentCreated() {
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        new ActionInvocation(ad1, ai1, 0, uid1);
    }

    @Test
    public void parentRunning() {
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.RUNNING);
        ActionInvocation ai2 = new ActionInvocation(ad1, ai1, 0, uid1);
        assertEquals(ai1, ai2.getCaller());
    }

    @Test
    public void parentEnded() {
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.ENDED);
        ActionInvocation ai2 = new ActionInvocation(ad1, ai1, 0, uid1);
        assertEquals(ai1, ai2.getCaller());
    }

    @Test
    public void parentFailed() {
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.FAILED);
        ActionInvocation ai2 = new ActionInvocation(ad1, ai1, 0, uid1);
        assertEquals(ai1, ai2.getCaller());
    }

    @Test
    public void status() {
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        assertEquals(Status.CREATED, ai1.getStatus());
        ai1.setStatus(Status.RUNNING);
        assertEquals(Status.RUNNING, ai1.getStatus());
        ai1.setStatus(Status.ENDED);
        assertEquals(Status.ENDED, ai1.getStatus());

        ActionInvocation ai2 = new ActionInvocation(ad1, null, 0, uid1);
        assertEquals(Status.CREATED, ai2.getStatus());
        ai2.setStatus(Status.RUNNING);
        assertEquals(Status.RUNNING, ai2.getStatus());
        ai2.setStatus(Status.FAILED);
        assertEquals(Status.FAILED, ai2.getStatus());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void setNullStatus() {
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(null);
    }

    @Test
    public void skipRunningState1() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.setStatus(Status.ENDED);
        assertEquals(Status.ENDED, ai.getStatus());
    }

    @Test
    public void skipRunningState2() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.setStatus(Status.FAILED);
        assertEquals(Status.FAILED, ai.getStatus());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void backwardsState1() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.setStatus(Status.RUNNING);
        ai.setStatus(Status.CREATED);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void backwardsState2() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.setStatus(Status.ENDED);
        ai.setStatus(Status.RUNNING);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void backwardsState3() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.setStatus(Status.ENDED);
        ai.setStatus(Status.CREATED);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void backwardsState4() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.setStatus(Status.FAILED);
        ai.setStatus(Status.RUNNING);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void backwardsState5() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.setStatus(Status.FAILED);
        ai.setStatus(Status.CREATED);
    }

    @Test
    public void listenForSuccess()
            throws Exception {
        TestListener listener = new TestListener();
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.addListener(listener);
        assertEquals(0, TestListener.numFinalized());

        listener.reset();
        ai.setStatus(Status.RUNNING);
        assertEquals(1, listener.numEvents());
        assertEquals(Status.RUNNING, listener.lastStatus());

        listener.reset();
        ai.setStatus(Status.ENDED);
        assertEquals(1, listener.numEvents());
        assertEquals(Status.ENDED, listener.lastStatus());

        listener = null;
        System.gc();
        Thread.sleep(1000);
        assertEquals(1, TestListener.numFinalized());
    }

    @Test
    public void listenForFailure()
            throws Exception {
        TestListener listener = new TestListener();
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.addListener(listener);
        assertEquals(0, TestListener.numFinalized());

        ai.setStatus(Status.RUNNING);
        assertEquals(2, listener.numEvents());
        assertEquals(Status.RUNNING, listener.lastStatus());
        listener.reset();

        ai.setStatus(Status.FAILED);
        assertEquals(1, listener.numEvents());
        assertEquals(Status.FAILED, listener.lastStatus());
        listener.reset();

        listener = null;
        System.gc();
        Thread.sleep(1000);
        assertEquals(1, TestListener.numFinalized());
    }

    @Test
    public void removeCreatedListener()
            throws Exception {
        TestListener listener = new TestListener();
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.addListener(listener);

        listener.reset();
        ai.removeListener(listener);
        ai.setStatus(Status.RUNNING);
        assertEquals(0, listener.numEvents());
    }

    @Test
    public void removeRunningListener()
            throws Exception {
        TestListener listener = new TestListener();
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.setStatus(Status.RUNNING);
        ai.addListener(listener);

        listener.reset();
        ai.removeListener(listener);
        ai.setStatus(Status.ENDED);
        assertEquals(0, listener.numEvents());
    }

    @Test
    public void removeEndedListener()
            throws Exception {
        TestListener listener = new TestListener();
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.setStatus(Status.ENDED);
        ai.addListener(listener);

        listener.reset();
        ai.removeListener(listener);
    }

    @Test
    public void addEndedListener()
            throws Exception {
        TestListener listener = new TestListener();
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        ai.setStatus(Status.ENDED);
        assertEquals(0, TestListener.numFinalized());
        ai.addListener(listener);

        listener = null;
        System.gc();
        Thread.sleep(1000);
        assertEquals(1, TestListener.numFinalized());
    }

    @Test
    public void cancelCreated() {
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.RUNNING);
        ActionInvocation ai2 = new ActionInvocation(ad1, ai1, 0, uid1);
        ai1.addListener(listener1);
        ai2.addListener(listener2);

        listener1.reset();
        listener2.reset();
        executor.cancel(ai2);
        assertEquals(3, listener2.numEvents());
        assertEquals(Status.FAILED, ai2.getStatus());
        assertEquals(0, listener1.numEvents());
        assertEquals(Status.RUNNING, ai1.getStatus());
    }

    @Test
    public void cancelRunning() {
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.RUNNING);
        ActionInvocation ai2 = new ActionInvocation(ad1, ai1, 0, uid1);
        ai1.addListener(listener1);
        ai2.addListener(listener2);
        ai2.setStatus(Status.RUNNING);

        listener1.reset();
        listener2.reset();
        executor.cancel(ai2);
        assertEquals(2, listener2.numEvents());
        assertEquals(Status.FAILED, listener2.lastStatus());
        assertEquals(Status.FAILED, ai2.getStatus());
        assertEquals(0, listener1.numEvents());
        assertNull(listener1.lastStatus());
        assertEquals(Status.RUNNING, ai1.getStatus());
    }

    @Test
    public void cancelEnded() {
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.RUNNING);
        ActionInvocation ai2 = new ActionInvocation(ad1, ai1, 0, uid1);
        ai1.addListener(listener1);
        ai2.addListener(listener2);
        ai2.setStatus(Status.ENDED);

        listener1.reset();
        listener2.reset();
        executor.cancel(ai2);
        assertEquals(0, listener2.numEvents());
        assertEquals(Status.ENDED, ai2.getStatus());
        assertEquals(0, listener1.numEvents());
        assertEquals(Status.RUNNING, ai1.getStatus());
    }

    @Test
    public void cancelFailed() {
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.RUNNING);
        ActionInvocation ai2 = new ActionInvocation(ad1, ai1, 0, uid1);
        ai1.addListener(listener1);
        ai2.addListener(listener2);
        ai2.setStatus(Status.FAILED);

        listener1.reset();
        listener2.reset();
        executor.cancel(ai2);
        assertEquals(0, listener2.numEvents());
        assertEquals(Status.FAILED, ai2.getStatus());
        assertEquals(0, listener1.numEvents());
        assertEquals(Status.RUNNING, ai1.getStatus());
    }

    @Test
    public void errorCreated() {
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.RUNNING);
        ActionInvocation ai2 = new ActionInvocation(ad1, ai1, 0, uid1);
        ai1.addListener(listener1);
        ai2.addListener(listener2);

        listener1.reset();
        listener2.reset();
        ErrorInfo e = new ErrorInfo("test", 1, "err", "error", null);
        ai2.error(e);
        assertEquals(3, listener2.numEvents());
        assertEquals(e, listener2.lastError());
        assertEquals(Status.FAILED, listener2.lastStatus());
        assertEquals(Status.FAILED, ai2.getStatus());
    }

    @Test
    public void errorRunning() {
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.RUNNING);
        ActionInvocation ai2 = new ActionInvocation(ad1, ai1, 0, uid1);
        ai1.addListener(listener1);
        ai2.addListener(listener2);
        ai2.setStatus(Status.RUNNING);

        listener1.reset();
        listener2.reset();
        ErrorInfo e = new ErrorInfo("test", 1, "err", "error", null);
        ai2.error(e);
        assertEquals(2, listener2.numEvents());
        assertEquals(e, listener2.lastError());
        assertEquals(Status.FAILED, listener2.lastStatus());
        assertEquals(Status.FAILED, ai2.getStatus());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void errorEnded() {
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.RUNNING);
        ActionInvocation ai2 = new ActionInvocation(ad1, ai1, 0, uid1);
        ai1.addListener(listener1);
        ai2.addListener(listener2);
        ai2.setStatus(Status.ENDED);

        listener1.reset();
        listener2.reset();
        ErrorInfo e = new ErrorInfo("test", 1, "err", "error", null);
        ai2.error(e);
    }

    @Test
    public void errorFailed() {
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        ActionInvocation ai1 = new ActionInvocation(ad1, null, 0, uid1);
        ai1.setStatus(Status.RUNNING);
        ActionInvocation ai2 = new ActionInvocation(ad1, ai1, 0, uid1);
        ai1.addListener(listener1);
        ai2.addListener(listener2);
        ai2.setStatus(Status.FAILED);

        listener1.reset();
        listener2.reset();
        ErrorInfo e = new ErrorInfo("test", 1, "err", "error", null);
        ai2.error(e);
        assertEquals(0, listener2.numEvents());
        assertEquals(null, listener2.lastError());
        assertEquals(Status.FAILED, ai2.getStatus());
    }

    @Test
    public void cancelTopLevel() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        TestListener listener = new TestListener();
        ai.addListener(listener);
        ai.setStatus(Status.RUNNING);
        assertEquals(2, listener.numEvents());

        executor.cancel(ai);
        assertEquals(4, listener.numEvents());
        assertEquals(Status.FAILED, listener.lastStatus());
        assertEquals(Status.FAILED, ai.getStatus());
    }

    @Test
    public void errorTopLevel() {
        ActionInvocation ai = new ActionInvocation(ad1, null, 0, uid1);
        TestListener listener = new TestListener();
        ai.addListener(listener);
        ai.setStatus(Status.RUNNING);
        assertEquals(2, listener.numEvents());

        ErrorInfo e = new ErrorInfo("test", 1, "err", "error", null);
        ai.error(e);
        assertEquals(4, listener.numEvents());
        assertEquals(e, listener.lastError());
        assertEquals(Status.FAILED, listener.lastStatus());
        assertEquals(Status.FAILED, ai.getStatus());
    }

    private static class TestListener
            implements ActionInvocationStatusListener {
        private static int numFinalized = 0;

        private int numEvents = 0;
        private ErrorInfo lastError;
        private Status lastStatus;

        @Override
        public void error(ErrorInfo error) {
            synchronized (this) {
                numEvents++;
                lastError = error;
            }
        }

        @Override
        public void newStatus(Status newStatus) {
            synchronized (this) {
                numEvents++;
                lastStatus = newStatus;
            }
        }

        public int numEvents() {
            synchronized (this) {
                return numEvents;
            }
        }

        public Status lastStatus() {
            synchronized (this) {
                return lastStatus;
            }
        }

        public ErrorInfo lastError() {
            synchronized (this) {
                return lastError;
            }
        }

        public void reset() {
            synchronized (this) {
                numEvents = 0;
                lastError = null;
                lastStatus = null;
            }
        }

        @Override
        public void finalize() {
            numFinalized++;
        }

        public static int numFinalized() {
            return numFinalized;
        }

        public static void resetFinalized() {
            numFinalized = 0;
        }
    }

    private class TestExecutor
            implements ActionExecutor {
        @Override
        public void execute(ActionInvocation invocation)
                throws PALException {
            // Do nothing.
        }

        @Override
        public void cancel(ActionStreamEvent invocation) {
            /*
             * Not really the correct implementation, but sufficient for these
             * tests.
             */
            if (invocation instanceof ActionInvocation
                    && (invocation.getStatus() == Status.CREATED || invocation
                            .getStatus() == Status.RUNNING)) {
                ((ActionInvocation) invocation).error(new ErrorInfo("test", 5,
                        "cancel", "cancel", null));
            }
        }

        @Override
        public void executeStepped(ActionInvocation invocation)
                throws PALException {
            // Do nothing.
        }

        @Override
        public void continueStepping(ActionInvocation invocation,
                                     StepCommand command,
                                     List<Object> actionArgs)
                throws PALException {
            // Do nothing.
        }
    }
}
