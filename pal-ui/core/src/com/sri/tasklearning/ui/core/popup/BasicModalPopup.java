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

package com.sri.tasklearning.ui.core.popup;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Callback;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;

/**
 * Abstract base class for modal 'popup' dialogs. Provides OK/Cancel buttons,
 * appropriate callbacks and layout functionality. Note that the pops contained
 * within this namespace are separate from dialog windows for opening and 
 * saving procedures, etc. The popups in this namespace are facilitate specific
 * edit scenarios on the currently open procedure. These popups are definitely
 * modal, but don't actually popup in a separate window. They are instead 
 * painted on the top of the main application stage's scene. 
 */
public abstract class BasicModalPopup {

    protected Scene scene;
    protected Node sender;

    protected Callback<Object, Boolean> onOkayPressed;
    protected Runnable onCancelPressed;
    protected Runnable onLoaded;
    protected Runnable onClosing;

    protected String okButtonText = "OK";
    protected String cancelButtonText = "Cancel";
    protected String titleText = "Dialog Box";
    protected boolean showOkButton = true;

    protected Rectangle overlay;

    public static final double PAD = 10.0;
    protected double prefWidth = 400.0;   
    protected double prefHeight = 250;

    public abstract Pane getContent();

    public abstract void focus();
    
    // Place to put memory cleanup logic independent of Okay/Cancel
    public void cleanup() {
        // Intentionally empty
    }

    protected boolean showOverlay = true;

    protected Text title = new Text();
    {
        title.setFont(Fonts.DIALOG_EMPHASIZED);
        title.setTranslateX(PAD);
        title.setTranslateY(PAD * 2);
    }

    protected void show(Scene scene, Node sender) {

        this.scene = scene;
        this.sender = sender;

        overlay = new Rectangle();
        overlay.widthProperty().bind(scene.widthProperty());
        overlay.heightProperty().bind(scene.heightProperty());
        overlay.setFill(Color.WHITE);
        overlay.setStroke(null);
        overlay.setVisible(showOverlay);
        overlay.setOpacity(.2);

        ((Pane) scene.getRoot()).getChildren().add(overlay);

        dialog.setPrefHeight(prefHeight);
        dialog.setPrefWidth(prefWidth);
        dialog.translateXProperty().bind(
                scene.widthProperty().divide(2).subtract(prefWidth / 2));
        dialog.translateYProperty().bind(
                scene.heightProperty().divide(2)
                        .subtract(dialog.heightProperty().divide(2)));

        title.setText(titleText);
        ok.setVisible(showOkButton);
        ok.setText(okButtonText);
        cancel.setText(cancelButtonText);

        content = getContent();

        dialog.getChildren().addAll(bg, title, content, ok, cancel);

        ((Pane) scene.getRoot()).getChildren().add(dialog);

        focus();
    }

    public void hide() {
        if (onClosing != null)
            onClosing.run();

        ((Pane) scene.getRoot()).getChildren().remove(dialog);
        ((Pane) scene.getRoot()).getChildren().remove(overlay);

        // restore focus to sender node
        if (sender != null && sender.isFocusTraversable())
            sender.requestFocus();
    }

    // **************************** UI Components *****************************

    protected final Pane dialog = new Pane() {
        protected void layoutChildren() {
            content.setPrefWidth(getWidth());
            content.setPrefHeight(getPrefHeight()
                    - (title.getBoundsInParent().getMaxY() + PAD * 1.5)
                    - (ok.getPrefHeight() + PAD * 2));

            super.layoutChildren();
            
            title.setWrappingWidth((int) (prefWidth - PAD * 2));

            bg.setHeight(getHeight());
            bg.setWidth(getWidth());

            Button right = cancel;
            Button left = ok;
            left.setLayoutX(getWidth() - PAD
                    - cancel.getLayoutBounds().getWidth() - PAD
                    - ok.getLayoutBounds().getWidth());
            left.setLayoutY(getHeight() - PAD - ok.getLayoutBounds().getHeight());
            right.setLayoutY(getHeight() - PAD
                    - cancel.getLayoutBounds().getHeight());
            right.setLayoutX(getWidth() - PAD
                    - cancel.getLayoutBounds().getWidth());
            content.setLayoutY(title.getBoundsInParent().getMaxY() + PAD * 1.5);
        }
    };

    protected Pane content;

    protected final Button ok = new Button();
    {
        ok.setPrefWidth(95);
        ok.setPrefHeight(28);
        ok.setDefaultButton(true);
        ok.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                boolean proceed = true;
                if (onOkayPressed != null)
                    proceed = onOkayPressed.call(null);
                
                if (proceed) {
                    hide();
                    cleanup();
                }
            }
        });
    }

    protected final Button cancel = new Button();
    {
        cancel.setPrefWidth(95);
        cancel.setPrefHeight(28);
        cancel.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                if (onCancelPressed != null)
                    onCancelPressed.run();
                hide();
                cleanup();
            }
        });
    }

    protected final Rectangle bg = new Rectangle();
    {
        bg.setFill(Colors.SystemGray);
        bg.setStroke(Colors.SystemDarkGray);
        DropShadow eff = new DropShadow();
        eff.setOffsetX(6);
        eff.setOffsetY(6);
        eff.setColor(Colors.SystemDarkGray);
        eff.setRadius(12);
        bg.setEffect(eff);
    }
}
