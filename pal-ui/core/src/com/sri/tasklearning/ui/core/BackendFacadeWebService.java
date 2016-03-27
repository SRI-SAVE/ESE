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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.pal.ActionModelDef;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.GlobalActionListener;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.TypeStorage;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.validation.EditorIssue;

import javafx.util.Callback;

public class BackendFacadeWebService extends BackendInterface {
    @Override
    public void connect(String clientName) {
        //com.sri.pal.ws.PalService service = new com.sri.pal.ws.PalService();
        //com.sri.pal.ws.Pal port = service.getPalPort();
        //port.start(clientName);
    }

    @Override
    public void disconnect() {
        //com.sri.pal.ws.PalService service = new com.sri.pal.ws.PalService();
        //com.sri.pal.ws.Pal port = service.getPalPort();
        //port.shutdown();
    }

    @Override
    public void runProcedure(ProcedureModel procModel, boolean synchronous, List<Object> args, boolean stepped,
                             Callback<Integer, Void> onPaused) {

    }

    @Override
    public void cancelProcedureRun() {
        
    }

    @Override
    public void stepProcedureRun() {
        
    }

    @Override
    public void continueProcedureRun() {
        
    }

    @Override
    public ActionModelDef getType(TypeName typeName) throws PALException {
        return null;  
    }

    @Override
    public Set<SimpleTypeName> listTypes(TypeStorage.Subset... subset) throws PALException {
        return null;  
    }

    @Override
    public void storeType(SimpleTypeName name, ActionModelDef type) throws PALException {
        
    }

    @Override
    public Map<String, String> getNamespaceMetadata(String ns, String version) throws PALException {
        return null;  
    }

    @Override
    public ProcedureDef palExecutorLoad(String xmlSource) throws PALException {
        return null;  
    }

    @Override
    public void addActionListener(GlobalActionListener listener) {
        
    }

    @Override
    public void shutdown() throws PALRemoteException {
        
    }

    @Override
    public ProcedureModel instantiateProcedure(String name) {
        return null;  
    }

    @Override
    public ProcedureModel instantiateProcedure(TypeName name) {
        return null;  
    }

    @Override
    public ProcedureModel instantiateProcedureFromSource(String procSource) {
        return null;  
    }

    @Override
    public List<EditorIssue> validateProcedure(ProcedureModel procModel) {
        return null;  
    }

    @Override
    public List<VariableModel> getInScopeVariables(CommonModel procModel, ATRTask step) {
        return null;  
    }

    @Override
    public ProcedureDef learn(ActionStreamEvent[] actions) {
        return null;  
    }

    @Override
    public Set<TypeName> findDataflowCompletionActions() throws PALException {
        return null;  
    }

	
}
