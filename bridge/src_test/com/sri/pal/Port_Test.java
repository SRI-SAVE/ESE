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

// $Id: Port_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.sri.pal.util.PALBridgeTestCase;

import org.testng.annotations.Test;

public class Port_Test
        extends PALBridgeTestCase {
    private Throwable error;

    @Test
    public void testRmiActive()
            throws Throwable {
        // start module here then check if it uses a port...
        for (int port = 1024; port < 2048; port++) {
            try {
                ServerSocket srv = new ServerSocket(port);
                srv.close();
            } catch (IOException e) {
                checkForRmi(port);
            }
        }
        Thread.sleep(1000);

        if (error != null) {
            throw error;
        }
    }

    private void checkForRmi(final int port) {
        Thread t = new Thread() {
            public void run() {
                try {
                    Registry reg = LocateRegistry.getRegistry(port);
                    reg.list();
                    fail("RMI active on port " + port);
                } catch (AssertionError r) {
                    error = r;
                } catch(Exception e) {
                    // Ignore
                }
            }
        };
        t.start();
    }
}
