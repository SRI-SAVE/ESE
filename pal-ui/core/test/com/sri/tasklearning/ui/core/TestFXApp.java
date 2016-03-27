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

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Callback;

public class TestFXApp extends Application {
    private static Callback<Stage, Object> test;
    private static Object monitor;
    
    @Override
    public void start(Stage stage) {        
        test.call(stage);
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }
    
    public static void setTestCallback(Callback<Stage, Object> argTest) {
        test = argTest;
    }
    
    public static void runTest(Object argMonitor) {
        monitor = argMonitor;
        launch(new String[]{});
    }
}
