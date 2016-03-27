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
import collection.mutable.Set
import collection.JavaConversions.asScalaBuffer
import ConstraintOwnershipType._

/**
 * Represents a pattern graph structure. 
 * 
 * Patterns extend the concept of a graph
 * with the concept of "constraints" between nodes. Unlike candidate edges and
 * nodes, pattern edges, nodes and constraints are weighted. The weights are
 * used by the pattern matcher algorithm to bound the search space.
 * 
 * Although not fully enforced at a code-level, it is implied that a Pattern 
 * (and all descendents of Graph) is immutable after its initial 
 * construction. Nodes must be connected prior to constructing a Pattern.
 * 
 * NOTE: Unlike other labels, the labels on PatternNodes must be unique within
 * a given Pattern. 
 */
class Pattern(
    val primaries: scala.collection.Map[String, PrimaryPatternNode],
    val secondaries: scala.collection.Map[String, SecondaryPatternNode],
    var constraints : Seq[Constraint]) extends 
    Graph[PatternEdge, PatternNode](nodes = {val tmp = new ArrayBuffer[PatternNode]() ++= primaries.values ++= secondaries.values; tmp}) {
  
  for (constraint <- constraints) 
    constraint.nodes.foreach(n => n.immediateConstraints += constraint)  
 
  nodes.foreach(_.init)

  /**
   * This is a convenience constructor for tests, the Grapher, etc. Not recommended
   * for use in actual pattern matching. 
   */
  def this(
       nodes: Seq[PatternNode],
       constraints: Seq[Constraint]) {
    this(nodes.filter(_.isInstanceOf[PrimaryPatternNode]).map(n => n.label -> n.asInstanceOf[PrimaryPatternNode]).toMap,
         nodes.filter(_.isInstanceOf[SecondaryPatternNode]).map(n => n.label -> n.asInstanceOf[SecondaryPatternNode]).toMap,
         constraints)
  }
  
  /**
   * The sum of all weights in the pattern graph. Compare this value to the
   * cost of a match to get an idea of how good the match was.   
   */
  lazy val totalWeight = {
    var weight = 0.0
    nodes.foreach(n => weight += n.weight)
    edges.foreach(e => weight += e.weight)
    constraints.foreach(c => weight += c.weight)
    weight
  }
  
  val totalEdits = nodes.size + edges.size + constraints.size
  
  override def toString = {
    super.toString + "constraints: " + constraints + 
      "\ntotal edits: " + totalEdits  + 
      "\ntotal weight: " + totalWeight
  }
  
  def addConstraints(adds: Seq[Constraint]) {
    constraints = constraints ++ adds
    for (add <- adds)
      for (node <- add.nodes)
        node.immediateConstraints += add 
  }
  
  def getPrimaryNode(id: String) = {
    primaries(id)
  }
  
  def getSecondaryNode(id: String) = {
    secondaries(id)
  }
}

/** Companion object for pattern that provides the ability to connect pattern 
  * nodes. 
  */
object Pattern {
  def connectNodes(label : String, source : PatternNode, destination : PatternNode, weight : Double = 0) = {
    val edge = PatternEdge(label, source, destination, weight)
    Graph.connect(edge, source, destination)
    edge
  }
}

/** Represents an edge in a Pattern graph */
class PatternEdge (
    val label : String, 
    val source : PatternNode, 
    val destination : PatternNode, 
    val weight : Double) extends Edge[PatternNode] {
}

/** Companion object for PatternEdge */
object PatternEdge {
  def apply(label: String, source : PatternNode, destination : PatternNode, weight : Double) = {
    new PatternEdge(label, source, destination, weight)
  }
}

/**
 * Represents a node in a Pattern Graph. Note that although labels don't impact
 * pattern matching, labels on PatternNode objects must be unique within a 
 * given Pattern (for canonical ordering purposes) 
 */
trait PatternNode extends Node[PatternEdge] {
  override val incoming_edges = ArrayBuffer[PatternEdge]();
  override val outgoing_edges = ArrayBuffer[PatternEdge]();
  
  val label: String
  var weight: Double
  
  /** The constraints this node participates in. Will be populated when the 
    * Pattern that will contain this node is constructed
    */
  val immediateConstraints = collection.mutable.Set[Constraint]()
  
  var maxDeleteCost = 0.0
  var minDeleteCost = 0.0
  
  val allConstraints = Set[Constraint]()
  val allEdges = Set[PatternEdge]()
  var allNodes = Set[PatternNode]()
  
  def getConstraints =  {
    java.util.Arrays.asList(allConstraints.toArray: _*)
  }
  
  def init = {
    maxDeleteCost = 0
    minDeleteCost = 0
    allConstraints.clear
    allEdges.clear
    allNodes.clear    
    
    recalcDeleteCostAux(this, null)
    
    allEdges.foreach(e => {minDeleteCost += e.weight; maxDeleteCost += e.weight})    
    allNodes.foreach(n => {minDeleteCost += n.weight; maxDeleteCost += n.weight})
    allConstraints.foreach(c => {maxDeleteCost += c.weight; if (c.getOwnershipType == SINGLE_OWNER) minDeleteCost += c.weight})
  }
  
  private def recalcDeleteCostAux(
      n: PatternNode, 
      travelNode: PatternNode) {
    
    allNodes += n    
    allEdges ++= n.outgoing_edges
    allEdges ++= n.incoming_edges
    allConstraints ++= n.immediateConstraints
    
    val travellers = collection.mutable.Set[PatternNode]()
    travellers ++= n.outgoing_edges.map(e => e.destination).filter(next => next != travelNode && next.isSecondary)
    travellers ++= n.incoming_edges.map(e => e.source).filter(next => next != travelNode && n.isPrimary && next.isSecondary)        
    travellers.foreach(t => recalcDeleteCostAux(t, n))
  }
  
  def isPrimary : Boolean
  def isSecondary = !isPrimary
}

class PrimaryPatternNode(
    override val label: String, 
    override var weight: Double) 
    extends PatternNode {
  override def isPrimary = true
}

class SecondaryPatternNode(
    override val label: String, 
    override var weight: Double) 
    extends PatternNode {
    override def isPrimary = false
}

/**
 * Companion object for PrimaryNode. Provides a priority ordering for 
 * PrimaryNodes based on impact. 
 */
object PrimaryPatternNode extends Ordering[PrimaryPatternNode] {
    def compare(o1 : PrimaryPatternNode, o2 : PrimaryPatternNode) = {
    if (o1.maxDeleteCost > o2.maxDeleteCost) 
      -1
    else if (o1.maxDeleteCost < o2.maxDeleteCost)
      1
    else 
      o1.label.compareTo(o2.label)   
  }
    
  def apply(name : String, weight : Double) = {
    new PrimaryPatternNode(name, weight)
  }
}