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

package com.sri.tasklearning.ui.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;

import com.sri.pal.Bridge;
import com.sri.pal.LumenProcedureDef;
import com.sri.pal.PALException;
import com.sri.tasklearning.ui.core.BackendFacade;

public class CoreUITestUtilities {
    public static final File ROOT_DIR = new File(CoreUITestUtilities.class.getResource("../testStorage").getPath().replace("%20", " "));
    // Directory used by bridge to store types for loaded actions and procedures
    public static final File STORAGE_DIR = new File("storage");
    // Directory that contains our test action models 
    public static final File ACT_SRC_DIR = new File(ROOT_DIR, "action_models");
    // Directory that contains our test procedures
    public static final File PROC_SRC_DIR = new File(ROOT_DIR, "procedures");

    /**
     * Loads all the action models stored in our test storage directory.
     */
    public static void loadActionModels(Bridge bridge) throws MalformedURLException, PALException {
        loadActionModels(bridge, ACT_SRC_DIR);
    }    
    public static void loadActionModels(Bridge bridge, File dir) throws MalformedURLException, PALException {
        final File[] listOfFiles = dir.listFiles();        
        for (int i = 0; i < listOfFiles.length; i++) {
            String fileName = listOfFiles[i].getName();
            if (!listOfFiles[i].isDirectory() && fileName.endsWith(".xml")) {
                loadActionModel(bridge, listOfFiles[i]);
            }
        }
    }
        
    public static void loadActionModel(final Bridge bridge, final File amFile) throws MalformedURLException, PALException {
        String fileName = amFile.getName();
        bridge.getActionModel().load(amFile.toURI().toURL(),
                fileName.substring(0, fileName.length() - 4));
    }
    
    /**
     * Loads all procedures stored in our test storage directory    
     */
    public static void loadProcedures(Bridge bridge) throws Exception {
        loadProcedures(bridge, PROC_SRC_DIR);                 
    }
    public static void loadProcedures(Bridge bridge, File dir) throws Exception {
        for (File proc : dir.listFiles()) {
            if (!proc.getName().endsWith(".procedure"))
                continue;
            loadProcedure(bridge, proc);
        }                    
    }
    
    public static void loadProcedure(Bridge bridge, File procFile) throws Exception {
        final String source = CoreUITestUtilities.ctrsFromFile(procFile, false);
        bridge.getPALExecutor().load(source);
    }
    
    public static void removeDir(File file) {
        if (file.isDirectory())
            for (File sub : file.listFiles())
                removeDir(sub);

        file.delete();
    }
    
    /**
     * Get CTR-S for a given type.
     * 
     * @param typeName
     *            the name of the type to lookup
     * @param unwrapXML
     *            whether or not the wrapping xml should be removed
     * @return the CTR-S string for this type
     * @throws PALException
     *             if we cannot look up the type
     * @throws IOException
     *             if reading the file for this type fails
     */
    public static String ctrsFromFile(final File procFile, final boolean unwrapXML) throws PALException,
            IOException {

        final StringBuffer buffer = new StringBuffer();
        final BufferedReader in = new BufferedReader(new FileReader(procFile));
        try {
            String line = in.readLine();
            while (line != null) {
                buffer.append(line);
                buffer.append("\n");
                line = in.readLine();
            }
        } finally {
            in.close();
        }

        if (unwrapXML)
            return LumenProcedureDef.unwrapXml(buffer.toString());
        else
            return buffer.toString();
    }
    
   
}
