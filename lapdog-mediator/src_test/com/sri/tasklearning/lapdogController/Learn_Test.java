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

// $Id: Learn_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lapdogController;

import static org.testng.AssertJUnit.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.impl.CTRParameter;
import com.sri.ai.lumen.atr.learning.ATRDemonstratedAction;
import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.ai.lumen.atr.learning.impl.ATRDemonstratedActionImpl;
import com.sri.ai.lumen.atr.learning.impl.ATRDemonstrationImpl;
import com.sri.ai.lumen.atr.term.ATRVariable;
import com.sri.ai.lumen.atr.term.impl.CTRVariable;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.impl.jms.util.MockSpine;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.util.LogUtil;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class Learn_Test {
    private LapdogClientCallServiceImpl service;
    private LapdogClient lapdogClient;
    private MockSpine spine;
    private SimpleTypeName actionName;
    private ATRActionDeclaration actionDef;
    private ATRTypeDeclaration stringDef;
    private SimpleTypeName stringName;

    @BeforeMethod
    public void setup()
            throws Exception {
        LogUtil.configureLogging("lapdog", Learn_Test.class);

        spine = new MockSpine();
        LapdogFacade lapdogFacade = new LapdogFacade(spine);
        TypePublicationFacade typeFacade = new TypePublicationFacade(
                lapdogFacade);
        lapdogClient = new LapdogClient(typeFacade, lapdogFacade, spine);
        service = lapdogClient.getService();
        lapdogClient._lapdogInitialized = true;

        stringName = (SimpleTypeName) TypeNameFactory.makeName("String");
        stringDef = ATRTestUtil.makeCustomType(stringName, String.class);
        actionName = (SimpleTypeName) TypeNameFactory.makeName("action1");
        ATRParameter[] params = new ATRParameter[1];
        ATRVariable var = new CTRVariable("inArg");
        params[0] = new CTRParameter(var, Modality.INPUT, stringName.getFullName());
        actionDef = ATRTestUtil.makeAction(actionName, params, null, null);

        spine.addType(actionName, actionDef);
        spine.addType(stringName, stringDef);
    }

    @Test
    public void testLearn()
            throws Exception {
        List<ATRDemonstratedAction> actions = new ArrayList<ATRDemonstratedAction>();
        List<Object> args = new ArrayList<Object>();
        args.add("a");
        ATRDemonstratedAction action = new ATRDemonstratedActionImpl("action1", args);
        actions.add(action);
        ATRDemonstration demo = new ATRDemonstrationImpl(actions);
        String result = service.learn(demo, new Properties(), null, "test");

        assertNotNull(result);
    }
}
