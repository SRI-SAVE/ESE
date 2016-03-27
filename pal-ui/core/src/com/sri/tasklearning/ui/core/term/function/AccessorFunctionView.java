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

package com.sri.tasklearning.ui.core.term.function;

import java.util.List;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.WindowEvent;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.ToolTipper;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ParameterView;
import com.sri.tasklearning.ui.core.term.TermView;

/**
 * View for accessor functions such as first, last, nth, mapGet, etc. 
 * Typically consists of at least one term view and an assortment of labels
 * to convey to the user what the function call is doing. 
 */
public class AccessorFunctionView extends TermView {
    protected static final int HBOX_VPADDING = 2;
    protected static final int HBOX_HPADDING = 10;
    protected static final Font LABEL_FONT = Fonts.STANDARD;
    
    protected boolean funcNested = false;    
    protected FunctionModel funcModel;
    
    public AccessorFunctionView(
            final FunctionModel argFuncModel,
            final ParameterModel argParam,
            final StepView argStepView,
            final CommonView argProcView) {
        
        super(argFuncModel, argParam, argStepView, argProcView);
        
        funcModel = argFuncModel;             
        
        if (argument.getOwner() instanceof FunctionModel &&
            (FunctionModel.isAccessorFunction((FunctionModel)argument.getOwner())))
            funcNested = true;
        
        if (funcNested) {
            compTermPane.addEventHandler(MouseEvent.MOUSE_ENTERED, 
                new EventHandler<MouseEvent>() {
                public void handle(MouseEvent e) {
                    bg.setStroke(Colors.SystemDarkGray);
                    polygon.setDisable(false);
                }
            });
            compTermPane.addEventHandler(MouseEvent.MOUSE_EXITED,
                new EventHandler<MouseEvent>() {
                public void handle(MouseEvent e) {
                    if (!menu.isShowing()) {
                        bg.setStroke(Color.TRANSPARENT);
                        polygon.setDisable(true);
                    }
                }
            });
            polygon.setDisable(true); 
        } else 
            bg.setStroke(Colors.SystemDarkGray);                   
        
        createTermViews(); 
    }
              
    @Override
    public Pane getNode() {
        return compTermPane;
    }
    
    @Override
    public void setSpecialMenuItems(List<MenuItem> items) {
        menu.getItems().clear();
        menu.getItems().addAll(items); 
    }

    protected void createTermViews() {
        Text lbl = new Text(funcModel.getFunctor());
        lbl.setFont(LABEL_FONT);
        
        hbox.getChildren().add(lbl);
        
        for (ParameterModel pm : funcModel.getInputs()) {
            ParameterView pv = new ParameterView(pm, stepView, procView, true, false, null);
            hbox.getChildren().add(pv);
        }
    }    
    
    // **************************** UI Components *****************************
    
    protected static final LinearGradient BG = new LinearGradient(0.0, 0.0,
            0.0, 1.0, true, null, new Stop[] { new Stop(0.0, Color.LIGHTGRAY),
                    new Stop(1.0, Color.DARKGRAY) });
    protected static final LinearGradient SEL_BG = new LinearGradient(0.0, 0.0,
            0.0, 1.0, true, null, new Stop[] { new Stop(0.0, Color.LIGHTGRAY),
                    new Stop(1.0, Color.LIGHTSLATEGRAY) });

    protected final HBox hbox; {
        hbox = new HBox(5); 
        hbox.setAlignment(Pos.CENTER_LEFT);
    };

    protected final Rectangle bg; {
        bg = new Rectangle(); 
        bg.setFill(BG);  
    }
    
    protected final Polygon polygon = new Polygon(
            new double[] { 0.0, 0.0, 4.0, 5.0, 8.0, 0.0 }); {
        polygon.setFill(Colors.SystemDarkGray);
        polygon.visibleProperty().bind(polygon.disabledProperty().not());
    }
    
    protected final ContextMenu menu = new ContextMenu();
    protected final Pane compTermPane; {        
        compTermPane = new Pane() {
            @Override
            protected void layoutChildren() {
                super.layoutChildren();
                
                if (!funcNested)
                    hbox.setLayoutX(HBOX_HPADDING - 4);
                hbox.setLayoutY(HBOX_VPADDING);
                polygon.setLayoutX(this.getWidth() - HBOX_HPADDING - 1);
                polygon.setLayoutY(this.getHeight() / 2 - 2.5);    
            }
            
            @Override
            public double computeMinHeight(double width) {
                return hbox.prefHeight(width) + 2 * HBOX_VPADDING;
            }
            
            @Override
            public double computePrefHeight(double width) {
                return computeMinHeight(width); 
            }
            
            @Override
            public double computeMaxHeight(double width) {
                return computeMinHeight(width); 
            }
            
            @Override
            public double computeMinWidth(double height) {   
                return hbox.prefWidth(height) + 2 * HBOX_HPADDING;
            }  
            @Override
            public double computePrefWidth(double height) {   
                return computeMinWidth(height);
            }  
            @Override
            public double computeMaxWidth(double height) {   
                return computeMinWidth(height);
            }               
        };

        menu.setOnShowing(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent e) {
                bg.setFill(SEL_BG);
                ToolTipper.setEnabled(false);
            }
        });
        menu.setOnHiding(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent e) {
                bg.setFill(BG);
                if (funcNested) {
                    polygon.setDisable(true);
                    bg.setStroke(Color.TRANSPARENT);
                }
                ToolTipper.setEnabled(true);
            }
        });
        
        compTermPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                if (isGenerateMenuItems()) {
                    menu.getItems().clear(); 
                    List<MenuItem> items = AccessorFunctionView.this
                        .buildTermOptionsMenu();
                    menu.getItems().addAll(items);
                }
                menu.show(bg, Side.BOTTOM, 0, 0);
                e.consume();
            }
        });
        
        bg.widthProperty().bind(compTermPane.widthProperty());
        bg.heightProperty().bind(compTermPane.heightProperty());

        compTermPane.getChildren().addAll(bg, hbox, polygon); 
    };
}
