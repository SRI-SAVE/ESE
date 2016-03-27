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

/**
 * Listener interface for receiving updates on the status of the overall PAL
 * system. A class implementing this interface can register to receive updates
 * using {@link PALStatusMonitor#addListener}.
 *
 * @author chris
 */
public interface PALStatusListener {
    /**
     * Notifies the listener that the PAL system has changed states. This will
     * also be called immediately when a new listener is registered.
     *
     * @param status
     *            the current state of the PAL system
     */
    public void newStatus(Status status);

    /**
     * Enumeration of the states the PAL system can be in.
     *
     * @author chris
     */
    public enum Status {
        /**
         * The PAL components haven't reported in (yet).
         */
        UNKNOWN,
        /**
         * The task learning communication server is online. There is reason to
         * believe the rest of the task learning system is functioning.
         */
        UP,
        /**
         * The PAL components are no longer available.
         */
        DOWN,
        /**
         * Not only is the communication server online, but a watchdog is
         * running and reporting positive status information. The watchdog is
         * not present in every configuration of the PAL system, so for most
         * purposes {@code UP} is sufficient rather than this value.
         */
        WATCHDOG_UP
    };
}
