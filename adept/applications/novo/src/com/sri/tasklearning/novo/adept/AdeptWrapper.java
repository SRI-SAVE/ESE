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
package com.sri.tasklearning.novo.adept;

import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionModel;
import com.sri.pal.ActionModelDef;
import com.sri.pal.Bridge;
import com.sri.pal.PALException;
import com.sri.pal.PALStatusListener;
import com.sri.pal.PALStatusMonitor;
import com.sri.pal.TypeStorage;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.novo.Novo;

/**
 * AdeptWrapper acts as an interface to Adept, allowing access to functionality
 * such as instrumentation, type instantiation, and setting up the initial state
 * of Adept by loading the Novo action model if ncessary. This class handles
 * creating a thread that will listen for the presence of the Adept/task 
 * learning system and 
 */
public final class AdeptWrapper {
    private static final Logger log = LoggerFactory
            .getLogger(AdeptWrapper.class);

    public static final String NAMESPACE = "novo";
    public static final String VERSION = "1.0";
    public static final String APP_NAME = "novo";
    
    private static Bridge bridge;

    private static boolean connectedToTaskLearning = false;
    private static boolean disconnecting = false;    
    
    public static void initialize() {
        PALStatusListener listener = new PALStatusListener() {
            public void newStatus(PALStatusListener.Status status) {
                switch (status) {
                case DOWN:
                case UNKNOWN:
                    if (connectedToTaskLearning)
                        log.info("Disconnected from PAL");
                    connectedToTaskLearning = false;
                    break;
                case UP:
                    if (!disconnecting && !connectedToTaskLearning)
                        connectBridge();
                    break;
                case WATCHDOG_UP:
                    // Ignore; we'll have transitioned through UP already.
                    break;
                }
            }
        };
        
        PALStatusMonitor.addListener(listener);
    }
    
    private static void connectBridge() {
        try {
            bridge = Bridge.newInstance(APP_NAME + System.currentTimeMillis());

            ActionModel actionModel = bridge.getActionModel();

            TypeName name = TypeNameFactory.makeName("piece", VERSION,
                    NAMESPACE);

            // Check to see if our action model is already registered
            if (actionModel.getType(name) == null) {
                URL url = AdeptWrapper.class.getResource("novo.xml");
                actionModel.load(url, NAMESPACE);
                Map<String, String> meta = new TreeMap<String, String>();
                URL path = Novo.class.getResource("images");
                meta.remove("resource_path");
                meta.put("resource_path", path.toString()); 
                actionModel.addNamespaceMetadata("novo", "1.0", meta);
            }

            NovoExecutor executor = NovoExecutor.getInstance();

            for (SimpleTypeName type : bridge.getActionModel().listTypes(
                    TypeStorage.Subset.ACTION)) {
                if (type.getNamespace().equals(NAMESPACE))
                    actionModel.registerExecutor(type, executor);
            }
            connectedToTaskLearning = true;            
            log.info("Connected to PAL");
        } catch (PALException e) {
            log.error("Failed to load action model: ", e);
        }
    }
    
    public static boolean isConnectedToTaskLearning() {
        return connectedToTaskLearning; 
    }
    
    public static void disconnect() {
        connectedToTaskLearning = false;
        if (bridge != null) {
            try {
                bridge.disconnect();
            } catch (PALException e) {
                // We're exiting so we don't care
            }
        }
        disconnecting = true;
    }

    /**
     * Takes an action name along with its arguments and invokes it so that
     * its instrumentation is collected by Adept. Input variables should come 
     * before output variables. This function does not accept null arguments or fewer
     * arguments than what is defined for the given action in the actionmodel.
     */
    public static void instrumentAction(NovoAction act, Object... argVals) {
        String name = act.getFunctor();
        
        if (!connectedToTaskLearning) {
            log.warn("Could not instrument {}: learning not running.", name);
            return;
        }
        ActionDef def = (ActionDef) getType(name);
        ActionInvocation action = null;
        
        if (def == null)
            return; 

        // set input arguments
        Object[] inputs = new Object[def.numInputParams()];
        int index = 0;
        for (; index < def.numInputParams(); index++) {
            inputs[index] = argVals[index];
        }

        try {
            action = def.invoke(null, inputs);
        } catch (PALException e) {
            log.error("Action invocation failed: ", e);
            return;
        }

        // set output arguments
        for (; index < argVals.length; index++) {
            action.setValue(index, argVals[index]);
        }
        action.setStatus(ActionInvocation.Status.ENDED);
    }

    /**
     * Requests a type from Adept given a type name.
     * 
     * @param typeName
     * @return
     */
    public static ActionModelDef getType(String typeName) {
        try {
            return bridge.getActionModel()
                    .getType(TypeNameFactory.makeName(typeName, VERSION, NAMESPACE));
        } catch (PALException e) {
            log.error("Type get failed: ", e);
            return null;
        }
    }
}
