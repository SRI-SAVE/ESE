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

package com.sri.tasklearning.ui.core.term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.TermSplitMenuButton;
import com.sri.tasklearning.ui.core.control.ToolTipper;
import com.sri.tasklearning.ui.core.step.StepView;

/**
 * Abstract base class for views that represent their terms with a single menu
 * button. Such terms include variable references, constant atomic values, 
 * inline collections and structures, null, etc. Terms that are not represented
 * by a single menu button are 'accessor' functions such as first/last, 
 * nth/structGet, etc. They are visualized by 
 * {@link function.AccessorFunctionView}.
 */
public abstract class MenuButtonTermView extends TermView {
    
    public static final double DEFAULT_HEIGHT = 16.0;
    public static final boolean PSYCHEDELIC = false;
    
    protected final TermSplitMenuButton button = new TermSplitMenuButton(this) {
        @Override
        public void show() {
            if (isGenerateMenuItems()) {
                List<MenuItem> items = buildTermOptionsMenu();
                if (items != null) {
                    button.getItems().clear();
                    button.getItems().addAll(buildTermOptionsMenu());
                }
            }
            ToolTipper.setEnabled(false); 
            super.show();
        }
        
        @Override
        public void hide() {
            ToolTipper.setEnabled(true);
            super.hide();
        }
    };
    
    public MenuButtonTermView(
            final TermModel argModel,
            final ParameterModel argParamModel,
            final StepView argStepView,
            final CommonView argProcView) {
        super(argModel, argParamModel, argStepView, argProcView);
    }
    
    public TermSplitMenuButton getNode() {
        return button; 
    }
    
    public void setSpecialMenuItems(List<MenuItem> items) {
        button.getItems().clear();
        button.getItems().addAll(items); 
    }    
    
    private static final String HIGHLIGHT = "highlighted";    
    
    protected void configureButton() {
    	
        button.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                button.show();         
            }
        });

        if (button.getText().length() < 1) {
            setButtonText(termModel.getDisplayString());
            
            termModel.displayStringProperty().addListener(
                    new ChangeListener<String>() {
                        public void changed(
                                ObservableValue<? extends String> val,
                                String oldVal, String newVal) {
                            setButtonText("X "+termModel.getDisplayString());
                        }
                    });
        }        
        
        termModel.highlightedProperty().addListener(
                new ChangeListener<Boolean>() {
                    public void changed(
                            ObservableValue<? extends Boolean> value,
                            Boolean old, Boolean newVal) {
                        if (newVal.booleanValue())
                            button.getStyleClass().add(HIGHLIGHT);
                        else 
                            button.getStyleClass().remove(HIGHLIGHT);
                    }
                });
        
        if (PSYCHEDELIC) {
            String css ="-fx-background-color: #" + getColor(termModel).toString().substring(2, 8) + ";";
            button.setStyle(css);
        }
    }
    
    private static Map<TermModel, Color> colorMap = new HashMap<TermModel, Color>();
    private static List<Color> availColors = new ArrayList<Color>();
    {
        availColors.add(Color.ORANGE);
        availColors.add(Color.ORANGERED);
        availColors.add(Color.FIREBRICK);
        availColors.add(Color.HOTPINK);
        availColors.add(Color.OLIVE);
        availColors.add(Color.THISTLE);
        availColors.add(Color.VIOLET);
        availColors.add(Color.RED);
        availColors.add(Color.GREEN);
        availColors.add(Color.YELLOW);
        availColors.add(Color.SILVER);
        availColors.add(Color.CADETBLUE);
        availColors.add(Color.PEACHPUFF);
        availColors.add(Color.AQUA);
        availColors.add(Color.GREENYELLOW);
        availColors.add(Color.LIGHTBLUE);
        availColors.add(Color.LIGHTGREEN);
        availColors.add(Color.LIGHTPINK);
        availColors.add(Color.LIGHTSEAGREEN);
        availColors.add(Color.LIGHTSKYBLUE);
        availColors.add(Color.BISQUE);
        availColors.add(Color.BLUEVIOLET);
        availColors.add(Color.SLATEBLUE);
        availColors.add(Color.DEEPPINK);
        availColors.add(Color.TOMATO);
    }
    private static int colorIdx =0;
    
    private Color getColor(TermModel term) {
        if (colorMap.containsKey(term))
            return colorMap.get(term);
        
        if (term instanceof ListModel) {
            ListModel list = (ListModel)term;
           if (list.size() == 1) {
               if (colorMap.containsKey(list.get(0)))
                   return colorMap.get(list.get(0));
           }
               
        } else {
            // Treat singleton lists as equal to the item itself
            List<TermModel> elts = new ArrayList<TermModel>();
            elts.add(term);
            ListModel list = new ListModel(elts);
            if (colorMap.containsKey(list))
                return colorMap.get(list);            
        }
        
        colorMap.put(term, availColors.get(colorIdx++ % availColors.size()));
        return colorMap.get(term);
    }
    
    private void setButtonText(String displayText) {                        
        if (displayText != null && displayText.length() > VariableModel.MAX_NAME_LENGTH) {
            String str = displayText.substring(0, VariableModel.MAX_NAME_LENGTH)
                    + "\u2026";
            button.setText(str);
            button.setStyle("-fx-text-fill: black;");
        } else {
            if (displayText == null || displayText.trim().equals("")
                    || displayText.length() == 0) {

                button.setText(ConstantValueView.BLANK_VALUE_LABEL);
                button.setStyle("-fx-text-fill: "
                        + Colors.toCssString(Colors.DisabledText) + ";");
            } else {
                button.setText(displayText);
                button.setStyle("-fx-text-fill: black;");
            }
        }
    }
}
