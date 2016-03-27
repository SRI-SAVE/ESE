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

// $Id: MultiThreadReadWriteLock.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a stripped-down implementation of {@link ReadWriteLock} which doesn't
 * enforce thread ownership of a given lock. Essentially, this is a reference
 * counting class.
 *
 * @author chris
 */
public class MultiThreadReadWriteLock
        implements ReadWriteLock {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ReadLock readLock;
    private final WriteLock writeLock;
    private int readLockCount;
    private int writeLockCount;

    private static int staticSerial = 0;
    private int serial = staticSerial++;

    public MultiThreadReadWriteLock() {
        readLock = new ReadLock();
        writeLock = new WriteLock();
        readLockCount = 0;
        writeLockCount = 0;
    }

    @Override
    public Lock readLock() {
        return readLock;
    }

    @Override
    public Lock writeLock() {
        return writeLock;
    }

    private synchronized boolean tryReadLock() {
        log.debug("{}: {}+{} tryReadLock", new Object[] { serial,
                readLockCount, writeLockCount });
        if (writeLockCount > 0) {
            return false;
        }
        readLockCount++;
        return true;
    }

    private synchronized boolean tryWriteLock() {
        log.debug("{}: {}+{} tryWriteLock", new Object[] { serial,
                readLockCount, writeLockCount });
        if (readLockCount > 0) {
            return false;
        }
        if (writeLockCount > 0) {
            return false;
        }
        writeLockCount = 1;
        return true;
    }

    private synchronized void releaseReadLock() {
        log.debug("{}: {}+{} readUnlock", new Object[] { serial,
                readLockCount, writeLockCount });
        if (readLockCount <= 0) {
            throw new IllegalStateException("Not locked");
        }
        readLockCount--;
        notifyAll();
    }

    private synchronized void releaseWriteLock() {
        log.debug("{}: {}+{} writeUnlock", new Object[] { serial,
                readLockCount, writeLockCount });
        if (writeLockCount <= 0) {
            throw new IllegalStateException("Not locked");
        }
        writeLockCount--;
        notifyAll();
    }

    private class ReadLock
            extends AbstractLock {
        @Override
        public boolean tryLock() {
            return tryReadLock();
        }

        @Override
        public void unlock() {
            releaseReadLock();
        }
    }

    private class WriteLock
            extends AbstractLock {
        @Override
        public boolean tryLock() {
            return tryWriteLock();
        }

        @Override
        public void unlock() {
            releaseWriteLock();
        }
    }

    private abstract class AbstractLock
            implements Lock {
        @Override
        public void lock() {
            Object monitor = MultiThreadReadWriteLock.this;
            synchronized (monitor) {
                while (!tryLock()) {
                    log.debug("{}: {}+{} waiting", new Object[] { serial,
                            readLockCount, writeLockCount });
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        // Ignore.
                    }
                }
            }
        }

        @Override
        public Condition newCondition() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public boolean tryLock(long time,
                               TimeUnit unit) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public void lockInterruptibly() {
            throw new RuntimeException("not implemented");
        }
    }
}
