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

package com.sri.tasklearning.novo.factory;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Callback;

import com.sri.tasklearning.novo.Controller;
import com.sri.tasklearning.novo.Novo;
import com.sri.tasklearning.novo.thing.Piece;
import com.sri.tasklearning.novo.thing.SizeEnum;
import com.sri.tasklearning.novo.thing.Thing;

public final class SizeFactory extends NovoFactory {
    public static final SizeFactory INSTANCE = new SizeFactory();
    private SizeEnum selectedSize = SizeEnum.MEDIUM;
    private final ThingSocket socket = new ThingSocket(false, new Callback<ThingSocket, Object>() {
        public Object call(ThingSocket socket) {            
            if (socket.getThing() != null)
                buildButton.setDisable(false);
            
            updatePreview();
            return null; 
        }
    });
    
    private SizeFactory() {
        super();

        Label titleLabel = new Label("Size Factory");
        titleLabel.setFont(TITLE_FONT);
        title.getChildren().addAll(getIcon(ICON_SIZE), titleLabel);

        final SplitMenuButton sizeMenu = new SplitMenuButton();

        for (SizeEnum size : SizeEnum.values()) {
            final SizeEnum finalSize = size;
            MenuItem mi = new MenuItem(size.sizeName());
            mi.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    sizeMenu.setText(finalSize.sizeName());
                    selectedSize = finalSize;
                    updatePreview();
                }
            });
            sizeMenu.getItems().add(mi);
        }

        sizeMenu.getItems().get(0).fire();

        Label inputsLabel = new Label("Factory Inputs:");
        inputsLabel.setFont(INPUT_FONT);

        Label pieceLbl = new Label("Input piece:");
        Label colorLbl = new Label("Size:");
        inputs.add(pieceLbl, 0, 0);
        inputs.add(socket, 1, 0);
        inputs.add(colorLbl, 0, 1);
        inputs.add(sizeMenu, 1, 1);

        GridPane.setConstraints(pieceLbl, 0, 0, 1, 1, HPos.RIGHT, VPos.TOP,
                Priority.NEVER, Priority.NEVER);

        buildButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                Controller.sizeCopy((Piece)socket.getThing(), selectedSize, false);
                socket.clear();
                preview.getChildren().clear();
            }
        });
        buildButton.setDisable(true);

        Label outLbl = new Label("Factory Output Preview:");
        outLbl.setFont(INPUT_FONT);

        getChildren().addAll(title, new Spacer(), inputsLabel, inputs,
                new Spacer(), outLbl, preview, new Spacer(), buildButton);
    }

    public void updatePreview() {
        preview.getChildren().clear();
        Piece piece = (Piece)socket.getThing();
        if (piece != null) {
            Thing output = piece.getResizedCopy(selectedSize);
            preview.getChildren().add(output.getNode());
        }
    }

    public ImageView getIcon(int size) {
        return Novo.getImageView("images/" + size + "/resize_color.png", size);
    }
}
