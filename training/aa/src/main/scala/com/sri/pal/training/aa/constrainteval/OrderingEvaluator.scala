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
package com.sri.pal.training.aa.constrainteval

import java.util.ArrayList
import java.util.List

import scala.collection.JavaConversions.asScalaBuffer

import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.ai.patternmatcher.graph.CandidateNode
import com.sri.ai.patternmatcher.graph.Constraint
import com.sri.ai.patternmatcher.graph.ConstraintOwnershipType
import com.sri.pal.training.aa.AtomCandidateNode
import com.sri.pal.training.aa.AtomPatternNode

/**
 * Asserts that one action happened after another. If predecessor is null, it
 * asserts that successor is preceded by every other action, in other words it's
 * the last action. Similarly if successor is null, it asserts that predecessor
 * is the first action.
 */
class OrderingEvaluator(
  name: String,
  val predecessor: AtomPatternNode,
  val successor: AtomPatternNode, 
  weight: Double,
  val inferred: Boolean = false)
  extends Constraint(name, {
    val list = new ArrayList[AtomPatternNode]; 
    if (predecessor != null) 
      list.add(predecessor) 
    if (successor != null)
      list.add(successor)
    list}
  , weight) {
  /* Candidate has a sequence of nodes, only some of which are actions. */
  override def evaluate(args: List[CandidateNode], cand: Candidate): Boolean = {
    /* Extract only the nodes which represent actions. */
    val actions = cand.nodes.filter { n =>
      n.isInstanceOf[AtomCandidateNode]
    }

    if (predecessor == null)
      return args.head == actions.last

    if (successor == null)
      return args.head == actions.head

    def isSorted(l: Seq[Int]): Boolean = {
      l.view.zip(l.tail).forall(z => z._1 < z._2)
    }
    val indexes = args.map { node =>
      node.asInstanceOf[AtomCandidateNode].getResponseIndex
    }
    isSorted(indexes)
  }

  override def isEquivalentTo(other: Constraint) : Boolean = {
    if (!other.isInstanceOf[OrderingEvaluator]) {
      return false
    }
    val oOther = other.asInstanceOf[OrderingEvaluator]
    if ((predecessor == null && oOther.predecessor != null)
        || (predecessor != null && oOther.predecessor == null)) {
      return false
    }
    if ((successor == null && oOther.successor != null)
        || (successor != null && oOther.successor == null)) {
      return false
    }
    if (predecessor != null &&
        !predecessor.getActionDef().equals(oOther.predecessor.getActionDef())) {
      return false
    }
    if (successor != null &&
        !successor.getActionDef().equals(oOther.successor.getActionDef())) {
      return false
    }
    return true
  }

  override def isOrderDependent = true
        
  override def operatesOn = classOf[AtomCandidateNode]
  
  override def getOwnershipType = ConstraintOwnershipType.MULTIPLE_OWNERS
}