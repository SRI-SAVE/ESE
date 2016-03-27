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

// $Id: DeeplyNestedTypes_FuncTest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.pal.jaxb.ActionType;
import com.sri.pal.jaxb.BagType;
import com.sri.pal.jaxb.CustomType;
import com.sri.pal.jaxb.EnumType;
import com.sri.pal.jaxb.ListType;
import com.sri.pal.jaxb.MemberType;
import com.sri.pal.jaxb.NullableType;
import com.sri.pal.jaxb.ObjectFactory;
import com.sri.pal.jaxb.ParamType;
import com.sri.pal.jaxb.SetType;
import com.sri.pal.jaxb.StructMemberType;
import com.sri.pal.jaxb.StructType;
import com.sri.pal.jaxb.TypeRef;
import com.sri.pal.jaxb.TypeType;
import com.sri.pal.util.PALBridgeTestCase;
import com.sri.tasklearning.spine.messages.contents.ActionCategory;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Ensure that we can handle all permutations of data types when they are deeply
 * nested within each other. For example, a list of sets of tuples.
 *
 * @author chris
 */
public class DeeplyNestedTypes_FuncTest
        extends PALBridgeTestCase {
    private static final int NESTING_DEPTH = 3;
    private static final String NS = "dnt";
    private static final String AM_VERS = "1.0";
    private static final String PRODUCE = "produce";
    private static final String CONSUME = "consume";
    private static List<String> generatedTypeNames = new ArrayList<String>();

    private static final Class<?> containerTypes[] = new Class<?>[] {
            BagDef.class, ListDef.class, NullableDef.class, SetDef.class,
            StructDef.class };

    @BeforeClass
    public static void setup()
            throws Exception {
        File amFile = buildActionModel(NESTING_DEPTH);

        URL amUrl = amFile.toURI().toURL();
        setup(amUrl, NS);
    }

    /**
     * Build an action model file which has all those permutations up to a
     * certain nesting level.
     *
     * @param depth
     *            depth of nesting of data types
     * @return the file containing the generated action model
     */
    private static File buildActionModel(int depth)
            throws Exception {
        File amFile = File.createTempFile("nested", ".xml", new File("."));
        PrintWriter writer = new PrintWriter(new FileWriter(amFile));
        try {
            ActionModelType amXml = new ActionModelType();
            amXml.setVersion(AM_VERS);
            List<String> thisLevelTypes = new ArrayList<String>();
            for (int i = 0; i <= depth; i++) {
                List<String> newTypes = addTypes(amXml, thisLevelTypes);
                generatedTypeNames.addAll(newTypes);
                thisLevelTypes = newTypes;
            }
            JAXBContext jc = JAXBContext.newInstance(ActionModelType.class
                    .getPackage().getName());
            Marshaller marsh = jc.createMarshaller();
            marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            ObjectFactory objectFactory = new ObjectFactory();
            JAXBElement<ActionModelType> actionModel = objectFactory
                    .createActionModel(amXml);
            marsh.marshal(actionModel, writer);
        } finally {
            writer.close();
        }
        return amFile;
    }

    /**
     * Creates another nesting level of types which contain the types listed in
     * subTypes. For each type foo in subTypes, this method will create: list of
     * foo, bag of foo, set of foo, etc.
     */
    private static List<String> addTypes(ActionModelType am,
                                         List<String> subTypes) {
        List<String> result = new ArrayList<String>();

        if (subTypes.isEmpty()) {
            // Create a custom base type
            List<TypeType> baseTypes = new ArrayList<TypeType>();
            TypeType customType = new TypeType();
            customType.setId("customType");
            CustomType cust = new CustomType();
            cust.setJavaType(CustomType1.class.getName());
            customType.setCustom(cust);
            baseTypes.add(customType);

            // Create an enum base type
            TypeType enumType = new TypeType();
            enumType.setId("enumType");
            EnumType enu = new EnumType();
            enu.getValue().add("Green");
            enu.getValue().add("Orange");
            enu.getValue().add("Red");
            enumType.setEnum(enu);
            baseTypes.add(enumType);

            for (TypeType base : baseTypes) {
                base.setDescription("Base type which is contained by others.");
                addType(am, base);
                result.add(base.getId());
            }

            // Add builtins
            for (PrimitiveTypeDef.Predefined prim :
                     PrimitiveTypeDef.Predefined.values()) {

                String name = prim.name().toLowerCase();
                addAction(am, name, PRODUCE);
                addAction(am, name, CONSUME);

                result.add(name);
            }
        } else {
            for (String subTypeName : subTypes) {
                for (Class<?> containerType : containerTypes) {
                    TypeType type = new TypeType();
                    String name;
                    MemberType member = new MemberType();
                    member.setTypeRef(subTypeName);
                    if (containerType.equals(BagDef.class)) {
                        name = "bag" + subTypeName;
                        type.setDescription("Bag of " + subTypeName);
                        BagType bag = new BagType();
                        bag.setRef(member);
                        type.setBag(bag);
                    } else if (containerType.equals(ListDef.class)) {
                        name = "list" + subTypeName;
                        type.setDescription("List of " + subTypeName);
                        ListType list = new ListType();
                        list.setRef(member);
                        type.setList(list);
                    } else if (containerType.equals(NullableDef.class)) {
                        name = "nullable" + subTypeName;
                        type.setDescription("Nullable " + subTypeName);
                        NullableType nul = new NullableType();
                        nul.setRef(member);
                        type.setNullable(nul);
                    } else if (containerType.equals(SetDef.class)) {
                        name = "set" + subTypeName;
                        type.setDescription("Set of " + subTypeName);
                        SetType set = new SetType();
                        set.setRef(member);
                        type.setSet(set);
                    } else if (containerType.equals(StructDef.class)) {
                        name = "tuple" + subTypeName;
                        type.setDescription("Tuple of " + subTypeName);
                        StructType struct = new StructType();
                        struct.setOpaque(false);
                        StructMemberType structMember = new StructMemberType();
                        structMember.setTypeRef(member.getTypeRef());
                        struct.getRef().add(structMember);
                        type.setStruct(struct);
                    } else {
                        throw new RuntimeException("Unknown type "
                                + containerType);
                    }
                    type.setId(name);
                    addType(am, type);
                    result.add(name);
                }
            }
        }

        return result;
    }

    /**
     * Adds the type to the action model, and adds a producer action and a
     * consumer action as well.
     */
    private static void addType(ActionModelType am,
                                TypeType type) {
        am.getType().add(type);
        String name = type.getId();

        addAction(am, name, PRODUCE);
        addAction(am, name, CONSUME);
    }

    /**
     * Adds an action to the action model. It's either a producer or a consumer,
     * as determined by the modality param.
     */
    private static void addAction(ActionModelType am,
                                  String name,
                                  String modality) {
        ActionType action = new ActionType();
        action.setId(modality + name);
        action.setDescription(modality + " an object of type " + name);
        action.setCategory(ActionCategory.EFFECTOR.getName());
        ParamType param = new ParamType();
        param.setId("param1");
        param.setDescription(name + " object");
        TypeRef typeRef = new TypeRef();
        typeRef.setTypeId(name);
        param.setTypeRef(typeRef);
        if (modality.equals(PRODUCE)) {
            action.getOutputParam().add(param);
        } else {
            action.getInputParam().add(param);
        }
        am.getAction().add(action);
    }

    @Test(timeOut = 900000)
    public void testAllTypes()
            throws Exception {
        // Iterate over the list of data types.
        for (String name : generatedTypeNames) {
            // Get the data type ID, as well as IDs for an action that produces
            // it and another that consumes it.
            SimpleTypeName dataTypeName = (SimpleTypeName) TypeNameFactory
                    .makeName(name, AM_VERS, NS);
            SimpleTypeName producerName = (SimpleTypeName) TypeNameFactory
                    .makeName(PRODUCE + name, AM_VERS, NS);
            SimpleTypeName consumerName = (SimpleTypeName) TypeNameFactory
                    .makeName(CONSUME + name, AM_VERS, NS);

            actionModel.registerExecutor(producerName, callbackHandler);
            actionModel.registerExecutor(consumerName, callbackHandler);

            TypeDef dataType = (TypeDef) actionModel.getType(dataTypeName);
            TypeDef baseType = dataType;
            int nesting = 0;
            while (baseType instanceof CollectionTypeDef ||
                   baseType instanceof StructDef) {
                if (baseType instanceof CollectionTypeDef)
                    baseType = ((CollectionTypeDef)baseType).getElementType();
                else
                    baseType = ((StructDef)baseType).getFieldType(0);
                nesting++;
            }

            // In order to prevent this test from taking forever, we only test
            // custom types past 1 level of nesting. In this way, we assume that
            // if set<string> is working, that list<bag<set<string>>> is also
            // working, if list<bag<set<CustomTypeDef>>> is working.
            if (!(baseType instanceof CustomTypeDef) && nesting > 1)
                continue;

            ActionDef producerDef = (ActionDef) actionModel
                    .getType(producerName);
            ActionDef consumerDef = (ActionDef) actionModel
                    .getType(consumerName);

            // Create an object of that type. At the bottom level, it contains
            // one or more instances of our custom type. We can make sure an
            // object was actually created by querying the custom type's class.
            int serial = 0;
            if (dataType instanceof CustomTypeDef)
                serial = CustomType1.getSerial();

            Object dataObject = VerifiableCallbackHandler
                    .makeBogusValue(dataType);

            if (dataType instanceof CustomTypeDef) {
                Assert.assertTrue(CustomType1.getSerial() > serial, name);
            }

            // Create invocations of the producer and consumer object.
            List<ActionInvocation> actions = new ArrayList<ActionInvocation>();
            ActionInvocation produceAct = producerDef.invoke(null);
            produceAct.setValue(0, dataObject);
            actions.add(produceAct);
            ActionInvocation consumeAct = consumerDef.invoke(null);
            consumeAct.setValue(0, dataObject);
            actions.add(consumeAct);

            if (dataType instanceof CustomTypeDef)
                serial = CustomType1.getSerial();

            // Learn and invoke a procedure which produces one of those objects
            // and then consumes it. As above, ensure that the low-level custom
            // type object was actually generated.
            ProcedureLearner.learnAndInvokeProcedure(actions, name + "proc");

            if (dataType instanceof CustomTypeDef)
                Assert.assertTrue(CustomType1.getSerial() > serial, name);
        }
    }

    /**
     * A functional test that insures instances of all types generated by
     * makeTypes can be successfully translated to and from their ATR
     * representations.
     *
     * @throws PALException
     */
    @Test
    public void toAtrFromAtrRoundTripTest() throws PALException {
        for (String name : generatedTypeNames) {
            SimpleTypeName dataTypeName = (SimpleTypeName) TypeNameFactory
                    .makeName(name, AM_VERS, NS);
            TypeDef dataType = (TypeDef) actionModel.getType(dataTypeName);

            Object orig = VerifiableCallbackHandler
                    .makeBogusValue(dataType);

            ATRTerm atr = dataType.toAtr(orig);
            Object roundTripped = dataType.fromAtr(atr);

            Assert.assertEquals(orig, roundTripped);

            // Can't do the following part yet because CTR doesn't implement
            // equals universally.

            //ATRTerm atrFromCopy = dataType.toAtr(roundTripped);
            //Assert.assertEquals(atr, atrFromCopy);
        }
    }
}
