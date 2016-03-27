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

package com.sri.tasklearning.novo;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.Bridge;
import com.sri.tasklearning.novo.adept.AdeptWrapper;
import com.sri.tasklearning.novo.factory.ThingSocket;
import com.sri.tasklearning.novo.thing.Thing;
import com.sri.tasklearning.util.LogUtil;

public class Novo extends Application {
    private static final Logger log = LoggerFactory.getLogger(Bridge.class);
    private static final double WINDOW_HEIGHT = 800;
    private static final double WINDOW_WIDTH = 1200;
    private static final double TOOLBAR_HEIGHT = 60;
    private static final int TOOLBAR_ICON_SIZE = 48;
    private static Novo instance;
    
    private final AnchorPane root = new AnchorPane();
    private final ToolBar toolBar = new ToolBar();
    private final PartsViewer unusedParts = new PartsViewer("Working Area", false);
    private final PartsViewer usedParts = new PartsViewer("Used Pieces", true); 
    {
        usedParts.setOpacity(0.65);
    }
    private final SplitPane outerSplitPane = new SplitPane(); {
        outerSplitPane.setOrientation(Orientation.HORIZONTAL);
        outerSplitPane.setDividerPosition(0, 0.25);
    }
    private final SplitPane innerSplitPane = new SplitPane(); {
        innerSplitPane.setOrientation(Orientation.VERTICAL);
    }
    private final Label solvedMessage = new Label(); {
        solvedMessage.setFont(Font.font("Tahoma", FontWeight.NORMAL,
            FontPosture.REGULAR, 18.2f));
    }
    private Stage stage = null;
    private Stage popupStage = null;
    
    private final Button viewSolution = new Button(); {        
        viewSolution.setVisible(false);  
        viewSolution.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {

                popupStage = new Stage(StageStyle.UTILITY);
                Group grp = new Group();
                grp.setTranslateX(10);
                Scene scene = new Scene(grp);
                popupStage.setScene(scene);

                PartsViewer flow = new PartsViewer("", false);
                flow.setPrefWrapLength(400);
                for (Thing thing : currentPuzzle.getSolution())
                    flow.getChildren().add(thing.getNode());                

                ((Group)popupStage.getScene().getRoot()).getChildren().clear();
                ((Group)popupStage.getScene().getRoot()).getChildren().add(flow);

                popupStage.setTitle(currentPuzzle.getName());
                popupStage.setX(stage.getX() + stage.getWidth() / 2 - 200);
                popupStage.setY(stage.getY() + 200);
                popupStage.initModality(Modality.APPLICATION_MODAL);
                popupStage.show();
                popupStage.toFront();
            }
        });
    }
    private Puzzle currentPuzzle = null; 
    
    public static Novo getInstance() {
        return instance; 
    }
    
    public static void main(String[] args) {        
        Application.launch(args);
    } 
    
    @Override 
    public void start(Stage stage) {
        LogUtil.configureLogging("novo_log", Novo.class);
        
        instance = this;
        this.stage = stage; 
        
        stage.setHeight(WINDOW_HEIGHT);
        stage.setWidth(WINDOW_WIDTH);
       
        stage.setTitle("Novo");
        
        stage.getIcons().add(new Image(Novo.class.getResourceAsStream("images/24/puzzle_piece_red.png")));
        
        AdeptWrapper.initialize(); 
        
        stage.setOnHidden(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent e) {
                if (popupStage != null && popupStage.isShowing())
                    popupStage.close();
                AdeptWrapper.disconnect();
            }
        });
        
        Scene scene = new Scene(root);  
        
        String cssPath = Novo.class.getResource("Novo.css").toExternalForm();
        log.info("Using stylesheet from {}", cssPath);
        scene.getStylesheets().addAll(cssPath);
        
        toolBar.setPrefHeight(TOOLBAR_HEIGHT);
        
        populateToolBar();

        ScrollPane unusedScrollPane = new ScrollPane();
        unusedScrollPane.setContent(unusedParts);
        unusedScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        unusedParts.prefWidthProperty().bind(unusedScrollPane.widthProperty());
        unusedParts.minHeightProperty().bind(unusedScrollPane.heightProperty());
        
        final ScrollPane usedScrollPane = new ScrollPane();
        usedScrollPane.setContent(usedParts);
        usedScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        usedParts.prefWidthProperty().bind(usedScrollPane.widthProperty());
        usedParts.minHeightProperty().bind(usedScrollPane.heightProperty());
        usedScrollPane.setPrefHeight(500);
        
        innerSplitPane.getItems().addAll(unusedScrollPane, usedScrollPane);
        outerSplitPane.getItems().addAll(FactoryViewer.INSTANCE, innerSplitPane);
        
        innerSplitPane.setDividerPosition(0, 1);
        AnchorPane.setLeftAnchor(toolBar, 0.0);
        AnchorPane.setRightAnchor(toolBar, 0.0);
        AnchorPane.setTopAnchor(toolBar, 0.0);
        
        AnchorPane.setTopAnchor(outerSplitPane, TOOLBAR_HEIGHT);
        AnchorPane.setLeftAnchor(outerSplitPane, 0.0);
        AnchorPane.setRightAnchor(outerSplitPane, 0.0);
        AnchorPane.setBottomAnchor(outerSplitPane, 0.0);
        
        root.getChildren().addAll(toolBar, outerSplitPane);        
       
        stage.setScene(scene);        
        stage.show();   
    }    
    
    private void populateToolBar() {
        final MenuButton openPuzzle = new MenuButton("Puzzles", getImageView("images/48/puzzle_color.png", TOOLBAR_ICON_SIZE));
        openPuzzle.setContentDisplay(ContentDisplay.LEFT);
        MenuItem blank = new MenuItem("Blank Canvas");
        blank.setOnAction(new EventHandler<ActionEvent>() {
           public void handle(ActionEvent e) {
               reset();
           }
        });        
        
//        Menu easy = new Menu("Easy");        
//        Menu medium = new Menu("Medium");
//        Menu hard = new Menu("Hard");
        
        openPuzzle.getItems().addAll(blank, new SeparatorMenuItem());
        
        for (Puzzle puzzle : Puzzle.getPuzzles()) {
            final Puzzle finalPuzzle = puzzle;
            MenuItem mi = new MenuItem(puzzle.getName());
            mi.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    reset();
                    currentPuzzle = finalPuzzle; 
                    for (Thing thing : finalPuzzle.getStartingConfiguration())
                        unusedParts.addThing(thing);

                    unusedParts.registerSolution(finalPuzzle.getSolution());
                    viewSolution.setText(finalPuzzle.getName());
                    viewSolution.setVisible(true); 
                    viewSolution.setPrefHeight(openPuzzle.getHeight());
                    viewSolution.fire();
                }
            });
//            switch (puzzle.getDifficulty()) {
//            case EASY: easy.getItems().add(mi); break;
//            case MEDIUM: medium.getItems().add(mi); break;
//            case HARD: hard.getItems().add(mi); break; 
//            }
            // FX 2.0 sub-menus aren't working correctly, so just add them to a flat list for now
            openPuzzle.getItems().add(mi);
        }

        toolBar.getItems().addAll(openPuzzle, viewSolution, solvedMessage);
    }
    
    public void reset() {
        Controller.numStepsProperty().setValue(0);
        unusedParts.clear();
        usedParts.clear();
        for (ThingSocket socket : ThingSocket.getAllSockets())
            socket.clear();
        viewSolution.setVisible(false);
        solvedMessage.setText("");
    }
    
    public void solvePuzzle() {        
        solvedMessage.setText("Puzzle solved in " + Controller.numStepsProperty().getValue() + " steps!");
    }
    
    public PartsViewer getUsedPartsViewer() {
        return usedParts;
    }
    
    public PartsViewer getUnusedPartsViewer() {
        return unusedParts; 
    }
    
    public static ImageView getImageView(String filename, int size) {
        Image img = new Image(Novo.class.getResourceAsStream(filename), size, size, true, true);
        return new ImageView(img);
    }
}
