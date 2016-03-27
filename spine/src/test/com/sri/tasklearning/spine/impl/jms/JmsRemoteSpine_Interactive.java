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

package com.sri.tasklearning.spine.impl.jms;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.learning.ATRDemonstratedAction;
import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.ai.lumen.atr.learning.impl.ATRDemonstratedActionImpl;
import com.sri.ai.lumen.atr.learning.impl.ATRDemonstrationImpl;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.util.MockClientMessageHandler;
import com.sri.tasklearning.spine.impl.jms.util.SpineTestCase;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.LearnRequest;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.messages.contents.UID;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class JmsRemoteSpine_Interactive {
    private static final Logger log = LoggerFactory.getLogger(JmsRemoteSpine_Interactive.class);
    private static String bridge = "Bridge";
    private static String lapdog = "Lapdog";
    private static String lumen = "Lumen";
    private static long patience = 500; // milliseconds
    private static List<Object> params;

    /**
     * This test is not intended to be run by itself.
     * Its main purpose is to support a multi Jms Spine instance test - see the
     * JmsSpine_Test::canHandleComplexInteractionWithMultipleSpines() for further
     * details. It is that test which invokes this class in the proper context.
     * @param args any arguments to this psvm
     * @throws com.sri.tasklearning.spine.SpineException if there is a spine problem
     * @throws InterruptedException if the sleep goes wrong
     */
    public static void main(String[] args) throws Throwable, SpineException {
        LogUtil.configureLogging(SpineTestCase.LOG_CONFIG_BASE, SpineTestCase.class);
        // Create require variable types
        List<ATRDemonstratedAction> actions = new ArrayList<ATRDemonstratedAction>();
        actions.add(new ATRDemonstratedActionImpl("step"));
        ATRDemonstration demonstration = new ATRDemonstrationImpl(actions);
        ATRTypeDeclaration spineType = ATRTestUtil.makeCustomType(
                (SimpleTypeName) TypeNameFactory
                        .makeName("definition of walking"), String.class);
        params = new ArrayList<Object>();

        // Create a mock Bridge message handler and subscribe it to the
        // type_query and execution_status topic through the bridge spine
        JmsSpine bridgeSpine = new JmsSpine(JmsClient.REMOTE, bridge);
        MockClientMessageHandler bridgeHandler = new MockClientMessageHandler(bridgeSpine, false);
        bridgeSpine.subscribe(bridgeHandler, UserMessageType.TYPE_QUERY, UserMessageType.LEARN_RESULT, UserMessageType.EXECUTION_STATUS);

        // Allow subscriptions to propagate
        Thread.sleep(100);

        // Send the Learn Request
        TransactionUID learnRequestUID = bridgeSpine.getNextUid();
        boolean received = bridgeSpine.send(new LearnRequest(bridge,
                demonstration, (SimpleTypeName) TypeNameFactory
                        .makeName("walk"), null, null, learnRequestUID));
        if (!received) {
            log.error("----------------Lapdog did not subscribe to the Learn Request - Bridge cannot send!");
            bridgeSpine.finalize();
            System.exit(1);
        }
        Thread.sleep(100);

        // When lapdog responds it will be in the form of a type query. The originiator
        // will be lapdog as that is a different message transaction than the learn request
        TransactionUID typeQueryUID = null;
        boolean noTypeQuery = true;
        long wait = 0;
        while (noTypeQuery && wait < patience) {
            if (bridgeHandler.getMessageUidListFromSender(lapdog) != null) {
                noTypeQuery = false;
                log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>Recieved TypeQuery from Lapdog");
                typeQueryUID = bridgeHandler.getMessageUidListFromSender(lapdog).get(0);
            }
            else {
                log.info("No TypeQuery from Lapdog yet...");
            }
            Thread.sleep(100);
            wait += 100;
        }
        if (noTypeQuery) {
            log.error("----------------------------Lapdog did not reply with a TypeQuery.");
            bridgeSpine.finalize();
            System.exit(1);
        }

        // Bridge responds to a TypeQuery with a TypeResult using the original UID
        bridgeSpine.send(new TypeResult(bridge,
                (SimpleTypeName) TypeNameFactory.makeName("walk"), ATRSyntax
                        .toSource(spineType), typeQueryUID));
        Thread.sleep(100);

        // Lapdog should now process the learn request, with the new found type
        // information and then respond with a learn response. As a LearnRequest
        // is a chatty transaction, the uid should be that of the original request.
        boolean noLearnResponse = true;
        wait = 0;
        while (noLearnResponse && wait < patience) {
            if (bridgeHandler.getMessageUidListFromSender(lapdog).size() == 2) {
                UID uid = bridgeHandler.getMessageUidListFromSender(lapdog).get(1);
                if (uid.equals(learnRequestUID)) {
                    noLearnResponse = false;
                    log.info(">>>>>>>>>>>>>>>>>>>>>>>>>Recieved LearnResponse from Lapdog");
                }
                else {
                    log.warn("Got a response from Lapdog but it was not the correct UID!");
                    System.exit(1);
                }
            }
            else {
                log.info("No LearnResponse from Lapdog yet...");
            }
            Thread.sleep(100);
            wait += 100;
        }
        if (noLearnResponse) {
            log.error("----------------------------Lapdog did not reply with a LearnRespose.");
            bridgeSpine.finalize();
            System.exit(1);
        }

        // Now that we have the LearnedProcedure we are going to get Lumen to
        // execute it
        TransactionUID executeRequestUid = bridgeSpine.getNextUid();
        bridgeSpine.send(new ExecuteRequest(bridge, executeRequestUid, null,
                (SimpleTypeName) TypeNameFactory.makeName("walk"), params,
                false));
        Thread.sleep(100);

        // Lumen will need to make a typeRequest also. The originiator
        // will be lumen type request is a new transaction message
        noTypeQuery = true;
        wait = 0;
        while (noTypeQuery && wait < patience) {
            if (bridgeHandler.getMessageUidListFromSender(lumen) != null) {
                noTypeQuery = false;
                log.info(">>>>>>>>>>>>>>>>>>>>>>>>>Recieved TypeQuery from Lumen");
                typeQueryUID = bridgeHandler.getMessageUidListFromSender(lumen).get(0);
            }
            else {
                log.info("No TypeQuery from Lumen yet...");
            }
            Thread.sleep(100);
            wait += 100;
        }
        if (noTypeQuery) {
            log.error("-------------------------------Lumen did not reply with a TypeQuery.");
            bridgeSpine.finalize();
            System.exit(1);
        }

        // Bridge responds to a TypeQuery with a TypeResult
        bridgeSpine.send(new TypeResult(bridge,
                (SimpleTypeName) TypeNameFactory.makeName("walk"), ATRSyntax
                        .toSource(spineType), typeQueryUID));
        Thread.sleep(100);

        // Lumen should send an execution status started as soon as it gets going
        // with the request
        boolean noExecutionStartedStatus = true;
        wait = 0;
        while (noExecutionStartedStatus && wait < patience) {
            if (bridgeHandler.getMessageUidListFromSender(lumen).size() == 2) {
                UID uid = bridgeHandler.getMessageUidListFromSender(lumen).get(1);
                if (uid.equals(executeRequestUid)) {
                    noExecutionStartedStatus = false;
                    log.info(">>>>>>>>>>>>>>>>>>>>>>>>Recieved ExecutionStatusStarted from Lumen");
                }
                else {
                    log.warn("Got a response from Lumen but it was not the correct UID!");
                    bridgeSpine.finalize();
                    System.exit(1);
                }
            }
            else {
                log.info("No ExecutionStatusStarted from Lumen yet...");
            }
            Thread.sleep(100);
            wait += 100;
        }
        if (noExecutionStartedStatus) {
            log.error("----------------------------------Lumen did not reply with an ExecutionStatusStarted.");
            bridgeSpine.finalize();
            System.exit(1);
        }

        // Lumen should send an execution status started as soon as it gets going
        // with the request
        boolean noExecutionSuccessStatus = true;
        wait = 0;
        while (noExecutionSuccessStatus && wait < patience) {
            if (bridgeHandler.getMessageUidListFromSender(lumen).size() == 3) {
                UID uid = bridgeHandler.getMessageUidListFromSender(lumen).get(2);
                if (uid.equals(executeRequestUid)) {
                    noExecutionSuccessStatus = false;
                    log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>Recieved ExecutionStatusSuccess from Lumen");
                }
                else {
                    log.warn("Got a response from Lumen but it was not the correct UID!");
                    bridgeSpine.finalize();
                    System.exit(1);
                }
            }
            else {
                log.info("No ExecutionStatusSuccess from Lumen yet...");
            }
            Thread.sleep(100);
            wait += 100;
        }
        if (noExecutionSuccessStatus) {
            log.error("--------------------------Lumen did not reply with an ExecutionStatusSuccess.");
            bridgeSpine.finalize();
            System.exit(1);
        }

        log.info("Test complete - all assertions were satisfied in the Bridge JVM!");
        bridgeSpine.finalize();
    }

}
