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

// $Id: ErrorType.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.util;

public enum ErrorType {
    CANCEL("canceled", "The action or procedure %s was canceled."),
    STRING_TOO_BIG(
            "parameter too big",
            "A parameter was too big to be transmitted."),
    INTERNAL_ACTION_EXEC(
            "internal action-exec error",
            "Internal error trying to start action for %s"),
    INTERNAL_FILE_LOADER(
            "internal file persistence error",
            "Internal error trying to load a procedure from a file"),
    INTERNAL_COMMS(
            "internal communications error",
            "Internal communications error"),
    INTERNAL_PARSE("internal parse error", "Internal error trying to parse %s"),
    ACTION_MODEL("action model error", "Action model error with type/action %s"),
    INTERNAL_LOADER_START(
            "internal type loader error",
            "Internal error starting type loaders for %s"),
    NOT_ALL_LOADED(
            "some actions were not loaded",
            "%s"),
    PROC_START("unable to start procedure", "Unable to start procedure %s: %s"),
    LUMEN("Lumen error", "Generic Lumen error (%s); probably subtask failure"),
    LAPDOG("LAPDOG error", "Generic LAPDOG error (%s)"),
    CONSTRAINT_COALESCE(
            "unable to coalesce constraints",
            "Couldn't coalesce constraints for procedure %s"),
    UNKNOWN("unknown error", "Unknown error"),
    EXEC_MISSING("missing executor error", "A required executor is missing"),
    EXEC_IGNORED("exec request ignored", "No executor accepted request to execute %s"),
    STORAGE("type storage error", "TypeStorage implementation threw an exception"),
    VALIDATION("validation error", "Unable to fetch declarations map for validation"),
    SANITY("internal consistency error", "Internal consistency check failed: %s");

    private final String terseMsg;
    private final String detailMsg;

    private ErrorType(String terseMsg,
                      String detailMsg) {
        this.terseMsg = terseMsg;
        this.detailMsg = detailMsg;
    }

    public String getTerseMsg() {
        return terseMsg;
    }

    public String getDetailMsg() {
        return detailMsg;
    }
}
