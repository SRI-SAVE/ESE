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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.training.core.exercise.Exercise;

public class ExerciseStorage {
    private static final Logger log = LoggerFactory
            .getLogger(ExerciseStorage.class);
    
    public static final String TRAINING_DIR_PROP_NAME
            = "training.dir";

    public static List<Exercise> getExercises() {
        List<Exercise> exercises = new ArrayList<Exercise>();
        String dirStr = System.getProperty(TRAINING_DIR_PROP_NAME);
        
        if (dirStr == null || dirStr.length() == 0) {
            log.error("No training directory specified. Unable to load exercises.");
            return exercises;
        }
        File exerciseDir = new File(dirStr);
        File[] files = exerciseDir.listFiles();
        
        if (files == null)
            return exercises;
        
        for (File f : files) {
            String name = f.getName();
            if (name.endsWith(".xml")) {
                Exercise e = getExercise(name.substring(0, name.length() - 4));
                if (e != null)
                    exercises.add(e);
            }
        }
        
        return exercises; 
    }
    
    public static Exercise getExercise(String id) {
        try {
            String dirStr = System.getProperty(TRAINING_DIR_PROP_NAME);
            
            String name = id + ".xml";
            Unmarshaller um = ExerciseFactory.getUnmarshaller();
            @SuppressWarnings("unchecked")
            JAXBElement<Exercise> ex = 
                (JAXBElement<Exercise>)um.unmarshal(new File(dirStr + File.separator  + name));            
            return ex.getValue();
        } catch (Exception e) {
            log.error("Error loading exercise '" + id + "'", e);
            return null;
        }
    }
    
    public static Exercise getExerciseFromFile(String id) {
        try {
              
            // String name = id + ".xml";
        	String name = id; 
            Unmarshaller um = ExerciseFactory.getUnmarshaller();
            @SuppressWarnings("unchecked")
            JAXBElement<Exercise> ex = 
                (JAXBElement<Exercise>)um.unmarshal(new File(name));            
            return ex.getValue();
        } catch (Exception e) {
            log.error("Error loading exercise '" + id + "'", e);
            return null;
        }
    }
    
    public static Exercise getExerciseFromURL(URL url) {
        try {
              
            Unmarshaller um = ExerciseFactory.getUnmarshaller();
            @SuppressWarnings("unchecked")
            JAXBElement<Exercise> ex = 
                (JAXBElement<Exercise>)um.unmarshal(url); 
            return ex.getValue();
        } catch (Exception e) {
            log.error("Error loading exercise '" + url + "'", e);
            return null;
        }
    }
    
    public static Exercise getExerciseFromString(String content) {
        try {
              
            Unmarshaller um = ExerciseFactory.getUnmarshaller();
            InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));            
            @SuppressWarnings("unchecked")            
            JAXBElement<Exercise> ex = 
                (JAXBElement<Exercise>)um.unmarshal(stream);            
            return ex.getValue();
        } catch (Exception e) {
            log.error("Error loading exercise from string", e);
            return null;
        }
    }
    
    
}
