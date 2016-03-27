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

package com.sri.tasklearning.ui.core.step;

import java.util.Comparator;
import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;

import com.sri.tasklearning.ui.core.ISelectable;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;

/**
 * Abstract base class for step views. Provides common properties and methods
 * common to all step views. This class does not contain the common visual 
 * elements for steps in a procedure view. Those are defined in 
 * {@link StepViewBasicUI}.
 */
public abstract class StepView extends Pane implements ISelectable {
	
    public static final double DEF_WIDTH = 700.0;
    public static final double MIN_WIDTH = 700.0;
    public static final double EDGE_ROUNDING = 6.0;
    public static final double DEFAULT_BORDER_WIDTH = 1.0;
    public static final double TITLE_VAR_SCALAR = 1.25;
    public static final double LHS_OFFSET = 55.0;
    public static final double PAD = 7.0;
    public static final double RHS_PAD = 8 * PAD;
    public static final double ICON_SIZE = 32.0;
    public static final double SMALL_ICON_SIZE = 28.0;
    
    // A comparator that will compare step views based on their
    // underlying model's step index
    public static final Comparator<StepView> INDEX_COMPARATOR = new Comparator<StepView>() {
        public int compare(StepView a, StepView b) {
            int a_idx = a.getStepModel().getIndex();
            int b_idx = b.getStepModel().getIndex();
            if (a_idx < b_idx)
                return -1;
            else if (a_idx > b_idx)
                return 1;              
            else 
                return 0;
        }
    };
    
    protected StepModel stepModel;
    protected IStepViewContainer parentView; 
    protected CommonView procedureView;
    
    // State properties
    protected SimpleBooleanProperty expanded = new SimpleBooleanProperty(false);
    protected SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
    protected SimpleBooleanProperty stepIndexVisibilityProperty = new SimpleBooleanProperty(false); 
    
    
    public StepView(
            final StepModel argModel, 
            final IStepViewContainer argParent, 
            final CommonView argProcView) {
        this.stepModel = argModel;  
        this.parentView = argParent;
        this.procedureView = argProcView;        
    }

    public StepModel getStepModel() {
        return stepModel; 
    }
    
    public void setSelected(boolean argSelected) {
        selected.setValue(argSelected);
    }
    
    public boolean isSelected() {
        return selected.getValue();    
    }
    
    public SimpleBooleanProperty selectedProperty() {
        return selected; 
    }   
  
    
    public IStepViewContainer getStepViewContainer() {
        if (parentView != null)
            return parentView;
        
        return procedureView; 
    }
     
    public void setStepViewContainer(IStepViewContainer parentView) {
        this.parentView = parentView;
    }
    
    public CommonView getView() {
        return procedureView;
    }
    
    public void setView(CommonView procedureView) {
        this.procedureView = procedureView;
    }
    
    // Intended to be overwritten
    public void updateIssueVisualization() {
        
    }

    /**
     * Returns true if the point is w/in the bounds of the step. Some steps may
     * override this function, if they have a complex shape.
     */
    public boolean isPointInContent(double x, double y) {
        return getLayoutBounds().contains(x, y);
    }

    /**
     * Returns true if the box intersects the bounds of the step. Some steps may
     * override this function, if they have a complex shape.
     */
    public boolean intersectsContent(Bounds box) {
        return getLayoutBounds().intersects(box);
    }
    
    /**
     * Indicates how deeply nested the visual for this step is within the 
     * ProcedureView. For instance, if this step is located within a loop
     * within a loop, the method will return two. A nesting level of 0 
     * indicates that the step is at the top-level of the procedure. This
     * logic is in the View rather than the model since the views could
     * conceivably abstract the structure of the models.  
     * 
     * @return - the number of visual nesting levels for this step
     */
    public int getVisualNestingLevel() {
        int level = 0; 
        StepView sv = this;
        while (sv.getStepViewContainer() instanceof LoopView) {
            level++;
            sv = (LoopView)sv.getStepViewContainer();
        }
        return level; 
    }
    
    public boolean isOnScreen() {
        Bounds bounds = localToScene(parentToLocal(this.getBoundsInParent()));
        Rectangle2D r2d2 = new Rectangle2D(bounds.getMinX(), bounds.getMinY(),
                bounds.getWidth(), bounds.getHeight());

        List<Screen> screens = Screen.getScreensForRectangle(r2d2);
        return screens != null && screens.size() > 0;
    }
     
     /**
      * Intended to be overwritten by some subclasses
      *
      * @return The height of the step for purposes of drag/drop calculations
      */
     public double getCoreHeight() {
         return getHeight();     
     }
     
     public SimpleBooleanProperty getStepIndexVisibility() {
 		
 		return stepIndexVisibilityProperty;
 		
 	}          
   
     public List<StepView> getSteps() {    	
    	 return null;     	 
     }

     @Override
     public int getIndex() {
         return getStepModel().getIndex();
     }
   
}
