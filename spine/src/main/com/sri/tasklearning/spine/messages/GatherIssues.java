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

package com.sri.tasklearning.spine.messages;

/**
 * The types of issues that can occur when performing a gather
 */
public enum GatherIssues {

    /**
     * This status means that there were no other spines in the system registered to receive
     * messages of the gather request type.
     */
    NO_SUBSCRIBERS,

    /**
     * This status means that the system is not configured to gather this type of message. To
     * make a message gatherable, see the javadoc for the gather method in the StateManager
     */
    NOT_GATHERABLE

}
