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

import java.io.PrintWriter
import com.sri.ai.patternmatcher.graph.Constraint
import com.sri.ai.patternmatcher.graph.PatternNode
import com.sri.ai.patternmatcher.graph.PatternEdge
import com.sri.ai.patternmatcher.graph.Pattern
import java.io.File
import java.io.FileWriter
import scala.collection.JavaConversions._
import scala.collection.mutable
import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.ai.patternmatcher.graph.Graph
import com.sri.ai.patternmatcher.graph.Node
import com.sri.ai.patternmatcher.graph.Edge

object GraphGrapher {
  /* A sequence generator. */
  var _nextId = 0
  private def nextId: Int = {
    _nextId += 1
    _nextId
  }

  /* A color "generator." */
  val colors = List("red", "blue", "gold", "green", "orange", "purple", "yellow")
  var _nextColor = 0
  private def nextColor: String = {
    val color = colors(_nextColor)
    _nextColor += 1
    color
  }

  /* Map from Node classes to colors used to represent them. */
  val colorMap = mutable.Map[Class[_], String]()

  def dotsFromPatterns(pats: Seq[Pattern],
    baseFile: File,
    prefix: String) = {
    val outs = scala.collection.mutable.Buffer[File]()
    var counter = 1
    
    pats.foreach { pat =>
      var out: PrintWriter = null
      try {
        val dotFile = new File(baseFile.getParent, prefix + "-" + counter + ".dot")
        out = new PrintWriter(new FileWriter(dotFile))
        out.println("digraph {")
        out.println("layout=sfdp")
        out.println("overlap=\"false\"")
        dotPattern(out, pat)
        out.println("}")
        out.close
        outs += dotFile
      } finally {
        if (out != null)
          out.close()
      }
      counter += 1
    }
   
    outs
  }

  def dotFromCandidate(cand: Candidate,
    baseFile: File): File = {
    val dotFile = new File(baseFile.getParent, baseFile.getName + ".dot")
    val out = new PrintWriter(new FileWriter(dotFile))
    try {
      out.println("digraph {")
      out.println("layout=sfdp")
      out.println("overlap=\"false\"")
      dotCandidate(out, cand)
      out.println("}")
    } finally {
      out.close
    }
    dotFile
  }

  private def dotPattern(out: PrintWriter,
    pat: Pattern): Unit = {
    out.println("subgraph {")

    /* Not only is this pattern its own subgraph, but it contains groups of
     * nodes and edges that are connected to each other only by constraints.
     * Each such group should be its own subgraph inside this one. */
    var allNodes = Set[PatternNode]() ++ pat.nodes
    var allEdges = Set[PatternEdge]() ++ pat.edges
    val constraints = pat.constraints

    while (!allNodes.isEmpty()) {
      out.println("subgraph {")

      /* Pick a node and build sets of all edges and nodes (transitively)
       * connected to it. */
      var theseNodes = Set[PatternNode]()
      var theseEdges = Set[PatternEdge]()
      def pickNode(node: PatternNode): Unit = {
        allNodes -= node
        theseNodes += node
        allEdges.filter { e =>
          e.source.equals(node) || e.destination.equals(node)
        }.foreach { e =>
          pickEdge(e)
        }
      }
      def pickEdge(edge: PatternEdge): Unit = {
        allEdges -= edge
        theseEdges += edge
        if (allNodes.contains(edge.source)) {
          pickNode(edge.source)
        }
        if (allNodes.contains(edge.destination)) {
          pickNode(edge.destination)
        }
      }
      val node = allNodes.head
      pickNode(node)

      /* Add those nodes and edges to the current subgraph. */
      theseNodes.foreach { n => dotNode(out, n) }
      theseEdges.foreach { e => dotEdge(out, e) }

      out.println("}")
    }

    /* Now that we've done all that, we can plot out the constraints which
     * connect those subgraphs. They belong in this higher-level subgraph. */
    constraints.foreach { c => dotConstraint(out, c) }

    out.println("}")
  }

  private def dotCandidate(out: PrintWriter,
    cand: Candidate): Unit = {
    out.println("subgraph {")

    cand.nodes.foreach { n => dotNode(out, n) }
    cand.edges.foreach { e => dotEdge(out, e) }

    out.println("}")
  }

  private def dotNode(out: PrintWriter,
    node: Node[_]): Unit = {
    val nodeClass = node.getClass
    var label = node.label
    
    if (node.isInstanceOf[PatternNode])
      label += " (" + node.asInstanceOf[PatternNode].weight + ")"
      
    val color = {
      if (!colorMap.contains(nodeClass)) {
        colorMap.put(nodeClass, nextColor)
      }
      colorMap(nodeClass)
    }
    out.println("\"%s\" [label=\"%s\", color=%s]".format(node.index, label,
      color))
  }

  private def dotEdge(out: PrintWriter,
    edge: Edge[Node[_]]): Unit = {
    var label = edge.label
    
    if (edge.isInstanceOf[PatternEdge])
      label += " (" + edge.asInstanceOf[PatternEdge].weight + ")"
      
    out.println("\"%s\" -> \"%s\" [label=\"%s\"]".format(edge.source.index,
      edge.destination.index, label))
  }

  private def dotConstraint(out: PrintWriter,
    con: Constraint): Unit = {
    val id = "constraint_%s".format(nextId)
    out.println("\"%s\" [label=\"%s\", shape=box]".format(id, con.label.replace("\"", "'") + " (" + con.weight + ")"))
    (0 until con.nodes.size).foreach { argNum =>
      val node = con.nodes(argNum)
      out.println("\"%s\" -> \"%s\" [label=\"%s\"]".format(id, node.index,
        argNum))
    }
  }

}