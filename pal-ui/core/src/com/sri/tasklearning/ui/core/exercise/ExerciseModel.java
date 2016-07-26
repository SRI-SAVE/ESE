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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import com.sri.pal.training.core.exercise.TypeConstraint;
import com.sri.pal.training.core.exercise.ValueConstraint;
import com.sri.pal.training.core.storage.ExerciseFactory;
import com.sri.pal.training.core.storage.ExerciseStorage;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.PalUiException;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.step.ExerciseCreateStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseGroupOfStepsModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseSubtaskModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameter;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

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
	
	private List<OrderingConstraint> fixedOrderingConstraints = new LinkedList<OrderingConstraint>();  
	
	// all the create actions
	private List<ExerciseStepModel> initialCreateActions = new LinkedList<ExerciseStepModel>();
	
	// server credentials 
	private String serverUser;
	private String serverPassword;
	
	// mapping parameters to constraints
	private HashMap<String, ValueConstraint> paramToValueConstraintMap;
	private HashMap<String, EqualityConstraint> paramToEqualityConstraintMap;
	private HashMap<String, TypeConstraint> paramToTypeConstraintMap;
	
	//
	//
	// 
	
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
	 * @throws PALException 
	 * @throws PalUiException 
	 */
	public ExerciseModel(Exercise exercise, File file, URL url, String serverUser, String serverPassword, boolean recursive) throws PALException {

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

					throw e; 

					//					log.error("Failed to load action model " + am + " into namespace " + ns);

				} catch (MalformedURLException e) {

					log.error("Failed to load action model - URL "+ am + " is invalid!"); 
				}					
			}

		//
		//
		//

		if ( exercise.getSolution() != null ) {

			List<TaskSolution> solutions = exercise.getSolution().getTaskSolutions(); 

			//
			//
			//         

			for (TaskSolution sol : solutions) {    			

				solutionOption = sol.getOption();

				//
				//
				//

				List<ValueConstraint> vcs = solutionOption.getValueConstraints();

				paramToValueConstraintMap = new HashMap<String, ValueConstraint>(); 

				for (ValueConstraint vc : vcs) 									
					paramToValueConstraintMap.put(vc.getParameter().toString(), vc);

				//
				//
				//        	

				List<EqualityConstraint> ecs = solutionOption.getEqualityConstraints();

				paramToEqualityConstraintMap = new HashMap<String, EqualityConstraint>(); 

				for (EqualityConstraint ec : ecs) {

					if (ec.getParameters().size() == 2) { 					
						paramToEqualityConstraintMap.put(ec.getParameters().get(0), ec); 
					} else {						
						log.error("Found bad equality constraint - ignoring it: " + ec); 						
					}
				}        

				//
				//
				//        	

				List<TypeConstraint> tcs = solutionOption.getTypeConstraints(); 

				paramToTypeConstraintMap = new HashMap<String, TypeConstraint>(); 

				for (TypeConstraint tc : tcs) {

					if (tc.getType().size() == 1 || tc.getParameter() == null) {				
						paramToTypeConstraintMap.put(tc.getParameter(), tc); 
					} else {						
						log.error("Found bad type constraint - ignoring it: " + tc); 						
					}
				}        

				//
				//
				// 

				Map<String, ExerciseSubtaskModel> subtaskModels = new HashMap<String, ExerciseSubtaskModel>(); 

				ExerciseSubtaskModel subtaskModel = null; 

				List<StepModel> steps = new ArrayList<StepModel>();  

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

						if (subtask == null)  {

							subtaskModel = null;

						} else {

							subtaskModel = subtaskModels.get(subtask);

							if ( subtaskModel == null) {							
								subtaskModel = new ExerciseSubtaskModel(this, subtask);								
								subtaskModels.put(subtask,  subtaskModel); 
								steps.add(subtaskModel);
							}															
						}							

						//
						//
						//


						boolean isCreateAction = functor.startsWith("Create");

						ExerciseStepModel stepModel = isCreateAction ? new ExerciseCreateStepModel(this, atom, params) : new ExerciseStepModel(this, atom, params);

						stepModel.setOptional(optional);

						idToStep.put(atom.getStep().getId(), stepModel);

						//
						//
						//

						if (subtaskModel == null ) {						
							steps.add(stepModel);						  
						} else {							
							subtaskModel.addStep(stepModel, subtaskModel.getStepCount()); 								
						}
					}	
				}

				//
				//
				//

				for (ExerciseStepParameter p : allParams) {
					p.initializeParameterDescription(); 
				}					
			
				//
				//
				// 

				for ( OrderingConstraint oc : solutionOption.getOrderingConstraints()) {

					String pred = oc.getPredecessor(); 
					String succ = oc.getSuccessor();

					ExerciseStepModel pred1 = idToStep.get(pred); 
					ExerciseStepModel succ1 = idToStep.get(succ);

					if (pred != null && succ != null ) {
						succ1.registerPredecessor(pred1, oc.isEditable());
						if (! oc.isEditable())
							fixedOrderingConstraints.add(oc);					
					}
				}	

				//
				// sanity check ordering constraints 
				// 

				for (StepModel step : steps) {

					if (step.isBefore(step)) {
						log.error("Bad ordering constraints: " + step + " is before itself!");
						throw new PALException("Bad ordering constraints given!");
					}
				}

				//
				//
				//

				Comparator<StepModel> comparator = new Comparator<StepModel>() {

					@Override
					public int compare(StepModel o1, StepModel o2) {
						if (o1 == o2)
							return 0;
						else if ( o1.isBefore(o2))
							return -1; 
						else 
							return 1; 
					}					

				};

				steps.sort(comparator);
				int stepCounter = 0; 

				for (StepModel step : steps) {

					if (step instanceof ExerciseCreateStepModel) {							
						initialCreateActions.add((ExerciseStepModel) step); 							
					} else {				
						addStep(step, stepCounter++);														
					}
				}

				//
				// ordering of substeps in groups 
				// 


				for (StepModel group : steps) {

					if (group instanceof ExerciseSubtaskModel) {	

						// make a copy, because of clearSteps().... 
						steps = new LinkedList<StepModel>(((ExerciseSubtaskModel) group).getSteps());

						// sort 
						steps.sort(comparator);

						// reassign sorted sub-steps
						stepCounter = 0; 						
						((ExerciseSubtaskModel) group).clearSteps();	
						for (StepModel step : steps) {									
							((ExerciseSubtaskModel) group).addStep(step, stepCounter++);
						}

					}
				}

				//
				// delete all editable ordering constraints - they get recomputed when saved. 
				// only preserve the non-editiable ones. 
				// 

				solutionOption.getOrderingConstraints().clear();
				solutionOption.getOrderingConstraints().addAll(fixedOrderingConstraints);									

			}	

			//
			// currently, all groups are always unordered 
			// also compute the optional attribute of the group - if all
			// steps in group are optional, set optional on group also,  
			// just to get the icon right. notice that there might be single			
			// steps which are not optional, so...
			// 

			for (StepModel step : getSteps()) {		
				if (step instanceof ExerciseGroupOfStepsModel) {			
					ExerciseGroupOfStepsModel group = ((ExerciseGroupOfStepsModel) step); 
					group.setInAnyOrder(true); 
					boolean allOptional = true; 
					for (StepModel step1 : group.getSteps()) {									
						allOptional &= ((ExerciseStepModel) step1).isOptional(); 
						if ( !allOptional )
							break;
					}
					// System.out.println("Group is optional " + allOptional);
					group.setOptional(allOptional);
				}
			}					

			//
			// read in original exercise solution
			//

			if ( ! recursive ) {

				System.out.println();
				System.out.println("================== READING ORIGINAL ================");

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
										// originalExerciseModel.readOnlyProperty.setValue(true); 
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
						// originalExerciseModel.readOnlyProperty.setValue(true); 
						this.hasOriginalExerciseModelProperty.set(true);

						originalExerciseModel.setName("Original " + getName());  

					}

				}

			}

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
			
		ValueConstraint vc = paramToValueConstraintMap.get(p.getId()); 
		EqualityConstraint ec = paramToEqualityConstraintMap.get(p.getId()); 
		TypeConstraint tc = paramToTypeConstraintMap.get(p.getId()); 
		
		ExerciseStepParameter param = new ExerciseStepParameter(this, p, parent, vc, ec, tc);
		
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
	public ExerciseModel(Exercise exercise, File file, String serverUser, String serverPassword) throws PALException {
		this(exercise, file, null, serverUser, serverPassword, false);		 	
	}

	public ExerciseModel(Exercise exercise, URL url, String serverUser, String serverPassword) throws PALException {
		this(exercise, null, url, serverUser, serverPassword, false);
	}

	public void updateName() {		

		String fullName = 
				exercise.getName() + "\n" + 
						((fileSource != null) ? fileSource.getName().toString() : ((urlSource != null) ? urlSource.toString() : "Unnamed"));

		this.name.setValue(fullName);
		 
	}
	
	public void updateNameFile() {		

		String fullName = 
				exercise.getName() + "\n" + 
						((fileSource != null) ? fileSource.getName().toString() : "Unnamed");

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

	/*
	public Collection<? extends OrderingConstraint> getOrderingConstraints() {
		return orderingConstraints;
	} */

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
	
	//
	//
	//

	public ExerciseModel getOriginalExerciseModel() {
		return originalExerciseModel;
	}

	//
	//
	//
	
	public List<StepModel> getEditableSuccessors(StepModel exerciseStepModel) {
		
		List<StepModel> res = new ArrayList<StepModel>(); 
		int n = indexOf(exerciseStepModel);
		
		if (n+1 < getStepCount()) {
			StepModel succ = getStepNo(n+1);
			if (! getFixedSuccessors(exerciseStepModel).contains(succ)) {
				res.add(succ);
			}
		}
		
		return res; 

	}
	
	public List<StepModel> getFixedSuccessors(StepModel exerciseStepModel) {
		
		return exerciseStepModel.getFixedSuccessors(); 

	}
	
}
