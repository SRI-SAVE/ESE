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

public enum JmsClient {
    // The LOCAL Spine instance is the master spine, of which there can only be one.
    LOCAL,

    // The REMOTE Spine instances are the slave spine - there can be more than one but they
    // must have unique names. The REMOTE Spine also takes advantage of all the available
    // failover and connection reestablishment protocols
    REMOTE,

    // The TEST Spine instance is a throw away spine instance that has no failover capability.
    // It is used to quickly probe the initial upstate of the LOCAL instance
    TEST,

    // The DURABLE_TEST Spine instance has the same failover capability as the REMOTE spines
    // This is to allow the SpineStatus class to reconnect its spine if the existing connection
    // is interrupted.
    DURABLE_TEST
}
