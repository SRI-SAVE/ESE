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

package com.sri.tasklearning.ui.gseditor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.CoreUIApplication;
import com.sri.tasklearning.ui.core.DragDropManager;
import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.EditSession;
import com.sri.tasklearning.ui.core.EditSessionManager;
import com.sri.tasklearning.ui.core.EditSessionManager.ISessionListener;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.common.CommonModel;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.control.Alert.AlertResult;
import com.sri.tasklearning.ui.core.control.ScrollPanePlus;
import com.sri.tasklearning.ui.core.exercise.AnnotationPanel;
import com.sri.tasklearning.ui.core.exercise.ExerciseEditController;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.exercise.ExerciseView;
import com.sri.tasklearning.ui.core.exercise.ExerciseView.ExerciseOpener;
import com.sri.tasklearning.util.LogUtil;

public class GsEditor extends CoreUIApplication implements ISessionListener {

    public static final double VALUE_LIST_WIDTH = 156;
    public static final double LIBRARY_WIDTH = 310.0;
    public static final double WINDOW_MIN_WIDTH = 1000;
    public static final double WINDOW_MIN_HEIGHT = 500;
    public static final double WINDOW_DEFAULT_WIDTH = 1200;
    public static final double WINDOW_DEFAULT_HEIGHT = 800;   
    
    private static GsEditor instance; 
    
    private GsEditorToolBar toolBar;
    private TabPane tabPane;
    private SplitPane splitPane;
    // private GSLibraryPanel library;
    private EditController control;    
    
    /* The lame-o tab control doesn't provide a way to disable just the tabs
     * portion of it, so I have to overlay a rectangle to block mouse events
     * during procedure execution. */
    private final Rectangle blocker = new Rectangle();
    
    private Map<CommonModel, Tab> tabMap = new HashMap<CommonModel, Tab>();

    protected static final Logger log = LoggerFactory
            .getLogger(GsEditor.class);    
    
    public static void main(String[] args) {
        Application.launch(args);
    }
    
    @Override 
    public void start(final Stage stage) { 
   
    	if (!LogUtil.isLoggingConfigured())
            LogUtil.configureLogging("editor_log", GsEditor.class);
        
        BackendFacade.getInstance().connect("pal-ui-editor");
        
        this.stage = stage;
        stage.setOnHidden(new EventHandler<WindowEvent>() { 
            public void handle(WindowEvent e) { 
                BackendFacade.getInstance().disconnect();
            }});
        
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent e) {
                boolean unsaved = false;
                for (EditSession sess : EditSessionManager.getSessions())
                    if (sess.getController().isUnsavedChanges()) {
                        unsaved = true;
                        break;
                    }
                
                if (!unsaved)
                    return; 
                
                // There are unsaved changes. Make the user confirm that they
                // want to abandon them
                e.consume(); 
                Callback<AlertResult, Void> call = 
                    new Callback<AlertResult, Void>() {
                    public Void call(AlertResult result) {
                        if (result == AlertResult.YES) {
                            Platform.runLater(new Runnable() {
                                public void run() {
                                    stage.hide();
                                }
                            });
                        }
                        return null;    
                    }
                };
                Alert.show("Abandon unsaved changes?", 
                           "You currently have unsaved changes. " +
                           "Are you sure you want to exit?", 
                           AlertConfig.YES_NO, call);
            }
        });
        
        stage.setWidth(WINDOW_DEFAULT_WIDTH);
        stage.setHeight(WINDOW_DEFAULT_HEIGHT);        
        
        EditSessionManager.addSessionListener(this);
        
        initScene();
        
        stage.setScene(scene);           
        stage.show();
        
        this.instance = this; 
        
    }
    
    private ChangeListener<Boolean> unsavedListener = new ChangeListener<Boolean>() {
        public void changed(
                ObservableValue<? extends Boolean> value,
                Boolean oldVal, Boolean newVal) {
            updateWindowTitle(); 
        }
    };
    
    public void activeSessionChanged(
            final EditSession oldSession, 
            final EditSession newSession) {
        if (oldSession != null) {
            EditController oldC = oldSession.getController();
            oldC.unsavedChangesProperty().removeListener(unsavedListener);
        }
        
        if (newSession == null) {
            control = null;
            return; 
            
        }
        
        EditController newC = newSession.getController();
        newC.unsavedChangesProperty().addListener(unsavedListener);
        
        control = newC;
        
        updateWindowTitle();
        
        /* if (library != null)
            library.refresh(); */
    }

    private void initScene() {        
        final AnchorPane root = new AnchorPane();
        
        this.scene = new Scene(root);
              
        Utilities.initPalStage(stage, scene, "PALCoreESE.css");
                
        toolBar = new GsEditorToolBar(this);
        tabPane = new TabPane() {
            @Override
            public void layoutChildren() {
                super.layoutChildren();
                Bounds b = tabPane.localToScene(tabPane.getLayoutBounds());
                blocker.setLayoutX(b.getMinX());
                blocker.setLayoutY(b.getMinY());
            }
        };
        
        // library = new GSLibraryPanel();       

        BackendFacade.getInstance().debuggingProcedureProperty().addListener(
            new ChangeListener<Boolean>() {
            public void changed(
                final ObservableValue<? extends Boolean> value,
                final Boolean oldVal,
                final Boolean newVal) {
                toolBar.requestFocus();
                control.getView().setReadOnly(newVal);                                   
            }                    
        });
        
        blocker.setFill(Color.WHITE);
        blocker.setOpacity(0.5);
        blocker.widthProperty().bind(tabPane.widthProperty());
        blocker.setHeight(30);
       
        BackendFacade.getInstance().debuggingProcedureProperty().addListener(
            new InvalidationListener() {
            public void invalidated(Observable value) {
            	/* 
                if (BackendFacade.getInstance().isDebuggingProcedure())
                    root.getChildren().add(blocker);
                else
                    root.getChildren().remove(blocker); */ 
            }
        });
        
        initOverlay("Loading\u2026");
        
        /*
        EventHandler<ActionEvent> hideLibraryEvent = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                if (!libraryHidden) { 
                    hideLibrary(); 
                    toolBar.hideLibrary(); 
                }
                else { 
                    showLibrary(); 
                    toolBar.showLibrary(); 
                }
            }
        }; */
        
        // toolBar.setLibraryButtonEventHandler(hideLibraryEvent);
        
        splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPosition(0, 0.50);
        //splitPane.getItems().add(library);
        splitPane.getItems().addAll(tabPane);
        
        AnchorPane.setTopAnchor(toolBar, 0.0);
        AnchorPane.setLeftAnchor(toolBar, 0.0);
        AnchorPane.setRightAnchor(toolBar, 0.0);
                
        AnchorPane.setTopAnchor(splitPane, GsEditorToolBar.DEF_HEIGHT);
        AnchorPane.setLeftAnchor(splitPane, -1.0);
        AnchorPane.setRightAnchor(splitPane, -1.0);
        AnchorPane.setBottomAnchor(splitPane, 0.0);
        
        root.getChildren().addAll(splitPane, toolBar);
    }    
    
    /* 
    public void hideLibrary() { 
        lastDividerPos = splitPane.getDividerPositions()[0];
        splitPane.getItems().remove(library);        
        libraryHidden = true;
    }
    
    public void showLibrary() {
        splitPane.getItems().add(0, library);
        splitPane.setDividerPosition(0, lastDividerPos);
        libraryHidden = false;
    }         
    */ 
    
    public Stage getStage() {
        return stage; 
    }
    
    public Scene getScene() {
        return scene; 
    }    
    
    public com.sri.tasklearning.ui.core.exercise.ExerciseView.ExerciseOpener getExerciseOpener() {
        return exerciseOpener;
    }
      
    
    // ************************* Opening/Loading exercises *******************
    
    private final com.sri.tasklearning.ui.core.exercise.ExerciseView.ExerciseOpener exerciseOpener = new ExerciseOpener() {
        public void open(final ExerciseModel exercise) {
            if (exercise != null) {
                // show a "Loading" overlay
                showOverlay();

                Platform.runLater(new Runnable() {
                    public void run() {
                    	loadExercise(exercise);
                        hideOverlay();
                        control.getView().requestFocus();
                    }
                });

            } else {
                // procedure failed to load, so hide the overlay and leave the
                // current procedure on-screen
                log.error("Unable to load exercise ");
            }
        }
        
        public void close(final ExerciseModel procedure) {
            if (tabMap.containsKey(procedure)) {
                tabPane.getTabs().remove(tabMap.get(procedure));
                tabMap.get(procedure).getOnClosed().handle(null);
                tabMap.remove(procedure);
                EditSessionManager.removeSessionByName(procedure.getName());                                	
            }
        }
	
    };
       
    
    private void updateWindowTitle() {
        String name = control.getModel().getName();
        boolean unsavedChanges = control.isUnsavedChanges();
        
        if (name != null && name != "")
            stage.setTitle("Exercise Solution Editor - "              
            		+ name 
                    + (unsavedChanges ? "*" : ""));
        else
            stage.setTitle("Exercise Solution Editor - New Exercise Solution" 
                    + (unsavedChanges ? "*" : ""));
    }
    
    private void updateTabTitle(Tab tab, EditController control) {
        String name = control.getModel().getName();
        
        if (name == null || name.length() == 0)         	
            name = "New Exercise Solution"; 
        
        tab.setText(name + (control.isUnsavedChanges() ? " *" : ""));
    }
    
   
    //    
    // loads a given exercise into the UI
    //
    
     private void loadExercise(final ExerciseModel em) {

    	for (Entry<CommonModel, Tab> entry : tabMap.entrySet()) {
    		if (em.getName() != null && em.getName().length() > 0)
    			if (em.getName().equals(entry.getKey().getName())) {
    				tabPane.getSelectionModel().select(entry.getValue());
    				return;
    			}

    	}
    	em.nameProperty().addListener(new ChangeListener<String>() {
    		public void changed(
    				final ObservableValue<? extends String> value,
    				final String oldVal, 
    				final String newVal) {
    			updateWindowTitle();
    		}
    	});

    	long start = System.currentTimeMillis();        

    	// create the new exercise view
    	ExerciseEditController controller = new ExerciseEditController(em);
    	ScrollPanePlus procedureScrollPane = new ScrollPanePlus();
    	procedureScrollPane.setStyle("-fx-border-width: 0;");
    	procedureScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);


    	ExerciseView pv = new ExerciseView(em, controller, procedureScrollPane, exerciseOpener);

    	controller.setView(pv);

    	// put the procedure view inOto the scrollview        
    	procedureScrollPane.setContent(pv);
    	procedureScrollPane.scrollToTop();
    	
    

    	AnnotationPanel annotationPanel = new AnnotationPanel(controller);
 
    	AnchorPane pane = new AnchorPane();
    	AnchorPane.setTopAnchor(procedureScrollPane, 0.0);
    	AnchorPane.setLeftAnchor(procedureScrollPane, -1.0);
    	AnchorPane.setRightAnchor(procedureScrollPane, AnnotationPanel.DEF_WIDTH);
    	//AnchorPane.setRightAnchor(procedureScrollPane, 0.0);
    	AnchorPane.setBottomAnchor(procedureScrollPane, -1.0);        

    	AnchorPane.setTopAnchor(annotationPanel, 0.0);
    	AnchorPane.setRightAnchor(annotationPanel, 0.0);
    	AnchorPane.setBottomAnchor(annotationPanel, 0.0);

    	pane.getChildren().addAll(procedureScrollPane, annotationPanel);    	
    	
    	if (em.getOriginalExerciseModel() == null) {
    		pv.disableProperty().setValue(true);
    		annotationPanel.setDisable(true);
    	}

    	final EditSession sess = new EditSession(controller);
    	EditSessionManager.addSession(sess);
    	EditSessionManager.setActiveSession(sess);    
    
    	final Tab newTab = new Tab(em.getName());
    	newTab.setOnSelectionChanged(new EventHandler<Event>() {
    		public void handle(Event e) {
    			if (newTab.isSelected()) {
    				EditSessionManager.setActiveSession(sess);
    				DragDropManager.getInstance().configure(
    						sess.getController().getView()
    						.getSelectionManager(),
    						sess.getController().getView());
    			}
    		}
    	});
    	
    	newTab.setOnClosed(new EventHandler<Event>() {
    		public void handle(Event e) {
    	
    			sess.getController().getView().prepareToClose();        		
    			EditSessionManager.removeSessionByName(em.getName());
    			tabMap.remove(em);   
    			
    			ExerciseModel originalModel = em.getOriginalExerciseModel(); 
    			
    			if (originalModel != null) {
    	                    	
    				Tab otap = tabMap.get(originalModel);
    				
    				if (otap != null) {
    					tabPane.getSelectionModel().select(otap);
    					otap.getOnClosed().handle(null);
    					tabPane.getTabs().remove(otap);
    					tabMap.remove(originalModel);
    					EditSessionManager.removeSessionByName(originalModel.getName());
    				}
    			}
    		}	    		
    	}); 
    
    	InvalidationListener tabTitleListener = new InvalidationListener() {
    		public void invalidated(Observable val) {
    			updateTabTitle(newTab, control);
    		}
    	};
    	control.unsavedChangesProperty().addListener(tabTitleListener);
    	em.nameProperty().addListener(tabTitleListener);
 
    	newTab.setContent(pane);
    	tabPane.getTabs().add(newTab);
    	tabPane.getSelectionModel().select(newTab);
    	tabMap.put(em, newTab);

    	updateTabTitle(newTab, control);
    	updateWindowTitle();  

    	pv.layout();

    	// For some reason Mnemonics were causing us leaks even though we never
    	// explicitly created any
    	scene.getMnemonics().clear();

    	long end = System.currentTimeMillis();
    	log.info("Finished loading exercise '" + em.getName() + "' (total render time: " + (end - start) + " ms)");

    }

	public static GsEditor getInstance() {
		return instance;
	}
	
}
