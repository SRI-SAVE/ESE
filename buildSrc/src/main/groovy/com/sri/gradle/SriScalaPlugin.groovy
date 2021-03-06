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

package com.sri.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.scala.ScalaCompile

class SriScalaPlugin implements Plugin<Project> {
    void apply(Project project) {
        // Start with all the config from our Java plugin.
        project.apply(plugin: 'SriJava')

        // Use the Scala plugin for scala modules.
        project.apply(plugin: 'scala')

        // Depend on the Scala libraries.
        project.dependencies.add('compile', 'org.scala-lang:scala-library:2.10+')

        // Modify the docZip task from the SRI Java plugin to also
        // include the results of ScalaDoc.
        project.docZip {
            from project.scaladoc
        }
    }
}
