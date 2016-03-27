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

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import com.sri.pal.ActionModelDef;
import com.sri.pal.LumenProcedureDef;
import com.sri.pal.PALException;
import com.sri.pal.TypeStorage.Subset;
import com.sri.pal.common.SimpleTypeName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pprovides a map of dependencies between procedures. The map
 * is used by the UI to prevent the user from creating recursive relationships
 * between procedures, and also to detect when the user has made a 
 * "breaking change" to a procedure that is referenced as a step in another 
 * procedure. 
 * 
 * This class is thread-safe in case we decide we want these calculations
 * to be off of the event thread.   
 */

public final class ProcedureMap {
    private static final Logger log = LoggerFactory
        .getLogger(ProcedureMap.class);
    
    private final HashMap<String, Set<String>> map =
        new HashMap<String, Set<String>>();

    private boolean populated = false;

    private static final ProcedureMap instance = new ProcedureMap();

    private ProcedureMap() {}

    /**
     * Retreive the singleton instance
     * @return the singleton
     */
    public static ProcedureMap getInstance() {
        return instance; 
    }

    /**
     * Repopulates the dependency map using the latest type information from
     * the bridge. 
     */
    public void repopulate() {
        synchronized (map) {
            map.clear();

            Set<SimpleTypeName> procNames;

            try {
                procNames = BackendFacade.getInstance().listTypes(Subset.PROCEDURE);
            } catch (PALException e) {
                log.error("Failed to list types for ProcedureMap population");
                return;
            }
            
            for (SimpleTypeName procName : procNames) {
                String key = procName.getSimpleName();

                if (!map.containsKey(key))
                    initMapEntry(key);

                LumenProcedureDef proc;
                Set<ActionModelDef> reqs; 

                try {
                    proc = (LumenProcedureDef)BackendFacade.getInstance().getType(procName);
                    if (proc == null)
                        continue;
                    
                    reqs = proc.getRequiredDefs();                    
                } catch (PALException e) {
                    log.error("Error loading type for ProcedureMap population", e);
                    continue; 
                }

                for (ActionModelDef req : reqs) {
                    if (req instanceof LumenProcedureDef) {
                        LumenProcedureDef procReq = (LumenProcedureDef)req;

                        String reqKey = procReq.getName().getSimpleName();

                        if (!map.containsKey(reqKey))
                            initMapEntry(reqKey);

                        map.get(reqKey).add(key);
                    }
                }
            }
            populated = true; 
        }
    }

    private void initMapEntry(String key) {
        Set<String> set = new TreeSet<String>();

        map.put(key, set);
    }

    /**
     * Returns the set of procedure names (as strings, without namespace or
     * version info) corresponding to all of the procedures that call proc 
     * either directly or indirectly.
     * 
     * @param proc - the name of the procedure you want to the callers of 
     * @return the set of procedure names that call proc directly or indirectly
     */
    public Set<String> getCallers(String proc) {
        synchronized (map) {
            if (!populated)
                repopulate();

            return map.get(proc);
        }
    }    
    
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (String key : map.keySet())
            buff.append(key + " => " + map.get(key).toString() + "\n");
        return buff.toString();
    }
}
