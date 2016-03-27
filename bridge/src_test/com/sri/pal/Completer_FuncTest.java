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

// $Id: Completer_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.actionmodels.DebugExecutor;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;
import com.sri.tasklearning.spine.util.SubTasksFinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests LAPDOG's ability to build procedures with completer actions in them. A
 * completer action fills in the gaps in data flow.
 */
public class Completer_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    private static final String NS1 = "cft.completers";
    private static final String VERS1 = "1.0";
    private static final String NS2 = "cft.debug";
    private static final String VERS2 = "1.0";

    private static ActionDef makeStringDef;
    private static ActionDef logDef;
    private static ActionDef concatDef;
    private static DebugExecutor debugExec;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ActionModels.COMPLETERS), NS1);
        actionModel.load(ActionModels.class.getResource(ActionModels.DEBUG),
                NS2);

        debugExec = new DebugExecutor();

        SimpleTypeName makeStringName = (SimpleTypeName) TypeNameFactory
                .makeName("makeString", VERS2, NS2);
        makeStringDef = (ActionDef) actionModel.getType(makeStringName);
        actionModel.registerExecutor(makeStringName, debugExec);

        SimpleTypeName logName = (SimpleTypeName) TypeNameFactory.makeName(
                "log", VERS2, NS2);
        logDef = (ActionDef) actionModel.getType(logName);
        actionModel.registerExecutor(logName, debugExec);

        SimpleTypeName concatName = (SimpleTypeName) TypeNameFactory.makeName(
                "concat", VERS1, NS1);
        concatDef = (ActionDef) actionModel.getType(concatName);
        actionModel.registerExecutor(concatName, debugExec);
    }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    @BeforeMethod
    public void reset()
            throws Exception {
        callbackHandler.reset();
    }

    /**
     * Demonstrate
     *
     * <pre>
     * makeString(-"foo")
     * makeString(-"bar")
     * log(+"foobar")
     * </pre>
     *
     * and expect a completer action to be inserted to concatenate the two
     * strings. We should end up with a procedure of 0 inputs and 3 outputs, and
     * 4 actions.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void simpleCompleter()
            throws Exception {
        String str1 = "foo";
        String str2 = "bar";

        List<ActionStreamEvent> demo = new ArrayList<ActionStreamEvent>();

        ActionInvocation event = makeStringDef.invoke(null);
        event.setValue(0, str1);
        demo.add(event);

        event = makeStringDef.invoke(null);
        event.setValue(0, str2);
        demo.add(event);

        event = logDef.invoke(null);
        event.setValue(0, str1 + str2);
        demo.add(event);

        Set<TypeName> extraTypes = new HashSet<TypeName>();
        extraTypes.add(concatDef.getName());
        ProcedureDef proc = learningBridge.learn("simpleCompleter", extraTypes,
                demo.toArray(new ActionStreamEvent[0]));

        log.info("Procedure learned: {}", proc.getAtrStr());

        Assert.assertEquals(0, proc.numInputParams());
        Assert.assertEquals(3, proc.size());
        ATRActionDeclaration atr = proc.getAtr();
        Set<SimpleTypeName> calledActions = SubTasksFinder.findSubTasks(atr);
        Assert.assertTrue(calledActions.contains(makeStringDef.getName()),
                makeStringDef.toString());
        Assert.assertTrue(calledActions.contains(logDef.getName()),
                logDef.toString());
        Assert.assertTrue(calledActions.contains(concatDef.getName()),
                concatDef.toString());
    }
}
