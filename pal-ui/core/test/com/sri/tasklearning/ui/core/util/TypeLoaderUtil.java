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

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import com.sri.pal.Bridge;
import com.sri.pal.PALStatusMonitor;

public class TypeLoaderUtil {

    /** 
     * Simple utility for loading all action models and procedures located 
     * in the testStorage directory in to the bridge's default FileTypeStorage.
     * This allows for development of the editor without the need to install apps
     * or learn procedures.
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {

        boolean startedBackend = false;

        if (!PALStatusMonitor.isTaskLearningRunning()) {
            Bridge.startPAL();
            startedBackend = true;
        }

        Bridge bridge = Bridge.newInstance("-bridge-"
                + System.currentTimeMillis());
        
        if (JOptionPane.showConfirmDialog(
                null,
                "Do you want to (re)load the canned action models/procedures?",
                "Load Canned Procedures?",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            CoreUITestUtilities.loadActionModels(bridge);
            CoreUITestUtilities.loadProcedures(bridge);
        }

        JFileChooser chooser = new JFileChooser(CoreUITestUtilities.ROOT_DIR);
        chooser.setDialogTitle("Choose an additional action model or procedure to load"); 
        GenericFilter f = new GenericFilter(new String[] {".xml", ".procedure"}, "actions models or procedure files");        
        chooser.setFileFilter(f);

        while (JOptionPane.showConfirmDialog(null,
                "Do you want to open another action model or procedure?",
                "Load another?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File chosen = chooser.getSelectedFile();
                String fileName = chosen.getName().toLowerCase();

                if (fileName.endsWith(".xml"))
                    CoreUITestUtilities.loadActionModel(bridge, chosen);
                else if (fileName.endsWith(".procedure"))
                    CoreUITestUtilities.loadProcedure(bridge, chosen);
            }         
        }
        
        if (startedBackend) {
            bridge.shutdown();
            System.exit(0);
        } else {
            bridge.disconnect();
        }
    }
}
