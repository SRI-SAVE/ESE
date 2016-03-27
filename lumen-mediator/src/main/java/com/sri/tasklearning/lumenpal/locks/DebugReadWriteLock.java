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

// $Id: DebugReadWriteLock.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
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
public class DebugReadWriteLock
        implements ReadWriteLock {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Object lockedObject;
    private int readLockCount;
    private int writeLockCount;

    private static int staticSerial = 0;
    private int serial = staticSerial++;

    public DebugReadWriteLock(Object lockedObject) {
        this.lockedObject = lockedObject;
        readLockCount = 0;
        writeLockCount = 0;
    }

    @Override
    public Lock readLock() {
        return new ReadLock();
    }

    @Override
    public Lock writeLock() {
        return new WriteLock();
    }

    private synchronized boolean tryReadLock() {
        if (writeLockCount > 0) {
            return false;
        }
        readLockCount++;
        return true;
    }

    private synchronized boolean tryWriteLock() {
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
        if (readLockCount <= 0) {
            throw new IllegalStateException("Not locked");
        }
        readLockCount--;
        notifyAll();
    }

    private synchronized void releaseWriteLock() {
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
            log.debug("{}: {}+{} tryReadLock", new Object[] { serial,
                    readLockCount, writeLockCount });
            if (tryReadLock()) {
                localLock();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void unlock() {
            log.debug("{}: {}+{} readUnlock", new Object[] { serial,
                    readLockCount, writeLockCount });
            releaseReadLock();
            localUnlock();
        }
    }

    private class WriteLock
            extends AbstractLock {
        @Override
        public boolean tryLock() {
            log.debug("{}: {}+{} tryWriteLock", new Object[] { serial,
                    readLockCount, writeLockCount });
            if (tryWriteLock()) {
                localLock();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void unlock() {
            log.debug("{}: {}+{} writeUnlock", new Object[] { serial,
                    readLockCount, writeLockCount });
            releaseWriteLock();
            localUnlock();
        }
    }

    private abstract class AbstractLock
            implements Lock {
        protected final StackTraceElement[] creationStack;
        private int lockCount;

        protected AbstractLock() {
            creationStack = Thread.currentThread().getStackTrace();
            lockCount = 0;
        }

        @Override
        public void lock() {
            Object monitor = DebugReadWriteLock.this;
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

        protected void localLock() {
            lockCount++;
            if (lockCount > 1) {
                String msg = "Lock " + this + " was locked " + lockCount
                        + " times";
                Exception e = new Exception();
                log.warn(msg, e);
                e = new Exception("Stack trace at lock creation");
                e.setStackTrace(creationStack);
                log.warn(msg, e);
            }
        }

        protected void localUnlock() {
            lockCount--;
            if (lockCount < 0) {
                String msg = "Lock "
                        + this
                        + " was unlocked too many times."
                        + " A different instance should have been unlocked instead.";
                Exception e = new Exception();
                log.warn(msg, e);
                e = new Exception("Stack trace at lock creation");
                e.setStackTrace(creationStack);
                log.warn(msg, e);
            }
        }

        @Override
        protected void finalize() {
            if (lockCount > 0) {
                Exception e = new Exception("Stack trace at lock creation");
                e.setStackTrace(creationStack);
                log.warn("Unlock was never called on " + this, e);
                while (lockCount > 0) {
                    unlock();
                }
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " of lock for " + lockedObject;
        }
    }
}
