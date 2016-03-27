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

// $Id: UID_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages.contents;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import com.sri.tasklearning.spine.impl.jms.util.SpineTestCase;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UID_Test
        extends SpineTestCase {
    private UID uid1;
    private UID uid2;
    private UID uid3;
    private UID uid4;
    private UID uid5;

    @BeforeMethod
    public void setup() {
        uid1 = new UID("foo", 1);
        uid2 = new UID("foo", 1);
        uid3 = new UID("bar", 1);
        uid4 = new UID("foo", 2);
        uid5 = new UID("bar", 2);
    }

    @Test
    public void testEquals() {
        assertTrue(uid1.equals(uid2));
        assertTrue(uid2.equals(uid1));
        assertFalse(uid1.equals(uid3));
        assertFalse(uid3.equals(uid1));
        assertFalse(uid1.equals(uid4));
        assertFalse(uid4.equals(uid1));
        assertFalse(uid1.equals(uid5));
        assertFalse(uid5.equals(uid1));

        assertFalse(uid2.equals(uid3));
        assertFalse(uid3.equals(uid2));
        assertFalse(uid2.equals(uid4));
        assertFalse(uid4.equals(uid2));
        assertFalse(uid2.equals(uid5));
        assertFalse(uid5.equals(uid2));

        assertFalse(uid3.equals(uid4));
        assertFalse(uid4.equals(uid3));
        assertFalse(uid3.equals(uid5));
        assertFalse(uid5.equals(uid3));

        assertFalse(uid4.equals(uid5));
        assertFalse(uid5.equals(uid4));
    }

    @Test
    public void testHashCode() {
        assertEquals(uid1.hashCode(), uid2.hashCode());
    }
}
