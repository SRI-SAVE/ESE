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

package com.sri.tasklearning.spine.messages.contents;

/**
 * This class is a variation on the UID (Unique IDentifier) notion.
 * For task execution, status updates are broadcast from the
 * executor. These status updates are identified as belonging to
 * a particular execution request by their UID.
 * The ExecutionUID used to identify the ExecutionRequest
 * message is used to identify all further status messages related
 * to this execution request.
 * This subclass was created to show that although it is not unique
 * accross the entire system, it is unique to an individual execution.
 * The parentUID of status messages will also be an ExecutionUI.
 */
public class TransactionUID extends UID {
    private static final long serialVersionUID = 1L;

    public TransactionUID(String sender, int id) {
        super(sender, id);
    }

    public TransactionUID(String uidString) {
        super(uidString);
    }
}
