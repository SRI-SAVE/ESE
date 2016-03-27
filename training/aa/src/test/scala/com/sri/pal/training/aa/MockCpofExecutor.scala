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

import com.sri.pal.ActionExecutor
import com.sri.pal.ActionInvocation
import com.sri.pal.Bridge
import java.util.List
import scala.collection.JavaConversions._
import com.sri.pal.ActionStreamEvent
import com.sri.pal.ActionStreamEvent.Status

class MockCpofExecutor(val bridge: Bridge) extends ActionExecutor {
  override def continueStepping(invoc: ActionInvocation,
    cmd: ActionInvocation.StepCommand,
    args: List[Object]) = {
    // Do nothing.
  }

  override def cancel(event: ActionStreamEvent) = {
    // Do nothing.
  }

  override def executeStepped(invoc: ActionInvocation) = {
    // Do nothing.
  }

  override def execute(invoc: ActionInvocation) = {
    /* We should only ever receive execute requests for query actions. */
    val actDef = invoc.getDefinition()
    val inputs = (0 until actDef.numInputParams()).map { i =>
      invoc.getValue(i)
    }
    val result = actDef.getName().getSimpleName() match {
      case "div2" => {
        val arg0 = inputs(0).asInstanceOf[Long]
        arg0 % 2 == 0
      }
      case "lessThan2" => {
        val arg0 = inputs(0).asInstanceOf[Long]
        val arg1 = inputs(0).asInstanceOf[Long]
        arg0 < arg1
      }
      case "lessThan3" => {
        val args = inputs.map { o => o.asInstanceOf[Long] }
        args.zip(args.tail.view).forall { t => t._1 < t._2 }
      }
      case default => throw new IllegalArgumentException("Can't execute " +
        invoc)
    }
    invoc.setValue(actDef.numInputParams(), result)
    invoc.setStatus(Status.ENDED)
  }
}
