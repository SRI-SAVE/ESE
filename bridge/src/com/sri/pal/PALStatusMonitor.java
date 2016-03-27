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
package com.sri.pal;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sri.pal.PALStatusListener.Status;

import com.sri.tasklearning.spine.util.SpineStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors the status of the task learning (also known as PAL or Adept)
 * components and reports changes.
 */
public class PALStatusMonitor {
    private static final Logger log = LoggerFactory
            .getLogger(PALStatusMonitor.class);

    /**
     * How frequently (in ms) the backend will be checked to see if it's online.
     */
    public static final long CHECK_FREQ = 1000;

    /**
     * After how many ms should we assume that not hearing from the Shell
     * watchdog means it's offline?
     */
    public static final long DEFAULT_PAL_BACKEND_TIMEOUT = 5 * 1000;
    public static final String PAL_BACKEND_TIMEOUT_PROPERTY = "PAL.ShellPingDelayTimeout";
    private static long palBackEndTimeout;
    static {
        palBackEndTimeout = DEFAULT_PAL_BACKEND_TIMEOUT;
        String overrideTimeout = System
                .getProperty(PAL_BACKEND_TIMEOUT_PROPERTY);
        if (overrideTimeout != null) {
            palBackEndTimeout = Long.parseLong(overrideTimeout);
            if (palBackEndTimeout < 0) {
                log.warn("failed to parse {} as long; defaulting to {}",
                        overrideTimeout, DEFAULT_PAL_BACKEND_TIMEOUT);
                palBackEndTimeout = DEFAULT_PAL_BACKEND_TIMEOUT;
            } else {
                log.info("Changing backend ping timeout from {} to {}",
                        DEFAULT_PAL_BACKEND_TIMEOUT, palBackEndTimeout);
            }
        }
    }

    private static List<PALStatusListener> listeners = new CopyOnWriteArrayList<PALStatusListener>();
    private static volatile Status status = Status.UNKNOWN;
    private static MonitorThread monitor = null;
    private static TimerTask task;
    private static Timer timer = new Timer(
            PALStatusMonitor.class.getSimpleName() + " timer", true);
    private static SpineStatus spineStatus;

    /**
     * Checks to see if the PAL learning components are running.
     *
     * @return {@code true} if PAL is up
     */
    public static boolean isTaskLearningRunning() {
        if (spineStatus == null ) {
            spineStatus = new SpineStatus();
        }
        boolean running = spineStatus.isMasterRunning();
        if (running) {
            log.trace("PAL is running");
        }
        else {
            log.debug("PAL is not running");
        }
        return running;
    }

    /**
     * Checks to see if the learning components are running.
     * Does the same thing as isTaskLearningRunning(), but is a nicer
     * name for Adept clients to use and understand.
     *
     * @return {@code true} if task learning core components are up
     *   and in a state they can be connected to by a client application.
     */
    public static boolean isAdeptRunning() {
        return isTaskLearningRunning();
    }

    /**
     * Adds a new listener which will receive updates as to the status of the
     * task learning backend.
     *
     * @param newListener
     *            the listener to add
     */
    public static void addListener(PALStatusListener newListener) {
        log.debug("Adding listener {}", newListener);

        deliverStatus(newListener);

        synchronized(listeners) {
            checkRunning();
            listeners.add(newListener);
        }
    }

    public static boolean removeListener(PALStatusListener newListener) {
        log.debug("Removing listener {}", newListener);

        synchronized(listeners){
            boolean result = listeners.remove(newListener);
            maybeStop();
            return result;
        }
    }

    private static void deliverStatus(PALStatusListener listener) {
        try {
            log.trace("delivering {} to {}", status, listener);
            listener.newStatus(status);
            log.trace("finished delivering {} to {}", status, listener);
        } catch (Exception e) {
            log.warn(
                    "PALStatusListener " + listener + " ("
                            + listener.getClass() + ") threw exception", e);
        }
    }

    private static void checkRunning() {
        if (monitor == null) {
            monitor = new MonitorThread();
            Thread thread = new Thread(monitor);
            thread.setDaemon(true);
            thread.setName(PALStatusMonitor.class.getSimpleName());
            thread.start();
        }
    }

    private static void maybeStop() {
        if (listeners.isEmpty()) {
            cancelTimer();
            monitor.quit();
        }
    }

    static void currentStatus(Status newStatus) {
        switch (newStatus) {
        case DOWN:
            cancelTimer();
            newStatus(newStatus);
            break;
        case WATCHDOG_UP:
            cancelTimer();
            newStatus(newStatus);
            break;
        case UP:
            if (status == Status.WATCHDOG_UP) {
                startTimer(newStatus);
            } else {
                newStatus(newStatus);
            }
            break;
        default:
            log.error("Unknown status {}", newStatus);
        }
    }

    private static void newStatus(Status newStatus) {
        if (newStatus == status) {
            return;
        }
        log.debug("Delivering new status {} to listeners: {}", newStatus,
                listeners);

        status = newStatus;
        for (PALStatusListener l : listeners) {
            deliverStatus(l);
        }
    }

    private static void startTimer(final Status status) {
        synchronized (timer) {
            if (task != null) {
                return;
            }
            cancelTimer();
            task = new TimerTask() {
                @Override
                public void run() {
                    newStatus(status);
                }
            };
            timer.schedule(task, palBackEndTimeout);
        }
    }

    private static void cancelTimer() {
        synchronized (timer) {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
    }

    private static class MonitorThread
            implements Runnable {
        private boolean run = true;

        @Override
        public void run() {
            while (run) {
                if (isTaskLearningRunning()) {
                    currentStatus(Status.UP);
                } else {
                    currentStatus(Status.DOWN);
                }
                try {
                    Thread.sleep(CHECK_FREQ);
                } catch (InterruptedException e) {
                    log.warn("Interrupted.", e);
                }
            }
        }

        public void quit() {
            run = false;
            synchronized (MonitorThread.this) {
                MonitorThread.this.notifyAll();
            }
        }
    }
}
