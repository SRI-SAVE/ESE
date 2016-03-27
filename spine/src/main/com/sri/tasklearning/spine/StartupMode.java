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

// $Id: StartupMode.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine;

/**
 * Defines the ways a module can be started.
 *
 * @author chris
 */
public enum StartupMode {
    /**
     * The module is already running. Its status will be verified; but if it
     * isn't running, no attempt will be made to start it. We will wait a brief
     * time for it to start up, and if it doesn't appear then we'll give up and
     * throw an exception.
     */
    PREEXISTING,

    /**
     * No separate process will be started; rather, the module will be run in
     * its own thread within this JVM.
     */
    IN_JVM,

    /**
     * The module will be started in a new JVM, and all of its output will be
     * captured by this process and logged.
     */
    CHILD_JVM,

    /**
     * The module will be started in a new JVM, with its own shell window for
     * any console output it may generate.
     */
    NEW_CONSOLE
}
