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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import com.sri.tasklearning.novo.Novo;

public class NovoFactory extends VBox {
    protected static final int ICON_SIZE = 48;
    protected static final double SPACING = 10;
    protected static final Font TITLE_FONT = Font.font("Tahoma", FontWeight.NORMAL,
            FontPosture.REGULAR, 17.2f);
    protected static final Font INPUT_FONT = Font.font("Tahoma", FontWeight.NORMAL,
            FontPosture.REGULAR, 14.2f);    
    protected final HBox preview = new HBox(SPACING);
    protected final GridPane inputs = new GridPane(); {
        inputs.setAlignment(Pos.CENTER_LEFT);
        inputs.setHgap(SPACING);
        inputs.setVgap(SPACING);
    }
    protected final HBox title = new HBox(5); {
        title.setAlignment(Pos.CENTER_LEFT);
    }
    
    protected final Button buildButton = new Button("Run Factory", Novo.getImageView("images/24/gear.png", 24));

    protected NovoFactory() {
        super(10);
        
        setPadding(new Insets(20, 20, 20, 20));
    }   
    
    class Spacer extends Rectangle {
        public Spacer() {
            super(10, 10);
            
            setFill(Color.TRANSPARENT);
        }
    }
    
    public void updatePreview() {
        throw new RuntimeException("Developer forgot to override updatePreview " +
                 "in concrete Factory class: " + this.getClass());
    }
}
