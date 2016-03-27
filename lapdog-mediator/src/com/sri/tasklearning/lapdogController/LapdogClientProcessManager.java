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

package com.sri.tasklearning.lapdogController;

import java.util.List;
import java.util.Vector;

import com.sri.tasklearning.spine.SpineStartupException;
import com.sri.tasklearning.spine.StartupMode;
import com.sri.tasklearning.spine.TLModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start the internal or external version of Lapdog client.
 */
public class LapdogClientProcessManager
        extends TLModule {
    private static final Logger log = LoggerFactory
            .getLogger(LapdogClientProcessManager.class);

    static final String LAPDOG_MODULE_NAME = "LAPDOG";

    private LapdogClient client = null;

    @Override
    public boolean isRunning() {
        if(client == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected StartupMode getDefaultStartupMode() {
        return StartupMode.IN_JVM;
    }

    @Override
    protected String getModuleName() {
        return LAPDOG_MODULE_NAME;
    }

    @Override
    public List<String> getStartupJvmArgs()
            throws SpineStartupException {
        List<String> args = new Vector<String>();

        args.add(LapdogClient.class.getName());

        return args;
    }

    @Override
    public void startInJvm()
            throws SpineStartupException {
        startInternally();
    }

    public void startInternally() {
        try {
            client = LapdogClient.start();
        } catch (final Exception e) {
            // Controller initialization is not recoverable
            log.error("Failed to initialize LAPDOG, shutting down:", e);
        }
    }

    @Override
    public void stop() {
        client.shutdown();
    }

    @Override
    public void setup()
            throws SpineStartupException {
    }
}
