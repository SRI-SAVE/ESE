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

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.term.ATRNoEvalTerm;
import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.pal.jaxb.ConstraintsType;
import com.sri.pal.jaxb.ObjectFactory;
import com.sri.pal.util.PALBridgeTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Constraints_FuncTest
        extends PALBridgeTestCase {
    private static final Logger log = LoggerFactory
            .getLogger(Constraints_FuncTest.class);

    private static final String ACTION_MODEL = ActionModels.CONSTRAINTS;
    private static final String NAMESPACE = "cns";

    private static ActionDef actDef1;
    private static ActionDef actDef2;
    private static ActionDef actDef3;
    private static ActionDef actDef4;
    private static ActionDef actDef5;
    private static ActionDef actDef6;
    private static ActionDef actDef7;

    @BeforeClass
    public static void setup()
            throws Exception {
        setup(ActionModels.class.getResource(ACTION_MODEL), NAMESPACE);
        SimpleTypeName actName1 = (SimpleTypeName) TypeNameFactory.makeName(
                "action1", "1.0", NAMESPACE);
        actDef1 = (ActionDef) actionModel.getType(actName1);
        actionModel.registerExecutor(actName1, callbackHandler);
        SimpleTypeName actName2 = (SimpleTypeName) TypeNameFactory.makeName(
                "action2", "1.0", NAMESPACE);
        actDef2 = (ActionDef) actionModel.getType(actName2);
        actionModel.registerExecutor(actName2, callbackHandler);
        SimpleTypeName actName3 = (SimpleTypeName) TypeNameFactory.makeName(
                "action3", "1.0", NAMESPACE);
        actDef3 = (ActionDef) actionModel.getType(actName3);
        actionModel.registerExecutor(actName3, callbackHandler);
        SimpleTypeName actName4 = (SimpleTypeName) TypeNameFactory.makeName(
                "action4", "1.0", NAMESPACE);
        actDef4 = (ActionDef) actionModel.getType(actName4);
        actionModel.registerExecutor(actName4, callbackHandler);
        SimpleTypeName actName5 = (SimpleTypeName) TypeNameFactory.makeName(
                "action5", "1.0", NAMESPACE);
        actDef5 = (ActionDef) actionModel.getType(actName5);
        actionModel.registerExecutor(actName5, callbackHandler);
        SimpleTypeName actName6 = (SimpleTypeName) TypeNameFactory.makeName(
                "action6", "1.0", NAMESPACE);
        actDef6 = (ActionDef) actionModel.getType(actName6);
        actionModel.registerExecutor(actName6, callbackHandler);
        SimpleTypeName actName7 = (SimpleTypeName) TypeNameFactory.makeName(
                "action7", "1.0", NAMESPACE);
        actDef7 = (ActionDef) actionModel.getType(actName7);
        actionModel.registerExecutor(actName7, callbackHandler);
   }

    @AfterClass
    public static void teardown()
            throws PALRemoteException {
        palBridge.shutdown();
    }

    private static String jaxbToString(ConstraintsType xmlObj) {
        ObjectFactory of = new ObjectFactory();
        try {
            JAXBElement<ConstraintsType> constraints = of
                    .createConstraints(xmlObj);
            JAXBContext jc = JAXBContext.newInstance(ActionModelType.class
                    .getPackage().getName());
            Marshaller marsh = jc.createMarshaller();
            marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter buffer = new StringWriter();
            marsh.marshal(constraints, buffer);
            return buffer.toString().trim();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to marshal XML string for "
                    + xmlObj, e);
        }
    }

    private void verifyConstraints(ActionDef actDef,
                                   String expConstraintStr,
                                   boolean roundTrip)
            throws Exception {
        ATRNoEvalTerm atrConstraints = actDef.getATRConstraints();
        ConstraintsType xmlConstraints = actDef.getXmlConstraints();

        // expected constraint string
        Assert.assertEquals(expConstraintStr,
                ATRSyntax.toSource(atrConstraints));

        // Is the XML constraints object valid XML?
        JAXBContext jc = JAXBContext.newInstance(ActionModelType.class
                .getPackage().getName());
        Unmarshaller unmar = jc.createUnmarshaller();
        ValidationEventCollector vec = new ValidationEventCollector();
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL schemaUrl = ActionModel.class.getResource(ActionModel.XSD_NAME);
        Schema schema = schemaFactory.newSchema(schemaUrl);
        unmar.setSchema(schema);
        unmar.setEventHandler(vec);
        unmar.unmarshal(new StringReader(jaxbToString(xmlConstraints)));

        // Can we round-trip ATR to XML and back to ATR?
        ATRNoEvalTerm atr2 = palBridge.getActionModelFactory().atrConstraints(
                xmlConstraints, "1.0", NAMESPACE);
        String atrStr1 = ATRSyntax.toSource(atrConstraints);
        String atrStr2 = ATRSyntax.toSource(atr2);
        if (roundTrip) {
            Assert.assertEquals(atrStr2, atrStr1);
        }
    }

    @Test
    public void simpleActionConstraints()
            throws Exception {
        verifyConstraints(actDef1, "`'cns^1.0^isFoo'($arg1)", true);
        verifyConstraints(actDef2, "`()", true);
        verifyConstraints(actDef3,
                "`('cns^1.0^isFoo'($arg1) && 'cns^1.0^isBar'($arg1, $arg2))",
                true);
    }

    // TODO Add a test verifying that we can't load an action model with invalid
    // constraint bindings.

    /**
     * Simple example of coalescing constraints on a procedure.
     */
    @Test
    public void simpleCoalesce()
            throws Exception {
        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();

        ActionInvocation action1 = actDef1.invoke(null, "act1arg1");
        log.debug("{} ATR constraints: {}", actDef1.getName(),
                ATRSyntax.toSource(actDef1.getATRConstraints()));
        log.debug("{} XML constraints: {}", actDef1.getName(),
                actDef1.getConstraints());
        actions.add(action1);

        ActionInvocation action2 = actDef2.invoke(null);
        action2.setValue(0, "act2arg1");
        log.debug("{} ATR constraints: {}", actDef2.getName(),
                ATRSyntax.toSource(actDef2.getATRConstraints()));
        log.debug("{} XML constraints: {}", actDef2.getName(),
                actDef2.getConstraints());
        actions.add(action2);

        ActionInvocation action3 = actDef3.invoke(null, "act3arg1", "act3arg2");
        log.debug("{} ATR constraints: {}", actDef3.getName(),
                ATRSyntax.toSource(actDef3.getATRConstraints()));
        log.debug("{} XML constraints: {}", actDef3.getName(),
                actDef3.getConstraints());
        actions.add(action3);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(actions,
                "simpleCoalesce");
        log.debug("{} ATR constraints: {}", proc.getName(),
                ATRSyntax.toSource(proc.getATRConstraints()));
        log.debug("{} XML constraints: {}", proc.getName(),
                proc.getConstraints());
        log.debug("Learned proc: {}", proc.getSource());
        String expConstraintStr = "`('cns^1.0^isFoo'($arg1_1) && 'cns^1.0^isFoo'($arg1_2) && 'cns^1.0^isBar'($arg1_2, $arg2_3))";
        verifyConstraints(proc, expConstraintStr, true);
    }

    @Test
    public void nestedCoalesce()
            throws Exception {
        ActionInvocation action1 = actDef1.invoke(null, "act1arg1");
        ProcedureDef proc1 = ProcedureLearner.learnAndInvokeProcedure(action1,
                "nested1");
        verifyConstraints(proc1, "`'cns^1.0^isFoo'($arg1_1)", true);

        ProcedureInvocation invoc1 = proc1.invoke(null);
        invoc1.start();
        invoc1.waitUntilFinished();
        ProcedureDef proc2 = ProcedureLearner.learnAndInvokeProcedure(invoc1,
                "super1");
        verifyConstraints(proc2, "`'cns^1.0^isFoo'($arg1_1_1)", true);
    }

    /**
     * Make a procedure from an action with constraints, and an action with no
     * constraint declaration at all. The procedure constraints should be copied
     * from the constrained action.
     */
    @Test
    public void coalesceUnknown()
            throws Exception {
        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();

        ActionInvocation action1 = actDef6.invoke(null);
        action1.setValue(0, new ArrayList<Object>());
        actions.add(action1);

        ActionInvocation action2 = actDef1.invoke(null);
        action2.setValue(0, "foo");
        actions.add(action2);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(actions,
                "coalesceUnknown");
        log.warn("{} ATR constraints: {}", proc.getName(),
                ATRSyntax.toSource(proc.getATRConstraints()));
        log.warn("{} XML constraints: {}", proc.getName(),
                proc.getConstraints());
        log.warn("Learned proc: {}", proc.getSource());
        String expConstraints = "`'cns^1.0^isFoo'($arg1_1)";
        verifyConstraints(proc, expConstraints, true);
    }

    /*
     * TODO Make a procedure that tries to produce a constraint like
     * canModify(first($list)). Currently that should reduce to an empty
     * constraint.
     */

    /**
     * One action has an (unconstrained) output parameter. The value from that
     * is used as input to a constrained action later in the procedure. We
     * should see an <inputUnknown/> in the XML, or a $_ in the ATR.
     */
    @Test
    public void internalParam()
            throws Exception {
        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();

        ActionInvocation action1 = actDef2.invoke(null);
        action1.setValue(0, "foo");
        actions.add(action1);

        ActionInvocation action2 = actDef1.invoke(null, "foo");
        actions.add(action2);

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(actions,
                "internalParam");
        log.debug("Learned proc: {}", proc.getSource());
        log.debug("{} ATR constraints: {}", proc.getName(),
                ATRSyntax.toSource(proc.getATRConstraints()));
        log.debug("{} XML constraints: {}", proc.getName(),
                proc.getConstraints());
        String expConstraints = "`'cns^1.0^isFoo'($arg1_1)";
        verifyConstraints(proc, expConstraints, false);
        Assert.assertTrue(proc.getConstraints().contains("<inputUnknown/>"));
    }

    /**
     * CPAL-269 reports a problem when we try to evaluate a constraint on a
     * procedure which uses a function as a parameter. For example,
     * action1(first($list)) will balk at the use of first().
     */
    /*
     * Temporarily disabled because LAPDOG seems to have changed its
     * generalization behavior. Rather than parameterizing A from [A] with
     * only([A]), it's constructing [A] from A.
     */
    @Test(enabled = false)
    public void constraintOnFunction()
            throws Exception {
        /*
         * We'll demonstrate action4([A]) followed by action1(A). LAPDOG should
         * create a variable $V to hold [A], and then it should use only($V)
         * when it needs to get just A.
         */
        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();

        String item = "A";
        List<String> list = new ArrayList<String>();
        list.add(item);

        // action4([A])
        ActionInvocation action1 = actDef4.invoke(null);
        action1.setValue(0, list);
        actions.add(action1);

        // action1(A)
        ActionInvocation action2 = actDef1.invoke(null, item);
        actions.add(action2);

        ProcedureDef proc = ProcedureLearner.learnProcedure(actions,
                "constraintOnFunction");
        ProcedureLearner.invokeTask(proc, actions, false);
        log.debug("Learned proc: {}", proc.getSource());
        log.debug("{} ATR constraints: {}", proc.getName(),
                ATRSyntax.toSource(proc.getATRConstraints()));
        log.debug("{} XML constraints: {}", proc.getName(),
                proc.getConstraints());
        String expConstraints = "`('cns^1.0^isFoo'($arg1_1) && 'cns^1.0^isFoo'(only($arg1_1)))";
        verifyConstraints(proc, expConstraints, true);

        /*
         * Verify that LAPDOG unified A with [A] by using the only() function.
         * We should have a procedure that takes only 1 input, and "only("
         * should appear in its source.
         */
        Assert.assertEquals(1, proc.size());
        Assert.assertTrue(proc.getSource().contains("only("));
    }

    // TODO Load an action model with an action having a constraint that uses a function.

    /**
     * What happens when we have a constraint on an ungeneralizable parameter?
     * We expect the coalesced constraint for the procedure to contain the same
     * constant value which is passed to the action in the procedure.
     * */
    @Test
    public void ungeneralizableConstraint()
            throws Exception {
        ActionInvocation action1 = actDef5.invoke(null);
        action1.setValue(0, "arg1");
        action1.setValue(1, "arg2");

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(action1,
                "ungeneralizableConstraint");
        log.debug("Learned proc: {}", proc.getSource());
        log.debug("{} ATR constraints: {}", proc.getName(),
                ATRSyntax.toSource(proc.getATRConstraints()));
        log.debug("{} XML constraints: {}", proc.getName(),
                proc.getConstraints());
        String expConstraints = "`'cns^1.0^isBar'(typed(\"arg1\", \"cns^1.0^String\"), $arg2_1)";
        verifyConstraints(proc, expConstraints, false);
    }

    /**
     * Learn a loop in which the loop variable is used in a constrained action.
     * CPAL-276. If a loop contains an action which has a constraint on both a
     * regular variable and a loop variable, then Lumen generates a procedure
     * constraint referencing that loop variable. Constraints shouldn't
     * reference loop variables, since they're not externally visible.
     */
    @Test
    public void loopVariable()
            throws Exception {
        String item1 = "item1";
        String item2 = "item2";
        List<ActionInvocation> actions = new ArrayList<ActionInvocation>();

        ActionInvocation action = actDef6.invoke(null);
        List<String> outputList = new ArrayList<String>();
        outputList.add(item1);
        outputList.add(item2);
        action.setValue(0, outputList);
        actions.add(action);

        for (String item : outputList) {
            action = actDef1.invoke(null);
            action.setValue(0, item);
            actions.add(action);

            action = actDef7.invoke(null);
            List<String> inputList = new ArrayList<String>();
            inputList.add(item);
            action.setValue(0, "foo");
            action.setValue(1, inputList);
            actions.add(action);
        }

        ProcedureDef proc = ProcedureLearner.learnAndInvokeProcedure(actions,
                "loopVariable");
        log.debug("Learned proc: {}", proc.getSource());
        log.debug("{} ATR constraints: {}", proc.getName(),
                ATRSyntax.toSource(proc.getATRConstraints()));
        log.debug("{} XML constraints: {}", proc.getName(),
                proc.getConstraints());
        String expConstraints = "`'cns^1.0^isBar'($arg1_1, $_)";
        verifyConstraints(proc, expConstraints, true);
    }

    // TODO Same action repeated twice with different args. Its constraints
    // should appear twice: once with each arg.

    // TODO Actions with empty constraints. Procedure should have empty
    // constraints.

    /**
     * The demonstration contains an action with null constraints. In other
     * words, unknown constraints rather than no constraints. We get an action
     * with null constraints by loading a procedure with no constraints
     * property.
     * <p>
     * CPAL-330
     */
    @Test
    public void nullConstraints()
            throws Exception {
        /* Get the unconstrained procedure def. */
        ProcedureExecutor sparkExec = palBridge.getPALExecutor();
        URL procUrl = getClass().getResource("unconstrained.xml");
        String procSrc = ProcedureLearner.readWholeFile(procUrl);
        ProcedureDef subProcDef = sparkExec.load(procSrc);
        /* Make sure its constraints are null. */
        Assert.assertNull(subProcDef.getATRConstraints());
        Assert.assertNull(subProcDef.getXmlConstraints());
        Assert.assertNull(subProcDef.getConstraints());

        /* Invoke it, and set the invocation's output params. */
        ProcedureInvocation invoc = subProcDef.invoke(null);
        for (int i = subProcDef.numInputParams(); i < subProcDef.size(); i++) {
            TypeDef fieldType = subProcDef.getParamType(i);
            Object value = VerifiableCallbackHandler.makeBogusValue(fieldType);
            invoc.setValue(i, value);
        }

        /* Now demonstrate a procedure that invokes this one. */
        ProcedureLearner.procedureNotLearned(invoc, "nullConstraints");
    }
}
