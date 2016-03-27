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

/**
 * Defines the color scheme for the editor. These colors are duplicated and in
 * some cases extended in PALCore.css
 */

import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class Colors {
    public static final Color DefaultText = Color.BLACK;

    public static final Color DisabledText = Color.rgb(87, 87, 87);

    public static final Color SelectionText = Color.WHITE;

    public static final Color StepBackground = Color.rgb(185, 203, 217);

    public static final Color SystemLightGray = Color.rgb(239, 239, 239);

    // public def SystemLightGray = Color.rgb(243,243,243); // original; too
    // light
    public static final Color SystemGray = Color.rgb(204, 204, 204);

    public static final Color SystemDarkGray = Color.rgb(103, 103, 103);

    public static final Color WindowBackground = Color.WHITE;

    public static final Color SelectedStepBackground = Color.rgb(255, 200, 145);

    // public def SelectedStepLightBackground = Color.rgb(255, 244, 232); //
    // original; too peachy
    public static final Color SelectedStepLightBackground = Color.rgb(255, 245,
            236);

    public static final Color SelectedStepBorder = Color.rgb(255, 128, 0);

    public static final Color Selection = Color.rgb(25, 49, 82);

    public static final Color SelectionAltDark = Color.rgb(130, 150, 185);

    // public def SelectionAlt0 = Color.rgb(48, 101, 183); // old dark; too dark
    public static final Color SelectionAltLite = Color.rgb(176, 191, 216);

    // public def SelectionAlt1 = Color.rgb(115, 162, 219); // old lite; too
    // dark
    // public def SelectionAlt1 = Color.rgb(44, 73, 109); // original lite
    // color; way too dark
    public static final Color SelectionAltBorder = Color.rgb(162, 176, 204);

    // public def SelectionAlt2 = Color.rgb(82, 124, 182); // old border; too
    // dark

    public static final Color SelectedVariableBackground = Color.rgb(255, 128,
            0);

    public static final Color SelectedVariableBackgroundDark = Color.rgb(157,
            86, 14);

    public static final Color SelectedVariableBorder = Color.rgb(38, 38, 38);

    public static final Color ConstantBackground = Color.rgb(225, 224, 224);

    public static final Color ConstantBorder = Color.rgb(134, 134, 134);

    public static final Color ConstantForeground = Color.rgb(130, 130, 130);

    public static final Color InputBackground = Color.rgb(196, 215, 237);

    // public def InputBorder = Color.rgb(39,85,129); // original; too dark
    public static final Color InputBorder = Color.rgb(69, 115, 159);

    public static final Color ResultBackground = Color.rgb(225, 230, 250);

    public static final Color ResultBorder = Color.rgb(114, 157, 196);

    public static final double DimOpacity = 0.4;

    public static final DropShadow InsetWhite;
    static {
        InsetWhite = new DropShadow();
        InsetWhite.setOffsetX(0.0);
        InsetWhite.setOffsetY(1.0);
        InsetWhite.setBlurType(BlurType.THREE_PASS_BOX);
        InsetWhite.setColor(Color.WHITE);
        InsetWhite.setRadius(0.0);
    }

    /**
     * Derives a color given a base color and the new opacity value
     */
    public static Color derive(Color c, double opacity) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), opacity);
    }

    /**
     * Makes a color darker by blending it with black by the given percentage.
     */
    public static Color darker(Color base, double percentage) {
        return base.darker();
    }

    /**
     * Makes a color lighter by blending it with white by the given percentage.
     */
    public static Color lighter(Color base, double percentage) {
        double opacity = ((1 - base.getOpacity()) * percentage)
                + base.getOpacity();
        return derive(base, opacity);
    }

    public static String toCssString(Color c, double opacity) {
        return "rgba(" + Math.round(c.getRed() * 255) + ","
                + Math.round(c.getGreen() * 255) + ","
                + Math.round(c.getBlue() * 255) + "," + c.getOpacity() + ")";
    }

    public static String toCssString(Color c) {
        return toCssString(c, 1);
    }
}
