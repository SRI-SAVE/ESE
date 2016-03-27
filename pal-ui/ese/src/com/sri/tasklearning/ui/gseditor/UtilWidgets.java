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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.sri.tasklearning.ui.core.exercise.ExerciseModel;

public class UtilWidgets {

	static public void messagePopup(String title, String text) {

		final Label message = new Label(text);

		message.setWrapText(true);

		final Stage dialog = new Stage();
		dialog.initModality(Modality.APPLICATION_MODAL); 					
		dialog.initOwner(GsEditor.getInstance().getStage());
		dialog.setTitle(title);

		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(10));

		HBox buttons = new HBox(30); 					

		Button okButton = new Button("OK");

		okButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {			
				dialog.close();
			} 
		}); 

		buttons.getChildren().add(okButton);

		pane.setCenter(message);
		pane.setBottom(buttons); 

		Scene dialogScene = new Scene(pane, 300, 100);
		dialog.setScene(dialogScene);
		dialog.show();			

	}

	static public void simpleURLEditor(String title, final ExerciseModel model, final Runnable onSuccess) {

		String url = URLDecoder.decode(model.getUrlSource().toString()); 
	
		final TextField editor = new TextField(); 
		editor.setText(url);

		final Stage dialog = new Stage();
		dialog.initModality(Modality.APPLICATION_MODAL); 					
		dialog.initOwner(GsEditor.getInstance().getStage());
		dialog.setTitle(title);
		
		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(10));
		
		HBox buttons = new HBox(30); 					
		
		Button okButton = new Button("OK");
		Button localhostButton = new Button("Use localhost:3001");
		Button cancelButton = new Button("Cancel"); 
		
		okButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				
				try {
					
					String url = editor.getText(); 
					
					if (! url.endsWith(".xml")) {
						url += ".xml"; 
					}
					
					URL url2 = new URL(url); 								
					url = new URI(url2.getProtocol(), url2.getUserInfo(), url2.getHost(), url2.getPort(), url2.getPath(), url2.getQuery(), url2.getRef()).toString();									
					
					model.setUrlSource(new URL(url));
					
					GsEditor.log.info("HTTP PUT URL is " + model.getUrlSource());

					onSuccess.run();					
					
					dialog.close();
					
				} catch (MalformedURLException | URISyntaxException e) {
					
					UtilWidgets.messagePopup("Error", "Error: " +e); 
					
				} 
			}
		}); 
		
		cancelButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {											
				dialog.close();												        
			}
		}); 	
		
		localhostButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				String url = editor.getText();														
				url = url.replace(Configuration.SERVER_URL, Configuration.LOCALHOST_URL);
				editor.setText(url);
			}
		}); 
		
		buttons.getChildren().add(okButton);
		buttons.getChildren().add(localhostButton);
		buttons.getChildren().add(cancelButton);
		buttons.setAlignment(Pos.CENTER);
		
		pane.setCenter(editor);
		pane.setBottom(buttons); 
		
		Scene dialogScene = new Scene(pane, 500, 100);
		dialog.setScene(dialogScene);
		dialog.show();	
	}

	
}	
