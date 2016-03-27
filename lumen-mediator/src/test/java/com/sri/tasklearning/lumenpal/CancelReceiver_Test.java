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

package com.sri.tasklearning.lumenpal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.lumenpal.mock.MockLumen;
import com.sri.tasklearning.lumenpal.util.LumenMediatorTestCase;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.messages.CancelRequest;
import com.sri.tasklearning.spine.messages.ExecuteRequest;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CancelReceiver_Test extends LumenMediatorTestCase {
    private MockLumen lumen;
    private String bridge = "Bridge";
    private TransactionUID parentUID;
    private TransactionUID uid;
    private SimpleTypeName typeName;
    private List<Object> inParams;

    @BeforeMethod
    public void setUp() {
        this.lumen = new MockLumen();
        parentUID = null;
        uid = new TransactionUID(bridge, 0);
        typeName = (SimpleTypeName) TypeNameFactory.makeName("test.type");
        inParams = new ArrayList<Object>();
    }

    @Test
    public void canConstruct() {
        CancelReceiver cancelReceiver = new CancelReceiver(lumen);
        assertNotNull(cancelReceiver);
        assertTrue(CancelReceiver.class.isInstance(cancelReceiver));
    }

    @Test
    public void rejectsIncorrectMessages() throws MessageHandlerException {
        CancelReceiver cancelReceiver = new CancelReceiver(lumen);

        ExecuteRequest badMessage = new ExecuteRequest(bridge, uid, parentUID,
                typeName, inParams, false);
        cancelReceiver.handleMessage(badMessage);

        // The message should have been stopped dead since it was the wrong type,
        // lets make sure
        assertNull(lumen.getLastCallUid());
    }

    @Test
    public void acceptsCorrectMessages() throws MessageHandlerException {
        CancelReceiver cancelReceiver = new CancelReceiver(lumen);

        CancelRequest goodMessage = new CancelRequest(bridge, uid);
        cancelReceiver.handleMessage(goodMessage);

        assertNotNull(lumen.getLastCallUid());
        assertEquals(uid, lumen.getLastCallUid());
    }

}
