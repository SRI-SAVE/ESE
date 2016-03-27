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

package com.sri.ai.patternmatcher

import scala.collection.JavaConversions.asJavaCollection
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConversions.setAsJavaSet
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ArrayBuffer

import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.ai.patternmatcher.graph.CandidateEdge
import com.sri.ai.patternmatcher.graph.CandidateNode
import com.sri.ai.patternmatcher.graph.Constraint
import com.sri.ai.patternmatcher.graph.Node
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.graph.PatternEdge
import com.sri.ai.patternmatcher.graph.PatternNode
import com.sri.ai.patternmatcher.graph.PrimaryCandidateNode
import com.sri.ai.patternmatcher.graph.PrimaryPatternNode
import com.sri.ai.patternmatcher.graph.SecondaryCandidateNode
import com.sri.ai.patternmatcher.graph.SecondaryPatternNode

/** Represents a partial (or complete) match between a Pattern and a Candidate. 
  * 
  * The matching algorithm uses this data structure to represent various 
  * expansions of the search space. 
  * 
  * This is an immutable class to avoid issues with various branches of the
  * search space inadvertently affecting each other. 
  */

final class Match private (
  val pattern : Pattern, // The pattern being matched against
  val candidate : Candidate, // The candidate graph under consideration
  var nodeMappings : Map[PatternNode, CandidateNode], // Mappings between nodes
  var deletedPrimaryNodes : Set[PrimaryPatternNode], // Primary nodes we weren't able to match
  var deletedSecondaryNodes : Set[SecondaryPatternNode], // Secondary nodes we couldn't match
  var edgeMappings : Map[PatternEdge, CandidateEdge], // Mappings between edges; Currently not being used for anything
  var deletedEdges : Set[PatternEdge], // Edges we weren't able to match
  var satisfiedConstraints : Set[Constraint], // Constraints satisfied by this hypothesis
  var violatedConstraints : Set[Constraint], // Constraints violated by this hypothesis
  var cost : Double, // The cost of this match; Comprises deleted nodes and edges, violated constraints and suboptimal mappings. 
  var currentPrimaryNodes : ArrayBuffer[PrimaryPatternNode], // Mapped nodes that have corresponding unexplored edges or constraints
  var unexploredPrimaryNodes : ArrayBuffer[PrimaryPatternNode], // Primary nodes not yet expanded in this hypothesis
  var unexploredEdges : ArrayBuffer[PatternEdge], // Edges not yet expanded in this hypothesis
  var unexploredConstraints : ArrayBuffer[Constraint], // Constraints not yet expanded in this hypothesis
  val ontology : Ontology, // The ontology to be used during pattern matching,
  val globalCosts: PatternMatcher.GlobalProblems, 
  var numEdits: Int, 
  var trailblazer: Boolean = false,
  val idx : Int = Match.counter) {
  
  Match.counter += 1  
  
  /* The worst case cost of this partial match. */
  lazy val worstCaseCost = {
    var worst = cost
    unexploredPrimaryNodes.foreach(n => {
      worst += n.weight
      for (s <- n.allNodes)
        if (s != n)
          worst += s.weight
    })
    unexploredEdges.foreach(n => worst += n.weight) 
    unexploredConstraints.foreach(n => worst += n.weight)
    
    worst
  }
    
  /* Extra cost for this partial match that can be inferred from global problems */
  lazy val inferredCost = {
    var globalDeleteCost = 0.0
    for (key <- globalCosts._1.keys) {
      val deletes = deletedPrimaryNodes.filter(n => ontology.getSemanticIdentifier(n).equals(key))
      if (deletes.size < globalCosts._1(key).size)
        globalDeleteCost += globalCosts._1(key)(globalCosts._1(key).size - deletes.size - 1)
    }
    
    var unsafeConstraintCost = 0.0

    for (key <- globalCosts._2.keys) {
      val fails = violatedConstraints.filter(c => c.isEquivalentTo(key))
      if (fails.size < globalCosts._2(key).size)
        unsafeConstraintCost += globalCosts._2(key)(globalCosts._2(key).size - fails.size - 1)
    }
    
    var safeConstraintCost = 0.0
    for (key <- globalCosts._3.keys) {
            val fails = violatedConstraints.filter(c => c.isEquivalentTo(key))
      if (fails.size < globalCosts._3(key).size)
        safeConstraintCost += globalCosts._3(key)(globalCosts._3(key).size - fails.size - 1)
    }
    
    math.max(globalDeleteCost + safeConstraintCost, unsafeConstraintCost)
  }
  
  /* Mappings from candidate to pattern nodes */
  lazy val reverseNodeMappings = {
    nodeMappings.map(_.swap)
  }
  
  /** Returns true if this match is a full match, i.e. everything has been explored. 
    * Logically this could be implemented as cost == worstCaseCost but that 
    * approach wasn't taken to avoid floating point arithmetic issues.
    * 
    * @return true if this match is a full match
    */
  def isFullMatch() = {
    unexploredPrimaryNodes.size == 0 &&
    unexploredEdges.size == 0 &&
    unexploredConstraints.size == 0
  }
  
  /** Return a copy of this match with pNode mapped to cNode with a given weight. 
    * Any secondary edges and nodes will be inferred as part of this mapping. 
    * Any constraints that become available for evaluation will be evaluated
    * 
    * @return the match resulting from mapping pNode to cNode
    */
  def mapPrimaryNode(
      pNode: PrimaryPatternNode, 
      cNode: PrimaryCandidateNode, 
      weight: Double) : Match =  {
    var result = copy(numEdits = this.numEdits + 1)
    
    result.mapNode(pNode, cNode, weight)
    
    result.unexploredPrimaryNodes = result.unexploredPrimaryNodes - pNode
    result.currentPrimaryNodes = result.currentPrimaryNodes :+ pNode
    
    val constraints = result.findConstraints()
    
    for (c <- constraints)
      result.evaluateConstraint(c)
    
    result.currentPrimaryNodes = result.maintainCurrentNodes()    
    
    result
  }
  
  private def mapNode(
      pNode: PatternNode,
      cNode: CandidateNode,
      weight: Double) {
    cost += weight  
    nodeMappings = nodeMappings + ((pNode, cNode))
    inferSecondaryMappings(pNode.incoming_edges, cNode.incoming_edges, true)
    inferSecondaryMappings(pNode.outgoing_edges, cNode.outgoing_edges, false)    
  }
  
  /** Infer mappings of secondary nodes and edges recursively */
  private def inferSecondaryMappings(
      pEdges: Seq[PatternEdge], 
      cEdges: Seq[CandidateEdge], 
      incoming: Boolean) {
    for (pe <- pEdges) {
      if (unexploredEdges.contains(pe)) {
        val pNode = if (incoming) pe.source else pe.destination
        
        if (pNode.isSecondary) {
          var ceOpt = cEdges.find(ce => pe.label == ce.label)
          var weight = 0.0
          if (!ceOpt.isEmpty) {
            val ce = ceOpt.get
            val cNode = if (incoming) ce.source else ce.destination

            weight = ontology.semanticDistance(pNode, cNode)
            if (weight >= 0) {
              mapEdge(pe, ce, 0)       
              mapNode(pNode, cNode, weight)
            }
          }         
        
          if ((ceOpt.isEmpty || weight < 0))          
            deleteNode(pNode)         
        }
      }
    }
  }
  
  /** Find any unevaluated constraints that are evaluable */
  private def findConstraints() = {
    val constraints = collection.mutable.Set[Constraint]()
    
    for (node <- currentPrimaryNodes) {
      val c = node.allConstraints.filter(c => unexploredConstraints.contains(c)).foreach(c => {
        var ready = true;
        for (n <- c.nodes)
          if (!nodeMappings.contains(n))
            ready = false; 
        if (ready)
          constraints += c
      })
    }
    constraints
  }
    
  /** Evaluate the given constraint */
  def evaluateConstraint(constraint: Constraint) {
    val args = constraint.nodes.map(node => nodeMappings(node))
    val c = 
    if (constraint.evaluate(args, candidate)) {
      unexploredConstraints = unexploredConstraints - constraint
      satisfiedConstraints = satisfiedConstraints + constraint
    } else {
      unexploredConstraints = unexploredConstraints - constraint
      violatedConstraints = violatedConstraints + constraint
      cost += constraint.weight
    }    
  }   
  
  /** Return a copy of this with node deleted */
  def deletePrimaryNode(node: PrimaryPatternNode) = deleteNode(node)
  
  private def deleteNode(node: PatternNode) = {    
    val result = if (node.isPrimary) copy(numEdits = this.numEdits + 1) else this
        
    var edgeSum = 0.0;
    
    var cost = 0.0
    val affectedConstraints = node.allConstraints.filter(unexploredConstraints.contains(_))
    
    node.allNodes.foreach(cost += _.weight)
    node.allEdges.foreach(cost += _.weight)
    affectedConstraints.foreach(cost += _.weight)
    
    result.cost += cost
    
    val func : (PatternEdge => Unit) = (e => edgeSum += e.weight)
    node.incoming_edges.filter(e => result.unexploredEdges.contains(e)).foreach(func)
    node.outgoing_edges.filter(e => result.unexploredEdges.contains(e)).foreach(func)
    
    if (node.isPrimary) {
      val pnode = node.asInstanceOf[PrimaryPatternNode]
      result.deletedPrimaryNodes = result.deletedPrimaryNodes + pnode
      for (n <- node.allNodes)
        if (n != node)
          result.deletedSecondaryNodes = result.deletedSecondaryNodes + n.asInstanceOf[SecondaryPatternNode]
    
      result.unexploredPrimaryNodes = result.unexploredPrimaryNodes - pnode
    } else {
      node.allNodes.foreach(n => result.deletedSecondaryNodes = result.deletedSecondaryNodes + n.asInstanceOf[SecondaryPatternNode])
    }
    
    result.deletedEdges = result.deletedEdges ++ node.allEdges
    result.unexploredEdges = result.unexploredEdges -- node.allEdges
    
    result.violatedConstraints = result.violatedConstraints ++ affectedConstraints
    result.unexploredConstraints = result.unexploredConstraints -- affectedConstraints

    if (node.isPrimary)
      result.maintainCurrentNodes()         
      
    result         
  }
  
  def getSecondaryMappings : java.util.Map[SecondaryPatternNode, SecondaryCandidateNode] = {
    val ret: java.util.Map[SecondaryPatternNode, SecondaryCandidateNode] = 
      nodeMappings
        .filter(e => e._1.isInstanceOf[SecondaryPatternNode])
        .map(e => ((e._1.asInstanceOf[SecondaryPatternNode], e._2.asInstanceOf[SecondaryCandidateNode])))
    ret
  }
  
  def getSecondaryDeletions : java.util.Set[SecondaryPatternNode] = {
    deletedSecondaryNodes
  }
  def getMappedSecondaryNode(id: String) = {
    nodeMappings.getOrElse(pattern.secondaries(id), null)
  }
  
  def getMappedPrimaryNode(id: String) = {
    nodeMappings.getOrElse(pattern.primaries(id), null)
  }
  
  private def maintainCurrentNodes() = {
    currentPrimaryNodes.filter(node => 
      node.allConstraints.find(c => unexploredConstraints.contains(c)) != None)     
  }
    
  private def mapEdge(pEdge: PatternEdge, cEdge: CandidateEdge, weight: Double) {
    //edgeMappings = edgeMappings + ((pEdge, cEdge)) 
    unexploredEdges = unexploredEdges - pEdge
    cost = cost + weight     
  } 
  
  /** Constructs a copy of this PartialMatch */
  def copy(
      pattern : Pattern = this.pattern,
      candidate : Candidate = this.candidate,
      nodeMappings : Map[PatternNode, CandidateNode] = this.nodeMappings,
      edgeMappings : Map[PatternEdge, CandidateEdge] = this.edgeMappings,
      deletedPrimaryNodes : Set[PrimaryPatternNode] = this.deletedPrimaryNodes,
      deletedSecondaryNodes : Set[SecondaryPatternNode] = this.deletedSecondaryNodes,      
      deletedEdges : Set[PatternEdge] = this.deletedEdges,
      satisfiedConstraints : Set[Constraint] = this.satisfiedConstraints,
      violatedConstraints : Set[Constraint] = this.violatedConstraints,
      cost : Double = this.cost,
      currentNodes : ArrayBuffer[PrimaryPatternNode] = this.currentPrimaryNodes,
      unexploredNodes : ArrayBuffer[PrimaryPatternNode] = this.unexploredPrimaryNodes,
      unexploredEdges : ArrayBuffer[PatternEdge] = this.unexploredEdges,
      unexploredConstraints : ArrayBuffer[Constraint] = this.unexploredConstraints,
      numEdits : Int = this.numEdits) : Match = {
    new Match(pattern, candidate, nodeMappings, deletedPrimaryNodes,
        deletedSecondaryNodes, edgeMappings, deletedEdges, 
        satisfiedConstraints, violatedConstraints, cost, currentNodes,
        unexploredNodes, unexploredEdges, unexploredConstraints, ontology, 
        globalCosts, numEdits, false);
  }
  
  override def toString = {
    if (isFullMatch())
      "full match: " +
      "\n  total-weight: " + pattern.totalWeight +
      "\n  cost: " + cost +         
      "\n  num-edits: " + numEdits +       
      "\n  node mappings: " + nodeMappings +
      "\n  deleted primary nodes: " + deletedPrimaryNodes + 
      "\n  deleted secondary nodes: " + deletedSecondaryNodes +
      "\n  deleted edges: " + deletedEdges + 
      "\n  violated constraints: " + violatedConstraints +
      "\n  satisfied constraints: " + satisfiedConstraints                             
    else
      "partial match: " +
      "\n  total-weight: " + pattern.totalWeight +      
      "\n  cost: " + cost +
      "\n  worst-case: " + worstCaseCost +
      "\n  implied: " + inferredCost +      
      "\n  num-edits: " + numEdits + 
      "\n  current-nodes: " + currentPrimaryNodes + 
      "\n  node mappings: " + nodeMappings +
      "\n  primary deleted nodes: " + deletedPrimaryNodes + 
      "\n  deleted secondary nodes: " + deletedSecondaryNodes +      
      "\n  deleted edges: " + deletedEdges + 
      "\n  violated constraints: " + violatedConstraints +
      "\n  satisfied constraints: " + satisfiedConstraints +                           
      "\n  unexplored nodes: " + unexploredPrimaryNodes + 
      "\n  unexplored edges: " + unexploredEdges +
      "\n  unexplored constraints: " + unexploredConstraints                        
  }
}

/** Companion object for PartialMatch. */
object Match {
  def apply(
      pattern : Pattern,
      candidate : Candidate,
      ontology: Ontology,
      counts : PatternMatcher.CountsMap,
      globalCosts: PatternMatcher.GlobalProblems) = {
 
    new Match(pattern, candidate, Map(), Set(), Set(), Map(), Set(), Set(), Set(),
        0.0, ArrayBuffer(), 
        ArrayBuffer[PrimaryPatternNode]() ++= pattern.nodes.filter(_.isPrimary).map(_.asInstanceOf[PrimaryPatternNode]),
        ArrayBuffer[PatternEdge]() ++= pattern.edges,
        ArrayBuffer[Constraint]() ++= pattern.constraints,
        ontology, globalCosts, 0, true, 0)
  }
  
  var counter = 0
}

/** Defines an ordering on PartialMatches where lowest worst cases come first */
object PartialMatchOrdering extends Ordering[Match] {
  def compare(o1 : Match, o2 : Match) = {
    val o1Cost = o1.cost + o1.inferredCost
    val o2Cost = o2.cost + o2.inferredCost
    
    // Trailblazers go first, followed by non-trailblazers
    if (o1.trailblazer && !o2.trailblazer)
      1
    else if (!o1.trailblazer && o2.trailblazer)
      -1
    else if (o1.trailblazer && o2.trailblazer) {
      // Trailblazers themselves are ordered in a way that allows for a bit
      // more breadth-first exploration than non-trailblazers
      var comp = o1Cost.compareTo(o2Cost)
       
      if (comp == 0)
        comp = o1.numEdits.compareTo(o2.numEdits) * -1
       
      if (comp == 0)
        comp = o1.idx.compareTo(o2.idx)
       
      -comp // PQ is reversed
    } else {  
      // Non trailblazers ordered in a depth-first manner based on 
      // worst case cost. Ties are resolved by a lower score, followed by the
      // match index
      val o1WCost = o1.worstCaseCost + o1.inferredCost
      val o2WCost = o2.worstCaseCost + o2.inferredCost
     
      var comp = o1WCost.compareTo(o2WCost)
      
      if (comp == 0)
        comp = o1Cost.compareTo(o2Cost)
        
      if (comp == 0)
        comp = o1.idx.compareTo(o2.idx)
    
      -comp // PQ is reversed
    }
  }
}

object NodeImpactOrdering extends Ordering[PatternNode] {
  def compare(n1 : PatternNode, n2 : PatternNode) = {
    var comp = n2.maxDeleteCost.compareTo(n1.maxDeleteCost)
    
    if (comp == 0)
      comp = n1.index.compareTo(n2.index)
    
    comp
  }
}
