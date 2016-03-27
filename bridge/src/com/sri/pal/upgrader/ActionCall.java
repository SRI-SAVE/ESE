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

// $Id: ActionCall.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.upgrader;

import java.util.ArrayList;
import java.util.List;

import com.sri.ai.lumen.atr.term.ATRTerm;

/**
 * Represents a call to an action which appears in the source of a procedure.
 * All arguments have been converted to their string representations, for
 * example:
 * <table>
 * <tr>
 * <th><b>in procedure source</b></th>
 * <th><b>in this class</b></th>
 * </tr>
 * <tr>
 * <td>$var</td>
 * <td>"$var"</td>
 * </tr>
 * <tr>
 * <td>first($list)</td>
 * <td>"first($list)"</td>
 * </tr>
 * <tr>
 * <td>"John Doe"</td>
 * <td>"\"John Doe\""</td>
 * </tr>
 * </table>
 * <b>In particular, note that constant values contain embedded quotes.</b>
 */
public class ActionCall {
    private final String name;
    private List<String> args;
    private List<ATRTerm> atrArgs;

    /**
     * Creates a new action call with a particular name.
     *
     * @param name
     *            the name of the action which will be called
     */
    public ActionCall(String name) {
        this.name = name;
        args = new ArrayList<String>();
    }

    /**
     * Provides the name of the action which is called.
     *
     * @return the called action name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the argument expressions passed to the action call. This list
     * may be modified in place, and changes will affect the {@code ActionCall}
     * object it came from.
     *
     * @return the list of argument expressions for this action call
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * Replaces the argument expressions for this action call with a new list.
     *
     * @param args
     *            the new expressions to use for this action call
     */
    public void setArgs(List<String> args) {
        this.args = args;
    }

    /**
     * @return the ATR form of the arguments to this action call
     */
    List<ATRTerm> getAtrArgs() {
        return atrArgs;
    }

    /**
     * For setting the ATR form of the arguments to this action call
     * @param atrArgs - the ATR arguments
     */
    void setAtrArgs(List<ATRTerm> atrArgs) {
        this.atrArgs = atrArgs;
    }
}
