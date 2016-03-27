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

/* $Id: TestUtils.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $ */
package com.sri.pal.util;

import java.util.Collection;
import java.util.Iterator;

import org.testng.Assert;

/**
 * @author Valerie Wagner
 *         Date: Sep 29, 2006
 */
public class TestUtils {
    /**
     * Creates a unique SPARK test dir
     * @return unique path to test dir based on system time
     */
    public static String getTestSparkDir() {
        return "build/persist/persist-test-" + System.currentTimeMillis();
    }

    public static <T> void assertArraysEqual(String message,
                                             Collection<T> expected,
                                             Collection<T> actual) {
        Iterator<T> iter = expected.iterator();
        while(iter.hasNext()) {
            T exo = iter.next();
            Assert.assertTrue(actual.contains(exo), message
                    + ": Does not contain item " + exo);
        }

        iter = actual.iterator();
        while(iter.hasNext()) {
            T aco = iter.next();
            Assert.assertTrue(expected.contains(aco), message
                    + ": Contains extra item " + aco);
        }
    }
}
