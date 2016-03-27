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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javafx.application.Platform;
import javafx.stage.Window;
import javafx.util.Callback;

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

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.impl.CTRLiteral;
import com.sri.ai.lumen.core.CoreUtil;
import com.sri.ai.lumen.core.IStructure;
import com.sri.ai.lumen.spark.SPARKLParser;
import com.sri.ai.lumen.spark.SPARKLTranslator;
import com.sri.ai.lumen.syntax.FormatUtil;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.pal.LumenProcedureDef;
import com.sri.pal.PALActionMissingException;
import com.sri.pal.PALException;
import com.sri.pal.ProcedureDef;
import com.sri.pal.TypeStorage;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
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
import com.sri.pal.training.core.exercise.Value;
import com.sri.pal.training.core.exercise.ValueConstraint;
import com.sri.pal.training.core.storage.ExerciseFactory;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.control.Alert.AlertResult;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.library.ActionModelAssistant;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.ActionStepModel;
import com.sri.tasklearning.ui.core.step.ContainerStepModel;
import com.sri.tasklearning.ui.core.step.ExerciseGroupOfStepsModel;
import com.sri.tasklearning.ui.core.step.ExerciseStepModel;
import com.sri.tasklearning.ui.core.step.ProcedureStepModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.term.ExerciseStepParameter;
import com.sri.tasklearning.ui.core.term.TypeUtilities;

/**
 * A helper for reading and writing procedures to New Lumen Format. Also
 * supports reading SPARK-L.
 */

public class StorageAssistant {

    public static final String EXT = "*.procedure";
    public static final String OLD_EXT = "*.spark";
    

    private static final Logger log = LoggerFactory
            .getLogger(StorageAssistant.class);
    
    public static File browseForProcedureFile(Window parent) {
        return Utilities.promptOpenFile(parent, "Open", null, new String[] {
                OLD_EXT, EXT }, "Procedure Files (" + EXT + ", " + OLD_EXT
                + ")", false);
    }
    
   
    public static boolean saveProcedure(String name, ProcedureModel procedure, 
           boolean newProc) {
        return saveProcedureLocal(name, procedure, newProc, true);
    }
    
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


    private static boolean saveProcedureLocal(String name, ProcedureModel procedure,
            boolean newProc, boolean repopulate) {
        if (name.length() > 0 && procedure != null) {
            procedure.setName(name);
            procedure.updateDescriptionInProperties();
            SimpleTypeName typeName = (SimpleTypeName)TypeNameFactory.makeName(procedure.getFunctor());
            try {
                BackendFacade backend = BackendFacade.getInstance();
                ProcedureDef procType = backend.palExecutorLoad(
                                LumenProcedureDef.wrapXml(ATRSyntax
                                        .toSource(procedure)));
                
                backend.storeType(typeName, procType);
                
                if (newProc) {
                    ProcedureStepModel action = new ProcedureStepModel(typeName.getFullName(), null);
                    ActionModelAssistant.getInstance().addAction(action);
                }
            } catch (Exception e) {
                Alert.show("Error saving procedure", 
                           "An error occurred while saving this procedure",
                           AlertConfig.OK, null);
                log.error("Error saving procedure " + procedure.getName(), e);
                return false;
            }
            if (repopulate) {
                ProcedureMap.getInstance().repopulate();
                updateStorageUI(false);
            }
            return true;
        } else
            return false;
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

    					List<ValueConstraint> vcs = editorOption.getValueConstraints();
    					List<EqualityConstraint> ecs = editorOption.getEqualityConstraints();
    					
    					List<QueryConstraint> qcs = editorOption.getQueryConstraints();
    					List<StateConstraint> scs = editorOption.getStateConstraints();    			    				    		

    					List<ValueConstraint> vcs2 = new LinkedList<ValueConstraint>(); 
    					    				
    					// make sure that all value constraints which are "fresh" that got introduced during
    					// editing get registered... 
    					
    					List<ExerciseStepParameter> params = exerciseModel.getExerciseStepParameters();
    					for (ExerciseStepParameter exStep : params) {
    						ValueConstraint vc = exStep.getValueConstraint();
    						if (vc != null && ! vcs.contains(vc)) {
    							// System.out.println("Register a new value constraint ! " + exStep.getButtonLabel() + " : " + vc);
    							vcs.add(vc);
    						}    						
    					}
    				
    					// update the existing value constraints.... 
    					
    					for (ValueConstraint vc : vcs) {
    					
    						if ( !exerciseModel.valueConstraintIsInvalid(vc)) {
    							
    							vcs2.add(vc);    							
    							String param = vc.getParameter();
    							
    							ExerciseStepParameter stepParam = exerciseModel.lookupParameter(param);     							
    							
    							List<CTRLiteral> selected = stepParam.getCurrentSelection();    							
    							List<CTRLiteral> values = stepParam.getValueConstraintLiterals(vc);
    							
    							List<String> selected1 = new LinkedList<String>(); 					
    							List<String> values1 = new LinkedList<String>(); 
    							
    							for (CTRLiteral s : selected)  {    								
    								String type = TypeUtilities.getQualifiedTypeName(stepParam.getTypeDef().getName());    								
    								String ctrs = "typed(\"" +s.getString() + "\", \"" + type + "\")";   
    								selected1.add(ctrs);
    							}
    							
    							for (CTRLiteral s : values) {
    								String type = TypeUtilities.getQualifiedTypeName(stepParam.getTypeDef().getName());    								
									String ctrs = "typed(\"" +s.getString() + "\", \"" + type + "\")";   
    								values1.add(ctrs);
    							}
    							    							
    							List<String> toAdd = new LinkedList<String>(selected1); 
        						List<String> toRemove = new LinkedList<String>(values1);     							
    							
        						toAdd.removeAll(values1);
    							toRemove.removeAll(selected1);
    							
    							/* for (String lit : toAdd)
    								System.out.println("Add " + lit); 
    							
    							for (String lit : toRemove)
    								System.out.println("Remove " + lit); */ 

    							List<Value> newValues = new LinkedList<Value>(); 
        						
    							for (Value val : vc.getValues()) { 
    								try {

    									ATRTerm term = ATRSyntax.CTR.termFromSource(val.getCtrs());

    									if ( term instanceof CTRLiteral ) {
    												
    										CTRLiteral lit = (CTRLiteral) term;
    										    										
    										String type = TypeUtilities.getQualifiedTypeName(stepParam.getTypeDef().getName());    								
    										String ctrs = "typed(\"" + lit.getValue() + "\", \"" + type + "\")";   
    													
    	    								if (toRemove.contains(ctrs)) {    	    									
    	    									// System.out.println("Found value on constraint to remove: " + ctrs + " : " + val);      	    									
    	    								} else    	    									
    	    									newValues.add(val);    										
    									}

    								} catch (LumenSyntaxError e) {
    									e.printStackTrace();
    								}    								
    							}
    							    							
    							for (String add : toAdd) { 
    							
    								String type = TypeUtilities.getQualifiedTypeName(stepParam.getTypeDef().getName());    																	
								
									Value newValue = new Value(add, type); 
									// System.out.println("Created new value to add to constraint: " + add + " : " + newValue);      	
									
									newValues.add(newValue);
    								
    							}
    							
    							vc.getValues().clear();
    							vc.getValues().addAll(newValues);
    						}
    					}
    					
    					List<EqualityConstraint> ecs2 = new LinkedList<EqualityConstraint>(); 
    					for (EqualityConstraint ec : ecs) {    						
    						if ( !exerciseModel.equalityConstraintIsInvalid(ec))
    							ecs2.add(ec);
    					}    			

    					option.getValueConstraints().clear(); 
    					option.getValueConstraints().addAll(vcs2);
    					
    					option.getEqualityConstraints().clear();
    					option.getEqualityConstraints().addAll(ecs2);
    					
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
    					//
    					// 
    					
    					List<ActionStepModel> flattenedSteps = exerciseModel.getFlattenedSteps();     		

    					//
    					// 
    					//
    				
    					/* 
    					List<OrderingConstraint> ocs = option.getOrderingConstraints();

    					for (OrderingConstraint constraint : exerciseModel.getOrderingConstraints()) {

    						String pred = constraint.getPredecessor();
    						String succ = constraint.getSuccessor(); 

    						ExerciseStepModel predStep = null; 
    						ExerciseStepModel succStep = null; 

    						for (ActionStepModel step : flattenedSteps) {

    							ExerciseStepModel exStep = (ExerciseStepModel) step;
    							String stepId = exStep.getStep().getStep().getId();  

    							if (stepId.equals(pred))
    								predStep = exStep;
    							else if (stepId.equals(succ))
    								succStep = exStep;

    							if (predStep != null && succStep != null)
    								break;
    						}

    						if (predStep!= null && succStep != null) { 

    							ContainerStepModel predStepcontainer = predStep.getContainer().getValue();
    							ContainerStepModel succStepcontainer = succStep.getContainer().getValue();

    							boolean preserve = true; 

    							if ((predStepcontainer == succStepcontainer) &&
    									(predStepcontainer instanceof ExerciseGroupOfStepsModel) &&
    									((ExerciseGroupOfStepsModel) predStepcontainer).getInAnyOrder().getValue()) 

    								preserve = false; 

    							if (preserve)    							
    								ocs.add(constraint);

    						} else
    							ocs.add(constraint);
    					}
    					
    					*/
    					
    					//
    					// interim code: write ordering constraints that match what is visually displayed in the editor 
    					// basically, this ignores the initial ordering constraints from the file....
    					// 
    					
    					ExerciseStepModel predStep = null; 
    					List<OrderingConstraint> ocs = option.getOrderingConstraints();

    					for ( ActionStepModel x : allFlattenedSteps) {

    						ExerciseStepModel succStep = (ExerciseStepModel) x;
    						
    						if (predStep != null) {

    							ContainerStepModel predStepcontainer = predStep.getContainer().getValue(); 
    							ContainerStepModel succStepcontainer = succStep.getContainer().getValue();

    							boolean preserve = true; 

    							if (! ( predStepcontainer == succStepcontainer &&  
    									(predStepcontainer instanceof ExerciseGroupOfStepsModel) &&
    									((ExerciseGroupOfStepsModel) predStepcontainer).getInAnyOrder().getValue())) { 

    								ocs.add(new OrderingConstraint(predStep.getStep().getStep().getId(), succStep.getStep().getStep().getId()));  

    							}
    						}
    						
    						predStep = succStep;
    						
    					}

    					//
    					//
    					//

    					for (StepModel step : flattenedSteps) {

    						ExerciseStepModel exStep = (ExerciseStepModel) step;
    						Step step1 = exStep.getStep().getStep();

    						boolean optional = exStep.isOptional();
    						ContainerStepModel container = exStep.getContainer().getValue();

    						if (container != null && (container instanceof ExerciseGroupOfStepsModel)) { 
    							step1.setSubtask(container.getName());    					
    						} else 
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

    public static boolean deleteProcedure(String name) {
        return deleteProcedureLocal(name, true);
    }
    
    private static boolean deleteProcedureLocal(String name, boolean repopulate) {        
        try {
            SimpleTypeName typeName = (SimpleTypeName) TypeNameFactory
                    .makeName(BackendInterface.PROC_VERSIONED_NAMESPACE + name);
            BackendFacade.getInstance().storeType(typeName, null);
            ActionModelAssistant.getInstance().deleteProcedure(name);
            
            if (repopulate) {
                ProcedureMap.getInstance().repopulate();
                updateStorageUI(false); 
            }
        }
        catch (Exception e) {
            log.error("Exception occured while deleting procedure", e);
        }
        return true;
    }
    
    /**
     * Renames pm with the value of newName. The rename will cascade in to other
     * procedures that may have referenced pm as a step. The rename operation 
     * works by doing a standard save of the procedure under a new name and then 
     * deletes the old copy.
     * 
     * @param newName - The new name of the procedure
     * @param pm - The procedure to be renamed
     * @return - whether or not the rename was successful
     */
    
    public static boolean renameProcedure(final String newName, final ProcedureModel pm) {
        final String oldName = pm.getName();
        final boolean nameChanged = !newName.equals(oldName);
        
        saveProcedureLocal(newName, pm, true, false);
        
        if (nameChanged) {
            try {
                BackendFacade bf = BackendFacade.getInstance();

                Set<SimpleTypeName> procs = bf.listTypes(
                        TypeStorage.Subset.PROCEDURE);
                
                final TypeName oldTypeName = TypeNameFactory.makeName(oldName,
                        BackendInterface.PROC_VERSION,
                        BackendInterface.PROC_NAMESPACE);
                
                final TypeName newTypeName = TypeNameFactory.makeName(newName,
                        BackendInterface.PROC_VERSION,
                        BackendInterface.PROC_NAMESPACE);

                // The following procedure loading isn't as inefficient as it 
                // seems. All procedures were loaded when the editor fired up 
                // and should be cached by the bridge. So only new/modified 
                // procedures are coming across the wire.
                for (SimpleTypeName type : procs) {
                    ProcedureModel other = bf.instantiateProcedure(type);
                    boolean referenceUpdated = updateProcedureReferences(other,
                            oldTypeName, newTypeName);
                    
                    if (referenceUpdated)
                        saveProcedureLocal(other.getName(), other, false, false);
                }
            } catch (PALException e) {
                log.error("Exception occurred while renaming procedure", e);
                Alert.show("Error renaming procedure", 
                           "An error occured while updating references to " +
                           "this procedure. Some procedures may be broken.", 
                           AlertConfig.OK, null);
                return false; 
            }
            
            deleteProcedureLocal(oldName, true);
        
            return true;
        }
        
        return false;
    }
    
    private static boolean updateProcedureReferences(
            final ContainerStepModel container,
            final TypeName oldName, 
            final TypeName newName) {        
        boolean ret = false;
        for (StepModel step : container.getSteps()) {
            if (step instanceof ContainerStepModel)
                ret |= updateProcedureReferences((ContainerStepModel)step, oldName, newName);
            else {
                if (step instanceof ProcedureStepModel && 
                    step.getFunctor().equals(oldName.getFullName())) {
                    ((ProcedureStepModel)step).setFunctor(newName.getFullName());
                    ret = true;
                }
            }
        }
        return ret;
    }

    // Given a string, give back the procedure source and bridge XML in Lumen
    // format
    private static SourceAndXml convertToLumenAndXml(String origString) {
        String xmlString;
        String sourceString;

        // If the procedure is wrapped in Bridge XML, strip it off for source
        if (origString.startsWith("<TaskModel")
                || origString.startsWith("<?xml")) {
            sourceString = stripBridgeXml(origString);
            xmlString = origString;
        }
        // If the original procedure is not in Bridge XML, wrap it for xml
        else {
            sourceString = origString;
            try {
                xmlString = LumenProcedureDef.wrapXml(origString);
            } catch (JAXBException e) {
                log.error("Failed to wrap procedure in XML", e);
                return null;
            }
        }

        // If the procedure definition is SPARK-L, convert it
        if (sourceString.indexOf("{defaction") != -1) {
            IStructure<?> lumenStruct = convertSparklToLumen(sourceString);

            try {
                sourceString = FormatUtil.toStringStatement(null, lumenStruct, 2);
                xmlString = LumenProcedureDef.wrapXml(sourceString);
            } catch (Exception e) {
                log.error("Failed to convert SPARK-L", e);
                return null;
            }
        }

        return new SourceAndXml(sourceString, xmlString);
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

    // Convert a SPARK-L string to Lumen format.
    public static IStructure<?> convertSparklToLumen(String procString) {
        String trimmed = procString.trim();

        // see if the procedure definition is SPARK-L
        int i = trimmed.indexOf("{defaction");
        int j = trimmed.indexOf("{defprocedure");

        if (i > 0 || j > 0) {
            // THIS IS SPARK-L!!
            String procBody = "";
            String procCue = "";
            // split the body and cue, and parse the defs
            if (j == 0) {
                procBody = trimmed.substring(0, i);
                procCue = trimmed.substring(i);
            } else if (i == 0) {
                procCue = trimmed.substring(0, j);
                procBody = trimmed.substring(j);
            }

            SPARKLParser<Object> cueParser = new SPARKLParser<Object>(procCue, CoreUtil.FACTORY);
            SPARKLParser<Object> procParser = new SPARKLParser<Object>(procBody, CoreUtil.FACTORY);

            try {
                IStructure<?> lumenProc = SPARKLTranslator
                        .translateDefactionDefprocedure(
                                ((IStructure<?>) cueParser.parseOne()),
                                ((IStructure<?>) procParser.parseOne()));

                return lumenProc;
            } catch (LumenSyntaxError e) {
                log.error("Failed to convert SPARK-L to Lumen", e);
                return null;
            }
        } else
            throw new RuntimeException("Not a SPARK-L string.");
    }
    
    // opens the given spark file, or prompts the user to browse for a file
    public static void importProcedure(Window parent, String filePath) {
    	
    	File file = (filePath == null || filePath.length() == 0) ?
            browseForProcedureFile(parent) : new java.io.File(filePath);        
        
        if (file != null && file.exists()) {
            // open the file
            StringBuilder builder = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) 
                        break;                    
                    builder.append(line);
                }
                reader.close();

                String procString = builder.toString();

                // Get the bridge XML and Lumen source 
                // (will convert SPARK-L if necessary)
                final SourceAndXml xmlSourcePair = convertToLumenAndXml(procString);

                // Store the procedure for use by the type loader
                int beginIdx = xmlSourcePair.source.indexOf("action '") + 8;
                int endIdx = xmlSourcePair.source.indexOf("'", beginIdx);
                final String functor = xmlSourcePair.source.substring(beginIdx,
                        endIdx);
                final String name = functor
                        .substring(functor.lastIndexOf('^') + 1);
                
                final ActionModelAssistant ass = ActionModelAssistant
                        .getInstance();

                if (ass.isProcedureNameInUse(name)) {
                    final Set<String> callers = ProcedureMap.getInstance()
                            .getCallers(name);
                    final boolean usedAsStep = (callers != null && callers
                            .size() > 0) ? true : false;
                    
                    if (usedAsStep) {
                        Callback<AlertResult, Void> call = 
                            new Callback<AlertResult, Void>() {
                            public Void call(AlertResult result) {
                                if (result != AlertResult.YES)
                                    return null;
                                    
                                StringBuffer autoName = new StringBuffer(name);
                                autoName.append("_import");
                                
                                int i = 1;
                                while (ass.isProcedureNameInUse(
                                                (autoName.toString() + i))) {
                                    i++;
                                }
                                autoName.append(i);
                                
                                final String newFunctor = functor.replace(name,
                                        autoName);
                                
                                xmlSourcePair.xml = xmlSourcePair.xml
                                        .replace("action '" + functor + "'",
                                                 "action '" + newFunctor + "'");
                                xmlSourcePair.source = xmlSourcePair.source
                                        .replace("action '" + functor + "'",
                                                 "action '" + newFunctor + "'");
                                doImport(newFunctor, xmlSourcePair, true);
                                
                                return null;
                            }
                        };
                        Alert.show("Procedure name in use", 
                                   "You already have a procedure called '" +
                                   name + "' in your library. Because it is " +
                                   "used as a step it cannot be overwritten. " +
                                   "Would you like to import it with an " +
                                   "auto-generated name?",
                                   AlertConfig.YES_NO, call); 
                    } else {
                        Callback<AlertResult, Void> call = 
                            new Callback<AlertResult, Void>() {
                            public Void call(AlertResult result) {
                                if (result == AlertResult.YES)
                                    doImport(functor, xmlSourcePair, false);
                                return null;
                            }
                        };
                        Alert.show("Procedure name in use", 
                                   "You already have a proceure called '" +
                                   name + "' in your library. Do you want to " +
                                   "overwrite it?",
                                   AlertConfig.YES_NO, call);                        
                    }
                } else
                    doImport(functor, xmlSourcePair, true);
            } catch (IOException e) {
                log.error("Error importing procedure", e);
                Alert.show("Error importing procedure", 
                           "The editor was unable to import a procedure",
                           AlertConfig.OK, null);
            }
        }
    }
    
   
    
    private static void doImport(final String functor, 
                          final SourceAndXml xmlSourcePair,
                          final boolean addToLibrary) {
    	
        try {
            SimpleTypeName typeName = (SimpleTypeName)TypeNameFactory.makeName(functor);    
            BackendFacade backend = BackendFacade.getInstance();
            
            ProcedureDef procType =  backend.palExecutorLoad(xmlSourcePair.xml);
            backend.storeType(typeName, procType);
    
            // TODO: This extra parse to check for errors is a lame hack.
            FormatUtil.parseLumenStatement(xmlSourcePair.source);
    
            if (addToLibrary) {
                ActionStepModel action = new ActionStepModel(typeName.getFullName(), null);
                ActionModelAssistant.getInstance().addAction(action);
            }
            ProcedureMap.getInstance().repopulate();
            updateStorageUI(false);
        } catch (PALActionMissingException e) {
            String typeName = ((SimpleTypeName) e.getMissingType())
                    .getSimpleName();

            Alert.show(
                    "Missing a required dependency",
                    "You are missing the following required dependency: " +
                    "\n\n" + typeName + "\n\n" + 
                    "This could mean that the procedure you're importing " + 
                    "relies on another procedure, or that you're attempting " + 
                    "to import an out-of-date procedure. If the missing " + 
                    "dependency is another procedure then you must import " +
                    "that procedure first.",
                    AlertConfig.OK, null);
            
        } catch (Exception e) {
            log.error("Error importing procedure", e);
            Alert.show("Error importing procedure", 
                       "The editor was unable to import a procedure",
                       AlertConfig.OK, null);
        }
    }

    // save the current procedure out to disk
    public static void exportProcedure(final Window parent, 
                                       final String filePath, 
                                       final ProcedureModel procedure, 
                                       final Boolean includeXmlWrapper) {
        
        final File file = (filePath == null || filePath.length() == 0) ?
            Utilities.promptSaveFile(parent, EXT, true, "Procedure Files (" + EXT + ")") :
            new File(filePath); // or open the given file path
            
        if (file == null)
            return; 
        
        // The dialog window provides a prompt if the user selects an existing
        // file, so we don't need to worry about doing that ourselves. 
        exportProcedureLocal(parent, file, procedure, includeXmlWrapper);
    }

    private static boolean exportProcedureLocal(
            final Window parent, 
            final File file, 
            final ProcedureModel procedure, 
            final boolean includeXmlWrapper) {
        if (file != null) {
            String newProc = ATRSyntax.toSource(procedure);
            FileWriter writer = null;
            try {
                // write the procedure out to file
                writer = new FileWriter(file);
                if (includeXmlWrapper) {
                    // wrap the procedure in Bridge XML
                    String outputXML = LumenProcedureDef.wrapXml(newProc);
                    writer.write(outputXML);
                }
                else 
                    writer.write(newProc);
                
                writer.flush();
                log.info("Saved procedure to {}", file.getCanonicalPath());
                return true;
            } catch (Exception e) {
                log.error("Error saving file", e);
                return false;
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.error("Error closing file writer", e);
                }
            }
        }
        return false;
    }

    // update and manage UIs for persistent storage
    private static final List<IStorageUI> storageUiList = new ArrayList<IStorageUI>();

    public static void registerStorageUI(IStorageUI ui) {
        if (storageUiList.indexOf(ui) < 0)
            storageUiList.add(ui);
    }

    public static void unregisterStorageUI(IStorageUI ui) {
        storageUiList.remove(ui);
    }

    public static void updateStorageUI(boolean runAsync) {
        if (runAsync) {
            Platform.runLater(new Runnable() {
                public void run() {
                    for (IStorageUI ui : storageUiList) 
                        ui.refresh();
                }
            });
        } else {
            for (IStorageUI ui : storageUiList) 
                ui.refresh();
        }
    }
    
    public interface IStorageUI {
        public void refresh();
    }        
    
    private static class SourceAndXml {
        public String source;
        public String xml;
        
        public SourceAndXml(String source, String xml){
            this.source = source;
            this.xml = xml; 
        }      
    }
}
