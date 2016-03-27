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

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.Utilities;

/**
 * A callback-based implementation of Alert windows. JavaFX 2.0 lacked Alerts
 * so we had to roll our own. 
 */

public final class Alert extends Stage {    
    
    //*************************************************************************
    
    public enum AlertResult {
        CANCEL("Cancel", false),
        NO("No", false),
        OK("OK", true),
        YES("Yes", true);
        
        public final String displayText;
        public final boolean dflt; 
        
        AlertResult(String displayText, boolean dflt) {
            this.displayText = displayText;
            this.dflt = dflt; 
        }
    };    
    
    public enum AlertConfig  {
        YES_NO(AlertResult.YES, AlertResult.NO),
        YES_NO_CANCEL(AlertResult.YES, AlertResult.NO, AlertResult.CANCEL),
        OK_CANCEL(AlertResult.OK, AlertResult.CANCEL),
        OK(AlertResult.OK);
        
        public final AlertResult[] opts;
        AlertConfig(AlertResult...opts) {
            this.opts = opts;
        }
    }
    
    //*************************************************************************
    
    private AlertResult result = null; 
    
    private Alert(final String title, final String message,
            final AlertConfig config, final Callback<AlertResult, Void> call) {
        
        super();       
        
        setOnHiding(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent e) {
                if (result == null)
                    result = AlertResult.CANCEL;
                
                if (call != null) {
                    call.call(result);
                }
            }
        });
        
        final VBox vbox = new VBox(20);
        final HBox hbox = new HBox(10);
        final Scene scene = new Scene(vbox);
        
        Utilities.initPalStage(this, scene); 
        this.setScene(scene); 
        this.setTitle(title);
        this.setWidth(300);
        this.initModality(Modality.APPLICATION_MODAL);
        this.initStyle(StageStyle.UTILITY);
        
        scene.setFill(Colors.SystemLightGray);
        vbox.setMinHeight(200);
        vbox.setPrefWidth(300);
        vbox.setPadding(new Insets(10, 20, 20, 10));
        hbox.setAlignment(Pos.BOTTOM_RIGHT);
               
        
        final Text msg = new Text(message);
        msg.wrappingWidthProperty().bind(vbox.widthProperty().subtract(30));
        msg.setFont(Fonts.DIALOG_TEXT);
        
        for (AlertResult res : config.opts) {
            final AlertResult finalRes = res; 
            Button b = new Button(res.displayText);
            b.setPrefWidth(70);
            b.setDefaultButton(res.dflt);
            b.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    result = finalRes; 
                    hide();
                }              
            });
            hbox.getChildren().add(b);
        }
        
        vbox.getChildren().addAll(msg, hbox);
    };
    
    public AlertResult getResult() {
        return result; 
    }

    public static void show(final String title, final String message,
            final AlertConfig config, final Callback<AlertResult, Void> call) {

        final Alert pop = new Alert(title, message, config, call);
        
        
        pop.show();        
    }
}
