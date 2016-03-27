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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.DragDropManager;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.ISelectable;
import com.sri.tasklearning.ui.core.SelectionManager;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.Knurling;
import com.sri.tasklearning.ui.core.control.ToolTippedImageView;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.layout.TextFlowLayout;
import com.sri.tasklearning.ui.core.step.dialog.DialogManager;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ParameterView;
import com.sri.tasklearning.ui.core.term.TermView;

/**
 * Abstract class that defines the common visual elements of steps as visualized
 * in a {@code ProcedureView}. This includes rounded rectangle that represents
 * the step, a step header, a step number lable, a collapsible information panel
 * and a content panel which is defined/populated by subclasses
 */
public abstract class StepViewBasicUI extends StepView {
	
    protected static final double EXP_COL_1_WIDTH = 80;
    protected final SimpleObjectProperty<Paint> borderColor = new SimpleObjectProperty<Paint>(Colors.SystemDarkGray);
    protected final SimpleObjectProperty<Paint> stepBackgroundColor 
        = new SimpleObjectProperty<Paint>(Colors.StepBackground);
    protected final SimpleObjectProperty<Paint> stepLightBackground 
        = new SimpleObjectProperty<Paint>(Colors.SystemLightGray);
    protected final Image toggleImageUntoggled = Utilities.getImage("info-icon1.png");
    protected Image toggleImageToggled = Utilities.getImage("info-icon2.png");
    protected final SimpleObjectProperty<Image> toggleImage = new SimpleObjectProperty<Image>(toggleImageUntoggled);
    protected final SimpleDoubleProperty contentHeight = new SimpleDoubleProperty();
    protected final SimpleBooleanProperty showConfigButton = new SimpleBooleanProperty(false);
    protected Runnable onConfigRequested;

    protected List<ParameterView> params = new ArrayList<ParameterView>();
    protected Pane titlePane;
    protected final GridPane expansionArea = new GridPane();
    protected Group stepBackground;
    protected Rectangle borderRect;
    protected Rectangle selectionRect;
    protected Label stepIndexLabel;
    protected SimpleObjectProperty<Region> contentArea = new SimpleObjectProperty<Region>();
    protected ToolTippedImageView appIcon;
    protected TextFlowLayout headerText;

    protected StepViewBasicUI(final StepModel argModel, final IStepViewContainer argParent,
            final CommonView argProcView) {

        super(argModel, argParent, argProcView);

        stepModel.highlightedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(final ObservableValue<? extends Boolean> value, final Boolean oldVal,
                    final Boolean newVal) {
                if (newVal) {
                    borderRect.setStrokeWidth(4);
                    borderColor.set(Colors.SelectedVariableBackground);
                } else {
                    borderRect.setStrokeWidth(1);
                    borderColor.set(Colors.SystemDarkGray);
                }
            }
        });

        createSelectionRect();
        createBorderRect();
        createTitlePane();
        createStepBackground();
        createExpansionArea();
        createStepIndexLabel();

        procedureView.readOnlyProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(final ObservableValue<? extends Boolean> value, final Boolean oldVal,
                    final Boolean newVal) {
                headerText.setReadOnly(newVal);
                for (ParameterView pv : params)
                    pv.setReadOnlyRequest(newVal);
            }
        });

        selected.addListener(new ChangeListener<Boolean>() {
            public void changed(final ObservableValue<? extends Boolean> value, final Boolean oldVal,
                    final Boolean newVal) {
                if (BackendFacade.getInstance().debuggingProcedureProperty().getValue()) {
                    headerText.setReadOnly(!newVal);
                    for (ParameterView pv : params)
                        pv.setReadOnlyRequest(newVal);
                }
            }
        });

        prefHeightProperty().bind(contentHeight);
        recalcHeight();

        registerEventHandlers();
    }

    public TermView getTermView(List<String> accessors) {
        // First accessor is the arg name
        String arg = accessors.get(0);
        TermView view = null;
        List<ParameterView> allParams = new ArrayList<ParameterView>();
        allParams.addAll(params);
        for (Node n : headerText.getChildren())
            if (n instanceof ParameterView)
                allParams.add((ParameterView) n);

        for (ParameterView pv : allParams) {
            if (pv.getParameterModel().getName().equals(arg)) {
                view = pv.getTermView();
                break;
            }
        }

        return view;
    }

    public final void addChildren() {
        getChildren().addAll(selectionRect, titlePane, stepBackground, expansionArea, borderRect);
        if (!(getStepViewContainer() instanceof IdiomStepView)
                && (!(this instanceof ActionStepView) || ((ActionStepModel) this.getStepModel()).getActionStreamEvent() == null))
            getChildren().add(stepIndexLabel);
    }

    @Override
    public double computePrefWidth(double height) {
        if (parentView != null)
            return MIN_WIDTH;
        else
            return DEF_WIDTH;
    }

    @Override
    protected double computePrefHeight(double width) {
        return contentHeight.getValue();
    }

    public void recalcHeight() {
        double height = titlePane.prefHeight(getWidth());

        if (expanded.getValue())
            height += expansionArea.prefHeight(getWidth());

        if (contentArea.getValue() != null) {
            height += contentArea.getValue().prefHeight(getWidth());
        }

        contentHeight.setValue(height);
    }

    @Override
    protected void layoutChildren() {
        recalcHeight();

        super.layoutChildren();

        if (contentArea.getValue() != null) {
            Region content = contentArea.getValue();
            content.setLayoutY(titlePane.getHeight() + ((expanded.getValue() ? expansionArea.getHeight() : 0)));
            content.setLayoutX(borderRect.getLayoutX());
        }

        stepIndexLabel.setLayoutX(borderRect.getLayoutX() - stepIndexLabel.getWidth() - PAD * 1.5);
        stepIndexLabel.setLayoutY(titlePane.getHeight() / 2 - 10);

        titlePane.setLayoutX(borderRect.getLayoutX() + DEFAULT_BORDER_WIDTH);
        titlePane.setLayoutY(borderRect.getLayoutY() + DEFAULT_BORDER_WIDTH);

        selectionRect.setWidth(getWidth() + (PAD * 2 - 2));
        selectionRect.setHeight(getHeight() + (PAD * 2 - 2));
    }

    protected Rectangle createSelectionRect() {
        selectionRect = new Rectangle();
        selectionRect.relocate(-(PAD - 1), -(PAD - 1));
        selectionRect.setArcHeight(EDGE_ROUNDING * 4);
        selectionRect.setArcWidth(EDGE_ROUNDING * 4);
        selectionRect.visibleProperty().bind(selected);
        selectionRect.setOpacity(0.8);
        selectionRect.setStroke(new LinearGradient(0.0, 0.0, 1.0, 0.0, true, null, new Stop[] {
                new Stop(0.25, Colors.SelectedStepBorder), new Stop(1.0, Colors.SelectedStepBackground) }));
        selectionRect.setFill(new LinearGradient(0.0, 0.0, 1.0, 0.0, true, null, new Stop[] {
                new Stop(0.1, Colors.SelectedStepBackground), new Stop(1.0, Colors.SelectedStepLightBackground) }));

        return selectionRect;
    }

    protected Rectangle createBorderRect() {
        borderRect = new Rectangle();
        borderRect.widthProperty().bind(widthProperty());
        borderRect.heightProperty().bind(contentHeight);
        borderRect.setFill(null);
        borderRect.setStrokeWidth(DEFAULT_BORDER_WIDTH);
        borderRect.strokeProperty().bind(borderColor);
        borderRect.setArcHeight(EDGE_ROUNDING * 2);
        borderRect.setArcWidth(EDGE_ROUNDING * 2);

        return borderRect;
    }

    protected Label createStepIndexLabel() {
    	    	
        stepIndexLabel = new Label();        
        stepIndexLabel.textProperty().bind(getStepModel().indexProperty().add(1).asString());
        

        stepIndexLabel.setFont(Fonts.STEP_NUMBERING);
        stepIndexLabel.setTextFill(Colors.SystemGray);

        this.selected.addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> val, Boolean oldVal, Boolean newVal) {
                stepIndexLabel.setTextFill(newVal ? Colors.SelectedStepBorder : Colors.SystemGray);           
            }
        });
        	
        // Don't show label while a step is being dragged around the Scene
        stepIndexLabel.visibleProperty().bind(this.opacityProperty().isEqualTo(1).and(this.getStepViewContainer().getStepIndexVisibility())); 

        return stepIndexLabel;
    }

    private static final double COLUMN_1_WIDTH = 60;

    protected Region createContentArea() {
        GridPane grid = new GridPane();
        grid.setVgap(PAD * 1.5);
        grid.setHgap(PAD);
        grid.prefWidthProperty().bind(widthProperty().subtract(DEFAULT_BORDER_WIDTH * 2));
        grid.setPadding(new Insets(PAD, 0, PAD, 0));

        List<String> hiddenParams = new ArrayList<String>();
        if (stepModel instanceof ActionStepModel) {
            String csv = ((ActionStepModel) stepModel).getActionDefinition().getMetadata("hide");

            if (csv != null && csv.contains(","))
                for (String p : csv.split(","))
                    hiddenParams.add(p);
        }

        boolean unimportantResults = false;
        boolean importantResults = false;
        for (ParameterModel res : stepModel.getResults())
            if (!res.isImportant() && !hiddenParams.contains(res.getName()))
                unimportantResults = true;
            else
                importantResults = true;

        if (unimportantResults) {
            Label resultsLabel = new Label(importantResults ? ("other results:") : ("results:"));
            resultsLabel.setFont(Fonts.STANDARD_TINY);
            resultsLabel.setTextFill(Colors.DisabledText);
            GridPane.setConstraints(resultsLabel, 0, 0, 1, 1, HPos.RIGHT, VPos.TOP, Priority.NEVER, Priority.NEVER,
                    new Insets(5, 0, 0, 0));

            VBox results = new VBox(PAD);
            results.setAlignment(Pos.CENTER_LEFT);

            for (ParameterModel result : stepModel.getResults()) {
                if (result.isImportant())
                    continue;
                ParameterView pv = new ParameterView(result, this, procedureView, false, false, null);
                results.getChildren().add(pv);
                params.add(pv);
            }
            GridPane.setConstraints(results, 1, 0);

            grid.getChildren().addAll(resultsLabel, results);

            grid.getColumnConstraints().add(new ColumnConstraints(COLUMN_1_WIDTH));
            grid.layout();
            contentArea.setValue(grid);
            return grid;
        }

        return null;
    }

    protected final ToolTippedImageView issueIcon = new ToolTippedImageView(Utilities.getImage("curl-warning.png"));
    {
        issueIcon.setVisible(false);
        issueIcon.mouseTransparentProperty().bind(procedureView.readOnlyProperty().or(disabledProperty()));
    }

    protected static final double TITLE_PANE_LHS = 40.0;
    protected static final double TITLE_PANE_BUTTON_SIZE = 18.0;
    
    protected Pane createTitlePane() {
    	
        headerText = new TextFlowLayout(this);
        headerText.setPadding(new Insets(1.5 * PAD, 0, 1.5 * PAD, 0));

        final Knurling knurling = new Knurling("Click-and-drag to move steps.");
        final Line bottomBorder = new Line();
        final ToolTippedImageView infoToggle = new ToolTippedImageView();
        infoToggle.setFitHeight(TITLE_PANE_BUTTON_SIZE);
        infoToggle.setFitWidth(TITLE_PANE_BUTTON_SIZE);

        infoToggle.setToolTipCallback(new IToolTipCallback() {
            public String getToolTipText() {
                return "Click to show step information.";
            }
        });
        final ToolTippedImageView config = new ToolTippedImageView();
        config.setToolTipCallback(new IToolTipCallback() {
            public String getToolTipText() {
                return "Click to configure the properties of this step.";
            }
        });
        config.setFitHeight(TITLE_PANE_BUTTON_SIZE);
        config.setFitWidth(TITLE_PANE_BUTTON_SIZE);

        appIcon = new ToolTippedImageView(Utilities.getImage(stepModel.getIconPath()));
        issueIcon.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                DialogManager.showErrorDialog(StepViewBasicUI.this, stepModel.getIssues().get(0));
            }
        });

        titlePane = new Pane() {
            @Override
            protected void layoutChildren() {
                super.layoutChildren(); // Resizes children to preferred sizes

                appIcon.setLayoutX(TITLE_PANE_LHS / 2 - SMALL_ICON_SIZE / 2);
                appIcon.setLayoutY(getHeight() / 2 - SMALL_ICON_SIZE / 2);

                headerText.setLayoutX(TITLE_PANE_LHS);

                infoToggle.setLayoutX(getWidth() - 2 * PAD - TITLE_PANE_BUTTON_SIZE - Knurling.WIDTH);
                infoToggle.setLayoutY(getHeight() / 2 - (TITLE_PANE_BUTTON_SIZE / 2));

                config.setLayoutX(infoToggle.getLayoutX() - PAD - TITLE_PANE_BUTTON_SIZE);
                config.setLayoutY(infoToggle.getLayoutY());

                knurling.setLayoutX(Math.floor(getWidth() - PAD - Knurling.WIDTH));
                knurling.setLayoutY(Math.floor(getHeight() / 2) - (Knurling.HEIGHT / 2));

                issueIcon.relocate(titlePane.getWidth() - issueIcon.getFitWidth() + 1, -1);
            }

            @Override
            public double computePrefHeight(double width) {
                return headerText.prefHeight(-1);
            }
        };

        double right = 3 * PAD + TITLE_PANE_BUTTON_SIZE + Knurling.WIDTH;
        right += showConfigButton.getValue() ? PAD + TITLE_PANE_BUTTON_SIZE : 0;

        headerText.prefWrapLengthProperty().bind(
                widthProperty().subtract(2 * DEFAULT_BORDER_WIDTH + TITLE_PANE_LHS + right));
        titlePane.prefWidthProperty().bind(widthProperty().subtract(2 * DEFAULT_BORDER_WIDTH));
        titlePane.setMinWidth(Region.USE_PREF_SIZE);
        titlePane.setMaxWidth(Region.USE_PREF_SIZE);
        titlePane.setMinHeight(Region.USE_PREF_SIZE);
        titlePane.setMaxHeight(Region.USE_PREF_SIZE);

        bottomBorder.visibleProperty().bind(contentArea.isNotNull().or(expanded));
        bottomBorder.setStroke(borderColor.getValue());
        bottomBorder.setStrokeWidth(DEFAULT_BORDER_WIDTH);

        appIcon.setFitWidth(StepView.SMALL_ICON_SIZE);
        appIcon.setFitHeight(StepView.SMALL_ICON_SIZE);
        appIcon.setPreserveRatio(true);
        appIcon.setToolTipCallback(new IToolTipCallback() {
            public String getToolTipText() {
                return "This action is part of \"" + stepModel.getNamespace() + "\"";
            }
        });

        EventHandler<MouseEvent> eh = new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                e.consume();
                if (infoToggle.contains(e.getX(), e.getY())) {
                    expanded.setValue(!expanded.getValue());
                    if (expanded.getValue())
                        toggleImage.setValue(toggleImageToggled);
                    else
                        toggleImage.setValue(toggleImageUntoggled);
                    recalcHeight();
                }
            }
        };

        infoToggle.mouseTransparentProperty().bind(disabledProperty());
        infoToggle.imageProperty().bind(toggleImage);
        infoToggle.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                e.consume();
            }
        });
        infoToggle.setOnMouseReleased(eh);

        EventHandler<MouseEvent> eh2 = new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                if (config.contains(e.getX(), e.getY()))
                    onConfigRequested.run();
            }
        };

        config.visibleProperty().bind(
                showConfigButton.and(procedureView.readOnlyProperty().not().and(disabledProperty().not())));
        config.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                e.consume();
            }
        });
        config.setOnMouseReleased(eh2);
        config.setImage(Utilities.getImage("config-icon1.png"));
        config.pressedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> value, Boolean oldVal, Boolean newVal) {
                if (newVal)
                    config.setImage(Utilities.getImage("config-icon2.png"));
                else
                    config.setImage(Utilities.getImage("config-icon1.png"));
            }
        });

        if (getStepViewContainer() instanceof IdiomStepView)
            knurling.setVisible(false);
        else
            knurling.visibleProperty().bind(procedureView.readOnlyProperty().not().and(disabledProperty().not()));

        bottomBorder.setStartX(0);
        bottomBorder.endXProperty().bind(titlePane.widthProperty());
        bottomBorder.startYProperty().bind(titlePane.heightProperty().subtract(DEFAULT_BORDER_WIDTH));
        bottomBorder.endYProperty().bind(titlePane.heightProperty().subtract(DEFAULT_BORDER_WIDTH));

        titlePane.getChildren().addAll(headerText, appIcon, knurling, bottomBorder, config, infoToggle, issueIcon);               

        titlePane.getStyleClass().add("step-header-rounded");
        InvalidationListener roundedListener = new InvalidationListener() {
            public void invalidated(Observable value) {
                String cssClass;
                if (contentArea.getValue() == null && !expanded.getValue())
                    cssClass = "step-header-rounded";
                else
                    cssClass = "step-header-half-rounded";

                titlePane.getStyleClass().clear();
                titlePane.getStyleClass().add(cssClass);
            }
        };
        contentArea.addListener(roundedListener);
        expanded.addListener(roundedListener); 

        return titlePane;
    }

    @Override
    public void updateIssueVisualization() {
        if (stepModel.getIssues().size() <= 0 || isDisabled() || procedureView.isReadOnly()) {
            issueIcon.setVisible(false);
            return;
        }

        issueIcon.setToolTipCallback(new IToolTipCallback() {
            public String getToolTipText() {
                StringBuffer tt = new StringBuffer(stepModel.getIssues().get(0).isError() ? "Error: " : "Warning: ");

                for (Object obj : stepModel.getIssues().get(0).getFancyMessage(true))
                    tt.append(obj);

                if (stepModel.getIssues().size() > 1)
                    tt.append(" (plus " + (stepModel.getIssues().size() - 1) + " more)");

                tt.append("\n\nClick to fix.");

                return tt.toString();
            }
        });
        issueIcon.setCursor(Cursor.HAND);
        String imgPath;
        if (stepModel.getIssues().get(0).isError())
            imgPath = "curl-error.png";
        else
            imgPath = "curl-warning.png";

        issueIcon.setImage(Utilities.getImage(imgPath));
        issueIcon.setVisible(true);
    }

    protected GridPane createExpansionArea() {
        final Label nameLabel = new Label("name");
        nameLabel.setFont(Fonts.STANDARD_TINY);
        nameLabel.setTextFill(Colors.DisabledText);
        nameLabel.setTextAlignment(TextAlignment.RIGHT);
        nameLabel.setWrapText(false);
        nameLabel.setTextOverrun(OverrunStyle.CLIP);

        final Label name = new Label();
        name.textProperty().bind(stepModel.nameProperty());
        name.setFont(Fonts.STANDARD);
        name.setTextFill(Colors.DisabledText);
        name.setWrapText(true);

        final Label descLabel = new Label("description");
        descLabel.setFont(Fonts.STANDARD_TINY);
        descLabel.setTextFill(Colors.DisabledText);
        descLabel.setTextAlignment(TextAlignment.RIGHT);

        final Text description = new Text();
        if (stepModel.getName().equals(stepModel.getDescriptionText()))
            description.textProperty().bind(stepModel.nameProperty());
        else
            description.setText(stepModel.getDescriptionText());
        description.setFont(Fonts.STANDARD);
        description.setFill(Colors.DisabledText);
        description.wrappingWidthProperty().bind(widthProperty().subtract(EXP_COL_1_WIDTH));

        expansionArea.layoutYProperty().bind(titlePane.heightProperty());
        expansionArea.layoutXProperty().bind(borderRect.layoutXProperty().add(DEFAULT_BORDER_WIDTH));
        expansionArea.setPadding(new Insets(PAD, 0, PAD, PAD));
        expansionArea.setVgap(PAD);
        expansionArea.setHgap(PAD);
        expansionArea.prefWidthProperty().bind(widthProperty().subtract(DEFAULT_BORDER_WIDTH * 2));

        GridPane.setConstraints(nameLabel, 0, 0, 1, 1, HPos.RIGHT, VPos.TOP, Priority.NEVER, Priority.NEVER,
                new Insets(2, 0, 0, 0));
        GridPane.setConstraints(name, 1, 0);
        GridPane.setConstraints(descLabel, 0, 1, 1, 1, HPos.RIGHT, VPos.TOP, Priority.NEVER, Priority.NEVER,
                new Insets(2, 0, 0, 0));
        GridPane.setConstraints(description, 1, 1);

        expansionArea.getChildren().setAll(nameLabel, name, descLabel, description);
        getAdditionalExpansionContent(expansionArea, 2);
        expansionArea.visibleProperty().bind(expanded);

        return expansionArea;
    }

    protected void getAdditionalExpansionContent(GridPane grid, int row) {
        // Intentionally empty
    }

    public final Group createStepBackground() {
        
    	Rectangle background = new Rectangle();
        background.fillProperty().bind(stepLightBackground);
        background.setArcHeight(EDGE_ROUNDING);
        background.setArcWidth(EDGE_ROUNDING);
        background.setStrokeWidth(0);
        background.widthProperty().bind(widthProperty().subtract(DEFAULT_BORDER_WIDTH * 2));
        background.heightProperty().bind(
                contentHeight.subtract(titlePane.heightProperty()).subtract(DEFAULT_BORDER_WIDTH * 2));
        background.layoutXProperty().bind(borderRect.layoutXProperty().add(DEFAULT_BORDER_WIDTH));
        background.layoutYProperty().bind(expansionArea.layoutYProperty().add(DEFAULT_BORDER_WIDTH));

        Line border = new Line();
        border.setStrokeWidth(1);
        border.setStroke(Colors.SystemGray);
        border.startXProperty().bind(background.layoutXProperty());
        border.endXProperty().bind(border.startXProperty().add(background.widthProperty()));
        border.startYProperty().bind(
                titlePane.layoutYProperty().add(titlePane.heightProperty()).add(expansionArea.heightProperty()));
        border.endYProperty().bind(
                titlePane.layoutYProperty().add(titlePane.heightProperty()).add(expansionArea.heightProperty()));
        border.visibleProperty().bind(expanded.and(contentArea.isNotNull()));

        stepBackground = new Group();
        stepBackground.getChildren().addAll(background, border);

        return stepBackground;
    }

    private static boolean selectedAtStart = false;

    protected void registerEventHandlers() {
        setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                me.consume();
                SelectionManager selectionManager = procedureView.getSelectionManager();
                selectionManager.setMultiSelect(me);

                StepView sv = (StepView) me.getSource();

                boolean currentlySelected = selectedAtStart = sv.isSelected();

                if (sv.isPointInContent(me.getX(), me.getY())) {
                    boolean dragListen = true;

                    // Prevent user from multi-selecting a step from a different
                    // StepLayout
                    // than the currently selected steps.
                    if (selectionManager.isMultiSelecting()) {
                        for (ISelectable sel : selectionManager.getSelectedItems())
                            if (((StepView) sel).getStepViewContainer().getStepLayout() != sv.getStepViewContainer()
                                    .getStepLayout())
                                return;
                    }

                    // Don't allow steps within loops and their sub-steps to be
                    // selected at the same time
                    IStepViewContainer parent = sv.getStepViewContainer();
                    while (parent instanceof LoopView) {
                        LoopView loop = (LoopView) parent;
                        if (loop.isSelected())
                            selectionManager.setSelection(loop, false);
                        parent = loop.getStepViewContainer();
                    }

                    if (sv.getStepModel().getStepType() == StepType.LOOP)
                        ((LoopView) sv).unselectDescendents();

                    if (currentlySelected && selectionManager.isMultiSelecting()) {
                        selectionManager.setSelection(sv, false);
                        dragListen = false; // Don't listen for drag on a step
                                            // just got unselected
                    } else if (currentlySelected) {
                        // Do nothing, let the released event handle it in case
                        // user
                        // is trying to initiate a multi-step drag
                    } else if (!selectionManager.isMultiSelecting())
                        selectionManager.selectOnly(sv);
                    else
                        selectionManager.setSelection(sv, true);

                    // Start watching for a drag
                    if (dragListen) {
                        DragDropManager.getInstance().handleDragging(sv, me);
                    }
                } else {
                    if (!selectionManager.isMultiSelecting())
                        selectionManager.selectNone();
                    procedureView.startSelectionRect(new Point2D(me.getSceneX(), me.getSceneY()));
                }
            }
        });
        setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                SelectionManager selectionManager = procedureView.getSelectionManager();
                StepView sv = (StepView) e.getSource();
                if (sv.isPointInContent(e.getX(), e.getY())) {
                    if (selectedAtStart && !selectionManager.isMultiSelecting()
                            && !DragDropManager.getInstance().isDragging()) {
                        selectionManager.selectOnly(sv);
                    }
                }

                e.consume();
                procedureView.clearSelectionRect();
            }
        });
        setOnMouseDragged(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                e.consume();
                procedureView.moveSelectionRect(new Point2D(e.getSceneX(), e.getSceneY()));
            }
        });
    }

    // Properties
    public SimpleObjectProperty<Paint> getBorderColor() {
        return borderColor;
    }

    public Label getStepIndexLabel() {
        return stepIndexLabel;
    }
    
    public int getIndex() {
        return getStepModel().getIndex();
    }
    
}
