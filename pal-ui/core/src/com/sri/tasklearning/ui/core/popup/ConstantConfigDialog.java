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

package com.sri.tasklearning.ui.core.popup;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.Callback;

import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.control.constant.ConstantEditor;
import com.sri.tasklearning.ui.core.procedure.ProcedureView;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.ProcedureInputVariableModel;
import com.sri.tasklearning.ui.core.term.TypeUtilities;

/**
 * A popup dialog for editing atomic constant values. Non-atomic values
 * are edited using a descendent of ComplexEditorDialog. 
 */
public class ConstantConfigDialog extends BasicModalPopup {

    final private ParameterModel arg;
    final private StepView owningStepView;
    final private CommonView procView; 
    private ConstantEditor constantEditor;

    public static void showDialog(final ParameterModel arg, 
                                  final Scene owner,
                                  final StepView sender,
                                  final CommonView procView, 
                                  final Callback<Object, Boolean> onOkay,
                                  final Runnable onCancel) {
        ConstantConfigDialog cfd = 
            new ConstantConfigDialog(arg, sender, procView, onOkay, onCancel);
        
        cfd.show(owner, sender);
    }

    protected ConstantConfigDialog(final ParameterModel arg, 
                                   final StepView stepView,
                                   final CommonView procView, 
                                   final Callback<Object, Boolean> onOkay,
                                   final Runnable onCancel) {
        this.arg = arg;
        this.owningStepView = stepView;
        this.procView = procView;
        this.onCancelPressed = onCancel;        
        this.onOkayPressed = new Callback<Object, Boolean>() {
            public Boolean call(Object obj){
                if (onOkay != null)
                    onOkay.call(constantEditor.getValue());
                return true;
            }
        };

        titleText = "Set Value";
        okButtonText = "Set";
    }

    @Override
    public void focus() {
        constantEditor.select();
    }

    @Override
    public Pane getContent() {
        constantEditor = ConstantEditor.create(arg.getTypeDef(), owningStepView, procView);
        constantEditor.getPane().setTranslateX(PAD);

        // Set the editor's value to the current value, if the variable is
        // already a constant.
        // Or if the variable is a procedure input with a default value, use
        // that value.
        // Otherwise, just use the type's default value.
        if (arg.getTerm() instanceof ConstantValueModel)
            constantEditor.setValue(((ConstantValueModel) arg.getTerm())
                    .getValue());
        else if (arg.getTerm() instanceof ProcedureInputVariableModel
                && ((ProcedureInputVariableModel) arg.getTerm())
                        .getDefaultValue() != null)
            constantEditor.setAtrValue(((ProcedureInputVariableModel) arg
                    .getTerm()).getDefaultValue().deepCopy());
        else
            constantEditor.setValue(constantEditor.getDefault());

        constantEditor.getPane().setPrefWidth(prefWidth - 4 - PAD * 2);

        final Text instructions = new Text("Set value: " + arg.getDescription()
                + " (" + TypeUtilities.getAName(arg.getTypeDef()) + ")");
        instructions.setFont(Fonts.DIALOG_TEXT);

        Pane dialogContent = new Pane() {
            @Override
            protected void layoutChildren() {
                super.layoutChildren();

                instructions.relocate(PAD, PAD);
                instructions.setWrappingWidth((int) (getWidth() - PAD * 2));

                constantEditor.getPane().setLayoutY(
                        instructions.getBoundsInParent().getMaxY() + PAD);
            }
        };

        dialogContent.getChildren().addAll(instructions,
                constantEditor.getPane());

        return dialogContent;
    }
}
