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

import collection.JavaConversions.asScalaBuffer
import collection.JavaConversions.seqAsJavaList
import collection.JavaConversions.bufferAsJavaList
import collection.mutable.PriorityQueue
import collection.mutable.HashMap
import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.graph.PatternNode
import com.sri.ai.patternmatcher.graph.CandidateNode
import com.sri.ai.patternmatcher.graph.PatternEdge
import com.sri.ai.patternmatcher.graph.Constraint
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import scala.math.Ordering.DoubleOrdering
import com.sri.ai.patternmatcher.graph.ConstraintOwnershipType._
import com.sri.ai.patternmatcher.graph.PrimaryPatternNode
import com.sri.ai.patternmatcher.graph.PrimaryCandidateNode
import com.sri.ai.patternmatcher.graph.ConstraintOwnershipType

object PatternMatcher {
  val log = LoggerFactory.getLogger(PatternMatcher.getClass());  
  
  type CountsMap = Map[String, Tuple4[Int, Int, Seq[PrimaryPatternNode], Seq[PrimaryCandidateNode]]]
  
  /* ._1 Global missing node counts/costs
   * ._2 Global unsafe constraint violations (cannot be combined with inferred missing node costs)
   * ._3 Global safe constraint violations (can be combined with inferred missing node costs)
   */
  type GlobalProblems = Tuple3[HashMap[String,     Seq[Double]], 
                               HashMap[Constraint, Seq[Double]], 
                               HashMap[Constraint, Seq[Double]]]
  
  /**
   * The main pattern matching algorithm. 
   * 
   * Takes a single pattern and a single candidate and attempts to find the best
   * match for those two graphs using a branch and bound algorithm based on 
   * weights defined in the Pattern. Uses an Ontology that knows how pattern 
   * nodes can be mapped to candidate nodes and at what cost. 
   */
  def computeMatch(
      pattern: Pattern, 
      candidate: Candidate, 
      ontology: Ontology,
      costBound: Double, 
      maxExpansions: Int = 25000,
      maxTrailblazers: Int = 100) : (Option[Match], ExecutionInfo) = {

    Match.counter = 0
    
    var pq = new PriorityQueue[Match]()(PartialMatchOrdering)
    val counts = countMappings(pattern, candidate, ontology)  
    val globalCosts = identifyGlobalProblems(pattern, candidate, ontology, counts)
    var pm = Match(pattern, candidate, ontology, counts, globalCosts);      
    var best : Option[Match] = None
    val execInfo = new ExecutionInfo
    execInfo.numTrailblazers = 1
    
    // Evaluate all constraints that are independent of the pattern matching problem
    pattern.constraints.filter(c => c.getOwnershipType == NO_OWNERS).foreach(c => { pm.evaluateConstraint(c) })
    
    // Special case for when nothing matches, basically short-circuits the algorithm
    if (pm.cost + pm.inferredCost == pattern.totalWeight)
      pm.unexploredPrimaryNodes.foreach(n => pm = pm.deletePrimaryNode(n))
    
    for (atomShortage <- globalCosts._1) {
      val id = atomShortage._1
      if (counts(id)._2 == 0) {
        for (n <- pm.unexploredPrimaryNodes.filter(n => ontology.getSemanticIdentifier(n).equals(id))) {
          pm = pm.deletePrimaryNode(n)
        }
      }
    }
    for (count <- counts) {
      if (count._2._1 == 1 && count._2._2 == 1) {
        val pn = count._2._3(0)
        val cn = count._2._4(0)
        pm = pm.mapPrimaryNode(pn, cn, 0)
      }
    }
    
    pq += pm 
    
    log.debug(pattern.toString)
    log.debug(candidate.toString)

    while (!pq.isEmpty && execInfo.numExpansions < maxExpansions) {
      val curMatch = pq.dequeue();
      execInfo.numExpansions += 1

      if (curMatch.isFullMatch)
        execInfo.numFullMatches += 1         
            
      if (curMatch.isFullMatch() && (best.isEmpty || curMatch.cost < best.get.cost)) {
        best = Option(curMatch)
        execInfo.bestMatchIndex = execInfo.numExpansions

      } else if (!curMatch.isFullMatch()) {
        if (((curMatch.cost + curMatch.inferredCost) < costBound) && (best.isEmpty || (curMatch.cost + curMatch.inferredCost) < best.get.cost)) {
          val expansions = expandSearchSpace(curMatch, best, ontology, counts, globalCosts)
          if (execInfo.numTrailblazers < maxTrailblazers) {
            execInfo.numTrailblazers -= 1
            expansions.foreach(e => if (execInfo.numTrailblazers < maxTrailblazers) {e.trailblazer = true; execInfo.numTrailblazers += 1})
          } else if (curMatch.trailblazer)
            expansions.sorted(PartialMatchOrdering).last.trailblazer = true
            
          pq ++= expansions
        }
      }
    }
        
    if (pq.isEmpty)
      execInfo.optimalMatch = true   

    log.debug(execInfo.toString)
    ((best, execInfo))
  }
  
  private def getCombinations[T](n: Int, l: Seq[PrimaryPatternNode]): Seq[Seq[PrimaryPatternNode]] =
    n match {
      case 0 => Seq(Seq())
      case _ => for(el <- l;
                  sl <- getCombinations(n-1, l dropWhile { _ != el } ))
                  yield sl :+ el
  }
  
  /** Further expands a partial match. Expands any deletes first since we
    * we know they're costly and will push us closer to the current bound. Then
    * uses a greedy approach to node mapping that should cause constraints to
    * be evaluated sooner rather than later. 
    */
  private def expandSearchSpace(
      pm: Match,
      best: Option[Match], 
      ontology: Ontology,
      counts: CountsMap, 
      globalProblems: GlobalProblems) : List[Match] = {
    
    // Check to see if this partial match still has deletes left to perform. 
    // If so, choose one of them and generate the legal delete expansions
    for (atomShortage <- globalProblems._1) {
      val id = atomShortage._1
      val numReqDeletes = atomShortage._2.size
      val deleted = pm.deletedPrimaryNodes
        .filter(n => ontology.getSemanticIdentifier(n).equals(id))
        
      if (deleted.size < numReqDeletes) {
        val dels = pm.unexploredPrimaryNodes.filter(n => ontology.getSemanticIdentifier(n).equals(id))
        val combs = getCombinations(numReqDeletes, dels)
        var expansions = List[Match]()        
                
        combs.foreach(comb => {
          var exp = pm
          comb.foreach(n => exp = exp.deletePrimaryNode(n))
          expansions = expansions :+ exp
        })
        
        return expansions        
      }
    }  
    
    // There weren't any remaining deletes so now we move on to mappings
    val possibilities = pm.unexploredPrimaryNodes
    
    val node = 
    if (pm.currentPrimaryNodes.size > 0) {
      // If there are current nodes, choose the unexplored node that will cause
      // the highest weight of constraints on the current nodes to be expanded
      var best: Option[(PrimaryPatternNode, Double)] = None
      for (n <- possibilities) {
        var contribution = 0.0
        
        val affected = collection.mutable.Set[Constraint]()
        pm.currentPrimaryNodes.foreach(curr => affected ++= curr.allConstraints.filter(con => con.nodes.contains(n)))
        affected.foreach(contribution += _.weight)
        
        n.allConstraints.filter(c => c.getOwnershipType == ConstraintOwnershipType.SINGLE_OWNER).foreach(c => contribution += c.weight)
        
        if (best == None || best.get._2 < contribution) {
          best = Option((n, contribution))
        }
      }

      best.get._1
    } else {
      // If there are no current nodes, choose the node that has the most
      // constraint weight (= max delete cost - min delete cost) to be expanded
      // next
      var best : Option[PrimaryPatternNode] = None
      for (n <- possibilities)
        if (best == None || best.get.maxDeleteCost - best.get.minDeleteCost < n.maxDeleteCost - n.minDeleteCost)
          best = Option(n)
      best.get
    }
      
    expandNodeMappings(pm, node, ontology, counts, globalProblems)
  } 
  
  /** Expand the search space by mapping an unexplored pattern node to an 
    * unmapped candidate node. 
    * 
    * Return the set of search space expansions. 
    */
  private def expandNodeMappings(
      pm : Match, 
      patNode: PrimaryPatternNode, 
      ontology: Ontology,
      counts: CountsMap,    
      globalProblems: GlobalProblems) : List[Match] = {
    
    var expansions = List[Match]()   
    
    if (pm.unexploredPrimaryNodes.size == 0)
      return expansions       

    val preferences = getMappingPreferences(patNode, counts, ontology, pm)
    
    expansions = expansions ++ preferences.map(
        candidateNode => 
          pm.mapPrimaryNode(patNode, candidateNode, 
              ontology.semanticDistance(patNode, candidateNode) * patNode.weight))
              
    // You might wonder why we generate a delete expansion here even though we
    // have supposedly satisfied any required deletes first in expandSearchSpace.
    // The reason is because in general a candidate node may be mapped to more
    // pattern node semantic identifier, meaning that the global deletion data
    // structure might not realize all node shortages. This could be addressed
    // with a more cunning data structure and analysis
    expansions = expansions :+ pm.deletePrimaryNode(patNode)
  
    expansions    
  }
  
  /** Perform a global analysis on the availability of candidate nodes to be
    * mapped to pattern nodes that share the same semantic identifier. These
    * counts are later translated in to implied deletions if there is a shortcoming. 
    */
  private def countMappings(
      pattern: Pattern, 
      candidate: Candidate, 
      ontology: Ontology) = {
    var map = Map[String, Tuple4[Int, Int, ArrayBuffer[PrimaryPatternNode], ArrayBuffer[PrimaryCandidateNode]]]()
    for (pn <- pattern.primaries.values) {
      val id = ontology.getSemanticIdentifier(pn)
      if (!map.contains(id)) {
        val candidates = ArrayBuffer[PrimaryCandidateNode]()
        for (cn <- candidate.primaryNodes)
          if (ontology.semanticDistance(pn, cn) >= 0) {
            candidates += cn
          }
        map = map + ((id, (1, candidates.size, ArrayBuffer[PrimaryPatternNode]() += pn, candidates)))
      } else {
        val tuple = map(id)
        tuple._3 += pn 
        map = map - id
        map = map + ((id, (tuple._1 + 1, tuple._2, tuple._3, tuple._4)))
      }
    }
    map
  }
      
  private val mappingPreferences = HashMap[PrimaryPatternNode, Seq[PrimaryCandidateNode]]()
  
  /** Calculate the preferred order for mapping a given pattern node to all
    * mappable candidate nodes. This involves looking at the ordering and
    * proximity of nodes between the Pattern and Candidate
    */
  private def getMappingPreferences(
      patNode: PrimaryPatternNode,
      counts: CountsMap,
      ontology: Ontology,
      pm: Match) = {
    if (!mappingPreferences.contains(patNode)) {
      val id = ontology.getSemanticIdentifier(patNode)
      val idx = counts(id)._3.indexOf(patNode)
      val diff = counts(id)._2 - counts(id)._1
      var candIdx = (idx * counts(id)._2) / counts(id)._1 
      val candidates = counts(id)._4
    
      if (candidates.size > 0) { 
        var preferences = ArrayBuffer[PrimaryCandidateNode]()
        var step = 0
        var oldSize = -1
        while (preferences.size != oldSize) {
          oldSize = preferences.size
          if (step == 0) {
            preferences += candidates(candIdx)
          } else {
            if (candIdx + step < candidates.size)
              preferences += candidates(candIdx + step)
            if (candIdx - step >= 0)
              preferences += candidates(candIdx - step)
          }
          step += 1
        }
        mappingPreferences += ((patNode, preferences))
      }
    }
    mappingPreferences(patNode).filter(cn => !pm.reverseNodeMappings.contains(cn))    
  }
  
  /** Performs a global analysis that identifies node shortages in the Candidate
    * and constraints that won't be satisfied by any possible match.     
    */
  private def identifyGlobalProblems(
      pattern: Pattern,
      candidate: Candidate, 
      ontology: Ontology, 
      counts: CountsMap) = {
    val globalProblems : GlobalProblems = ((HashMap(), HashMap(), HashMap()))
    val nodeMap = globalProblems._1
    val unsafeConstraintMap = globalProblems._2
    val safeConstraintMap = globalProblems._3
    
    for (key <- counts.keys.filter(key => { val pair = counts(key); pair._1 > pair._2 })) {
      val pair = counts(key)
      val matches = new ArrayBuffer[PatternNode]
      for (node <- pattern.primaries.values) {
        if (ontology.getSemanticIdentifier(node).equals(key))
          matches += node
      }
      if (matches.size > 0) {
        var costs = matches.map(n => n.minDeleteCost)
        costs = costs.sorted(math.Ordering.Double)
        costs = costs.dropRight(matches.size - (pair._1 - pair._2))

        for (i <- 1 until costs.size)
          costs(i) = costs(i) + costs(i - 1)

        globalProblems._1 += ((key, costs))
      }
    }
    
    val constraints = pattern.constraints.filter(c => c.getOwnershipType != NO_OWNERS).toBuffer
    
    while (!constraints.isEmpty) {
      val model = constraints.remove(0)
      val cons = ArrayBuffer() += model      
      for (other <- constraints.toBuffer[Constraint]) {
        if (model.isEquivalentTo(other)) {
          constraints -= other
          cons += other
        }
      }
      
      val cands = candidate.nodes.filter(model.operatesOn.isInstance(_)).toList
      val found = findConstraintMatches(candidate, cands, List[CandidateNode](), model, ontology, 0, cons.size)
      
      if (found < cons.size) {
        var costs = cons.map(c => c.weight)
        costs = costs.sorted(math.Ordering.Double)
        costs = costs.dropRight(found)
        for (i <- 1 until costs.size)
          costs(i) = costs(i) + costs(i - 1)
        if (model.getOwnershipType == SINGLE_OWNER)
          unsafeConstraintMap += ((model, costs))
        else
          safeConstraintMap += ((model, costs))
      }
    }
    
    globalProblems
  }
  
  /** Finds matches for a given constraint in the Candidate without regard for
    * pattern matching/connectivity.       
    */
  private def findConstraintMatches(
      candidate: Candidate,
      candidates: List[CandidateNode],
      args: List[CandidateNode],
      constraint: Constraint,
      ontology: Ontology,
      found: Int, 
      enough: Int) : Int = {
    var localCands = candidates
    var localFound = found;
    if (args.size == constraint.nodes.size) {
      val test = new java.util.ArrayList[CandidateNode]
      args.foreach(test.add(_))
      localFound += { if (constraint.evaluate(test, candidate)) 1 else 0 }
    } else {
      for (c <- candidates) {
        if (ontology.semanticDistance(constraint.nodes(args.size), c) >= 0) {
          if (!constraint.isOrderDependent) {
            localCands = localCands.filterNot(_ == c)
            localFound = findConstraintMatches(candidate, localCands, args :+ c, constraint, ontology, localFound, enough)
          } else          
            localFound = findConstraintMatches(candidate, localCands.filterNot(_ == c), args :+ c, constraint, ontology, localFound, enough)
            
          if (localFound >= enough)
            return localFound
        }
      }      
    }
    localFound
  }
}

class ExecutionInfo {
  var numFullMatches = 0
  var numExpansions = 0
  var bestMatchIndex = 0
  var numTrailblazers = 0
  var optimalMatch = false
  
  override def toString = "Optimal Match: " + optimalMatch + 
                          "\n  Best Match Index: " + bestMatchIndex +  
                          "\n  # Expansions: " + numExpansions +
                          "\n  # Trailblazers: " + numTrailblazers +
                          "\n  # Full Matches: " + numFullMatches
}

