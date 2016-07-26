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

package com.sri.tasklearning.lumenpal;


import com.sri.ai.lumen.agent.SimpleAgent;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.core.Solution;
import com.sri.ai.lumen.core.impl.SymUtil;
import com.sri.ai.lumen.performer.Performer;
import com.sri.ai.lumen.performer.SourcePerformerGenerator;
import com.sri.ai.lumen.runtime.LumenConnection;
import com.sri.ai.lumen.syntaxops.Syntax;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.mediators.MediatorsException;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs exactly once, possibly loading the Lumen preload file after first loading prerequisite actions.
 */
public class RunOnce {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String PRELOAD_FILE = "PAL.lumen-preload-file";
    private static final String PRELOAD_PREREQS = "PAL.lumen-preload-prerequisites";

    private final ProcedureDependencyFinder procDepFinder;
    private final LockingActionModel actionModel;
    private final LumenFacade lumen;

    private boolean done = false;
    private boolean running = false;

    RunOnce(LumenFacade lumen,
            LockingActionModel actionModel,
            ProcedureDependencyFinder procDepFinder) {
        this.lumen = lumen;
        this.actionModel = actionModel;
        this.procDepFinder = procDepFinder;
    }

    synchronized void runOnce()
            throws MediatorsException {
        log.debug("Starting runOnce");
        if (done) {
            return;
        }

        if (running) {
            while (!done) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    log.warn("Interrupted", e);
                }
            }
            return;
        }

        running = true;

        run();

        done = true;
        notifyAll();
    }

    public void run()
            throws MediatorsException {
        log.debug("Running preload");
        Set<String> prereqs = new HashSet<>();
        String preloadFileName = System.getProperty(PRELOAD_FILE);
        if (preloadFileName != null) {
            // Load all of the action model elements which are required by the preload file.
            String prereqStrs = System.getProperty(PRELOAD_PREREQS);
            if (prereqStrs != null) {
                prereqs.addAll(Arrays.asList(prereqStrs.split(",")));
                for (String prereqStr : prereqs) {
                    TypeName prereqName = TypeNameFactory.makeName(prereqStr);
                    List<ATRDecl> types = null;
                    try {
                        types = procDepFinder.getRequiredTypes(prereqName, true);
                    } catch (SpineException e) {
                        throw new MediatorsException(e);
                    }
                    for (ATRDecl type : types) {
                        SimpleTypeName typeName = TypeUtil.getName(type);
                        actionModel.getReadLock(typeName);
                        actionModel.add(type);
                    }
                }
            }

            // Actually load the preload file.
            File preloadFile = new File(preloadFileName);
            String preloadFilePath = preloadFile.toURI().toString();
            log.debug("Loading {}", preloadFilePath);
            lumen.executeAction("load", new Object[]{preloadFilePath});
        }

        // Find all of the defined symbols, now that the preload has potentially been loaded. Add all of those to
        // the action model, so we don't try to load them if a procedure calls one of them.
        SimpleAgent lumenAgent = lumen.getAgent();
        Syntax syntax = LumenConnection.getSyntax();
        SourcePerformerGenerator pg = new SourcePerformerGenerator(lumenAgent, syntax);
        Performer<List<Solution>> p = pg.multipleSolver("m_AgentOp($sym,$)");
        List<Solution> solutions = p.perform();
        for (Solution s : solutions) {
            String actionNameStr = s.keyGet(SymUtil.sym("$sym")).toString();
            if (prereqs.contains(actionNameStr)) {
                continue;
            }
            TypeName actionName = TypeNameFactory.makeName(actionNameStr);
            if (actionName instanceof SimpleTypeName) {
                SimpleTypeName simpleName = (SimpleTypeName) actionName;
                log.debug("Adding pre-existing action {} to the action model", actionNameStr);
                actionModel.getReadLock(simpleName);
                actionModel.addPredefined(simpleName);
            }
        }
    }
}
