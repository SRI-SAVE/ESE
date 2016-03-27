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

// $Id: ModuleManager.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Class responsible for overseeing the startup and shutdown of other ITL
 * modules. The individual modules are controlled by respective classes which
 * extend {@link TLModule}.
 *
 * @author chris
 */
public class ModuleManager {
    private static final Logger log = LoggerFactory
            .getLogger(ModuleManager.class);

    private TLModule[] modules;

    public ModuleManager(TLModule... modules) {
        this.modules = modules;
    }

    public void startAll()
            throws SpineStartupException {
        addShutdownHook();
        for (TLModule module : modules) {
            start(module);
        }
    }

    private void start(TLModule module)
            throws SpineStartupException {
        module.setup();

        StartupMode mode = module.getStartupMode();
        switch (mode) {
        case PREEXISTING:
            if (!module.isRunning()) {
                throw new SpineStartupException("ITL module "
                        + module.getModuleName()
                        + " is expected to be running, but it isn't");
                // TODO If it's not running, give it a few seconds and check
                // again.
            }
            break;

        case IN_JVM:
            if (module.isRunning()) {
                log.warn(module.getModuleName()
                        + " is already running; not starting");
            } else {
                log.info("Starting " + module.getModuleName() + " in this JVM");
                module.startInJvm();
                module.waitFor();
            }
            break;

        case CHILD_JVM:
            if (module.isRunning()) {
                log.warn(module.getModuleName()
                        + " is already running; not starting");
            } else {
                log.info("Starting " + module.getModuleName()
                                + " in a new JVM");
                startNewJvm(module);
                module.waitFor();
            }
            break;

        case NEW_CONSOLE:
            if (module.isRunning()) {
                log.warn(module.getModuleName()
                        + " is already running; not starting");
            } else {
                log.info("Starting " + module.getModuleName()
                        + " in a new console");
                startInConsole(module);
                module.waitFor();
            }
            break;

        default:
            throw new SpineStartupException("Unknown startup mode " + mode
                    + " for module " + module);
        }
    }

    private void startNewJvm(TLModule module)
            throws SpineStartupException {
        Process process = module.startNewJvm();
        Logger log = LoggerFactory.getLogger(module.getModuleName());
        ProcessMonitor monitor = new ProcessMonitor(process, log, module
                .getModuleName()
                + ": ");
        monitor.start();
    }

    private void startInConsole(TLModule module)
            throws SpineStartupException {
        // "cmd.exe /c start java args"
        List<String> args = module.getStartupJvmArgs();

        File javaPath = new File(System.getProperty("java.home"));
        javaPath = new File(javaPath, "bin");
        javaPath = new File(javaPath, "java");
        args.add(0, javaPath.getPath());

        args.add(0, "start");
        args.add(0, "/c");
        args.add(0, "cmd.exe");

        ProcessBuilder builder = new ProcessBuilder(args);
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            String msg = "Couldn't start process with args: " + args;
            log.error(msg, e);
            throw new SpineStartupException(msg, e);
        }
        Logger log = LoggerFactory.getLogger(module.getModuleName()
                + " CONSOLE");
        ProcessMonitor monitor = new ProcessMonitor(process, log, module
                .getModuleName()
                + " CONSOLE: ");
        monitor.start();
    }

    private void addShutdownHook() {
        Thread t = new Thread() {
            @Override
            public void run() {
                for (int i = modules.length - 1; i >= 0; i--) {
                    TLModule module = modules[i];
                    try {
                        module.stop();
                    } catch (Exception e) {
                        log.warn("Exception shutting down module " + module, e);
                    }
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(t);
    }
}
