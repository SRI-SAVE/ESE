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

package com.sri.tasklearning.ui.core.library;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.ISelectable;
import com.sri.tasklearning.ui.core.control.ToolTipper;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTippable;

/**
 * Base class for items appearing the library including instance of 
 * {@code LibraryRowGroup} and {@code LibraryRowStep}. 
 */
public abstract class LibraryRowBasicUI extends Pane implements IToolTippable, ISelectable {
    protected String text = "Item";
    protected Image icon = null;
    protected double leftIndent = 4; 
    protected static final Paint SELECT_COLOR = new LinearGradient(0.0, 0.0, 0.0, 1.0, true, null, new Stop[]{new Stop(0.0, Colors.SelectionAltLite), new Stop(1.0, Colors.SelectionAltDark)}); 
    protected static final Paint SELECT_BORDER_COLOR = Colors.SelectionAltBorder;
    protected static final Paint UNSELECT_COLOR = Colors.WindowBackground;
    protected static final double PAD = 8;
    private IToolTipCallback toolTipCallback = new IToolTipCallback() {
        public String getToolTipText() {
            return null;
        }
    };
    
    // Properties
    protected SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
    
    public LibraryRowBasicUI(String argText, Image argIcon) {
        this.text = argText;
        this.icon = argIcon;
        
        ToolTipper.registerEventHandlers(this);
        
        setPrefHeight(Region.USE_COMPUTED_SIZE);
        setMinHeight(Region.USE_PREF_SIZE);
        
        ((ImageView)label.getGraphic()).setImage(icon);
        label.setText(text);
        
        this.hoverProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(
                    final ObservableValue<? extends Boolean> val,
                    final Boolean oldVal, 
                    final Boolean newVal) {
                rectangle.setStroke((newVal || selected.getValue()) 
                        ? SELECT_BORDER_COLOR : UNSELECT_COLOR);
            }
        });

        getChildren().setAll(rectangle, label);
    }
    
    @Override
    protected void layoutChildren() {
        this.setHeight(computePrefHeight(0));
        super.layoutChildren();
        
        label.relocate(leftIndent, PAD / 2);
        rectangle.relocate(.5, .5);
    } 

    @Override
    protected double computePrefHeight(double width) {
        return Math.max(16, label.getLayoutBounds().getHeight()) + PAD; 
    }
    
    // Not an override
    public double computeWidth() {
        return leftIndent + label.getLayoutBounds().getWidth() + 10;
    }
    
    public void setLeftIndent(double indent) {
        leftIndent = indent; 
    }
    
    public String getText() {
        return text; 
    }
    
    @Override
    public int getIndex() {
        return -1;
    }
    
    // ************************** IToolTippable *******************************
    
    @Override
    public IToolTipCallback getToolTipCallback() {
        return toolTipCallback;
    }
    
    @Override
    public void setToolTipCallback(IToolTipCallback cb) {
        toolTipCallback = cb;
    }
    
    @Override
    public Node getToolTipNode() {
        return this; 
    }
    
    // ************************** ISelectable *********************************
    
    @Override
    public SimpleBooleanProperty selectedProperty() {
        return selected;
    }
    
    @Override
    public void setSelected(boolean selected) {
        if (selected != this.selected.getValue()) {
            this.selected.setValue(selected);

            updateVisualElements();
        }        
    }
    
    private void updateVisualElements() {
        boolean selected = this.selected.getValue();       
        
        rectangle.setStroke(selected ? SELECT_BORDER_COLOR : UNSELECT_COLOR);
        rectangle.setFill(selected ? SELECT_COLOR : UNSELECT_COLOR);
        label.setTextFill(selected ? Colors.SelectionText : Colors.DefaultText);
    }
    
    @Override
    public boolean isSelected() {
        return selected.getValue();
    }

    // ************************** UI Components *******************************
    
    protected final Label label = new Label(); {
        ImageView imageview = new ImageView();
        imageview.setPreserveRatio(true);
        imageview.setFitHeight(16.0);
        imageview.setFitWidth(16.0);
        label.setText(text);
        label.setTextOverrun(OverrunStyle.WORD_ELLIPSIS);
        label.setGraphic(imageview);
        label.setContentDisplay(ContentDisplay.LEFT);
        label.setWrapText(false);
        label.setFont(Fonts.STANDARD_MEDIUM);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setTextFill(Colors.DefaultText);
    }
    
    protected final Rectangle rectangle = new Rectangle(); {
        rectangle.widthProperty().bind(widthProperty().subtract(1));
        rectangle.heightProperty().bind(heightProperty().subtract(1));
        rectangle.setFill(UNSELECT_COLOR);
        rectangle.setStroke(UNSELECT_COLOR);
    }      
}
