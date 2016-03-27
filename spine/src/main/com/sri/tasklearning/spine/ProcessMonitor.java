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

// $Id: ProcessMonitor.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.slf4j.Logger;

/**
 * This thread monitors stdout from the spawned process to make sure it gets
 * logged and doesn't cause the process to block.
 *
 * @author chris
 */
// TODO Change this to use nio so it's event-driven and doesn't cause the
// process to run so slow.
public class ProcessMonitor
        extends Thread {
    private Process proc;
    private Logger log;
    private String prefix;
    private boolean procRunning = true;

    public ProcessMonitor(Process backendProc,
                          Logger logger,
                          String prefix) {
        proc = backendProc;
        log = logger;
        this.prefix = prefix;
        setDaemon(true);
    }

    public void run() {
        log.debug("Starting");

        DeathMonitor dm = new DeathMonitor();
        dm.start();

        InputStream inputStream = proc.getInputStream();
        InputStream errorStream = proc.getErrorStream();
        InputStreamReader inputReader = new InputStreamReader(inputStream);
        InputStreamReader errorReader = new InputStreamReader(errorStream);
        StringBuffer inputBuffer = new StringBuffer();
        StringBuffer errorBuffer = new StringBuffer();
        while (procRunning) {
            try {
                boolean hadInput, hadError;
                do {
                    hadInput = read(inputReader, inputBuffer);
                    hadError = read(errorReader, errorBuffer);
                } while (hadInput || hadError);
            } catch (IOException e) {
                log.error("Couldn't read from the process", e);
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        log.debug("Done");
    }

    public void stopProcess() {
        proc.destroy();
    }

    private boolean read(Reader input,
                         StringBuffer buffer)
            throws IOException {
        boolean printed = false;
        while (input.ready()) {
            char ch = (char) input.read();
            if (ch == '\n') {
                String line = buffer.toString();
                log.info(prefix + line);
                buffer.setLength(0);
                printed = true;
            } else {
                buffer.append(ch);
            }
        }

        return printed;
    }

    private class DeathMonitor
            extends Thread {
        public void run() {
            boolean keepGoing = true;
            while (keepGoing) {
                try {
                    proc.waitFor();
                    keepGoing = false;
                } catch (InterruptedException ignored) {
                }
            }
            procRunning = false;
            log.warn("Process died with exit value " + proc.exitValue());
        }
    }
}
