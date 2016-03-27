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

import com.sri.tasklearning.spine.messages.Message;

/**
 * This interface shows how callers of the asynchronousGather method
 * should prepare for a response from the gather thread. When the gather
 * is successful, the result method is called with the results, otherwise
 * the warning method is called with details of the encountered failure.
 */
public interface GatherCallback {

    public void result(Message[] messages);

    public void warning(com.sri.tasklearning.spine.messages.GatherIssues warningType);

}
