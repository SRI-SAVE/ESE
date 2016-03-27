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
package com.sri.pal.training.aa

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConversions.asScalaBuffer
import com.sri.ai.patternmatcher.ExecutionInfo
import com.sri.ai.patternmatcher.Match
import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.pal.Bridge
import com.sri.pal.training.core.assessment.AssessedResponseIndexes
import com.sri.pal.training.core.assessment.TaskAssessment
import com.sri.pal.training.core.exercise.Exercise
import com.sri.pal.training.core.exercise.Task
import com.sri.pal.training.core.exercise.TaskSolution
import com.sri.pal.training.core.response.Response
import com.sri.pal.training.core.response.TaskResponse
import com.sri.pal.training.core.exercise.Value
import com.sri.ai.patternmatcher.PatternMatcher
import java.util.HashMap
import com.sri.pal.training.aa.state.StateMap
import com.sri.pal.training.core.util.ResponseUtil


/**
 * This is the high-level, outward-facing interface of the automated assessment
 * module. It masks the complexity of automated assessment behind the relatively
 * simple data flow required.
 */

class Assessor(val bridge: Bridge) {
  /**
   * Execution statistics from the most recent completed call to the assess
   * method.
   */
  var lastExecInfo: ExecutionInfo = null
  var lastMatch: Match = null

  /**
   * Perform an assessment -- how well did a student respond to an exercise?
   *
   * @param resp
   *            the response to the given exercise
   * @return an assessment of how well {@code resp} answers {@code ex}
   */
  def assessTask(
      task: Task, 
      solution: TaskSolution, 
      resp: TaskResponse,
      symbols: SymbolManager) = {
    val dispatcher = new QueryDispatcher(bridge)
    val ontology = new ActionModelOntology()
    val candidate = Translator.taskResponseToCandidate(resp, bridge)
    val stateMap = new StateMap()
    val learner = bridge.getLearner
    stateMap.populate(ResponseUtil.getEventStream(resp, learner))

    if (solution != null) {
      val patterns = Translator.taskSolutionToPatterns(solution, bridge, dispatcher, stateMap, symbols, ontology)
      var best: Option[Match] = None
      for (pattern <- patterns) {
        val bound = if (best == None) pattern.totalWeight + 1 else (best.get.cost / best.get.pattern.totalWeight) * pattern.totalWeight
        val result = PatternMatcher.computeMatch(pattern, candidate, ontology, bound)
        if (!result._1.isEmpty) {
          val poss = result._1.get
          if (best == None || (poss.cost / poss.pattern.totalWeight < best.get.cost / best.get.pattern.totalWeight)) {
            best = result._1
            
            // Set statistics for purposes of functional tests
            lastExecInfo = result._2
            lastMatch = result._1.get
          }
        }
      }
      
      println("Best pattern match across expansions: " + best.get)

      new TaskAssessmentResult(Translator.matchToTaskAssessment(bridge, best.get, symbols, stateMap), best.get, stateMap);
    } else {
      val assess = new TaskAssessment()
      val indexes = new AssessedResponseIndexes();
      for (atom <- candidate.primaryNodes)
        indexes.getIndexes().add(atom.asInstanceOf[AtomCandidateNode].getResponseIndex)
      assess.setResponseIndexes(indexes)
      new TaskAssessmentResult(assess, null, stateMap)
    }
  }

  def assessExercise(exercise: Exercise, response: Response) {

  }
}

class TaskAssessmentResult(val assessment: TaskAssessment, val mtch: Match, val stateMap: StateMap) 
