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

// $Id: LearnCfg_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.events;

import com.sri.pal.ProcedureDef;
import com.sri.pal.util.PALBridgeTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Properties;

/**
 * Use LapdogConfiguration to disable loop learning.
 */
public class LearnCfg_FuncTest
        extends PALBridgeTestCase {

    @BeforeClass
    public static void setup()
            throws Exception {
        LoopLearning_FuncTest.setup();
    }

    @AfterClass
    public static void teardown()
            throws Exception {
        LoopLearning_FuncTest.teardown();
    }

    /**
     * Call a test that we know works for loop learning. There, it's expected to
     * pass. Here, it's expected to fail.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void learnLoopAndSerialize()
            throws Exception {
        Properties learnProps = new Properties();
        learnProps.setProperty("lapdog.disable-loop-learning", Boolean.TRUE.toString());

        ProcedureDef proc = LoopLearning_FuncTest.loopTest1(learnProps);
        Assert.assertFalse(proc.getSource().contains("forall"));
    }
}
