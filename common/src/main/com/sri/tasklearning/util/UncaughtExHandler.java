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

// $Id: UncaughtExHandler.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.util;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class gets used to handle uncaught throwables. If the throwable is a
 * subclass of {@link java.lang.Error}, this class will abort the entire JVM by
 * calling {@link System#exit}.
 */
public class UncaughtExHandler
        implements UncaughtExceptionHandler {
    private static final Logger log = LoggerFactory
            .getLogger(UncaughtExHandler.class);

    @Override
    public void uncaughtException(Thread t,
                                  Throwable e) {
        if (e instanceof Error) {
            log.error("Uncaught error in " + t.getName(), e);
            throw ((Error) e);
        } else {
            log.warn("Uncaught exception in " + t.getName(), e);
        }
    }
}
