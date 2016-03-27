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

// $Id: ArgCandidateNode.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.training.aa;

import com.sri.ai.patternmatcher.graph.SecondaryCandidateNode;
import com.sri.pal.TypeDef;

/**
 * This node in the candidate represents an argument to an abstract action in
 * the solution demonstration.
 */
public class ArgCandidateNode
        extends SecondaryCandidateNode {
    private final TypeDef typeDef;
    private final Object arg;

    /**
     * Create a candidate node for the argument.
     *
     * @param td
     *            the type of the argument
     */
    public ArgCandidateNode(String label,
                            TypeDef td,
                            Object value) {
        super(label);
        if (td == null) {
            throw new NullPointerException();
        }
        typeDef = td;
        arg = value;
    }

    /**
     * Provides access to the type of the argument.
     *
     * @return the argument's type
     */
    public TypeDef getTypeDef() {
        return typeDef;
    }

    public Object getValue() {
        return arg;
    }
}
