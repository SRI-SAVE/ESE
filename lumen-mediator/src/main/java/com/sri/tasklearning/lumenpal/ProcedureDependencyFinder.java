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

// $Id: ProcedureDependencyFinder.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameExpr;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.mediators.DependencyFinder;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.mediators.TypeFetcher;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.util.TypeUtil;

/**
 * Finds dependencies of a procedure. This is called before procedure execution
 * starts; it recursively loads all sub-procedures, actions, and types that a
 * given procedure depends on.
 */
public class ProcedureDependencyFinder
        implements DependencyFinder<ATRActionDeclaration> {
    private final LockingActionModel actionModel;
    private final TypeFetcher fetcher;

    public ProcedureDependencyFinder(LockingActionModel lam,
                                     TypeFetcher typeFetcher) {
        actionModel = lam;
        fetcher = typeFetcher;
    }

    @Override
    public List<ATRDecl> getDependencies(ATRActionDeclaration actDecl)
            throws SpineException {
        List<ATRDecl> result = new ArrayList<ATRDecl>();
        /*
         * Retrieve all the required types of this procedure. Don't retrieve the
         * procedure itself; we should already have it.
         */
        for (TypeName name : TypeUtil.getRequiredTypes(actDecl)) {
            result.addAll(getRequiredTypes(name, true));
        }

        /* Eliminate duplicates, keeping the first occurrence of each item. */
        Set<TypeName> names = new HashSet<TypeName>();
        for (int i = 0; i < result.size(); i++) {
            ATRDecl item = result.get(i);
            TypeName name = TypeUtil.getName(item);
            if (names.contains(name)) {
                result.remove(i);
                i--;
            } else {
                names.add(name);
            }
        }

        result.add(actDecl);

        return result;
    }

    /**
     * Fetches type definitions for the named type and everything it depends on.
     * This method must return types in order they can be sent to Lumen. In
     * other words, a complex type appears after the simple types it depends on.
     *
     * @param typeName
     *            the typeName to request
     * @param mustRetrieve
     *            if {@code true}, and a required type can't be found, a
     *            {@code SpineException} will be thrown.
     * @throws SpineException
     *             if something goes wrong
     * @return The type and all sub types associated with the typeName
     */
    List<ATRDecl> getRequiredTypes(TypeName typeName,
                                   boolean mustRetrieve)
            throws SpineException {
        List<ATRDecl> result = new ArrayList<ATRDecl>();

        if (TypeNameFactory.isPrimitive(typeName)) {
            return result;
        }

        ATRDecl type = null;
        if (typeName instanceof SimpleTypeName) {
            SimpleTypeName simpleName = (SimpleTypeName) typeName;
            type = actionModel.getRaw(simpleName);
            if (type == null) {
                type = fetcher.getType(simpleName);
            }

            if (type == null) {
                if (mustRetrieve) {
                    throw new SpineException("Unable to load type/action "
                            + typeName.getFullName());
                }
                return result;
            }

            for (TypeName subTypeName : TypeUtil.getRequiredTypes(type)) {
                result.addAll(getRequiredTypes(subTypeName, mustRetrieve));
            }
            result.add(type);
        } else {
            TypeNameExpr nameExpr = (TypeNameExpr) typeName;
            ATR atrColl = TypeUtil.makeCollection(nameExpr);
            TypeName eleName = TypeUtil.getElementType(atrColl);
            result.addAll(getRequiredTypes(eleName, mustRetrieve));
        }

        return result;
    }
}
