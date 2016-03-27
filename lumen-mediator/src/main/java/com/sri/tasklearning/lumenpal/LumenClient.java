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

package com.sri.tasklearning.lumenpal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.runtime.SteppingSupport;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.mediators.TypeFetcher;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.JmsClient;
import com.sri.tasklearning.spine.messages.JmsSpineClosing;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.util.ReplyWatcher;

/**
 * This class is the top layer interface to the Lumen module proper. This is
 * responsible for constructing all the pieces of the Lumen Mediator and
 * starting them running.
 */
public class LumenClient
        implements MessageHandler {
    private static final Logger log = LoggerFactory
            .getLogger(LumenClient.class);

    private final LumenFacade lumen;
    private final ServerConnectionImpl sci;
    private final Spine spine;
    private final ExecutionWatcher execWatcher;
    private final CancelReceiver cancelReceiver;
    private final ExecutionHandler execHandler;
    private final ReplyWatcher<SerialNumberResponse> serialGetter;
    private final ConstraintHandler constraintHandler;
    private final StepHandler stepHandler;
    private final ReplyWatcher<TypeResult> typeResultWatcher;
    private final LumenTypeAdder adder;
    private final LockingActionModel actionModel;
    private final TypeFetcher typeFetcher;
    private final ProcedureDependencyFinder procDepFinder;

    /**
     * Constructor for the client - this is called during initialization of the
     * entire system by the LumenProcess class - part of the ITLModule
     * initializating mechanism
     *
     * @throws SpineException
     */
    public LumenClient(LumenFacade lumenFacade,
                       Spine spineFacade)
            throws SpineException {
        log.debug("Constructing");

        lumen = lumenFacade;
        spine = spineFacade;

        typeResultWatcher = new ReplyWatcher<TypeResult>(TypeResult.class,
                spine);

        adder = new LumenTypeAdder(lumen);
        actionModel = new LockingActionModel(adder);
        typeFetcher = new TypeFetcher(spine, actionModel, typeResultWatcher);
        procDepFinder = new ProcedureDependencyFinder(actionModel, typeFetcher);

        serialGetter = new ReplyWatcher<SerialNumberResponse>(
                SerialNumberResponse.class, spine);

        execHandler = new ExecutionHandler(actionModel, typeFetcher,
                serialGetter, spine, procDepFinder);
        execWatcher = new ExecutionWatcher(spine);
        sci = new ServerConnectionImpl(execHandler, execWatcher, actionModel,
                spine, serialGetter);

        cancelReceiver = new CancelReceiver(lumen);

        constraintHandler = new ConstraintHandler(typeFetcher, actionModel,
                spine);

        stepHandler = new StepHandler(spine, lumen);
        SteppingSupport.STEPPING_HANDLER = stepHandler;

        lumen.setServerConnection(sci);
    }

    /**
     * Initialize Lumen and subscribe to the Spine topics we're interested in.
     *
     * @throws SpineException
     */
    void start()
            throws SpineException {
        lumen.initialize();
        // Note: We are not subscribing to the MessageTypes.TYPE_RESULT message
        // since the TypeFetcher indirectly subscribes itself to these events
        // in the form of the gather method.
        spine.subscribe(execWatcher, UserMessageType.EXECUTION_STATUS);
        spine.subscribe(cancelReceiver, UserMessageType.CANCEL);
        spine.subscribe(execHandler, UserMessageType.EXECUTE_REQUEST);
        spine.subscribe(serialGetter, SystemMessageType.SERIAL_NUMBER_RESPONSE);
        spine.subscribe(constraintHandler, UserMessageType.CONSTRAINT_REQUEST);
        spine.subscribe(stepHandler.getBreakpointReceiver(),
                UserMessageType.BREAKPOINT_RESPONSE);
        spine.subscribe(stepHandler, UserMessageType.EXPR_EVAL_REQUEST);
        spine.subscribe(typeResultWatcher, UserMessageType.TYPE_RESULT);
        spine.subscribe(this, SystemMessageType.SPINE_CLOSING);
    }

    public void shutdown() {
        try {
            spine.unsubscribe(UserMessageType.EXECUTION_STATUS);
            spine.unsubscribe(UserMessageType.CANCEL);
            spine.unsubscribe(UserMessageType.EXECUTE_REQUEST);
            spine.unsubscribe(SystemMessageType.SERIAL_NUMBER_RESPONSE);
            spine.unsubscribe(UserMessageType.CONSTRAINT_REQUEST);
            spine.unsubscribe(UserMessageType.BREAKPOINT_RESPONSE);
            spine.unsubscribe(UserMessageType.EXPR_EVAL_REQUEST);
            spine.unsubscribe(UserMessageType.TYPE_RESULT);
        } catch (SpineException e) {
            // Ignore.
        }
        lumen.shutdown();
        typeResultWatcher.shutdown();
        actionModel.shutdown();
        typeFetcher.shutdown();
        serialGetter.shutdown();
        execHandler.shutdown();
        sci.shutdown();
        constraintHandler.shutdown();
        stepHandler.shutdown();
        try {
            spine.shutdown(false);
        } catch (Exception e) {
            // Ignore.
        }
    }

    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (message instanceof JmsSpineClosing) {
            JmsSpineClosing close = (JmsSpineClosing) message;
            if (close.getSpineType() == JmsClient.LOCAL) {
                shutdown();
            }
        } else {
            log.warn("Unexpected message ({}): {}", message.getClass(), message);
        }
    }
}
