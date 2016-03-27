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

import java.io.{File, FileWriter, PrintWriter, StringReader, StringWriter}
import javax.xml.bind.JAXBElement

import com.sri.ai.patternmatcher.ExecutionInfo
import com.sri.pal.{ActionDef, ActionInvocation, Bridge, CustomTypeDef, ListDef, TypeDef}
import com.sri.pal.training.core.exercise.{Atom, ConstraintArgument, EqualityConstraint, Exercise, Parameter, Problem, QueryConstraint, Solution, Step, SubTask, Task, TaskSolution}
import com.sri.pal.training.core.response.{Response, TaskResponse}
import com.sri.pal.training.core.storage.ExerciseFactory
import com.sri.pal.training.core.util.ResponseUtil
import com.sri.pal.upgrader.MemoryTypeStorage
import com.sri.tasklearning.spine.messages.contents.ActionCategory
import org.testng.annotations.{AfterMethod, Test}

import scala.collection.JavaConversions.{asJavaCollection, asScalaBuffer, asScalaSet, mutableSeqAsJavaList, seqAsJavaList, setAsJavaSet}
import scala.collection.mutable
import scala.util.Random

/**
 * Perform a load test of the automated assessment and pattern matcher modules
 * by constructing a solution of arbitrary size. Then try to match it against a
 * response of similar size, but with 0 or more mutations which may make it
 * incorrect.
 */
class LoadTest {
  /* How many actions to expect the solution to contain. */
  val NUM_ACTIONS = 10
  /* How many query constraints to add to the solution. */
  val NUM_CONSTRAINTS = 5
  /* How incorrect should the response be; how many times should it be
     * altered? */
  val NUM_MUTATIONS = 1
  /* What's the probability that each generated argument will be a reference to
   * a previous one? */
  val REUSE_ARG_CHANCE = 0.8f
  /* What action model to load. */
  val AM_FILE = "MockCpof.xml"

  /* A sequence generator. */
  var _nextId = 0
  private def nextId: Int = {
    _nextId += 1
    _nextId
  }

  val bridge = {
    Bridge.startPAL()
    Bridge.newInstance(getClass.getSimpleName)
  }
  bridge.setTypeStorage(new MemoryTypeStorage())
  val actionModel = bridge.getActionModel

  val random = new Random()

  val executor = {
    val execBridge = Bridge.newInstance("executor")
    new MockCpofExecutor(execBridge)
  }

  /* Load the action model, and register the executor for all actions. */
  actionModel.load(getClass.getResource(AM_FILE),
    getClass.getSimpleName).foreach {
    case actDef: ActionDef =>
      actionModel.registerExecutor(actDef.getName, executor)
    case default =>
  }

  /* What action definitions can we choose from to build our solution and
   * response, and what are our query actions? */
  var availActDefs = IndexedSeq[ActionDef]()
  var availQueryDefs = IndexedSeq[ActionDef]()
  actionModel.getTypes().foreach {
    case actDef: ActionDef => actDef.getCategory match {
      case ActionCategory.EFFECTOR => availActDefs +:= actDef
      case ActionCategory.QUERY => availQueryDefs +:= actDef
      case default =>
    }
    case default =>
  }

  /* Generate a map of all the parameter values used in the trace, with
   * type => Seq[values] */
  val params = mutable.Map[TypeDef, IndexedSeq[Object]]()

  @AfterMethod
  def shutdown(): Unit = {
    bridge.shutdown()
  }

  @Test(enabled = false)
  def loadTest(): Unit = {
    /* Generate a series of action calls with random argument values. */
    var trace = Seq[ActionInvocation]()
    (0 until NUM_ACTIONS).foreach { _ =>
      val actDef = availActDefs(random.nextInt(availActDefs.size))
      val invoc = actDef.invoke(null)
      (0 until actDef.size()).map { paramNum =>
        (paramNum, actDef.getParamType(paramNum))
      }.foreach { t =>
        val paramNum = t._1
        val typeDef = t._2
        val reuseChance: Float = if (actDef.numInputParams > paramNum) { REUSE_ARG_CHANCE } else { 0 }
        invoc.setValue(paramNum, makeArg(typeDef, reuseChance))
      }
      trace :+= invoc
      (actDef.numInputParams() until actDef.size()).map { paramNum =>
          (actDef.getParamType(paramNum), invoc.getValue(paramNum))
      }.foreach { t =>
          val typeDef = t._1
          val value = t._2
          updateParams(typeDef, value)
      }
    }
    trace.foreach { invoc =>
      val actDef = invoc.getDefinition
      (0 until actDef.numInputParams()).map { paramNum =>
        (actDef.getParamType(paramNum), invoc.getValue(paramNum))
      }.foreach { t =>
        val typeDef = t._1
        val value = t._2
        updateParams(typeDef, value)
      }
    }

    /* Generate a set of constraints on those parameters. */
    val constraints = (0 until NUM_CONSTRAINTS).map { i =>
      val actDef = availQueryDefs(random.nextInt(availQueryDefs.size))
      val args = (0 until actDef.numInputParams()).map { i =>
        actDef.getParamType(i)
      }.map { td =>
        val argsList = params(td)
        argsList(random.nextInt(argsList.size))
      }
      actDef.invoke(null, args: _*)
    }

    /* Build the solution from the action calls and constraints. If a given
     * constraint is not satisfied by the provided argument values, record it
     * as negated. */
    val exercise = makeExercise(trace, constraints)

    /* Mutate the action trace the requested number of times. */
    var mutations = Seq[String]()
    val mutatedTrace = {
      var mut = trace
      for (i <- 0 until NUM_MUTATIONS) {
        val t = mutate(mut)
        mut = t._1
        mutations :+= t._2
      }
      mut
    }

    /* Build a response from the mutated action trace. */
    val response = makeResponse(mutatedTrace, exercise.getId, mutations)

    /* Start recording performance metrics. */
    val preMem = {
      val runtime = Runtime.getRuntime
      runtime.gc()
      runtime.maxMemory() - runtime.freeMemory()
    }
    val preTime = System.currentTimeMillis()

    /* Perform the assessment. */
    val assessor = new Assessor(bridge)
    val assessment = assessor.assessTask(
        exercise.getProblem.getTasks.get(0), 
        exercise.getSolution.getTaskSolutions.get(0), 
        response.getTaskResponses.get(0),
        new SymbolManager(bridge))

    /* Stop recording performance metrics. */
    val postTime = System.currentTimeMillis()
    val postMem = {
      val runtime = Runtime.getRuntime
      runtime.gc()
      runtime.maxMemory() - runtime.freeMemory()
    }

    /* Verify that the assessment is correct, based on the number of mutations
     * we performed. */
    // TODO

    /* Analyze recorded performance metrics. */
    println("Test took " + (postTime - preTime) + "ms for " + NUM_ACTIONS +
      " actions, " + NUM_CONSTRAINTS + " constraints, and " + NUM_MUTATIONS +
      " mutations.")
    println("Memory used: " + (preMem / 1024 / 1024) + "MB before, " +
      (postMem / 1024 / 1024) + "MB after.")

    /* Log the parameters for future reference. */
    logResults(NUM_ACTIONS, NUM_CONSTRAINTS, NUM_MUTATIONS, REUSE_ARG_CHANCE,
      postTime - preTime, exercise, response, mutations,
      assessor.lastMatch.cost, assessor.lastExecInfo)
  }

  /**
   * Given a type definition, generate a value of that type. Some values will
   * be created from scratch, while others will be pulled from previous values.
   * @param typeDef the type definition to make a value for
   * @return a single value of the appropriate type
   */
  private def makeArg(typeDef: TypeDef,
    reuseChance: Float): Object = {
    /* Do we reuse a previous value? */
    if (random.nextFloat() < reuseChance) {
      params.get(typeDef) match {
        case Some(values) =>
          /* Reuse one of these. */
          return values(random.nextInt(values.size))
        case default =>
      }
    }

    /* Make a new value. */
    typeDef match {
      case ld: ListDef =>
        val javaList = (0 until 3).map { j =>
          makeArg(ld.getElementType, reuseChance)
        }
        seqAsJavaList(javaList)
      case cd: CustomTypeDef =>
        /* Assume it's a Long. */
        random.nextLong(): java.lang.Long
      case default =>
        throw new IllegalArgumentException("Can't make a value for " + typeDef)
    }
  }

  private def updateParams(typeDef: TypeDef,
    value: Object): Unit = {
    if (!params.contains(typeDef)) {
      params.put(typeDef, IndexedSeq[Object]())
    }
    params.put(typeDef, value +: params(typeDef))
    typeDef match {
      case ld: ListDef =>
        val javaList = value.asInstanceOf[java.util.List[Object]]
        updateParams(ld.getElementType, javaList.get(0))
        updateParams(ld.getElementType, javaList.get(javaList.size - 1))
      case cd: CustomTypeDef =>
    }
  }

  private def makeExercise(trace: Seq[ActionInvocation],
    constraints: IndexedSeq[ActionInvocation]): Exercise = {
    /* We need to be able to track where parameters (by value) are in the trace.
     * A Loc is an ordered sequence of accessors, starting with the step number
     * (not step ID). */
    type Loc = Seq[String]
    var valueLocs = Map[Object, Set[Loc]]()

    trace.indices.foreach { stepNum =>
      val invoc = trace(stepNum)
      val actDef = invoc.getDefinition
      val stepLoc = Seq[String]() :+ stepNum.toString
      (0 until actDef.size).foreach { paramNum =>
        val value = invoc.getValue(paramNum)
        val typeDef = actDef.getParamType(paramNum)
        val paramLoc = stepLoc :+ actDef.getParamName(paramNum)
        valueLocs = addToValueLocs(valueLocs, value, typeDef, paramLoc)
      }
    }

    /* Build the exercise JAXB object. */
    val ex = new Exercise()
    ex.setId("ex-" + nextId)
    ex.setName("Load test")
    ex.setDescription("Load test")
    ex.setProblem(new Problem())
    ex.getProblem.getTasks.add {
      val task = new Task()
      task.setId("task-" + nextId)
      val s = new SubTask()
      s.setPrompt("placeholder prompt")
      task.getSubTasks.add(s)
      task
    }
    ex.setSolution(new Solution())
    ex.getSolution.getTaskSolutions.add {
      val ts = new TaskSolution()
      val opt = new com.sri.pal.training.core.exercise.Option()
      opt.getSteps.addAll(trace.map { invoc =>
        val actDef = invoc.getDefinition
        val step = new Step()
        val atom = new Atom()
        step.setId("step-" + nextId)
        step.setAtom(atom)
        atom.setFunctor(actDef.getName.getFullName)
        step
      })

      /* Assign query constraints. */
      opt.getQueryConstraints.addAll(constraints.map { con =>
        val conDef = con.getDefinition
        val qcon = new QueryConstraint()
        qcon.setFunctor(conDef.getName.getFullName)
        qcon.getArguments.addAll((0 until conDef.numInputParams()).map { i =>
          val argValue = con.getValue(i)
          val qArg = new ConstraintArgument()
          val paramId: String = getOrSetParamId(valueLocs(argValue).iterator.next, opt.getSteps)
          qArg.setRef(paramId)
          qArg
        })

        /* Execute the constraint, so we can find out if we should expect it
         * to be true or false. */
        executor.execute(con)
        qcon.setNegated(!con.getValue(conDef.numInputParams()).asInstanceOf[Boolean])

        qcon
      })

      /* Assign equality constraints. */
      opt.getEqualityConstraints.addAll(valueLocs.filter { t =>
        t._2.size > 1
      }.map { t =>
        val locs = t._2
        val eqc = new EqualityConstraint()
        eqc.getParameters.addAll(locs.map { loc =>
          getOrSetParamId(loc, opt.getSteps)
        })
        eqc
      })

      ts.setOption(opt)
      ts
    }      

    val xml = writeXml(ExerciseFactory.createExercise(ex), "exercise.xml", Seq())

    /* This object is weird. It maintains internal back references that are set
     * by the unmarshaller. So we marshal and unmarshal it to hook those up. */
    unmarshal(xml, classOf[Exercise])
  }

  /**
   * Used to build the map of where each value in the trace is located.
   */
  private def addToValueLocs(valueLocs: Map[Object, Set[Seq[String]]],
    value: Object,
    typeDef: TypeDef,
    loc: Seq[String]): Map[Object, Set[Seq[String]]] = {
    var map = valueLocs
    if (!map.contains(value)) {
      map += ((value, Set[Seq[String]]()))
    }
    map += ((value, map(value) + loc))
    typeDef match {
      case ld: ListDef =>
        val memberType = ld.getElementType
        val list = value.asInstanceOf[java.util.List[_]]
        if (list.size > 0) {
          map = addToValueLocs(map, list.get(0).asInstanceOf[Object], memberType,
            loc :+ "first")
        }
        if (list.size > 1) {
          map = addToValueLocs(map, list.get(list.size - 1).asInstanceOf[Object],
            memberType, loc :+ "last")
        }
      case cd: CustomTypeDef =>
    }

    map
  }

  /**
   * This method is overloaded! Top-level call which handles the first link
   * in the location, which happens to be an integer referring to a step's
   * atom.
   */
  private def getOrSetParamId(loc: Seq[String],
    steps: mutable.Seq[Step]): String = {
    /* One or more of the invocations in trace contain argValue. Find them, and
     * ensure those parameters are all named in the XML. Return the XML ID of
     * one of them. */
    val invocNum = loc.head.toInt
    val step = steps(invocNum)
    val params = step.getAtom.getParameters
    getOrSetParamId(loc.tail, params, step.getId)
  }

  /**
   * This method is overloaded! Recursive method which descends into data
   * structures while following a location trail. It creates parallel data
   * structures in the JAXB pattern as it goes.
   */
  private def getOrSetParamId(loc: Seq[String],
    params: mutable.Seq[Parameter],
    id: String): String = {
    if (loc.isEmpty) {
      id
    } else {
      val accessor = loc.head
      /* Find or create the parameter named by accessor. */
      params.foreach { param =>
        if (param.getAccessor.equals(accessor)) {
          return getOrSetParamId(loc.tail, param.getSubParameters, param.getId)
        }
      }
      val param = new Parameter()
      param.setId("param-" + nextId)
      param.setAccessor(accessor)
      params.add(param)
      getOrSetParamId(loc.tail, param.getSubParameters, param.getId)
    }
  }

  private def mutate(trace: Seq[ActionInvocation]): (Seq[ActionInvocation], String) = {
    random.nextInt(5) match {
      case 0 =>
        /* delete */
        val index = random.nextInt(trace.size)
        (trace.slice(0, index) ++ trace.slice(index + 1, trace.size),
          "Delete step #%d".format(index + 1))
      case 1 =>
        /* transpose */
        val index = random.nextInt(trace.size - 1)
        ((trace.slice(0, index) :+ trace(index + 1) :+ trace(index)) ++
          trace.slice(index + 2, trace.size),
          "Swap steps #%d and #%d".format(index + 1, index + 2))
      case 2 =>
        /* change one parameter */
        val actNum = random.nextInt(trace.size)
        val oldAct = trace(actNum)
        val actDef = oldAct.getDefinition
        val newAct = actDef.invoke(null)
        (0 until actDef.size()).foreach { i =>
          newAct.setValue(i, oldAct.getValue(i))
        }
        val paramNum = random.nextInt(actDef.size())
        newAct.setValue(paramNum, makeArg(actDef.getParamType(paramNum), REUSE_ARG_CHANCE))
        ((trace.slice(0, actNum) :+ newAct) ++ trace.slice(actNum + 1, trace.size),
          "Change param #%d of step #%d from %s to %s".format(paramNum + 1,
            actNum + 1, oldAct.getValue(paramNum), newAct.getValue(paramNum)))
      case 3 =>
        /* replace with a different action */
        val index = random.nextInt(trace.size)
        val oldInvoc = trace(index)
        val actDef = availActDefs(random.nextInt(availActDefs.size))
        val invoc = actDef.invoke(null)
        (0 until actDef.size()).foreach { i =>
          invoc.setValue(i, makeArg(actDef.getParamType(i), REUSE_ARG_CHANCE))
        }
        ((trace.slice(0, index) :+ invoc) ++ trace.slice(index + 1, trace.size),
          "Change step #%d from %s to %s".format(index + 1,
            oldInvoc.getDefinition.getName, actDef.getName))
      case 4 =>
        /* insert a new action */
        val index = random.nextInt(trace.size + 1)
        val actDef = availActDefs(random.nextInt(availActDefs.size))
        val invoc = actDef.invoke(null)
        (0 until actDef.size()).foreach { i =>
          invoc.setValue(i, makeArg(actDef.getParamType(i), REUSE_ARG_CHANCE))
        }
        ((trace.slice(0, index) :+ invoc) ++ trace.slice(index, trace.size),
          "Insert step #%d: %s".format(index + 1, actDef.getName))
    }
  }

  private def makeResponse(trace: Seq[ActionInvocation],
    exId: String,
    mutations: Seq[String]): Response = {
    val resp = new Response()
    resp.setExerciseId(exId)
    resp.setId("resp-" + nextId)
    resp.getTaskResponses.add {
      val tr = new TaskResponse()
      ResponseUtil.setDemonstration(tr, trace, bridge.getLearner)
      tr
    }
    val xml = writeXml(ExerciseFactory.createResponse(resp), "response.xml",
      mutations)
    unmarshal(xml, classOf[Response])
  }

  private def writeXml[A](jaxbEle: JAXBElement[_ >: A],
    name: String,
    comments: Seq[String]): String = {
    val jaxbMarsh = ExerciseFactory.getMarshaller
    val sw = new StringWriter()
    jaxbMarsh.marshal(jaxbEle, sw)
    val xml = sw.toString
    val dir = new File("../perflogs")
    if (!dir.isDirectory) {
      dir.mkdirs()
    }
    val file = new File(dir, name)
    val out = new PrintWriter(new FileWriter(file))
    out.print(xml)
    comments.foreach { c => out.print("<!-- %s -->\n".format(c)) }
    out.close()
    xml
  }

  private def unmarshal[A](xml: String,
    aClass: Class[A]): A = {
    val um = ExerciseFactory.getUnmarshaller
    val reader = new StringReader(xml)
    val newEle = um.unmarshal(reader).asInstanceOf[JAXBElement[A]]
    newEle.getValue
  }

  private def logResults(numActions: Int,
    numConstraints: Int,
    numMutations: Int,
    argReuseChance: Double,
    duration: Long,
    exer: Exercise,
    resp: Response,
    mutations: Seq[String],
    cost: Double,
    execInfo: ExecutionInfo) = {
    val dir = new File("../perflogs")
    if (!dir.isDirectory) {
      dir.mkdirs()
    }
    val csvFile = new File(dir, "perflogs.csv")
    if (!csvFile.isFile) {
      val pw = new PrintWriter(new FileWriter(csvFile))
      pw.println("id,numActions,numConstraints,numMutations,argReuseChance," +
        "duration(ms),cost,numFullMatches,numExpansions,bestMatchIndex")
      pw.close()
    }

    val id = {
      var i = 0
      while (new File(dir, "exercise" + i + ".xml").exists()) { i += 1 }
      i
    }

    writeXml(ExerciseFactory.createExercise(exer), "exercise" + id + ".xml", Seq())
    writeXml(ExerciseFactory.createResponse(resp), "response" + id + ".xml", mutations)

    /* And delete the temporary copies that were created before assess() was
     * called. */
    new File(dir, "exercise.xml").delete()
    new File(dir, "response.xml").delete()

    val out = new PrintWriter(new FileWriter(csvFile, true))
    out.println("%d,%d,%d,%d,%f,%d,%f,%d,%d,%d".format(id, numActions,
      numConstraints, numMutations, argReuseChance, duration, cost,
      execInfo.numFullMatches, execInfo.numExpansions, execInfo.bestMatchIndex))
    out.close()
  }
}
