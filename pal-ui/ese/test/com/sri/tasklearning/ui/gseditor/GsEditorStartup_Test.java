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

import org.testng.annotations.Test;

import com.sri.tasklearning.ui.core.CoreUITest;

public class GsEditorStartup_Test
        extends CoreUITest {
    @Test(timeOut = 10000, groups = "GUI")
    public void startEditor()
            throws InterruptedException {
        new Thread(new Runnable() {
            public void run() {
                GsEditorWrapper.run();
            }
        }).start();

        synchronized (GsEditorWrapper.monitor) {
            while (!GsEditorWrapper.open) {
                GsEditorWrapper.monitor.wait();
            }
        }

        assertNoLogErrors(true);
    }
}
