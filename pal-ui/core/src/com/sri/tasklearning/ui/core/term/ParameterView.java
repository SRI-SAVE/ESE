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

package com.sri.tasklearning.ui.core.term;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.TypeDef;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.Colors;
import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.PalUiException;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.TermSplitMenuButton;
import com.sri.tasklearning.ui.core.control.ToolTipper;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTippable;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.function.FunctionModel;
import com.sri.tasklearning.ui.core.term.function.StructureGetKeyView;

/**
 * View for a {@link ParameterModel}. Essentially consists of a pairing between
 * an optional label and a {@link TermView}.
 */
public class ParameterView extends HBox implements IToolTippable {
    public static final double SPACING = 4;
    
    private static final Logger log = LoggerFactory
            .getLogger(ParameterView.class); 
    
    protected ParameterModel parameterModel;
    protected StepView stepView;
    protected CommonView procView;
    protected TermModel term; 
    protected TermView termView;
    protected Label paramLabel = new Label();
    protected final boolean nested;
    
    protected SimpleObjectProperty<Paint> labelColor = 
        new SimpleObjectProperty<Paint>(Colors.DisabledText);

    protected SimpleBooleanProperty inline = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty readOnly = new SimpleBooleanProperty(false);
    private BackendFacade backend = BackendFacade.getInstance();
    private boolean readOnlyRequest = false;
    private BooleanProperty parentReadOnly = null;

    public ParameterView(
            final ParameterModel argParamModel,
            final StepView argStepView,
            final CommonView argProcView, 
            final boolean argInline, 
            final boolean nested,
            final BooleanProperty parentReadOnly) {
    	
        super(SPACING);
        
        this.parameterModel = argParamModel;
        this.stepView = argStepView;
        this.procView = argProcView;
        this.term = parameterModel.getTerm();
        this.nested = nested;
        this.parentReadOnly = parentReadOnly; 
        
        // When the model's term argument changes, refresh the view
        parameterModel.termProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable val) {
                updateTermView();
            }
        });
        
        readOnly.addListener(new ChangeListener<Boolean>() {
            public void changed(
                    final ObservableValue<? extends Boolean> value, 
                    final Boolean oldValue, 
                    final Boolean newValue) {
                if (termView != null)
                    termView.getNode().setDisable(newValue);
            }
        });
        
        
        parameterModel.boundProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable value) {
                calculateReadOnly(); 
            }
        });
        
        if (parentReadOnly != null)
            parentReadOnly.addListener(new InvalidationListener() {
                public void invalidated(Observable value) {
                    calculateReadOnly(); 
                }
            });            
        
        setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                ToolTipper.hideTooltip();
            }
        });
        setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                ToolTipper.hideTooltip();
                ToolTipper.showTooltip(ParameterView.this, event);
            }
        });
        setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                ToolTipper.hideTooltip();
            }
        });
        setOnMouseMoved(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                if (!ToolTipper.isTooltipShowing())
                    ToolTipper.showTooltip(ParameterView.this, e);
            }
        });
        
        inline.setValue(argInline); 

        this.setAlignment(Pos.CENTER_LEFT);      

        // Label not displayed for inline arguments. 
        if (!argInline)
            this.getChildren().add(createLabel());
        
        if (createTermView() != null)
            this.getChildren().add(termView.getNode());      
    }

    public boolean isReadOnlyRequest() {
        return readOnlyRequest;
    }

    public void setReadOnlyRequest(boolean readOnlyRequested) {
        this.readOnlyRequest = readOnlyRequested;
        calculateReadOnly();
    }

    private void calculateReadOnly() {
        if (readOnly.getValue() != (readOnlyRequest || parameterModel.isBound() || 
            (parentReadOnly != null && parentReadOnly.getValue())))
            readOnly.setValue(!readOnly.getValue());
    }
    
    private Label createLabel() {
        paramLabel.textFillProperty().bind(labelColor); 
        paramLabel.setFont(Fonts.STANDARD);
        paramLabel.setOpacity(1);
        
        if (parameterModel.isResult()) {
            if (term instanceof VariableModel)  
                paramLabel.setText(
                        TypeUtilities.getAName(parameterModel.getTypeDef()) +
                        ", let's call it");            
        } else
            paramLabel.setText(parameterModel.getName());
        
        return paramLabel;
    }
    
    private String getTooltipAdvice() {
        if (readOnly.getValue())
            return "";
        
        return parameterModel.isResult() ? "\n\nClick to rename." : "\n\nClick to modify";
    }

    private TermView createTermView() {    	
    	
        try {
            termView = TermView.create(term, parameterModel, stepView, procView, nested);
        } catch (Exception e) {
            log.error("Failed to create view for term", e);
            return null; 
        }
        termView.setReadOnly(parameterModel.isResult());
        
        TypeDef type = parameterModel.getTypeDef(); 

        if (type == null && term != null)
            type = term.getTypeDef();
        
        final TermModel term = parameterModel.getTerm();       
        final String typeAName = TypeUtilities.getAName(type);
        
        String tt = null;
        if (parameterModel.isResult())
            tt = "Result: " + typeAName;
        else {
            String paramName = parameterModel.getName();
            if (paramName != null && paramName.length() > 0)
                tt = "Input \"" + paramName + "\" ";
            
            if (typeAName != null)
                tt += "(" + typeAName + "):";
            else 
                tt += ":";
            
            tt += "\n";
            
            if (parameterModel.getDescription() != null &&
                parameterModel.getDescription().length() > 0 &&
                !parameterModel.getDescription().equals(paramName))
                tt += parameterModel.getDescription().trim() + "\n\n";
            else
                tt += "\n";
        }        
        
        final String tooltip = tt;        
        
        if (term instanceof FunctionModel && FunctionModel.isAccessorFunction((FunctionModel)term)) {
            setToolTipCallback(new IToolTipCallback() {
                public String getToolTipText() {
                    return tooltip + "Using " +
                           term.getDisplayString() +
                           getDebugMessage() +
                           getTooltipAdvice();
                }
            });
        } else if (term instanceof CompositeTermModel) {
            setToolTipCallback(new IToolTipCallback() {
                public String getToolTipText() {
                    String disp = term.getDisplayString();
                    if (disp.length() > 100)
                        disp = disp.substring(0, 100) + "...)"; 
                    
                    return disp; 
                    //return tooltip + "Using value " + disp + getDebugMessage() + getTooltipAdvice();
                }
            });        
        } else if (term instanceof ProcedureInputVariableModel) {
            final ProcedureInputVariableModel pim = 
                    (ProcedureInputVariableModel) term;
            setToolTipCallback(new IToolTipCallback() {
                public String getToolTipText() {
                    String ret = tooltip + "Using procedure input \"" + 
                                 term.getDisplayString() + "\"";
                    
                    String debug = getDebugMessage();
                    
                    if (debug.length() > 0)
                        ret += debug;
                    else if (pim.getDefaultValue() != null)
                        ret += "\nDefault value: "
                                + pim.getDefaultValue().getDisplayString()
                                        .trim();                    
                    
                    ret += getTooltipAdvice();
                    return ret; 
                }
            });
        } else if (term instanceof VariableModel) {         
            setToolTipCallback(new IToolTipCallback() {
                public String getToolTipText() {
                    if (parameterModel.isResult())
                        return tooltip + " " + 
                               getDebugMessage() + 
                               getTooltipAdvice();
                    else
                        return tooltip + "Using the variable '"
                                + term.getDisplayString() + "'"
                                + getDebugMessage()
                                + getTooltipAdvice();
                }
            });
        } else if (termView instanceof StructureGetKeyView) {
            setToolTipCallback(new IToolTipCallback() {
                public String getToolTipText() {
                    return "Using the field: "
                            + ((TermSplitMenuButton) ((StructureGetKeyView) termView).getNode())
                                    .getText() + getTooltipAdvice();
                }
            });
        } else if (term instanceof ConstantValueModel) {        
            setToolTipCallback(new IToolTipCallback() {
                public String getToolTipText() {
                    return /*tooltip + "Using the value: "
                            + */term.getDisplayString()/* + getTooltipAdvice()*/;
                }
            });
        } else if (term instanceof NullValueModel) {
            setToolTipCallback(new IToolTipCallback() {
                public String getToolTipText() {
                    return "(empty)";
                    //return tooltip + "Value is undefined (null). Click to fix.";
                }
            });
        } else {
            log.error("Unsupported term type. {}", term);
            return null;
        }
        
        termView.getNode().setDisable(readOnly.getValue());

        return termView;
    }
    
    private String getDebugMessage() {
        if (!backend.isDebuggingProcedure())
            return "";
        
        Object value; 
        try {
            value = backend.getDebugValue(termView.getTermModel());
            if (value == null)
                return "";
        } catch (PalUiException e) {
            return "";
        }        
        
        return "\n\nDebug value: " + value;
    }
    
    public TermView getTermView() {
        return termView; 
    }
    
    public ParameterModel getParameterModel() {
        return parameterModel; 
    }
    
    public void updateTermView() {
        if (parameterModel.getTerm() == null)
            return; 
        
        this.getChildren().remove(termView.getNode());
        this.term = parameterModel.getTerm();        
        termView = createTermView();        
        
        getChildren().add(termView.getNode());

        if (stepView != null)
            stepView.requestLayout();
    }

    // IToolTippable
    private IToolTipCallback toolTipCallback = new IToolTipCallback() {
        public String getToolTipText() {
            return "";
        }
    };

    public IToolTipCallback getToolTipCallback() {
        return toolTipCallback;
    }

    public void setToolTipCallback(IToolTipCallback toolTipCallback) {
        this.toolTipCallback = toolTipCallback;
    }

    public void refresh() {
    }

    @Override
    public Node getToolTipNode() {
        return this;
    }
}
