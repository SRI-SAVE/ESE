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

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.util.Duration;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;

/**
 * A Tooltips implementation. JavaFX 2.0 provides tooltips for all classes that 
 * descend from Control, but anything that is not a Control must be manually 
 * tool-tipped. This class fulfills that need. 
 */

public final class ToolTipper {
    private static final double PAD = 4.0;
    private static final Text textNode = new Text(PAD + 1, PAD, "I'm a tooltip!");
    private static final Group tooltip = new Group();
    private static final Rectangle background = new Rectangle();
    private static final Timeline showTip = new Timeline();
    private static boolean canShow = true; 
    
    private static boolean initialized = false;

    private static void initialize() {
        if (initialized)
            return;

        textNode.setFont(Fonts.STANDARD);
        textNode.setFill(Colors.DefaultText);
        textNode.setOpacity(0.9);
        textNode.setWrappingWidth(350);
        textNode.setTextOrigin(VPos.TOP);
        textNode.setBoundsType(TextBoundsType.VISUAL);

        background.setFill(Color.rgb(255, 255, 190));
        background.setStroke(Color.BLACK);
        background.setStrokeWidth(0.0);
        DropShadow dropshadow = new DropShadow();
        dropshadow.setOffsetX(2.0);
        dropshadow.setOffsetY(2.0);
        dropshadow.setColor(Colors.derive(Color.BLACK, 0.75));
        dropshadow.setRadius(7.0);
        background.setEffect(dropshadow);

        tooltip.setOpacity(0.0);
        tooltip.setAutoSizeChildren(false);
        tooltip.setVisible(false);
        tooltip.getChildren().setAll(background, textNode);
        
        showTip.getKeyFrames().setAll(
                new KeyFrame(Duration.millis(0), 
                             new KeyValue(tooltip.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(1400), 
                             new KeyValue(tooltip.opacityProperty(), 0)),                         
                new KeyFrame(Duration.millis(1500), 
                             new KeyValue(tooltip.opacityProperty(), 1.0)));        
        
        initialized = true;
    }
    
    public static boolean isEnabled() {
        return canShow; 
    }
    
    public static void setEnabled(boolean enabled) {
        if (!enabled && tooltip.isVisible())
            hideTooltip();
        
        canShow = enabled; 
    }
    
    public static void showTooltip(IToolTippable sender, MouseEvent evt) {
        if (!initialized)
            initialize();
        
        if (!canShow)
            return; 
        
        if (sender.getToolTipCallback() == null || sender.getToolTipCallback().getToolTipText() == null)
            return;
        
        final Node node = sender.getToolTipNode();

        // Move tooltip to correct scene
        if (tooltip.getScene() != node.getScene()) {
            if (tooltip.getScene() != null)
                ((Pane) tooltip.getScene().getRoot()).getChildren().remove(
                        tooltip);
            ((Pane)node.getScene().getRoot()).getChildren().add(tooltip);
        }

        textNode.setText(sender.getToolTipCallback().getToolTipText());
        
        background.setWidth(textNode.getLayoutBounds().getWidth() + 2 * PAD);
        background.setHeight(textNode.getLayoutBounds().getHeight() + 4 * PAD);
        
        if (!tooltip.isVisible()) {
            tooltip.setTranslateX(evt.getSceneX() - 20);
            tooltip.setTranslateY(evt.getSceneY() + 20);
            
            if (tooltip.getTranslateX() + tooltip.getLayoutBounds().getWidth() > tooltip
                    .getScene().getWidth()) {
                // scooch tooltip to the left however much it needs to
                tooltip.setTranslateX(tooltip.getTranslateX()
                        - (tooltip.getTranslateX()
                           + tooltip.getLayoutBounds().getWidth()
                           - tooltip.getScene().getWidth()));
            }
            if (tooltip.getTranslateY() + tooltip.getLayoutBounds().getHeight() > tooltip
                    .getScene().getHeight()) {
                // show the tooltip above the cursor if there's no room below
                tooltip.setTranslateY(tooltip.getTranslateY()
                        - (tooltip.getLayoutBounds().getHeight() + 15));
            }

            tooltip.toFront();        
            tooltip.setVisible(true);
            showTip.playFromStart();
        }
    }

    public static void hideTooltip() {
        if (!initialized)
            initialize();
        
        if (tooltip.isVisible()) {
            showTip.stop();
            tooltip.setOpacity(0.0);
            tooltip.setVisible(false); 
        }
    }
    
    public static boolean isTooltipShowing() {
        return tooltip.isVisible();
    }
    
    public static void registerEventHandlers(final IToolTippable sender) {
        Node node = sender.getToolTipNode();
        
        node.setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                ToolTipper.hideTooltip();
            }
        });
        node.setOnMouseMoved(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                ToolTipper.showTooltip(sender, e);
            }
        });
        node.setOnMouseExited(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                ToolTipper.hideTooltip();
            }
        });
    }
    
    /**
     * Defines an Node that can be tool-tipped by our tooltips implementation
     */
    public interface IToolTippable {
        public Node getToolTipNode();
        public IToolTipCallback getToolTipCallback();
        public void setToolTipCallback(IToolTipCallback cb);
    }
    
    /**
     * Callback for when updated tooltip text is required
     */
    public interface IToolTipCallback {
        public String getToolTipText();
    }
    
    public static IToolTipCallback basicTooltip(final String tip) {
        return new IToolTipCallback() {
            public String getToolTipText() {
                return tip; 
            }
        };
    }
}

