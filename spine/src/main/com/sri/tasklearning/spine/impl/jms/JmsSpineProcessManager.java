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

package com.sri.tasklearning.spine.impl.jms;

import java.util.List;
import java.util.Vector;

import com.sri.tasklearning.spine.SpineConstants;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.SpineStartupException;
import com.sri.tasklearning.spine.StartupMode;
import com.sri.tasklearning.spine.TLModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends the TLModule class which specifies the methods that must
 * be implemented in order for this class to be started by the ModuleManager.
 * Each of ths clients in the PALCPOF system are started by the Module Manager
 * in this way.
 */
public class JmsSpineProcessManager extends TLModule {

    private static final Logger log = LoggerFactory.getLogger(JmsSpineProcessManager.class);
    final StartupMode SPINE_STARTUP_MODE = StartupMode.IN_JVM;
    static final String SPINE_MODULE_NAME = "SPINE";
    private JmsSpine jmsSpine;

    /**
     * Performs any necessary setup which the module requires in order to start
     * up the first time.
     * @throws SpineStartupException if there is a problem
     */
    @Override
    public void setup() throws SpineStartupException {
        setStartupMode(SPINE_STARTUP_MODE);
    }

    /**
     * Shuts down the JmsSpine Instance
     */
    @Override
    public void stop() {
        try {
            log.debug("About to shut down Shell spine");
            if (jmsSpine != null) {
                jmsSpine.shutdown(true);
                jmsSpine = null;
            }
        } catch (Exception e) {
            log.info("Shutting down the Shell JmsSpine Instance experienced difficulty: {}", e);
        }
    }

    /**
     * How this client should be started by default - for the
     * spine this will be IN_JVM due to the fact that it lives with
     * Lumen and Lapdog on the PAL server
     * @return the default start up mode
     */
    protected StartupMode getDefaultStartupMode() {
        return SPINE_STARTUP_MODE;
    }

    /**
     * The module name for this system client
     * @return the spine module name
     */
    protected String getModuleName() {
        return SPINE_MODULE_NAME;
    }

    /**
     * Called to initiate the in jvm spine startup procedure
     * @throws SpineStartupException
     */
    public void startInJvm() throws SpineStartupException {
        log.debug("Starting the JMS Spine module");
        try {
            jmsSpine = new JmsSpine(JmsClient.LOCAL, SpineConstants.MASTER_SPINE);
        } catch (SpineException e) {
            throw new SpineStartupException(e);
        }
    }

    /**
     * Provide arguments needed
     * @return the arguments for starting the spine
     * @throws SpineStartupException
     */
    public List<String> getStartupJvmArgs() throws SpineStartupException {
        return new Vector<String>();
    }

    public JmsSpine getSpine() {
        return jmsSpine;
    }

    /**
     * This method allows other modules to query whether the spine is running
     * @return true if running
     */
    @Override
    public boolean isRunning() {
        return jmsSpine != null && jmsSpine.isRunning();
    }
}
