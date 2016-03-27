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

package com.sri.tasklearning.spine.impl.jms.util;

import java.io.File;
import java.io.IOException;

import com.sri.tasklearning.spine.impl.jms.JmsRemoteSpine_Interactive;

public class BridgeJmsSpine implements Runnable {
    public void run() {
        // This is where the Bridge JmsSpine will be launched in another JMV See the
        // JmsRemoteSpine_Interactive class for what is done there.
        String[] args = new String[] {
                "java",
                "-cp",
                System.getProperty("java.class.path"),
                JmsRemoteSpine_Interactive.class.getName()
        };
        Runtime rt = Runtime.getRuntime();
        Process proc = null;
        try {
            proc = rt.exec(args, new String[0], new File("../.."));
        } catch (IOException e) {
            e.printStackTrace();
        }
        StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");
        StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT");
        errorGobbler.start();
        outputGobbler.start();
        try {
            int exitVal = proc.waitFor();
            System.out.println("Exit value is: " + exitVal);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
