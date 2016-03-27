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

package com.sri.tasklearning.novo.thing;

import java.util.List;

import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import com.sri.pal.Struct;
import com.sri.tasklearning.novo.Novo;
import com.sri.tasklearning.novo.PartsViewer;
import com.sri.tasklearning.novo.factory.ThingSocket;

public abstract class Thing {
    public abstract Node getNode();
    
    private boolean dragging = false;
    private Thing ghost = null;
    private double dragBeginX;
    private double dragBeginY;
    
    private Pane oldParent;
    
    protected boolean used;     
    
    protected void registerEvents(final Node node) {
        node.setOnMouseDragged(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                // Can only drag from the parts viewer
                if (node.getParent() == null ||
                    ((node.getParent().getClass() == PartsViewer.class) ||
                     (node.getParent().getClass() == ThingSocket.class))) {
                    if (!dragging) {
                        Pane pane = (Pane) (((Node) e.getSource()).getScene()
                                .getRoot());
                        oldParent = (Pane)node.getParent();
                        
                        if (oldParent instanceof PartsViewer)
                            ((PartsViewer)oldParent).removeThing(Thing.this);
                        else
                            ((ThingSocket)oldParent).clear(); 
                        
                        oldParent.getChildren().remove(node);  
                        
                        ghost = Thing.this.clone();                           
                        
                        dragBeginX = e.getX();
                        dragBeginY = e.getY();
                        
                        ghost.getNode().setOpacity(0.75);

                        pane.getChildren().add(ghost.getNode());
                        
                        dragging = true;
                    }

                    ghost.getNode().setTranslateX(e.getSceneX() - dragBeginX);
                    ghost.getNode().setTranslateY(e.getSceneY() - dragBeginY);
                }
            }           
        });
        node.setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                if (dragging) {
                    Pane pane = (Pane)ghost.getNode().getScene().getRoot();
                    pane.getChildren().remove(ghost.getNode());
                    
                    PartsViewer pv = Novo.getInstance().getUnusedPartsViewer();
                    PartsViewer pv2 = Novo.getInstance().getUsedPartsViewer();
                    Bounds pvBounds = pv.localToScene(pv.getBoundsInLocal());
                    Bounds pvBounds2 = pv2.localToScene(pv2.getBoundsInLocal());
                    
                    boolean dropped = false;
                    if (!Thing.this.isUsed() && pvBounds.contains(e.getSceneX(), e.getSceneY())) {                    
                        pv.addThing(Thing.this);
                        dropped = true;
                    } else if (Thing.this.isUsed() && pvBounds2.contains(e.getSceneX(), e.getSceneY())) {
                        pv2.addThing(Thing.this);
                        dropped = true;
                    }
                    
                    // Check if its over a thing socket
                    if (!dropped) {
                        List<ThingSocket> dropLocations = ThingSocket.getAllSockets();                                        
                        for (ThingSocket socket : dropLocations) {
                            Bounds socketBounds = socket.localToScene(socket.getBoundsInLocal());
                            
                            if (socket.getScene() != null && socketBounds.contains(e.getSceneX(), e.getSceneY())) {
                                // Found the drop location
                                ghost.getNode().setTranslateX(0);
                                ghost.getNode().setTranslateY(0);
                                socket.dropThing(ghost);
                                dropped = true;
                                break;
                            }
                        }
                    }
                    
                    // Put it back wherever it was
                    if (!dropped) {
                        if (oldParent instanceof PartsViewer)
                            ((PartsViewer)oldParent).addThing(Thing.this);
                        else {
                            ghost.getNode().setTranslateX(0);
                            ghost.getNode().setTranslateY(0);
                            ((ThingSocket)oldParent).dropThing(ghost);
                        }
                        dropped = true; 
                    }
                    
                    dragging = false;                    
                }
            }
        });
    }
    
    public void use() {
        used = true; 
    }
    
    public boolean isUsed() {
        return used; 
    }
        
    /**     
     * @return An Adept Struct instance that matches the current state of this
     * Thing. 
     */
    public abstract Struct toStruct();
    
    public abstract Thing copy();
    
    public abstract Thing clone();
}
