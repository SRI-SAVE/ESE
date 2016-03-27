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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.DragDropManager;
import com.sri.tasklearning.ui.core.EditSessionManager;
import com.sri.tasklearning.ui.core.ProcedureMap;
import com.sri.tasklearning.ui.core.SelectionManager;
import com.sri.tasklearning.ui.core.StorageAssistant;
import com.sri.tasklearning.ui.core.StorageAssistant.IStorageUI;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.control.ToolTipper;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.ActionStepModel;
import com.sri.tasklearning.ui.core.step.ProcedureStepModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;

/**
 * This Pane appears on the right side of the library and contains the list of
 * actions (or procedures) that is currently selected in the left side of the
 * library. 
 */
public class LibraryActionList extends Pane implements IStorageUI {
    private final VBox view = new VBox();
    private final ScrollPanePlus scrollPane = new ScrollPanePlus();
    private final SelectionManager actionSelectionMgr = new SelectionManager(this);
    
    // The current list of actions, which may not be equal to the list of displayed actions
    private final ArrayList<LibraryRowStep> itemList = new ArrayList<LibraryRowStep>();
    private final LibraryInfoPanel infoPanel;
    private final boolean dialog;
    private final Runnable doubleClickHandler; 
    
    public LibraryActionList(LibraryInfoPanel infoPanel, boolean dialog, Runnable doubleClickHandler) {
        this.infoPanel = infoPanel;
        this.dialog = dialog;
        this.doubleClickHandler = doubleClickHandler; 
        
        StorageAssistant.registerStorageUI(this);
        
        scrollPane.getStyleClass().add("library-action-list-scroll");
        
        if (dialog)  
            scrollPane.setStyle("-fx-border-width: 0 1 0 0;");            
        else
            scrollPane.setStyle("-fx-border-width: 0 0 0 1;");            
        
        view.setStyle("-fx-border-color: null; -fx-border-width: 0;");
        view.prefWidthProperty().bind(scrollPane.prefWidthProperty().subtract(14));

        getStyleClass().add("library-action-list"); 
        
        scrollPane.setContent(view);        
        scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
        
        getChildren().addAll(scrollPane);
        
        layoutChildren();
    }   
    
    @Override
    protected void layoutChildren() {
        scrollPane.setPrefWidth(getWidth());
        scrollPane.setPrefHeight(getHeight() + 2);
        scrollPane.setTranslateX(1);
        scrollPane.setTranslateY(-1);
        for (LibraryRowStep step : itemList)
            step.setMaxWidth(getWidth()); 
        super.layoutChildren();
    }
    
    // *************************** IStorageUI *********************************
    
    public void refresh() {
        showActions(lastNamespace, false, false);
    }
    
    private void disableRecursiveProcedureSteps() {
        for (LibraryRowStep lrs : itemList) {
            lrs.setDisable(false);

            StepModel step = lrs.getStepModel();
            if (!dialog && step instanceof ProcedureStepModel) {
                CommonModel currentProc = EditSessionManager
                        .getActiveSession().getController().getModel();
                
                if (currentProc.getName() != null && !currentProc.getName().equals("")) {
                    Set<String> callers = ProcedureMap.getInstance()
                            .getCallers(currentProc.getName());              
                    
                    if (currentProc.getName().equals(step.getName()) || 
                        (callers != null && callers.contains(((ProcedureStepModel) step)
                                    .getName()))) {
                        lrs.setDisable(true);
                    }
                }
            }
        }
    }
    
    private void disableUnavailableActionSteps() {
        for (LibraryRowStep lrs : itemList) {
            if (lrs.isDisable())
                continue;
            
            StepModel step = lrs.getStepModel();
            if ((step instanceof ActionStepModel)) {
                ActionStepModel action = (ActionStepModel)step;
                for (ParameterModel input : action.getInputs()) {
                    TypeDef type = input.getTypeDef();
                    
                    boolean exist = TypeUtilities.allowExistingValue(type);
                    boolean fixed = TypeUtilities.allowFixedValue(type);
                    boolean ask   = TypeUtilities.allowAskUser(type); 
                    
                    // TODO if the type is explicitly nullable, then we 
                    // shouldn't disable the action.But we don't support 
                    // explicitly nullable types yet. 
                    if (!exist && !fixed && !ask)
                        lrs.setDisable(true);
                }
            }
        }
    }
    
    public SelectionManager getSelectionManager() {
        return actionSelectionMgr; 
    }
    
    public List<Node> getRowSteps() {
        return view.getChildren();
    }
    
    public void scrollToTop() {
        scrollPane.scrollToTop();
    }
    
    public void filterDisplayedActions(final String argText) {
        final String text = argText.toLowerCase();
        ArrayList<LibraryRowBasicUI> items = new ArrayList<LibraryRowBasicUI>();
        for(LibraryRowBasicUI row : itemList) {
            if(row.getText().toLowerCase().contains(text)) {
                items.add(row);
            }
        }
        view.getChildren().clear();
        view.getChildren().addAll(items);
    }
    
    private Namespace lastNamespace = null;
    public void showActions(Namespace ns, boolean recent, boolean toolkit) {
        lastNamespace = ns; 
        Set<StepModel> steps;
        ActionModelAssistant amAssistant = ActionModelAssistant.getInstance();
        
        if (recent) steps = amAssistant.getRecentActions();
        else if (toolkit) steps = amAssistant.getToolActions();
        else if (ns == null) steps = amAssistant.getActions();
        else steps = amAssistant.getActions(ns);
        
        actionSelectionMgr.selectNone();
        itemList.clear();
        
        for (StepModel step : steps) {
            LibraryRowStep lrs = new LibraryRowStep(step.getName(),
                    Utilities.getImage(step.getIconPath()), step, dialog);
            lrs.setOnMousePressed(getActionClickedEvent(lrs));
            itemList.add(lrs);
        }
        
        view.getChildren().clear();
        view.getChildren().addAll(itemList);
        
        disableRecursiveProcedureSteps();
        disableUnavailableActionSteps(); 
    }
    
    private EventHandler<MouseEvent> getActionClickedEvent(final LibraryRowStep step) {
        return new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                ToolTipper.hideTooltip();
                infoPanel.setAction(step.getStepModel());
                actionSelectionMgr.selectOnly(step);
                step.requestFocus();
                
                // Logic below does not apply when this control used in an open/save dialog window
                if (!dialog)
                    DragDropManager.getInstance().handleDragging(step, e);
                
                if (e.getClickCount() >= 2 && doubleClickHandler != null)
                    doubleClickHandler.run(); 
            }
        };
    }
    
    public void select(LibraryRowStep lrs) {
        actionSelectionMgr.selectOnly(lrs);
        infoPanel.setAction(lrs.getStepModel());
    }
}
