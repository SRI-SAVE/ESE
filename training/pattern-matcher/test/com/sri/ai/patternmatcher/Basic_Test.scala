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

import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.mutable.ArrayBuffer

import org.testng.annotations.Test

import com.sri.ai.patternmatcher.graph.ConstraintOwnershipType.SINGLE_OWNER
import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.ai.patternmatcher.graph.CandidateNode
import com.sri.ai.patternmatcher.graph.Constraint
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.graph.PatternNode
import com.sri.ai.patternmatcher.graph.PrimaryCandidateNode
import com.sri.ai.patternmatcher.graph.PrimaryPatternNode
import com.sri.ai.patternmatcher.graph.SecondaryCandidateNode
import com.sri.ai.patternmatcher.graph.SecondaryPatternNode

@Test
class Basic_Test extends PatternMatcher_Test {
  val ontology = new Ontology() {
    def semanticDistance(pn : PatternNode, cn : CandidateNode) = {
      if (pn.label.equals(cn.label)) 0 else 1;         
    }
    def getSemanticIdentifier(pn: PrimaryPatternNode) = "foo"
  }
  
  @Test def simplePerfectMatch() : Unit = {
    val n1 = new PrimaryPatternNode("A1", 10)
    val n2 = new PrimaryPatternNode("A2", 10)
    val n3 = new SecondaryPatternNode("P1", 10)
    
    // Pattern links are weighted
    Pattern.connectNodes("output", n1, n3, 5)
    Pattern.connectNodes("input", n3, n2, 5)
    
    val pattern = new Pattern(ArrayBuffer(n1, n2, n3), ArrayBuffer())
    
    val cn1 = new PrimaryCandidateNode("A1")
    val cn2 = new PrimaryCandidateNode("A2")
    val cn3 = new SecondaryCandidateNode("P1")
    
    Candidate.connectNodes("output", cn1, cn3)
    Candidate.connectNodes("input", cn3, cn2)
    
    val candidate = new Candidate(ArrayBuffer(cn1, cn2, cn3))
    val res = PatternMatcher.computeMatch(pattern, candidate, ontology, pattern.totalWeight);

    assert(res._1.get.cost == 0)
    assert(res._1.get.deletedEdges.size == 0)
    assert(res._1.get.deletedPrimaryNodes.size == 0)
    assert(res._1.get.nodeMappings.size == 3)
    //assert(res._1.edgeMappings.size == 2)
    assert(res._1.get.nodeMappings(n1) == cn1)
    assert(res._1.get.nodeMappings(n2) == cn2)
    assert(res._1.get.nodeMappings(n3) == cn3)
    //assert(res._1.edgeMappings(n1.outgoing_edges.head) == cn1.outgoing_edges.head)
    //assert(res._1.edgeMappings(n2.incoming_edges.head) == cn2.incoming_edges.head)
  }
  
  @Test def effectivelyPerfectMatch() = {
    val n1 = new PrimaryPatternNode("A1", 10)
    val n2 = new PrimaryPatternNode("A2", 10)
    val n3 = new SecondaryPatternNode("P1", 10)
    
    // Pattern links are weighted
    Pattern.connectNodes("output", n1, n3, 5)
    Pattern.connectNodes("input", n3, n2, 5)
    
    val pattern = new Pattern(ArrayBuffer(n1, n2, n3), ArrayBuffer())
    
    val cn1 = new PrimaryCandidateNode("A1")
    val cn2 = new PrimaryCandidateNode("A2")
    val cn3 = new SecondaryCandidateNode("P1")
    val cn4 = new SecondaryCandidateNode("P2")
    val cn5 = new SecondaryCandidateNode("P3")
    
    Candidate.connectNodes("input", cn4, cn1)
    Candidate.connectNodes("output", cn1, cn3)
    Candidate.connectNodes("input", cn3, cn2)
    Candidate.connectNodes("input", cn5, cn2)
    
    val candidate = new Candidate(ArrayBuffer(cn1, cn2, cn3))
    val res = PatternMatcher.computeMatch(pattern, candidate, ontology, pattern.totalWeight);

    assert(res._1.get.cost == 0)
    assert(res._1.get.deletedEdges.size == 0)
    assert(res._1.get.deletedPrimaryNodes.size == 0)
    assert(res._1.get.nodeMappings.size == 3)
    //assert(res._1.edgeMappings.size == 2)
    assert(res._1.get.nodeMappings(n1) == cn1)
    assert(res._1.get.nodeMappings(n2) == cn2)
    assert(res._1.get.nodeMappings(n3) == cn3)
    //assert(res._1.edgeMappings(n1.outgoing_edges.head) == cn1.outgoing_edges.head)
    //assert(res._1.edgeMappings(n2.incoming_edges.head) == cn2.incoming_edges.head)
  }
  
  @Test def constraintViolation() = {
    val n1 = new PrimaryPatternNode("A1", 10)
    val patNodes = new ArrayBuffer[PrimaryPatternNode]()
    patNodes += n1
    val c = new Constraint("failure", patNodes, 15) {
      override def evaluate(args: java.util.List[CandidateNode], cand: Candidate) = {
        false
      }
      override def isEquivalentTo(c: Constraint) = false
      override def operatesOn = classOf[PrimaryCandidateNode]
      override def isOrderDependent = true
      override def getOwnershipType = SINGLE_OWNER
    }
    
    val pattern = new Pattern(ArrayBuffer(n1), ArrayBuffer(c))
    
    val cn1 = new PrimaryCandidateNode("A1")
    val candidate = new Candidate(ArrayBuffer(cn1))
    val res = PatternMatcher.computeMatch(pattern, candidate, ontology, pattern.totalWeight);
    
    assert(res._1.get.cost == 15)
    assert(res._1.get.violatedConstraints.size == 1)
    assert(res._1.get.satisfiedConstraints.size == 0)
  }
  
  @Test def constraintSatisfaction() = {
    val n1 = new PrimaryPatternNode("A1", 10)
    val patNodes = new ArrayBuffer[PrimaryPatternNode]()
    patNodes += n1
    val c = new Constraint("success", patNodes, 15) {
      override def evaluate(args: java.util.List[CandidateNode], cand: Candidate) = {
        true
      }
      override def isEquivalentTo(c: Constraint) = false
      override def operatesOn = classOf[PrimaryCandidateNode]
      override def isOrderDependent = true
      override def getOwnershipType = SINGLE_OWNER     
    }
    
    val pattern = new Pattern(ArrayBuffer(n1), ArrayBuffer(c))
    
    val cn1 = new PrimaryCandidateNode("A1")
    val candidate = new Candidate(ArrayBuffer(cn1))
    val res = PatternMatcher.computeMatch(pattern, candidate, ontology, pattern.totalWeight);
    
    assert(res._1.get.cost == 0)
    assert(res._1.get.violatedConstraints.size == 0)
    assert(res._1.get.satisfiedConstraints.size == 1)
  }
}