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

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.layout.StepLayout;
import com.sri.tasklearning.ui.core.layout.TextFlowLayout;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.step.dialog.DialogManager;
import com.sri.tasklearning.ui.core.step.dialog.DialogStepView;
import com.sri.tasklearning.ui.core.term.ListModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;
import com.sri.tasklearning.ui.core.term.function.ZipModel;

/**
 * View for {@link LoopModel}. Consists of a loop header which appears much like
 * a normal action step, a {@code StepLayout} to visualize the loop's child 
 * steps and a footer.  
 */
public class LoopView extends StepViewBasicUI implements IStepViewContainer {
    
    private LoopModel loopModel; 
    private StepLayout stepLayout;
    private Pane loopFooter;
    private TextFlowLayout footerText;
    private TextFlowLayout accumText;
    private TextFlowLayout titleText;
    private Group subStepBackground;
    private Line leftBorder;
    
    public LoopView(
            final LoopModel argModel, 
            final IStepViewContainer argParent, 
            final CommonView argProcView) {

        super(argModel, argParent, argProcView);
        

        loopModel = argModel;
        stepLayout = new StepLayout(loopModel, this, procedureView);
        
        prefHeightProperty().unbind();
        setPrefHeight(Region.USE_COMPUTED_SIZE);
           
        getChildren().addAll(createSubStepBackground(), 
                             selectionRect, 
                             createLoopFooter(),
                             createFooterIcon(),
                             titlePane,
                             stepBackground,
                             leftBorder, 
                             stepLayout,
                             expansionArea,
                             stepIndexLabel, 
                             borderRect,
                             createContentArea());
        
        // This is so the body of the loop is not displayed while a loop step is 
        // undergoing drag/drop.
        stepLayout.visibleProperty().bind(this.opacityProperty().isEqualTo(1.0, 0.0));
        
        if (loopModel.getLoopList() instanceof FunctionModel)
            showConfigButton.setValue(false);
        else
            showConfigButton.setValue(true);
        
        onConfigRequested = new Runnable() {
            public void run() {
                DialogStepView dsv = DialogManager.showEditDialog(LoopView.this);
                procedureView.getScrollPane().scrollIntoView(dsv);
            }
        };
    }
    
    @Override
    protected void layoutChildren() {
        stepLayout.setMinWidth(getWidth() - LHS_OFFSET);
        
        super.layoutChildren();       
        
        stepLayout.setLayoutY(contentHeight.getValue());        
        stepLayout.setLayoutX(LHS_OFFSET);
        
        subStepBackground.setLayoutY(contentHeight.getValue());
        leftBorder.setLayoutY(subStepBackground.getLayoutY() - 5);
        
        loopFooter.setLayoutY(computePrefHeight(0) - loopFooter.prefHeight(0)); 
    }
                
    @Override
    protected double computeMinWidth(double height) {
        return computePrefWidth(0);
    }
    
    @Override
    public double computePrefWidth(double height) {        
        return Math.max(stepLayout.computePrefWidth(height) + LHS_OFFSET,
                StepView.MIN_WIDTH + LHS_OFFSET);
    }    

    @Override
    public double computeMinHeight(double width) {
        return computePrefHeight(width);
    }
    
    @Override
    public double computePrefHeight(double width) {
        return super.computePrefHeight(width)
                + (stepLayout.isVisible() ? stepLayout.prefHeight(width) : 0)
                + loopFooter.prefHeight(width);
    }
    
    @Override
    public double computeMaxHeight(double width) {
        return computePrefHeight(width);
    }
    
    @Override
    public double getCoreHeight() {
        return Math.min(titlePane.getHeight() * 2, getHeight());
    }
    
    @Override
    public ContainerStepModel getContainerStepModel() {
        return loopModel;
    }
    
    @Override
    public StepView findStepView(StepModel step) {
        return stepLayout.findStepView(step);
    }
    
    @Override
    public void updateIssuesVisualization() {
        for (StepView view : stepLayout.getStepViews()) {
            view.updateIssueVisualization();
            if (view instanceof IStepViewContainer)
                ((IStepViewContainer)view).updateIssuesVisualization();
        }
    }
    
    @Override
    protected Region createContentArea() {
        titleText = new TextFlowLayout(this);
        titleText.setFont(Fonts.STANDARD);
        titleText.setTextColor(Colors.DisabledText);
        titleText.setAlignment(Pos.CENTER_LEFT);  
        titleText.readOnlyProperty().bind(procedureView.readOnlyProperty());
        
        List<Object> list = new ArrayList<Object>(); 
        if (loopModel.getLoopTerm() instanceof ListModel) {
            list.add("In the following steps, let's refer to ");
            int idx = 0;
            for (ParameterModel pm : ((ZipModel)loopModel.getLoopList()).getInputs()) {
                ParameterModel iterand = ((ListModel)loopModel.getLoopTerm()).getInputs().get(idx);   
                String itemName = TypeUtilities.getName(iterand.getTerm().getTypeDef());
                if (itemName == null)
                    itemName = "item";
                list.add("the " + itemName + " from ");
                list.add(pm);
                list.add("as");

                list.add(iterand);
                list.add("and");
                idx++;
            }
            list.remove(list.size() -1);
        } else { 
            String itemName = TypeUtilities.getName(loopModel.getLoopTerm()
                    .getTypeDef());
            if (itemName == null)
                itemName = "item";
            list.add("In the following steps, let's refer to each "
                    + itemName
                    + " as:");
            list.add(loopModel.getIterand());
        }
        titleText.setContents(list);
        
        Pane pane = new Pane() {
            @Override
            protected void layoutChildren() {
                super.layoutChildren(); 
                
                titleText.setLayoutX(LHS_OFFSET);
                titleText.setLayoutY(PAD * 1.5);
                                
                // Not sure why I need to do this after build 23
                stepLayout.requestLayout();
            }
            
            @Override
            protected double computePrefHeight(double width) {
                return titleText.minHeight(width) + PAD * 3;
            }            
            
            @Override
            protected double computePrefWidth(double height) {
                return borderRect.getWidth() - LHS_OFFSET;
            }                  
        };
        
        titleText.prefWrapLengthProperty().bind(borderRect.widthProperty().subtract(LHS_OFFSET + PAD));   
              
        pane.getChildren().add(titleText);
        contentArea.setValue(pane);   
        return pane;   
    }
    
    private Group createSubStepBackground() {
        subStepBackground = new Group();
        
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(this.widthProperty());
        bg.heightProperty().bind(this.heightProperty().subtract(contentHeight));           
        
        LinearGradient lg = new LinearGradient(0, 0, 1, 0, true,
                CycleMethod.NO_CYCLE, new Stop[] {
                        new Stop(0.0, (Color) stepLightBackground.getValue()),
                        new Stop(1.0, (Color) Colors.WindowBackground) });
        bg.setFill(lg);
        
        leftBorder = new Line();
        leftBorder.startYProperty().bind(bg.layoutYProperty());
        leftBorder.endYProperty().bind(bg.layoutYProperty().add(bg.heightProperty()));
        leftBorder.startXProperty().bind(bg.layoutXProperty());
        leftBorder.endXProperty().bind(bg.layoutXProperty());
        leftBorder.setStroke(borderColor.getValue());
        leftBorder.strokeProperty().bind(borderColor);
        leftBorder.strokeWidthProperty().bind(borderRect.strokeWidthProperty());
        
        subStepBackground.getChildren().addAll(bg);
        
        return subStepBackground;
    }
    
    private void refreshAccumulatorDisplay(boolean visible) {
        if (visible && accumText.getChildren().size() == 0) {
            List<Object> objs = new ArrayList<Object>();

            objs.add("collect the values of ");
            objs.add(loopModel.getCollect());
            objs.add(" into ");
            objs.add(loopModel.getInto());
            accumText.setContents(objs);
        }        
        accumText.setVisible(visible);
        if (loopFooter != null)
            loopFooter.requestLayout();
    }
    
    private Pane createLoopFooter() {
        footerText = new TextFlowLayout(this, 5, 5, false);
        footerText.setFont(Fonts.LARGE); 
        footerText.readOnlyProperty().bind(procedureView.readOnlyProperty());
        
        String itemsName = TypeUtilities.getPluralName(loopModel.getLoopTerm()
                .getTypeDef());
        if (itemsName == null)
            itemsName = "items";
        
        List<Object> objs = new ArrayList<Object>();
        objs.add("continue repeating while");
        objs.add(loopModel.getInputCollection());
        objs.add("still ");
        if (loopModel.getLoopTerm() instanceof ListModel)
            objs.add("have ");
        else
            objs.add("has ");
        objs.add(itemsName);   
        footerText.setContents(objs);        
        
        accumText = new TextFlowLayout(5, 5);
        accumText.readOnlyProperty().bind(procedureView.readOnlyProperty()); 
        accumText.setTextColor(Colors.DisabledText);
        accumText.setFont(Fonts.STANDARD);        
        
        refreshAccumulatorDisplay(loopModel.getIntoTerm() != null);
        
        loopModel.getInto().termProperty()
                .addListener(new ChangeListener<TermModel>() {
                    public void changed(
                            final ObservableValue<? extends TermModel> value,
                            final TermModel oldVal, final TermModel newVal) {
                        refreshAccumulatorDisplay(newVal != null);
                    }
                });
        
        final Rectangle bg = new Rectangle();
        
        loopFooter = new Pane() {
            @Override
            protected void layoutChildren() {
                super.layoutChildren(); 

                accumText.setLayoutX(LHS_OFFSET);
                accumText.setLayoutY(EDGE_ROUNDING);
                
                footerText.relocate(LHS_OFFSET,
                        EDGE_ROUNDING
                        + (accumText.isVisible() ? accumText
                                .getLayoutBounds().getHeight() + EDGE_ROUNDING
                                : 0));
            }
            
            @Override
            protected double computePrefHeight(double width) {
                double height = footerText.prefHeight(width) + 2 * EDGE_ROUNDING;
                
                if (accumText.isVisible())
                    height += accumText.prefHeight(width) + EDGE_ROUNDING;
                
                return height;
            }            
            
            @Override
            protected double computePrefWidth(double height) {
                return borderRect.getWidth();
            }                  
        };
        
        loopFooter.prefWidthProperty().bind(widthProperty());
        loopFooter.setMaxHeight(Region.USE_PREF_SIZE);
        
        footerText.prefWrapLengthProperty().bind(
                loopFooter.widthProperty().subtract(DEFAULT_BORDER_WIDTH + RHS_PAD));

        accumText.prefWrapLengthProperty().bind(
                loopFooter.widthProperty().subtract(RHS_PAD + DEFAULT_BORDER_WIDTH));

        bg.widthProperty().bind(loopFooter.widthProperty());
        bg.heightProperty().bind(loopFooter.heightProperty());
        bg.fillProperty().bind(stepLightBackground);
        bg.setStroke(borderColor.getValue());
        bg.strokeProperty().bind(borderColor);
        bg.strokeWidthProperty().bind(borderRect.strokeWidthProperty());
        bg.setArcHeight(2 * EDGE_ROUNDING);
        bg.setArcWidth(2 * EDGE_ROUNDING);        
        
        loopFooter.getChildren().addAll(bg, footerText, accumText);
        
        return loopFooter;
    }
    
    private ImageView createFooterIcon() {
        ImageView footerIcon = Utilities.getImageView("loop-icon2.png");
        footerIcon.layoutYProperty().bind(loopFooter.layoutYProperty().subtract(footerIcon.getImage().getHeight() / 2));
        footerIcon.layoutXProperty().bind(loopFooter.layoutXProperty().add((LHS_OFFSET / 2) - (footerIcon.getImage().getWidth() / 2)));
        return footerIcon;
    }
    
    public void unselectDescendents() {
        for (Node node : stepLayout.getChildren()) {
            StepView sv = (StepView)node;
            if (sv.isSelected())
                procedureView.getSelectionManager().setSelection(sv, false);
            if (sv instanceof LoopView)
                ((LoopView) sv).unselectDescendents();
        }
    }
    
    public List<StepView> getStepViews() {
        List<StepView> subStepViews = new ArrayList<StepView>();
        for (Node node : stepLayout.getChildren()) 
            subStepViews.add((StepView)node);        
        return subStepViews;
    }
    
    public StepLayout getStepLayout() {
        return this.stepLayout;
    }

    public int indexOf(StepView child) {
        return stepLayout.getChildren().indexOf(child);
    }
    
    /** 
     * Container steps, unlike regular steps, have a central "content" portion that should not be
     * selectable. This allows the contained steps to be surrounded by "white space", like any other
     * step would be. Thus, we must override the isPointInContent, and have it only allow selection
     * if the user clicks on the header, footer, or within a couple pixels of the line on the LHS.
     */
     
    public boolean isPointInContent(double x, double y) {
        return (titlePane.getBoundsInParent().contains(x, y) || 
                stepBackground.getBoundsInParent().contains(x, y) ||
                loopFooter.getBoundsInParent().contains(x, y) ||
                leftBorder.getBoundsInParent().contains(x, y));
    }

    /**
     * Just as we override isPointInContent, we also override intersectsContent for the same reason.
     */
     public boolean intersectsContent(Bounds box) {
         return (titlePane.getBoundsInParent().intersects(box) ||
                 stepBackground.getBoundsInParent().intersects(box) ||
                 loopFooter.getBoundsInParent().intersects(box) ||
                 leftBorder.getBoundsInParent().intersects(box));
     }

}
