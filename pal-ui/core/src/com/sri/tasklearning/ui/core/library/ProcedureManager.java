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

import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.EditSession;
import com.sri.tasklearning.ui.core.EditSessionManager;
import com.sri.tasklearning.ui.core.ProcedureMap;
import com.sri.tasklearning.ui.core.SelectionManager;
import com.sri.tasklearning.ui.core.StorageAssistant;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.control.Alert.AlertResult;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.StepModel;

/**
 * A dialog window for opening, renaming, deleting, importing and exporting 
 * procedures. Hence being the procedure "manager". Utilizes 
 * {@link ProcedureBrowser} to list procedures. Some of the functionality of this 
 * class is provided in a public/static manner so that it can be reused by the 
 * special "Actions' button in the Adept UI (rename, import, export, delete).  
 */
public class ProcedureManager extends Pane {
    private final ProcedureBrowser browser;
    private final Stage stage; 
    private final Scene owner; 
    private final ProcedureOpener opener;
    private final ProcedureModel toClose;    
    
    public static final double HEIGHT = 450;
    public static final double WIDTH = 700;
    
    private static final double FOOTER_HEIGHT = 80;
    private static final double PADDING = 3;
    private static final double BUTTON_HEIGHT = 28;
    private static final double BUTTON_WIDTH = 90;
    private static final double BUTTON_PAD = 15;
    private static final double BUTTON_Y = HEIGHT - FOOTER_HEIGHT + (FOOTER_HEIGHT / 2) - (BUTTON_HEIGHT);
    
    public interface ProcedureOpener {
        public void open(ProcedureModel pm);
        public void close(ProcedureModel pm);
    }   
    
    public ProcedureManager(
            final Scene owner, 
            final ProcedureOpener opener,
            final ProcedureModel toClose) {
        this.opener  = opener;
        this.owner   = owner;
        this.toClose = toClose;

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setHeight(HEIGHT);
        stage.setWidth(WIDTH);
        stage.setTitle("Procedure Browser");
        stage.setResizable(false);
        
        stage.setOnHidden(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent e) {
                for (Node n : owner.getRoot().getChildrenUnmodifiable())
                    if (!n.disableProperty().isBound())
                        n.setDisable(false);
                browser.unregisterWatchers();
            }
        });        

        Scene scene = new Scene(this);
        stage.setScene(scene);

        Utilities.initPalStage(stage, scene);
        
        Runnable doubleClick = new Runnable() {
            public void run() {
                if (!openButton.isDisabled())
                    openButton.fire();
            }
        };
        browser = new ProcedureBrowser(doubleClick);
        browser.setPrefHeight(HEIGHT - FOOTER_HEIGHT);
        browser.setPrefWidth(WIDTH - 4 * PADDING);
        browser.relocate(PADDING, 0);
        
        actionButton = getManageButton(browser, this, opener); 
        
        initializeUIComponents();
        
        getChildren().addAll(browser, openButton, cancelButton, actionButton);
    }    
    
    public void showOpenDialog() {
        for (Node n : owner.getRoot().getChildrenUnmodifiable())
            if (!n.disableProperty().isBound())
                n.setDisable(true);
        
        stage.show();
        stage.toFront();    
    }
    
    public ProcedureBrowser getBrowser() {
        return browser;
    }
    
    // ************************** UI Components *******************************
    
    private void initializeUIComponents() {
        final SelectionManager mgr = browser.getSelectionManager();
        
        openButton.disableProperty().bind(mgr.numSelectedProperty().isEqualTo(0));
        openButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                if (toClose != null) 
                    opener.close(toClose);
                
                StepModel sm = ((LibraryRowStep) mgr.getSelectedItems().get(0))
                        .getStepModel();
                
                ProcedureModel proc = BackendFacade.getInstance()
                        .instantiateProcedure(sm.getName());
                
                stage.hide();
                opener.open(proc);                
            }
        });
    }
    
    private final Button openButton = new Button("Open"); {
        openButton.setTooltip(new Tooltip("Open the selected procedure."));
        openButton.relocate(WIDTH - BUTTON_WIDTH * 2 - 2 * BUTTON_PAD, BUTTON_Y);
        openButton.setDefaultButton(true);
        openButton.setPrefWidth(BUTTON_WIDTH);
        openButton.setPrefHeight(BUTTON_HEIGHT);
    };
    
    private final Button cancelButton = new Button("Cancel"); {
        cancelButton.setTooltip(new Tooltip("Close procedure browser window."));
        cancelButton.relocate(WIDTH - BUTTON_PAD - BUTTON_WIDTH, BUTTON_Y);
        cancelButton.setPrefWidth(BUTTON_WIDTH);
        cancelButton.setPrefHeight(BUTTON_HEIGHT);
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                stage.hide(); 
            }
        });
    };
    
    private final MenuButton actionButton;
    
    public static MenuButton getManageButton(
            final ProcedureBrowser browser, 
            final ProcedureManager manager,
            final ProcedureOpener opener) {
        
        MenuButton manageButton = new MenuButton("Actions");
        
        manageButton.relocate(BUTTON_PAD, BUTTON_Y);
        MenuItem menuItem1 = new MenuItem();
        
        menuItem1.setText("Import Procedure from a file\u2026");
        menuItem1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                importSelectedProcedure(browser);
            }
        });
        
        MenuItem menuItem2 = new MenuItem();
        menuItem2.setText("Export\u2026");
        menuItem2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                exportSelectedProcedure(browser);
            }
        });
        MenuItem menuItem3 = new MenuItem();
        menuItem3.setText("Rename\u2026");
        menuItem3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                renameSelectedProcedure(manager, browser);
            }
        });
        MenuItem menuItem4 = new MenuItem();
        menuItem4.setText("Delete");
        menuItem4.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    deleteSelectedProcedure(browser, manager, opener);
                }
            });
        

        final SelectionManager mgr = browser.getSelectionManager();

        menuItem2.disableProperty()
                .bind(mgr.numSelectedProperty().isEqualTo(0));
        menuItem3.disableProperty()
                .bind(mgr.numSelectedProperty().isEqualTo(0));
        menuItem4.disableProperty()
                .bind(mgr.numSelectedProperty().isEqualTo(0));
            
        manageButton.getItems().setAll(menuItem1,  
        		new SeparatorMenuItem(),
                menuItem2, menuItem3, menuItem4);
        
        return manageButton;
    }
    
    // *********************** Static "action" methods *************************
    
    private static void deleteSelectedProcedure(
            final ProcedureBrowser browser, 
            final ProcedureManager manager,
            final ProcedureOpener  opener) {
        final StepModel sel = ((LibraryRowStep) browser.getSelectionManager()
                .getTopSelection()).getStepModel();
        
        if (sel != null) {
            Set<String> callers = ProcedureMap.getInstance().getCallers(
                    sel.getName());
            boolean memoryRef = false;
            for (EditSession sess : EditSessionManager.getSessions()) {
                if (sess.getController().getModel().referencesFunctor(sel.getFunctor()))
                    memoryRef = true;
            }
            
            if (callers.size() > 0 || memoryRef) {
                Alert.show("Cannot delete procedure", 
                           "You may not delete a procedure that is referenced " +
                           "by other procedures as a step.",
                           AlertConfig.OK, null);
                return;
            }
            
            Callback<AlertResult, Void> call = new Callback<AlertResult, Void>() {
                public Void call(AlertResult result) {
                    if (result == AlertResult.YES) {
                        EditSession sess = EditSessionManager.findSessionByFunctor(sel.getFunctor());
                        if (sess != null && opener != null)
                            opener.close((ProcedureModel) sess.getController().getModel());
                        
                        StorageAssistant.deleteProcedure(sel.getName());                        
                    }
                    return null; 
                }
            };
            
            Alert.show("Confirm delete", 
                       "Are you sure you want to delete the procedure '" + 
                       sel.getName() + "'?", 
                       AlertConfig.YES_NO, call);
        }                                
    }
    
    private static void renameSelectedProcedure(
            final ProcedureManager manager,
            final ProcedureBrowser browser) {
        
        StepModel sel = ((LibraryRowStep) browser.getSelectionManager()
                .getTopSelection()).getStepModel();

        if (sel != null) {
            final String currentName = sel.getName();                                   

            Callback<CommonModel, Void> onRenameSuccess = 
                new Callback<CommonModel, Void>() {
                public Void call(CommonModel pm) {
                    if (pm.getName() != currentName)
                        browser.select((ProcedureModel) pm);
                    return null;
                }
            };            
            
            boolean renameOpenProcedure = false;
            EditSession renameSess = null;
            for (EditSession sess : EditSessionManager.getSessions())
                if (sess.getController().getModel().getName().equals(currentName)) {
                    renameOpenProcedure = true;
                    renameSess = sess;
                    break;
                }
            
            if (renameOpenProcedure) {
                ((ProcedureEditController) renameSess.getController()).attemptRename(onRenameSuccess, browser.getScene());
            } else {
                ProcedureModel pm = BackendFacade.getInstance()
                    .instantiateProcedure(sel.getName());
                ProcedureEditController.doRename(pm, onRenameSuccess, browser.getScene());
            }           
        }
    }
    
    public static  void exportSelectedProcedure(final ProcedureBrowser browser) {
        StepModel selection = ((LibraryRowStep) browser.getSelectionManager()
                .getTopSelection()).getStepModel();
        
        if (selection != null) {
            ProcedureModel pm = BackendFacade.getInstance()
                    .instantiateProcedure(selection.getName());
            StorageAssistant.exportProcedure(browser.getScene().getWindow(),
                    null, pm, true);
        }      
    }
    
    public static void importSelectedProcedure(final ProcedureBrowser browser) {
        StorageAssistant.importProcedure(browser.getScene().getWindow(), null);
           
    }
    
}
