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

package com.sri.tasklearning.lumenpal.mock;

import com.sri.tasklearning.lumenpal.ExecutionHandler;
import com.sri.tasklearning.lumenpal.ProcedureDependencyFinder;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.mediators.TypeFetcher;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.util.ReplyWatcher;

public class MockExecutionHandler extends ExecutionHandler {
    public MockExecutionHandler(LockingActionModel actionModel,
                                TypeFetcher typeFetcher,
                                ReplyWatcher<SerialNumberResponse> serialGetter,
                                Spine spine,
                                ProcedureDependencyFinder finder) {
        super(actionModel, typeFetcher, serialGetter, spine, finder);
    }

    @Override
    public void handleMessage(Message message) throws MessageHandlerException {

    }

}
