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
import scala.collection.JavaConversions._
import com.sri.ai.patternmatcher.graph.CandidateNode
import com.sri.ai.patternmatcher.graph.Constraint
import com.sri.ai.patternmatcher.graph.PatternNode
import com.sri.pal.training.aa.ArgCandidateNode
import com.sri.pal.training.aa.ParamPatternNode
import com.sri.pal.training.core.exercise.EqualityConstraint
import com.sri.pal.training.aa.AtomPatternNode
import com.sri.ai.patternmatcher.graph.ConstraintOwnershipType
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.graph.Candidate

class EqualityEvaluator(    
  val name: String,
  val node1: ParamPatternNode,
  val node2: ParamPatternNode,
  val constraint: EqualityConstraint,
  weight: Double,
  val support: Boolean) extends Constraint(name, { val list = new ArrayList[PatternNode]; list.add(node1); list.add(node2); list}, weight) {

  override def evaluate(candidateNodes: List[CandidateNode], cand: Candidate): Boolean = {
    val nodes = candidateNodes.asInstanceOf[List[ArgCandidateNode]]
    nodes.zip(nodes.view.tail).forall { t =>
      val node1 = t._1
      val node2 = t._2
      val type1 = node1.getTypeDef()
      val type2 = node2.getTypeDef()
      val value1 = Option(node1.getValue())
      val value2 = Option(node2.getValue())
      
      val equal = value1.equals(value2)
      
      ((type1 == null || type2 == null || 
        type1.isAssignableTo(type2) || 
        type2.isAssignableTo(type1)) &&
        (equal && !constraint.isNegated ||
         !equal && constraint.isNegated))
    }
  }
  
  override def isEquivalentTo(other: Constraint) : Boolean = {
    if (other.isInstanceOf[EqualityEvaluator]) {
      val eeOther = other.asInstanceOf[EqualityEvaluator]
      val type1 = node1.getTypeDef
      val type2 = node2.getTypeDef
      val otype1 = eeOther.node1.getTypeDef
      val otype2 = eeOther.node2.getTypeDef
      
      if (type1 == null || type2 == null || otype1 == null || otype2 == null)
        return false; 
      
      if ((type1.equals(otype1) && type2.equals(otype2)) ||
          (type1.equals(otype2) && type2.equals(otype1))) {
        return true
      }
    }
    false
  }

  def getSupporter = node1.getAtom
  def getSupported = node2.getAtom
  
  override def isOrderDependent = false
  
  override def operatesOn = classOf[ArgCandidateNode]
  
  // There might be some cases where the constraint is subordinate but we'd
  // have to determine the corresponding atom of both params and insure they 
  // are the same node. 
  override def getOwnershipType = ConstraintOwnershipType.MULTIPLE_OWNERS
}
