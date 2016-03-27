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

// $Id: Classpath_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.classpath;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sri.pal.ActionModel;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.Bridge;
import com.sri.pal.GlobalActionListener;
import com.sri.pal.PALRemoteException;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.ProcessMonitor;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * If two clients are both using the Bridge, and they have different classpaths,
 * problems can result. This class reproduces those problems. We have a type
 * backed by a representation class (Type1) which is in a separate jar file.
 * That jar file is not available to this JVM, which starts the PAL backend. But
 * it is available to a child JVM which we start. That child JVM should be able
 * to learn and execute a procedure using that class, even though LAPDOG and
 * Lumen can't load it.
 *
 * @author chris
 */
public class Classpath_FuncTest
        extends PALTestCase {
    private static final String TEST2_JAR_PATH = "../build/test2.jar";
    public static final String TYPE1_CLASS = "com.sri.pal.Type1";

    private static int procsSeen = 0;
    private Bridge bridge;

    @AfterMethod
    public void teardown() throws PALRemoteException {
        bridge.shutdown();
    }

    @Test
    public void classpaths()
            throws Exception {
        /* Ensure we don't have the class in question on our classpath. */
        try {
            Class.forName(TYPE1_CLASS);
            Assert.fail();
        } catch(ClassNotFoundException e) {
            // Ignore; this is expected.
        }

        Bridge.startPAL();
        bridge = Bridge.newInstance("mockShell");
        ActionModel actionModel = bridge.getActionModel();
        Listener listener = new Listener();
        bridge.addActionListener(listener);

        startJvm(Client2.class);

        Thread.sleep(10 * 1000);

        assertNotNull(actionModel.getType(TypeNameFactory.makeName("type1",
                "1.0", "client2")));
        assertNotNull(actionModel.getType(TypeNameFactory.makeName("passthru",
                "1.0", "client2")));

        // Give the other process a chance to complete.
        Thread.sleep(20 * 1000);

        assertEquals(2, procsSeen);
    }

    private void startJvm(Class<?> mainClass)
            throws Exception {
        List<String> args = new ArrayList<String>();

        File javaPath = new File(System.getProperty("java.home"));
        javaPath = new File(javaPath, "bin");
        javaPath = new File(javaPath, "java");
        args.add(javaPath.getPath());

        args.add("-classpath");

        String classpath = System.getProperty("java.class.path");
        File test2_file = new File(TEST2_JAR_PATH);
        assertTrue(test2_file.exists());
        String test2_path = test2_file.getCanonicalPath();
        classpath = classpath + File.pathSeparator + test2_path;
        args.add(classpath);

        String palPort = System.getProperty(JmsSpine.JMS_MESSAGE_BROKER_PORT);
        if (palPort != null) {
            args.add("-D" + JmsSpine.JMS_MESSAGE_BROKER_PORT + "=" + palPort);
        }

        args.add(mainClass.getCanonicalName());

        ProcessBuilder builder = new ProcessBuilder(args);
        File dir = new File(mainClass.getSimpleName());
        dir.mkdirs();
        builder.directory(dir);
        Process process = builder.start();
        Logger log = LoggerFactory.getLogger(mainClass.getSimpleName());
        ProcessMonitor monitor = new ProcessMonitor(process, log, mainClass
                .getSimpleName()
                + ": ");
        monitor.start();
    }

    private static class Listener
            implements GlobalActionListener {
        @Override
        public void actionStarted(ActionStreamEvent action) {
            SimpleTypeName name = action.getDefinition().getName();
            String shortName = name.getSimpleName();
            if (Client2.PROC_NAME.equals(shortName)) {
                procsSeen++;
            }
        }
    }
}
