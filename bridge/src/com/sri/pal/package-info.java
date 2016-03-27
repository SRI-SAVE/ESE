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

// $Id: package-info.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
/**
 * Provides the interface into the PAL system. PAL allows learning of procedures
 * based on user demonstrations.
 *
 * <h2>Package Specification</h2>
 *
 * The broad categories of this interface are:
 * <ul>
 * <li> {@link com.sri.pal.Bridge}, the main entry point.
 * <li>The <b>action model</b> contains all known types and actions. The root
 * type is {@link com.sri.pal.TypeDef}, and its descendant classes
 * {@link com.sri.pal.ActionDef} and {@link com.sri.pal.ProcedureDef} represent
 * actions. Types and actions may be accessed via
 * {@link com.sri.pal.ActionModel}.
 * <li><b>Learning</b> of new procedures from demonstrations is handled by the
 * {@link com.sri.pal.Learner}.
 * <li><b>Execution</b> is the capability which either causes the PAL system to
 * perform learned procedures, or allows the PAL system to direct the
 * application to perform actions in the user's name as part of a running
 * procedure. This is managed by {@link com.sri.pal.ActionExecutor} and
 * {@link com.sri.pal.ProcedureExecutor}, using
 * {@link com.sri.pal.ActionInvocation} and its subclass
 * {@link com.sri.pal.ProcedureInvocation} to keep track of state.
 * <li><b>Instrumentation</b> allows the PAL system to see what the user and
 * application are doing. This is managed by the
 * {@link com.sri.pal.InstrumentationControl} object, which receives
 * {@link com.sri.pal.ActionInvocation} notifications from the application.
 * <li><b>Monitoring</b> allows the application to see all actions and
 * procedures within the PAL system, by registering a
 * {@link com.sri.pal.GlobalActionListener}.
 * <li><b>Persistence</b> is managed by {@link com.sri.pal.TypeLoader} and
 * {@link com.sri.pal.TypeLibrary} implementations which manage instances of
 * {@link com.sri.pal.ProcedureDef}.
 * </ul>
 *
 * <h2>Related Documentation</h2>
 *
 * <ul>
 * <li>Links to LAPDOG and lumen papers should go in this section.
 * </ul>
 */
package com.sri.pal;
