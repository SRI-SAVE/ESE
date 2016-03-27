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

// $Id: InstrumentationControl.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

/**
 * Receives events concerning the state of cross-application instrumentation.
 * This is needed when the application driving the learning process is
 * interested in user actions performed in a different application. At least two
 * implementations of this class should be present: The application's
 * implementation and PAL's implementation.
 * <p>
 * The application's implementation receives notification from PAL that it
 * should start or stop reporting user actions, also known as instrumentation.
 * When instrumentation is enabled by receipt of a start event, the application
 * should create an {@code ActionInvocation} by calling
 * {@link ActionDef#invoke} to track every user action performed. This object
 * must be created before anything is done which another application could
 * respond to with its own instrumentation. The application is then responsible
 * for setting the status of the resultant {@code ActionInvocation} by calling
 * {@link ActionInvocation#setStatus}. Specifically, the {@code RUNNING} state
 * should be set after all input parameters have been assigned and the action is
 * executing. The {@code ENDED} or {@code FAILED} state should be set once the
 * action has completed. The application must register its implementation of
 * this interface using {@link Bridge#addApplicationInstrumentation}.
 * <p>
 * The PAL implementation of this interface can be used by the application to
 * control instrumentation in all currently running, PAL enabled applications.
 * Calling {@link #startWatching} on this object will result in instrumentation
 * being enabled globally, if it is not already. That, in turn, will result in
 * all registered implementations of this class having their
 * {@code startWatching} methods called. Once the application is done observing
 * instrumented actions, it should call {@link #stopWatching} to disable
 * instrumentation.
 * <p>
 * An interested party may observe instrumented actions by way of its registered
 * {@link GlobalActionListener}.
 *
 * @author chris
 */
public interface InstrumentationControl {
    public void startWatching();

    public void stopWatching();
}
