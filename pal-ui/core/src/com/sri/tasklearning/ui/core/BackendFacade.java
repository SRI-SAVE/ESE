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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRLocator;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.core.IStructure;
import com.sri.ai.lumen.core.Issue;
import com.sri.ai.lumen.editorsupport.ProcedureInfo;
import com.sri.ai.lumen.syntax.FormatUtil;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.ai.lumen.util.repair.RepairSuggestion;
import com.sri.pal.ActionDef;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionInvocation.StepCommand;
import com.sri.pal.ActionInvocationStatusListener;
import com.sri.pal.ActionModelDef;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.Bridge;
import com.sri.pal.GlobalActionListener;
import com.sri.pal.LumenProcedureDef;
import com.sri.pal.LumenProcedureExecutor;
import com.sri.pal.PALException;
import com.sri.pal.PALRemoteException;
import com.sri.pal.PALStatusMonitor;
import com.sri.pal.ProcedureDef;
import com.sri.pal.ProcedureInvocation;
import com.sri.pal.TypeStorage;
import com.sri.pal.Validator;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.messages.contents.ActionCategory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.Wrapper;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.validation.EditorIssue;

import javafx.application.Platform;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Override
    public ProcedureModel instantiateProcedure(String name) {
        TypeName type = TypeNameFactory.makeName(name, PROC_VERSION, PROC_NAMESPACE);
        return instantiateProcedure(type);
    }
    
    /**
     * Instantiates a procedure model by looking up a type's source and constructing
     * it.
     * 
     * TODO: Replace with real bridge API call (Bridge doesn't yet support using a
     * different ATR factory so we must instead get the procedure source first and then
     * have Lumen create the the models from source
     */
    @Override
    public ProcedureModel instantiateProcedure(TypeName name) {
        procedureLoading.setValue(true); 
        try {
            ProcedureDef procDef = (ProcedureDef) bridge.getActionModel().getType(name);
            
            if (procDef == null) {
                throw new RuntimeException("Type " + name + " does not exist");
            }
            String xmlSource = procDef.getSource();
            
            ProcedureModel pm = this.instantiateProcedureFromSource(LumenProcedureDef
                    .unwrapXml(xmlSource));
            
            
            return pm;
            
        } catch (PALException e) {
            log.error("Unable to instantiate procedure '" + name.getFullName()
                    + "'", e);
            return null;
        } finally {
            procedureLoading.setValue(false);
        }
    }
    
    /**
     * Instantiates a ProcedureModel by parsing the Lumen procedure source 
     * 
     * @param procSource
     *            the Lumen source
     * @return a Procedure model for this Lumen source
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ProcedureModel instantiateProcedureFromSource(String procSource) {
        procedureLoading.setValue(true); 
        ATRSyntax converter = new ATRSyntax(new CoreUIModelFactory());
        try {
            return (ProcedureModel) (converter
                    .toActionDeclaration((IStructure) FormatUtil
                            .parseLumenStatement(procSource)));
        } catch (LumenSyntaxError e) {
            log.error("Failed to instantiate ProcedureMdel", e);
            return null;
        } finally {
            procedureLoading.setValue(false);
        }
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

    /**
     * Validate a procedure and return its issues, if any.
     * 
     * @param procModel
     *            the model to validate
     * @return a sequence of EditorIssues to address to make this procedure
     *         valid
     */
    @Override
    public List<EditorIssue> validateProcedure(ProcedureModel procModel) {
        Validator val = new Validator(bridge);
        ProcedureInfo info;
        try {
            info = val.makeProcedureInfo(procModel);
        } catch (PALException e) {
            log.error("Failed to load procedure dependencies", e);
            return null;
        }
        Map<Issue, List<RepairSuggestion>> issues = info.getSuggestions();
        ArrayList<EditorIssue> editorIssues = new ArrayList<EditorIssue>();
        for (Issue issue : issues.keySet()) {
            EditorIssue ei = EditorIssue.create(issue, issues.get(issue), procModel);
            if (ei != null)
                editorIssues.add(ei); 
        }

        return editorIssues;
    }

    /**
     * Uses Lumen to gets the variables in scope at a given step
     * 
     * @param procModel the procedure
     * @param step the step at which to evaluate scope
     * @return a list of the variables in scope at step
     */
    @Override
    public List<VariableModel> getInScopeVariables(
            final CommonModel procModel,
            final ATRTask step) {
        try {
            Validator val = new Validator(bridge);
            ProcedureInfo info = val.makeProcedureInfo(procModel);
            Collection<String> syms = info.getValidInputs(step, null);
            List<VariableModel> vars = new ArrayList<VariableModel>();
            
            for (String sym : syms) {
                VariableModel v = procModel.getVariableManager().getVariableByName(sym);
                if (v != null) 
                    vars.add(v);                
            }
            return vars;
        } catch (Exception e) {
            log.error("Failed to get list of valid inputs", e);
            return null;
        }
     }
    
    

    /**
     * Run a procedure.
     * 
     * @param procModel the model to run
     */
    @Override
    public void runProcedure(final ProcedureModel procModel, final boolean synchronous, final List<Object> args,
            final boolean stepped, final Callback<Integer, Void> onPaused) {
        if (runningProcedureInvocation.getValue() != null)
            return; 
        
        final ProcedureInvocation procInvoke;
        try {
            final ProcedureDef procDef = ((LumenProcedureExecutor) bridge
                .getPALExecutor()).load(procModel);
            
            if (args != null)
                procInvoke = procDef.invoke(null, args);
            else
                procInvoke = procDef.invoke(null);
        } catch (Exception e) {
            log.error("Exception while creating procedure invocation", e);
            Alert.show("Procedure execution failed",
                       "Failed to initialize procedure for execution",
                       AlertConfig.OK, null);
            return; 
        }
        
        final Object monitor = new Object();
        
        ActionInvocationStatusListener listener = new ActionInvocationStatusListener() {
            @Override
            public void error(final ErrorInfo error) {
                if (error.getErrorId() == ErrorType.CANCEL.ordinal())
                    return; 
                
                if (!(error.getErrorId() == ErrorType.EXEC_MISSING.ordinal()))                 
                    log.error("EXECUTION ERROR: " + error.getDetailedMessage());
                    
                Platform.runLater(new Runnable() {
                    public void run() {
                        if (error.getErrorId() == ErrorType.EXEC_MISSING.ordinal()) {
                            Alert.show(
                                "Application not running",
                                "An application used by this procedure is not "+
                                "running. Please start it and try again.",
                                AlertConfig.OK, null);
                        } else {                            
                            String extra = args == null || args.size() == 0 ? ""
                                    : " Double-check your input values.";
                            Alert.show(
                                "Procedure execution failed",
                                "Sorry, there was an error running the " +
                                "procedure." + extra,
                                AlertConfig.OK, null);
                            }
                    }
                });
            }

            @Override
            public void newStatus(ActionInvocation.Status status) {
                switch (status) {
                case ENDED:
                case FAILED:
                    runningProcedureInvocation.setValue(null);
                    if (debuggingProcedure.getValue()) {
                        // Some UI components are bound to the value of
                        // debuggingProcedure, so we change it on the FX
                        // event thread
                        Platform.runLater(new Runnable() {
                            public void run() {
                                debuggingProcedure.setValue(false);    
                            }
                        });
                    }
                    if (synchronous) {
                        synchronized (monitor) {
                            monitor.notifyAll();
                        }
                    }
                    break; 
                case PAUSED:
                    ATR located = locate(procInvoke.getLocation(), procModel);

                    if (located instanceof StepModel)
                        debugMgr.manageSymbols(procModel, (StepModel)located);
                    
                    if (onPaused != null)
                        onPaused.call(procInvoke.getLocation());
                    break;
                case RUNNING:
                case CREATED:
                    // ignore
                }
            }
        };
                        
        procInvoke.addListener(listener);     
        
        synchronized (monitor) {
            try {              
                if (stepped) {
                    debugMgr.reset();
                    procInvoke.startStepping();
                    debuggingProcedure.setValue(true); 
                } else
                    procInvoke.start();
                
                runningProcedureInvocation.setValue(procInvoke);
                runningProcedure.setValue(procModel);
            } catch (Exception e) {
                log.error("EXECUTION ERROR: ", e);
                Alert.show("Procedure execution failed",
                           "Sorry, there was an error running the procedure.",
                           AlertConfig.OK, null);
                return; 
            }
            
            if (synchronous) {             
                while (runningProcedureInvocation.getValue() != null)
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                    }                
            }
        }
    }    
    
    @Override
    public void cancelProcedureRun() {
        ProcedureInvocation invoc = runningProcedureInvocation.getValue();
        if (invoc != null)
            invoc.cancel();         
    }
    
    @Override
    public void stepProcedureRun() {
        ProcedureInvocation invoc = runningProcedureInvocation.getValue();
        CommonModel      proc  = runningProcedure.getValue();
        if (invoc != null) {

            final ATR located = locate(invoc.getLocation(), proc);
            final List<Object> vals = new ArrayList<Object>();

            boolean success = false;

            if (located instanceof StepModel) {
                StepModel currentStep = (StepModel) located;
                for (ParameterModel pm : currentStep.getInputs()) {
                    try {
                        Object value = debugMgr.getDebugValue(pm.getTerm());
                        vals.add(value);
                    } catch (PalUiException e) {
                        break;
                    }
                }
                if (vals.size() == currentStep.getInputs().size())
                    success = true;
            }
            try {
                if (success)
                    invoc.continueStepping(StepCommand.STEP_OVER, vals);
                else
                    invoc.continueStepping(StepCommand.STEP_OVER);
            } catch (PALException e) {
                log.error("Error occurred while attempting to step during debugging", e);
            }
        }
    }
    
    @Override
    public void continueProcedureRun() {
        ProcedureInvocation invoc = runningProcedureInvocation.getValue();
        if (invoc != null) {
            try {
                invoc.continueStepping(StepCommand.CONTINUE);
            } catch (PALException e) {
                log.error(
                    "Error occurred while attempting to continue procedure " +
                    "execution after debugging", e);
            }
        }
    }

    @Override
    public ProcedureDef learn(ActionStreamEvent[] actions) {
        try {
            return bridge.getLearner().learn(
                    "learned-proc" + System.currentTimeMillis(),
                    findDataflowCompletionActions(),
                    actions);
        } catch (PALException e) {
            log.error("Error occured while attempting to learn procedure", e);
            return null;
        }
    }

    @Override
    public Set<TypeName> findDataflowCompletionActions() throws PALException {
        // Return the set of names of all dataflow completion actions
        // (COMPLETERS + SUPPORTERS) currently present in the Bridge action 
        // model.
        // TODO perhaps replace with a technique that doesn't iterate over all
        // actions every call.
        HashSet<TypeName> result = new HashSet<TypeName>();
        for (SimpleTypeName name : bridge.getActionModel().listTypes(
                TypeStorage.Subset.ACTION)) {
            ActionDef actionDef = (ActionDef) bridge.getActionModel().getType(
                    name);
            if (actionDef.getCategory() == ActionCategory.COMPLETER
                    || actionDef.getCategory() == ActionCategory.SUPPORTER)
                result.add(actionDef.getName());
        }
        return result;
    }
    
    
    public List<EditorIssue> validate(ProcedureModel procModel) { 
    	
    	return validateProcedure(procModel);

    	
    }
    	  

	
}

