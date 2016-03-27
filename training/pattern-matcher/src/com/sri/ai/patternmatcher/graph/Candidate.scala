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

import collection.mutable.ArrayBuffer

/**
 * Graph structure that represents "candidate" data to be matched against a 
 * pattern. Comprises CandidateEdges and CandidateNodes. 
 * 
 * Although not fully enforced at a code-level, it is implied that a Candidate 
 * (and all descendents of Graph) is immutable after its initial 
 * construction. Nodes must be connected prior to constructing a Candidate. 
 */
class Candidate (nodes : Seq[CandidateNode]) extends Graph[CandidateEdge, CandidateNode](nodes) {
  lazy val primaryNodes = nodes.filter(_.isPrimary).map(_.asInstanceOf[PrimaryCandidateNode])
}

/**
 * Companion object for Candidate class the provides helper methods for 
 * dealing with CandidateNodes and CandidateEdges.
 */
object Candidate {
  def connectNodes(label : String, source : CandidateNode, destination : CandidateNode) = {
    val edge = CandidateEdge(label, source, destination, 0.0)
    Graph.connect(edge, source, destination)
  }
}

/** Represents an edge in a Candidate graph. Note that */
class CandidateEdge (
    val label : String, 
    val source : CandidateNode, 
    val destination : CandidateNode, 
    val weight : Double) extends Edge[CandidateNode] { 
}

/** Companion object for CandidateEdge class */
object CandidateEdge {
  def apply(label: String, source : CandidateNode, destination : CandidateNode, weight : Double) = {
    new CandidateEdge(label, source, destination, weight)
  }
}

/** Represents a node in a Candidate graph. */
trait CandidateNode extends Node[CandidateEdge] {
  val label: String
  override val incoming_edges = ArrayBuffer[CandidateEdge]();
  override val outgoing_edges = ArrayBuffer[CandidateEdge]();
  def isPrimary : Boolean
  def isSecondary = !isPrimary
}

class PrimaryCandidateNode(override val label: String) extends CandidateNode {
  override def isPrimary = true
}

class SecondaryCandidateNode(override val label: String) extends CandidateNode {
  override def isPrimary = false
}