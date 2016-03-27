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

// $Id: NamespaceAdder.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.upgrader;

import java.util.HashSet;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRStruct;
import com.sri.ai.lumen.atr.ATRTraverser;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;

class NamespaceAdder
        extends ATRTraverser {
    private final Set<String> reservedFunctors;

    public NamespaceAdder() {
        reservedFunctors = new HashSet<String>();
        reservedFunctors.add("positionalTupleGen");
        reservedFunctors.add("mapGet");
        reservedFunctors.add("setGen");
        reservedFunctors.add("bagGen");
        reservedFunctors.add("first");
        reservedFunctors.add("last");
        reservedFunctors.add("NULL");
    }

    public void run(ATRActionDeclaration proc) {
        traverse(proc);
    }

    @Override
    protected boolean process(ATR atr,
                              int preorderIndex) {
        if (atr instanceof ATRStruct) {
            /*
             * Rename this thing, which could be a procedure declaration or an
             * action call.
             */
            ATRStruct.Mutable<?> mut = (ATRStruct.Mutable<?>) atr;
            String functor = mut.getFunctor();
            if (!functor.contains("_ns_") && !reservedFunctors.contains(functor)) {
                functor = "SPARK_ns_" + functor;
                mut.setFunctor(functor);
            }
        }
        return true;
    }
}
