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

package com.sri.ai.patternmatcher

import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import org.apache.log4j.Level
import org.testng.annotations.BeforeClass

class PatternMatcher_Test {
}

object PatternMatcher_Test {   
  @BeforeClass def setupLogging {
    Logger.getRootLogger().getLoggerRepository().resetConfiguration();
    val console = new ConsoleAppender();
    val PATTERN = "%p %d{HH:mm:ss} %C{1}: %n%m%n";
    console.setLayout(new PatternLayout(PATTERN)); 
    console.setThreshold(Level.DEBUG);
    console.activateOptions();
    Logger.getRootLogger().addAppender(console);    
  }
}
