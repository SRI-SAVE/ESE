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

package com.sri.tasklearning.ui.core.step.dialog;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.DragDropManager;
import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.VariableManager;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.step.StepViewBasicUI;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.VariableModel;

/**
 * An special extension of StepView that acts as a dialog nested between
 * other normal step views. In this way it's possible to have non-modal 
 * configuration dialogs appear within the main procedure view. Each 
 * DialogStepView is associated with a parent StepView that is being configured
 * somehow by the DialogStepView. 
 */
public class DialogStepView extends StepView {
	
    protected static final double BUTTON_ROW_HEIGHT = 55.0;
    protected static final double BUTTON_PADDING = 10.0;
    protected static final double BUTTON_WIDTH = 90;
    protected static final double BUTTON_HEIGHT = 30;
    protected static final double CONTENT_PADDING = 70 + BUTTON_ROW_HEIGHT;
    protected static final double ARROW_SIZE = StepLayout.INTER_STEP_PADDING;
    
    private DialogContentPanel contentPanel;
    private StepView target;
    private boolean deleteOnCancel = false;
    private Runnable onOkayClicked;
    private Image dialogIcon;
    
    private final VariableManager varMgr;
    private final EditController controller;
    
    // ****************** Constructors/Overrides ******************************    

    public DialogStepView(            
            final StepView argTarget, 
            final DialogContentPanel argPanel,
            final boolean argDelete, 
            final Image argIcon) {
        super(argTarget.getStepModel(), 
              argTarget.getStepViewContainer(),
              argTarget.getView());
        
        controller = procedureView.getController();
        varMgr     = controller.getVariableManager();
        
        this.setMinHeight(Region.USE_PREF_SIZE);
        this.setMaxHeight(Region.USE_PREF_SIZE);
        this.setPrefHeight(Region.USE_COMPUTED_SIZE);
        
        contentPanel = argPanel; 
        target = argTarget; 
        deleteOnCancel = argDelete;
        dialogIcon = argIcon;
        
        setPrefWidth(Region.USE_COMPUTED_SIZE);
        setMinWidth(Region.USE_PREF_SIZE);
        
        // turn off all the variable highlighting, to avoid conflicts
        if (varMgr != null)
        	for (VariableModel v : varMgr.getVariables())
        		v.setHighlighted(false);        

        argTarget.getView().setReadOnly(true);
        argTarget.getView().getSelectionManager().selectNone();
        procedureView.getSelectionManager().setDisable(true);
        DragDropManager.getInstance().setDisabled(true);
        this.setDisable(false);
                
        initUIComponents();             
        
        cancelButton.requestFocus(); // take focus
        
        getChildren().addAll(background, foreground);                     
    }   
    
    @Override
    protected double computePrefWidth(double height) {
        return contentPanel.prefWidth(height);
    }
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren(); 

        final double lhs_offset = -1 * target.getVisualNestingLevel()
                * StepViewBasicUI.LHS_OFFSET;

        background.setLayoutX(-procedureView.sceneToLocal(
                this.localToScene(0, 0)).getX());
        icon.relocate(lhs_offset - 46, 10);
        title.relocate(lhs_offset, 15);
        contentPanel.relocate(lhs_offset, 60);

        final double buttonY = contentPanel.prefHeight(-1) + 80;
        okButton.relocate(lhs_offset + getWidth()
                - (2 * BUTTON_WIDTH) - BUTTON_PADDING, buttonY);
        cancelButton.relocate(lhs_offset + getWidth()
                - BUTTON_WIDTH, buttonY);
        recalcTotalHeight();
    }
    
    @Override
    public double computePrefHeight(double width) {
        return recalcTotalHeight();
    }
    
    public double recalcTotalHeight() {
        double targetHeight = contentPanel.prefHeight(contentPanel.getWidth())
                + CONTENT_PADDING;
        this.setHeight(targetHeight);
        return targetHeight;
    }
    
    // *********************** Non-overrides **********************************
    
    public void setOnOkayClicked(Runnable onOkayClicked) {
        this.onOkayClicked = onOkayClicked;
    }
    
    public void setOkButtonText(String text) {
        okButton.setText(text); 
    }
    
    protected void handleClose(boolean cancel, boolean deleteOnCancel) {
        if (cancel && deleteOnCancel) {
            // unmanage any results and then delete the step we were adding
            for (ParameterModel result : target.getStepModel().getResults())
            	if (varMgr != null)
            		varMgr.unmanageVariable((VariableModel)result.getTerm());
            
            controller.deleteStep_impl(target);
        }

        procedureView.setReadOnly(false);
        procedureView.getSelectionManager().setDisable(false);
        DragDropManager.getInstance().setDisabled(false);
        getStepViewContainer().getStepLayout().deleteStepView(this);
         
        // Only validate if we actually made changes
        if (!cancel)
            controller.validate(true);        
    }
    
    // *************************** UI Components ******************************
    
    private final Group background = new Group(); {        
        InnerShadow innershadow = new InnerShadow(10.0, 0.0, 1.0, Colors.SystemDarkGray);
        innershadow.setBlurType(javafx.scene.effect.BlurType.ONE_PASS_BOX);
        background.setEffect(innershadow);
        Rectangle rectangle = new Rectangle();
        rectangle.widthProperty().bind(procedureView.widthProperty().add(1));
        rectangle.heightProperty().bind(heightProperty());
        rectangle.setFill(Colors.SystemLightGray);
        Polygon polygon = new Polygon(new double[]{0.0, -(ARROW_SIZE - 2), ARROW_SIZE * 0.8, 0.0, -ARROW_SIZE * 0.8, 0.0});
        polygon.translateXProperty().bind(background.layoutXProperty().multiply(-1).add(this.widthProperty().divide(2)));
        polygon.setFill(Colors.SystemLightGray);
        
        background.getChildren().setAll(rectangle, polygon);
    }    
    private Button cancelButton;
    private Button okButton;
    private ImageView icon;
    private Text title; 
    private Group foreground;
    
    private void initUIComponents() {
        cancelButton = new Button("Cancel");
        cancelButton.setVisible(true);
        cancelButton.setDisable(false);
        cancelButton.setPrefHeight(BUTTON_HEIGHT);
        cancelButton.setPrefWidth(BUTTON_WIDTH);
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                contentPanel.abandonChanges();
                handleClose(true, deleteOnCancel);
            }
        });

        okButton = new Button("OK");
        okButton.setVisible(true);
        okButton.disableProperty().bind(contentPanel.canContinueProperty().not());
        okButton.setPrefHeight(BUTTON_HEIGHT);
        okButton.setPrefWidth(BUTTON_WIDTH);
        okButton.setDefaultButton(true);
        okButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                if (onOkayClicked != null)
                    onOkayClicked.run();
            }
        });
        
        icon = new ImageView(dialogIcon);
        icon.setFitWidth(32.0);
        icon.setFitHeight(32.0);
        icon.setPreserveRatio(true);
        
        // title text
        title = new Text(contentPanel.getTitle());
        title.setFont(Fonts.DIALOG_TITLE);
        title.setTextOrigin(VPos.TOP);
        title.wrappingWidthProperty().bind(widthProperty());
        
        foreground = new Group();
        foreground.getChildren().addAll(contentPanel, cancelButton, okButton, icon, title);
    }
    
   
}
