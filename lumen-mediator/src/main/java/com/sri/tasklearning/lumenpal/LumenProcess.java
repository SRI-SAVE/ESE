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

package com.sri.tasklearning.lumenpal;

import java.util.List;
import java.util.Vector;

import com.sri.ai.lumen.runtime.LumenConnection;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineConstants;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.SpineStartupException;
import com.sri.tasklearning.spine.StartupMode;
import com.sri.tasklearning.spine.TLModule;
import com.sri.tasklearning.spine.impl.jms.JmsClient;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used by the Bridge to launch the Lumen service. It extends the
 * TLModule class that defines how a service to the Task Learning system should
 * behave.
 */
public class LumenProcess extends TLModule {
    private static final Logger log = LoggerFactory.getLogger(LumenProcess.class);
    public static final String LUMEN_AGENT_NAME = "lumen";

    private LumenClient lumenClient;
    private Spine spine;

    /**
     * Shutdown the Lumen JmsSpine Instance
     */
    @Override
    public void stop() {
        lumenClient.shutdown();
        lumenClient = null;
    }

    /**
     * Check to see if Lumen is already running
     * @return true if lumen is running
     */
    @Override
    public boolean isRunning() {
        if (lumenClient == null || spine == null || !spine.isRunning()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Query the default startup mode
     * @return the default startup mode for lumen
     */
    @Override
    protected StartupMode getDefaultStartupMode() {
        return StartupMode.IN_JVM;
    }

    /**
     * Get the module name
     * @return the module name (lumen)
     */
    @Override
    protected String getModuleName() {
        return LUMEN_AGENT_NAME;
    }

    /**
     * Get the startup arguments for the JVM
     * @return the Lumen class name
     * @throws SpineStartupException
     */
    @Override
    public List<String> getStartupJvmArgs() throws SpineStartupException {
        List<String> args = new Vector<String>();
        args.add(LumenConnection.class.getName());
        return args;
    }

    /**
     * Initiate Lumen in the current JVM
     * @throws SpineStartupException if there is a problem
     */
    @Override
    public void startInJvm() throws SpineStartupException {
        startInternally();
    }

    /**
     * Start Lumen in the current JVM
     */
    private void startInternally() {
        log.debug("Start internally called.");

        try {
            LumenFacade lumenFacade = new LumenFacadeImpl(LumenConnection
                    .getInstance());
            spine = new JmsSpine(JmsClient.REMOTE, SpineConstants.LUMEN_MEDIATOR);
            lumenClient = new LumenClient(lumenFacade, spine);
            lumenClient.start();
        }
        catch (SpineException e) {
            log.error("Problem starting the Lumen process: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void setup()
            throws SpineStartupException {
    }
}
