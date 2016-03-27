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
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import com.sri.tasklearning.ui.core.resources.ResourceLoader;

/**
 * Dumping grounds for static utility methods that don't seem to belong anywhere
 * else. 
 */

public final class Utilities {
    private static Map<String, Image> imageCache = new HashMap<String, Image>();
    private static Properties properties = new Properties(
            Properties.DEFAULT_FILE_NAME);
    private static String lastBrowsedDirectory = (String) properties.getValue(
            "browseDir", null);
    
    private Utilities() {}

    private static void setLastBrowsedDirectory(String dir) {
        lastBrowsedDirectory = dir;
        properties.putValue("browseDir", dir);
        properties.saveProperties();        
    }
    // load an image from Resources
    public static Image getImage(String path) {
        if (!imageCache.containsKey(path)) {        	
        	Image img = new Image(ResourceLoader.getStream(path));                      
            imageCache.put(path, img);
        }
        
        return imageCache.get(path);
    }
    
    public static Image getImage(URL url) {
        try {
            if (!imageCache.containsKey(url.getPath())) {
                Image img = new Image(url.openStream());
                imageCache.put(url.getPath(), img);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(url.getPath(), e);
        }

        return imageCache.get(url.getPath());
    }
    
    // load an image from Resources
    public static ImageView getImageView(String filename) {
        return new ImageView(getImage(filename));
    }    

    // show a Save File dialog and prompt the user to save
    public static File promptSaveFile(
            final Window parent, 
            final String extension, 
            final boolean forceExtension,
            final String fileDescription) {     
        String[] ext = { extension };
        
        FileChooser fileChooser = new FileChooser();       
        
        if (lastBrowsedDirectory != null && new File(lastBrowsedDirectory).exists()) 
            fileChooser.setInitialDirectory(new File(lastBrowsedDirectory));
        
        fileChooser.setTitle("Save");
        if (extension != null) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(fileDescription, ext));
        }

        File retval = null;
        File file = null;
        if ((file = fileChooser.showSaveDialog(parent)) != null) {
            if (extension != null) {
                // Strip off leading wildcard
            
                String sExt = ext[0].substring(1);
                // append extension if needed
                if (forceExtension
                        && !file.getName().toLowerCase().endsWith(sExt)) {
                    try {
                        file = new java.io.File(file.getAbsolutePath() + sExt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            setLastBrowsedDirectory(file.getParent());
            retval = file;            
        }
        return retval;
    }    

    public static File promptOpenFile(
            final Window parent,
            final String title,
             String overrideDirectory,
            final String[] extensions,
            final String fileDescription,
            final boolean allowNonExist) {
        FileChooser fileChooser = new FileChooser();
           
        if (overrideDirectory != null && new File(overrideDirectory).exists())
            fileChooser.setInitialDirectory(new File(overrideDirectory));
        else if (lastBrowsedDirectory != null && new File(lastBrowsedDirectory).exists())
            fileChooser.setInitialDirectory(new File(lastBrowsedDirectory));
        
        fileChooser.setTitle(title);        

        if (extensions != null && extensions.length > 0) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(fileDescription, extensions));
        }

        File file = null;
        File retVal = null;
        if ((file = fileChooser.showOpenDialog(parent)) != null) {
            if (file.exists() || allowNonExist) {
                setLastBrowsedDirectory(file.getParent());
                retVal = file;
            }
        } 
        return retVal;
    }

    public static int indexOf(Object[] array, Object item) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && array[i].equals(item)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Applies the CSS stylings and icon that should be associated with all
     * PAL UI popup windows.
     * 
     * @param scene
     */
    public static void initPalStage(Stage stage, Scene scene) {
        String cssPath = ResourceLoader.getURL("PALCore.css").toExternalForm();
        //scene.getStylesheets().addAll(cssPath);
        stage.getIcons().add(getImage("gear.png"));
    } 
    
    public static void initPalStage(Stage stage, Scene scene, String CSSFileName) {
        String cssPath = ResourceLoader.getURL(CSSFileName).toExternalForm();
        scene.getStylesheets().addAll(cssPath);
        stage.getIcons().add(getImage("gear.png"));
    } 
    

    private static final String EXER_EXT = "*.xml";

    public static File browseForExerciseFile(Window parent) {
    	return Utilities.promptOpenFile(parent, "Open", null, new String[] {
    			EXER_EXT }, "Exercise Files (" + EXER_EXT + ")", false);
    }
    
    public static File browseForNewExerciseFile(Window parent) {
    	return Utilities.promptSaveFile(parent, EXER_EXT, true, "Create New Exercise File"); 
    
    }

}