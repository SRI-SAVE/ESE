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

package com.sri.pal.stress;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility Class used by NetworkStress_ExtraFuncTest.class
 * to drop the network during a stress test.  Call as:
 * new DropNetworkThread.start();
 * @author Josh
 */
public class DropNetworkThread extends Thread {
	private static final Logger log = LoggerFactory
    .getLogger("TestSourceLogger");

	public void run() {
		try {
			//Wait for the stress test to initialize
			sleep(3000);
			releaseRenew();

		} catch (InterruptedException e) {}
	}

	public static void releaseRenew() throws InterruptedException {
		log.info("Calling ipconfig -release");
		makeSystemCall("ipconfig -release");
		// Give the system a moment to see that its network interface is down
		sleep(2000);
		log.info("Calling ipconfig -renew");
		makeSystemCall("ipconfig -renew");
	}

	public static void makeSystemCall(String systemCall) {
		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(systemCall);

			InputStream stdout = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stdout);
			BufferedReader br = new BufferedReader(isr);

			String line = null;
			while ( (line = br.readLine()) != null) {
				log.info(line);
			}
			int exitVal = proc.waitFor();
			log.info("System call exit value: " + exitVal);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}