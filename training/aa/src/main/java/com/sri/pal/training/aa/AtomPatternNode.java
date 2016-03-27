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

// $Id: AtomPatternNode.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.training.aa;

import com.sri.ai.patternmatcher.graph.PrimaryPatternNode;
import com.sri.pal.AbstractActionDef;
import com.sri.pal.training.core.exercise.Step;

/**
 * This node in the pattern represents an atom: An action, action family, or
 * idiom.
 */
public class AtomPatternNode
        extends PrimaryPatternNode {
    private final AbstractActionDef actionDef;
    private final Step step; 

    private AtomPatternNode previous;
    private AtomPatternNode next; 

    /**
     * Create a pattern node for an atom.
     *
     * @param weight
     *            the weight assigned to this node; larger values imply higher
     *            cost
     * @param actDef
     *            the action, action family, or idiom type which instances of
     *            this node must conform to in the candidate
     */
    public AtomPatternNode(Step step, 
                           String label,
                           double weight,
                           AbstractActionDef actDef) {
        super(label, weight);
        if (step == null) {
            throw new NullPointerException();
        }
        if (actDef == null) {
            throw new NullPointerException();
        }
        actionDef = actDef;
        this.step = step; 
    }

    /**
     * Provides access to the action, action family, or idiom represented by
     * this node.
     *
     * @return this node's type definition
     */
    public AbstractActionDef getActionDef() {
        return actionDef;
    }
    
    public AtomPatternNode getPrevious() {
        return previous;
    }

    public void setPrevious(AtomPatternNode previous) {
        this.previous = previous;
    }

    public AtomPatternNode getNext() {
        return next;
    }

    public void setNext(AtomPatternNode next) {
        this.next = next;
    }

    public Step getStep() {
        return step;
    }

}
