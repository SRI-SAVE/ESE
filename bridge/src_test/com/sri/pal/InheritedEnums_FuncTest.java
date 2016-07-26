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

package com.sri.pal;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.util.PALBridgeTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Tests involving inheritance of enumerated types.
 */
public class InheritedEnums_FuncTest
        extends PALBridgeTestCase {
    private static final String NAMESPACE = "ieft";
    private static final String VERSION = "1.0";
    private static Map<String, List<String>> thingTypes;

    private static int procNum = 0;

    @BeforeClass
    public static void setup()
            throws Exception {
        thingTypes = new HashMap<>();
        thingTypes.put("Parmesan", Arrays.asList("cheese", "food", "thing"));
        thingTypes.put("Swiss", Arrays.asList("cheese", "food", "thing"));
        thingTypes.put("chocolateChips", Arrays.asList("food", "thing"));
        thingTypes.put("2%milk", Arrays.asList("milk", "food", "thing"));
        thingTypes.put("muffinTin", Arrays.asList("bakeware", "accessory", "kitchenware", "thing"));
        thingTypes.put("plate", Arrays.asList("dinnerware", "accessory", "kitchenware", "thing"));

        setup(ActionModels.class.getResource(ActionModels.INH_ENUMS), NAMESPACE);

        // Register executors.
        Set<String> types = new HashSet<>();
        for (List<String> theseTypes : thingTypes.values()) {
            types.addAll(theseTypes);
        }
        for (String type : types) {
            String cap = type.substring(0, 1).toUpperCase() + type.substring(1);
            for (String verb : Arrays.asList("make", "use")) {
                String actionStr = verb + cap;
                SimpleTypeName actionName = new SimpleTypeName(actionStr, VERSION, NAMESPACE);
                actionModel.registerExecutor(actionName, callbackHandler);
            }
        }
    }

    @DataProvider(name = "valuesWithTypes")
    public Object[][] valuesWithTypes()
            throws Exception {
        List<Object[]> result = new ArrayList<>();
        for (String value : thingTypes.keySet()) {
            for (String type : thingTypes.get(value)) {
                String cap = type.substring(0, 1).toUpperCase() + type.substring(1);
                String actionStr = "use" + cap;
                SimpleTypeName actionName = new SimpleTypeName(actionStr, VERSION, NAMESPACE);
                result.add(new Object[] { actionName, value });
            }
        }
        return result.toArray(new Object[0][0]);
    }

    /**
     * Demonstrate and execute a procedure using different valid combinations of value and type, where the value is used
     * as an input to a single action.
     */
    @Test(dataProvider = "valuesWithTypes")
    public void useAsInput(SimpleTypeName actionName,
                           String value)
            throws Exception {
        ActionDef actionDef = (ActionDef) actionModel.getType(actionName);
        ActionInvocation action = actionDef.invoke(null, value);

        String procName = "proc" + procNum++;
        ProcedureLearner.learnAndInvokeProcedure(action, procName);
    }

    @DataProvider(name = "makeAndUseWideningTypes")
    public Object[][] makeAndUseWideningTypes() {
        List<Object[]> result = new ArrayList<>();
        for (String value : thingTypes.keySet()) {
            List<String> types = thingTypes.get(value);
            for (int pn = 0; pn < types.size(); pn++) {
                String produceType = types.get(pn);
                String produceCap = produceType.substring(0, 1).toUpperCase() + produceType.substring(1);
                String produceStr = "make" + produceCap;
                SimpleTypeName produceName = new SimpleTypeName(produceStr, VERSION, NAMESPACE);
                for (int cn = pn; cn < types.size(); cn++) {
                    String consumeType = types.get(cn);
                    String consumeCap = consumeType.substring(0, 1).toUpperCase() + consumeType.substring(1);
                    String consumeStr = "use" + consumeCap;
                    SimpleTypeName consumeName = new SimpleTypeName(consumeStr, VERSION, NAMESPACE);
                    result.add(new Object[] { value, produceName, consumeName } );
                }
            }
        }
        return result.toArray(new Object[0][0]);
    }

    /**
     * Test widening conversions. Produce a value of a given type, and consume the same value as a
     * different, more general, type.
     */
    @Test(dataProvider = "makeAndUseWideningTypes")
    public void typeConversions(String value,
                                SimpleTypeName producerName,
                                SimpleTypeName consumerName)
            throws Exception {
        ActionDef produceDef = (ActionDef) actionModel.getType(producerName);
        ActionInvocation produce = produceDef.invoke(null);
        produce.setValue(0, value);

        ActionDef consumeDef = (ActionDef) actionModel.getType(consumerName);
        ActionInvocation consume = consumeDef.invoke(null);
        consume.setValue(0, value);

        List<ActionStreamEvent> actions = Arrays.asList(produce, consume);
        String procName = "widen" + procNum++;
        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(actions, procName);
        Assert.assertEquals(proc.size(), 1);
        Assert.assertEquals(proc.numInputParams(), 0);
    }
}
