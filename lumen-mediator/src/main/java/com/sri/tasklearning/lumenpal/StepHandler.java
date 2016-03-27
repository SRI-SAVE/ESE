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

// $Id: StepHandler.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sri.ai.lumen.core.Failure;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.mediator.TaskExecutionListener;
import com.sri.ai.lumen.runtime.SteppingSupport;
import com.sri.ai.lumen.runtime.SteppingSupport.StepCommand;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.BreakpointNotify;
import com.sri.tasklearning.spine.messages.BreakpointResponse;
import com.sri.tasklearning.spine.messages.ExprEvalRequest;
import com.sri.tasklearning.spine.messages.ExprEvalResult;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.spine.util.ReplyWatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles breakpoint notifications from Lumen. This happens when we've
 * requested to execute a procedure in stepped mode, in other words to debug it.
 * Lumen notifies us at every breakpoint and pauses that invocation until we
 * respond with a command.
 */
public class StepHandler
        implements SteppingSupport.SteppingHandler, MessageHandler {
    private static final Logger log = LoggerFactory
            .getLogger(StepHandler.class);

    private final Spine spine;
    private final LumenFacade lumen;
    private final ReplyWatcher<BreakpointResponse> replyWatcher;
    private final Map<TransactionUID, Map<String, Object>> procBindings;
    private final ErrorFactory errorFactory;

    public StepHandler(Spine spine,
                       LumenFacade lumen) {
        this.spine = spine;
        this.lumen = lumen;
        replyWatcher = new ReplyWatcher<BreakpointResponse>(
                BreakpointResponse.class, spine);
        procBindings = new HashMap<TransactionUID, Map<String, Object>>();
        errorFactory = new ErrorFactory(spine.getClientId());
    }

    @Override
    public StepCommand handleStep(TaskExecutionListener execListener,
                                  String actionNameStr,
                                  int preorderIndex,
                                  Map<String, Object> bindings,
                                  String subActionNameStr,
                                  Object[] arguments) {
        log.debug("stepping {} into {} at {} for {}", new Object[] {
                actionNameStr, subActionNameStr, preorderIndex, execListener });
        try {
            SimpleTypeName actionName = (SimpleTypeName) TypeNameFactory
                    .makeName(actionNameStr);
            SimpleTypeName subActionName = (SimpleTypeName) TypeNameFactory
                    .makeName(subActionNameStr);
            String uidStr = execListener.getTaskMessageUID();
            TransactionUID uid = new TransactionUID(uidStr);
            BreakpointNotify query = new BreakpointNotify(spine.getClientId(),
                    actionName, preorderIndex, subActionName, uid);
            procBindings.put(uid, bindings);
            BreakpointResponse response;
            try {
                response = replyWatcher.sendAndGetReply(query);
            } finally {
                procBindings.remove(uid);
            }
            List<Object> newArgs = response.getArgs();
            if (newArgs != null) {
                for (int i = 0; i < newArgs.size(); i++) {
                    arguments[i] = newArgs.get(i);
                }
            }
            /*
             * BreakpointResponse.Command is a mirror enum of
             * SteppingSupport.StepCommand. Copy values from one to the other.
             */
            StepCommand command = StepCommand.valueOf(response.getCommand()
                    .name());
            return command;
        } catch (Exception e) {
            /*
             * It doesn't matter what the exception was; we won't be getting a
             * proper response from some other Spine client. So log a warning
             * and tell Lumen what to do.
             */
            log.warn("Unable to send " + BreakpointNotify.class.getSimpleName()
                    + " for " + actionNameStr, e);
            return StepCommand.STEP_OVER;
        }
    }

    @Override
    public void handleFailure(TaskExecutionListener listener,
                              String actionNameStr,
                              int preorderIndex,
                              Failure failure) {
        /*
         * Whenever this is called, we also get a call to
         * LumenTaskResultListener.taskFailed(). Until this method carries more
         * information or allows us to respond, we ignore it in favor of the
         * other.
         */
    }

    public MessageHandler getBreakpointReceiver() {
        return replyWatcher;
    }

    /*
     * Handles incoming ExprEvalRequest messages. BreakpointReponse messages are
     * handled by our captive ReplyWatcher.
     */
    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (!(message instanceof ExprEvalRequest)) {
            log.warn("Unexpected message (" + message.getClass() + "): "
                    + message);
            return;
        }

        ExprEvalRequest evalReq = (ExprEvalRequest) message;
        TransactionUID reqUid = evalReq.getUid();
        try {
            Map<String, Object> bindingsMods = evalReq.getBindingsChanges();
            List<ATRTerm> exprs;
            try {
                exprs = evalReq.getExpressions();
            } catch (LumenSyntaxError e) {
                log.warn("Couldn't parse expressions from " + evalReq);
                ErrorInfo error = errorFactory.error(ErrorType.INTERNAL_PARSE, "");
                sendError(reqUid, error);
                return;
            }

            /*
             * Retrieve the current bindings for that procedure execution.
             */
            TransactionUID procUid = evalReq.getProcedureUid();
            Map<String, Object> bindings = (procUid == null) 
                    ? new HashMap<String, Object>() : procBindings.get(procUid);

            /* Update Lumen's bindings. */
            if (bindingsMods != null)
                bindings.putAll(bindingsMods);

            /*
             * Call Lumen to evaluate the expressions.
             */
            List<Object> values = new ArrayList<Object>();
            for (ATRTerm expr : exprs) {
                Object value = lumen.evaluateATRTerm(expr, bindings);
                values.add(value);
            }

            String sender = spine.getClientId();
            ExprEvalResult result = new ExprEvalResult(sender, reqUid, values);
            try {
                spine.send(result);
            } catch (SpineException e) {
                log.warn("Unable to send response " + result, e);
            }
        } catch (Exception e) {
            log.warn("Unexpected error evaluating expressions from " + evalReq, e);
            ErrorInfo error = errorFactory.error(ErrorType.UNKNOWN);
            sendError(reqUid, error);
        }
    }

    private void sendError(TransactionUID uid,
                           ErrorInfo error) {
        ExprEvalResult errMsg = new ExprEvalResult(spine.getClientId(), uid,
                error);
        try {
            spine.send(errMsg);
        } catch (SpineException e) {
            log.warn("Failed to send error message " + errMsg, e);
        }
    }

    void shutdown() {
        replyWatcher.shutdown();
    }
}
