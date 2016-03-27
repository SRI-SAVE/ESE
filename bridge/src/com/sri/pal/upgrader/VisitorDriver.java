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

// $Id: VisitorDriver.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.upgrader;

import java.util.ArrayList;
import java.util.List;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.ATRTraverser;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.task.ATRAction;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.syntax.LumenSyntaxError;

/**
 * Driver class which calls {@link ActionVisitor} as part of
 * {@link ProcedureUpgrader#visitActions}.
 */
public class VisitorDriver
        extends ATRTraverser {
    private final ActionVisitor visitor;
    @SuppressWarnings("rawtypes")
    private final ATRSyntax atrSyntax;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public VisitorDriver(final ActionVisitor visitor) {
        this.visitor = visitor;

        atrSyntax = new ATRSyntax(new CTRConstructor());
    }

    public void run(ATRActionDeclaration proc) {
        traverse(proc);
    }

    @Override
    protected boolean process(ATR atr,
                              int preorderIndex) {
        if (atr instanceof ATRAction) {
            @SuppressWarnings("unchecked")
            ATRAction.Mutable<ATRTerm, ATRTask> atrAction = (ATRAction.Mutable<ATRTerm, ATRTask>) atr;
            // Build an ActionCall out of this.
            String functor = atrAction.getFunctor();
            ActionCall call = new ActionCall(functor);
            List<String> strArgs = new ArrayList<String>();
            List<ATRTerm> atrArgs = new ArrayList<ATRTerm>();
            for (ATRTerm atrArg : atrAction.getElements()) {
                String strArg = ATRSyntax.toSource(atrArg);
                strArgs.add(strArg);
                atrArgs.add(atrArg);
            }
            call.setArgs(strArgs);
            call.setAtrArgs(atrArgs);

            // Call the visitor and get its answer.
            ActionCall newCall = visitor.visit(call);

            // Replace the existing call with whatever the visitor returned.
            atrAction.setFunctor(newCall.getName());
            atrArgs = atrAction.getElements();
            atrArgs.clear();
            for (String strArg : newCall.getArgs()) {
                try {
                    ATRTerm atrArg = (ATRTerm) atrSyntax.termFromSource(strArg);
                    atrArgs.add(atrArg);
                } catch (LumenSyntaxError e) {
                    throw new RuntimeException("Unable to parse argument "
                            + strArg + " to functor " + newCall.getName(), e);
                }
            }
        }
        return true;
    }
}
