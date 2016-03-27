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

package com.sri.pal.events;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.Struct;
import com.sri.pal.StructDef;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

// TODO This test needs to work if it's a child of PALTestCase, not PALBridgeTestCase.
public class LoopMTRS114_FuncTest extends PALBridgeTestCase {

    private static final String TESTNS_AM = "../test/data/action-model-3.0/model1.xml";
    private static final String NAMESPACE = "TESTNS";

    private static SimpleTypeName act148Name;
    private static SimpleTypeName act149Name;
    private static SimpleTypeName act168Name;
    private static SimpleTypeName act178Name;
    private static SimpleTypeName act194Name;
    private static SimpleTypeName act196Name;
    private static SimpleTypeName act209Name;
    private static SimpleTypeName act223Name;

    @BeforeClass
    public static void setup() throws Exception {
        File file = new File(TESTNS_AM);
        URL url = file.toURI().toURL();
        setup(url, NAMESPACE);

        act168Name = (SimpleTypeName) TypeNameFactory.makeName("action168",
                "2.0", NAMESPACE);
        actionModel.registerExecutor(act168Name, callbackHandler);
        act196Name = (SimpleTypeName) TypeNameFactory.makeName("action196",
                "2.0", "TESTNS");
        actionModel.registerExecutor(act196Name, callbackHandler);
        act149Name = (SimpleTypeName) TypeNameFactory.makeName("action149",
                "2.0", "TESTNS");
        actionModel.registerExecutor(act149Name, callbackHandler);
        act148Name = (SimpleTypeName) TypeNameFactory.makeName("action148",
                "2.0", "TESTNS");
        actionModel.registerExecutor(act148Name, callbackHandler);
        act194Name = (SimpleTypeName) TypeNameFactory.makeName("action194",
                "2.0", "TESTNS");
        actionModel.registerExecutor(act194Name, callbackHandler);
        act223Name = (SimpleTypeName) TypeNameFactory.makeName("action223",
                "2.0", "TESTNS");
        actionModel.registerExecutor(act223Name, callbackHandler);
        act209Name = (SimpleTypeName) TypeNameFactory.makeName("action209",
                "2.0", "TESTNS");
        actionModel.registerExecutor(act209Name, callbackHandler);
        act178Name = (SimpleTypeName) TypeNameFactory.makeName("action178",
                "2.0", "TESTNS");
        actionModel.registerExecutor(act178Name, callbackHandler);
    }

    @AfterClass
    public static void teardown() throws PALRemoteException {
        palBridge.shutdown();
    }

    @Test
    public void reproduceBug() throws RemoteException, PALException, MalformedURLException {

        URL url = new File(TESTNS_AM).toURI().toURL();
        ProcedureLearner.buildBridge(url, NAMESPACE);
        List<ActionInvocation> events = new ArrayList<ActionInvocation>();


//



                List<Object> list1 = new ArrayList<Object>();
        list1.add("0x0732666562656662663832343036613237613a353737313466343838383662383363653a666261373931623530643a2d33643837");
        list1.add("0x0732666562656662663832343036613237613a353737313466343838383662383363653a666261373931623530643a2d33633337");
        ActionInvocation event1 = ((ActionDef) actionModel
                .getType(act168Name)).bindAll(
                null, "0x080e0000011d640de629c0a8012186eb", "members", list1);
        events.add(event1);


//


                List<Object> list2 = new ArrayList<Object>();
        list2.add("0x0732666562656662663832343036613237613a353737313466343838383662383363653a666261373931623530643a2d33643837");
        list2.add("0x0732666562656662663832343036613237613a353737313466343838383662383363653a666261373931623530643a2d33633337");

        List<Struct> list3 = new ArrayList<Struct>();
        StructDef structDef1 = (StructDef) actionModel.getType(TypeNameFactory.makeName(
                "type118", "2.0", NAMESPACE));
//		List<Object> list4 = new ArrayList<Object>();
//		List<Object> list5 = new ArrayList<Object>();
//		list5.add("0");
//		list5.add("1");
//		list4.add(list5);
        Struct struct1 = new Struct(structDef1);
        struct1.setValue(0, 1747.5);
        struct1.setValue(1, 550.0);
        list3.add(struct1);
//		list4.add("1747.5");
//		list4.add("550.0");
//		list3.add(list4);
//		List<Object> list7 = new ArrayList<Object>();
//		List<Object> list8 = new ArrayList<Object>();
//		list8.add("0");
//		list8.add("1");
//		list7.add(list8);
        Struct struct2 = new Struct(structDef1);
        struct2.setValue(0, 1772.5);
        struct2.setValue(1, 550.0);
        list3.add(struct2);
//		list7.add("1772.5");
//		list7.add("550.0");
//		list3.add(list7);
//		list3.add("1772.5");
//		list3.add("550.0");

                List<Object> list10 = new ArrayList<Object>();
        list10.add("0x080e0000011d640de629c0a80121beda");
        list10.add("0x080e0000011d640de629c0a80121bedb");
        ActionInvocation event2 = ((ActionDef) actionModel.getType(act196Name)).bindAll(null,
                "0xfc59cc1b4c6f63616c576f726b73706163652b4c6f63616c5265662e70616c",
                list2,
                list3,
                list10
        );
        events.add(event2);


//


        ActionInvocation event3 = ((ActionDef) actionModel.getType(act149Name)).bindAll(null,
                "0x0732666562656662663832343036613237613a353737313466343838383662383363653a666261373931623530643a2d33643837",
                "12"
        );
        events.add(event3);


//


        ActionInvocation event4 = ((ActionDef) actionModel.getType(act149Name)).bindAll(null,
                "0x0732666562656662663832343036613237613a353737313466343838383662383363653a666261373931623530643a2d33633337",
                "13"
        );
        events.add(event4);


//



        ActionInvocation event5 = ((ActionDef) actionModel.getType(act148Name)).bindAll(null,
                "com.generic.value2",
                "0x080e0000011aa28a4ea27f0000018264",
                "0x080e0000011d640de629c0a80121bf57"
        );
        events.add(event5);


//


                List<Object> list11 = new ArrayList<Object>();
        list11.add("0x080e0000011d640de629c0a80121bf57");

        List<Struct> list12 = new ArrayList<Struct>();
//		List<Object> list13 = new ArrayList<Object>();
//		List<Object> list14 = new ArrayList<Object>();
//		list14.add("0");
//		list14.add("1");
//		list13.add(list14);
        Struct struct3 = new Struct(structDef1);
        struct3.setValue(0, 2065.5);
        struct3.setValue(1, 777.0);
        list12.add(struct3);
//		list13.add("2065.5");
//		list13.add("777.0");
//		list12.add(list13);
//		list12.add("2065.5");
//		list12.add("777.0");

                List<Object> list16 = new ArrayList<Object>();
        list16.add("0x080e0000011d640de629c0a80121bf59");
        ActionInvocation event6 = ((ActionDef) actionModel.getType(act196Name)).bindAll(null,
                "0xfc59cc1b4c6f63616c576f726b73706163652b4c6f63616c5265662e70616c",
                list11,
                list12,
                list16
        );
        events.add(event6);


//



                List<Object> list17 = new ArrayList<Object>();
        list17.add("SUPT1");
        ActionInvocation event7 = ((ActionDef) actionModel.getType(act194Name)).bindAll(null,
                "12",
                "Description",
                list17
        );
        events.add(event7);


//



        ActionInvocation event8 = ((ActionDef) actionModel.getType(act223Name)).bindAll(null,
                "0x080e0000011d640de629c0a80121bf57",
                "Name",
                "SUPT1"
        );
        events.add(event8);


//


                List<Object> list18 = new ArrayList<Object>();
        list18.add("0x080e0000011d640de629c0a80121bf57");

                List<Object> list19 = new ArrayList<Object>();
        list19.add("0x080e0000011d640de629c0a80121bf59");
        ActionInvocation event9 = ((ActionDef) actionModel.getType(act209Name)).bindAll(null,
                "0xfc59cc1b4c6f63616c576f726b73706163652b4c6f63616c5265662e70616c",
                list18,
                list19
        );
        events.add(event9);


//



                List<Object> list20 = new ArrayList<Object>();
        list20.add("0x080e0000011d640de629c0a80121bf57");
        ActionInvocation event10 = ((ActionDef) actionModel.getType(act178Name)).bindAll(null,
                "0x0732666562656662663832343036613237613a353737313466343838383662383363653a666261373931623530643a2d33643837",
                "members",
                list20
        );
        events.add(event10);


//



        ActionInvocation event11 = ((ActionDef) actionModel.getType(act148Name)).bindAll(null,
                "com.generic.value2",
                "0x080e0000011aa28a4ea27f0000018264",
                "0x080e0000011d640de629c0a80121bf81"
        );
        events.add(event11);


//


                List<Object> list21 = new ArrayList<Object>();
        list21.add("0x080e0000011d640de629c0a80121bf81");

        List<Struct> list22 = new ArrayList<Struct>();
//		List<Object> list23 = new ArrayList<Object>();
//		List<Object> list24 = new ArrayList<Object>();
//		list24.add("0");
//		list24.add("1");
//		list23.add(list24);
        Struct struct4 = new Struct(structDef1);
        struct4.setValue(0, 2093.5);
        struct4.setValue(1, 781.0);
        list22.add(struct4);
//		list23.add("2093.5");
//		list23.add("781.0");
//		list22.add(list23);
//		list22.add("2093.5");
//		list22.add("781.0");

                List<Object> list26 = new ArrayList<Object>();
        list26.add("0x080e0000011d640de629c0a80121bf83");
        ActionInvocation event12 = ((ActionDef) actionModel.getType(act196Name)).bindAll(null,
                "0xfc59cc1b4c6f63616c576f726b73706163652b4c6f63616c5265662e70616c",
                list21,
                list22,
                list26
        );
        events.add(event12);


//



                List<Object> list27 = new ArrayList<Object>();
        list27.add("SUPT_3");
        ActionInvocation event13 = ((ActionDef) actionModel.getType(act194Name)).bindAll(null,
                "13",
                "Description",
                list27
        );
        events.add(event13);


//



        ActionInvocation event14 = ((ActionDef) actionModel.getType(act223Name)).bindAll(null,
                "0x080e0000011d640de629c0a80121bf81",
                "Name",
                "SUPT_3"
        );
        events.add(event14);


//


                List<Object> list28 = new ArrayList<Object>();
        list28.add("0x080e0000011d640de629c0a80121bf81");

                List<Object> list29 = new ArrayList<Object>();
        list29.add("0x080e0000011d640de629c0a80121bf83");
        ActionInvocation event15 = ((ActionDef) actionModel.getType(act209Name)).bindAll(null,
                "0xfc59cc1b4c6f63616c576f726b73706163652b4c6f63616c5265662e70616c",
                list28,
                list29
        );
        events.add(event15);


//



                List<Object> list30 = new ArrayList<Object>();
        list30.add("0x080e0000011d640de629c0a80121bf81");
        ActionInvocation event16 = ((ActionDef) actionModel.getType(act178Name)).bindAll(null,
                "0x0732666562656662663832343036613237613a353737313466343838383662383363653a666261373931623530643a2d33633337",
                "members",
                list30
        );
        events.add(event16);

        ProcedureDef proc = ProcedureLearner.learnProcedure(events, "MTRS114Procedure");
        ProcedureLearner.invokeTask(proc, events, false);
    }
}
