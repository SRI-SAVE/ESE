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

import java.util.ArrayList;
import java.util.List;

import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.util.SpineTestCase;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.util.LogUtil;

public class JmsRemoteSpine_Basic {
    /**
     * This test is not intended to be run by itself.
     * Its main purpose is to support a multi Jms Spine instance test - see the
     * JmsSpine_Test::canReceiveMessagesFromOtherJVM() for further details. It is
     * that test which invokes this class in the proper context.
     * @param args any arguments to this psvm
     * @throws com.sri.tasklearning.spine.SpineException if there is a spine problem
     * @throws InterruptedException if the sleep goes wrong
     */
    public static void main(String[] args) throws InterruptedException, SpineException {
        LogUtil.configureLogging(SpineTestCase.LOG_CONFIG_BASE, SpineTestCase.class);
        JmsSpine jmsSpine = new JmsSpine(JmsClient.REMOTE, "Bridge");
        List<Object> inParams = new ArrayList<Object>();
        Thread.sleep(1000);
        jmsSpine.send(new ExecuteRequest("Bridge", new TransactionUID("Bridge",
                0), null, (SimpleTypeName) TypeNameFactory.makeName("run"),
                inParams, false));
        System.exit(1);
    }

}
