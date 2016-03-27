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

import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import com.sri.tasklearning.novo.Controller;
import com.sri.tasklearning.novo.Novo;
import com.sri.tasklearning.novo.PartsViewer;
import com.sri.tasklearning.novo.thing.Assembly;
import com.sri.tasklearning.novo.thing.AssemblyConfigurationEnum;
import com.sri.tasklearning.novo.thing.Piece;
import com.sri.tasklearning.novo.thing.Thing;

public final class AssemblyFactory extends NovoFactory {
    public static final AssemblyFactory INSTANCE = new AssemblyFactory();
    
    private AssemblyConfigurationEnum selectedConf;
    private VBox socketBox = new VBox(); {
        socketBox.setMinHeight(Region.USE_PREF_SIZE);
    }
    private List<ThingSocket> sockets = new ArrayList<ThingSocket>();
    
    private AssemblyFactory() {
        super();
        
        Label titleLabel = new Label("Assembly Line");
        titleLabel.setFont(TITLE_FONT);
        title.getChildren().addAll(getIcon(ICON_SIZE),titleLabel);
        
        final SplitMenuButton directionMenu = new SplitMenuButton();

        for (AssemblyConfigurationEnum conf : AssemblyConfigurationEnum.values()) {
            final AssemblyConfigurationEnum finalConf = conf;
            MenuItem mi = new MenuItem(conf.configurationDisplayName());
            mi.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    directionMenu.setText(finalConf.configurationDisplayName());
                    selectedConf = finalConf;
                    updatePreview();
                }
            });
            directionMenu.getItems().add(mi);
        }
        
        directionMenu.getItems().get(0).fire();
        
        addSocket();
        addSocket();
        addSocket();
        
        Label inputsLabel = new Label("Factory Inputs:");
        inputsLabel.setFont(INPUT_FONT);
                   
        Label colorLbl = new Label("Direction:");
        Label pieceLbl = new Label("Input pieces:");
        inputs.add(colorLbl, 0, 0);
        inputs.add(directionMenu, 1, 0);
        inputs.add(pieceLbl, 0, 1);
        inputs.add(socketBox, 1, 1);
        
        GridPane.setConstraints(pieceLbl, 0, 1, 1, 1, HPos.RIGHT,
                VPos.TOP, Priority.NEVER, Priority.NEVER);

        final Button clear = new Button("Clear Inputs");
        clear.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                for (ThingSocket socket : sockets) {
                    Thing thing = socket.getThing();

                    socket.clear();
                    
                    PartsViewer used = Novo.getInstance().getUsedPartsViewer();
                    PartsViewer unused = Novo.getInstance().getUnusedPartsViewer();
                    
                    if (thing != null) {
                        if (thing.isUsed() && !used.getChildren().contains(thing.getNode()))
                            used.addThing(thing);
                        else if (!thing.isUsed())
                            unused.addThing(thing);
                    }
                }
                preview.getChildren().clear();
            }
        });
        
        buildButton.setText("Assemble");
        buildButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                List<Piece> things = new ArrayList<Piece>();
                for (ThingSocket socket : sockets)
                    if (socket.getThing() != null)
                        things.add((Piece)socket.getThing());
                
                if (things.size() == 2)
                    Controller.assembleTwo(selectedConf, things.get(0), things.get(1), false);
                else if (things.size() == 3)
                    Controller.assembleThree(selectedConf, things.get(0), things.get(1), things.get(2), false);
                
                clear.fire();             
            }
        });
        buildButton.setDisable(true);       
        
        Label outLbl = new Label("Factory Output Preview:");
        outLbl.setFont(INPUT_FONT);
        
        getChildren().addAll(title, new Spacer(), inputsLabel, inputs, clear,
                new Spacer(), outLbl, preview,
                new Spacer(), buildButton);
    }
    
    private void addSocket() {
        final ThingSocket newSocket = new ThingSocket(false, new Callback<ThingSocket, Object>() {
            public Object call(ThingSocket socket) {
                updatePreview();
                return null; 
            }
        });
        
        sockets.add(newSocket);
        socketBox.getChildren().add(newSocket); 
    }
       
    @Override
    public void updatePreview() {
        preview.getChildren().clear();
        
        List<Thing> things = new ArrayList<Thing>();
        //ThingSocket lastSocket = null;
        for (ThingSocket socket : sockets) {
            if (socket.getThing() != null)
                things.add((Thing)(socket.getThing().clone()));
            //lastSocket = socket;
        }
        
        // This should be uncommented to support arbitrary-length assemblies. 
        // However, the Novo action model doesn't currently support assemblies 
        // of arbitrary length. 
        
//        if (lastSocket != null && lastSocket.getThing() != null) {
//            addSocket();
//        } 
        
        if (things.size() >= 2) {
            buildButton.setDisable(false);
            Assembly ass = new Assembly(selectedConf, things);
            preview.getChildren().add(ass.getNode());
        } else {
            buildButton.setDisable(true); 
            preview.getChildren().clear(); 
        }
    }
    
    public ImageView getIcon(int size) {
        return Novo.getImageView("images/" + size + "/assembly_color.png", size);
    }
}
