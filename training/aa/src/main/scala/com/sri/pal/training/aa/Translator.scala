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

import java.util.ArrayList
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.setAsJavaSet
import scala.collection.JavaConversions.asJavaCollection
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.LinkedHashMap
import com.sri.ai.lumen.runtime.FirstFunOp
import com.sri.ai.lumen.runtime.LastFunOp
import com.sri.ai.lumen.runtime.OnlyFunOp
import com.sri.ai.patternmatcher.graph.Candidate
import com.sri.ai.patternmatcher.graph.CandidateNode
import com.sri.ai.patternmatcher.graph.Constraint
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.graph.PatternNode
import com.sri.ai.patternmatcher.Match
import com.sri.ai.patternmatcher.Ontology
import com.sri.pal.common.TypeNameFactory
import com.sri.pal.training.aa.constrainteval.EqualityEvaluator
import com.sri.pal.training.aa.constrainteval.OrderingEvaluator
import com.sri.pal.training.aa.constrainteval.QueryEvaluator
import com.sri.pal.training.aa.constrainteval.ValueEvaluator
import com.sri.pal.training.core.assessment.Assessment
import com.sri.pal.training.core.exercise.EqualityConstraint
import com.sri.pal.training.core.exercise.Option
import com.sri.pal.training.core.exercise.OptionSet
import com.sri.pal.training.core.exercise.Parameter
import com.sri.pal.training.core.exercise.QueryConstraint
import com.sri.pal.training.core.exercise.Step
import com.sri.pal.training.core.exercise.TaskSolution
import com.sri.pal.training.core.response.Response
import com.sri.pal.AbstractActionDef
import com.sri.pal.Bridge
import com.sri.pal.CollectionTypeDef
import com.sri.pal.GestureEnd
import com.sri.pal.GestureStart
import com.sri.pal.ListDef
import com.sri.pal.Struct
import com.sri.pal.StructDef
import com.sri.pal.TypeDef
import com.sri.pal.training.core.response.TaskResponse
import com.sri.ai.patternmatcher.NodeImpactOrdering
import com.sri.pal.training.core.assessment.TaskAssessment
import com.sri.pal.training.core.assessment.MissingAtomIssue
import com.sri.pal.training.core.assessment.ExtraAtomsIssue
import com.sri.pal.training.aa.constrainteval.EqualityEvaluator
import com.sri.pal.training.aa.constrainteval.EqualityEvaluator
import com.sri.pal.training.aa.constrainteval.EqualityEvaluator
import com.sri.pal.training.core.assessment.EqualityIssue
import com.sri.pal.training.core.assessment.OrderingIssue
import com.sri.pal.training.core.assessment.QueryIssue
import com.sri.pal.training.core.assessment.ValueIssue
import com.sri.pal.training.core.assessment.ArgumentLocation
import com.sri.pal.training.core.exercise.ValueConstraint
import com.sri.pal.training.core.exercise.OrderingConstraint
import com.sri.pal.ListDef
import com.sri.pal.StructDef
import com.sri.pal.training.core.assessment.AssessedResponseIndexes
import com.sri.pal.CollectionTypeDef
import com.sri.pal.ActionStreamEvent
import com.sri.pal.IdiomDef
import com.sri.ai.patternmatcher.NodeImpactOrdering
import com.sri.ai.patternmatcher.graph.Constraint
import com.sri.ai.patternmatcher.Match
import com.sri.ai.patternmatcher.graph.Pattern
import com.sri.ai.patternmatcher.NodeImpactOrdering
import com.sri.ai.patternmatcher.graph.PrimaryPatternNode
import com.sri.pal.training.core.exercise.Value
import com.sri.pal.training.core.exercise.StateConstraint
import com.sri.pal.training.aa.state.StateMap
import com.sri.pal.training.aa.constrainteval.StateEvaluator
import com.sri.pal.training.core.assessment.StateIssue
import com.sri.pal.PALActionMissingException
import com.sri.pal.training.core.util.ValueUtil
import com.sri.pal.training.core.util.ResponseUtil

object Translator {
  val PARAM_NODE_WEIGHT = 10
  val ATOM_NODE_WEIGHT = 20
  val PARAM_EDGE_WEIGHT = 10
  val ORDERING_CONSTRAINT_WEIGHT = 20
  val VALUE_CONSTRAINT_WEIGHT = 25
  val EQUALITY_CONSTRAINT_WEIGHT = 20
  val QUERY_CONSTRAINT_WEIGHT = 20
  val STATE_CONSTRAINT_WEIGHT = 20  
  val SET_AND_BAG_ACCESSOR = "contains"
  val ONLY = OnlyFunOp.NAME.toString
  val FIRST = FirstFunOp.NAME.toString
  val LAST = LastFunOp.NAME.toString

  //--------------------- RESPONSE TRANSLATION ---------------------------------

  def taskResponseToCandidate(response: TaskResponse, bridge: Bridge) = {
    val nodes = scala.collection.mutable.ArrayBuffer[CandidateNode]()
    val learner = bridge.getLearner
    val allActions = ResponseUtil.getEventStream(response, learner)

    var invocations = Seq[ActionStreamEvent]() ++ allActions
    var index = 0
    // The recognize idiom code insists that only one gesture start/end block be
    // sent to it at a time, with no extra actions outside the block. Hence the
    // gymnastics below. 
    while (index < allActions.size) {
      val act = allActions(index)
      if (act.isInstanceOf[GestureStart]) {
        var end = index
        while (!allActions(end).isInstanceOf[GestureEnd])
          end += 1

        val unrecs = invocations.slice(index, end + 1)
        val recs = learner.recognizeIdiom(unrecs: _*)
        
        for (i <- 0 until unrecs.size)
          recs(i).setSerialNumber(unrecs(i).getSerialNumber)
                
        invocations = invocations.slice(0, math.max(0, index)) ++ recs ++ invocations.slice(end + 1, invocations.size)
        index = end + 1
      } else
        index += 1
    }

    var atomCount = 0;

    var processingIdiom = false;

    for (invocation <- invocations) {
      if (invocation.getDefinition != null && !invocation.getDefinition.isBenign) {
        if (invocation.isInstanceOf[GestureStart] ||
          (!processingIdiom && !invocation.isInstanceOf[GestureEnd])) {
          val actdef = invocation.getDefinition();
          val atom = new AtomCandidateNode(
            atomCount + " " + actdef.getName().getSimpleName(),
            invocation,
            invocations.indexOf(invocation))
          val args = for (i <- 0 until actdef.size) yield invocation.getValue(i)

          var i = 0;
          for (arg <- args) {
            val typedef =
              if (invocation.isInstanceOf[GestureStart])
                invocation.asInstanceOf[GestureStart].getParamType(i)
              else
                actdef.getParamType(i)
            if (typedef == null) {
              throw new RuntimeException("Can't get type of arg " + i + " of " + actdef)
            }

            val argNode = new ArgCandidateNode(if (arg == null) "null" else arg.toString, typedef, arg)

            if (i >= actdef.numInputParams)
              Candidate.connectNodes(actdef.getParamName(i), atom, argNode)
            else
              Candidate.connectNodes(actdef.getParamName(i), argNode, atom)

            nodes += argNode

            explodeArgument(argNode, typedef, arg, nodes)

            i += 1
          }

          nodes += atom;
          atomCount += 1;

          if (invocation.isInstanceOf[GestureStart])
            processingIdiom = true

        } else if (invocation.isInstanceOf[GestureEnd])
          processingIdiom = false
      }
    }

    new Candidate(nodes)
  }

  private def explodeArgument(
    node: ArgCandidateNode,
    typedef: TypeDef,
    arg: Object,
    nodes: collection.mutable.ArrayBuffer[CandidateNode]) {

    typedef match {
      case structdef: StructDef => {
        val struct = arg.asInstanceOf[Struct]
        for (i <- 0 until structdef.size) {
          val fieldType = structdef.getFieldType(i)
          val fieldValue = struct.getValue(i)

          if (fieldValue != null) {
            val argNode = new ArgCandidateNode(fieldValue.toString, fieldType, fieldValue)

            Candidate.connectNodes(structdef.getFieldName(i), node, argNode)
            nodes += argNode

            explodeArgument(argNode, fieldType, fieldValue, nodes)
          }
        }
      }
      case listdef: ListDef => {
        val list = arg.asInstanceOf[java.util.List[Object]]
        val elementType = listdef.getElementType()
        for (i <- 0 until list.size) {
          val value = list.get(i)
          val argNode = new ArgCandidateNode(value.toString, elementType, value)

          Candidate.connectNodes(i.toString, node, argNode)

          if (i == 0)
            Candidate.connectNodes(FIRST, node, argNode)
          if (i == list.size - 1)
            Candidate.connectNodes(LAST, node, argNode)
          if (list.size == 1)
            Candidate.connectNodes(ONLY, node, argNode)

          nodes += argNode

          explodeArgument(argNode, elementType, value, nodes)
        }
      }
      case colldef: CollectionTypeDef => {
        // Sets n' Bags
        val coll = arg.asInstanceOf[java.util.Collection[Object]]
        val elementType = colldef.getElementType()
        for (element <- coll) {
          val argNode = new ArgCandidateNode(element.toString, elementType, element)

          Candidate.connectNodes(SET_AND_BAG_ACCESSOR, node, argNode)
          nodes += argNode

          explodeArgument(argNode, elementType, element, nodes)
        }
      }
      case _ =>
    }
  }

  //--------------------- SOLUTION TRANSLATION ---------------------------------  

  def taskSolutionToPatterns(
    solution: TaskSolution,
    bridge: Bridge,
    dispatcher: QueryDispatcher,
    stateMap: StateMap,
    symbols: SymbolManager,
    ontology: ActionModelOntology) = {

    val expansions = expandTaskSolutionAux(solution.getOption, Set[Expansion]())

    val patterns = ArrayBuffer[Pattern]()
    if (expansions.size > 0) {
      expansions.foreach(expansion => patterns += expandedSolutionToPattern(solution, expansion, bridge, dispatcher, stateMap, symbols, ontology))
    } else
      patterns += expandedSolutionToPattern(solution, null, bridge, dispatcher, stateMap, symbols, ontology)

    patterns
  }

  type Expansion = List[Int]

  private def expandTaskSolutionAux(option: Option, expansions: Set[Expansion]): Set[Expansion] = {
    var newExpansions = expansions
    for (step <- option.getSteps()) {
      if (step.isOptionSet()) {
        val newExps = new ArrayBuffer[List[Int]]
        for (i <- 0 until step.getOptionSet.getOptions.size) {
          val expandeds = if (newExpansions.size > 0) newExpansions.map(exp => exp :+ i) else Set[Expansion]() + List[Int](i)
          newExps ++= expandTaskSolutionAux(step.getOptionSet.getOptions.get(i), expandeds)
        }
        newExpansions = newExps.toSet
      }
    }
    newExpansions
  }

  private def expandedSolutionToPattern(
    solution: TaskSolution,
    expansion: Expansion,
    bridge: Bridge,
    dispatcher: QueryDispatcher,
    stateMap: StateMap,    
    symbols: SymbolManager,
    ontology: Ontology) = {

    val pattern = ((
      new LinkedHashMap[String, AtomPatternNode],
      new LinkedHashMap[String, ParamPatternNode],
      collection.mutable.ArrayBuffer[Constraint]()))
    expandedSolutionToPatternAux(expansion, pattern, solution.getOption, bridge, dispatcher, stateMap, symbols)

    // partial pattern is now complete
    val p = new Pattern(pattern._1, pattern._2, pattern._3)
    val inferred = inferOrderingConstraints(pattern._1.values)
    p.addConstraints(inferred)

    var atoms = pattern._1.values.map(n => ontology.getSemanticIdentifier(n))
    var atomsArr = ArrayBuffer[String]() ++= atoms
    atomsArr = atomsArr.distinct
    
    // Equalize weights of nodes of the same atom
    for (atom <- atomsArr) {
      val matchingNodes = ArrayBuffer[AtomPatternNode]()
      for (n <- pattern._1.values)
        if (ontology.getSemanticIdentifier(n).equals(atom))
          matchingNodes += n

      val sortedNodes = matchingNodes.sorted(NodeImpactOrdering)
      val highest = sortedNodes(0).maxDeleteCost

      for (n <- sortedNodes) {
        n.weight = n.weight + (highest - n.maxDeleteCost)
        n.init
      }
    }

    p
  }

  private def inferOrderingConstraints(atoms: Iterable[AtomPatternNode]) = {
    var orderConstraints = ArrayBuffer[(AtomPatternNode, AtomPatternNode)]()
    for (atom <- atoms) {
      val seq = ArrayBuffer[AtomPatternNode]() += atom
      inferOrderingConstraintsAux(seq, orderConstraints)
    }
    // TODO verify this removes duplicates as expected
    orderConstraints = orderConstraints.distinct
    orderConstraints.map(p => {
      new OrderingEvaluator(p._1.label + " precedes " + p._2.label, p._1, p._2, ORDERING_CONSTRAINT_WEIGHT, true)
    })
  }

  private def inferOrderingConstraintsAux(
    chain: Seq[AtomPatternNode],
    inferences: ArrayBuffer[(AtomPatternNode, AtomPatternNode)]) {

    if (chain.size >= 3) {
      val last = chain.last
      chain.dropRight(2).foreach(pred => inferences += ((pred, last)))
    }

    var supportedAtoms = ArrayBuffer[AtomPatternNode]()
    chain.last.outgoing_edges.foreach(e => { supportedAtoms ++= findSupportedAtoms(e.destination.asInstanceOf[ParamPatternNode]) })
    supportedAtoms.distinct.foreach(atom => { inferOrderingConstraintsAux(chain :+ atom, inferences) })
  }

  private def findSupportedAtoms(output: ParamPatternNode): Iterable[AtomPatternNode] = {
    val supported = ArrayBuffer[AtomPatternNode]()
    for (constraint <- output.immediateConstraints) {
      if (constraint.isInstanceOf[EqualityEvaluator] &&
        constraint.asInstanceOf[EqualityEvaluator].support) {
        val eq = constraint.asInstanceOf[EqualityEvaluator]
        var far: PatternNode = if (eq.node1 == output) eq.node2 else eq.node1
        while (!(far.isInstanceOf[AtomPatternNode])) {
          if (far.incoming_edges.size > 0)
            far = far.incoming_edges(0).source
          else
            far = far.outgoing_edges.find(e => e.destination.isInstanceOf[AtomPatternNode]).map(e => e.destination).get
        }

        supported += far.asInstanceOf[AtomPatternNode]
      }
    }

    for (e <- output.outgoing_edges)
      supported ++= findSupportedAtoms(e.destination.asInstanceOf[ParamPatternNode])

    supported
  }

  type PartialPattern = Tuple3[LinkedHashMap[String, AtomPatternNode], LinkedHashMap[String, ParamPatternNode], collection.mutable.ArrayBuffer[Constraint]]

  private def expandedSolutionToPatternAux(
    expansion: Expansion,
    pattern: PartialPattern,
    option: Option,
    bridge: Bridge,
    dispatcher: QueryDispatcher,
    stateMap: StateMap, 
    symbols: SymbolManager) {
    
    var shortenedExpansion = expansion

    for (step <- option.getSteps()) {
      if (step.isAtom()) {
        val atom = step.getAtom();
        val actName = TypeNameFactory.makeName(atom.getFunctor())
        val actdef = bridge.getActionModel().getType(actName).asInstanceOf[AbstractActionDef]
        if (actdef == null) {
          throw new PALActionMissingException(actName)
        }
        val atomId = step.getId()
        val atomLabel = atomId + ": " + actdef.getName().getSimpleName()
        val atomNode = new AtomPatternNode(step, atomLabel, ATOM_NODE_WEIGHT, actdef)

        if (!pattern._1.isEmpty) {
          val previous = pattern._1.last
          previous._2.setNext(atomNode)
          atomNode.setPrevious(previous._2)
        }

        pattern._1 += ((atomId, atomNode))

        for (param <- atom.getParameters) {
          val id = param.getId()
          val idx = actdef.getParamNum(param.getAccessor)
          if (idx == -1) {
            throw new RuntimeException("Can't get parameter " + param.getAccessor
              + " of atom " + actdef + " (step " + step.getId() + ")")
          }
          val chain = Vector[String](param.getAccessor())
          if (!pattern._2.contains(id)) {
            pattern._2 += ((id, new ParamPatternNode(atomNode, id, PARAM_NODE_WEIGHT, actdef.getParamType(idx), chain)))
          }

          if (idx >= actdef.numInputParams)
            Pattern.connectNodes(param.getAccessor(), atomNode, pattern._2(id), PARAM_EDGE_WEIGHT)
          else
            Pattern.connectNodes(param.getAccessor(), pattern._2(id), atomNode, PARAM_EDGE_WEIGHT)

          explodeParameter(atomNode, param, pattern._2(id), pattern, actdef.getParamType(idx), chain)
        }
      } else {
        val set = step.getOptionSet
        val option = set.getOptions.get(shortenedExpansion.head)
        shortenedExpansion = shortenedExpansion.drop(1)
        expandedSolutionToPatternAux(shortenedExpansion, pattern, option, bridge, dispatcher, stateMap, symbols)
      }
    }

    processOrderingConstraints(option.getOrderingConstraints, pattern)
    processQueryConstraints(option.getQueryConstraints, pattern, dispatcher, bridge, symbols)
    processStateConstraints(option.getStateConstraints, pattern, stateMap, bridge, symbols)
    processEqualityConstraints(option.getEqualityConstraints, pattern)
    processValueConstraints(option.getValueConstraints, pattern, bridge, symbols)
  }

  private def explodeParameter(
    atom: AtomPatternNode,
    parm: Parameter,
    node: ParamPatternNode,
    pattern: PartialPattern,
    typedef: TypeDef,
    chain: Vector[String]) {

    for (sub <- parm.getSubParameters) {
      val newChain = chain :+ sub.getAccessor
      typedef match {
        case structdef: StructDef => {
          val subIdx = structdef.getFieldNum(sub.getAccessor)
          val subType = structdef.getFieldType(subIdx)
          val subNode = new ParamPatternNode(atom, sub.getId, PARAM_NODE_WEIGHT, subType, newChain)

          Pattern.connectNodes(sub.getAccessor, node, subNode, PARAM_EDGE_WEIGHT)
          pattern._2 += ((sub.getId, subNode))
          explodeParameter(atom, sub, subNode, pattern, subType, newChain)
        }
        case colldef: CollectionTypeDef => {
          val subType = colldef.getElementType
          val subNode = new ParamPatternNode(atom, sub.getId, PARAM_NODE_WEIGHT, subType, newChain)

          Pattern.connectNodes(sub.getAccessor, node, subNode, PARAM_EDGE_WEIGHT)
          pattern._2 += ((sub.getId, subNode))

          explodeParameter(atom, sub, subNode, pattern, subType, newChain)
        }
        case null => {
          // This happens on idioms. We have to fly blind in this case because we don't
          // know anything about the type
          val subNode = new ParamPatternNode(atom, sub.getId, PARAM_NODE_WEIGHT, null, newChain)
          Pattern.connectNodes(sub.getAccessor, node, subNode, PARAM_EDGE_WEIGHT)
          pattern._2 += ((sub.getId, subNode))

          explodeParameter(atom, sub, subNode, pattern, null, newChain)
        }
        case _ =>
      }
    }
  }

  private def processQueryConstraints(
    constraints: java.util.List[QueryConstraint],
    pattern: PartialPattern,
    dispatcher: QueryDispatcher,
    bridge: Bridge,
    symbols: SymbolManager) {
    var unsatisfiable = false
    for (constraint <- constraints) {
      val list = new ArrayList[ParamPatternNode]()
      for (arg <- constraint.getArguments) {
        if (arg.getRef != null) {
          // If it's not in the pattern then it's a constant reference
          if (pattern._2.contains(arg.getRef))
            list.add(pattern._2(arg.getRef))
          else if (symbols.lookupReference(arg.getRef) == null) {
            unsatisfiable = true
          }
        }
      }

      if (!unsatisfiable)
        pattern._3 += new QueryEvaluator(dispatcher,
          constraint,
          constraint.getFunctor,
          list, QUERY_CONSTRAINT_WEIGHT, bridge, symbols)
    }
  }
  
  private def processStateConstraints(
    constraints: java.util.List[StateConstraint],
    pattern: PartialPattern,
    stateMap: StateMap,
    bridge: Bridge,
    symbols: SymbolManager) {
    var unsatisfiable = false
    for (constraint <- constraints) {
      val list = new ArrayList[ParamPatternNode]()
      for (arg <- constraint.getArguments) {
        if (arg.getRef != null) {
          // If it's not in the pattern then it's a constant reference
          if (pattern._2.contains(arg.getRef))
            list.add(pattern._2(arg.getRef))
          else if (symbols.lookupReference(arg.getRef) == null) {
            unsatisfiable = true
          }
        }
      }

      if (!unsatisfiable)
        pattern._3 += new StateEvaluator(stateMap,
          constraint,
          constraint.getFunctor,
          list, STATE_CONSTRAINT_WEIGHT, bridge, symbols)
    }
  }  

  private def processEqualityConstraints(
    constraints: java.util.List[EqualityConstraint],
    pattern: PartialPattern) {
    for (constraint <- constraints) {
      val anchor = pattern._2(constraint.getParameters.get(0))
      for (i <- 1 until constraint.getParameters.size) {
        val list = new ArrayList[ParamPatternNode]()
        val node = pattern._2(constraint.getParameters.get(i))
        pattern._3 += new EqualityEvaluator(
          node.label + " equals " + anchor.label, anchor, node, constraint,
          EQUALITY_CONSTRAINT_WEIGHT, constraint.isSupport())
      }
    }
  }

  private def processOrderingConstraints(
    constraints: java.util.List[OrderingConstraint],
    pattern: PartialPattern) {
    for (constraint <- constraints) {     
      val pred = if (constraint.getPredecessor != null) pattern._1(constraint.getPredecessor) else null
      val succ = if (constraint.getSuccessor   != null) pattern._1(constraint.getSuccessor)   else null
      val label = 
        if (pred == null)
          succ.label + " succeeds all"
        else if (succ == null)
          pred.label + " preceeds all"
        else
          pred.label + " precedes " + succ.label
        
      pattern._3 +=
        new OrderingEvaluator(label, pred, succ, ORDERING_CONSTRAINT_WEIGHT)
    }
  }

  private def processValueConstraints(
    constraints: java.util.List[ValueConstraint],
    pattern: PartialPattern,
    bridge: Bridge,
    symbols: SymbolManager) {

    for (constraint <- constraints) {
      val node = pattern._2(constraint.getParameter)
      val vals = for (i <- 0 until constraint.getValues.size) yield constraint.getValues.get(i)
      val unsatisfiable = {
        if (constraint.getValues.size > 0)
          false
        else {
          val f = constraint.getRefs.find(ref => symbols.lookupReference(ref) != null)
          if (f.isEmpty) true else false
        } 
      }
      
      if (!unsatisfiable) {
        val valStr = if (vals.size == 1) vals.head.toString else vals.toString 
        pattern._3 += new ValueEvaluator(
          constraint,
          constraint.getParameter + { if (constraint.isNegated) "not" else "" } + " equals " + valStr,
          node, VALUE_CONSTRAINT_WEIGHT, bridge, symbols)
      }
    }
  }

  //--------------------- ASSESSMENT TRANSLATION -------------------------------

  def matchToTaskAssessment(
      bridge: Bridge, 
      orig: Match,
      symbols: SymbolManager,
      stateMap: StateMap) : TaskAssessment = {
    val assess = new TaskAssessment()
    val scratch = orig.copy()
    val ignoredViolations = Set[Constraint]()
    val candidateAtoms = orig.candidate.nodes
      .filter(_.isInstanceOf[AtomCandidateNode])
      .map(_.asInstanceOf[AtomCandidateNode])

    val indexes = new AssessedResponseIndexes();
    val serialMap = collection.mutable.Map[Long, AtomCandidateNode]()
    for (atom <- candidateAtoms) {
      indexes.getIndexes().add(atom.getResponseIndex)
      serialMap += ((atom.getEvent.getSerialNumber, atom))
    }
    assess.setResponseIndexes(indexes)

    // For all the missing atom nodes
    for (deleted <- orig.deletedPrimaryNodes.filter(_.isInstanceOf[AtomPatternNode]).toList.sortWith((n1, n2) => n1.index > n2.index)) {
      val atom = deleted.asInstanceOf[AtomPatternNode]

      // TODO this is where I propose we try to identify "you did this 
      // instead of that" type errors. We would look at the extra unmapped atoms
      // in the match and see if any of them were related by incorrect (e.g. a 
      // member of the same family but a specific action was required) and then 
      // replace the "missing" error with a "wrong action" message. Maybe check
      // the failed constraints due to the missing atom against the wrong atom

      // It would be silly to give them failed constraint feedback when they
      // missed the atom entirely
      scratch.violatedConstraints =
        scratch.violatedConstraints.filter(c => !deleted.allConstraints.contains(c))

      val missing = new MissingAtomIssue()
      missing.setFunctor(atom.getActionDef.getName.getFullName)
      missing.setStep(atom.getStep.getId)

      // The missing atom must "appear" after any atoms that support it. 
      val supporters = collection.mutable.Set[AtomPatternNode]()
      findSupporters(atom, supporters)
      var minIndex = 0
      supporters.foreach(support => {
        if (scratch.nodeMappings.contains(support)) {
          val idx = candidateAtoms.indexOf(scratch.nodeMappings(support))
          if (idx >= minIndex) {
            minIndex = idx + 1
            missing.setMinDisplayIndex(minIndex)
          }
        }
      })
      
      // Also, before any that it supports
      val supportees = collection.mutable.Set[AtomPatternNode]()
      findSupportees(atom, supportees)
      var maxIndex = candidateAtoms.size
      supportees.foreach(support => {
        if (scratch.nodeMappings.contains(support)) {
          val idx = candidateAtoms.indexOf(scratch.nodeMappings(support))
          if (idx < maxIndex) {
            maxIndex = idx
            missing.setMaxDisplayIndex(maxIndex)
          }
        }
      })

      var index = minIndex

      // Now that we know what the minimum is, try to place it between its neighbors. 
      var anchoredToPrevious = false
      var node = atom.getPrevious
      while (node != null && !scratch.nodeMappings.contains(node))
        node = node.getPrevious

      if (node != null && scratch.nodeMappings.contains(node)) {
        val prevIdx = candidateAtoms.indexOf(scratch.nodeMappings(node))
        if (prevIdx >= index) {
          index = prevIdx + 1
          anchoredToPrevious = true
        }
      }

      if (!anchoredToPrevious) {
        node = atom.getNext
        while (node != null && !scratch.nodeMappings.contains(node))
          node = node.getNext
        if (node != null && scratch.nodeMappings.contains(node)) {
          val nextIdx = candidateAtoms.indexOf(scratch.nodeMappings(node))
          if (nextIdx - 1 > index && nextIdx - 1 < maxIndex) {
            index = nextIdx - 1
          }
        }
      }

      missing.setPrefDisplayIndex(index)
      assess.getMissingAtomIssues().add(missing)
    }

    // Now process any extra steps. We do this second in case we eventually 
    // handle the "wrong action" case, which transforms an extra step in to 
    // wrong step. This code assumes the atom nodes are in order in the candidate
    var chain = false
    var idx = 0;
    var chainStart = 0;
    
    val influencers = collection.mutable.Set[Long]()
    
    for (con <- scratch.satisfiedConstraints ++ scratch.violatedConstraints)
      if (con.isInstanceOf[StateEvaluator]) {
        val scon = con.asInstanceOf[StateEvaluator]
        val objects = ArrayBuffer[Object]()
        for (arg <- scon.constraint.getArguments()) {
          if (arg.getRef != null) {
            val value = symbols.lookupReference(arg.getRef)
            if (value != null)
              objects += ValueUtil.getObject(value, bridge)
            else
              objects += scratch.getMappedSecondaryNode(arg.getRef).asInstanceOf[ArgCandidateNode].getValue
          } else {
            val v = arg.getValue()
            objects += ValueUtil.getObject(v, bridge)
          }
        }

        val res = stateMap.getPredicateResult(scon.constraint.getFunctor, objects)
        
        if (res != None) {
          // TODO detect support atoms for this candidate atom and consider them influencers as well
          influencers ++= res.get._2.map(p => p._2)
        }
      }

    for (cand <- candidateAtoms) {
      val atom = cand.asInstanceOf[AtomCandidateNode]
      val extra = !scratch.reverseNodeMappings.contains(atom) && !influencers.contains(atom.getEvent.getSerialNumber)      
      
      if (!chain && atom == candidateAtoms.last && extra) {
        chain = true
        chainStart = idx;
      }

      if (chain && (!extra || atom == candidateAtoms.last)) {
        chain = false
        val extraIss = new ExtraAtomsIssue()
        extraIss.setStartAtomIndex(chainStart)
        extraIss.setEndAtomIndex(if (extra) idx else idx - 1)
        assess.getExtraAtomsIssues().add(extraIss)
      }

      if (!chain && extra) {
        chainStart = idx
        chain = true
      }

      idx += 1
    }

    for (viol <- scratch.violatedConstraints) {
      viol match {
        case eq: EqualityEvaluator => {
          val atom1 = eq.node1.getAtom
          val atom2 = eq.node2.getAtom
          val atom1idx = candidateAtoms.indexOf(scratch.nodeMappings(atom1))
          val atom2idx = candidateAtoms.indexOf(scratch.nodeMappings(atom2))

          val issue = new EqualityIssue(
            new ArgumentLocation(atom1idx, eq.node1.getAccessorPath),
            new ArgumentLocation(atom2idx, eq.node2.getAccessorPath),
            eq.constraint.isSupport(),
            eq.constraint.isNegated())

          assess.getEqualityIssues().add(issue)
        }
        case order: OrderingEvaluator => {
          if (!order.inferred) {
            val issue = new OrderingIssue()
            issue.setPredecessorIndex(if (order.predecessor != null) candidateAtoms.indexOf(scratch.nodeMappings(order.predecessor)) else -1)
            issue.setSuccessorIndex(if (order.successor != null) candidateAtoms.indexOf(scratch.nodeMappings(order.successor)) else -1)
            assess.getOrderingIssues().add(issue)
          }
        }
        case query: QueryEvaluator => {
          val issue = new QueryIssue(query.constraint)
          val locs = query.nodes
            .map(n =>
              new ArgumentLocation(
                candidateAtoms.indexOf(scratch.nodeMappings(n.asInstanceOf[ParamPatternNode].getAtom)),
                n.asInstanceOf[ParamPatternNode].getAccessorPath));

          issue.getLocations().addAll(locs)
          assess.getQueryIssues().add(issue)
        }
        case state: StateEvaluator => {
          val issue = new StateIssue(state.constraint)
          val locs = state.nodes
            .map(n =>
              new ArgumentLocation(
                candidateAtoms.indexOf(scratch.nodeMappings(n.asInstanceOf[ParamPatternNode].getAtom)),
                n.asInstanceOf[ParamPatternNode].getAccessorPath));

          issue.getLocations().addAll(locs)
          assess.getStateIssues().add(issue)          
        }
        case value: ValueEvaluator => {
          val atomIdx = candidateAtoms.indexOf(scratch.nodeMappings(value.pnode.getAtom))

          val issue = new ValueIssue(
            value.constraint,
            new ArgumentLocation(atomIdx, value.pnode.getAccessorPath))

          assess.getValueIssues().add(issue)
        }
      }
    }

    assess
  }

  private def findSupporters(
    atom: AtomPatternNode,
    supporters: collection.mutable.Set[AtomPatternNode]) {
    atom.allConstraints
      .filter(_.isInstanceOf[EqualityEvaluator])
      .map(_.asInstanceOf[EqualityEvaluator])
      .foreach(c => {
        if (c.support && c.getSupported == atom) {
          supporters += c.getSupporter
          findSupporters(c.getSupporter, supporters)
        }
      })
  }
  
  private def findSupportees(
    atom: AtomPatternNode,
    supportees: collection.mutable.Set[AtomPatternNode]) {
    atom.allConstraints
      .filter(_.isInstanceOf[EqualityEvaluator])
      .map(_.asInstanceOf[EqualityEvaluator])
      .foreach(c => {
        if (c.support && c.getSupporter == atom) {
          supportees += c.getSupported
          findSupporters(c.getSupporter, supportees)
        }
      })
  }
}
