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

// $Id: ParamPatternNode.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.training.aa;

import java.util.List;

import com.sri.ai.patternmatcher.graph.SecondaryPatternNode;
import com.sri.pal.TypeDef;

/**
 * This node in the pattern represents a parameter to an abstract action in the
 * solution.
 */
public class ParamPatternNode
        extends SecondaryPatternNode {
    private final TypeDef typeDef;
    private final AtomPatternNode atom;
    private final List<String> accessorPath; 
    private final String id; 
    
    /**
     * Creates a node to represent a parameter to an action.
     *
     * @param weight
     *            the weight assigned to this node, with larger values implying
     *            higher cost
     * @param td
     *            the type of the parameter which arguments in the pattern must
     *            conform to
     */
    public ParamPatternNode(AtomPatternNode atom,
                            String id,
                            double weight,
                            TypeDef td,
                            List<String> accessorPath) {
        super(id, weight);
        if (atom == null) {
            throw new NullPointerException();
        }
        // td might be null, if atom is an idiom.
        if (accessorPath == null) {
            throw new NullPointerException();
        }
        typeDef = td;
        this.id = id; 
        this.atom = atom; 
        this.accessorPath = accessorPath; 
    }

    /**
     * Provides access to the type of the parameter represented by this node.
     *
     * @return this node's type
     */
    public TypeDef getTypeDef() {
        return typeDef;
    }
    
    public AtomPatternNode getAtom() {
        return atom;
    }
    
    public String getId() {
        return id; 
    }
    
    public List<String> getAccessorPath() {
        return accessorPath;
    }    
}
