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

import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.pal.*;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.common.InputVariableModel;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;
import com.sri.tasklearning.ui.core.step.LoopModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.term.*;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;
import com.sri.tasklearning.ui.core.validation.EditorIssue;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.util.Callback;

import java.util.*;


public abstract class BackendInterface {
    public static String PROC_NAMESPACE = LumenProcedureExecutor.getNamespace();
    public static String PROC_VERSION = LumenProcedureExecutor.getVersion();
    static public String PROC_VERSIONED_NAMESPACE = PROC_NAMESPACE + '^' + PROC_VERSION + '^';
    protected final ReadOnlyBooleanWrapper debuggingProcedure = new ReadOnlyBooleanWrapper(false);
    protected final ReadOnlyObjectWrapper<ProcedureInvocation> runningProcedureInvocation = new ReadOnlyObjectWrapper<ProcedureInvocation>(null);
    protected final ReadOnlyObjectWrapper<ProcedureModel> runningProcedure = new ReadOnlyObjectWrapper<ProcedureModel>(null);
    protected final ReadOnlyBooleanWrapper procedureLoading = new ReadOnlyBooleanWrapper(false);
    protected final DebugSymbolManager debugMgr = new DebugSymbolManager();

    abstract public void connect(String clientName);

    abstract public void disconnect();

    public ReadOnlyObjectProperty<ProcedureInvocation> runningProcedureProperty() {
        return runningProcedureInvocation.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty procedureLoadingProperty() {
        return procedureLoading.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty debuggingProcedureProperty() {
        return debuggingProcedure.getReadOnlyProperty();
    }

    public boolean isDebuggingProcedure() {
        return debuggingProcedure.getValue();
    }

    abstract public void cancelProcedureRun();

    abstract public void stepProcedureRun();

    abstract public void continueProcedureRun();

    // Action Model Methods
    abstract public ActionModelDef getType(TypeName typeName) throws PALException;
    abstract public Set<SimpleTypeName> listTypes(TypeStorage.Subset... subset) throws PALException;
    abstract public void storeType(SimpleTypeName name, ActionModelDef type) throws PALException;
    abstract public Map<String, String> getNamespaceMetadata(String ns, String version) throws PALException;

    // Bridge Methods
    abstract public ProcedureDef palExecutorLoad(String xmlSource) throws PALException;
    abstract public void addActionListener(GlobalActionListener listener);
    abstract public void shutdown() throws PALRemoteException;

    public Object getDebugValue(final TermModel term) throws PalUiException {
        return debugMgr.getDebugValue(term);
    }

    public Object lookupDebugValue(final TermModel term) {
        return debugMgr.lookupDebugValue(term);
    }

    abstract public ProcedureModel instantiateProcedure(String name);

    abstract public ProcedureModel instantiateProcedure(TypeName name);

    abstract public ProcedureModel instantiateProcedureFromSource(String procSource);

    abstract public List<EditorIssue> validateProcedure(ProcedureModel procModel);

    abstract public List<VariableModel> getInScopeVariables(
            CommonModel procModel,
            ATRTask step);

    abstract public void runProcedure(
    		ProcedureModel procModel,
            boolean synchronous,
            List<Object> args,
            boolean stepped,
            Callback<Integer, Void> onPaused);

    abstract public ProcedureDef learn(ActionStreamEvent[] actions);

    abstract public Set<TypeName> findDataflowCompletionActions() throws PALException;

    protected class DebugSymbolManager {
        private final HashMap<TermModel, Object> map = new HashMap<TermModel, Object>();
        private final List<TermModel> results = new ArrayList<TermModel>();
        private final List<TermModel> toBeUnbound = new ArrayList<TermModel>();
        private LoopModel lastLoop;

        private boolean processedProcInputs = false;

        protected void reset() {
            map.clear();
            results.clear();
            processedProcInputs = false;
        }

        private void addTermsRecursive(
                final CompositeTermModel fm,
                final List<TermModel> terms,
                final List<TypeDef> types) {
            for (ParameterModel pm : fm.getInputs()) {
                if (!map.containsKey(pm.getTerm())) {
                    terms.add(pm.getTerm());
                    types.add(pm.getTypeDef());
                }
                if (pm.getTerm() instanceof ListModel || FunctionModel.isAccessorFunction(pm.getTerm()))
                    addTermsRecursive((CompositeTermModel)pm.getTerm(), terms, types);
            }
        }

        private LoopModel isSpecialStepInLoop(
                final ContainerStepModel container,
                final StepModel step) {
            for (StepModel iter : container.getSteps())
                if (iter instanceof LoopModel) {
                    LoopModel loop = (LoopModel)iter;

                    if (loop.getSteps().get(0).equals(step) ||
                        loop.getSteps().get(loop.getSteps().size() - 1).equals(step))
                        return loop;
                    else {
                        LoopModel found = isSpecialStepInLoop(loop, step);

                        if (found != null)
                            return found;
                    }
                }
            return null;
        }

        public void manageSymbols(
                final CommonModel proc,
                final StepModel step) {

            // First, unbind any values that we determined needed to be unbound
            // on this step
            synchronized (map) {
                for (TermModel term : toBeUnbound) {
                    map.remove(term);
                }
            }
            toBeUnbound.clear();

            // Now, go about augmenting the map with any new inputs or
            // results bound by the previous step
            final List<TermModel> terms = new ArrayList<TermModel>();
            final List<TypeDef> types = new ArrayList<TypeDef>();

            // First, make sure we've evaluated the procedure inputs
            if (!processedProcInputs) {
                for (InputVariableModel in : proc.getSignature().getInputs()) {
                    terms.add(in);
                    types.add(in.getTypeDef());
                }
                processedProcInputs = true;
            }

            // Now evaluate the inputs to the current step
            for (ParameterModel pm : step.getInputs()) {
                if (!map.containsKey(pm.getTerm())) {
                    terms.add(pm.getTerm());
                    types.add(pm.getTypeDef());
                }

                if (FunctionModel.isAccessorFunction(pm.getTerm()))
                    addTermsRecursive((FunctionModel)pm.getTerm(), terms, types);
            }

            LoopModel found = isSpecialStepInLoop(proc, step);
            boolean skipResultEvaluation = false;

            // Special processing for loops

            if (lastLoop != null && (found == null || found != lastLoop)) {
                // Now we can ask for the accumulator...
                if (lastLoop.getIntoTerm() != null) {
                    terms.add(lastLoop.getIntoTerm());
                    types.add(lastLoop.getIntoTerm().getTypeDef());
                }
                lastLoop = null;
            }
            if (found != null) {
                if (found.getSteps().get(0).equals(step)) {
                    // first step in loop, lookup loop term
                    if (found.getLoopTerm() instanceof CompositeTermModel)
                        addTermsRecursive((CompositeTermModel)found.getLoopTerm(), terms, types);
                    else {
                        terms.add(found.getLoopTerm());
                        types.add(found.getLoopTerm().getTypeDef());
                    }
                }

                if (found.getSteps().get(found.getSteps().size() - 1).equals(step)) {
                    // Last step in loop. Unbind values for results of all
                    // steps in the loop...after the next step of course
                    for (StepModel loopStep : found.getSteps()) {
                        if (loopStep instanceof LoopModel) {
                            LoopModel subLoop = (LoopModel)loopStep;
                            toBeUnbound.add(subLoop.getLoopTerm());
                            if (subLoop.getIntoTerm() != null)
                                toBeUnbound.add(subLoop.getIntoTerm());
                        } else
                            for (ParameterModel pm : loopStep.getResults())
                                toBeUnbound.add(pm.getTerm());
                    }

                    // Skip evaluating results of this step because there's
                    // no point and it will cause a problem when we get
                    // paused again
                    skipResultEvaluation = true;

                    lastLoop = found;
                }
            }

            // Add any results from the last step to the list to be evaluated
            if (!skipResultEvaluation && results.size() > 0) {
                for (TermModel term : results) {
                    terms.add(term);
                    types.add(term.getTypeDef());
                }
            }
            results.clear();

            // Add results of this step to be evaluated next time we pause
            if (!skipResultEvaluation)
                for (ParameterModel pm : step.getResults())
                    results.add(pm.getTerm());

            // Update the map
            updateMap(terms, types);
        }

        public void updateMap(
                final List<TermModel> terms,
                final List<TypeDef> types) {

            final Task<Void> asyncTask = new Task<Void>() {
                public Void call() {
                    final List<Object> evals =
                        runningProcedureInvocation.getValue().evaluateTerms(terms, types);

                    synchronized (map) {
                        int idx = 0;
                        for (TermModel term : terms) {
                            map.put(term, evals.get(idx++));
                        }
                    }

                    return null;
                }
            };

            new Thread(asyncTask).start();
        }

        public Object getDebugValue(final TermModel term) throws PalUiException {
            if (map.containsKey(term))
                return map.get(term);

            throw new PalUiException("Failed to find debug value for " + term);
        }

        public Object lookupDebugValue(final TermModel term) {
            if (map.containsKey(term))
                return map.get(term);

            final List<TermModel> terms = new ArrayList<TermModel>();
            final List<TypeDef> types = new ArrayList<TypeDef>();

            terms.add(term);
            types.add(term.getTypeDef());

            updateMap(terms, types);

            return map.get(term);
        }
    }


}
