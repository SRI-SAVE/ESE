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

import java.util.concurrent.locks.Lock;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.decl.ATRSigDecl;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.lumenpal.mock.MockLumen;
import com.sri.tasklearning.lumenpal.util.LumenMediatorTestCase;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.spine.util.ATRTestUtil;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LockingActionModel_Test extends LumenMediatorTestCase {

    private MockLumen mockLumen;
    private LumenTypeAdder adder;
    private ATRSigDecl spineAction;
    private SimpleTypeName spineActionTypeName;
    private boolean removedAtLeastOnce = false;

    @BeforeMethod
    public void setUp() {
        mockLumen = new MockLumen();
        adder = new LumenTypeAdder(mockLumen);

        spineActionTypeName = (SimpleTypeName) TypeNameFactory.makeName("SpineAction");
        spineAction = ATRTestUtil.makeAction(spineActionTypeName,
                new ATRParameter[0], null, null);

    }

    @Test
    public void canConstruct() {
        LockingActionModel lockingActionModel = new LockingActionModel(
                adder);
        assertNotNull(lockingActionModel);
        assertTrue(LockingActionModel.class.isInstance(lockingActionModel));
    }

    @Test
    public void typeReadLockisProperLocked() throws Exception {
        LockingActionModel lockingActionModel = new LockingActionModel(
                adder);

        // Lock the type name
        Lock lock = lockingActionModel.getReadLock(spineActionTypeName);
        // Add the type to the action model
        lockingActionModel.add(spineAction);

        // now lets diabolically try removing it to test the lock
        lockingActionModel.maybeRemove(spineActionTypeName);

        assertNotNull(lockingActionModel.getRaw(spineActionTypeName));

        lock.unlock();
    }

    @Test
    public void typeCanBeRemovedWhenUnlocked() throws Exception {
        LockingActionModel lockingActionModel = new LockingActionModel(
                adder);

        // Lock the type name
        Lock lock = lockingActionModel.getReadLock(spineActionTypeName);
        // Add the type to the action model
        lockingActionModel.add(spineAction);

        // Unlock the type
        lock.unlock();

        // now lets reasonably try removing it to test the lock
        boolean success = lockingActionModel.maybeRemove(spineActionTypeName);
        if (success) {
            assertNull(lockingActionModel.getRaw(spineActionTypeName));
        }
    }

    @Test
    public void lockingCanStayLockedWithMultiThreadedPounding() throws InterruptedException {
        // Spawn a load of these adder remover threads and see if we can get it to
        // hit an NPE
        int iterations = 300;
        for (int i = 0; i < iterations; i++) {
            new TypeAdderLockerAndRemover().run();
        }
        // Sleep to let the threads finish, then lets make sure we can remove the type
        Thread.sleep(500);
        LockingActionModel lockingActionModel = new LockingActionModel(
                adder);
        boolean success = lockingActionModel.maybeRemove(spineActionTypeName);
        if (!success) {
            // If we could not remove it, the listener may have already cleaned it up - lets
            // check to be sure
            ATRDecl type = lockingActionModel.getRaw(spineActionTypeName);
            int wait = 0;
            int patience = 5;
            while (type != null && wait < patience) {
                lockingActionModel.maybeRemove(spineActionTypeName);
                wait++;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {

                }
                type = lockingActionModel.getRaw(spineActionTypeName);
            }
            if (type == null)
                removedAtLeastOnce = true;
        }

        assertTrue(removedAtLeastOnce);
    }

    class TypeAdderLockerAndRemover implements Runnable {

        @Override
        public void run() {
            randomSleep();
            LockingActionModel lockingActionModel = new LockingActionModel(
                    adder);

            // Lock the type name
            randomSleep();
            Lock lock = lockingActionModel.getReadLock(spineActionTypeName);

            // Add the type to the action model
            try {
                randomSleep();
                lockingActionModel.add(spineAction);
                randomSleep();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Make sure the lock is still working for a bit
            assertNotNull(lockingActionModel.getRaw(spineActionTypeName));
            randomSleep();
            assertNotNull(lockingActionModel.getRaw(spineActionTypeName));
            randomSleep();
            assertNotNull(lockingActionModel.getRaw(spineActionTypeName));

            // Unlock the type
            lock.unlock();

            // now lets reasonably try removing it to test the lock
            lockingActionModel.maybeRemove(spineActionTypeName);
        }

        private void randomSleep() {
            try {
                long sleepies = System.currentTimeMillis() % 10;
                Thread.sleep(sleepies);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
