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

// $Id: ActionModelOntology.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.training.aa;

import java.util.Set;

import com.sri.ai.patternmatcher.Ontology;
import com.sri.ai.patternmatcher.graph.CandidateNode;
import com.sri.ai.patternmatcher.graph.PatternNode;
import com.sri.ai.patternmatcher.graph.PrimaryPatternNode;
import com.sri.pal.AbstractActionDef;
import com.sri.pal.ActionDef;
import com.sri.pal.ActionFamilyDef;
import com.sri.pal.IdiomDef;
import com.sri.pal.TypeDef;
import com.sri.pal.common.SimpleTypeName;

/**
 * Implements the pattern matcher's ontology by checking the action model
 * provided by the Bridge.
 */
public class ActionModelOntology
        implements Ontology {
    /**
     * TODO This should exist somewhere else.
     */
    private static final double FORBIDDEN = -1;

    /*
     * If cn is the same action type as pn, or if pn is an action family of
     * which cn is a member, then return 0.
     *
     * If cn and pn are both parameters, and the type of cn is assignable to pn,
     * then return 0.
     *
     * If one node is an action, and the other is a param, return FORBIDDEN.
     */
    @Override
    public double semanticDistance(PatternNode pn,
                                   CandidateNode cn) {
        /* If the nodes we receive weren't created by us, throw an exception. */
        if (!((pn instanceof AtomPatternNode) || (pn instanceof ParamPatternNode))) {
            throw new RuntimeException("Unexpected class (" + pn.getClass()
                    + ") of pattern node");
        }
        if (!((cn instanceof AtomCandidateNode) || (cn instanceof ArgCandidateNode))) {
            throw new RuntimeException("Unexpected class (" + cn.getClass()
                    + ") of candidate node");
        }

        /*
         * Don't allow the pattern matcher to match a param against an atom, or
         * vice versa.
         */
        if (pn instanceof AtomPatternNode && cn instanceof ArgCandidateNode) {
            return FORBIDDEN;
        }
        if (pn instanceof ParamPatternNode && cn instanceof AtomCandidateNode) {
            return FORBIDDEN;
        }

        if (pn instanceof AtomPatternNode) {
            /* Both objects represent atoms. */
            AtomPatternNode patt = (AtomPatternNode) pn;
            AtomCandidateNode cand = (AtomCandidateNode) cn;
            AbstractActionDef pattActDef = patt.getActionDef();
            AbstractActionDef candActDef = cand.getActionDef();

            /* If they're both actions, they have to match. */
            if (pattActDef instanceof ActionDef
                    && candActDef instanceof ActionDef) {
                if (pattActDef.equals(candActDef)) {
                    return 0;
                } else {
                    /*
                     * Do they belong to a common action family? If so, the
                     * match is still wrong; but we can offer partial credit.
                     *
                     * TODO this is a nice idea but it's difficult in practice because 
                     * it violates assumptions in the Student UI and elsewhere about the
                     * presence of certain argument names/types. 
                     */
                    
//                    Set<SimpleTypeName> pFams = ((ActionDef) pattActDef)
//                            .getFamilies();
//                    Set<SimpleTypeName> cFams = ((ActionDef) candActDef)
//                            .getFamilies();
//                    Set<SimpleTypeName> intersect = new HashSet<SimpleTypeName>(
//                            pFams);
//                    intersect.retainAll(cFams);
//                    if (!intersect.isEmpty()) {
//                        return 0.5;
//                    }
                    return FORBIDDEN;                    
                }
            }

            /*
             * If cn is an action and pn is an action family, cn has to belong
             * to that family.
             */
            if (candActDef instanceof ActionDef
                    && pattActDef instanceof ActionFamilyDef) {
                ActionDef ad = (ActionDef) candActDef;
                Set<SimpleTypeName> families = ad.getFamilies();
                if (families.contains(pattActDef.getName())) {
                    return 0;
                }
            }

            /* If they're both idioms, check for equality. */
            if (pattActDef instanceof IdiomDef
                    && candActDef instanceof IdiomDef) {
                if (pattActDef.equals(candActDef)) {
                    return 0;
                }
            }

            /* Otherwise, return a cost of 1. */
            return FORBIDDEN;
        }

        if (pn instanceof ParamPatternNode) {
            /* The objects represent a param and an argument, respectively. */
            ParamPatternNode param = (ParamPatternNode) pn;
            ArgCandidateNode arg = (ArgCandidateNode) cn;
            TypeDef pType = param.getTypeDef();
            TypeDef cType = arg.getTypeDef();

            /* Can the candidate arg type be assigned to the pattern param type? 
             * A null param type means the parameter is on an idiom or action
             * family and has no declared type. So we have to allow the mapping. 
             */
            if (pType == null || cType.isAssignableTo(pType)) {
                return 0;
            } else {
                return FORBIDDEN;
            }
        }

        /*
         * If we get to this point, it means we've violated some assumption
         * about the objects' classes.
         */
        throw new RuntimeException("Cannot compute for " + pn.getClass()
                + " and " + cn.getClass() + " (" + pn + " and " + cn + ")");
    }

    @Override
    public String getSemanticIdentifier(PrimaryPatternNode pn) {
      return ((AtomPatternNode)pn).getActionDef().getName().getFullName();
    }
}
