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

// $Id: AtomCandidateNode.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.training.aa;

import com.sri.ai.patternmatcher.graph.PrimaryCandidateNode;
import com.sri.pal.AbstractActionDef;
import com.sri.pal.ActionStreamEvent;

/**
 * This node in the candidate represents an atom: An action or idiom in the
 * demonstration.
 */
public class AtomCandidateNode
        extends PrimaryCandidateNode {
    private final ActionStreamEvent asEvent;
    private final int responseIndex; 

    /**
     * Create a candidate node for the atom.
     *
     * @param event
     *            the action or idiom event represented by this node
     */
    public AtomCandidateNode(String label, ActionStreamEvent event, int respIndex) {
        super(label);
        if (event == null) {
            throw new NullPointerException();
        }
        asEvent = event;
        responseIndex = respIndex;
    }

    /**
     * Provides access to the idiom or action definition for this node.
     *
     * @return this node's corresponding type definition
     */
    public AbstractActionDef getActionDef() {
        return asEvent.getDefinition();
    }

    /**
     * Provides access to the event which this node represents.
     *
     * @return this node's corresponding action stream event
     */
    public ActionStreamEvent getEvent() {
        return asEvent;
    }
    
    public int getResponseIndex() {
        return responseIndex;
    }    
}
