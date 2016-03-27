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

import java.util.List
import scala.collection.JavaConversions.asScalaBuffer
import org.slf4j.LoggerFactory
import com.sri.ai.lumen.atr.ATRSyntax
import com.sri.ai.patternmatcher.graph.ConstraintOwnershipType._
import com.sri.ai.patternmatcher.graph.CandidateNode
import com.sri.ai.patternmatcher.graph.Constraint
import com.sri.pal.AbstractActionDef
import com.sri.pal.Bridge
import com.sri.pal.TypeDef
import com.sri.pal.common.SimpleTypeName
import com.sri.pal.common.TypeNameFactory
import com.sri.pal.training.aa.ArgCandidateNode
import com.sri.pal.training.aa.ParamPatternNode
import com.sri.pal.training.aa.QueryDispatcher
import com.sri.pal.training.aa.Translator
import com.sri.pal.training.core.exercise.QueryConstraint
import com.sri.pal.training.aa.SymbolManager
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.pal.training.core.util.ValueUtil

/**
 * A constraint that calls out to the client to execute a predicate,
 * and compares the result of that predicate to a given value.
 */

object QueryEvaluator {
  val log = LoggerFactory.getLogger(QueryEvaluator.getClass()); 
}

class QueryEvaluator(
  val predEval: QueryDispatcher,
  val constraint: QueryConstraint,
  name: String,
  nodes: List[ParamPatternNode],
  weight: Double,
  val bridge: Bridge,
  val symbols: SymbolManager)
  extends Constraint(name, nodes, weight) {
  val actionName = TypeNameFactory.makeName(constraint.getFunctor()).asInstanceOf[SimpleTypeName]
  val actionDef = bridge.getActionModel.getType(actionName).asInstanceOf[AbstractActionDef]
  val desiredResult = !constraint.isNegated()   

  override def evaluate(candidateNodes: List[CandidateNode], cand: Candidate): Boolean = {    
    val cNodes = candidateNodes.asInstanceOf[List[ArgCandidateNode]]
    var argNum = -1
    var nodeNum = 0
    var typeMismatch = false
    
    val args = constraint.getArguments().map { arg =>
      // Each argument is either a value or a reference
      argNum += 1
      if (arg.getValue() != null) {
        val value = arg.getValue
        val typeDef = ValueUtil.getTypeDef(value, bridge)

        typeDef.fromAtr(ATRSyntax.CTR.termFromSource(value.getCtrs));
      } else {
        val ref = arg.getRef;
        
        // A reference can either be a reference to a "constant" or to param node
        val sym = symbols.lookupReference(ref);
        if (sym != null) {
          ValueUtil.getObject(sym, bridge)
        } else {
          val node = cNodes.get(nodeNum)
          
          if (!node.getTypeDef.isAssignableTo(actionDef.getParamType(argNum)))
            typeMismatch = true
            
          val value = node.getValue()
          nodeNum += 1
          value
        }
      }
    }
    
    if (typeMismatch)
      return false;
    
    val output = predEval.evaluate(actionName, args: _*)
    desiredResult.equals(output)
  }
  
  override def isEquivalentTo(other: Constraint) : Boolean = {
    if (other.isInstanceOf[QueryEvaluator]) {
      val qOther = other.asInstanceOf[QueryEvaluator]
      if (actionName.equals(qOther.actionName) && desiredResult == qOther.desiredResult) {
        for (i <- 0 until actionDef.numInputParams()) {
          if (!constraint.getArguments.get(i).isEquivalentTo(qOther.constraint.getArguments.get(i)))
            return false;
        }
        return true;
      }
    }
    false
  }
  
  override def isOrderDependent = true
  override def operatesOn = classOf[ArgCandidateNode]
  
  // This could be smarter by insuring that the param arguments indeed
  // come from different atom nodes, in which case the constraint is not subordinate
  override def getOwnershipType = {
    if (nodes.size == 0)
      NO_OWNERS
    else if (nodes.size == 1)
      SINGLE_OWNER
    else
      MULTIPLE_OWNERS
  }
}