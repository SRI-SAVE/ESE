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

import collection.JavaConversions.asScalaSet
import collection.JavaConversions.asScalaBuffer
import collection.mutable.ArrayBuffer
import collection.mutable.Map
import com.sri.pal.ActionStreamEvent
import com.sri.pal.ActionDef
import com.sri.pal.common.TypeNameFactory

object Contains {
  val FUNCTOR = "contains"
  private val ADD_FUNCTOR = "CPOF^2.0^ADD"
  private val REM_FUNCTOR = "CPOF^2.0^REMOVE"
  private val ADD_NAME = TypeNameFactory.makeName(ADD_FUNCTOR);
  private val REM_NAME = TypeNameFactory.makeName(REM_FUNCTOR);
}

class Contains extends PredicateEvaluator {
  val map = Map[Object, Map[Object, PredicateResult]]()
  
  override def getFunctor = Contains.FUNCTOR
  
  override def evaluate(args: Seq[Object]) = {
    val container = args(0)
    val obj = args(1)
    if (map.contains(container)) {
      val objectMap = map.get(container).get
      objectMap.get(obj)
    } else
      None
  }
  
  override def update(event: ActionStreamEvent, serial: Long) {
    val actDef = event.getDefinition.asInstanceOf[ActionDef];
    for (fam <- actDef.getFamilies) {
      if (fam.equals(Contains.ADD_NAME) || fam.equals(Contains.REM_NAME)) {
        val adding = fam.equals(Contains.ADD_NAME)
                    
        val containerRole = if (adding) "DESTINATION" else "SOURCE"
        val objectRole = if (adding) "ADDED_ITEMS" else "REMOVED_ITEMS"

        var container : Object = null;
        var obj : Object = null;
        
        for (i <- 0 until actDef.numInputParams()) {
          if (actDef.getParamRoles(i, fam).contains(containerRole))
            container = event.getValue(i)
          if (actDef.getParamRoles(i, fam).contains(objectRole))
            obj = event.getValue(i).asInstanceOf[java.util.List[Object]].get(0);
        }
        
        if (!map.contains(container))
          map += ((container, Map[Object, PredicateResult]()))
          
        val objectMap = map.get(container).get

        if (!objectMap.contains(obj)) {
          val result = ((adding, ArrayBuffer(((adding, serial)))))
          objectMap += ((obj, result))
        } else {
          val prev = objectMap.get(obj).get
          objectMap -= obj
          val result = ((adding, prev._2 += ((adding, serial))))
          objectMap += ((obj, result))
        }        
      } 
    }    
  }
  
  override def getBlameMessage(args: java.util.List[Object], negated: Boolean) = {
    val cont = args(0)
    val obj = args(1)
    
    if (negated) 
      "You shouldn't add " + obj + " to " + cont
    else
      "You shouldn't remove " + obj + " from " + cont + ". Try a mirror instead.";
  }
  
  override def getErrorMessage(args: java.util.List[Object], negated: Boolean) = {
    val cont = args(0)
    val obj = args(1)
    
    if (negated)
      "You never removed " + obj + " from " + cont
    else
      "You never added " + obj + " to " + cont;      
  }
  
  override def toString = map.toString
}