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

import java.text.SimpleDateFormat

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

class SriJavaFxPlugin implements Plugin<Project> {
    void apply(Project project) {
        def rootProject = project.rootProject

        def fxhome = System.getenv("JAVAFX_HOME")
        if (fxhome == null) {
            fxhome = System.properties['java.home']
        }

        // JavaFX projects are based on (our own) Java projects.
        project.apply(plugin: SriJavaPlugin)

        // JavaFX jar files are built strangely.
        project.jar.deleteAllActions()
        project.jar.doLast {
            def fxPath = "$fxhome/tools/ant-javafx.jar;$fxhome/../lib/ant-javafx.jar;$fxhome/lib/ant-javafx.jar"
            def jarCp = archiveName
            project.configurations.runtime.resolvedConfiguration.resolvedArtifacts.each { dep ->
                jarCp += " ${dep.file.name}"
            }
            def dateFormat = new SimpleDateFormat("yyyy-MM-dd")
            rootProject.ant {
                try {
                    taskdef(name: 'fxjar',
                            classname: 'com.sun.javafx.tools.ant.FXJar',
                            classpath: fxPath)
                } catch(Exception e) {
                    def msg = "Can't find fxjar; search path: '$fxPath'. Try pointing JAVAFX_HOME to a JDK (not JRE)."
                    throw new GradleException(msg, e)
                }
                fxjar(destfile: "${archivePath}") {
                    application(id: "${project.name}",
                        name: "${project.name}",
                        mainClass: "${project.fxMainClass}")
                    source.addToAntBuilder(ant, "fileset", FileCollection.AntType.FileSet)
                    ant.manifest {
                        attribute(name: 'Built-By',
                                  value: System.properties['user.name'])
                        attribute(name: 'Build-Date',
                                  value: dateFormat.format(new Date()))
                        attribute(name: 'Specification-Title',
                                  value: project.name)
                        attribute(name: 'Specification-Version',
                                  value: rootProject.release)
                        attribute(name: 'Specification-Build',
                                  value: rootProject.build)
                        attribute(name: 'Specification-Vendor',
                                  value: 'SRI International')
                        attribute(name: 'Implementation-Version',
                                  value: "${rootProject.revision} ${rootProject.branch}")
                        attribute(name: 'Class-Path',
                                  value: "$jarCp")
                    }
                }
            }
        }
    }
}
