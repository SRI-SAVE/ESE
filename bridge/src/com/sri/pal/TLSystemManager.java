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

package com.sri.pal;

import com.sri.tasklearning.lapdogController.LapdogClientProcessManager;
import com.sri.tasklearning.lumenpal.LumenProcess;
import com.sri.tasklearning.spine.ModuleManager;
import com.sri.tasklearning.spine.TLModule;
import com.sri.tasklearning.spine.impl.jms.JmsSpineProcessManager;

/**
 * Utility class used internally to start and manage different components of the
 * ITL system: the server, spark and the lapdog client.
 *
 * @author jaswal, chris
 */
public class TLSystemManager {
    private static boolean startedAllServices = false;

    public static void ensureStarted()
            throws PALException {
        if (!startedAllServices) {
            JmsSpineProcessManager server = new JmsSpineProcessManager();
            LapdogClientProcessManager lapdog = new LapdogClientProcessManager();
            LumenProcess lumen = new LumenProcess();

            TLModule modules[] = new TLModule[] { server, lapdog, lumen };
            ModuleManager mm = new ModuleManager(modules);
            try {
                mm.startAll();
            } catch (Exception e) {
                throw new PALException("Unable to start ITL services", e);
            }
            startedAllServices = true;
        }
    }

    // WARNING: do not use this method unless module startup is being handled
    // elsewhere (ie the shell)
    public static void setStarted() {
        startedAllServices = true;
    }
}
