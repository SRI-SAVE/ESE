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

// $Id: BreakpointResponse.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import java.util.List;

import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Response message sent by a UI component back to Lumen in response to a
 * {@link BreakpointNotify} message. This response includes a command on how to
 * resume execution of the paused procedure.
 */
public class BreakpointResponse
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final Command command;
    private final List<Object> args;

    public BreakpointResponse(String sender,
                              Command command,
                              TransactionUID uid,
                              List<Object> actionArgs) {
        super(sender, uid, UserMessageType.BREAKPOINT_RESPONSE);
        this.command = command;
        args = actionArgs;
    }

    public Command getCommand() {
        return command;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID)uid;
    }

    /**
     * Provides the replacement args to be given to the next (sub-)action called
     * by the paused action.
     *
     * @return replacement args for the next action, or {@code null}
     */
    public List<Object> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return super.toString() + " to " + command;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((command == null) ? 0 : command.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        BreakpointResponse other = (BreakpointResponse) obj;
        if (command != other.command)
            return false;
        return true;
    }

    public static enum Command {
        STEP_INTO, STEP_OVER, STEP_OUT, CONTINUE
    }
}
