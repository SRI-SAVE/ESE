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

import javafx.scene.Node;
import javafx.scene.layout.Pane;

import com.sri.tasklearning.ui.core.SelectionManager;
import com.sri.tasklearning.ui.core.StorageAssistant;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;

/**
 * A pane that lists all known procedures in scrollpane and allows for their
 * selection. Also supports a double-click callback. This pane is most notably 
 * used inside of a ProcedureManager or SaveProcedureDialog to present the user 
 * with a dialog window that includes a procedure listing.  
 */
public class ProcedureBrowser extends Pane {
    private final LibraryActionList procList;
    private final LibraryInfoPanel infoPanel;
    
    public ProcedureBrowser(Runnable doubleClick) {
        super();
        
        getStyleClass().add("procedure-browser");
        infoPanel = new LibraryInfoPanel();
        procList = new LibraryActionList(infoPanel, true, doubleClick);
        procList.showActions(Namespace.BUILTIN, false, false);
        
        getChildren().addAll(procList, infoPanel);
    }
    
    @Override
    protected void layoutChildren() {
        double ipWidth = getWidth() * .45;
        procList.setPrefWidth(this.getWidth() - ipWidth);
        procList.setPrefHeight(this.getHeight() - 4);
        procList.setLayoutY(2);
        infoPanel.setPrefHeight(this.getHeight());
        infoPanel.setPrefWidth(ipWidth);
        
        super.layoutChildren();
        
        infoPanel.relocate(procList.getWidth(), 0);        
    }
    
    public SelectionManager getSelectionManager() {
        return procList.getSelectionManager();
    }
    
    public void select(ProcedureModel procedure) {
        for (Node node : procList.getRowSteps()) {
            LibraryRowStep lrs = (LibraryRowStep)node;
            if (lrs.getStepModel().getName().equals(procedure.getName())) {
                procList.select(lrs);
                break;
            }
        }
    }
    
    public void unregisterWatchers() {
        StorageAssistant.unregisterStorageUI(procList);
    }
}
