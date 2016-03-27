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
import java.util.UUID

/**
 * Represents a directed graph. Note that you must connect edges via 
 * Graph.connect() before constructing a graph because it creates helper
 * data structures when its constructed. 
 */
abstract class Graph[E <: Edge[_], N <: Node[E]](val nodes : Seq[N]) {
  val edges : ArrayBuffer[E] = {
    var s = ArrayBuffer[E]()
    nodes.foreach(n => s ++= n.incoming_edges)
    s
  }
  
  var counter = 0
  nodes.foreach(n => {n.index = counter; counter += 1})
  
  override def toString = {
    var ret = this.getClass.getSimpleName + ":\n"
    nodes.foreach(n => ret += n.toString + "\n") 
    edges.foreach(e => ret += e.toString + "\n")
    ret
  } 
}

/**
 * Companion object for Graph that provides basic functionality for connecting
 * edges in the graph.  
 */
object Graph {
  def connect[E <: Edge[_], N <: Node[E]](edge : E, source : N, destination : N) = {
    source.outgoing_edges += edge
    destination.incoming_edges += edge
  }  
}

/**
 * Represents a directed edge in a graph. Edge and Node are declared using 
 * generics to support different types of graphs (eg. patterns and candidates) 
 * while still enforcing appropriate relationships between Edges and Nodes.  
 */
trait Edge[+N <: Node[_]] {
  val label: String
  val source: N
  val destination: N
  
  override def toString = source + "--" + label + "-->" + destination 
}

/**
 * Represents a Node in a graph. Contains references to incoming and outgoing
 * edges.
 */ 
trait Node[E <: Edge[_]] {
  val label: String
  val incoming_edges: ArrayBuffer[E]
  val outgoing_edges: ArrayBuffer[E]
  var index = 0
  
  override def toString = "(" + label + ")"
}