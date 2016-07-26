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

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/**
 * Defines standard fonts for use within PAL UI applications to help provide
 */

public class Fonts {

    public static final String FONTFACE = "Tahoma";

    public static final Font STANDARD = Font.font(FONTFACE, FontWeight.NORMAL,
            FontPosture.REGULAR, 12.2f);
    
    public static final Font HUGE = Font.font(FONTFACE, FontWeight.NORMAL,
            FontPosture.REGULAR, 18f);
    
    public static final Font LARGE = Font.font(FONTFACE, FontWeight.NORMAL,
            FontPosture.REGULAR, 14.2f);

    public static final Font STANDARD_TINY = Font.font(FONTFACE,
            FontWeight.LIGHT, FontPosture.REGULAR, 9f);

    public static final Font STANDARD_EMPHASIZED = Font.font(FONTFACE,
            FontWeight.BOLD, FontPosture.REGULAR, 12.2f);

    public static final Font STANDARD_ITALICIZED = Font.font(FONTFACE,
            FontWeight.NORMAL, FontPosture.ITALIC, 12.2f);

    public static final Font STANDARD_MEDIUM = Font.font(FONTFACE,
            FontWeight.MEDIUM, FontPosture.REGULAR, 11f);

    public static final Font TITLE = Font.font(FONTFACE, FontWeight.NORMAL,
            FontPosture.REGULAR, 17f);

    public static final Font BUTTON_TEXT = Font.font(FONTFACE,
            FontWeight.NORMAL, FontPosture.REGULAR, 9f);

    public static final Font DIALOG_TEXT = Font.font(FONTFACE,
            FontWeight.NORMAL, FontPosture.REGULAR, 11f);
    public static final Font DIALOG_EMPHASIZED = Font.font(FONTFACE,
            FontWeight.BOLD, FontPosture.REGULAR, 11f);
    public static final Font DIALOG_ITALICIZED = Font.font(FONTFACE,
            FontWeight.NORMAL, FontPosture.ITALIC, 11f);
    public static final Font DIALOG_TITLE = Font.font(FONTFACE,
            FontWeight.NORMAL, FontPosture.REGULAR, 15f);

    public static final Font STEP_NUMBERING = Font.font("Arial",
            FontWeight.NORMAL, FontPosture.REGULAR, 19f);
    
    public static final Font TRAINING_LARGE = Font.font("Tahoma",
            FontWeight.NORMAL, FontPosture.REGULAR, 16f);
    
    public static final Font TRAINING_LARGE_BOLD = Font.font("Tahoma",
            FontWeight.BOLD, FontPosture.REGULAR, 16f);
}
