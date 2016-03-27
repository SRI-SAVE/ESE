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

import java.util.ArrayList
import java.util.List
import scala.collection.JavaConversions.collectionAsScalaIterable
import org.slf4j.LoggerFactory
import com.sri.ai.lumen.atr.ATRSyntax
import com.sri.ai.patternmatcher.graph.CandidateNode
import com.sri.ai.patternmatcher.graph.Constraint
import com.sri.ai.patternmatcher.graph.ConstraintOwnershipType
import com.sri.ai.patternmatcher.graph.PatternNode
import com.sri.pal.common.TypeNameFactory
import com.sri.pal.training.aa.ArgCandidateNode
import com.sri.pal.training.aa.ParamPatternNode
import com.sri.pal.training.aa.SymbolManager
import com.sri.pal.training.core.exercise.ValueConstraint
import com.sri.pal.Bridge
import com.sri.pal.TypeDef
import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.pal.training.core.util.ValueUtil

object ValueEvaluator {
  val log = LoggerFactory.getLogger(ValueEvaluator.getClass()); 
}

class ValueEvaluator(
  val constraint: ValueConstraint,
  val name: String,
  val pnode: ParamPatternNode,
  weight: Double,
  val bridge: Bridge,
  val symbols: SymbolManager) extends Constraint(name, {val list = new ArrayList[PatternNode]; list.add(pnode); list}, weight) {

  val negated = constraint.isNegated()
  val typeName = if (constraint.getValues.size > 0) 
    constraint.getValues.get(0).getType 
  else {
    // Translator is supposed to have insured that at least one of the refs is mapped if there
    // were no literal values on this constraint. Otherwise, this constraint wouldn't be getting
    // constructed. 
    val opt = constraint.getRefs.find(ref => symbols.lookupReference(ref) != null)
    symbols.lookupReference(opt.get).getType
  }
  val typeDef = bridge.getActionModel.getType(TypeNameFactory.makeName(typeName)).asInstanceOf[TypeDef]
  val expectedAtrValues = {
    for (i <- 0 until constraint.getValues.size)
      yield ATRSyntax.CTR.termFromSource(constraint.getValues.get(i).getCtrs)
  }
  val namedValues = constraint.getRefs.filter(key => symbols.lookupReference(key) != null).map(key => ValueUtil.getObject(symbols.lookupReference(key), bridge))

  override def evaluate(candidateNodes: List[CandidateNode], cand: Candidate): Boolean = {
    val cnode = candidateNodes.get(0).asInstanceOf[ArgCandidateNode]
    val actualValue = cnode.getValue()
    
    if (!typeDef.isAssignableTo(cnode.getTypeDef) && !cnode.getTypeDef.isAssignableTo(typeDef))
      return false; 

    val objects = expectedAtrValues.map(typeDef.fromAtr(_)) ++ namedValues
    val found = objects.contains(actualValue)
    found.equals(!negated)
  }
  
  override def isEquivalentTo(other: Constraint) :Boolean = {
    if (other.isInstanceOf[ValueEvaluator]) {
      val veOther = other.asInstanceOf[ValueEvaluator]
      // Convert to sets because we don't care if the values aren't in the same
      // order. 
      if (negated.equals(veOther.negated) && 
          expectedAtrValues.toSet.equals(veOther.expectedAtrValues.toSet) &&
          namedValues.toSet.equals(veOther.namedValues.toSet)) {
        return true
      }
    }
    false
  }
  
  override def isOrderDependent = false
  
  override def operatesOn = classOf[ArgCandidateNode]
  
  override def getOwnershipType = ConstraintOwnershipType.SINGLE_OWNER
}
