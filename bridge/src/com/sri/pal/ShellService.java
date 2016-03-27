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

import java.util.List;

/**
 * Used internally by the PAL system. An access point for the bridge/CPOF to
 * send a shutdown notice to the shell and to retrieve a list of names of
 * modules that are currently running.
 *
 * @author Tim Holland
 */
public interface ShellService {
    /**
     * Internally used constant. This is the name the PAL Shell will be known by
     * in the ITL system.
     */
    public static final String ITL_SHELL_NAME = "shell";

    public enum Modules {
        LUMEN("lumen"), LAPDOG("lapdog-client"), SPINE("spine"), SINGLE_LOCK("singleLock"), REMOTEXML("remotexml");
        private Modules(String n) {
            name = n;
        }

        private final String name;

        @Override
        public String toString() {
            if(this.name == null) {
                return super.toString();
            }
            return name;
        }

        public static Modules valueOfName(String n) {
            for(Modules m : Modules.values()) {
                if(m.toString().equals(n)) {
                    return m;
                }
            }
            throw new IllegalArgumentException("Unknown name: "+n);
        }
    };
    public List<Modules> listRunningModules();

    public void shutdown();
}
