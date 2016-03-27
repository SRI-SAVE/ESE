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

import org.testng.annotations.Test
import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.ai.patternmatcher.graph.CandidateNode
import com.sri.ai.patternmatcher.graph.PrimaryCandidateNode
import com.sri.pal.Bridge
import com.sri.pal.upgrader.MemoryTypeStorage
import com.sri.pal.ActionModel
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import com.sri.pal.training.aa.LoadTest
import com.sri.pal.training.aa.LoadTest
import com.sri.pal.common.SimpleTypeName
import com.sri.pal.training.aa.AtomPatternNode
import com.sri.pal.AbstractActionDef
import org.testng.Assert
import scala.collection.JavaConversions._
import scala.collection.JavaConversions
import com.sri.pal.training.aa.AtomCandidateNode
import com.sri.pal.training.aa.func.EndToEnd
import com.sri.pal.training.core.exercise.Exercise
import com.sri.pal.training.core.response.Response
import javax.xml.bind.Unmarshaller
import com.sri.pal.training.core.storage.ExerciseFactory
import javax.xml.bind.JAXBElement
import java.io.File
import com.sri.pal.common.TypeNameFactory
import com.sri.pal.training.core.util.ResponseUtil

/**
 * Tests for the ordering constraint evaluator. It handles relative ordering of,
 * two actions, first action, and last action.
 */
object OrderingEvaluatorTest {
  var bridge: Bridge = null
  var pnodes = Seq[AtomPatternNode]()
  var cnodes = Seq[CandidateNode]()
  var cand: Candidate = null

  @BeforeClass
  def setup(): Unit = {
    /* Initialize the Bridge. */
    Bridge.startPAL()
    bridge = Bridge.newInstance(getClass().getSimpleName())
    bridge.setTypeStorage(new MemoryTypeStorage())

    /* Load the action model. */
    val actionModel = bridge.getActionModel()
    val vers = "1.0"
    val amUrl = classOf[EndToEnd].getResource("/com/sri/tasklearning/novo/adept/novo.xml")
    val ns = "novo"
    actionModel.load(amUrl, ns)

    /* Load the exercise. */
    val exerciseUrl = classOf[EndToEnd].getResource("aikido-exer.xml")
    val um = ExerciseFactory.getUnmarshaller()
    val exeEle = um.unmarshal(new File(exerciseUrl.toURI()))
        .asInstanceOf[JAXBElement[Exercise]]
    val exercise = exeEle.getValue();
    /* Load the response. */
    val responseUrl = classOf[EndToEnd].getResource("aikido-resp.xml")
    val respEle = um.unmarshal(new File(responseUrl.toURI()))
        .asInstanceOf[JAXBElement[Response]]
    val response = respEle.getValue()

    /* Build pattern nodes. */
    val steps = exercise.getSolution().getTaskSolutions().get(0).getOption().getSteps()
    for(step <- steps) {
      val atom = step.getAtom()
      val functor = atom.getFunctor()
      val name = TypeNameFactory.makeName(functor).asInstanceOf[SimpleTypeName]
      val actDef = actionModel.getSimpleType(name).asInstanceOf[AbstractActionDef]
      val pnode = new AtomPatternNode(step, functor, 1, actDef)
      pnodes :+= pnode
    }

    /* Build candidate nodes. */
    val actions = ResponseUtil.getEventStream(response.getTaskResponses().get(0), bridge.getLearner())
    for ((action, index) <- actions.zipWithIndex) {
      val actDef = action.getDefinition()
      val name = actDef. getName()
      val cnode = new AtomCandidateNode(name.getFullName(), action, index)
      cnodes :+= cnode
    }

    cand = new Candidate(cnodes)
  }

  @AfterClass
  def shutdown(): Unit = {
    bridge.shutdown()
  }
}

class OrderingEvaluatorTest {
  @Test
  def inOrderGood(): Unit = {
    val o = OrderingEvaluatorTest
    val args = Seq(o.cnodes(1), o.cnodes(2))
    val oe = new OrderingEvaluator("eval", o.pnodes(1), o.pnodes(2), 1)
    Assert.assertTrue(oe.evaluate(args, o.cand))
  }

  @Test
  def inOrderBad(): Unit = {
    val o = OrderingEvaluatorTest
    val args = Seq(o.cnodes(2), o.cnodes(1))
    val oe = new OrderingEvaluator("eval", o.pnodes(1), o.pnodes(2), 1)
    Assert.assertFalse(oe.evaluate(args, o.cand))
  }

  @Test
  def firstGood(): Unit = {
    val o = OrderingEvaluatorTest
    val args = Seq(o.cnodes(0))
    val oe = new OrderingEvaluator("eval", o.pnodes(0), null, 1)
    Assert.assertTrue(oe.evaluate(args, o.cand))
  }

  @Test
  def firstBad(): Unit = {
    val o = OrderingEvaluatorTest
    val args = Seq(o.cnodes(2))
    val oe = new OrderingEvaluator("eval", o.pnodes(0), null, 1)
    Assert.assertFalse(oe.evaluate(args, o.cand))
  }

  @Test
  def lastGood(): Unit = {
    val o = OrderingEvaluatorTest
    val args = Seq(o.cnodes.last)
    val oe = new OrderingEvaluator("eval", null, o.pnodes.last, 1)
    Assert.assertTrue(oe.evaluate(args, o.cand))
  }

  @Test
  def lastBad(): Unit = {
    val o = OrderingEvaluatorTest
    val args = Seq(o.cnodes(2))
    val oe = new OrderingEvaluator("eval", null, o.pnodes.last, 1)
    Assert.assertFalse(oe.evaluate(args, o.cand))
  }
}
