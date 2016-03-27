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

// $Id: TLModule.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parent class for a supervisor which can start or stop a task learning system
 * module. Subclasses should consider overriding {@link #isRunning},
 * {@link #startNewJvm}, {@link #stop}, and {@link waitFor}. These are in
 * addition to the abstract methods which must be overridden.
 *
 * @author chris
 */
public abstract class TLModule {
    private static final Logger log = LoggerFactory.getLogger(TLModule.class);

    private static final int WAIT_TIME = 30;
    private static final String PROPERTY_PREFIX = "itl.module.";
    private static final String PROPERTY_SUFFIX = ".startup";

    private StartupMode startupMode;
    private Process childProcess;

    protected TLModule() {
        readStartupMode();
    }

    /**
     * Provides the startup mode of this module. See {@link StartupMode} for an
     * explanation of the startup modes. The startup mode can be specified and
     * overridden several ways; this call returns the highest priority of the
     * following:
     * <ol>
     * <li> {@link #getDefaultStartupMode}
     * <li>the environment variable {@link #PROPERTY_PREFIX} +
     * {@link #getModuleName} + {@link #PROPERTY_SUFFIX} -- for example,
     * &quot;itl.module.spark.startup&quot;
     * <li>the system property with the same name
     * <li>the most recent value set with {@link #setStartupMode}
     * </ol>
     *
     * @return the startup mode which will be used by this module
     */
    public final StartupMode getStartupMode() {
        return startupMode;
    }

    /**
     * Overrides the startup mode to be used for this module.
     *
     * @param newMode
     */
    public final void setStartupMode(StartupMode newMode) {
        startupMode = newMode;
        log.debug("API call overrode startup mode for " + getModuleName()
                + ": " + startupMode);
    }

    /**
     * Provides the default startup mode for this module.
     *
     * @return
     */
    protected abstract StartupMode getDefaultStartupMode();

    /**
     * Provides the name of this module. Note that the default implementation of
     * {@link #isRunning} assumes that this method returns the ITL agent name
     * used by this module. If that assumption is not true, then the subclass
     * should override {@link #isRunning}.
     *
     * @return this module's agent name
     */
    protected abstract String getModuleName();

    private void readStartupMode() {
        String name = getModuleName();

        startupMode = getDefaultStartupMode();
        log.debug("Default startup mode for " + name + ": " + startupMode);

        String propName = PROPERTY_PREFIX + name + PROPERTY_SUFFIX;
        String envValue = System.getenv(propName);
        if (envValue != null) {
            startupMode = StartupMode.valueOf(envValue);
            log.debug("Environment overrode startup mode for " + name + ": "
                    + startupMode);
        }

        String propValue = System.getProperty(propName);
        if (propValue != null) {
            startupMode = StartupMode.valueOf(propValue);
            log.debug("Property overrode startup mode for " + name + ": "
                    + startupMode);
        }
    }

    /**
     * Determines if the module is running. The default implementation queries
     * the ITL Server to determine if the service specified by
     * {@link #getModuleName} is registered. Subclasses may wish to provide a
     * more efficient implementation.
     *
     * @return
     * @throws XPSException
     *             if we can't communicate with the server
     */
    public boolean isRunning() {
        throw new RuntimeException("unimplemented");
    }

    /**
     * Wait for the module to be ready. The default implementation simply loops
     * calling {@link #isRunning}.
     *
     * @throws SpineStartupException
     */
    public void waitFor()
            throws SpineStartupException {
        for (int i = 0; i < WAIT_TIME; i++) {
            if (isRunning()) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        throw new SpineStartupException(getModuleName() + " never became ready");
    }

    /**
     * Shuts down the module. The default implementation throws an exception if
     * the module was started within this JVM, and destroys the child process
     * otherwise.
     */
    public void stop() {
        if (childProcess == null) {
            throw new IllegalStateException(
                    "Cannot shut down non-existent child process");
        } else {
            childProcess.destroy();
        }
    }

    /**
     * Start the module within this JVM.
     *
     * @throws SpineStartupException
     *             if this module cannot be started in this mode.
     */
    public abstract void startInJvm()
            throws SpineStartupException;

    /**
     * Returns the list of arguments to be sent to a new java invocation in
     * order to start this module. This list should NOT include
     * <code>java.exe</code> or similar as its first entry; finding the location
     * of the JVM is the caller's responsibility.
     *
     * @return
     */
    public abstract List<String> getStartupJvmArgs()
            throws SpineStartupException;

    /**
     * Starts the module in a new JVM. The caller is responsible for attaching
     * readers to the new process's stdout and stderr, to prevent it from
     * blocking. The default implementation calls {@link #getStartupJvmArgs} to
     * construct the arguments for the new JVM.
     *
     * @return the new child process
     * @throws SpineStartupException
     */
    public Process startNewJvm()
            throws SpineStartupException {
        List<String> args = getStartupJvmArgs();
        File javaPath = new File(System.getProperty("java.home"));
        javaPath = new File(javaPath, "bin");
        javaPath = new File(javaPath, "java");
        args.add(0, javaPath.getPath());

        ProcessBuilder builder = new ProcessBuilder(args);
        try {
            childProcess = builder.start();
            return childProcess;
        } catch (IOException e) {
            String msg = "Couldn't start " + getModuleName() + " with args: "
                    + args;
            log.error(msg, e);
            throw new SpineStartupException(msg, e);
        }
    }

    /**
     * Performs any necessary setup which the module requires in order to start
     * up the first time. The default implementation does nothing.
     *
     * @throws IOException
     */
    public abstract void setup()
            throws SpineStartupException;
}
