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

// $Id: ExecutionWatcher.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sri.ai.lumen.core.CoreUtil;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.mediator.MediatorException;
import com.sri.ai.lumen.mediator.TaskExecutionListener;
import com.sri.pal.common.ErrorInfo;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.messages.ErrorExecutionStatus;
import com.sri.tasklearning.spine.messages.ExecutionStatus;
import com.sri.tasklearning.spine.messages.IgnoredExecutionStatus;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SuccessExecutionStatus;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives {@link ExecutionStatus} messages from the Spine and dispatches them
 * to the appropriate {@link TaskExecutionListener}. In other words, it watches
 * an execution in the application and reports the status of that execution to
 * Lumen.
 *
 * @author chris
 */
public class ExecutionWatcher
        implements MessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<TransactionUID, TaskExecutionListener> watchers;
    private final Map<TransactionUID, ATRActionDeclaration> actions;
    private final Map<TransactionUID, List<Object>> arguments;
    private final ErrorFactory errorFactory;

    public ExecutionWatcher(Spine spine) {
        watchers = new HashMap<TransactionUID, TaskExecutionListener>();
        actions = new HashMap<TransactionUID, ATRActionDeclaration>();
        arguments = new HashMap<TransactionUID, List<Object>>();
        errorFactory = new ErrorFactory(spine.getClientId());
    }

    @Override
    public synchronized void handleMessage(Message message)
            throws MessageHandlerException {
        if (!(message instanceof ExecutionStatus)) {
            log.warn("Unexpected message ({}): {}", message.getClass(), message);
            return;
        }
        ExecutionStatus execMsg = (ExecutionStatus) message;

        TransactionUID uid = execMsg.getUid();
        TaskExecutionListener watcher = watchers.get(uid);
        if (watcher == null) {
            log.debug("Received {}, but I don't care.", execMsg);
        } else {
            log.debug("Received {} for {}", execMsg, watcher);
            // Remove this watcher if the execution is finished.
            ATRActionDeclaration action = null;
            List<Object> paramList = null;
            if (execMsg instanceof SuccessExecutionStatus ||
                    execMsg instanceof ErrorExecutionStatus) {
                watchers.remove(uid);
                action = actions.remove(uid);
                paramList = arguments.remove(uid);
            }

            // Handle the specific message type.
            if (execMsg instanceof IgnoredExecutionStatus) {
                IgnoredExecutionStatus ignoredMsg = (IgnoredExecutionStatus) execMsg;
                try {
                    log.warn(
                            "Execution request was ignored by all executors: {}",
                            execMsg);
                    ErrorInfo error = errorFactory.error(
                            ErrorType.EXEC_IGNORED, ignoredMsg.getUid());
                    watcher.taskFailed(uid.toString(), error, null);
                } catch (MediatorException e) {
                    log.warn("Unable to cancel due to ignore " + uid + " in Lumen", e);
                }
            }else if (execMsg instanceof ErrorExecutionStatus) {
                ErrorExecutionStatus errorMsg = (ErrorExecutionStatus) execMsg;
                ErrorInfo error = errorMsg.getError();
                try {
                    watcher.taskFailed(uid.toString(), error, null);
                } catch (MediatorException e) {
                    log.warn("Unable to send error for " + uid + " to Lumen", e);
                }
            } else if (execMsg instanceof SuccessExecutionStatus) {
                SuccessExecutionStatus successMsg = (SuccessExecutionStatus) execMsg;
                List<Object> inParams = successMsg.getInParams();
                List<Object> outParams = successMsg.getOutParams();
                ATRSig sig = action.getSignature();
                List<? extends ATRParameter> atrParams = sig.getElements();
                for (int i = 0; i < atrParams.size(); i++) {
                    ATRParameter atrParam = atrParams.get(i);
                    if (atrParam.getMode() == Modality.INPUT) {
                        continue;
                    }
                    Object value = outParams.get(i - inParams.size());
                    if(value == null) {
                        value = CoreUtil.NULL_VALUE;
                    }
                    paramList.set(i, value);
                }
                log.debug("finished {} with args: {}", uid, paramList);
                try {
                    watcher.taskSucceeded(uid.toString(), paramList);
                } catch (MediatorException e) {
                    log.warn("Unable to send success for " + uid + " to Lumen",
                            e);
                }
            }
        }
    }

    /**
     * Registers a watcher which is interested in notifications concerning the
     * status of a given execution. The watcher will be notified when the
     * execution finishes.
     *
     * @param uid
     * @param watcher
     * @param action
     */
    public synchronized void watch(TransactionUID uid,
                                   TaskExecutionListener watcher,
                                   ATRActionDeclaration action,
                                   List<Object> args) {
        log.debug("Watching for {}, will give to {}", uid, watcher);
        watchers.put(uid, watcher);
        actions.put(uid, action);
        arguments.put(uid, args);
    }
}
