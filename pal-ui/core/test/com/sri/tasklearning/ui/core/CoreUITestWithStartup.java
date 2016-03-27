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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.sri.pal.Bridge;
import com.sri.pal.FileTypeStorage;
import com.sri.tasklearning.ui.core.util.CoreUITestUtilities;

public class CoreUITestWithStartup extends CoreUITest {    
    private static FileTypeStorage testStorage;
    
    @BeforeClass(groups = "GUI")
    public static void startUp() throws Exception {
        File storageDir = CoreUITestUtilities.STORAGE_DIR;
        
        Bridge.startPAL();
        
        final String clientName = "CoreUITest-" + System.currentTimeMillis();
        
        bridge = Bridge.newInstance(clientName);        
        
        // Make sure each test run is separate from the last
        CoreUITestUtilities.removeDir(storageDir); 

        // Create our own storage directory within testStorage that is transient
        // between test runs
        testStorage = new FileTypeStorage(storageDir, clientName);

        bridge.setTypeStorage(testStorage);

        CoreUITestUtilities.loadActionModels(bridge);
        CoreUITestUtilities.loadProcedures(bridge);

        // Gnarly reflection to instantiate our BackendFacade "singleton"
        // without actually calling the standard instantiation logic
        final Constructor<BackendFacade> bfConstructor = BackendFacade.class
                .getDeclaredConstructor();
        bfConstructor.setAccessible(true);
        BackendInterface instance = bfConstructor.newInstance();

        Field fields[] = BackendFacade.class.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getName().equals("instance"))
                field.set(null, instance);
            else if (field.getName().equals("bridge"))
                field.set(instance, bridge);
        }
    }
    
    @AfterClass
    public static void tearDownBackend() throws Exception {
        try {
            if (bridge != null)
                bridge.shutdown();
        } catch(Exception e) {
            System.err.println("Bridge could not shutdown properly: "+e.getMessage());
        }        
    }
}
