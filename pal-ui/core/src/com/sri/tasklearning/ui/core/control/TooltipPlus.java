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

package com.sri.tasklearning.ui.core.control;

import javafx.event.EventHandler;
import javafx.scene.control.Tooltip;
import javafx.stage.WindowEvent;

import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTippable;

/**
 * An extension of Tooltip that will refresh its tooltip text each time the
 * tooltip is shown. 
 */
public class TooltipPlus extends Tooltip {
    public TooltipPlus(final IToolTippable tippable) {
        super();
        
        setOnShowing(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent e) {
                TooltipPlus.this.setText(tippable.getToolTipCallback().getToolTipText());
            }
         });
    }
}
