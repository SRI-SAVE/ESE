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

package com.sri.tasklearning.ui.core.exercise;

import java.io.File;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.sri.ai.lumen.atr.ATRCat;
import com.sri.pal.PALException;
import com.sri.pal.training.core.basemodels.EditorDataBase;
import com.sri.pal.training.core.exercise.Atom;
import com.sri.pal.training.core.exercise.EqualityConstraint;
import com.sri.pal.training.core.exercise.Exercise;
import com.sri.pal.training.core.exercise.Option;
import com.sri.pal.training.core.exercise.OrderingConstraint;
import com.sri.pal.training.core.exercise.Parameter;
import com.sri.pal.training.core.exercise.Step;
import com.sri.pal.training.core.exercise.TaskSolution;
import com.sri.pal.training.core.exercise.ValueConstraint;
import com.sri.pal.training.core.storage.ExerciseFactory;
import com.sri.pal.training.core.storage.ExerciseStorage;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.step.ExerciseCreateStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseGroupOfStepsModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseSubtaskModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameter;

/**
 * Top-level class representing an exercise. Contains references to all
 * steps, inputs, outputs, etc that comprise an exercise through composition. 
 */
public class ExerciseModel extends CommonModel {

	// Functors that are needed to populate a procedure
	public static final String DISPLAY_NAME = "terse_task_description";
	public static final String DESCRIPTION = "description";
			
	static private SimpleObjectProperty<ExerciseModel> activeModel = new SimpleObjectProperty<ExerciseModel>(); 

	private File fileSource;  
	private URL urlSource; 
	private URL absoluteActionModelURL; 	
	
	SimpleBooleanProperty hasFileSourceProperty = new SimpleBooleanProperty(false);
	SimpleBooleanProperty hasURLSourceProperty = new SimpleBooleanProperty(false);
	SimpleBooleanProperty hasOriginalExerciseModelProperty = new SimpleBooleanProperty(false);
	SimpleBooleanProperty readOnlyProperty = new SimpleBooleanProperty(false);
	
	private Map<String, ExerciseStepModel> idToStep = new HashMap<String, ExerciseStepModel>();
	private Map<String, ExerciseStepParameter> idToParam= new HashMap<String, ExerciseStepParameter>();

	private ExerciseEditController controller; 

	// wrap an exercise object:
	private Exercise exercise;
	private Option solutionOption;
	
	// from the exercise stored under editor_data
	private ExerciseModel originalExerciseModel;		
	
	// all params of all steps which are not subparameters
	private List<ExerciseStepParameter> allToplevelParams = new LinkedList<ExerciseStepParameter>();
	// all params of all steps including all the subparameters
	private List<ExerciseStepParameter> allParams = new LinkedList<ExerciseStepParameter>();
	
	private List<OrderingConstraint> orderingConstraints = new LinkedList<OrderingConstraint>();  
	
	// the value and equality constraints which have been removed during editing  (by changing to "Any <Class>") 
	private List<EqualityConstraint> invalidatedEqualityConstraints = new LinkedList<EqualityConstraint>();
	private List<ValueConstraint> invalidatedValueConstraints = new LinkedList<ValueConstraint>();

	// all the create actions
	private List<ExerciseStepModel> initialCreateActions = new LinkedList<ExerciseStepModel>();
	
	// server credentials 
	private String serverUser;
	private String serverPassword;
	
	// mapping parameters to constraints
	private HashMap<String, ValueConstraint> paramToValueconstraintMap;
	private HashMap<String, EqualityConstraint> paramToEqualityconstraintMap;
	
	public List<ExerciseStepModel> getInitialCreateActions() {
		return initialCreateActions;
	}

	private static final Logger log = LoggerFactory
	            .getLogger(ExerciseModel.class);    

	/**
	 * Constructor for existing procedures. Utilized by 
	 * {@link com.sri.tasklearning.ui.core.CoreUIModelFactory}.
	 * 
	 * @param sm the signature of the procedure (inputs/outputs & types info.)
	 * @param taskExpression the steps of the procedure
	 * @param props properties associated with the procedure 
	 */

	
	/**
	 * Constructor for creating new excercises
	 */
	public ExerciseModel(Exercise exercise, File file, URL url, String serverUser, String serverPassword, boolean recursive) {

		super("");
	
		this.fileSource = file; 
		this.urlSource = url; 
		this.exercise = exercise;
		
		updateName();
	
		this.setServerUser(serverUser); 
		this.setServerPassword(serverPassword);

		if (file != null) 
			this.hasFileSourceProperty.setValue(true);
		else if (url != null)
			this.hasURLSourceProperty.setValue(true); 

		com.sri.pal.Bridge bridge = BackendFacade.getInstance().getBridge();

		List<String> actionModels = exercise.getActionModel();

		if (! ( file == null && url == null ) ) 

			for (String actionModel : actionModels) {

				URL am = null;
				String filename = "";
				String ns = ""; 

				try {				

					if (url != null ) {

						am = new URL(url, actionModel);

						filename = Paths.get(am.getFile()).getFileName().toString();
						ns = new File(filename).getName();
						ns = ns.substring(0, ns.lastIndexOf('.'));				

						log.info("Now loading referenced action model " + am + " into namespace " + ns);   

						bridge.getActionModel().load(am, ns);				

						this.setAbsoluteActionModelURL(am);

					} else if (file != null) {

						filename = Paths.get(file.getParent(), new File(actionModel).toPath().toString()).toString();  
						ns = new File(filename).getName();
						ns = ns.substring(0, ns.lastIndexOf('.'));				

						am = new File(filename).toURI().toURL();

						log.info("Now loading referenced action model " + am + " into namespace " + ns);   

						bridge.getActionModel().load(am, ns);

						// don't set this, because the user cannot change location of files otherwise... 

						// this.setAbsoluteActionModelURL(am);					

					}

					log.info("Success!"); 

				} catch (PALException e) {

					log.error("Failed to load action model " + am + " into namespace " + ns);

				} catch (MalformedURLException e) {

					log.error("Failed to load action model - URL "+ am + " is invalid!"); 
				}					
			}

		//
		//
		//

		if ( exercise.getSolution() != null ) {

			List<TaskSolution> solutions = exercise.getSolution().getTaskSolutions(); 

			int pos = 0; 
			boolean orderingConstraintsFound = false;    		

			//
			//
			//         

			for (TaskSolution sol : solutions) {    			

				solutionOption = sol.getOption();

				//
				//
				//

				List<ValueConstraint> vcs = solutionOption.getValueConstraints();

				paramToValueconstraintMap = new HashMap<String, ValueConstraint>(); 

				for (ValueConstraint vc : vcs) 									
					paramToValueconstraintMap.put(vc.getParameter().toString(), vc);

				//
				//
				//        	

				List<EqualityConstraint> ecs = solutionOption.getEqualityConstraints();

				paramToEqualityconstraintMap = new HashMap<String, EqualityConstraint>(); 

				for (EqualityConstraint ec : ecs) {

					if (ec.getParameters().size() == 2) { 					
						paramToEqualityconstraintMap.put(ec.getParameters().get(1), ec); 
					} else {						
						log.error("Found bad equality constraint - ignoring it: " + ec); 						
					}
				}        

				//
				//
				// 

				String lastSubtask = null;  

				ExerciseSubtaskModel subtaskModel = null; 

				int subtaskCounter = 0;
				int stepCounter = 0; 
				int subtaskPosition = 0; 

				for (Step step : solutionOption.getSteps()) {        		

					if (step.isAtom()) {

						Atom atom = step.getAtom();
						String subtask = step.getSubtask();

						String functor = atom.getFunctor(); 
						int pos2 =  atom.getFunctor().lastIndexOf('^'); 

						if (pos2 > -1) {
							functor = functor.substring(pos2+1);  
						}

						//
						//
						//

						if (subtask != null && subtask.isEmpty())
							subtask = null; 

						boolean optional = false;

						try {
							optional = step.isOptional();
						} catch (Exception e1) {
							// if we land here then we have read in an exercise file
							// without optional attribute 
							optional = false;

						} 

						// params contains top level and sub-param parameters   

						List<ExerciseStepParameter> params = processParameters(atom);

						allParams.addAll(params); 

						// 
						// process new subtask 
						//

						if (subtask == null)

							subtaskModel = null;

						else if (lastSubtask == null || ! lastSubtask.equals(subtask)) {

							subtaskCounter++;
							subtaskModel = new ExerciseSubtaskModel(subtask);
							subtaskPosition = 0; 
							addStep(subtaskModel, stepCounter++); 

						}						    

						lastSubtask = subtask;						

						//
						//
						//


						boolean isCreateAction = functor.startsWith("Create");

						ExerciseStepModel stepModel = isCreateAction ? new ExerciseCreateStepModel(atom, params) : new ExerciseStepModel(atom, params);

						stepModel.setOptional(optional);

						idToStep.put(atom.getStep().getId(), stepModel);

						//
						//
						//

						if (isCreateAction) {							
							initialCreateActions.add(stepModel); 							
						} else {
							if (subtaskModel == null) {
								addStep(stepModel, stepCounter++);														
							} else { 						 										
								subtaskModel.addStep(stepModel, subtaskPosition++);
							}							
						}	

					}	
				}

				//
				//
				//

				for (ExerciseStepParameter p : allParams) {
					p.initializeParameterDescription(); 
					// System.out.println(p.getVariableName()); 
				}					

				//
				// assign "Group <number>" default names for unnamed subtasks
				//

				/* 
				for (StepModel unnamedStep : getSteps()) {
					if (unnamedStep instanceof ExerciseSubtaskModel) {
						if (((ExerciseSubtaskModel) unnamedStep).getName().equals("")) { 
							boolean found = true;
							int groupCounter = 1; 
							String name = ""; 
							while (found) {
								found = false; 
								for (StepModel step : getSteps()) {
									name = "Group "+Integer.toString(groupCounter); 
									found = (step instanceof ExerciseSubtaskModel) && (step.getName().equals(name));  
									if (found)
										break;
								}
								groupCounter++; 
							}

							((ExerciseSubtaskModel) unnamedStep).setName(name);
						}
					}
				} */

				//
				//
				// 

				for ( OrderingConstraint oc : solutionOption.getOrderingConstraints()) {

					orderingConstraintsFound = true; 

					String pred = oc.getPredecessor(); 
					String succ = oc.getSuccessor();

					ExerciseStepModel pred1 = idToStep.get(pred); 
					ExerciseStepModel succ1 = idToStep.get(succ);

					if (pred != null && succ != null ) {
						succ1.registerPredecessor(pred1);
					}

					orderingConstraints.add(oc);

				}						

			}

			//
			// auto-add ordering constraint in case there aren't any 
			// 

			/* 
			if (! orderingConstraintsFound) {
				ExerciseStepModel last = null; 
				for ( StepModel step : this.getSteps()) {
					ExerciseStepModel exStep = (ExerciseStepModel) step; 
					if (last != null) {    					
						exStep.registerPredecessor(last);    	
					}
					last = exStep; 

				}	
			} */ 

			//
			// deduce "in any order" attribute for subtasks 
			//

			for (StepModel step : getSteps()) {		

				if (step instanceof ExerciseGroupOfStepsModel) {					

					boolean inAnyOrder = true; 

					ExerciseGroupOfStepsModel group = (ExerciseGroupOfStepsModel) step;

					label: 
						for (StepModel step2 : group.getSteps()) {
							ExerciseStepModel exStep = (ExerciseStepModel) step2; 
							for (ExerciseStepModel predStep : exStep.getPredecessors()) {
								if ( predStep.getContainer().getValue() == group ) { 
									inAnyOrder = false; 
									break label; 
								} else {								

									log.warn("Found bad / non-editiable inter-group ordering constraint: " + predStep.getFunctor() + " < " + exStep.getFunctor());
									// orderingConstraints.add(new OrderingConstraint(predStep.getStep().getStep().getId(), exStep.getStep().getStep().getId()));  

								}
							}						
						}

					group.setInAnyOrder(inAnyOrder);

				}
			}

			//
			// read in original exercise solution
			//

			if ( ! recursive ) {

				EditorDataBase editorData = exercise.getEditorData();

				if (editorData != null) {

					for (Element elem : editorData.getAny()) {

						if (elem.getNodeName().equals("exercise")) {

							JAXBContext jaxbContext;
							try {

								jaxbContext = JAXBContext.newInstance(Exercise.class);
								Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();						

								JAXBElement<Exercise> root = unmarshaller.unmarshal(elem, Exercise.class);

								// for some reason, the following does not work: 
								// I need to go over unmarshal -> string -> marshal via ExerciseStorage 
								// to construct a Exercise instance which really works. Otherwise, I 
								// run into trouble with atoms not having steps for some reason... 

								// Exercise originalExercise = root.getValue();

								StringWriter writer = new StringWriter(); 

								Marshaller marshaller = ExerciseFactory.getMarshaller(); 

								marshaller.marshal(root, writer);

								String content = writer.toString(); 

								Exercise originalExercise = ExerciseStorage.getExerciseFromString(content);

								if (originalExercise != null) {

									originalExerciseModel = new ExerciseModel(originalExercise, null, null, null, null, true);

									if (originalExerciseModel != null) {
										originalExerciseModel.readOnlyProperty.setValue(true); 
										this.hasOriginalExerciseModelProperty.set(true);

										originalExerciseModel.setName("Original " + getName());  

									}
								}			    				

							} catch (JAXBException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
					}
				} else { 

					// no editor data? must be original - make a copy... 

					originalExerciseModel = new ExerciseModel(exercise, null, null, null, null, true);

					if (originalExerciseModel != null) {
						originalExerciseModel.readOnlyProperty.setValue(true); 
						this.hasOriginalExerciseModelProperty.set(true);

						originalExerciseModel.setName("Original " + getName());  

					}

				}

			}

			//
			//
			//

		}

	}

	private List<ExerciseStepParameter> processParameters(Atom atom) {
		
		List<ExerciseStepParameter> params = new LinkedList<ExerciseStepParameter>(); 
		
		for (Parameter p : atom.getParameters()) {      
			
			ExerciseStepParameter par = processParameter(p, null, params);
			
			allToplevelParams.add(par);
			
		}
				
		return params;
	}
	
	private ExerciseStepParameter processParameter(Parameter p, ExerciseStepParameter parent, List<ExerciseStepParameter> params) {
			
		ValueConstraint vc = paramToValueconstraintMap.get(p.getId()); 
		EqualityConstraint ec = paramToEqualityconstraintMap.get(p.getId()); 
		
		ExerciseStepParameter param = new ExerciseStepParameter(this, p, parent, vc, ec);
		
		idToParam.put(p.getId(), param);		
		params.add(param); 
		
		for (Parameter cp : p.getSubParameters())
			processParameter(cp, param, params); 
		
		return param; 
		
	}
	
	public ExerciseModel() {
		super("");
		this.setExercise(exercise);
	} 
	public ExerciseModel(Exercise exercise, File file, String serverUser, String serverPassword) {
		this(exercise, file, null, serverUser, serverPassword, false);		 	
	}

	public ExerciseModel(Exercise exercise, URL url, String serverUser, String serverPassword) {
		this(exercise, null, url, serverUser, serverPassword, false);
	}

	public void updateName() {		

		String fullName = 
				exercise.getName() + "\n" + 
						((fileSource != null) ? fileSource.toString() : ((urlSource != null) ? urlSource.toString() : "Unnamed"));

		this.name.setValue(fullName);
		 
	}
	
	public void updateNameFile() {		

		String fullName = 
				exercise.getName() + "\n" + 
						((fileSource != null) ? fileSource.toString() : "Unnamed");

		this.name.setValue(fullName);
		 
	}
	
	public void updateNameUrl() {		

		String fullName = 
				exercise.getName() + "\n" +
						((urlSource != null) ? urlSource.toString() : "Unnamed");

		this.name.setValue(fullName);
		 
	}
	
	
	public ExerciseEditController getController() {
		return controller;
	}

	public void setController(ExerciseEditController controller) {
		this.controller = controller;
	}  

	public String getDescriptionText() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ATRCat getCategory() {
		return ATRCat.getATRCat(this);
	}

   
	public Exercise getExercise() {
		return exercise;
	}


	public void setExercise(Exercise exercise) {
		this.exercise = exercise;
	}


	public Option getSolutionOption() {
		return solutionOption;
	}


	public void setSolutionOption(Option solutionOption) {
		this.solutionOption = solutionOption;
	}
	
	public static SimpleObjectProperty<ExerciseModel> getActiveModelProperty() {
		return activeModel;
	}

	public static void setActiveModel(ExerciseModel activeModel) {
		
		ExerciseModel.activeModel.setValue(activeModel); 
	}
	
	public List<ExerciseStepParameter> getExerciseStepParameters() {
		
		return allToplevelParams; 
	}

	public Collection<? extends OrderingConstraint> getOrderingConstraints() {
		return orderingConstraints;
	}

	//
	//
	// 
	
	public SimpleBooleanProperty getHasFileSourceProperty() {
		return hasFileSourceProperty;
	}

	public SimpleBooleanProperty getHasURLSourceProperty() {
		return hasURLSourceProperty;
	}
	
	public SimpleBooleanProperty getHasOriginalExerciseModelProperty() {
		return hasOriginalExerciseModelProperty;
	}
	
	public SimpleBooleanProperty getReadOnlyProperty() {
		return readOnlyProperty; 
	}
	
	
	//
	//
	//
	
	public File getFileSource() {
		return fileSource;
	}	

	public void setFileSource(File fileSource) {
		this.fileSource = fileSource;
		this.hasFileSourceProperty.setValue(fileSource != null);
	}
	
	public URL getUrlSource() {
		return urlSource;
	}

	public void setUrlSource(URL urlSource) {
		this.urlSource = urlSource;
		this.hasURLSourceProperty.setValue(urlSource != null);
	}

	public String getServerUser() {
		return serverUser;
	}

	public void setServerUser(String serverUser) {
		this.serverUser = serverUser;
	}

	public String getServerPassword() {
		return serverPassword;
	}

	public void setServerPassword(String serverPassword) {
		this.serverPassword = serverPassword;
	}
	
	public ExerciseStepParameter lookupParameter(String id) {	
		return this.idToParam.get(id); 
	}
		
	public URL getAbsoluteActionModelURL() {
		return absoluteActionModelURL;
	}

	public void setAbsoluteActionModelURL(URL absoluteActionModelURL) {
		this.absoluteActionModelURL = absoluteActionModelURL;
	}

	public boolean equalityConstraintIsInvalid(EqualityConstraint cs) {
		return invalidatedEqualityConstraints.contains(cs);
	}
	
	public void registerEqualityConstraintAsInvalid(EqualityConstraint cs) {
		invalidatedEqualityConstraints.add(cs);
	}
	
	public void registerEqualityConstraintAsValid(EqualityConstraint cs) {
		invalidatedEqualityConstraints.remove(cs);
	}	
	
	public boolean valueConstraintIsInvalid(ValueConstraint cs) {
		return invalidatedValueConstraints.contains(cs);
	}
	
	public void registerValueConstraintAsInvalid(ValueConstraint cs) {
		invalidatedValueConstraints.add(cs);
	}
	
	public void registerValueConstraintAsValid(ValueConstraint cs) {
		invalidatedValueConstraints.remove(cs);
	}

	public ExerciseModel getOriginalExerciseModel() {
		return originalExerciseModel;
	}
	
}
