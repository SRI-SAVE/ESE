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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.plugins.quality.FindBugs
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.testing.jacoco.plugins.JacocoPlugin

class SriJavaPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = "SRIJava"
    static Closure manifestInfo

    void apply(Project project) {
        def rootProject = project.rootProject

        // Depends on the Java and Jacoco plugins.
        project.apply(plugin:JavaPlugin)
        project.apply(plugin:JacocoPlugin)
        project.apply(plugin:MavenPlugin)

        // Set up dependencies for Jacoco.
        project.jacocoTestReport.dependsOn(project.test)
        project.check.dependsOn(project.jacocoTestReport)

        // Set jar manifest information.
        def dateFormat = new SimpleDateFormat("yyyy-MM-dd")
        manifestInfo = {
            attributes 'Built-By': System.properties['user.name'],
            'Build-Date': dateFormat.format(new Date()),
            'Specification-Title': project.name,
            'Specification-Version': rootProject.release,
            'Specification-Build': rootProject.build,
            'Specification-Vendor': 'SRI International',
            'Implementation-Version':
                    "${rootProject.revision} ${rootProject.branch}"
        }
        project.jar.manifest manifestInfo

        // Java 1.8
        project.sourceCompatibility = 1.8

        // Build a source jar.
        project.task([type: Jar], 'sourceJar') {
            from project.sourceSets.main.allSource
            classifier = 'sources'
            manifest manifestInfo
        }
        project.configurations.create('source')
        project.artifacts.add('source', project.sourceJar)
        project.artifacts.add('archives', project.sourceJar)

        // Don't fail the build if there are Javadoc problems.
        project.javadoc {
            failOnError = false
        }

        // Build a doc zip.
        project.task([type: Zip, dependsOn: project.javadoc], 'docZip') {
            from project.javadoc
            into('api')
            classifier = 'doc'
        }
        project.configurations.create('docs')
        project.artifacts.add('docs', project.docZip)
        project.artifacts.add('archives', project.docZip)

        // Make test jars available to other projects.
        project.task([type: Jar, dependsOn: project.testClasses], "testJar") {
            appendix = "test"
            from project.sourceSets.test.output
        }
        project.artifacts.add('testCompile', project.testJar)

        // All tests depend on TestNG.
        project.dependencies.add('testCompile', 'org.testng:testng:6.8')
        project.test.useTestNG()

        // Use the nicer report format for TestNG.
        project.test.reports.html.enabled = true

        // Provide more granular stdout and stderr.
        project.test.reports.junitXml.outputPerTestCase = true

        // New JVM for each test class. This may not be necessary.
        project.test.forkEvery = 1

        // Set test properties.
        project.test.options {
            def exclStr = project.rootProject.properties['test.excludes']
            excludeGroups exclStr.tokenize() as String[]
        }
        if (project.rootProject.properties['tasklearning.test.port'] != null) {
            def port = project.rootProject.properties['tasklearning.test.port']
            project.test.systemProperty 'PAL.JmsMessageBrokerPort', port
        }

        // Run tests in a sandbox directory.
        def sandbox = project.file('sandbox')
        project.test.workingDir(sandbox)
        project.clean.delete(sandbox)
        project.test.doFirst {
            project.delete sandbox
            project.mkdir(sandbox)
        }

        // System properties for tests.
        project.test {
            systemProperty 'PAL.storageDir', sandbox.path + '/AdeptTaskLearning'
        }

        // Possibly ignore test failures, for use with Jenkins.
        if (project.rootProject.hasProperty('test.ignoreFailures')) {
            project.test.ignoreFailures = true
        }

        // Eclipse's TestNG plugin will create this dumb directory, so
        // we should have clean remove it.
        project.clean.delete(project.file("test-output"))

        // Use FindBugs for static code analysis.
        project.apply(plugin: 'findbugs')
        project.tasks.withType(FindBugs) {
            ignoreFailures true
        }

        // These two plugins will build IDE config files based on the
        // corresponding gradle config.
        project.apply(plugin: 'eclipse')
        project.apply(plugin: 'idea')
        project.eclipse.classpath.defaultOutputDir =
            project.file('.eclipse.bin')

        // Create a directory with this project's jars and dependencies.
        project.task("createRunDir", dependsOn: project.assemble) {
            project.ext.runDir = project.file("${project.getBuildDir()}/run")
            doFirst {
                delete(project.ext.runDir)
                copy {
                    into project.ext.runDir
                    from (project.configurations.default + project.configurations.default.allArtifacts.files)
                    exclude('**/jfxrt.jar')
                }
            }
        }

        // Copy ("upload") dependencies and artifacts to a local directory.
        project.group = "com.sri"
        project.uploadArchives.repositories {
            mavenDeployer {
                repository(url: "file:///${project.rootDir}/dist/maven")
                // Remove test dependencies; they confuse Maven when
                // we have transitive dependencies plus depend source
                // configurations plus depend target configurations.
                pom.whenConfigured { pom ->
                    pom.dependencies.findAll { dep ->
                        dep.scope.equals("test")
                    }.each { dep ->
                        pom.dependencies.remove(dep)
                    }
                }
            }
        }
        project.uploadArchives.dependsOn(project.rootProject.cleanDist)
        project.rootProject.uploadArchives.dependsOn(project.uploadArchives)

        // Collect dependencies into the same place.
        project.task([type: Copy, dependsOn: project.assemble], "collectDependencies") {
            ext.depDir = project.file("$project.rootDir/dist/$project.name/lib")
            outputs.dir(depDir)
            from project.configurations.runtime
            into depDir
        }
    }
}
