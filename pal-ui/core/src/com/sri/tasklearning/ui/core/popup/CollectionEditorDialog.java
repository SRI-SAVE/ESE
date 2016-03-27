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

package com.sri.tasklearning.ui.core.popup;

import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import com.sri.pal.CollectionTypeDef;
import com.sri.pal.CustomTypeDef;
import com.sri.pal.PrimitiveTypeDef;
import com.sri.pal.SetDef;
import com.sri.pal.StructDef;
import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.control.Knurling;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.CompositeTermModel;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ParameterView;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;

/**
 * A popup for editing collection values. Note that this popup may trigger other 
 * nested popups that descend from ComplexEditorDialog, such as in
 * the case where the user edits a collection of structs, or a collection of
 * collections. The appearance of this popup differs based on whether a parent
 * {@code StepView} is provided to the constructor. If there's no parent 
 * StepView, it's assumed that the value being edited is a default value for
 * a procedure input, in which case the user shouldn't be able to reference
 * any variables in the collection. Therefore, that part of the dialog 
 * disappears. 
 */
public class CollectionEditorDialog extends ComplexEditorDialog {
    private final CollectionTypeDef collType;
    
    protected CollectionEditorDialog(
            final Scene scene, 
            final Node sender,
            final CollectionTypeDef type, 
            final CompositeTermModel argTerm,
            final Callback<Object, Void> argOnConfirmed,
            final StepView step,
            final ProcedureView procView) {
        super(scene, sender, type, argTerm, argOnConfirmed, step, procView);
        
        this.collType = type;
        
        String title = "Editing the details of "
                + TypeUtilities
                        .getCollectionExplanationText((CollectionTypeDef) type);        
      
        updateTitleText(title);
        refresh();
    }
    
    @Override
    public Pane getContent() {
        final Label lbl = new Label("Current elements");
        final Label possLbl = new Label("Possible variable additions");
        lbl.setLayoutX(PANE_PAD);
        final ScrollPanePlus currentValScroll = new ScrollPanePlus();
        currentValScroll.setContent(currentValBox);
        currentValBox.prefWidthProperty().bind(currentValScroll.prefWidthProperty().subtract(14));
        currentValBox.minHeightProperty().bind(currentValScroll.heightProperty().subtract(5));
        configScrollPane(currentValScroll);
        
        final ScrollPanePlus possValScroll;
        
        if (step == null)
            possValScroll = null;
        else {
            possValScroll = new ScrollPanePlus();
            possValScroll.setContent(possValBox);
            possValBox.prefWidthProperty().bind(possValScroll.prefWidthProperty().subtract(14));
            possValBox.minHeightProperty().bind(possValScroll.heightProperty().subtract(5));
            configScrollPane(possValScroll);
        }

        final Pane pane = new Pane() {
            @Override
            protected void layoutChildren() {
                double splitFactor = (step == null) ? 1 : 2;
                currentValScroll.setPrefWidth((this.prefWidth(-1) - 3 * PANE_PAD)
                                / splitFactor);
                currentValScroll.setPrefHeight(this.prefHeight(-1)
                        - (lbl.getHeight() + 10) - PANE_PAD);

                if (possValScroll != null) {
                    possValScroll.setPrefWidth(currentValScroll.getPrefWidth());
                    possValScroll.setPrefHeight(currentValScroll.getPrefHeight());
                }

                super.layoutChildren();

                link.relocate(PANE_PAD, currentValScroll
                        .getLayoutBounds().getMaxY() + 25);
                currentValScroll.relocate(PANE_PAD, lbl.getHeight() + 10);
                
                if (possValScroll != null) {
                    possValScroll.relocate(currentValScroll.getWidth() + 2
                            * PANE_PAD, currentValScroll.getLayoutY());
                    possLbl.relocate(possValScroll.getLayoutX(), lbl.getLayoutY());
                }
            }
        };

        pane.getChildren().addAll(lbl, link, currentValScroll);
        
        if (possValScroll != null)
            pane.getChildren().addAll(possValScroll, possLbl);
        
        return pane;
    }
    
    private void configScrollPane(ScrollPane scroll) {
        scroll.setStyle("-fx-background-color: -pal-LoopBackgroundDient;");
        scroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
    }
    
    @Override
    protected void refresh() {
        final List<Node> children = currentValBox.getChildren();
        children.clear();

        for (ParameterModel pm : newTerm.getInputs()) {
            final ElementPane n = new ElementPane(pm);            
            children.add(n);
        }
        
        if (step != null) {
            ProcedureEditController controller = procView.getController();
            possValBox.getChildren().clear();           
            
            final ParameterModel argument = new ParameterModel(null, null,
                    collType.getElementType(), new ConstantValueModel("test"),
                    ParameterModel.INPUT_FUNCTOR, false, newTerm);

            argument.setTerm(new ConstantValueModel("test"));

            final List<VariableModel> existingValues = 
                    controller.getVariableManager().getValidInputs(
                            step.getStepModel(), argument, null);
            
            for (VariableModel vm : existingValues) {
                ParameterModel pm = new ParameterModel(null, null,
                        collType.getElementType(), vm,
                        ParameterModel.INPUT_FUNCTOR, false, newTerm);

                final ElementPane n = new ElementPane(pm);
                possValBox.getChildren().add(n);
            }
            
            final List<FunctionModel> functionCalls = controller
                    .getSuggestedFunctionCalls(step.getStepModel(), argument);
            for (FunctionModel func : functionCalls) {
                ParameterModel pm = new ParameterModel(null, null,
                        collType.getElementType(), func,
                        ParameterModel.INPUT_FUNCTOR, false, newTerm);
                final ElementPane n = new ElementPane(pm);
                possValBox.getChildren().add(n);
            }
        }
    }
    
    @Override
    protected boolean  saveChanges() {
        newTerm.getInputs().clear();
        
        for (Node n : currentValBox.getChildren()) {
            if (n instanceof ElementPane) {
                ParameterModel pm = ((ElementPane)n).getParameterModel();
                
                if (pm.getTerm() == null
                        || (pm.getTerm() instanceof NullValueModel))
                    continue;
                else if (pm.getTerm() instanceof ConstantValueModel) {
                    ConstantValueModel c = (ConstantValueModel)pm.getTerm();
                    if (c.getValue() == null)
                        continue;
                }
                newTerm.getInputs().add(((ElementPane)n).getParameterModel());
            }
        }
        
        return true;
    }
    
    private void registerDragEvents(final ElementPane n) {
        final List<Node> children = currentValBox.getChildren();
        
        // This event is for when n is not the node being dragged, but rather
        // the node being dragged over. It is not called in the case when it
        // is the node being dragged because we set mouseTransparent = true; 
        n.setOnDragOver(new EventHandler<DragEvent>() {
            public void handle(DragEvent e) {
                if (!children.contains(n))
                    return;
                
                if (e.getY() < n.getLayoutBounds().getMinY()
                        + (n.getLayoutBounds().getHeight() / 2)) {
                    updatePlaceholder(n, true);
                } else {
                    updatePlaceholder(n, false);
                }
            }
        });
        
        // This is the event that detects the start of a drag on a node and
        // registers all necessary events to handle that drag/drop gesture
        n.setOnDragDetected(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                Dragboard db = n.startDragAndDrop(TransferMode.ANY);
                
                // Have to do the following to make the drag gesture occur
                ClipboardContent test = new ClipboardContent();
                test.putString("String");
                db.setContent(test);

                n.setPrefWidth(n.getWidth());
                placeholder.setHeight(n.getHeight());                

                int idx = -1;
                final Pane root =((Pane)currentValBox.getScene().getRoot());
                
                if (children.contains(n)) {
                    idx = children.indexOf(n);
                    children.add(idx, placeholder);
                    children.remove(n);
                } else {
                    idx = possValBox.getChildren().indexOf(n);
                    possValBox.getChildren().remove(n);

                    if (!(type instanceof SetDef)) {
                        // make a replacement for lists/bags since dups are allowed
                        ParameterModel pm = n.getParameterModel();
                        ParameterModel copy = new ParameterModel(null, null, pm
                                .getTypeDef(), pm.getTerm(),
                                ParameterModel.INPUT_FUNCTOR, false, newTerm);
                        ElementPane replacement = new ElementPane(copy);
                        addToChildList(possValBox.getChildren(), replacement, idx);
                    }
                }

                final boolean setDrop;
                final TermModel nTerm = n.getParameterModel().getTerm();
                if (type instanceof SetDef && nTerm instanceof VariableModel ||
                    (nTerm instanceof FunctionModel && 
                     FunctionModel.isAccessorFunction((FunctionModel)nTerm))) {
                    setDrop = true;
                } else
                    setDrop = false;

                n.setOpacity(.75);
                n.setMouseTransparent(true);
                n.relocate(-e.getX(), -e.getY());
                
                // Add it at scene level for dragging purposes
                root.getChildren().add(n);

                // Scene's drag event is what will update the location of
                // the ghost
                scene.setOnDragOver(new EventHandler<DragEvent>() {
                    public void handle(DragEvent e) {
                        n.setTranslateX(e.getSceneX());
                        n.setTranslateY(e.getSceneY());
                        e.acceptTransferModes(TransferMode.ANY);
                    }
                });

                // Set a cleanup event on the drag node that should do 
                // stuff that must happen regardless of whether the node
                // was dropped successfully or not
                final int fIdx = idx;
                n.setOnDragDone(new EventHandler<DragEvent>() {
                    public void handle(DragEvent e) {
                        scene.setOnDragOver(null);
                        n.setPrefWidth(Region.USE_COMPUTED_SIZE);
                        n.relocate(0, 0);
                        n.setTranslateX(0);
                        n.setTranslateY(0);
                        n.setMouseTransparent(false);
                        n.setOpacity(1.0);
                        
                        root.getChildren().remove(n); 
                                                
                        if (setDrop && !children.contains(n))  {
                            int idx = (fIdx == -1) ? possValBox.getChildren()
                                    .size() : fIdx;
                            addToChildList(possValBox.getChildren(), n, idx);
                        }
                        
                        if (children.contains(placeholder))
                            children.remove(placeholder);                           
                    }
                 });
            }            
        });
    }
    
    private void updatePlaceholder(Node n, boolean before) {
        final List<Node> children = currentValBox.getChildren();
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
    
    private void addToChildList(List<Node> children, Node n, int idx) {
        if (idx < children.size())
            children.add(idx, n);
        else
            children.add(n);
    }
    
    private void handleAddition() {
        TypeDef subType = collType.getElementType();
        final ParameterModel pm = new ParameterModel(null, null, subType, null,
                ParameterModel.INPUT_FUNCTOR, false, newTerm);
        
        if (subType instanceof PrimitiveTypeDef ||
            subType instanceof CustomTypeDef ||
            subType instanceof PrimitiveTypeDef) {
            ElementPane ep = new ElementPane(pm);
            currentValBox.getChildren().add(ep);
        } else if (subType instanceof CollectionTypeDef ||
                   subType instanceof StructDef) {
            final ComplexEditorDialog ced = ComplexEditorDialog.create(scene, link,
                    subType, null, null, step, procView);
            Callback<Object, Void> onSubConf = new Callback<Object, Void>() {
                public Void call(final Object obj) {
                    pm.setTerm(ced.getTerm());
                    ElementPane ep = new ElementPane(pm);
                    currentValBox.getChildren().add(ep);
                    return null;
                }
            };
            ced.setOnConfirmed(onSubConf);
            ced.showDialog();
        }
    }
    
    // ************************* UI Components ********************************

    private final Rectangle placeholder = new Rectangle(30, 30); 
    {
        placeholder.setFill(Color.TRANSPARENT);
    }
    
    private final VBox currentValBox = new VBox(10);
    {
        currentValBox.setPadding(new Insets(10, 10, 10, 10));
        currentValBox.setOnDragDropped(new EventHandler<DragEvent>() {
            // The VBox actually handles the drop event when an element
            // is drag/dropped. This makes it easier to know the drop location
            public void handle(DragEvent e) {
                final Node n = (Node)e.getGestureSource();
                final List<Node> children = currentValBox.getChildren();
                if (children.contains(placeholder)) {
                    int idx = children.indexOf(placeholder);
                    addToChildList(children, n, idx);

                    children.remove(placeholder);
                } else {
                    children.add(n);
                }
            }
        });
    }
    
    private final VBox possValBox = new VBox(10);
    {
        possValBox.setPadding(new Insets(10, 10, 10, 10));
    }
    
    private final Hyperlink link = new Hyperlink("Add a new value...");
    {
        link.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                handleAddition();     
            }
        });
    }
    
    // ********************** ElementPane *************************************
    
    protected class ElementPane extends BorderPane {
        protected final ParameterModel pm;
        protected final Node node;
        protected final Knurling knurling;
        
        protected ElementPane(final ParameterModel pm) {
            super();
            
            this.pm = pm;
            
            node = getNodeForParameterModel(pm, false);                   

            knurling = new Knurling("Click-and-drag to move values");
            if (!(node instanceof ParameterView))
                ((Pane)node).prefWidthProperty().bind(this.widthProperty().subtract(50));            
           
            BorderPane.setAlignment(node, Pos.CENTER_LEFT);
            BorderPane.setAlignment(knurling, Pos.CENTER_RIGHT);
            setStyle("-fx-background-color: lightsteelblue; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: lightslategray;");
            setPadding(new Insets(5, 10, 5, 10));
            setLeft(node);
            setRight(knurling);
            
            registerDragEvents(this);
        }
        
        @Override
        protected void layoutChildren() {
            super.layoutChildren();
        }
        
        protected ParameterModel getParameterModel() {
            return pm; 
        }
    }
}
