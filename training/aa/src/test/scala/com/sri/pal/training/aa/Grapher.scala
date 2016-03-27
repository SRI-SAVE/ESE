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

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import scala.sys.process.stringSeqToProcess
import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.GraphGrapher
import com.sri.pal.training.core.exercise.Atom
import com.sri.pal.training.core.exercise.EqualityConstraint
import com.sri.pal.training.core.exercise.Exercise
import com.sri.pal.training.core.exercise.OrderingConstraint
import com.sri.pal.training.core.exercise.Parameter
import com.sri.pal.training.core.exercise.QueryConstraint
import com.sri.pal.training.core.exercise.Step
import com.sri.pal.training.core.exercise.TaskSolution
import com.sri.pal.training.core.response.Response
import com.sri.pal.training.core.storage.ExerciseFactory
import com.sri.pal.Bridge
import javax.xml.bind.JAXBElement
import com.sri.pal.training.core.exercise.Task

/**
 * Support for building a graphviz (DOT) graph from an exercise or response XML
 * file.
 */
object Grapher {
  /**
   * Try to build the appropriate type of (PNG) graph from the provided XML
   * file. Based on the filename, this routine will try to interpret the file
   * as either an exercise or a response. It loads the MockCpof action model
   * in order to interpret the actions in the provided file; if the file uses
   * a different action model, this technique will fail to generate a graph.
   * @param args a list of XML files to draw graphs for
   */
  def main(args: Array[String]) {
    if (args.size == 0) {
      println("No files to process!")
    }

    Bridge.startPAL
    val bridge = Bridge.newInstance("Grapher")
    bridge.getActionModel.load(this.getClass.getResource("MockCpof.xml"), "LoadTest")
    val qd = new QueryDispatcher(bridge)

    args.foreach { arg =>
      val f = new File(arg)
      try {
        val baseFile = {
          val xmlName = f.getName
          val baseName = xmlName.split("\\.").apply(0)
          new File(f.getParent, baseName)
        }
        val unmarsh = ExerciseFactory.getUnmarshaller
        if (f.getName.contains("exercise")) {
          val exerEle = unmarsh.unmarshal(f).asInstanceOf[JAXBElement[Exercise]]
          val exercise = exerEle.getValue
          for (sol <- exercise.getSolution.getTaskSolutions) {
            val patterns = Translator.taskSolutionToPatterns(sol, bridge, qd, null, new SymbolManager(bridge), new ActionModelOntology())
            val prefix = baseFile.getName + "-" + sol.getTask.asInstanceOf[Task].getId
            graph(patterns, baseFile, prefix)
          }
        } else if (f.getName.contains("response")) {
          val respEle = unmarsh.unmarshal(f).asInstanceOf[JAXBElement[Response]]
          val response = respEle.getValue
          val candidate = Translator.taskResponseToCandidate(response.getTaskResponses.get(0), bridge)
          graph(candidate, baseFile)
        }
      } catch {
        case e: Exception =>
          throw new RuntimeException("Couldn't graph " + f, e)
      }
    }

    bridge.shutdown
  }

  /* A sequence generator. */
  var _nextId = 0
  private def nextId: Int = {
    _nextId += 1
    _nextId
  }

  /**
   * Draw a (PNG) graph from a set of (pattern matcher) pattern graphs. This
   * method will produce both a .dot file and, if graphviz is installed, a .png
   * file.
   * @param pats the set of patterns to graph
   * @param baseFile the base name of the file to write, without the .dot or
   * .png extension
   */
  def graph(pats: Seq[Pattern],
    baseFile: File,
    prefix: String): Unit = {
    val dotFiles = GraphGrapher.dotsFromPatterns(pats, baseFile, prefix)
    for (dotFile <- dotFiles) {
      try {
        val pngFile = imageFromDot(dotFile)
      } catch {
        case e: Exception =>
          System.err.println("Failed to build image from " + dotFile + ": " + e)
      }
    }
  }

  /**
   * Draw a (PNG) graph from a (pattern matcher) candidate graph. This method
   * will produce both a .dot file and, if graphviz is installed, a .png file.
   * @param cand the candidate to graph
   * @param baseFile the base name of the file to write, without the .dot or
   * .png extension
   */
  def graph(cand: Candidate,
    baseFile: File): Unit = {
    val dotFile = GraphGrapher.dotFromCandidate(cand, baseFile)
    try {
      val pngFile = imageFromDot(dotFile)
    } catch {
      case e: Exception =>
        System.err.println("Failed to build image from " + dotFile + ": " + e)
    }
  }

  /**
   * Calls graphviz (specifically, dot) to draw a PNG image.
   * @param dotFile the DOT file to read
   * @return a corresponding PNG file
   */
  def imageFromDot(dotFile: File): File = {
    val imgFile = {
      val dotName = dotFile.getName
      val imgName = dotName.split("\\.").apply(0) + ".png"
      new File(dotFile.getParent, imgName)
    }

    var cmd = Seq[String]()
    cmd = cmd :+ "dot"
    cmd = cmd :+ "-Tpng"
    cmd = cmd :+ "-o%s".format(imgFile.getPath)
    cmd = cmd :+ dotFile.getPath

    cmd.!

    imgFile
  }
}
