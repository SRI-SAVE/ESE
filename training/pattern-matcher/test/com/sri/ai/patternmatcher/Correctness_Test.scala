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

import scala.collection.mutable.ArrayBuffer
import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.ai.patternmatcher.graph.CandidateNode
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.graph.PatternNode
import org.testng.annotations.Test
import com.sri.ai.patternmatcher.graph.PrimaryCandidateNode
import com.sri.ai.patternmatcher.graph.PrimaryPatternNode

class Correctness_Test extends PatternMatcher_Test {  
  /**
   * In this test every mapping costs the full weight of the node.
   * This forces it to try all possibilities. 
   */
  @Test def forcedToTryAllPossibilities() {    
    val ontology = new Ontology() {
      def semanticDistance(pn : PatternNode, cn : CandidateNode) = 1      
      def getSemanticIdentifier(pn: PrimaryPatternNode) = "foo"
    }    
    
    val res = generateAndMatchHomogenousGraphs(6, ontology)

    assert(res._2.numFullMatches == 13327)
  } 
  
  /**
   * In this test the pattern matcher is "tempted" to explore all possibilities
   * but in actuality should only generate two full matches. The difference
   * between this and the last test is that the cost of mapping two nodes is 0. 
   * Thus the algorithm should be able to detect that the other possibilities 
   * could at best tie the current best match and thus not expand them. The
   * reason the expected number of full matches is two is because that it generates
   * two full matches when exploring the last unexplored node: the case where
   * it's mapped and the case where it's deleted (which is then tossed out).  
   */
  @Test def temptedToTryAllPossibilities() {
    val ontology = new Ontology() {
      def semanticDistance(pn : PatternNode, cn : CandidateNode) = 0      
      def getSemanticIdentifier(pn: PrimaryPatternNode) = "foo"
    }    
    
    val res = generateAndMatchHomogenousGraphs(6, ontology)

    assert(res._2.numFullMatches == 2)
  }
  
  private def generateAndMatchHomogenousGraphs(numNodes: Int, ontology: Ontology) = {
    val patternNodes = ArrayBuffer[PatternNode]()
    val candidateNodes = ArrayBuffer[CandidateNode]()
    for (i <- 0 until numNodes) {
      patternNodes += new PrimaryPatternNode(i.toString(), 1)
      candidateNodes += new PrimaryCandidateNode(i.toString())
    }
    
    val pat = new Pattern(patternNodes, ArrayBuffer())
    PatternMatcher.computeMatch(
        pat, 
        new Candidate(candidateNodes), 
        ontology,
        pat.totalWeight,
        100000)
  }
}