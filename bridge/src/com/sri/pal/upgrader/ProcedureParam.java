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

// $Id: ProcedureParam.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.upgrader;

public class ProcedureParam {
    private final String varName;
    private final String type;
    private final String defaultValue;

    public ProcedureParam(String name,
                          String type,
                          String defVal) {
        varName = name;
        this.type = type;
        defaultValue = defVal;
    }

    public String getVariableName() {
        return varName;
    }

    public String getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}