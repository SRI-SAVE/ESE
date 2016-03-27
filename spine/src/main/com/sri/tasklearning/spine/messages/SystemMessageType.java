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

public enum SystemMessageType implements MessageType {

    /**
     * Sent from the LOCAL Spine instance to the REMOTE Spine instance when
     * responding to the REGISTER message. This message, has the originating
     * uid in it that will allow the REMOTE Spine instances to identify their
     * own response message. This message signifies successful registration
     * with the Master/LOCAL Spine and permission to start sending messages.
     */
    REGISTER_CONFIRMATION,

    /**
     * This message is sent by the Spine instances when their Spine client
     * registers themselves as subscribers to a message type. Every other
     * Spine instance will take note in order to inform the Spine client
     * when they try to send a message on that topic if there is no registered
     * subscribers (ie: send() will return false if there are no known
     * subscribers)
     */
    NEW_SUBSCRIPTION,

    /**
     * The message is intended for the the LOCAL Spine instance which manages
     * the master list of registered subscriptions (since the LOCAL Spine
     * instance is always the first to start). The LOCAL instance of the spine
     * will respond with an EXISTING_SUBSCRIPTIONS message which contains all
     * the known registered subscriptions.
     */
    EXISTING_SUBSCRIPTIONS_REQUEST,

    /**
     * Sent from the LOCAL Spine instance when an EXISTING_SUBSCRIPTIONS_REQUEST
     * message is received, it contains all the known registered subscriptions.
     */
    EXISTING_SUBSCRIPTIONS,

    /**
     * Send from a Spine instance when it is about to exit - this is used to tell
     * the LOCAL Spine instance to treat it as a STOP_WATCHING_REQUEST if the
     * sending Spine had previously requested START_WATCHING.
     */
    SPINE_CLOSING,

    /**
     * Sent from any of the REMOTE Spine instances, this message will cause the
     * MASTER Spine instance to shutdown. Before doing so, it will issue a
     * SPINE_CLOSING message so that the REMOTE Spine instances can die gracefully
     */
    SHUTDOWN_MASTER,

    /**
     * Issued by the LOCAL Spine instance in response to a REQUEST_START_WATCHING
     */
    START_WATCHING,

    /**
     * Issued by the LOCAL Spine instance in response to a REQUEST_STOP_WATCHING
     */
    STOP_WATCHING,

    /**
     * Issued by the LOCAL Spine instance in response to a SERIAL_NUMBER_REQUEST
     */
    SERIAL_NUMBER_RESPONSE,

    /**
     * Issues by a REMOTE Spine instance to request permission to subscribe to
     * a privileged topic - some topics have a limited number of subscribers
     * allowed. The LOCAL Spine will reply with a response to the request.
     */
    PRIVILEGED_SUBSCRIPION_REQUEST,

    /**
     * Issued by the LOCAL Spine instance in response to a
     * PRIVILEGED_SUBSCRIPION_REQUEST. The LOCAL Spine instance manages the
     * privileged topics and will decide if it will permit the request or not.
     */
    PRIVILEGED_SUBSCRIPION_RESPONSE,

    /**
     * Issued by a spine client to indicate they will no longer be processing
     * these message types.
     */
    UNSUBSCRIBE,

    /**
     * Issued at a set rate from the LOCAL spine to allow subscribing spines to
     * know if the LOCAL Spine s still operational
     */
    HEARTBEAT

}
