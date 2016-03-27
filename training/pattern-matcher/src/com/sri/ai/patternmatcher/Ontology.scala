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

import com.sri.ai.patternmatcher.graph.CandidateNode
import com.sri.ai.patternmatcher.graph.PatternNode
import com.sri.ai.patternmatcher.graph.PrimaryPatternNode

/**
 * Interface that needs to be implemented by the driver of the pattern
 * matcher.  This allows the pattern matcher to calculate the cost of
 * representing a given pattern node with a given candidate node.
 */
trait Ontology {
  /**
   * Calculate the semantic distance between a node in the pattern and
   * a node in the candidate.  This answers the question: How suitable
   * is this candidate node for this spot in the pattern?
   * @param pn a node in the pattern
   * @param cn a node in the candidate
   * @return a value from 0 to 1, inclusive.
   */
  def semanticDistance(pn: PatternNode, cn: CandidateNode): Double
  def getSemanticIdentifier(pn: PrimaryPatternNode): String
}
