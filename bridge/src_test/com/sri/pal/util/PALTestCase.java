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

/* $Id: PALTestCase.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $ */
package com.sri.pal.util;

import com.sri.tasklearning.util.LogUtil;
import com.sri.tasklearning.util.PALRunListener;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;

/**
 * Base class for functional tests. Adds some prettiness to the output to help
 * differentiate outputs from different tests in the same class
 */
@Listeners(PALRunListener.class)
public abstract class PALTestCase {
    public static final String LOG_CONFIG_BASE = "bridge";
    public static final String LOG_CONFIG_QUIET = "quiet";

    @BeforeClass
    public static void palTestCaseInit() {
        LogUtil.configureLogging(PALTestCase.LOG_CONFIG_BASE, PALTestCase.class);
        System.setProperty("lapdog.output.directory", "lapdog_logs");
    }
}
