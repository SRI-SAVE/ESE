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

package com.sri.tasklearning.ui.core.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionModelDef;
import com.sri.pal.PALException;
import com.sri.pal.TypeStorage.Subset;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.step.ActionStepModel;
import com.sri.tasklearning.ui.core.step.LoopModel;
import com.sri.tasklearning.ui.core.step.ProcedureStepModel;
import com.sri.tasklearning.ui.core.step.StepModel;

/**
 * Maintains data structures about the actions, procedures and "tools"
 * available in the system. Also acts as a manager for Namespace instances since
 * many of the data structures are organized by namespace.  
 */

public class ActionModelAssistant {
    private static final Logger log = LoggerFactory
        .getLogger(ActionModelAssistant.class);
    
    private static ActionModelAssistant instance = new ActionModelAssistant();
    


    // map from namespace to list of related actions
    private final Map<Namespace, Set<StepModel>> actionGroups = new TreeMap<Namespace, Set<StepModel>>();
    // map of namespace name/version combos to Namespace instances
    private final Map<String, Namespace> namespaces = new TreeMap<String, Namespace>();

    private final Set<StepModel> recentlyUsedActions = new TreeSet<StepModel>();
    private final TreeSet<StepModel> toolActions = new TreeSet<StepModel>();
    private final Set<StepModel> allActions = new TreeSet<StepModel>();
    private final List<String> failures = new ArrayList<String>();

    private ActionModelAssistant() {
        instance = this; 
        initialize();
    }
    
    public static ActionModelAssistant getInstance() {
        return instance;
    }
    
    private void initialize() {
        // List all known actions and procedures
        Set<SimpleTypeName> typeNames = null;
        try {
            typeNames = BackendFacade.getInstance().listTypes(Subset.ACTION, Subset.PROCEDURE);
        } catch (PALException e) {
            log.error("Failed to initialize ActionModelAssistant", e);
            return;
        }

        if (typeNames == null)
            return;

        // Add procedure namespace to groups...
        namespaces.put(Namespace.BUILTIN.getFullName(), Namespace.BUILTIN);
        actionGroups.put(Namespace.BUILTIN, new TreeSet<StepModel>());
        
        toolActions.add(new LoopModel(null, null, null, null, null));

        for (SimpleTypeName name : typeNames) {
            ActionStepModel newStep;

            if (name.getNamespace().equals(Namespace.BUILTIN.getName()))
                newStep = new ProcedureStepModel(name.getFullName(), null);
            else {
                try {
                    ActionModelDef type = BackendFacade
                            .getInstance()
                            .getBridge()
                            .getActionModel()
                            .getType(
                                    TypeNameFactory.makeName(name.getFullName()));
                    if (!(type instanceof ActionDef))
                        continue;
                    newStep = new ActionStepModel(name.getFullName(), null);
                } catch (Exception e) {
                    continue;
                }
            }

            // This happens if a procedure references a non-existent/renamed
            // procedure as a step
            if (newStep.getActionDefinition() == null) {
                failures.add(name.getSimpleName());
                continue;
            }

            addAction(newStep);
        }
        // TODO temp code for DH #3
        //ProcedureMap.getInstance().repopulate();
    }
    
    public void reset() {
        actionGroups.clear();
        namespaces.clear();
        toolActions.clear();

        initialize();
    }
    
    public Namespace getNamespace(
            final String name,
            final String version) {
        
        Namespace nspace = null;
        if ((nspace = namespaces.get(Namespace.fullNamespaceName(name, version))) == null) {
            nspace = new Namespace(name, version);
            namespaces.put(nspace.getFullName(), nspace);
            actionGroups.put(nspace, new TreeSet<StepModel>());
        } 

        return nspace;
    }
    
    public Collection<Namespace> getNamespaces() {
        return namespaces.values();
    }
    
    public Set<StepModel> getActions() {
        return allActions;
    }
        
    public Set<StepModel> getActions(Namespace namespace) {
        if (namespace == null)
            return allActions; 
        else if (actionGroups.containsKey(namespace))
            return actionGroups.get(namespace);        
        else {
            log.error("Unknown namespace requested from ActionModelAssistant: {}", namespace);
            return null;
        }
    }
        
    public TreeSet<StepModel> getToolActions() {
        return toolActions;
    }
    
    public Set<StepModel> getRecentActions() {
        return recentlyUsedActions;
    }
    
    public void addRecentAction(StepModel action) {
        if (recentlyUsedActions.contains(action) == false) {
            recentlyUsedActions.add(action);
        }
    }
    
    public List<String> getFailures() {
        return failures;
    }           
    
    // adds a single action into our local knowledge space
    public boolean addAction(StepModel action) {  
        // if the namespace is already known, get its list of actions;
        // otherwise add the new namespace and start a list of actions for it
        Namespace key = namespaces.get(action.getNamespace().getFullName());
        Set<StepModel> actionList = getActions(key);
        // finally, add the new action to the namespace's list and our list of all actions
        actionList.add(action);
        
        if (!key.equals(Namespace.BUILTIN))
            allActions.add(action);            
        
        return true;
    }
    
    public boolean deleteProcedure(String name) {
        Set<StepModel> procList = getActions(Namespace.BUILTIN);
        boolean ret = false; 
        
        for (StepModel step : procList)
            if (step.getName().equals(name)) {
                procList.remove(step);
                ret = true;
                break;
            }
        
        for (StepModel step : recentlyUsedActions)
            if (step.getName().equals(name)) {
                recentlyUsedActions.remove(step);
                break;
            }
        
        return ret;         
    }
    
    public boolean isProcedureNameInUse(String name) {
        Set<StepModel> actionList = getActions(Namespace.BUILTIN);
        
        for (StepModel step : actionList)
            if (step.getName().equals(name.trim()))
                return true;

        return false;
    }
}
