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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.sri.pal.PALException;
import com.sri.pal.training.core.exercise.Exercise;
import com.sri.pal.training.core.storage.ExerciseStorage;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.BackendInterface;
import com.sri.tasklearning.ui.core.EditController;
import com.sri.tasklearning.ui.core.EditSession;
import com.sri.tasklearning.ui.core.EditSessionManager;
import com.sri.tasklearning.ui.core.EditSessionManager.ISessionListener;
import com.sri.tasklearning.ui.core.IUndoWatcher;
import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.control.Alert.AlertResult;
import com.sri.tasklearning.ui.core.exercise.ExerciseModel;
import com.sri.tasklearning.ui.core.exercise.InfoPanel;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

public class GsEditorToolBar extends ToolBar implements ISessionListener, IUndoWatcher {
	
	private final BackendInterface backend = BackendFacade.getInstance();

	private final GsEditor editor;   

	private EditController control;

	public static final double DEF_HEIGHT = 50;          

	public GsEditorToolBar(GsEditor argEditor) {
		
		editor = argEditor;           	
		EditSessionManager.addSessionListener(this);

		setPrefHeight(DEF_HEIGHT);

		getItems().addAll(
				openExerciseButton,
				saveButton, saveAsButton,
				finalizeButton, 
				getSeparator(), 
				// openURLExerciseButton,
				// saveURLButton,
				// saveAsURLButton,      		
				//getSeparator(), 
				showOriginalButton 
				// , restoreOriginalButton 
				);
		
	}

	//********************** IUndoWatcher **************************************

	@Override
	public void onUndoChanged(IUndoable action) {
		// toggleUndoRedoButtons();
	}

	@Override
	public void onUndoCleared() {
		// toggleUndoRedoButtons();
	}  

	//************************ ISessionListener ********************************

	@Override
	public void activeSessionChanged(
			final EditSession oldSession, 
			final EditSession newSession) {

		if (oldSession != null)
			oldSession.getController().getUndoManager().unregisterWatcher(this);

		if (newSession == null) {
			control = null;
			return; 
		}

		control = newSession.getController();
		control.getUndoManager().registerWatcher(this);

		/* 
		deleteButton.disableProperty().bind(
				control.getView().getSelectionManager()
				.numSelectedProperty().isEqualTo(0));

		control.getView().readOnlyProperty().addListener(
				new ChangeListener<Boolean>() {
					public void changed(
							final ObservableValue<? extends Boolean> value,
							final Boolean oldVal,
							final Boolean newVal) {
						
						if (newVal) {
							redoButton.setDisable(true);
							undoButton.setDisable(true);
						} else
							toggleUndoRedoButtons();
					}
				});
		 */ 
		
		openExerciseButton.disableProperty().bind(
				ExerciseModel.getActiveModelProperty().isNotNull().
				and(ExerciseModel.getActiveModelProperty().getValue().getReadOnlyProperty()));   
		
		/* 
		openURLExerciseButton.disableProperty().bind(
				ExerciseModel.getActiveModelProperty().isNotNull().
				and(ExerciseModel.getActiveModelProperty().getValue().getReadOnlyProperty()));
				*/   
	
		saveButton.disableProperty().bind(
				ExerciseModel.getActiveModelProperty().isNull().
				or(ExerciseModel.getActiveModelProperty().getValue().getHasFileSourceProperty().not())); 

		saveAsButton.disableProperty().bind(
				ExerciseModel.getActiveModelProperty().isNull()); 
		
		finalizeButton.disableProperty().bind(
				ExerciseModel.getActiveModelProperty().isNull().or(ExerciseModel.getActiveModelProperty().getValue().getReadOnlyProperty())); 

		/* 
		saveURLButton.disableProperty().bind(
				ExerciseModel.getActiveModelProperty().isNull().
				or(ExerciseModel.getActiveModelProperty().getValue().getHasURLSourceProperty().not()).
				or(ExerciseModel.getActiveModelProperty().getValue().getReadOnlyProperty()));

		saveAsURLButton.disableProperty().bind(
				ExerciseModel.getActiveModelProperty().isNull().
				or(ExerciseModel.getActiveModelProperty().getValue().getReadOnlyProperty()));
		*/
		
		showOriginalButton.disableProperty().bind(			
				ExerciseModel.getActiveModelProperty().isNull().
				or(ExerciseModel.getActiveModelProperty().getValue().getHasOriginalExerciseModelProperty().not())); 

		/* restoreOriginalButton.disableProperty().bind(			
				ExerciseModel.getActiveModelProperty().isNull().
				or(ExerciseModel.getActiveModelProperty().getValue().getHasOriginalExerciseModelProperty().not())); */ 

		// toggleUndoRedoButtons();
		
	}

	//************************************************************************* 

	
	@Override
	protected void layoutChildren() {
		super.layoutChildren();
		
	}

	public Button getButton(String text, String tooltip, String iconPath) {
		Button button = new Button();
		button.setContentDisplay(ContentDisplay.TOP);
		button.setTextAlignment(TextAlignment.CENTER);
		button.setText(text);
		button.setGraphic(Utilities.getImageView(iconPath));

		if (tooltip != null)
			button.setTooltip(new Tooltip(tooltip));

		return button;
	}

	private Separator getSeparator() {
		return new Separator(Orientation.VERTICAL);
	}

	/* public void setLibraryButtonEventHandler(EventHandler<ActionEvent> e) {
        showLibButton.setOnAction(e);
    } */

    /*
    public void hideLibrary() {
        showLibButton.setText("Show Library");
        showLibButton.setGraphic(Utilities
                .getImageView("toolbar/library_show.png"));
    }

    public void showLibrary() {
        showLibButton.setText("Hide Library");
        showLibButton.setGraphic(Utilities
                .getImageView("toolbar/library_hide.png"));
    }
     */

	/*
	public void toggleUndoRedoButtons() {
		UndoManager um = control.getUndoManager();

		if (!control.getView().isReadOnly()) {
			undoButton.setDisable(!um.canUndo());
			redoButton.setDisable(!um.canRedo());
		}

		if (um.canUndo())
			undoButton.setTooltip(new Tooltip(um.peekUndo().getDescription()));
		else
			undoButton.setTooltip(null);

		if (um.canRedo())
			redoButton.setTooltip(new Tooltip(um.peekRedo().getDescription()));
		else
			redoButton.setTooltip(null);
	} */

	// *********************** UI Component Declarations ***********************

	private final Separator errorSep = getSeparator();

	/* 
    private final Button showLibButton = new Button(); {
        showLibButton.setContentDisplay(ContentDisplay.TOP);
        showLibButton.setTextAlignment(TextAlignment.CENTER);
        showLibButton.setPrefWidth(90);
        showLibrary();
    } */

	private final Button openExerciseButton = getButton("Open File",
			//control == null || control.getModel() == null ? "Save" : control.getModel() instanceof ExerciseModel ? "Save the current procedure." : "Save the current exercise.",
			"Open an existing exercise.",
			"toolbar/folder_yellow_open.png");
	{
		openExerciseButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				try {
					openExercise();
				} catch (PALException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		
		openExerciseButton.disableProperty().set(false);
	}


	private final Button openURLExerciseButton = getButton("Open URL",
			//control == null || control.getModel() == null ? "Save" : control.getModel() instanceof ExerciseModel ? "Save the current procedure." : "Save the current exercise.",
			"Open an existing exercise.",
			"toolbar/folder_yellow_open.png");
	{
		openURLExerciseButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				openURLExercise();
			}
		});
		
		openURLExerciseButton.disableProperty().set(false);
	}


	private final Button saveButton = getButton("Save File",
			//control == null || control.getModel() == null ? "Save" : control.getModel() instanceof ExerciseModel ? "Save the current procedure." : "Save the current exercise.",
			"Save current exercise.",
			"toolbar/save.png");
	{
		saveButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				
				Alert.show("Question", "Do you want to overwrite existing file\n'" + ExerciseModel.getActiveModelProperty().getValue().getFileSource() + "'?",   
						AlertConfig.YES_NO, new Callback<AlertResult, Void> () {

					@Override
					public Void call(AlertResult arg0) { 
						
						if (arg0 == AlertResult.YES) {
					
							control.attemptSave(true, false, null, getScene());
							ExerciseModel model = ExerciseModel.getActiveModelProperty().getValue();
							model.updateNameFile();
						}
						
						return null; 
						
					}
				}
				);
			}
		}
		); 

		saveButton.disableProperty().set(true);
	}


	/*
	private final Button saveURLButton = getButton("Put to Server",
			//control == null || control.getModel() == null ? "Save" : control.getModel() instanceof ExerciseModel ? "Save the current procedure." : "Save the current exercise.",
			"Upload current exercise or procedure to server.",
			"toolbar/save.png");
	{
		saveURLButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {

				ExerciseModel model = ExerciseModel.getActiveModelProperty().getValue();
				
				try {
					
					if (model.getUrlSource() == null) {

						String url = Configuration.EXERCISE_TEST_BASE_URL + model.getExercise().getName().toString(); 

						if (! url.endsWith(".xml")) {
							url += ".xml"; 
						}

						URL url2 = new URL(url); 								
						url = new URI(url2.getProtocol(), url2.getUserInfo(), url2.getHost(), url2.getPort(), url2.getPath(), url2.getQuery(), url2.getRef()).toString();									
					
						model.setUrlSource(new URL(url));
					
					} 

					StorageAssistant.putExerciseFileHTTP(model);
					model.updateNameUrl();

				} catch (MalformedURLException | URISyntaxException e1) {

					UtilWidgets.messagePopup("Error", "Error: " +e); 

				} 
			}
		});

		saveURLButton.disableProperty().set(true);
	}
     */ 
	
	private final Button saveAsButton = getButton("Save As File",
			// control == null || control.getModel() == null ? "Save a copy" : control.getModel() instanceof ExerciseModel ? "Save a copy of the current procedure." : "Save a copy of the current exercise.",
			"Save a copy of current exercise or procedure.", 
			"toolbar/saveas.png");
	{
		saveAsButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				control.attemptSave(true, true, null, getScene());
				ExerciseModel model = ExerciseModel.getActiveModelProperty().getValue();
				model.updateNameFile();
			}
		});

		saveAsButton.disableProperty().set(true);
	}

	/* 
	private final Button saveAsURLButton = getButton("Put As to Server",
			// control == null || control.getModel() == null ? "Save a copy" : control.getModel() instanceof ExerciseModel ? "Save a copy of the current procedure." : "Save a copy of the current exercise.",
			"Save a copy of current exercise or procedure.", 
			"toolbar/saveas.png");
	{
		saveAsURLButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				
				final ExerciseModel model = ExerciseModel.getActiveModelProperty().getValue();
				
				try { 
					
				if (model.getUrlSource() == null) {

					String url = Configuration.EXERCISE_TEST_BASE_URL + model.getExercise().getName().toString(); 

					if (! url.endsWith(".xml")) {
						url += ".xml"; 
					}

					URL url2;
					url2 = new URL(url);
					url = new URI(url2.getProtocol(), url2.getUserInfo(), url2.getHost(), url2.getPort(), url2.getPath(), url2.getQuery(), url2.getRef()).toString(); 				
				
					model.setUrlSource(new URL(url.toString()));
				
				}  
						
				UtilWidgets.simpleURLEditor("Save to Server URL", model, new Runnable() {
			 
					@Override
					public void run() {
						
						StorageAssistant.putExerciseFileHTTP(ExerciseModel.getActiveModelProperty().getValue());
						model.updateNameUrl();
				
					} 
				});
				
			} catch (MalformedURLException | URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			}
		}); 

		saveAsURLButton.disableProperty().set(true);
	}	
	*/ 
	
	/*
	private final Button undoButton = getButton("Undo", null,
			"toolbar/undo2.png");
	{
		undoButton.setDisable(true);
		undoButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				if (control.getUndoManager().canUndo())
					control.getUndoManager().undo();
			}
		});
	}

	private final Button redoButton = getButton("Redo ", null,
			"toolbar/redo.png");
	{
		redoButton.setDisable(true);
		redoButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				if (control.getUndoManager().canRedo())
					control.getUndoManager().redo();
			}
		});
	}

	private final Button deleteButton = getButton("Delete",
			"Delete the selected steps.", "toolbar/editdelete.png");
	{
		deleteButton.setDisable(true);
		deleteButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				CommonView pv = control.getView();
				List<ISelectable> items = pv.getSelectionManager()
						.getSelectedItems();
				if (items.size() > 0) {
					final SignatureModel sm = pv.getModel()
							.getSignature();

					final List<StepView> theSteps = new ArrayList<StepView>();
					int publishedResults = 0;
					for (ISelectable sel : items) {
						final StepView sv = (StepView)sel;
						theSteps.add(sv);
						for (ParameterModel pm : sv.getStepModel().getResults())
							if (sm.getResults().contains(pm.getTerm()))
								publishedResults++; 
					}

					if (publishedResults > 0) {
						Callback<AlertResult, Void> call = 
								new Callback<AlertResult, Void>() {
							public Void call(AlertResult result) {
								if (result == AlertResult.YES)
									control.deleteSteps(theSteps);
								return null;
							}
						};

						String message = DragDropManager.getDeletePromptText(
								theSteps.size(), publishedResults);
						Alert.show("Confirm delete", message,
								AlertConfig.YES_NO, call);
					} else
						control.deleteSteps(theSteps);
				}
			}
		});
	}    
   */ 

	//
	//
	// 

	private void openNewExercise() {
		editor.getExerciseOpener().open(new ExerciseModel());
	}    

	
	private void openExercise() throws PALException {

		File file = Utilities.browseForExerciseFile(getScene().getWindow());

		if (file != null && file.exists()) {

			String path = file.getPath(); 
			String name = file.getName();

			path = path.replace("\\", "/"); 
			Exercise exercise = ExerciseStorage.getExerciseFromFile(path);          	
			//exercise.setName(name);

			ExerciseModel em = new ExerciseModel(exercise, file, Configuration.SERVER_PUT_USER, Configuration.SERVER_PUT_PASSWORD);

			editor.getExerciseOpener().open(em);			

		} else

			editor.getExerciseOpener().open(null); 

	}

	private void openURLExercise() {
			
		final ExerciseModel em = new ExerciseModel(); 
		try {
			em.setUrlSource(new URL(Configuration.CLEAR_EXERCISE_URL));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  

		UtilWidgets.simpleURLEditor("Load from Server URL", em, new Runnable() {
			 
			@Override
			public void run() {
						
				Exercise exercise = ExerciseStorage.getExerciseFromURL(em.getUrlSource());        
				
				if (exercise != null) {
				
					ExerciseModel em2 = null;
					try {
						em2 = new ExerciseModel(exercise, em.getUrlSource(), Configuration.SERVER_PUT_USER, Configuration.SERVER_PUT_PASSWORD);
					} catch (PALException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					editor.getExerciseOpener().open(em2);
					
				} else {
					
					UtilWidgets.messagePopup("Error", "Problem with URL: " + em.getUrlSource());					
					//openURLExercise(); 
					
				}
			}
		}); 
	}
	
	//
	//
	//

	private final Button showOriginalButton = getButton("Show / Restore Original", "Show (and possibly restore) the original version of the exercise file (read only, non-editable).",
			"toolbar/restore.png");
	

	/* private final Button restoreOriginalButton = getButton("Restore Original Version", "Restore the original version of the exercise. Use \"Save\" if required to restore original file.",
			"toolbar/folder_yellow_open.png"); */
	
	//
	//
	
	{
		showOriginalButton.setDisable(true);
		//restoreOriginalButton.setDisable(true);

		showOriginalButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {

				ExerciseModel original = ExerciseModel.getActiveModelProperty().getValue().getOriginalExerciseModel(); 
				original.getReadOnlyProperty().setValue(true);
				original.setFileSource(ExerciseModel.getActiveModelProperty().getValue().getFileSource());

				editor.getExerciseOpener().open(original); 
				
			}
		});

		showOriginalButton.disableProperty().set(true);
		//restoreOriginalButton.disableProperty().set(true);
	}
	
	//
	//
	//
	
	/* 
	
	{
		showOriginalButton.setDisable(true);
		restoreOriginalButton.setDisable(true);
		
		restoreOriginalButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {

				ExerciseModel original = ExerciseModel.getActiveModelProperty().getValue().getOriginalExerciseModel(); 
				original.getReadOnlyProperty().setValue(false);
				//original.getHasFileSourceProperty().set(true);
				original.setFileSource(ExerciseModel.getActiveModelProperty().getValue().getFileSource());

				editor.getExerciseOpener().open(original); 
				
			}
		});

		showOriginalButton.disableProperty().set(true);
		restoreOriginalButton.disableProperty().set(true);
	}

   */
	
	private final Button finalizeButton = getButton("Finalize File",
			//control == null || control.getModel() == null ? "Save" : control.getModel() instanceof ExerciseModel ? "Save the current procedure." : "Save the current exercise.",
			"Finalize Exercise.",
			"toolbar/finalize.png");
	{
		finalizeButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				
				InfoPanel.makeVisible(ExerciseModel.getActiveModelProperty().getValue()); 					
							
			}
		}
		); 

		finalizeButton.disableProperty().set(true);
	}

	
}
