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

package com.sri.tasklearning.ui.core.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

public class Arrow extends Pane {

	Line line; 
	Polygon arrow; 
	
	double fx, fy, tx, ty; 

	
	public Arrow(SimpleDoubleProperty fx, SimpleDoubleProperty fy, SimpleDoubleProperty tx, SimpleDoubleProperty ty) {

		super();
		
		line = new Line(fx.getValue(), fy.getValue(), tx.getValue(), ty.getValue()); 
		
		line.startXProperty().bind(fx); 
		line.startYProperty().bind(fy);
		
		line.endXProperty().bind(tx);
		line.endYProperty().bind(ty);
		
		line.setStroke(Color.BLACK);
		line.setStrokeWidth(5);

		Polygon arrow = new Polygon();
		
		arrow.getPoints().addAll(new Double[]{
				0.0, 5.0,
				-5.0, -5.0,
				5.0, -5.0});

		double angle = Math.atan2(ty.getValue() - fy.getValue(), tx.getValue() - fx.getValue()) * 180 / 3.14;
		
		arrow.setRotate((angle - 90));

		arrow.translateXProperty().bind(tx);
		arrow.translateYProperty().bind(ty);
				
		getChildren().addAll(line, arrow);
					
		InvalidationListener listener = new InvalidationListener() {

			@Override
			public void invalidated(Observable arg0) {
				
				double angle = Math.atan2(ty.getValue() - fy.getValue(), tx.getValue() - fx.getValue()) * 180 / 3.14;				
				arrow.setRotate((angle - 90));				
				
			}
			
			
		};
		
		fx.addListener(listener);
		fy.addListener(listener);
		tx.addListener(listener);
		ty.addListener(listener);				
				
	}
	
    @Override
    protected void layoutChildren() {
        
        super.layoutChildren();
                 
    } 
    
    @Override
    public double computePrefWidth(double height) {
      return 5;
    }

    @Override
    protected double computePrefHeight(double width) {
        return Math.abs(ty - fy);
    }
    
}
