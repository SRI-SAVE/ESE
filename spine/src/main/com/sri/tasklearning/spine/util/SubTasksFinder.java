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

// $Id: SubTasksFinder.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.util;

import java.util.HashSet;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.ATRTraverser;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.task.ATRAction;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;

/**
 * Given an {@code ATRActionDeclaration} representing a procedure, return the
 * names of all the actions (and procedures) it calls.
 */
public class SubTasksFinder
        extends ATRTraverser {
    Set<SimpleTypeName> subtasks = new HashSet<SimpleTypeName>();

    public static Set<SimpleTypeName> findSubTasks(ATRActionDeclaration actionDeclaration) {
        SubTasksFinder f = new SubTasksFinder();
        f.traverse(actionDeclaration);
        return f.subtasks;
    }

    @Override
    protected boolean process(ATR atr,
                              int preorderIndex) {
        if (atr.getCategory() == ATRCat.ACTION) {
            boolean recurse = false;
            ATRAction action = (ATRAction) atr;
            ATRTask body = action.getBody();
            if (body != null) {
                /*
                 * It's a gesture. If it has a name, it's also an idiom, and we
                 * should include that idiom. Regardless, descend into it and
                 * identify its constituent actions.
                 */
                recurse = true;
            }
            String functor = action.getFunctor();
            if (functor != null && functor.length() > 0) {
                SimpleTypeName typeName = (SimpleTypeName) TypeNameFactory
                        .makeName(functor);
                subtasks.add(typeName);
            }
            return recurse;
        } else {
            return true;
        }
    }
}
