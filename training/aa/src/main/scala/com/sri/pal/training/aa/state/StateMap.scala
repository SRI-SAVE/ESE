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

package com.sri.pal.training.aa.state
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.JavaConversions.asScalaBuffer
import com.sri.pal.ActionStreamEvent
import com.sri.pal.GestureStart
import com.sri.pal.GestureEnd
import org.slf4j.LoggerFactory

object StateMap {
  val log = LoggerFactory.getLogger(StateMap.getClass()); 
}

class StateMap {
  val predicates = List[PredicateEvaluator](new Contains())
  
  def populate(events: Seq[ActionStreamEvent]) {
    var gesture : GestureStart = null 
  
    for (event <- events) {
      // Skip over the GestureStarts/GestureEnds. For purposes of this
      // algorithm, gestures are not opaque.
      if (event.isInstanceOf[GestureStart]) {
        gesture = event.asInstanceOf[GestureStart];
      } else if (event.isInstanceOf[GestureEnd]) {
        gesture = null;
      } else {
        predicates.foreach(
            p => p.update(event, if (gesture != null) gesture.getSerialNumber else event.getSerialNumber))
      }
    }
  }
  
  def getPredicateEvaluator(functor: String) = {
    predicates.find(p => p.getFunctor.equals(functor))
  }
  
  def getPredicateEvaluator_Java(functor: String) = getPredicateEvaluator(functor).getOrElse(null)
    
  def evaluatePredicate(predicate: String, args: Seq[AnyRef]) = {
    val result = getPredicateResult(predicate, args)
    
    if (result == None)
      None
    else
      Some(result.get._1)        
  }
  
  def getPredicateResult(functor: String, args: Seq[AnyRef]) = {
    val pred = getPredicateEvaluator(functor);
    if (pred != None)    
      pred.get.evaluate(args)
    else
      None
  }
  
  def getInfluencingActions_Java(functor: String, args: java.util.List[AnyRef]) : java.util.List[Tuple2[Boolean, Long]] =  {
    val r = getPredicateResult(functor, args)
    
    if (r != None)
      r.get._2
    else
      null
  }
  
  override def toString = {
    var str = "";
    predicates.foreach(p => str += p.toString() + "\n")
    str
  }
}