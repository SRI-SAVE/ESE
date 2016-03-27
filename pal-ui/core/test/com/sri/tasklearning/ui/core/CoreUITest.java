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

package com.sri.tasklearning.ui.core;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import com.sri.pal.Bridge;
import com.sri.tasklearning.util.LogUtil;
import com.sri.tasklearning.util.PALRunListener;

@Listeners(PALRunListener.class)
public abstract class CoreUITest {    
    protected static Bridge bridge;
    protected TestAppender appender;
    
    protected static final String CLIENT_NAME = "CoreUITest";
    protected static final String LOG_CONFIG_BASE = "pal-ui";
    
    @BeforeClass
    public static void registerUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                fail("Uncaught exception on thread '" + t.getName() + "'");
            }
         });
    }
    @BeforeClass
    public static void preTests() throws Exception {     
        LogUtil.configureLogging(LOG_CONFIG_BASE, CoreUITest.class);      
    }
        
    @BeforeMethod
    public void preTest() throws Exception {
        appender = new TestAppender();
        final Logger logger = Logger.getRootLogger();
        logger.addAppender(appender);        
    }
          
    @AfterMethod
    public void postTest() throws Exception {       
        Logger.getRootLogger().removeAppender(appender);
    }
    
    @AfterClass
    public static void postTests() throws Exception { 
    }

    
    protected void assertNoLogErrors(boolean includeWarnings) {        
        assertTrue(appender.getErrorCount() + " errors in log file", appender.getErrorCount() == 0);
        assertTrue(appender.getFatalCount() + " fatal errors in log file", appender.getFatalCount() == 0);
        
        if (includeWarnings)
            assertTrue(appender.getWarnCount() + " warnings in log file", appender.getWarnCount() == 0);
    }
    
    class TestAppender extends AppenderSkeleton {
        private final List<LoggingEvent> log = new ArrayList<LoggingEvent>();
        private int warnCount = 0;        
        private int errorCount = 0;
        private int fatalCount = 0;
        private int infoCount = 0;
        private int otherCount;
        
        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        protected void append(final LoggingEvent loggingEvent) {
            Level lvl = loggingEvent.getLevel();

            switch (lvl.toInt()) {
                case Level.ERROR_INT: errorCount++; break;
                case Level.FATAL_INT: fatalCount++; break;
                case Level.INFO_INT: infoCount++; break;
                case Level.WARN_INT: warnCount++; break;
                default: otherCount++; break;
            }

            log.add(loggingEvent);
        }

        @Override
        public void close() {
        }

        public List<LoggingEvent> getLog() {
            return new ArrayList<LoggingEvent>(log);
        }
        
        public int getWarnCount() {
            return warnCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public int getFatalCount() {
            return fatalCount;
        }

        public int getInfoCount() {
            return infoCount;
        }

        public int getOtherCount() {
            return otherCount;
        }
    }
}
