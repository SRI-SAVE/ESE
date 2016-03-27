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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.net.URL;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sri.pal.actionmodels.ActionModels;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.training.core.exercise.Atom;
import com.sri.pal.training.core.exercise.Option;
import com.sri.pal.training.core.exercise.Parameter;
import com.sri.pal.training.core.exercise.Step;
import com.sri.pal.util.PALTestCase;

public class Learner_FuncTest
        extends PALTestCase {
    private Bridge bridge;
    private ActionModel actionModel;
    private Learner learner;

    @BeforeClass
    public void setup()
            throws Exception {
        Bridge.startPAL();
        bridge = Bridge.newInstance("lft");
        ProcedureLearner.setStorage(getClass(), bridge);
        actionModel = bridge.getActionModel();
        URL url = ActionModels.class.getResource(ActionModels.SIMPLE);
        actionModel.load(url, "ns");
        learner = bridge.getLearner();
    }

    @AfterClass
    public void shutdown() throws PALRemoteException {
        bridge.shutdown();
    }

    @Test
    public void learnSimpleProc()
            throws Exception {
        ActionDef actionDef = (ActionDef) actionModel.getType(TypeNameFactory
                .makeName("action1", "1.0", "ns"));
        ActionInvocation event1 = actionDef.invoke(null);
        event1.setValue(0, "foo");
        ProcedureDef proc = learner.learn("proc1", null, event1);
        assertNotNull(proc);
        assertNotNull(proc.getSource());
        assertEquals(1, proc.size());
    }

    @Test
    public void learnOption()
            throws Exception {
        ActionDef actionDef = (ActionDef) actionModel.getType(TypeNameFactory.makeName("action1","1.0","ns"));
        ActionInvocation event1 = actionDef.invoke(null);
        event1.setValue(0, "bar");
        Option option = learner.learnOption(event1);

        /* To see it as text: */
//        ObjectFactory objectFactory = new ObjectFactory();
//        JAXBElement<OptionBase> je = objectFactory.createOption(option);
//        Marshaller marshaller = ExerciseFactory.getMarshaller();
//        StringWriter sw = new StringWriter();
//        marshaller.marshal(je, sw);
//        Assert.assertEquals("foo", sw);

        List<Step> steps = option.getSteps();
        Assert.assertEquals(steps.size(), 1);
        Step step = steps.get(0);
        Atom atom = step.getAtom();
        Assert.assertNotNull(atom);
        Assert.assertEquals(atom.getFunctor(), actionDef.getName().getFullName());
        List<Parameter> params = atom.getParameters();
        Assert.assertEquals(params.size(), 1);
    }
}
