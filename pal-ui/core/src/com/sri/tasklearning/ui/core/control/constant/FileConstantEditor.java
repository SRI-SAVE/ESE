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

import java.io.File;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import com.sri.pal.StructDef;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.control.Alert;
import com.sri.tasklearning.ui.core.control.Alert.AlertConfig;
import com.sri.tasklearning.ui.core.control.ToolTipper.IToolTipCallback;
import com.sri.tasklearning.ui.core.control.TooltipPlus;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.function.StructureModel;

/**
 * A special ConstantEditor for struct values that represent a file. This editor
 * is currently only used by procedures learned over the image loader
 * application but could feasibly be used by another applicaiton that defined
 * a file struct in the same way as image loader. 
 */
public class FileConstantEditor extends ConstantEditor {
    private static final int IDX_DIR = 0;
    private static final int IDX_NAME = 1;
    private static final int IDX_EXT = 2;
    private final StructDef type;
    private final Button launcher = new Button("Browse");
    private final Label lbl = new Label();
    private final BorderPane box = new BorderPane();
    {
        box.setStyle("-fx-background-color: -pal-WindowBackground; -fx-border-color: -pal-SystemDarkGray;");
        BorderPane.setAlignment(lbl, Pos.CENTER_LEFT);
        BorderPane.setAlignment(launcher, Pos.CENTER_RIGHT);
        box.setPadding(new Insets(1,1,1,5));
        box.setLeft(lbl);
        box.setRight(launcher);
    }
    private Scene scene;
    protected StructureModel term = null; 
    
    public FileConstantEditor(final StructDef type,
                              final StepView step) {
        this.type = type;
        box.prefWidthProperty().bind(pane.prefWidthProperty());
        launcher.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                String dir = null;
                if (term != null) {
                    dir = (String) ((ConstantValueModel) term.getElements()
                            .get(IDX_DIR)).getValue();
                }
                Scene argScene = (scene == null) ? launcher.getScene() : scene;
                File res = Utilities.promptOpenFile(argScene.getWindow(),
                        "Select a file", dir, null, null, true);
                
                if (res != null) {                    
                    String path = res.getParent();
                    String name = res.getName();
                    
                    String ext = null;
                    int idx;
                    if ((idx = name.indexOf(".")) > 0) {
                        if (idx < name.length() -1)
                            ext = name.substring(idx + 1);
                        name = name.substring(0, idx);
                    }
                    
                    if (ext == null) {
                        Alert.show("Must specify a file extension", 
                                   "You must specify a file extension", 
                                   AlertConfig.OK, null);
                        return;
                    }
                                                           
                    StructureModel newFileStruct = new StructureModel(FileConstantEditor.this.type);
                    List<ParameterModel> inputs = newFileStruct.getInputs();
                    
                    ConstantValueModel cdir = new ConstantValueModel(path, inputs.get(IDX_DIR).getTypeDef());
                    ConstantValueModel cname = new ConstantValueModel(name, inputs.get(IDX_NAME).getTypeDef());
                    ConstantValueModel cext = new ConstantValueModel(ext, inputs.get(IDX_EXT).getTypeDef());
                    
                    inputs.get(IDX_DIR).setTerm(cdir);
                    inputs.get(IDX_NAME).setTerm(cname);
                    inputs.get(IDX_EXT).setTerm(cext);
                    
                    term = newFileStruct;
                    
                    if (onConfirmed != null)
                        onConfirmed.call(newFileStruct);
                    
                    lbl.setText(name + "." + ext);
                }
            }
        });

        pane.getChildren().add(box);
    }
    
    @Override
    public void setTooltip(IToolTipCallback cb) {
        super.setTooltip(cb);
        if (cb == null)
            launcher.setTooltip(null);
        else
            launcher.setTooltip(new TooltipPlus(pane));
    }
    
    // **************** ConstantEditor abstract methods ************************
    
    @Override
    public void setValue(Object val) {
        
    }
    
    @Override    
    public Object getValue() {
        return null; 
    }
    
    @Override    
    public void setAtrValue(final TermModel term) {
        if (term instanceof StructureModel) {
            this.term = (StructureModel)term;
            lbl.setText(this.term.getInputs().get(IDX_NAME).getTerm()
                    .getDisplayString()
                    + "."
                    + this.term.getInputs().get(IDX_EXT).getTerm()
                            .getDisplayString());
        } else if (term == null || term instanceof NullValueModel)
            this.term = null;
    }

    @Override
    public TermModel getATRValue() {
        if (term != null)
            return term;
        
        return NullValueModel.NULL;
    }
    
    @Override    
    public Object getDefault() {
        return null;
    }    
    
    @Override
    public void select() {
        
    }
    
    @Override
    public boolean isDialog() {
        return true;
    }
    
    @Override
    public void openDialog(Scene scene) {
        this.scene = scene;
        launcher.fire();
    }
}
