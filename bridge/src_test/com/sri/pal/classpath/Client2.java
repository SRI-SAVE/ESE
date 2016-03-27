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

// $Id: Client2.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.classpath;

import static org.testng.AssertJUnit.assertNotNull;

import java.net.URL;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionModel;
import com.sri.pal.Bridge;
import com.sri.pal.Learner;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.Type1;
import com.sri.pal.TypeDef;
import com.sri.pal.VerifiableCallbackHandler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client2 {
    private static final Logger log = LoggerFactory.getLogger("TestSourceLogger");

    private static final String NAMESPACE = "client2";
    static final String PROC_NAME = "proc";

    private static VerifiableCallbackHandler vch;

    public static void main(String args[])
            throws Exception {
        /* Ensure we have this class available on our classpath. */
        Class.forName(Classpath_FuncTest.TYPE1_CLASS);

        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(20 * 1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
                System.exit(0);
            }
        };
        t.setDaemon(true);
        t.start();

        PALTestCase.palTestCaseInit();

        try {
            run();
        } catch (Exception e) {
            log.error("Uncaught exception", e);
        }
    }

    private static void run()
            throws Exception {
        Bridge bridge = Bridge.newInstance(Client2.class.getSimpleName());
        ProcedureLearner.setStorage(Client2.class, bridge);
        ActionModel actionModel = bridge.getActionModel();
        URL url = Client2.class.getResource("client2.xml");
        vch = new VerifiableCallbackHandler();
        actionModel.load(url, NAMESPACE);
        log.debug("AM loaded");

        TypeDef type1 = (TypeDef) actionModel.getType(TypeNameFactory.makeName(
                "type1", "1.0", NAMESPACE));
        assertNotNull(type1);
        SimpleTypeName actionName = (SimpleTypeName) TypeNameFactory.makeName(
                "passthru", "1.0", NAMESPACE);
        actionModel.registerExecutor(actionName, vch);
        ActionDef actionDef = (ActionDef) actionModel.getType(actionName);
        log.debug("got types");

        Type1 obj = new Type1("mock value");
        ActionInvocation action = actionDef.bindAll(null, obj, obj);

        Learner learner = bridge.getLearner();
        ProcedureDef proc = learner.learn(PROC_NAME, null, action);
        log.debug("learned {}", proc.getSource());

        for (int i = 0; i < 2; i++) {
            ProcedureInvocation invoc = proc.invoke(null);
            invoc.start();
            log.debug("Invocation {} started", i);
            invoc.waitUntilFinished();
            log.debug("Invocation {} ended", i);
        }
    }
}
