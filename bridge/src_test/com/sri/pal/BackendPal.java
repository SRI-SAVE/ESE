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

// $Id: BackendPal.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.ProcessMonitor;
import com.sri.tasklearning.util.LogUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendPal {
    public static void main(String args[])
            throws PALException {
        LogUtil.configureLogging(PALTestCase.LOG_CONFIG_BASE, PALTestCase.class);
        Bridge.startPAL();
    }

    public static void start()
            throws IOException {
        List<String> args = new ArrayList<String>();

        File javaPath = new File(System.getProperty("java.home"));
        javaPath = new File(javaPath, "bin");
        javaPath = new File(javaPath, "java");
        args.add(javaPath.getPath());

        args.add("-classpath");

        String classpath = System.getProperty("java.class.path");
        args.add(classpath);

        args.add(BackendPal.class.getCanonicalName());

        ProcessBuilder builder = new ProcessBuilder(args);
        File dir = new File("backend");
        dir.mkdir();
        builder.directory(dir);
        Process process = builder.start();
        Logger log = LoggerFactory.getLogger(BackendPal.class.getSimpleName());
        ProcessMonitor monitor = new ProcessMonitor(process, log,
                BackendPal.class.getSimpleName() + ": ");
        monitor.start();
    }
}
