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

package com.sri.pal.training.core.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.sri.pal.training.core.assessment.Assessment;
import com.sri.pal.training.core.assessment.EqualityIssue;
import com.sri.pal.training.core.assessment.ExtraAtomsIssue;
import com.sri.pal.training.core.assessment.MissingAtomIssue;
import com.sri.pal.training.core.assessment.OrderingIssue;
import com.sri.pal.training.core.assessment.QueryIssue;
import com.sri.pal.training.core.assessment.TaskAssessment;
import com.sri.pal.training.core.assessment.ValueIssue;
import com.sri.pal.training.core.basemodels.AssessedResponseIndexesBase;
import com.sri.pal.training.core.basemodels.AssessmentBase;
import com.sri.pal.training.core.basemodels.AtomBase;
import com.sri.pal.training.core.basemodels.ConstraintArgumentBase;
import com.sri.pal.training.core.basemodels.EqualityConstraintBase;
import com.sri.pal.training.core.basemodels.ExerciseBase;
import com.sri.pal.training.core.basemodels.ObjectFactory;
import com.sri.pal.training.core.basemodels.OptionBase;
import com.sri.pal.training.core.basemodels.OptionSetBase;
import com.sri.pal.training.core.basemodels.OrderingConstraintBase;
import com.sri.pal.training.core.basemodels.ParameterBase;
import com.sri.pal.training.core.basemodels.QueryConstraintBase;
import com.sri.pal.training.core.basemodels.ResponseBase;
import com.sri.pal.training.core.basemodels.StateConstraintBase;
import com.sri.pal.training.core.basemodels.StepBase;
import com.sri.pal.training.core.basemodels.TaskBase;
import com.sri.pal.training.core.basemodels.TaskSolutionBase;
import com.sri.pal.training.core.basemodels.ValueConstraintBase;
import com.sri.pal.training.core.exercise.Atom;
import com.sri.pal.training.core.exercise.ConstraintArgument;
import com.sri.pal.training.core.exercise.EqualityConstraint;
import com.sri.pal.training.core.exercise.Exercise;
import com.sri.pal.training.core.exercise.HintSequence;
import com.sri.pal.training.core.exercise.Link;
import com.sri.pal.training.core.exercise.Option;
import com.sri.pal.training.core.exercise.OptionSet;
import com.sri.pal.training.core.exercise.OrderingConstraint;
import com.sri.pal.training.core.exercise.Parameter;
import com.sri.pal.training.core.exercise.Problem;
import com.sri.pal.training.core.exercise.QueryConstraint;
import com.sri.pal.training.core.exercise.Solution;
import com.sri.pal.training.core.exercise.StateConstraint;
import com.sri.pal.training.core.exercise.Step;
import com.sri.pal.training.core.exercise.SubTask;
import com.sri.pal.training.core.exercise.Task;
import com.sri.pal.training.core.exercise.TaskSolution;
import com.sri.pal.training.core.exercise.Value;
import com.sri.pal.training.core.exercise.ValueConstraint;
import com.sri.pal.training.core.response.Response;
import com.sri.pal.training.core.response.SubTaskDelineation;
import com.sri.pal.training.core.response.TaskResponse;
import com.sun.xml.bind.IDResolver;

public class ExerciseFactory extends ObjectFactory {
    private static final Logger log = LoggerFactory.getLogger(ExerciseFactory.class);

    private ExerciseFactory() {
    }

    @Override
    public Exercise createExerciseBase() {
        return new Exercise();
    }

    @Override
    public Problem createProblemBase() {
        return new Problem();
    }

    @Override
    public Solution createSolutionBase() {
        return new Solution();
    }

    @Override
    public SubTask createSubTaskBase() {
        return new SubTask();
    }
    
    @Override
    public HintSequence createHintSequenceBase() {
        return new HintSequence();
    }
    
    @Override
    public Link createLinkBase() {
        return new Link();
    }

    @Override
    public TaskBase createTaskBase() {
        return new Task();
    }

    @Override
    public TaskSolutionBase createTaskSolutionBase() {
        return new TaskSolution();
    }

    @Override
    public OptionSetBase createOptionSetBase() {
        return new OptionSet();
    }

    @Override
    public OptionBase createOptionBase() {
        return new Option();
    }

    @Override
    public StepBase createStepBase() {
        return new Step();
    }

    @Override
    public AtomBase createAtomBase() {
        return new Atom();
    }

    @Override
    public ParameterBase createParameterBase() {
        return new Parameter();
    }

    @Override
    public Value createValueBase() {
        return new Value();
    }

    @Override
    public ValueConstraintBase createValueConstraintBase() {
        return new ValueConstraint();
    }

    @Override
    public EqualityConstraintBase createEqualityConstraintBase() {
        return new EqualityConstraint();
    }

    @Override
    public OrderingConstraintBase createOrderingConstraintBase() {
        return new OrderingConstraint();
    }

    @Override
    public QueryConstraintBase createQueryConstraintBase() {
        return new QueryConstraint();
    }
    
    @Override
    public StateConstraintBase createStateConstraintBase() {
        return new StateConstraint();
    }    

    @Override
    public ConstraintArgumentBase createConstraintArgumentBase() {
        return new ConstraintArgument();
    }

    @Override
    public ResponseBase createResponseBase() {
        return new Response();
    }

    @Override
    public TaskResponse createTaskResponseBase() {
        return new TaskResponse();
    }

    @Override
    public SubTaskDelineation createSubTaskDelineationBase() {
        return new SubTaskDelineation();
    }

    @Override
    public Assessment createAssessmentBase() {
        return new Assessment();
    }

    @Override
    public AssessedResponseIndexesBase createAssessedResponseIndexesBase() {
        return new AssessedResponseIndexesBase();
    }

    @Override
    public TaskAssessment createTaskAssessmentBase() {
        return new TaskAssessment();
    }

    @SuppressWarnings("deprecation")
    @Override
    public QueryIssue createQueryIssueBase() {
        return new QueryIssue();
    }

    @Override
    public ExtraAtomsIssue createExtraAtomsIssueBase() {
        return new ExtraAtomsIssue();
    }

    @Override
    public EqualityIssue createEqualityIssueBase() {
        return new EqualityIssue();
    }

    @Override
    public MissingAtomIssue createMissingAtomIssueBase() {
        return new MissingAtomIssue();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ValueIssue createValueIssueBase() {
        return new ValueIssue();
    }

    @Override
    public OrderingIssue createOrderingIssueBase() {
        return new OrderingIssue();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResponseBase }
     * {@code >}
     * 
     */
    @XmlElementDecl(namespace = "", name = "response")
    public static JAXBElement<Response> createResponse(Response value) {
        return new JAXBElement<Response>(new QName("", "response"), Response.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExerciseBase }
     * {@code >}
     * 
     */
    @XmlElementDecl(namespace = "", name = "exercise")
    public static JAXBElement<Exercise> createExercise(Exercise value) {
        return new JAXBElement<Exercise>(new QName("", "exercise"), Exercise.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AssessmentBase }
     * {@code >}
     * 
     */
    @XmlElementDecl(namespace = "", name = "assessment")
    public static JAXBElement<Assessment> createAssessment(Assessment value) {
        return new JAXBElement<Assessment>(new QName("", "assessment"), Assessment.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExerciseBase }
     * {@code >}
     * 
     */
    @XmlElementDecl(namespace = "", name = "task_assessment")
    public static JAXBElement<TaskAssessment> createTaskAssessment(TaskAssessment value) {
        return new JAXBElement<TaskAssessment>(new QName("", "task_assessment"), TaskAssessment.class, null, value);
    }

    private static Unmarshaller.Listener listener = new Listener() {
        @SuppressWarnings("deprecation")
        @Override
        public void afterUnmarshal(Object target, Object parent) {
            if (target instanceof Option && parent instanceof OptionSet)
                ((Option) target).setOptionSet((OptionSet) parent);
            else if (target instanceof Step && parent instanceof Option)
                ((Step) target).setOption((Option) parent);
            else if (target instanceof Step && parent instanceof TaskSolution)
                ((Step) target).setTaskSolution((TaskSolution) parent);
            else if (target instanceof Atom)
                ((Atom) target).setStep((Step) parent);
            else if (target instanceof OptionSet && parent instanceof Step)
                ((OptionSet) target).setStep((Step) parent);
        }
    };

    public static Unmarshaller getUnmarshaller() {
        try {
            JAXBContext c = JAXBContext.newInstance(ExerciseBase.class.getPackage().getName());
            Unmarshaller um = c.createUnmarshaller();
            um.setListener(listener);
            um.setProperty("com.sun.xml.bind.ObjectFactory", new ExerciseFactory());
            um.setProperty(IDResolver.class.getName(), new MyIDResolver());
            /*
             * TODO This causes LoadTest.roundTrip() to throw exceptions trying
             * to unmarshal.
             */
//             SchemaFactory sf = SchemaFactory
//             .newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
//             Schema schema = sf.newSchema(ObjectFactory.class
//             .getResource(("training.xsd")));
//             um.setSchema(schema);
            return um;
        } catch (Exception e) {
            log.error("Failed to create Exercise unmarshaller", e);
            return null;
        }
    }

    public static Marshaller getMarshaller() {
        try {
            JAXBContext c = JAXBContext.newInstance(ExerciseBase.class.getPackage().getName());
            Marshaller m = c.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            /*
             * TODO This causes LoadTest.roundTrip() to throw exceptions trying
             * to marshal.
             */
            // SchemaFactory sf = SchemaFactory
            // .newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
            // Schema schema = sf.newSchema(ObjectFactory.class
            // .getResource(("training.xsd")));
            // m.setSchema(schema);
            return m;
        } catch (Exception e) {
            log.error("Failed to create Exercise marshaller", e);
            return null;
        }
    }

    private static class MyIDResolver extends IDResolver {
        private Map<Class<?>, Map<String, Object>> map = new HashMap<Class<?>, Map<String, Object>>();

        @Override
        public void startDocument(ValidationEventHandler eventHandler) {
            map.clear();
        }

        @Override
        public void bind(String id, Object obj) throws SAXException {
            Map<String, Object> submap;
            if ((submap = map.get(obj.getClass())) == null) {
                submap = new HashMap<String, Object>();
                map.put(obj.getClass(), submap);
            }

            if (submap.get(id) != null)
                throw new SAXException("Key collision on class " + obj.getClass());

            submap.put(id, obj);
        }

        @Override
        public Callable<?> resolve(final String id, @SuppressWarnings("rawtypes") final Class clazz)
                throws SAXException {
            Callable<?> call = new Callable<Object>() {
                public Object call() {
                    String prefix = id.substring(0, id.indexOf("-"));
                    Object obj = null;
                    if (prefix.equals("task")) {
                        obj = map.get(Task.class).get(id);
                    } else if (prefix.equals("step")) {
                        obj = map.get(Step.class).get(id);
                    } else if (prefix.equals("param")) {
                        obj = map.get(Parameter.class).get(id);
                    } else {
                        throw new RuntimeException("Can't resolve id '" + id
                                + "' as any known class");
                    }

                    if (obj == null) {
                        throw new RuntimeException("Unable to resolve id '"
                                + id + "'");
                    }

                    return obj;
                }
            };
            return call;
        }
    }
}
