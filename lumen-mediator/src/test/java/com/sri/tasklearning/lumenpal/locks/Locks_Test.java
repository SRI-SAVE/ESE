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

// $Id: Locks_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal.locks;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.concurrent.locks.Lock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class Locks_Test {
    private MultiThreadReadWriteLock rwl;
    private boolean fail;

    @BeforeMethod
    public void setup() {
        rwl = new MultiThreadReadWriteLock();
        fail = false;
    }

    @Test
    public void writeExcludesRead()
            throws Exception {
        Lock writeLock = rwl.writeLock();
        assertTrue(writeLock.tryLock());
        final Lock readLock = rwl.readLock();

        Thread t = new Thread() {
            @Override
            public void run() {
                readLock.lock();
                fail = true;
            }
        };
        t.start();

        Thread.sleep(1000);
        assertFalse(fail);
    }
}
