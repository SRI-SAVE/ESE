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

package com.sri.tasklearning.ui.core.control.constant;

import java.util.List;

import com.sri.tasklearning.ui.core.SelectionEventCallback;
import com.sri.tasklearning.ui.core.Utilities;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

public class IntegerRangeEditorMenuItem extends CustomMenuItem {

	private int min; 
	private int max;
	private String type = null;  
	private List<String> labels = null; 
		
	public IntegerRangeEditorMenuItem(final int min1, final int max1, List<String> labels, String type, SelectionEventCallback<Pair<Integer, Integer>> selectionEventCallback) {
		
		super(); 		
		
		this.min = min1; 
		this.max = max1; 
		this.labels = labels; 
		this.type = type;  
		
		GridPane grid = new GridPane();				
		this.getStyleClass().add("integer-range-editor-menu-item");
		
		Image minusIcon = Utilities.getImage("edit-remove-3.png");
		Image plusIcon = Utilities.getImage("edit-add-2.png");
		
		final Label downMin = new Label();
		final Label   upMin = new Label();
		
		downMin.setGraphic(new ImageView(minusIcon)); 
		upMin.setGraphic(new ImageView(plusIcon));
		
		final Label downMax = new Label();
		final Label   upMax = new Label();
	
		downMax.setGraphic(new ImageView(minusIcon)); 
		upMax.setGraphic(new ImageView(plusIcon));
	
		final Label minL = new Label();		
		final Label maxL = new Label();
		final Label curL = new Label();
			
		EventHandler<? super MouseEvent> highlight = new EventHandler() {
			@Override
			public void handle(Event arg0) {				
				((Label) arg0.getSource()).getStyleClass().add("highlighted-menu-item"); 				
				arg0.consume();
			}			
		}; 
		
		EventHandler<? super MouseEvent> unhighlight = new EventHandler() {
			@Override
			public void handle(Event arg0) {
				((Label) arg0.getSource()).getStyleClass().remove("highlighted-menu-item"); 				
				arg0.consume();
			}					
		}; 
		

		EventHandler<? super MouseEvent> click = new EventHandler() {
			@Override
			public void handle(Event arg0) {
				Label source = (Label) arg0.getSource();
				
				int oldMin = min;
				int oldMax = max; 
				 
				if (source == downMin ) { 
					if (labels == null || min > 0 ) 
						min--;			
				} else if (source == upMin ) { 
					if (min < max && ( labels == null || min < labels.size()-1))
						min++; 
				} else if (source == downMax ) { 
					if (max > min && ( labels == null || max > 0 ))
						max--;					
				} else if (source == upMax && (labels == null || max < labels.size() -1)) 
					max++; 
				
				if (labels != null ) {
					minL.setText(labels.get(min)); 
					maxL.setText(labels.get(max));  
				} else {				
					minL.setText( Integer.toString(min) ); 
					maxL.setText( Integer.toString(max) );
				}
								
				if (oldMin != min || oldMax != max ) {
					Pair<Integer, Integer> interval = null;
					
					interval = new Pair(min, max);
					selectionEventCallback.invoke(interval);
					
				}
					
				arg0.consume();
			}					
		}; 	
		
		downMin.setOnMouseClicked(click);
		downMax.setOnMouseClicked(click);
		upMin.setOnMouseClicked(click);
		upMax.setOnMouseClicked(click);
		
		downMin.setOnMouseEntered(highlight);
		downMax.setOnMouseEntered(highlight);
		upMin.setOnMouseEntered(highlight);
		upMax.setOnMouseEntered(highlight);
	
		downMin.setOnMouseExited(unhighlight);
		downMax.setOnMouseExited(unhighlight);
		upMin.setOnMouseExited(unhighlight);
		upMax.setOnMouseExited(unhighlight);		
		
		grid.setVgap(4);
		grid.setHgap(4);
		
		Label between =  new Label("between");		
		Label and =  new Label("and");		
		
		GridPane.setHalignment(between, HPos.CENTER);	
		GridPane.setHalignment(and, HPos.CENTER);
		
		GridPane.setHalignment(downMin, HPos.CENTER);		
		GridPane.setHalignment(minL, HPos.CENTER);		
		GridPane.setHalignment(upMin, HPos.CENTER);		

		GridPane.setHalignment(downMax, HPos.CENTER);		
		GridPane.setHalignment(maxL, HPos.CENTER);		
		GridPane.setHalignment(upMax, HPos.CENTER);		
		
		grid.add(between, 0, 1, 1 , 1);
	
		grid.add(downMin, 1, 2, 1 , 1); 
		grid.add(minL,    1, 1, 1 , 1);
		grid.add(upMin,   1, 0, 1 , 1);
		
		grid.add(downMax, 4, 2, 1 , 1); 
		grid.add(maxL,    4, 1, 1 , 1);
		grid.add(upMax,   4, 0, 1 , 1);
		
		grid.add(and, 3, 1, 1 , 1); 
		
		this.setContent(grid); 		
		
		this.min = min1;
		this.max = max1;
		
		if (labels != null ) {
			minL.setText(labels.get(min)); 
			maxL.setText(labels.get(max)); 
		} else {
			minL.setText(Integer.toString(min)); 		
			maxL.setText(Integer.toString(max));
		}
				
	}

	public String getType() {
		return type;
	}


	
	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public List<String> getLabels() {
		return labels;
	}

}
