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
 * This enumeration contains the official list of the message types the clients of a
 * Jms Spine instance is allowed to subscribe to receive or send into the Spine.
 */
public enum UserMessageType implements MessageType {

    /**
     * This message is sent from the Bridge to Lapdog to ask it to learn a procedure
     */
    LEARN_REQUEST,

    /**
     * This message is in response to the LEARN_REQUEST message and is send from Lapdog
     * to the Bridge with the learned procedure. The Bridge will know which request this
     * is related to as it will have the same TransactionUID as was sent by the Bridge in
     * the original Request.
     */
    LEARN_RESULT,

    /**
     * This message is either a request or a response to idiom recognition. The
     * Bridge sends a demonstration to LAPDOG, and LAPDOG sends a (possibly
     * modified) demonstration back.
     */
    PROCESS_DEMO,

    /**
     * Sent from the Bridge to LAPDOG to request a TaskSolutionOption be learned
     * from a demonstration.
     */
    LEARN_OPTION_REQUEST,

    /**
     * Sent from LAPDOG to the Bridge in response to a LEARN_OPTION_REQUEST.
     */
    LEARN_OPTION_RESULT,

    /**
     * This message is send from the Bridge to Lumen, or, from Lumen to the Bridge to
     * ask the receiver to execute a request.
     */
    EXECUTE_REQUEST,

    /**
     * This message is sent from the Bridge to Lumen, or, from Lumen to the Bridge to
     * inform the receiver as to how the execution of a a requested execution is
     * progressing. This may be STARTED, SUCCEEDED or CANCELLED or ERROR. The Bridge
     * (or Lumen) will know which request this is related to as it will have the same
     * TransactionUID as was sent by the originator in the original Request.
     */
    EXECUTION_STATUS,

    /**
     * This message is sent from the Bridge to Lumen, or, from Lumen to the Bridge to
     * inform the receiver to cancel the execution of a previously requested execution.
     * The Bridge (or Lumen) will know which request this is related to as it will have
     * the same TransactionUID as was sent by the originator in the original Request.
     */
    CANCEL,

    /**
     * This message is sent from Lumen or Lapdog to the Bridge to request a types
     * definition. This will be preceeded by a learn request or execution request from
     * the Bridge.
     */
    TYPE_QUERY,

    /**
     * This message is sent from the Bridge to Lumen or Lapdog in response to a
     * TYPE_QUERY message. Lapdog or Lumen will know which request this is related to
     * as it will have the same TransactionUID as was sent by the originator in the
     * original Request.
     */
    TYPE_RESULT,

    /**
     * This message is sent from one of the Spine clients to the LOCAL Spine to request
     * an issue of the START_WATCHING system message (command). This supports the
     * Adept feature for instrumentation. If the system is already in the watching
     * state, a counter is incremented to keep track of how many clients had requested
     * the start watching state.
     */
    REQUEST_START_WATCHING,

    /**
     * This message is sent from one of the Spine clients to the LOCAL Spine to request
     * an issue of the STOP_WATCHING system message (command). This supports the
     * Adept feature for instrumentation. If more than one client had requested the
     * start watching state, the stop watching order will only be issued after the
     * corresponding number of stop watching requests
     */
    REQUEST_STOP_WATCHING,

    /**
     * This message is sent from the Spine clients to the LOCAL Spine which manages
     * the master serial number counter. Each request will be responded to with the
     * requesting UID and the new serial number.
     */
    SERIAL_NUMBER_REQUEST,

    /**
     * This message is sent from a Spine instance in response to a gather
     * method call from a Spine Client. The query message is sent out to all subscribed
     * parties that will respond with the corresponding types in the form of a
     * TYPE_LIST_RESULT. These are then coalesced into one list and returned to
     * the caller of the gather method.
     */
    TYPE_LIST_QUERY,

    /**
     * This message is sent from all Spine clients in response to a
     * TYPE_LIST_QUERY.
     */
    TYPE_LIST_RESULT,

    /**
     * This message is broadcast as the result of a TYPE_STORE_REQUEST message
     * action.
     */
    TYPE_STORE_RESULT,

    /**
     * This message is sent from the PALBridge to the Shell to indicate it is
     * exiting - the Shell will then also exit
     */
    GOODBYE,

    /**
     * This message is sent from the Shell to the PALBridge to determine if
     * the PALBridge is still active. It will wait for a subsequent PING_REPLY
     * with a matching UID
     */
    PING_REQUEST,

    /**
     * This message is sent from the PALBridge to the Shell in response to a
     * PING_REQUEST.
     */
    PING_REPLY,

    /**
     * This message is sent from the executors to the master Spine instance to inform it
     * of requests that are not being handled.
     */
    REQUEST_IGNORED,

    /**
     * This is sent from a spine client (the Bridge) to Lumen to request that a
     * procedure have constraints generated for it, based on the constraints of
     * its component actions.
     */
    CONSTRAINT_REQUEST,

    /**
     * This is sent in response to a CONSTRAINT_REQUEST by Lumen with the
     * results of constraint coalescing.
     */
    CONSTRAINT_RESULT,

    /**
     * This is sent to inquire if any Spine clients are willing to execute the
     * named action.
     */
    EXECUTOR_LIST_QUERY,

    /**
     * This is sent in response to a EXECUTOR_LIST_QUERY.
     */
    EXECUTOR_LIST_RESULT,

    /**
     * Indicates that a particular type or action should be flushed from any
     * caches.
     */
    CACHE_EXPIRE,

    /**
     * Carries a command for responding to a breakpoint.
     */
    BREAKPOINT_RESPONSE,

    /**
     * Requests Lumen evaluate an ATR expression in the context of a paused
     * procedure.
     */
    EXPR_EVAL_REQUEST,

    /**
     * Carries the result of evaluating an ATR expression in the context of a
     * paused procedure.
     */
    EXPR_EVAL_RESULT,

    /**
     * This is a message channel that any client can use to send messages to the Bridge
     * Jms Spine instance. The Bridge client at a minimum will be listening on this topic
     * for messages. The payload is any Object that is Serializable. The Bridge is
     * responsible for deserializing the object itself.
     */
    CUSTOM_BRIDGE_MESSAGE,

    /**
     * This is a message channel that any client can use to send messages to the Lumen
     * Jms Spine instance. The Lumen client at a minimum will be listening on this topic
     * for messages. The payload is any Object that is Serializable. Lumen is
     * responsible for deserializing the object itself.
     */
    CUSTOM_LUMEN_MESSAGE,

    /**
     * This is a message channel that any client can use to send messages to the Lapdog
     * Jms Spine instance. The Lapdog client at a minimum will be listening on this topic
     * for messages. The payload is any Object that is Serializable. Laptog is
     * responsible for deserializing the object itself.
     */
    CUSTOM_LAPDOG_MESSAGE,

    /**
     * This is a message channel that any client can use to send messages to the Shell
     * Jms Spine instance. The Shell client at a minimum will be listening on this topic
     * for messages. The payload is any Object that is Serializable. The Shell is
     * responsible for deserializing the object itself.
     */
    CUSTOM_SHELL_MESSAGE,

    /**
     * This is a message channel that any client can use to send messages to the Editor
     * Jms Spine instance. The Editor client at a minimum will be listening on this topic
     * for messages. The payload is any Object that is Serializable. The Editor is
     * responsible for deserializing the object itself.
     */
    CUSTOM_EDITOR_MESSAGE,

    /**
     * This is a message channel that any client can use to send messages to the Remote
     * Xml module.
     */
    CUSTOM_REMOTEXML_MESSAGE
}
