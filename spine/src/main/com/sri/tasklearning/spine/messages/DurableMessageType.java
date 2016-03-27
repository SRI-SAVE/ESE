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

public enum DurableMessageType implements MessageType {

    /**
     * This durable topic (a durable topic is one which holds the messages until the
     * consuming client is active and able to receive them) is the first message sent
     * from the REMOTE Spine instances to the LOCAL Spine instance in order to
     * advertise their existence. This allows the LOCAL Spine instance, which manages
     * the secondary REMOTE Spine instances, to ensure that each instance has a unique
     * id, to get an accurate head count, and to signal the REMOTE instances when the
     * LOCAL Spine instance is ready (on occasion, the REMOTE instances were sending
     * messages before the LOCAL instance had a chance to register on them, these
     * messages were lost as they were not durable). The LOCAL Spine instance will
     * respond to this message with a REGISTER_CONFIRMATION Message
     */
    REGISTER

}
