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

package com.sri.tasklearning.ui.core;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRLocator;
import com.sri.pal.ActionModelDef;
import com.sri.pal.Bridge;
import com.sri.pal.GlobalActionListener;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.PALStatusMonitor;
import com.sri.pal.ProcedureDef;
import com.sri.pal.TypeStorage;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.step.Wrapper;

/**
 * Singleton that abstracts interactions with Lumen, the Bridge and other
 * core task learning components. 
 */
public final class BackendFacade extends BackendInterface {
    private static final Logger log = LoggerFactory
            .getLogger(BackendFacade.class);

    private static BackendFacade instance = new BackendFacade();

    private Bridge bridge;

    private boolean startedBackend = false;

    private BackendFacade() {

    }

    public static BackendFacade getInstance() {
        return instance;
    }

    @Override
    public void connect(String clientName) {
        if (bridge != null)
            return;

        // This block of code only exists for development purposes. There is
        // currently no case in production where the Editor will have to start
        // PAL.
        try {                  
            if (!PALStatusMonitor.isTaskLearningRunning()) {
                Bridge.startPAL();
                startedBackend = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            bridge = Bridge.newInstance(clientName + System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Bridge getBridge() {
        return bridge;
    }

    /**
     * Disconnect from the PAL backend, or in the (development-only) case where
     * the Editor or other PAL UI component started the backend, shut it down. 
     */
    @Override
    public void disconnect() {
        try {
            if (startedBackend) {
                bridge.shutdown();
            } else 
                bridge.disconnect();            
        } catch (Exception e) {
            log.error("Exception occurred while disconnecting from PAL backend.", e);
        }
    }
    
    @Override
    public ActionModelDef getType(TypeName typeName) throws PALException {
        return bridge.getActionModel().getType(typeName);
    }

    @Override
    public Set<SimpleTypeName> listTypes(TypeStorage.Subset... subset) throws PALException {
        return bridge.getActionModel().listTypes(subset);
    }

    @Override
    public void storeType(SimpleTypeName name, ActionModelDef type) throws PALException {
        bridge.getActionModel().storeType(name, type);
    }

    @Override
    public Map<String, String> getNamespaceMetadata(String ns, String version) throws PALException {
        return bridge.getActionModel().getNamespaceMetadata(ns, version);
    }

    @Override
    public ProcedureDef palExecutorLoad(String xmlSource) throws PALException {
        return bridge.getPALExecutor().load(xmlSource);
    }

    @Override
    public void addActionListener(GlobalActionListener listener) {
        bridge.addActionListener(listener);
    }

    @Override
    public void shutdown() throws PALRemoteException {
        bridge.shutdown();
    }

 
    public static ATR locate(int preorderIndex, CommonModel root) {
        ATR toRet = new ATRLocator<CommonModel>(root).getATR(preorderIndex);
        if (toRet instanceof Wrapper) {
            return ((Wrapper) toRet).getWrapped();
        } else {
            return toRet;
        }
    }

    public static ATR locate(int pIdx, CommonModel root, Integer parentLevel) {
        ATRLocator<CommonModel> loc = new ATRLocator<CommonModel>(root);
        
        for (int i = 1; i <= parentLevel; i++)
            pIdx = loc.getParentOf(pIdx);
        
        ATR toRet = loc.getATR(pIdx);
            
        if (toRet instanceof Wrapper) {
            return ((Wrapper) toRet).getWrapped();
        } else {
            return toRet;
        }
    }

   
	
}

