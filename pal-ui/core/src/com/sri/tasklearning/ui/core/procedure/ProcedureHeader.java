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

package com.sri.tasklearning.ui.core.procedure;

import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import com.sri.ai.lumen.atr.ATR;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.IVariableWatcher;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.control.TextAreaPlus;
import com.sri.tasklearning.ui.core.control.TextAreaPlus.ITextInputCallback;
import com.sri.tasklearning.ui.core.control.constant.ConstantEditor;
import com.sri.tasklearning.ui.core.procedure.ProcedureInputsPanel.ProcedureInputRow;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.VariableModel;

/**
 * Abstract base class for different flavors of a procedure header. There are
 * currently two concrete subclasses: {@code EditProcedureHeader} and 
 * {@code ExecProcedureHeader} for editing and executing a procedure, 
 * respectively. This class contains the visual elements shared by both classes
 * to provide them with a consistent appearance. 
 */
public abstract class ProcedureHeader extends Pane implements IVariableWatcher {    
    private static final int TOP_PAD = 40;
    private static final double OVERFLOW = 50.0;
    private static final double BOX_CONTENT_WIDTH = 430;
    private static final double RHS_OFFSET = BOX_CONTENT_WIDTH + 8 + 22;
    
    protected final ProcedureView procedureView;
    protected final ProcedureModel procedureModel;
    protected final ProcedureEditController controller;
    
    private double contentWidth = 400;
    
    protected BackendFacade backend = BackendFacade.getInstance();

    public ProcedureHeader(final ProcedureView argProcView) {
        this.procedureView  = argProcView;
        this.procedureModel = procedureView.getModel();
        this.controller     = procedureView.getController();
        
        renameLink.disableProperty().bind(procedureView.readOnlyProperty());
        
        getStyleClass().add("procedure-header");

        setMinWidth(700);

        initializeUIComponents(); 
    }
    
    @Override 
    protected void layoutChildren() {
        super.layoutChildren();
        
        double contentIndent = (getWidth() / 2.0) - (contentWidth / 2.0); 
        double y = TOP_PAD;
        
        titleBarBox.relocate(contentIndent - OVERFLOW, 8);
        
        gears.setLayoutX(contentIndent - OVERFLOW - 50);
        boxBorder.relocate(contentIndent - OVERFLOW, 8.0);
        topHighlight.setStartX(contentIndent - OVERFLOW + 1);
        topHighlight.setEndX(contentIndent + BOX_CONTENT_WIDTH + 19);
        leftHighlight.setStartX(contentIndent - OVERFLOW - 1);
        leftHighlight.setEndX(contentIndent - OVERFLOW - 1);
        bottomShadow.setStartX(contentIndent - OVERFLOW);
        bottomShadow.setEndX(contentIndent + BOX_CONTENT_WIDTH + 19);
        rightShadow.setStartX(contentIndent + BOX_CONTENT_WIDTH + 19);
        rightShadow.setEndX(contentIndent + BOX_CONTENT_WIDTH + 19);

        instructions.relocate(contentIndent + RHS_OFFSET, y);
        instructions.setWrappingWidth(120);
        
        title.relocate(contentIndent, y);
        renameLink.relocate(contentIndent + title.getPrefWidth() - 30, y + (title.getPrefHeight() / 2));

        y += title.getHeight() + 16;
        
        desc.relocate(contentIndent, y);
        
        y+= desc.getHeight() + 8;       
        
        exp.relocate(contentIndent + 1, y);
        dflt.relocate(contentIndent + ProcedureInputsPanel.LABEL_WIDTH + 2
                * ProcedureInputsPanel.HPAD, y);
        
        exp.setWrappingWidth((int)BOX_CONTENT_WIDTH);
        y += Math.ceil(exp.getLayoutBounds().getHeight() + 8);
                
        inputs.relocate(contentIndent, y);

        y += Math.ceil(inputs.getHeight() + 8);

        runButton.relocate(contentIndent + BOX_CONTENT_WIDTH - runButton.getWidth() - 10, y);

        y += runButton.getHeight() + 14;
              
        boxBorder.setHeight(y - boxBorder.getLayoutY());
        setPrefHeight(y);
        leftHighlight.setEndY(y);
        bottomShadow.setStartY(y + 1);
        bottomShadow.setEndY(y + 1);
        rightShadow.setEndY(y);
    }
    
    public static void runProcedureUI(
            final ProcedureView pv,
            final boolean requestDebug) {
        final ProcedureModel pm = pv.getModel();
        
        if (pm.getSignature().getInputs().size() == 0) {
            runProcedure(pv, null, requestDebug);
        } else {
            Stage pop = new Stage();
            ProcedureHeader exec = new ExecProcedureHeader(pv, requestDebug, pop);
            pop.initModality(Modality.APPLICATION_MODAL);
            pop.setTitle("Run '" + pm.getName() + "'");
            pop.setHeight(500);
            pop.setWidth(750);
            AnchorPane pane = new AnchorPane();
            ScrollPanePlus sp = new ScrollPanePlus();
            sp.setStyle("-fx-background-color: -pal-SystemLightGray;");
            sp.setHbarPolicy(ScrollBarPolicy.NEVER);
            AnchorPane.setBottomAnchor(sp, 0.0);
            AnchorPane.setLeftAnchor(sp, 0.0);
            AnchorPane.setTopAnchor(sp, 0.0);
            AnchorPane.setRightAnchor(sp, 0.0);
            sp.setContent(exec);
            exec.prefWidthProperty().bind(sp.widthProperty());
            pane.getChildren().add(sp);
            Scene scene = new Scene(pane);
            Utilities.initPalStage(pop, scene);
            scene.setFill(Colors.SystemLightGray);                  
            pop.setScene(scene);                    
            pop.show(); 
        }
    }
    
    protected static void runProcedure(
            final ProcedureView pv,
            final List<Object> args,
            final boolean debug) {

        final BackendFacade backend     = BackendFacade.getInstance();
        final ProcedureEditController controller = pv.getController();
        final ProcedureModel pm         = pv.getModel();

        Callback<Integer, Void> call = new Callback<Integer, Void>() {
            public Void call(Integer location) {
                pv.getSelectionManager().setDisable(false);
                pv.getSelectionManager().selectNone();
                ATR located = BackendFacade.locate(location, pm);
                if (located instanceof StepModel) {
                    StepView step = pv.findStepView((StepModel)located);
                    if (step != null) {
                        pv.getScrollPane().scrollIntoView(step);
                        pv.getSelectionManager().selectOnly(step);
                    }
                }
                
                pv.getSelectionManager().setDisable(true);
                return null; 
            }
        };
        
        backend.runProcedure(pm, false, args, debug, call);
        controller.updateUndoAtLastSave();
    }
    
    public void prepareToClose() { 
        // Intentionally empty
    }
    
    // *********************** IVariableWatcher *******************************
    
    public void onAddedVariable (VariableModel newVar) {        
        if (newVar instanceof ProcedureInputVariableModel)
            inputs.addRow((ProcedureInputVariableModel)newVar);        
    }

    public void onRemovedVariable(VariableModel removedVar) {
        if (removedVar instanceof ProcedureInputVariableModel)
            inputs.removeRow((ProcedureInputVariableModel)removedVar);      
    }
    
    public void onRenamedVariable(VariableModel vm) {
        // Intentionally left blank
    }

    public void setContentWidth(double contentWidth) {
        this.contentWidth = contentWidth;
    }
    
    public final void handleInputsChanged() {
        if (procedureView.getModel().getSignature().getInputs().size() > 0) {
            exp.setText(INPUTS_MSG);
            dflt.setVisible(true);
        } else {
            exp.setText(NO_INPUTS_MSG);
            dflt.setVisible(false); 
        }
    }
    
    private boolean changeProcedureDescription(
            final String newDesc,
            final String currentDesc, 
            final boolean createUndo) {
        if (newDesc.equals(currentDesc)) {
            return false; 
        } else {            
            procedureView.getModel().setDescription(newDesc);
            desc.setText(procedureView.getModel().getDescription());
            
            if (createUndo) {
                IUndoable undo = new IUndoable() {
                    public boolean redo() {
                        return changeProcedureDescription(newDesc, currentDesc,
                                false);
                    }
                    public boolean undo() {
                        return changeProcedureDescription(currentDesc, newDesc,
                                false);
                    }
                    public String getDescription() {
                        return "Change procedure description to '"
                                + newDesc.substring(0,
                                        Math.min(newDesc.length(), 30))
                                + (newDesc.length() > 30 ? "...'" : "'");
                    }
                };
                controller.getUndoManager().pushUndo(undo);
            }
            return true;
        }
    }    
    
    // ********************** UI Components ***********************************
    
    private void initializeUIComponents() {             
        inputs = getProcedureInputsPanel(); 
        inputs.setMaxHeight(400);
        inputs.setPrefWidth(BOX_CONTENT_WIDTH);
        
        if (procedureModel.getName() != null
                && procedureModel.getName().length() > 0)
            title.setText(procedureModel.getName());
        else
            title.setText("New Procedure");
        
        procedureModel.nameProperty().addListener(new ChangeListener<String>() {
            public void changed(
                    final ObservableValue<? extends String> value, 
                    final String oldName, 
                    final String newName) {
                if (newName != null && newName.length() > 0)
                    title.setText(newName);
                else
                    title.setText("New Procedure");
            }
        });
        
        handleInputsChanged();       

        String descText = procedureModel.getDescriptionText();
        if (descText == null)
            descText = "";
        desc.setTextPlus(descText);
        
        configureRunButton();
        
        renameLink.visibleProperty().bind(
                procedureModel.nameProperty().isNotEqualTo(""));

        addChildren();
    }

    protected abstract void configureRunButton();

    protected abstract void addChildren();

    protected abstract double getBottomOffset();

    protected abstract ProcedureInputsPanel getProcedureInputsPanel();

    public abstract ConstantEditor configureProcedureInputRow(
            ProcedureInputRow row, ConstantEditor editor,
            boolean forceEditor);

    // box "title bar"
    protected final Rectangle titleBarBox = new Rectangle();
    {
        titleBarBox.setWidth(BOX_CONTENT_WIDTH + OVERFLOW + 19);
        titleBarBox.setHeight(TOP_PAD - 15);
        titleBarBox.setFill(new LinearGradient(-OVERFLOW, 0.0,
                BOX_CONTENT_WIDTH + 19, 0.0, false, null, new Stop[] {
                        new Stop(0.0, Colors.SystemLightGray),
                        new Stop(1.0, Colors.SystemGray) }));
        titleBarBox.setStroke(null);
    }
    
    // Textbox for the name (title) of this procedure
    protected final Label title = new Label(); {
        title.setPrefWidth(BOX_CONTENT_WIDTH);
        title.setTooltip(new Tooltip("Name of this procedure.\nClick \"rename\" to modify (requires a save)."));
        title.setCursor(Cursor.DEFAULT);
        title.setFont(Fonts.DIALOG_TITLE);
    }
    
    // Textbox for the description of this procedure
    protected final TextAreaPlus desc = new TextAreaPlus(); {
        desc.setCallback(new ITextInputCallback() {
            public void callback(String oldVal, String newVal) {
                if (!newVal.equals(oldVal))
                    changeProcedureDescription(newVal, oldVal, true);                
            }
        });
        desc.setId("header-textbox");
        //desc.setPromptText("Click to enter a description");
        desc.setPrefWidth(BOX_CONTENT_WIDTH - 20);
        desc.setPrefHeight(60);
        desc.setMaximumLength(400);
        desc.setWrapText(true);
        desc.setTooltip(new Tooltip(
                "Description of this procedure.\nClick to modify."));
    }

    // hyperlink for bringing up rename dialog
    protected final Hyperlink renameLink = new Hyperlink("rename"); {
        renameLink.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                controller.attemptRename(null, getScene());           
            }
        });
    }
    
    protected final Group gears = new Group(); {
        SVGPath svgpath = new SVGPath();
        svgpath.setFill(Colors.SystemGray);
        svgpath.setStroke(null);
        svgpath.setContent("M56.26,47.40 C54.48,46.03 52.93,44.42 51.61,42.58 L41.70,46.18 L38.20,36.52 L48.01,32.97 C47.89,31.80 47.87,30.66 47.92,29.54 C47.98,28.41 48.13,27.30 48.40,26.20 L39.23,21.92 L43.59,12.61 L52.91,17.02 C53.59,16.23 54.32,15.48 55.11,14.76 C55.90,14.05 56.75,13.41 57.67,12.85 L54.29,3.50 L63.94,0.00 L67.40,9.55 C68.49,9.47 69.55,9.48 70.60,9.58 C71.65,9.69 72.68,9.87 73.70,10.12 L78.08,0.74 L87.39,5.09 L82.87,14.75 C83.57,15.38 84.23,16.05 84.85,16.74 C85.46,17.42 86.03,18.17 86.56,18.99 L95.60,15.71 L99.10,25.36 L89.96,28.68 C90.07,29.64 90.10,30.58 90.05,31.49 C89.99,32.40 89.88,33.31 89.70,34.23 L98.92,38.50 L94.56,47.80 L85.55,43.58 C84.31,45.19 82.87,46.58 81.25,47.75 L84.75,57.40 L75.10,60.90 L71.69,51.50 C69.61,51.76 67.62,51.72 65.72,51.40 L61.51,60.43 L52.20,56.08 Z M61.76,33.44 C62.15,34.50 62.72,35.44 63.48,36.25 C64.24,37.05 65.09,37.68 66.05,38.13 C67.01,38.57 68.04,38.83 69.15,38.89 C70.25,38.96 71.34,38.80 72.40,38.41 C73.50,38.01 74.44,37.44 75.23,36.69 C76.03,35.93 76.65,35.08 77.09,34.12 C77.54,33.17 77.79,32.14 77.85,31.03 C77.92,29.92 77.76,28.84 77.37,27.78 C76.97,26.69 76.40,25.74 75.65,24.95 C74.89,24.16 74.05,23.53 73.09,23.09 C72.14,22.64 71.11,22.38 70.02,22.31 C68.93,22.24 67.84,22.41 66.74,22.81 C65.68,23.19 64.74,23.76 63.94,24.52 C63.13,25.28 62.50,26.14 62.05,27.09 C61.60,28.05 61.34,29.07 61.27,30.16 C61.20,31.25 61.37,32.34 61.76,33.44 Z ");
        SVGPath svgpath2 = new SVGPath();
        svgpath2.setFill(Colors.SystemGray);
        svgpath2.setStroke(null);
        svgpath2.setContent("M15.02,84.54 C13.61,82.79 12.49,80.87 11.62,78.77 L1.15,79.95 L-0.00,69.75 L10.37,68.58 C10.53,67.42 10.77,66.30 11.09,65.23 C11.41,64.15 11.81,63.10 12.33,62.09 L4.41,55.78 L10.82,47.76 L18.86,54.21 C19.70,53.60 20.59,53.05 21.52,52.53 C22.46,52.03 23.43,51.61 24.46,51.27 L23.35,41.39 L33.55,40.24 L34.69,50.34 C35.77,50.51 36.80,50.77 37.79,51.11 C38.79,51.46 39.75,51.88 40.68,52.35 L47.14,44.26 L55.17,50.67 L48.52,59.00 C49.05,59.78 49.54,60.58 49.98,61.39 C50.41,62.20 50.79,63.07 51.12,63.98 L60.67,62.91 L61.82,73.11 L52.16,74.20 C52.05,75.16 51.86,76.08 51.59,76.96 C51.32,77.83 51.00,78.68 50.62,79.54 L58.58,85.84 L52.18,93.87 L44.39,87.66 C42.81,88.94 41.09,89.95 39.24,90.71 L40.39,100.91 L30.19,102.07 L29.07,92.13 C26.99,91.89 25.06,91.39 23.29,90.64 L17.09,98.43 L9.05,92.03 Z M23.63,72.25 C23.76,73.37 24.10,74.41 24.65,75.38 C25.20,76.34 25.88,77.15 26.71,77.81 C27.54,78.46 28.48,78.95 29.54,79.28 C30.60,79.60 31.70,79.69 32.82,79.57 C33.97,79.43 35.03,79.10 35.97,78.55 C36.92,78.00 37.72,77.32 38.38,76.49 C39.04,75.67 39.52,74.72 39.84,73.66 C40.16,72.60 40.26,71.51 40.13,70.39 C40.00,69.23 39.66,68.18 39.12,67.23 C38.57,66.29 37.89,65.48 37.06,64.83 C36.24,64.17 35.31,63.68 34.26,63.35 C33.22,63.03 32.11,62.94 30.96,63.07 C29.84,63.20 28.79,63.53 27.83,64.08 C26.87,64.63 26.05,65.32 25.39,66.14 C24.73,66.97 24.25,67.91 23.92,68.95 C23.60,69.99 23.50,71.09 23.63,72.25 Z ");
        gears.getChildren().setAll(svgpath, svgpath2);
    }
    
    protected ProcedureInputsPanel inputs;
    
    // instructional text, explaining the purpose of the input vars
    protected final Text instructions = new Text(); {
        instructions.setText("When this procedure is run, it will display the information shown to the left, and ask the user for the inputs listed. \n \nClick to edit text and default values.");
        instructions.setFill(Colors.DisabledText);
        instructions.setStroke(null);
        instructions.setTextOrigin(VPos.BASELINE);
        instructions.setFont(Fonts.STANDARD);
    }
    
    protected static final String NO_INPUTS_MSG = "No inputs needed by this procedure.";
    protected static final String INPUTS_MSG = "Procedure Inputs:";
    
    // If there are no inputs, show a little label explaining there are none to display.
    protected final Text exp = new Text(); {
        exp.setFont(Fonts.DIALOG_ITALICIZED);
        exp.setFill(Colors.DisabledText);
    }
    
    protected final Text dflt = new Text("Default Values:"); {
        dflt.setFont(Fonts.DIALOG_ITALICIZED);
        dflt.setFill(Colors.DisabledText);
    }
    
    // box border
    protected final Rectangle boxBorder = new Rectangle(); {
        boxBorder.setWidth(BOX_CONTENT_WIDTH + OVERFLOW + 18);
        boxBorder.setFill(null);
        boxBorder.setStroke(Colors.SystemGray);
    }
    
    // highlight along the top border
    protected final Line topHighlight = new Line(); {
        topHighlight.setStartY(9);
        topHighlight.setEndY(9);
        topHighlight.setStrokeWidth(1.0);
        topHighlight.setStroke(Color.WHITE);
    }
    
    // highlight along the LHS border
    protected final Line leftHighlight = new Line(); {
        leftHighlight.setStartY(9);
        leftHighlight.setStrokeWidth(1.0);
        leftHighlight.setStroke(Color.WHITE);
    }

    // shadow under the bottom border
    protected final Line bottomShadow = new Line(); {
        bottomShadow.setStrokeWidth(1.0);
        bottomShadow.setStroke(Colors.SystemDarkGray);
    }
    
    // shadow on the RHS border
    protected final Line rightShadow = new Line(); {
        rightShadow.setStartY(9);
        rightShadow.setStrokeWidth(1.0);
        rightShadow.setStroke(Colors.SystemDarkGray);
    }
    
    protected final MenuItem run = new MenuItem("Run");
    protected final MenuItem debug = new MenuItem("Debug");
    
    // the "run" button, to complete the illusion
    protected final SplitMenuButton runButton = new SplitMenuButton(); {
        runButton.setText("Run");        
        runButton.setVisible(true);
        runButton.setPrefHeight(22.0);
        runButton.setPrefWidth(90.0);
        
        runButton.getItems().addAll(run, debug);
    }
}
