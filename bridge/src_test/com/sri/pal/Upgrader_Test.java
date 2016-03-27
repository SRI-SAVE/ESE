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

// $Id: Upgrader_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import com.sri.pal.upgrader.ActionCall;
import com.sri.pal.upgrader.ActionVisitor;
import com.sri.pal.upgrader.ProcedureParam;
import com.sri.pal.upgrader.ProcedureUpgrader;
import com.sri.pal.util.PALTestCase;

import org.testng.Assert;
import org.testng.annotations.Test;

public class Upgrader_Test
        extends PALTestCase {
    /**
     * When we switch the executor namespace from SPARK to lumen, we need to
     * convert all procedures and procedure calls.
     */
    @Test
    public void upgradeSparkToLumen()
            throws Exception {
        File dir = new File("../test/data/upgrader/sparkToLumen");
        FilenameFilter inputFilter = new InputFilter();
        for (File inputFile : dir.listFiles(inputFilter)) {
            String xmlSrc = ProcedureLearner.readWholeFile(inputFile);
            String regex = "^SPARK\\.";
            String repl = "lumen.";
            String newXml = ProcedureUpgrader.substituteTypes(xmlSrc, regex,
                    repl);

            File expectedFile = getExpectedForInput(inputFile);
            String expected = ProcedureLearner.readWholeFile(expectedFile);
            Assert.assertEquals(expected.trim(), newXml.trim());
        }
    }

    /**
     * Convert not only actions, but also types, in the foo namespace to the bar
     * namespace. It should also upgrade the embedded version attribute of the
     * TaskModel element to SparkProcedureDef.SERIALIZATION_FORMAT_VERSION.
     */
    @Test
    public void namespaceFooToBar()
            throws Exception {
        File dir = new File("../test/data/upgrader/fooToBar");
        FilenameFilter inputFilter = new InputFilter();
        for (File inputFile : dir.listFiles(inputFilter)) {
            String xmlSrc = ProcedureLearner.readWholeFile(inputFile);
            String regex = "^foo\\.";
            String repl = "bar.";
            String newXml = ProcedureUpgrader.substituteTypes(xmlSrc, regex,
                    repl);

            File expectedFile = getExpectedForInput(inputFile);
            String expected = ProcedureLearner.readWholeFile(expectedFile);
            Assert.assertEquals(expected.trim(), newXml.trim());
        }
    }

    /**
     * Read the old SPARK-L format procedures and convert them to CTR-S.
     */
    @Test
    public void upgradeSPARKL()
            throws Exception {
        File dir = new File("../test/data/upgrader/sparkl");
        FilenameFilter inputFilter = new InputFilter();
        for (File inputFile : dir.listFiles(inputFilter)) {
            String xmlSrc = ProcedureLearner.readWholeFile(inputFile);
            String newXml = ProcedureUpgrader.upgradeSPARKL(xmlSrc);

            File expectedFile = getExpectedForInput(inputFile);
            String expected = ProcedureLearner.readWholeFile(expectedFile);
            Assert.assertEquals(expected.trim(), newXml.trim());
        }
    }

    /*
     * Test visitActions by making an ActionVisitor that does a particular
     * substitution. Provide a corpus of procedures to be processed, and an
     * expected result for each.
     */
    @Test
    public void visitActions()
            throws Exception {
        TestVisitor visitor = new TestVisitor();
        File dir = new File("../test/data/upgrader/visitor");
        FilenameFilter inputFilter = new InputFilter();
        for (File inputFile : dir.listFiles(inputFilter)) {
            String xmlSrc = ProcedureLearner.readWholeFile(inputFile);
            String newXml = ProcedureUpgrader.visitActions(xmlSrc, visitor);

            File expectedFile = getExpectedForInput(inputFile);
            String expected = ProcedureLearner.readWholeFile(expectedFile);
            Assert.assertEquals(expected.trim(), newXml.trim());
        }
    }

    @Test
    public void getProcedureParameters()
            throws Exception {
        File dir = new File("../test/data/upgrader/parser");
        File inputFile = new File(dir, "input1.xml");
        String xmlSrc = ProcedureLearner.readWholeFile(inputFile);
        List<ProcedureParam> params = ProcedureUpgrader
                .getProcedureParameters(xmlSrc);
        Assert.assertEquals(3, params.size());
        ProcedureParam param1 = params.get(0);
        Assert.assertEquals("entityID_1_1", param1.getVariableName());
        Assert.assertEquals("TESTNS.type128", param1.getType());
        Assert.assertEquals("first($value_2_2)", param1.getDefaultValue());
        ProcedureParam param2 = params.get(1);
        Assert.assertEquals("value_2_2", param2.getVariableName());
        Assert.assertEquals("TESTNS.StringList", param2.getType());
        Assert.assertEquals("\"foo\"", param2.getDefaultValue());
        ProcedureParam param3 = params.get(2);
        Assert.assertEquals("foo", param3.getVariableName());
        Assert.assertEquals("TESTNS.thingy", param3.getType());
        Assert.assertNull(param3.getDefaultValue());
    }

    private File getExpectedForInput(File input) {
        File dir = input.getParentFile();
        String inputName = input.getName();
        String expName = inputName.replaceFirst("input", "expected");
        File expFile = new File(dir, expName);
        return expFile;
    }

    private class InputFilter
            implements FilenameFilter {
        public boolean accept(File dir,
                              String name) {
            return name.startsWith("input");
        }
    }

    private class TestVisitor
            implements ActionVisitor {
        @Override
        public ActionCall visit(ActionCall action) {
            ActionCall result;
            String name = action.getName();
            List<String> args = action.getArgs();
            if (name.equals("foo.openUrl")) {
                if (args.get(0).equals("\"ftp\"")) {
                    name = "foo.openFtpUrl";
                    args.remove(0);
                    result = new ActionCall(name);
                    result.setArgs(args);
                } else if (args.get(0).equals("\"http\"")) {
                    name = "foo.openHttpUrl";
                    args.remove(0);
                    result = new ActionCall(name);
                    result.setArgs(args);
                } else {
                    throw new RuntimeException("Unknown scheme " + args.get(0));
                }
            } else if (name.equals("foo.action1")) {
                String arg = args.remove(1);
                args.add(0, arg);
                result = action;
            } else {
                throw new RuntimeException("Unknown action " + name);
            }

            return result;
        }
    }
}
