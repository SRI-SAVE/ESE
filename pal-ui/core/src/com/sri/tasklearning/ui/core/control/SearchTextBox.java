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
import javafx.scene.control.TextField;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.library.LibraryPanel;

/**
 * A special TextField that is used for the action "Search" box at the top
 * of the library. This class mainly handles special aspects of the 
 * visualization, whereas the library code is responsible for registering the
 * necessary action filtering events. 
 */
public class SearchTextBox extends TextField {
    private static final String DEFAULT = "Search";
    private boolean isUnused = true;
    private static final double HEIGHT = 20.0;

    public SearchTextBox(final LibraryPanel libPanel) {
        super();
        setPromptText(DEFAULT);
        
        setPrefHeight(HEIGHT);
        setMaxHeight(Region.USE_PREF_SIZE);

        this.getStyleClass().clear();
        this.getStyleClass().add("search-text-field");
        
        setEffect(new InnerShadow(2.0, 0.0, 1.0, Colors.SystemDarkGray));
        setOnKeyReleased(
                new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    libPanel.filterActions(SearchTextBox.this.getText());
                }
            });
        setOnKeyPressed(
                new EventHandler<KeyEvent>() {
                    @Override
                    public void handle(KeyEvent event) {
                        SearchTextBox.this.setMouseTransparent(true);
                    }
            });
        setOnMousePressed(
                new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        if(isUnused) {
                            isUnused = false;
                            setText("");
                        }
                    }
            });
        setOnMouseMoved(
                new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        SearchTextBox.this.setMouseTransparent(false);
                    }
            });
    }
}
