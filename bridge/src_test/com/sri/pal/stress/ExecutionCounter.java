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

// $Id: ExecutionCounter.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.stress;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.sri.pal.ActionInvocationStatusListener;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.GlobalActionListener;
import com.sri.pal.common.ErrorInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionCounter
        implements GlobalActionListener, ExecutionCounterMBean {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    private Set<ActionStreamEvent> running;
    private int numStarted = 0;
    private int numErrors = 0;

    public ExecutionCounter()
            throws JMException {
        running = new HashSet<ActionStreamEvent>();
        running = Collections.synchronizedSet(running);

        // MBean registration:
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName(getClass().getPackage().getName(),
                "type", getClass().getSimpleName());
        server.registerMBean(this, name);
    }

    public int getNumRunning() {
        return running.size();
    }

    public int getTotalStarted() {
        return numStarted;
    }

    public int getNumErrors() {
        return numErrors;
    }

    public Set<ActionStreamEvent> runningSet() {
        return Collections.unmodifiableSet(running);
    }

    @Override
    public void actionStarted(ActionStreamEvent action) {
        synchronized(running)
        {
            numStarted++;
            running.add(action);
        }
        Watcher watcher = new Watcher(action);
        action.addListener(watcher);
    }

    private class Watcher
            implements ActionInvocationStatusListener {
        ActionStreamEvent invoc;

        public Watcher(ActionStreamEvent action) {
            invoc = action;
        }

        @Override
        public void error(ErrorInfo error) {
            log.warn("Error for " + invoc, error);
            numErrors++;
        }

        @Override
        public void newStatus(Status newStatus) {
            if (newStatus == Status.ENDED || newStatus == Status.FAILED) {
                synchronized(running)
                {
                    running.remove(invoc);
                }
            }
        }
    }
}
