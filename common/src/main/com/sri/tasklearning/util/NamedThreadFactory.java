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

// $Id: NamedThreadFactory.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.util;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory
        implements ThreadFactory {
    private final ThreadGroup group;
    private final String baseName;
    private final Thread.UncaughtExceptionHandler exHandler;
    private int threadNum;

    public NamedThreadFactory(String name) {
        threadNum = 0;
        baseName = name;
        group = new ThreadGroup(baseName);
        exHandler = new UncaughtExHandler();
    }

    public NamedThreadFactory(Class<?> class1) {
        this(class1.getSimpleName());
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r);
        t.setDaemon(true);
        t.setName(baseName + " thread " + threadNum++);
        t.setUncaughtExceptionHandler(exHandler);
        return t;
    }
}
