
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

import com.sri.pal.util.PALTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This test uses release/renew to drop the network interface
 *  while running the stress test. To call this test by itself:
 * "ant extra-tests -Dtest.prefix="NetworkStress"
 * @author Josh
 */
public class NetworkStress_ExtraFuncTest
		extends PALTestCase {
	private static final Logger log = LoggerFactory
    .getLogger("TestSourceLogger");

	@BeforeClass
	public static void start() throws Exception {
		log.info("Starting PAL Bridge");
		Stress_FuncTest.start();
	}

    /*
     * Because this test does funny things with the network interfaces, it's
     * disabled by default.
     */
    @Test(enabled = false)
	public void stress() throws Exception {
		// Spawn a new thread to kill the network interface
		new DropNetworkThread().start();

		log.info("Calling the stress test");
		Stress_FuncTest stressTester = new Stress_FuncTest();
		stressTester.stress();
	}
}