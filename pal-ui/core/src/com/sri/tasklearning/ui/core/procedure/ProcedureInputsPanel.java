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

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.common.InputVariableModel;
import com.sri.tasklearning.ui.core.control.Knurling;
import com.sri.tasklearning.ui.core.control.ToolTippedImageView;
import com.sri.tasklearning.ui.core.control.constant.ConstantEditor;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableView;

/**
 * Visualizes the inputs to a procedure within a {@link ProcedureHeader}. 
 * Optionally allows for for drag/drop reordering of inputs. 
 */
public final class ProcedureInputsPanel extends VBox {

    public static final double DEFAULT_EDITOR_WIDTH = 250.0;
    public static final double LABEL_WIDTH = 140.0;
    public static final double DELETE_BUTTON_WIDTH = 16.0;
    public static final double HPAD = 4;
    public static final double VPAD = 4;

    private static final int SPACING = 6;

    private final SignatureModel signatureModel;
    private final ProcedureHeader header;
    private final ProcedureView procView;
    private final ProcedureEditController controller;
    
    private final boolean executor;

    public ProcedureInputsPanel(
            final ProcedureHeader header, 
            final ProcedureView procView,
            final boolean executor) {
        super(SPACING);

        this.executor = executor;
        this.procView = procView;
        this.header = header;
        this.signatureModel = procView.getModel().getSignature();
        this.controller = procView.getController();

        if (procView != null) {
            this.procView.readOnlyProperty().addListener(
                new ChangeListener<Boolean>() {
                    public void changed(
                            final ObservableValue<? extends Boolean> value,
                            final Boolean oldVal, 
                            final Boolean newVal) {
                        if (getChildren().size() != 1)
                            for (Node n : getChildren())
                                ((ProcedureInputRow) n).knurling
                                        .setVisible(!newVal);
                    }
                });
        }

        setMinHeight(Region.USE_PREF_SIZE);
        setFillWidth(false);
        
        setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                // This is here so that input drag/drop gestures don't
                // begin a selection rectangle
                e.consume();    
            }
        });
        setOnDragDropped(new EventHandler<DragEvent>() {
            public void handle(DragEvent e) {
                final ProcedureInputRow row = (ProcedureInputRow)e.getGestureSource();
                final List<Node> children = getChildren();

                final int newIdx = children.indexOf(placeholder);
                final int oldIdx = row.origIndex;
                
                if (newIdx != row.origIndex) {
                    moveInputInSignature(row.origIndex, newIdx);
                    
                    IUndoable undo = new IUndoable() {
                        public boolean undo() {
                            moveInputInSignature(newIdx, oldIdx);
                            children.remove(row);
                            addToChildList(children, row, oldIdx);
                            handleInputsChanged();
                            return true;
                        }
                        
                        public boolean redo() {
                            moveInputInSignature(oldIdx, newIdx);
                            children.remove(row);
                            addToChildList(children, row, newIdx);
                            handleInputsChanged();
                            return true;
                        }
                        public String getDescription() {
                            return "Change order of procedure inputs";
                        }
                    };
                    controller.getUndoManager().pushUndo(undo);
                }
                    
                addToChildList(children, row, newIdx);
                children.remove(placeholder);
            }
        });

        if (signatureModel != null)
            for (InputVariableModel input : signatureModel.getInputs())
                getChildren().add(makeInputRow((ProcedureInputVariableModel) input));

        handleInputsChanged();
    }
    
    private void moveInputInSignature(int oldIdx, int newIdx) {
        // Update the signature model, create an undo
        ProcedureInputVariableModel pim = (ProcedureInputVariableModel) signatureModel
                .getInputs().remove(oldIdx);
        
        signatureModel.getInputs().add(newIdx, pim);
    }

    public SignatureModel getSignatureModel() {
        return signatureModel;
    }

    public void addRow(ProcedureInputVariableModel newInput) {
        signatureModel.getInputs().add(newInput);
        Pane row = makeInputRow(newInput);
        getChildren().add(row);
        handleInputsChanged();
    }

    public boolean removeRow(ProcedureInputVariableModel input) {
        int idx = signatureModel.getInputs().indexOf(input);
        if (idx >= 0) {
            signatureModel.getInputs().remove(input);
            getChildren().remove(idx);
            handleInputsChanged();;

            return true;
        }
        return false;
    }
    
    private void handleInputsChanged() {
        if (getChildren().size() == 1) {
            ProcedureInputRow row = (ProcedureInputRow)getChildren().get(0);
            row.knurling.setVisible(false);
        } else {
            for (Node n : getChildren())
                ((ProcedureInputRow)n).knurling.setVisible(true);                
        }
        header.handleInputsChanged();
    }

    public Pane makeInputRow(final ProcedureInputVariableModel input) {
        return new ProcedureInputRow(this, input);
    }
    
    public ProcedureInputRow findProcedureInputRow(
            ProcedureInputVariableModel input) {
        for (Node n : getChildren()) {
            if (!(n instanceof ProcedureInputRow))
                continue;
            
            ProcedureInputRow row = (ProcedureInputRow)n;
            if (row.input.equals(input))
                return row;
        }
        
        return null;
    }
    
    private void addToChildList(List<Node> children, Node n, int idx) {
        if (idx < children.size())
            children.add(idx, n);
        else
            children.add(n);
    }

    // ************************** ProcedureInputRow ****************************

    /**
     * Visualizes an individual procedure input with a term button, 
     * constant editor and possible knurling, all within a Pane that is 
     * skinned to look like a nice rounded rectangle. 
     */
    public final class ProcedureInputRow extends Pane {
        private final Knurling knurling = new Knurling("Drag to reorder inputs");
        private final ProcedureInputVariableModel input;
        private final ProcedureInputsPanel panel;
        private final VariableView view;
        private int origIndex = -1;
        private ConstantEditor editor;

        public ProcedureInputRow(
                final ProcedureInputsPanel panel,
                final ProcedureInputVariableModel input) {
        
        	super();
             
            this.panel = panel;
            this.input = input;
            
            if (!executor)
                getStyleClass().add("moveable-procedure-input");

            refreshEditor(false);

            view = new VariableView(input, null, null, panel.procView) {
                public List<MenuItem> buildTermOptionsMenu() {
                    List<MenuItem> items = super.buildTermOptionsMenu();

                    MenuItem mi = null;
                    if (input.getDefaultValue() == null) {
                        mi = new MenuItem("Provide a default value");
                        mi.setOnAction(new EventHandler<ActionEvent>() {
                            public void handle(ActionEvent e) {
                                refreshEditor(true);
                                if (editor.isDialog())
                                    editor.openDialog(editor.getPane()
                                            .getScene());
                                else
                                    editor.select();
                            }
                        });
                        items.add(0, mi);
                    } else {
                        mi = new MenuItem(
                         "Always ask the user for this input (clear default)");
                        mi.setOnAction(new EventHandler<ActionEvent>() {
                            public void handle(ActionEvent e) {
                                ((ProcedureEditController) controller).clearDefault(input, ProcedureInputRow.this);
                            }
                        });
                        MenuItem mi2 = new MenuItem("Copy default value");
                        mi2.setOnAction(new EventHandler<ActionEvent>() {
                            public void handle(ActionEvent e) {                                
                                ProcedureEditController.setClipboardTerm(input.getDefaultValue());
                            }
                        });
                        items.add(0, mi2);
                        items.add(0, mi);
                    }

                    return items;
                }
            };

            ((SplitMenuButton) view.getNode()).setTooltip(new Tooltip(
                    "A value to ask for when the procedure is executed.\n("
                            + TypeUtilities.getName(input.getTypeDef())
                            + ")\nClick to rename."));
            view.setReadOnly(true);

            if (executor)  {
                view.setDisable(true);
            } else {
                ReadOnlyBooleanProperty readOnly = procView.readOnlyProperty();
                view.disableProperty().bind(readOnly);
                errorButton.visibleProperty().bind(
                        input.referencedProperty().not()
                                .and(readOnly.not()));
                editor.getPane().disableProperty().bind(readOnly);

                getChildren().addAll(knurling);
            }
            
            if (!executor)
                registerDragEvents(this);

            getChildren().addAll(view.getNode(), errorButton);
        }
        
        public ProcedureInputVariableModel getInput() {
            return input;
        }
        
        public ToolTippedImageView getErrorButton() {
            return errorButton; 
        }

        public void refreshEditor(final boolean forceEditor) {
            if (editor != null && getChildren().contains(editor.getPane())) 
                getChildren().remove(editor.getPane());
            
            editor = ConstantEditor.create(input.getTypeDef(), null, procView); 
            
            editor = header.configureProcedureInputRow(this, editor,
                    forceEditor);

            editor.getPane().setPrefWidth(
                    ProcedureInputsPanel.DEFAULT_EDITOR_WIDTH - 60);           
                       
            getChildren().add(editor.getPane());            
        }

        @Override
        protected void layoutChildren() {
            editor.getPane().setPrefWidth(DEFAULT_EDITOR_WIDTH);
            
            super.layoutChildren();

            errorButton.relocate(
                    -(errorButton.getLayoutBounds().getWidth() + HPAD),
                    prefHeight(-1) / 2
                            - errorButton.getLayoutBounds().getHeight() / 2);

            double x = HPAD;

            SplitMenuButton vview = (SplitMenuButton) view.getNode();
            vview.relocate(x, prefHeight(-1) / 2 - vview.prefHeight(-1) / 2);
            x += ProcedureInputsPanel.LABEL_WIDTH + HPAD;

            editor.getPane().relocate(x,
                    prefHeight(-1) / 2 - editor.getPane().prefHeight(-1) / 2);

            x += DEFAULT_EDITOR_WIDTH + HPAD;

            knurling.relocate(x, prefHeight(-1) / 2
                    - knurling.getLayoutBounds().getHeight() / 2);
            x += knurling.getLayoutBounds().getWidth();
            
            setPrefWidth(x + HPAD);
            setPrefHeight(editor.getPane().getHeight() + 2 * VPAD);
        }
        
        private void registerDragEvents(final ProcedureInputRow row) {
            
            // This event is for when n is not the node being dragged, but when
            // it's the node being dragged over. It is not called  when it
            // is the node being dragged because we set mouseTransparent = true; 
            row.setOnDragOver(new EventHandler<DragEvent>() {
                public void handle(DragEvent e) {                    
                    if (e.getY() < row.getLayoutBounds().getMinY()
                            + (row.getLayoutBounds().getHeight() / 2)) {
                        updatePlaceholder(row, true);
                    } else {
                        updatePlaceholder(row, false);
                    }
                }
            });
            
            // This is the event that detects the start of a drag on a node and
            // registers all necessary events to handle that drag/drop gesture
            row.setOnDragDetected(new EventHandler<MouseEvent>() {
                public void handle(MouseEvent e) {
                    Dragboard db = row.startDragAndDrop(TransferMode.ANY);
                    
                    // Have to do the following to make the drag gesture occur
                    ClipboardContent test = new ClipboardContent();
                    test.putString("String");
                    db.setContent(test);

                    placeholder.setHeight(row.getHeight());                

                    final List<Node> children = panel.getChildren();
                    final Scene scene = panel.getScene();
                    final Pane root = (Pane)scene.getRoot();                    

                    final int idx = origIndex = children.indexOf(row);
                    children.add(idx, placeholder);
                    children.remove(row);

                    row.setOpacity(.75);
                    row.setMouseTransparent(true);
                    row.relocate(-e.getX(), -e.getY());
                    
                    // Add it at scene level for dragging purposes
                    root.getChildren().add(row);

                    // Scene's drag event is what will update the location of
                    // the ghost
                    scene.setOnDragOver(new EventHandler<DragEvent>() {
                        public void handle(DragEvent e) {
                            row.setTranslateX(e.getSceneX());
                            row.setTranslateY(e.getSceneY());
                            e.acceptTransferModes(TransferMode.ANY);
                        }
                    });

                    // Set a cleanup event on the drag node that should do 
                    // stuff that must happen regardless of whether the node
                    // was dropped successfully or not
                    row.setOnDragDone(new EventHandler<DragEvent>() {
                        public void handle(DragEvent e) {
                            scene.setOnDragOver(null);
                            row.relocate(0, 0);
                            row.setTranslateX(0);
                            row.setTranslateY(0);
                            row.setMouseTransparent(false);
                            row.setOpacity(1.0);
                            
                            root.getChildren().remove(row);                             
                            
                            // User didn't drop on the input panel
                            if (children.contains(placeholder)) {
                                children.remove(placeholder);
                                addToChildList(children, row, idx);
                            }
                        }
                     });
                }            
            });
        }
        
        private void updatePlaceholder(Node n, boolean before) {
            final List<Node> children = panel.getChildren();
            int idx = children.indexOf(n);
            
            if (!before)
                idx++;
            
            if (children.contains(placeholder)) {            
                int pIdx = children.indexOf(placeholder);
                if (pIdx == idx)
                    return;
                else {
                    if (pIdx < idx)
                        idx--;

                    children.remove(placeholder);
                    addToChildList(children, placeholder, idx); 
                }                
            } else {
                addToChildList(children, placeholder, idx);
            }
        }
        

        // ************************* UI Components *****************************
        
        private final ToolTippedImageView errorButton = 
            new ToolTippedImageView();
        {
            errorButton.setImage(Utilities.getImage("warning_small.png"));
            errorButton.setFitHeight(DELETE_BUTTON_WIDTH);
            errorButton.setFitWidth(DELETE_BUTTON_WIDTH);
            errorButton.setPreserveRatio(true);            
        };
    }
    
    private final Rectangle placeholder = new Rectangle(30, 30); 
    {
        placeholder.setFill(Color.TRANSPARENT);
    }
}
