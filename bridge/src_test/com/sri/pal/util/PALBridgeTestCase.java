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

package com.sri.pal.util;

import java.net.URL;
import java.rmi.RemoteException;

import com.sri.pal.ActionModel;
import com.sri.pal.Bridge;
import com.sri.pal.Learner;
import com.sri.pal.PALException;
import com.sri.pal.ProcedureLearner;
import com.sri.pal.VerifiableCallbackHandler;

/**
 * @author Valerie Wagner
 *         Date: Feb 25, 2009
 */
public abstract class PALBridgeTestCase extends PALTestCase {

    public static Bridge palBridge;
    public static VerifiableCallbackHandler callbackHandler;
    public static Learner learningBridge;
    public static ActionModel actionModel;

    protected static void setup(URL actionModelFile, String namespace) throws RemoteException, PALException {
        ProcedureLearner.buildBridge(actionModelFile, namespace);
        init();
    }

    public static void setup() throws Exception {
        ProcedureLearner.buildBridge();
        init();
    }

    protected static void setupNoStorage()
            throws Exception {
        ProcedureLearner.buildBridgeNoStorage();
        init();
    }

    private static void init() {
        palBridge = ProcedureLearner.palBridge;
        callbackHandler = ProcedureLearner.callbackHandler;
        learningBridge = palBridge.getLearner();
        actionModel = palBridge.getActionModel();
    }
}
