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

// $Id: TypeUtil_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.net.URL;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.bridge.SparkTask_FuncTest;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * This is actually testing TypeUtil, which resides in the Spine, but the best
 * data sources are here in the Bridge source tree.
 */
public class TypeUtil_Test
        extends PALTestCase {
    private String procStr;
    private String actionStr;
    private String typeStr;

    @BeforeMethod
    public void setup()
            throws Exception {
        URL procUrl = SparkTask_FuncTest.class
                .getResource("SparkTask_proc1.xml");
        procStr = ProcedureLearner.readWholeFile(procUrl);
        URL actionUrl = ActionModels.class.getResource("actions_test.xml");
        actionStr = ProcedureLearner.readWholeFile(actionUrl);
        URL typeUrl = ActionModels.class.getResource("types_test.xml");
        typeStr = ProcedureLearner.readWholeFile(typeUrl);
    }

    @Test
    public void isProcedureString() {
        System.out.println("proc: " + procStr);
        Assert.assertTrue(TypeUtil.isProcedureString(procStr));
        Assert.assertFalse(TypeUtil.isProcedureString(actionStr));
        Assert.assertFalse(TypeUtil.isProcedureString(typeStr));
    }

    @Test
    public void isActionString() {
        Assert.assertFalse(TypeUtil.isActionString(procStr));
        Assert.assertTrue(TypeUtil.isActionString(actionStr));
        Assert.assertFalse(TypeUtil.isActionString(typeStr));
    }
}
