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
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.pal.CollectionTypeDef;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.ActionStepModel;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;
import com.sri.tasklearning.ui.core.step.LoopModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableModel;

/**
 * Tracks all of the variables known to the currently loaded
 * procedure. Provides methods for finding variable references and origins
 * as well as methods for renaming variables and generating unique variable
 * names. This class in conjunction with {@code VariableModel.create} ensure
 * that a variable is represented by a single object in memory, thus allowing
 * for memory-based comparisons although this is discouraged. 
 */

public final class VariableManager {
    private static VariableManager instance = new VariableManager();

    private static final Logger log = LoggerFactory
            .getLogger(VariableManager.class);

    private final ArrayList<IVariableWatcher> watchers;
    
    /* The keys of varMap should be the names of the variables in all lower-case,
     * which does not preclude the existence of variables that contain capital
     * letters but does prevent the user from creating two separate variable
     * names that only differ in their capitalization. 
     */
    private final HashMap<String, VariableModel> varMap;
    
    private CommonModel owner;
    
    private VariableManager() {
        watchers = new ArrayList<IVariableWatcher>();
        varMap   = new HashMap<String, VariableModel>();
    }
    
    public static VariableManager takeNextVariableManager(CommonModel owner) {
        VariableManager ret = instance;
        ret.owner = owner; 
        instance = new VariableManager();
        return ret; 
    }
    
    public static VariableManager getNextVariableManager() {
        return instance;
    }
    
    public List<VariableModel> getVariables() {
        return new ArrayList<VariableModel>(varMap.values());
    }   

    public void registerVarWatcher(IVariableWatcher watcher) {
        watchers.add(watcher);
    }

    public void unregisterVarWatcher(IVariableWatcher watcher) {
        watchers.remove(watcher);
    }

    public void reset() {
        varMap.clear();
    }

    /**
     * Add a variable to the manager if a variable with its name doesn't already
     * exist. Return the managed instance of the variable with the given name.
     * 
     * @param var the variable to manage
     * @return var if there was no managed variable with its name or the managed
     *         instance with var's name if one already existed
     * @throws RuntimeTimeException
     *             if the variable to manage has a different type than currently
     *             managed variable's type
     */
    public TermModel manageVariable(final VariableModel var) {
        if (!varMap.containsKey(var.getVariableName().toLowerCase())) {
            varMap.put(var.getVariableName().toLowerCase(), var);
            for (IVariableWatcher watcher : watchers) {
                watcher.onAddedVariable(var);
            }
        }
        return getVariable(var);
    }
    
    public boolean isManaged(final VariableModel var) {
        if (varMap.containsKey(var.getVariableName().toLowerCase())) 
            return true;
        
        return false; 
    }

    /**
     * Remove a variable from management.
     * 
     * @param var the variable to remove from management.
     */
    public void unmanageVariable(final VariableModel var) {
        if (varMap.containsKey(var.getVariableName().toLowerCase())) {
            varMap.remove(var.getVariableName().toLowerCase());
            for (IVariableWatcher watcher : watchers) {
                watcher.onRemovedVariable(var);
            }
        }
    }

    /**
     * Rename the given variable. Provide both the old (current) name and the
     * new (desired) name for sake of clarity. The new name is only managed if
     * the old name is currently managed.
     */
    public Boolean renameVariable(
            final VariableModel variable, 
            final String oldName,
            final String newName) {

        final String oldNameLower = oldName.toLowerCase();
        final String newNameLower = newName.toLowerCase();
        
        if (!newName.equals(oldName) || !varMap.containsKey(newNameLower)) {
            boolean manageNewName = false;
            if (varMap.containsKey(oldNameLower)) {
                VariableModel oldVar = varMap.remove(oldNameLower);
                if (!oldVar.equals(variable)) {
                    throw new RuntimeException(
                            "Multiple variables detected with name '" + oldName + "'");
                }
                manageNewName = true;
            }
            variable.setVariableName(newName);
            if (manageNewName)
                varMap.put(newNameLower, variable);
            
            for (IVariableWatcher watcher : watchers)
                watcher.onRenamedVariable(variable);
            
            return true;
        }

        return false;
    }

    /**
     * Rename the given variable. Provide both the old (current) name and the
     * new (desired) name for sake of clarity.
     */
    public Boolean renameVariable(final String oldName, final String newName) {
        final VariableModel variable = getVariableByName(oldName);
        if (variable == null)
            return false;
        else
            return renameVariable(variable, oldName, newName);
    }

    /**
     * Get the managed variable with the given name.
     * 
     * @param name
     *            the name to look up
     * @return the managed instance of the variable if it exists, null otherwise
     */
    public VariableModel getVariableByName(String name) {
        String normName = name.toLowerCase();
        if (normName.startsWith("$")) {
            normName = normName.substring(1);
        }
        return varMap.get(normName);
    }

    /**
     * Get the managed instance of a variable with this variable's name.
     * 
     * @param v
     *            the variable to look up
     * @return the managed instance of the variable if it exists, null otherwise
     * @throws RuntimeTimeException
     *             if the variable to look up has a different type than the
     *             managed variable's type
     */
    public VariableModel getVariable(VariableModel v) {
        VariableModel toRet = varMap.get(v.getVariableName().toLowerCase());
        if (!toRet.getClass().equals(v.getClass())) {
            throw new RuntimeException("Unexpected class for " + v + ": " + v.getClass());
        }
        return toRet;
    }

    /**
     * Count references and remove unused variables.
     */
    public void collectUnusedVariables(ContainerStepModel container) {
        for (VariableModel v : getVariables()) {
            boolean foundRef = isVariableReferenced(container, v);
            if (v instanceof ProcedureInputVariableModel) {
                v.setReferenced(foundRef);
            } else
                if (!foundRef)
                    unmanageVariable(v);            
        }
    }

    public List<StepModel> findReferencesToVariable(
            final ContainerStepModel container, 
            final VariableModel v) {
        List<StepModel> references = new ArrayList<StepModel>();
        for (StepModel s : container.getSteps()) {
            if (s.referencesVariable(v))
                references.add(s);
            
            if (s instanceof ContainerStepModel) 
                references.addAll(findReferencesToVariable(
                        (ContainerStepModel) s, v));            
        }
        
        return references;
    }
    
    public boolean isVariableReferenced(
            final ContainerStepModel container, 
            final VariableModel v) {
        
        return findReferencesToVariable(container, v).size() > 0;
    }

    /**
     * Get the variables that would be valid for the given input in the given
     * scope.
     * 
     */
    public List<VariableModel> getValidInputs(ATRTask step, ParameterModel arg) {
        return getValidInputs(step, arg, new ArrayList<VariableModel>());
    }

    /**
     * Get the variables that would be valid for the given input in the given
     * scope.
     * 
     * @param exceptions
     *            A list of variables that explicitly should NOT be returned in
     *            the valid list.
     */
    public List<VariableModel> getValidInputs(ATRTask step, ParameterModel arg,
            List<VariableModel> exceptions) {

        if (step instanceof LoopModel) {
            List<VariableModel> list = new ArrayList<VariableModel>();
            for (VariableModel var : getVariables()) {
                if (!(var.getTypeDef() instanceof CollectionTypeDef))
                    continue; 
                
                if (var instanceof ProcedureInputVariableModel) {
                    list.add(var);
                    continue;
                } 
                
                int origin = findVariableOriginStepIndex(var, owner);
                if (origin < ((LoopModel)step).getIndex() && origin >= 0)
                    list.add(var);               
            }
            return list;            
        } else {
            List<VariableModel> list = BackendFacade.getInstance()
                    .getInScopeVariables(owner, step);

            return filterValidInputs(list, arg, exceptions);
        } 
    }

    private int findVariableOriginStepIndex(VariableModel var, ContainerStepModel container) {
        for (StepModel step : container.getSteps()) {
            if (step instanceof ContainerStepModel) {
                if (step instanceof LoopModel) {
                    LoopModel lm = (LoopModel)step;
                    if (lm.getIterand().getTerm().equals(var))
                        return lm.getIndex();
                    else if (lm.getIntoTerm() != null && lm.getIntoTerm().equals(var)) {
                        // Accumulator var is effectively available after the last step in a loop. 
                        StepModel lastStep = lm;
                        do {
                            LoopModel lastLoop = (LoopModel)lastStep;
                            if (lastLoop.getSteps().size() == 0)
                                break;
                            
                            lastStep = lastLoop.getSteps().get(lastLoop.getSteps().size() - 1);
                        } while (lastStep instanceof LoopModel);
                            
                        return lastStep.getIndex(); 
                    } else {
                        int origin = findVariableOriginStepIndex(var, lm);
                        if (origin != -1)
                            return origin;
                    }
                } else {
                    log.error("Unsupported ContainerStepModel descendent: {}", step.getClass());
                }
            } else {
                ActionStepModel asm = (ActionStepModel)step;
                if (asm.getResults(). size() > 0) {
                    for (ParameterModel res : asm.getResults()) {
                        if (res.getTerm().equals(var))
                            return asm.getIndex();
                    }
                }
            }
        }
        return -1;
    }    

    /**
     * Get the variables that would be valid for the given input in the given
     * scope.
     * 
     * @param exceptions
     *            A list of variables that explicitly should NOT be returned in
     *            the valid list.
     */
    public List<VariableModel> filterValidInputs(
            final List<VariableModel> potentialInputs,
            final ParameterModel arg, 
            final List<VariableModel> exceptions) {

        ArrayList<VariableModel> validVars = new ArrayList<VariableModel>();

        if (potentialInputs != null) {
            for (VariableModel model : potentialInputs) {
                // make sure each variable:
                // b) is the same type as the given argument
                // c) isn't one of the values in the "exceptions" list                
                if (arg != null
                        && (exceptions == null || exceptions.indexOf(model) < 0)
                        && TypeUtilities.isAssignable(model.getTypeDef(),
                                arg.getTypeDef())) {
                    validVars.add(model);
                }
            }
        }

        return validVars;
    }

    /*
     * Construct a new variable name by appending a number to the baseName.
     * Function will return name with lowest non-duplicate number.
     */
    public String createValidName(String baseName) {
        int i = 1;
        String base = baseName;
        String sep = System.getProperty("lapdog.naming.variable.separator");

        if (sep == null)
            sep = "_";
        
        String potentialName = base + sep + i;

        while (varMap.containsKey(potentialName.toLowerCase()))
            potentialName = base + sep + ++i;            
        
        return potentialName;
    }

    /*
     * Check the given name to make sure it's valid. Valid names are non-empty,
     * start with a letter, and are not already in use.
     */
    public boolean isValidName(final String varName) {
        if (varName.length() > 0 && Character.isLetter(varName.charAt(0)))     
            return true;
        
        return false;
    }
    
    public boolean isNewName(final String varName) {
        String v = varName.toLowerCase();
        if (!varMap.containsKey(v))    
            return true;
        return false;
    }
}