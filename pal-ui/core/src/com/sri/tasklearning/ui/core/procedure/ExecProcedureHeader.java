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

package com.sri.tasklearning.ui.core.procedure;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.util.Callback;

import com.sri.pal.PALException;
import com.sri.tasklearning.ui.core.common.InputVariableModel;
import com.sri.tasklearning.ui.core.control.ToolTippedImageView;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.constant.ConstantEditor;
import com.sri.tasklearning.ui.core.procedure.ProcedureInputsPanel.ProcedureInputRow;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * This concrete extension of ProcedureHeader is for when you want to provide
 * the user with a mechanism for executing procedures and editing the values 
 * of inputs associated with that procedure invocation without affecting any
 * default values associated with inputs. This header initially loads input
 * controls that are populated with the default values, but allows them to be
 * overwritten and verifies that all inputs have a value before allowing
 * the procedure to be invoked. 
 */
public final class ExecProcedureHeader extends ProcedureHeader {
    
    private final List<Object> args;
    private final List<InputVariableModel> argDefs;
    private final boolean defaultDebug;
    private final Stage parent;
    
    public ExecProcedureHeader(
            final ProcedureView argProcView,
            final boolean defaultDebug,
            final Stage parent) {
        super(argProcView);
        
        this.defaultDebug = defaultDebug;
        this.parent = parent;
        
        setStyle("-fx-border-width: 0;");
        
        runButton.setText(defaultDebug ? "Debug" : "Run");
        
        argDefs = procedureModel.getSignature().getInputs();        
        args = new ArrayList<Object>(argDefs.size());

        for (InputVariableModel in : argDefs) {
            final TermModel dflt = in.getDefaultValue();
            
            if (dflt == null)
                args.add(null);
            else
                try {
                    args.add(in.getTypeDef().fromAtr(dflt));
                } catch (PALException e) {
                    args.add(null);
                }
        }
        
        checkArguments();
    }
    
    @Override
    protected void configureRunButton() {
        
        run.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {                
                if (checkArguments()) {
                    runProcedure(procedureView, args, false);
                }
            }
        });
        
        debug.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {                
                if (checkArguments()) {
                    runProcedure(procedureView, args, true);
                    Platform.runLater(new Runnable() { public void run() {parent.hide();} });
                }
            }
        });        
        
        runButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(final ActionEvent e) {
                if (defaultDebug)
                    debug.fire();
                else
                    run.fire();
            }
        });        
    }
    
    @Override
    protected void addChildren() {
        getChildren().addAll(titleBarBox, gears, boxBorder, topHighlight,
                leftHighlight, bottomShadow, rightShadow, title, inputs,
                exp, runButton);
        
        if (desc.getText() != null && desc.getText().length() > 0) {
            getChildren().add(desc);
            desc.setDisable(true);
        }
    }
    
    @Override
    protected double getBottomOffset() {
        return 8;
    }
    
    @Override
    protected ProcedureInputsPanel getProcedureInputsPanel() {
        return new ProcedureInputsPanel(this, procedureView, true);
    }
    
    @Override
    public ConstantEditor configureProcedureInputRow(
            final ProcedureInputRow row,
            final ConstantEditor editor, 
            final boolean forceEditor) {
        
        final ProcedureInputVariableModel input = row.getInput();
        
        if (input.getDefaultValue() != null)
            editor.setAtrValue(input.getDefaultValue());
        
        editor.setDoLiveUpdates(true);
            
        editor.setTooltip(new IToolTipCallback() {
            public String getToolTipText() {
                return "The value for the input '" + input.getDisplayString()
                        + "'";
            }
        });
        
        Callback<Object, Void> call = new Callback<Object, Void>() {
            public Void call(final Object value) {
                int idx = 0;
                for (InputVariableModel in : argDefs) {
                    if (in == input)
                        break;
                    idx++;
                }
                
                Object obj = null;
                
                if (editor.getATRValue() != null &&
                    !(editor.getATRValue() instanceof NullValueModel))
                    try {
                        obj = input.getTypeDef().fromAtr(editor.getATRValue());
                    } catch (PALException e) {
                        obj = null;
                    }
                
                args.set(idx, obj);
                checkArguments();
                return null; 
            }
        }; 
        
        editor.setOnConfirmed(call);
        editor.setOnChanged(call);
        
        ToolTippedImageView errorButton = row.getErrorButton();
        errorButton.setToolTipCallback(new IToolTipCallback() {
            public String getToolTipText() {
                return "The input value is currently empty/null. You may want to provide this value before running the procedure";
            }
        });
        
        return editor;
    }
    
    private boolean checkArguments() {
        boolean satisfied = true;
        // TODO Enable null enforcement once we stop treating all fields like they're nullable        
        int idx = 0;
        for (InputVariableModel in : argDefs) {
            ToolTippedImageView errorButton = inputs.findProcedureInputRow((ProcedureInputVariableModel) in)
                    .getErrorButton();
            if (args.get(idx) == null) {
                in.getTypeDef();

                //satisfied = false;
                errorButton.setVisible(true);
            } else
                errorButton.setVisible(false); 
                
            idx++;
        }
//
//        runButton.setDisable(!satisfied);

        return satisfied;
    }
}
