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
package com.sri.ai.patternmatcher.graph

import java.util.List

/** 
 * Represents a constraint in a Pattern.
 * 
 * Constraints represent constraints on the pattern that aren't explicit in
 * the Pattern graph (like pattern edges). Constraint is intended to be 
 * subclassed by the application using the pattern matcher. Extensions of 
 * Constraint are responsible for actually evaluating the constraint on various
 * mapping hypotheses the pattern matcher comes up with. 
 */

object ConstraintOwnershipType extends Enumeration {
  type ConstraintOwnershipType = Value
  val NO_OWNERS, // A global state constraint indepent of pattern matching problem 
      SINGLE_OWNER, // A constraint owned by a single node. Its violation can be inferred as part of the deletion of the node
      MULTIPLE_OWNERS = Value // A constraint owned by multiple nodes. It's unsafe to assume deletion of such a constraint when deleting one of its parent nodes
}

abstract class Constraint(
  val label: String,
  val nodes: List[_ <: PatternNode],
  val weight: Double) { 
  def evaluate(candidateNodes: List[CandidateNode], cand: Candidate): Boolean;  
  
  override def toString = label
  
  def isEquivalentTo(other: Constraint) : Boolean
  def operatesOn : Class[_]
  def isOrderDependent : Boolean
  def getOwnershipType : ConstraintOwnershipType.Value
}
