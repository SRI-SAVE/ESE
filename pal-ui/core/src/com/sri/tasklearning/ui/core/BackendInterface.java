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

import com.sri.pal.ActionModelDef;
import com.sri.pal.GlobalActionListener;
import com.sri.pal.LumenProcedureExecutor;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.TypeStorage;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;


public abstract class BackendInterface {
    public static String PROC_NAMESPACE = LumenProcedureExecutor.getNamespace();
    public static String PROC_VERSION = LumenProcedureExecutor.getVersion();
    static public String PROC_VERSIONED_NAMESPACE = PROC_NAMESPACE + '^' + PROC_VERSION + '^';
    protected final ReadOnlyBooleanWrapper debuggingProcedure = new ReadOnlyBooleanWrapper(false);
    protected final ReadOnlyObjectWrapper<ProcedureInvocation> runningProcedureInvocation = new ReadOnlyObjectWrapper<ProcedureInvocation>(null);
    protected final ReadOnlyBooleanWrapper procedureLoading = new ReadOnlyBooleanWrapper(false);
  
    abstract public void connect(String clientName);

    abstract public void disconnect();


    // Action Model Methods
    abstract public ActionModelDef getType(TypeName typeName) throws PALException;
    abstract public Set<SimpleTypeName> listTypes(TypeStorage.Subset... subset) throws PALException;
    abstract public void storeType(SimpleTypeName name, ActionModelDef type) throws PALException;
    abstract public Map<String, String> getNamespaceMetadata(String ns, String version) throws PALException;

    // Bridge Methods
    abstract public ProcedureDef palExecutorLoad(String xmlSource) throws PALException;
    abstract public void addActionListener(GlobalActionListener listener);
    abstract public void shutdown() throws PALRemoteException;
     


}
