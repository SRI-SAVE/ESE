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

package com.sri.tasklearning.ui.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.dom.DOMResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.sri.pal.jaxb.TaskType;
import com.sri.pal.training.core.basemodels.EditorDataBase;
import com.sri.pal.training.core.basemodels.ExerciseBase;
import com.sri.pal.training.core.basemodels.ObjectFactory;
import com.sri.pal.training.core.exercise.EqualityConstraint;
import com.sri.pal.training.core.exercise.Exercise;
import com.sri.pal.training.core.exercise.Option;
import com.sri.pal.training.core.exercise.OrderingConstraint;
import com.sri.pal.training.core.exercise.QueryConstraint;
import com.sri.pal.training.core.exercise.Solution;
import com.sri.pal.training.core.exercise.StateConstraint;
import com.sri.pal.training.core.exercise.Step;
import com.sri.pal.training.core.exercise.TaskSolution;
import com.sri.pal.training.core.exercise.TypeConstraint;
import com.sri.pal.training.core.exercise.ValueConstraint;
import com.sri.pal.training.core.storage.ExerciseFactory;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.step.ActionStepModel;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseSubtaskModel;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameter;

/**
 * A helper for reading and writing procedures to New Lumen Format. Also
 * supports reading SPARK-L.
 */

public class StorageAssistant {

    public static final String EXT = "*.procedure";
    public static final String OLD_EXT = "*.spark";
    

    private static final Logger log = LoggerFactory
            .getLogger(StorageAssistant.class);
    
    
    public static boolean saveExerciseFile(ExerciseModel procedure) {
    	
    	File file = procedure.getFileSource();

    	try ( FileWriter writer = new FileWriter(file); ) {

    		String string = renderExercise(procedure);

    		if (string == null)
    			return false;
    		else {
    			writer.write(string);
    			return true; 
    		}

    	} catch (IOException e) {
    		
    		Alert.show("Error saving exercise", 
                    "Error: " + e, 
                    AlertConfig.OK, null);
		
    		return false; 
    	}

    }
    
    public static boolean putExerciseFileHTTP(ExerciseModel procedure) {

    	URL url = procedure.getUrlSource(); 		

    	String string = renderExercise(procedure);

    	if (string == null)
    		return false;
    	else {

    		HttpURLConnection httpCon;
    		try {
    			httpCon = (HttpURLConnection) url.openConnection();
    			
    			String userpass = procedure.getServerUser() + ":" + procedure.getServerPassword();
    			
    			String auth = DatatypeConverter.printBase64Binary(userpass.getBytes("UTF-8"));
    			
    			httpCon.setRequestProperty("Authorization", "Basic " + auth);    			
    			httpCon.setDoOutput(true);
    			httpCon.setRequestMethod("PUT");
    			
    			try (OutputStream os = httpCon.getOutputStream();
    	                OutputStreamWriter osw = new OutputStreamWriter(os, "utf-8");
    	                PrintWriter out = new PrintWriter(osw)) {
    					out.print(string);
    	        }
    			    			
    			log.info("Successully wrote to HTTP PUT stream - URL was " + url + " : Response code is " + httpCon.getResponseCode()); 

    			httpCon.getInputStream();

    			return true; 
    			
    		} catch (IOException e) {
    			
    			Alert.show("Error putting exercise to server", 
                        "Error: " + e, 
                        AlertConfig.OK, null);    		
    			
    			return false; 
    		}    			

    	}    	
    }


    private static String renderExercise(ExerciseModel exerciseModel) {
    
    		StringWriter sw = new StringWriter(); 
    	
    		try {

    			Exercise editorExercise =  exerciseModel.getExercise(); 
    			Solution editorExerciseSolution = editorExercise.getSolution();     			

    			ObjectFactory objectFactory = new ObjectFactory();    			
    			Exercise exercise = new Exercise();  

    			//
    			// preserve original exercise solution in editor data element 
    			// 
    			
    			EditorDataBase editorData = editorExercise.getEditorData(); 

    			if (editorData == null) {
    				editorData = objectFactory.createEditorDataBase();     		
    				JAXBElement<ExerciseBase> originalExercise = objectFactory.createExercise(editorExercise);
    				DOMResult res = new DOMResult();
    				Marshaller marshaller = ExerciseFactory.getMarshaller(); 
        			marshaller.marshal(originalExercise, res);
    			    Document doc = (Document) res.getNode();
    				editorData.getAny().add(doc.getDocumentElement()); 
    			}
    			
    			exercise.setEditorData(editorData);
    			
    			//
    			//
    			// 

    			JAXBElement<ExerciseBase> exerciseSave = objectFactory.createExercise(exercise);
    			
    			exercise.setDescription(editorExercise.getDescription());
    			exercise.setId(editorExercise.getId()); 
    			exercise.setName(editorExercise.getName());
    			exercise.setHtml(editorExercise.getHtml());
    			exercise.setSuccessHtml(editorExercise.getSuccessHtml()); 
    			exercise.setProblem(editorExercise.getProblem()); 
    			exercise.setSetupProcedure(editorExercise.getSetupProcedure()); 
    			exercise.setExtraStepWarnings(editorExercise.isExtraStepWarnings()); 
    			
    			exercise.getDatafile().addAll(editorExercise.getDatafile());
    			
    			if (exerciseModel.getAbsoluteActionModelURL() != null) 
    				exercise.getActionModel().add(exerciseModel.getAbsoluteActionModelURL().toExternalForm());
    			else 
    				exercise.getActionModel().addAll(editorExercise.getActionModel()); 
    			
    			Option option = new Option(); 

    			Solution solution = new Solution();
    			TaskSolution taskSolution = new TaskSolution();  
    			solution.getTaskSolutions().add(taskSolution);                 
    			taskSolution.setOption(option);

    			List<Step> steps = option.getSteps();

    			if (editorExerciseSolution != null ) {

    				if (editorExerciseSolution.getTaskSolutions() != null) {

    					Option editorOption = editorExerciseSolution.getTaskSolutions().get(0).getOption();

    					List<ValueConstraint> vcs = new LinkedList<ValueConstraint>();
    					List<EqualityConstraint> ecs = new LinkedList<EqualityConstraint>();
    					List<TypeConstraint> tcs = new LinkedList<TypeConstraint>();

    					List<QueryConstraint> qcs = editorOption.getQueryConstraints();
    					List<StateConstraint> scs = editorOption.getStateConstraints();    			    				    		
		    				
    					// make sure that all value constraints which are "fresh" that got introduced during
    					// editing get registered... 
    					
    					List<ExerciseStepParameter> params = exerciseModel.getExerciseStepParameters();
    					
    					for (ExerciseStepParameter exStep : params) {
    						ValueConstraint vc = exStep.getValueConstraint();
    						if (vc != null && ! vcs.contains(vc)) {
    							vcs.add(vc);
    						}    						
    					}
    					
    					// make sure that all type constraints which are "fresh" that got introduced during
    					// editing get registered... 
    					
    					for (ExerciseStepParameter exStep : params) {
    						TypeConstraint tc = exStep.getTypeConstraint();
    						if (tc != null && ! tcs.contains(tc)) {
    							tcs.add(tc);
    						}    						
    					}
    					
    					//
    					//
    					// 
    					
    					List<EqualityConstraint> ecs2 = new LinkedList<EqualityConstraint>(); 
    				
    					//
    					//
    					//     					
    					
    					option.getValueConstraints().clear(); 
    					option.getValueConstraints().addAll(vcs);
    					
    					option.getEqualityConstraints().clear();
    					option.getEqualityConstraints().addAll(ecs);

    					option.getTypeConstraints().clear(); 
    					option.getTypeConstraints().addAll(tcs);
    					
    					option.getQueryConstraints().addAll(qcs);
    					option.getStateConstraints().addAll(scs);

    					//
    					// preserve initial (non-edited) create actions
    					//
    					
    					List<ActionStepModel> allFlattenedSteps = new LinkedList<ActionStepModel> (); 
    							    				
    					for (ExerciseStepModel step : exerciseModel.getInitialCreateActions()) {    						
    						steps.add(step.getStep().getStep());    
    						allFlattenedSteps.add(step);						
    					}
    					
    					allFlattenedSteps.addAll(exerciseModel.getFlattenedSteps());     	
    						
    					//
    					// interim code: write ordering constraints that match what is visually displayed in the editor 
    					// basically, this ignores the initial ordering constraints from the file....
    					// 
    					
    					ExerciseStepModel predStep = null; 
    					    					
    					// only the original non-editable ordering constraints get preserved - 
    					// the other editable ordering constraints were deleted and get 
    					// recreated here, in order to induce an ordering on the sequence of XML steps  
    					
    					List<OrderingConstraint> oocs = exerciseModel.getSolutionOption().getOrderingConstraints();
    					List<OrderingConstraint> ocs = option.getOrderingConstraints();
    					
    					ocs.addAll(oocs); 
    				
    					for ( ActionStepModel x : allFlattenedSteps) {
    						
    						ExerciseStepModel succStep = (ExerciseStepModel) x;
    						
    						if (predStep != null) {

    							ContainerStepModel predStepcontainer = predStep.getContainer().getValue(); 
    							ContainerStepModel succStepcontainer = succStep.getContainer().getValue();
    							
    							String predName = predStep.getStep().getStep().getId();
    							String succName = succStep.getStep().getStep().getId();
    							
    							if  ( predStepcontainer.equals(succStepcontainer) ) {
    								
    								boolean found = false; 
    								for (OrderingConstraint oc : oocs ) {
    									
    									if ( oc.getPredecessor().equals(predName) && oc.getSuccessor().equals(succName)) {
    										found = true; 
    										break;
    									}
    								}
    								    							
    								
    								if (! found ) {
    									ocs.add(new OrderingConstraint(predName, succName));
    									
    								}
    							}
    						}
    						
    						predStep = succStep;
    						
    					} 
    					
    					//
    					//
    					//

    					for (ActionStepModel step : allFlattenedSteps) {
    						
    						ExerciseStepModel exStep = (ExerciseStepModel) step;
    						Step step1 = exStep.getStep().getStep();

    						boolean optional = exStep.isOptional();
    						ContainerStepModel container = exStep.getContainer().getValue();    					
    						
    						if (container != null && ( container instanceof ExerciseSubtaskModel )) {
    							step1.setSubtask(container.getName());    					
    						}  else 
    							// important, in order to ungroup steps correctly
    							step1.setSubtask("");

    						step1.setOptional(optional);    				
    						steps.add(step1); 

    					}    			    	

    					//
    					//
    					//

    					exercise.setSolution(solution); 	

    				}
    			}

    			//
    			// create XML 
    			// 


    			Marshaller marshaller = ExerciseFactory.getMarshaller(); 
    		
    			marshaller.marshal(exerciseSave, sw);

    			// log.info("XML output is " + sw);    			    	

    		} catch (Exception e) {
    			Alert.show("Error saving exercise", 
    					"An error occurred while saving this exercise",
    					AlertConfig.OK, null);
    			log.error("Error saving exercise " + exerciseModel.getName(), e);
    			return null;
    			
    		}
    	
    		return sw.toString();     		    		
    		
    }

 

    /**
     * Convert from bridge XML to Lumen strings.
     * 
     * @param sourceXml
     * @return the Spark-l source
     */
    @SuppressWarnings("unchecked")
    public static String stripBridgeXml(String sourceXml) {
        JAXBElement<TaskType> ele;
        try {
            // strip the XML
            JAXBContext jc = JAXBContext.newInstance(TaskType.class
                    .getPackage().getName());
            Unmarshaller unmar = jc.createUnmarshaller();
            StringReader in = new StringReader(sourceXml);
            ele = (JAXBElement<TaskType>) unmar.unmarshal(in);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        TaskType taskXml = (TaskType) ele.getValue();
        String bodySource = taskXml.getBodySource();
        String cueSource = taskXml.getCueSource();

        // figure out if the code is SPARK-L or Lumen, and construct the output
        // accordingly.
        // only SPARK-L files have a cueSource, so we can detect them easily
        if (cueSource != null && cueSource.trim().length() > 0) {
            // various hacky edits for old-school SPARK-L
            bodySource = bodySource.replaceAll("_ns_", ".");
            cueSource = cueSource.replaceAll("_ns_", ".");
            if (cueSource.indexOf("export: ") > 0) {
                // hack the "export:" bit off the end of the cue (and hope it
                // works)
                cueSource = cueSource.substring(0,
                        cueSource.indexOf("export: "));
            }
            log.info("Trimmed Bridge XML from SPARK-L procedure:\n\t{}\n\t{}",
                    bodySource, cueSource);
            return "{bodySource}\n{cueSource}";
        } else {
            // it's Lumen format, so just return the body
            log.info("Trimmed Bridge XML:\n\t{}", bodySource);
            return bodySource;
        }
    }

}
