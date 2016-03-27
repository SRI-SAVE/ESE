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
package com.sri.tasklearning.lapdogController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.learning.ATRDemonstratedAction;
import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameExpr;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.mediators.DependencyFinder;
import com.sri.tasklearning.mediators.TypeFetcher;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a demonstration, find all the types and actions it depends on.
 */
public class DemonstrationDependencyFinder
        implements DependencyFinder<ATRDemonstration> {
    private static final Logger log = LoggerFactory
            .getLogger(DemonstrationDependencyFinder.class);

    private final TypeFetcher typeFetcher;
    private final Set<TypeName> extraTypes;

    /**
     * Creates a new dependency finder.
     *
     * @param fetcher
     *            the type fetcher which can be used to retrieve required types
     */
    public DemonstrationDependencyFinder(TypeFetcher fetcher) {
        typeFetcher = fetcher;
        extraTypes = new HashSet<TypeName>();
    }

    /**
     * Adds extra types which need to be fetched. This is used for fetching
     * completer actions.
     *
     * @param extraTypes
     *            the extra types (or actions) which need to be added to
     *            LAPDOG's vocabulary
     */
    void addTypes(Set<TypeName> extraTypes) {
        if (extraTypes != null) {
            this.extraTypes.addAll(extraTypes);
        }
    }

    @Override
    public List<ATRDecl> getDependencies(ATRDemonstration demo)
            throws SpineException {
        List<ATRDecl> requiredTypes = new ArrayList<ATRDecl>();

        /* Get the "extra" types. */
        for (TypeName extraName : extraTypes) {
            getRequiredTypes(requiredTypes, extraName, true);
        }

        // Get all the required types (and actions).
        for (ATRDemonstratedAction action : demo.getActions()) {
            List<ATRDecl> usedTypes = getRequiredTypes(action);
            for (ATRDecl type : usedTypes) {
                if (!requiredTypes.contains(type)) {
                    requiredTypes.add(type);
                }
            }
        }

        return requiredTypes;
    }

    private List<ATRDecl> getRequiredTypes(ATRDemonstratedAction action)
            throws SpineException {
        List<ATRDecl> result = new ArrayList<ATRDecl>();
        if (TypeUtil.isGesture(action)) {
            for (ATRDemonstratedAction subAction : action.optBody()) {
                result.addAll(getRequiredTypes(subAction));
            }
        } else {
            SimpleTypeName typeName = (SimpleTypeName) TypeNameFactory
                    .makeName(action.getName());
            getRequiredTypes(result, typeName, true);
        }
        return result;
    }

    /**
     * Fetches type definitions for the named type and everything it depends on.
     * This method must return types in order they can be sent to LAPDOG. In
     * other words, a complex type appears after the simple types it depends on.
     *
     * @param result
     *            the result set of required types
     * @param typeName
     *            the type to fetch dependents of
     * @param mustFetch
     *            is the named type truly required, or only desired?
     * @throws SpineException
     *             if a type can't be retrieved
     */
    synchronized void getRequiredTypes(List<ATRDecl> result,
                                       TypeName typeName,
                                       boolean mustFetch)
            throws SpineException {
        // If this type is already in the result set, stop recursing to avoid
        // infinite loops on mutually equivalent types.
        for (ATRDecl type : result) {
            if (TypeUtil.getName(type).equals(typeName)) {
                return;
            }
        }

        if (TypeNameFactory.isPrimitive(typeName)) {
            return;
        }

        if (typeName instanceof SimpleTypeName) {
            SimpleTypeName simpleName = (SimpleTypeName) typeName;
            ATRDecl type = typeFetcher.getType(simpleName);

            // If the type is still null, this means the Bridge was unable
            // to find any type associated with this typename
            if (type == null) {
                if (mustFetch) {
                    log.warn("Unable to load type {}; learning may fail",
                            simpleName);
                } else {
                    log.debug(
                            "Unable to load type {}; learning accuracy may be reduced",
                            simpleName);
                }
            } else {
                // Add all directly required types, and recurse on them.
                for (TypeName subTypeName : TypeUtil.getRequiredTypes(type)) {
                    getRequiredTypes(result, subTypeName, mustFetch);
                }
                // Add this type after all the ones that it depends on.
                result.add(type);
                /*
                 * Add all equivalent types, and recurse on them. Don't panic if
                 * one can't be loaded.
                 */
                if (TypeUtil.isType(type)) {
                    for (TypeName eqvTypeName : TypeUtil
                            .getEquivalentTypeNames((ATRTypeDeclaration) type)) {
                        getRequiredTypes(result, eqvTypeName, false);
                    }
                }
            }
        } else {
            TypeNameExpr nameExpr = (TypeNameExpr) typeName;
            ATR atrColl = TypeUtil.makeCollection(nameExpr);
            TypeName eleName = TypeUtil.getElementType(atrColl);
            getRequiredTypes(result, eleName, mustFetch);
        }
    }
}
