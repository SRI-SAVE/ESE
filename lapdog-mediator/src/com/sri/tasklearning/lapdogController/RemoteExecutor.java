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

// $Id: RemoteExecutor.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lapdogController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sri.ai.tasklearning.lapdog.CommonTaskRepresentation;
import com.sri.ai.tasklearning.lapdog.ItlTaskManagerImpl;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.BreakpointNotify;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.ExecutionStatus;
import com.sri.tasklearning.spine.messages.IgnoredExecutionStatus;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.messages.StartExecutionStatus;
import com.sri.tasklearning.spine.messages.SuccessExecutionStatus;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If LAPDOG needs to execute an external action, it calls out to this class in
 * order to do so. This class forwards the request on to the appropriate
 * (Bridge) client and returns the result to LAPDOG.
 */
class RemoteExecutor
        extends ItlTaskManagerImpl
        implements MessageHandler {
    private static final Logger log = LoggerFactory
            .getLogger(RemoteExecutor.class);

    private final Spine spine;
    private final Map<TransactionUID, ExecutionStatus> watchers;

    public RemoteExecutor(CommonTaskRepresentation ctr,
                          Spine spine)
            throws SpineException {
        super(ctr);
        this.spine = spine;
        watchers = new HashMap<TransactionUID, ExecutionStatus>();
        spine.subscribe(this, UserMessageType.EXECUTION_STATUS);
    }

    @Override
    public boolean executeAction(String actNameStr,
                                 List<Object> inArgs,
                                 List<Object> outArgs) {
        SimpleTypeName actName = (SimpleTypeName) TypeNameFactory
                .makeName(actNameStr);

        /* Build the execute request. */
        TransactionUID uid = spine.getNextUid();
        ExecuteRequest req = new ExecuteRequest(spine.getClientId(), uid, null,
                actName, inArgs, false);

        /* Send the request and wait for a reply. */
        ExecutionStatus status;
        try {
            synchronized (watchers) {
                watchers.put(uid, null);
                spine.send(req);
                while (watchers.get(uid) == null) {
                    watchers.wait();
                }
                status = watchers.remove(uid);
            }
        } catch (Exception e) {
            log.warn("Failed to send or get reply: " + req, e);
            return false;
        }

        /* Handle the result, based on what type of status we received. */
        boolean success;
        if (status instanceof IgnoredExecutionStatus) {
            log.info("Request to execute {} was ignored", actName);
            success = false;
        } else if (status instanceof ErrorExecutionStatus) {
            ErrorExecutionStatus errStatus = (ErrorExecutionStatus) status;
            ErrorInfo error = errStatus.getError();
            log.debug("Execute {} gave an error: {}", actName, error);
            success = false;
        } else if (status instanceof SuccessExecutionStatus) {
            SuccessExecutionStatus sucStatus = (SuccessExecutionStatus) status;
            List<Object> outParams = sucStatus.getOutParams();
            outArgs.clear();
            for (int i = 0; i < outParams.size(); i++) {
                outArgs.add(i, outParams.get(i));
            }
            log.debug("Execute {} results: {}", actName, outArgs);
            success = true;
        } else {
            log.warn("Unexpected status ({}): {}", status.getClass(), status);
            success = false;
        }

        /*
         * Return false if the execution was successful. LAPDOG will read output
         * args from the outArgs that it passed us.
         */
        return !success;
    }

    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (!(message instanceof ExecutionStatus)) {
            log.warn("Unexpected message ({}): {}", message.getClass(), message);
            return;
        }

        ExecutionStatus execMsg = (ExecutionStatus) message;
        TransactionUID uid = execMsg.getUid();
        if (!watchers.containsKey(uid)) {
            log.debug("Not watching for {} in {}", uid, message);
            return;
        }

        if (execMsg instanceof BreakpointNotify) {
            log.warn("Unexpected for {}: {}", uid, execMsg);
            /* But ignore it and wait for a final status. */
        } else if (execMsg instanceof StartExecutionStatus) {
            log.debug("{} started: {}", uid, execMsg);
            /* Ignore it and wait for a final status. */
        } else if (execMsg instanceof ErrorExecutionStatus
                || execMsg instanceof IgnoredExecutionStatus
                || execMsg instanceof SuccessExecutionStatus) {
            log.debug("{} finished: {}", uid, execMsg);
            synchronized (watchers) {
                watchers.put(uid, execMsg);
                watchers.notifyAll();
            }
        } else {
            log.warn("Unexpected message ({}): {}", execMsg.getClass(), execMsg);
        }
    }
}
