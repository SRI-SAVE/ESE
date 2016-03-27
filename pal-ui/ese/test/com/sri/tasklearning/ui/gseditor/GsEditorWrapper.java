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

package com.sri.tasklearning.ui.gseditor;

import com.sri.tasklearning.ui.gseditor.GsEditor;

import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GsEditorWrapper extends GsEditor {
    public static final Object monitor = new Object(); 
    public static boolean open = false;
    
    @Override
    public void start(final Stage stage) {
        stage.setOnShown(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent e) {
                synchronized (monitor) {
                    open = true;
                    monitor.notifyAll();
                }
            }
        });
        super.start(stage);
    }
    
    public static void run() {
        launch(new String[]{});
    }
}
