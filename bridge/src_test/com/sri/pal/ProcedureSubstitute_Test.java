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

// $Id: ProcedureSubstitute_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;

import com.sri.pal.upgrader.ProcedureUpgrader;
import com.sri.pal.util.PALTestCase;

import org.testng.annotations.Test;

/**
 * Tests for {@link ProcedureUpgrader#substitute}.
 *
 * @author chris
 */
public class ProcedureSubstitute_Test
        extends PALTestCase {
    /**
     * If the target string doesn't occur in the procedure, nothing should
     * change.
     */
    @Test
    public void noOccurrences()
            throws Exception {
        String proc = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<TaskModel version=\""
                + LumenProcedureDef.SERIALIZATION_FORMAT_VERSION
                + "\">\n"
                + "    <bodySource>action 'SPARK.foo1'()"
                + " execute:{"
                + "do 'testApp.foo'();}"
                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                + "</TaskModel>";
        assertEquals(proc, ProcedureUpgrader.substituteConstant(proc, "no such string",
                "foo"));
    }

    /**
     * Target string occurs once. It should be replaced.
     */
    @Test
    public void oneOccurrence()
            throws Exception {
        String proc = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<TaskModel version=\""
                + LumenProcedureDef.SERIALIZATION_FORMAT_VERSION
                + "\">\n"
                + "    <bodySource>action 'SPARK.foo1'()\n"
                + " execute:{\n"
                + "  do 'testApp.foo'(\"thingy\");}\n"
                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                + "</TaskModel>\n";
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<TaskModel version=\""
                + LumenProcedureDef.SERIALIZATION_FORMAT_VERSION
                + "\">\n"
                + "    <bodySource>action 'SPARK.foo1'()"
                + " execute:{"
                + "do 'testApp.foo'(\"stuff\");}"
                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                + "</TaskModel>";
        assertEquals(expected, ProcedureUpgrader.substituteConstant(proc, "thingy",
                "stuff"));
    }

    /**
     * Target string occurs three times.
     */
    @Test
    public void threeOccurrences()
            throws Exception {
        String proc = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<TaskModel version=\""
                + LumenProcedureDef.SERIALIZATION_FORMAT_VERSION
                + "\">\n"
                + "    <bodySource>action 'SPARK.foo1'()\n"
                + " execute:{\n"
                + "  do 'testApp.bar'(\"thingy\");\n"
                + "  do 'testApp.foo'(\"thingy\", \"thingy\");}\n"
                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                + "</TaskModel>\n";
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<TaskModel version=\""
                + LumenProcedureDef.SERIALIZATION_FORMAT_VERSION
                + "\">\n"
                + "    <bodySource>action 'SPARK.foo1'()"
                + " execute:{"
                + "do 'testApp.bar'(\"thing\");"
                + " do 'testApp.foo'(\"thing\", \"thing\");}"
                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                + "</TaskModel>";
        assertEquals(expected, ProcedureUpgrader.substituteConstant(proc, "thingy",
                "thing"));
    }

    /**
     * Even though the string is replaced when it occurs as a constant, it's not
     * replaced where it appears as part of the procedure's name.
     */
    @Test
    public void keepProcName()
            throws Exception {
        String proc = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<TaskModel version=\""
                + LumenProcedureDef.SERIALIZATION_FORMAT_VERSION
                + "\">\n"
                + "    <bodySource>action 'SPARK.foo'()\n"
                + " execute:{\n"
                + "  do 'testApp.bar'(\"foo\");}\n"
                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                + "</TaskModel>\n";
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<TaskModel version=\""
                + LumenProcedureDef.SERIALIZATION_FORMAT_VERSION
                + "\">\n"
                + "    <bodySource>action 'SPARK.foo'()"
                + " execute:{"
                + "do 'testApp.bar'(\"bar\");}"
                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                + "</TaskModel>";
        assertEquals(expected, ProcedureUpgrader.substituteConstant(proc, "foo", "bar"));
    }

    /**
     * The procedure calls an action with the same name as the string to be
     * replaced. Only the constant name should be altered, not the action name.
     */
    @Test
    public void keepActionName()
            throws Exception {
        String proc = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<TaskModel version=\""
                + LumenProcedureDef.SERIALIZATION_FORMAT_VERSION
                + "\">\n"
                + "    <bodySource>action 'SPARK.foo1'()\n"
                + " execute:{\n"
                + "  do 'testApp.foo'(\"foo\");}\n"
                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                + "</TaskModel>\n";
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<TaskModel version=\""
                + LumenProcedureDef.SERIALIZATION_FORMAT_VERSION
                + "\">\n"
                + "    <bodySource>action 'SPARK.foo1'()"
                + " execute:{"
                + "do 'testApp.foo'(\"bar\");}"
                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                + "</TaskModel>";
        assertEquals(expected, ProcedureUpgrader.substituteConstant(proc, "foo", "bar"));
    }

    /**
     * The raw Lumen source is passed in, rather than the XML-wrapped source.
     * This should cause a parse error.
     */
    @Test(expectedExceptions = PALException.class)
    public void badSyntax()
            throws Exception {
        String proc = "action 'SPARK.foo1'()\n"
                + " execute:{\n"
                + "  do 'testApp.foo'(\"foo\");}\n"
                + " argtypes:[]\n"
                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};\n";
        ProcedureUpgrader.substituteConstant(proc, "foo", "bar");
    }
}
