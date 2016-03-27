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

// $Id: TypeUtil_Test.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.util;

import java.util.Set;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.tasklearning.spine.impl.jms.util.SpineTestCase;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link TypeUtil}.
 */
public class TypeUtil_Test
        extends SpineTestCase {

    /**
     * TLEARN-500. If a procedure depends on a given type in its parameter list,
     * but none of its actions depend on the type, that type should still appear
     * in the procedure's set of required types.
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    public void parameterRequiredTypes()
            throws Exception {
        String vers = "1.0";
        String ns = "arda";
        String src = "action 'lumen^0.4^3.4Structs'(+'$input tuple 1' = structureGen(\"arda^1.0^parentStruct\", typed(\"some text\", \"string\"), structureGen(\"arda^1.0^tuple\", typed(\"1\", \"integer\"), typed(\"strVal\", \"string\"))), -'$output tuple 1')\n" +
                " argtypes:[\"arda^1.0^parentStruct\", \"arda^1.0^tuple\"]\n" +
                " execute:{\n" +
                "  do 'arda^1.0^UseTuple'(structureGet('$input tuple 1', \"child\", 1));\n" +
                "  do 'arda^1.0^CreateTuple'('$output tuple 1');\n" +
                "  do 'arda^1.0^UseTuple'('$output tuple 1');\n" +
                "  do 'arda^1.0^UseTuple'(structureGen(\"arda^1.0^tuple\", typed(\"1\", \"integer\"), typed(\"strVal\", \"string\")));}\n" +
                " properties:{isTransient:\"true\", metadata:{description:\"\"}}";
        ATRActionDeclaration proc = ATRSyntax.CTR.declFromSource(
                ATRActionDeclaration.class, src);
        Set<TypeName> reqdTypes = TypeUtil.getRequiredTypes(proc);
        Assert.assertEquals(4, reqdTypes.size());
        Assert.assertTrue(reqdTypes.contains(new SimpleTypeName("parentStruct",
                vers, ns)));
        Assert.assertTrue(reqdTypes.contains(new SimpleTypeName("tuple", vers,
                ns)));
        Assert.assertTrue(reqdTypes.contains(new SimpleTypeName("UseTuple",
                vers, ns)));
        Assert.assertTrue(reqdTypes.contains(new SimpleTypeName("CreateTuple",
                vers, ns)));
    }
}
