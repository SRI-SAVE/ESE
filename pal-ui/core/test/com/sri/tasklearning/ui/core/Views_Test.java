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
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sri.pal.PALException;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.util.CoreUITestUtilities;

public class Views_Test extends CoreUITestWithStartup { 
    private static final Logger log = LoggerFactory
            .getLogger(Views_Test.class);
    
    private Object monitor = new Object();
    private Exception exception; 
    private ScrollPanePlus sp;

    @Test(timeOut = 30000, groups = "GUI")
   public void testProcedureViews() throws InterruptedException, PALException, IOException {
        Callback<Stage, Object> test = new Callback<Stage,Object>() {
            public Object call(Stage stage) {
                sp = new ScrollPanePlus();
                final Scene scene = new Scene(sp);
                stage.setWidth(1024);
                stage.setHeight(800);
                stage.setScene(scene);
                Utilities.initPalStage(stage, scene);
                stage.show();
                return null;
            }
        };                
        
        TestFXApp.setTestCallback(test);

        synchronized (monitor) {
            new Thread(new Runnable() {
                public void run() {
                    TestFXApp.runTest(monitor);
                }
            }).start();
            monitor.wait();
        }        
        
        final ArrayList<ProcedureModel> models = new ArrayList<ProcedureModel>();
        for (File proc : CoreUITestUtilities.PROC_SRC_DIR.listFiles()) {
            if (!proc.getName().endsWith(".procedure"))
                continue;           

            final String source = CoreUITestUtilities.ctrsFromFile(proc, true);
            final ProcedureModel model = BackendFacade.getInstance()
                    .instantiateProcedureFromSource(source);
            models.add(model);
        }

        for (ProcedureModel m : models) {
            final ProcedureModel model = m;
            synchronized (monitor) {
                Platform.runLater(new Runnable() {
                    public void run() {
                        try {
                            log.info("Creating ProcedureView for "
                                    + model.getName());
                            ProcedureEditController control = new ProcedureEditController(model);
                            final ProcedureView pv = 
                              new ProcedureView(model, control, sp, true, null);
                            sp.setContent(pv);
                            pv.prepareToClose();
                        } catch (Exception e) {
                            exception = e;
                        } finally {
                            synchronized (monitor) {
                                monitor.notifyAll();
                            }
                        }
                    }
                });
                monitor.wait();
                if (exception != null) {
                    throw new Error("Exception in FX event thread", exception);
                }
                assertNoLogErrors(true);
            }
        }
    } 
}
