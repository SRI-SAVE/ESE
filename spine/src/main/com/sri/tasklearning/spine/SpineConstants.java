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

// $Id: SpineConstants.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine;

/**
 * Holds various constants that are useful for Spine clients to refer to.
 */
public class SpineConstants {
    /**
     * The name of the Spine client owned by the Bridge instance which is
     * started by the CPOF Shell.
     */
    public static final String SHELL_BRIDGE = "ShellBridge";

    /**
     * The name of the master (aka local) Spine. In CPOF, this is started by the
     * CPOF Shell and used directly by the Shell.
     */
    public static final String MASTER_SPINE = "Master";

    /**
     * The name of the Spine client owned by the Lumen Mediator.
     */
    public static final String LUMEN_MEDIATOR = "Lumen-mediator";

    /**
     * The name of the Spine client owned by the LAPDOG Mediator.
     */
    public static final String LAPDOG_MEDIATOR = "LAPDOG-mediator";
}
