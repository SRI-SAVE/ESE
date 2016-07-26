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

package com.sri.tasklearning.ui.core.term.function;

import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;

import com.sri.tasklearning.ui.core.IUndoable;
import com.sri.tasklearning.ui.core.common.CommonView;
import com.sri.tasklearning.ui.core.step.StepView;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.ConstantValueView;
import com.sri.tasklearning.ui.core.term.ParameterModel;

/** 
 * A view that visualizes the different 'keys' (field names) on a struct type
 * that could be used to access values of the same type on an instance of that 
 * struct type. Unless a struct has two fields whose types are assignable 
 * (usually means 'equal' give or take inheritance), this menu button for this 
 * view will only contain a single option. Although this class is named 
 * MapGetKeyView, it is also used for displaying the key corresponding to the 
 * index passed to the nth function.
 */
public class StructureGetKeyView extends ConstantValueView {
    private final StructureGetModel sgm;
    
    public StructureGetKeyView(
            final ConstantValueModel argModel, 
            final StructureGetModel sgm,
            final ParameterModel argParamModel, 
            final StepView argStepView,
            final CommonView argProcView) {
        super(argModel, argParamModel, argStepView, argProcView);
        
        this.sgm = sgm; 
    }
    

    @Override
    public List<MenuItem> buildTermOptionsMenu() {
        List<MenuItem> items = new ArrayList<MenuItem>();
        List<String> keys = sgm.getKeyOptions();
        final String oldKey = sgm.getKey();
        
        for (String key : keys) {
            final String newKey = key;
            CheckMenuItem cmi = new CheckMenuItem(key);
            cmi.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {                    
                    if (!newKey.equals(oldKey)) {
                        // Change is so simple, do it inline rather than via the Controller
                        sgm.setKey(newKey);
                        IUndoable undo = new IUndoable() {
                            public boolean undo() {                                    
                                sgm.setKey(oldKey);
                                return true;
                            }
                            public boolean redo() {
                                sgm.setKey(newKey);
                                return true;
                            }
                            public String getDescription() {
                                return "Change struct field from " + oldKey + " to " + newKey;
                            }
                        };
                        procView.getController()
                                .getUndoManager()
                                .pushUndo(undo);            
                    }
                }
            });
            cmi.setSelected(sgm.getKey().equals(key));
            items.add(cmi);
        }

        // Shouldn't happen, but just in case
        if (items.size() == 0) {
            MenuItem empty = new MenuItem("No appropriate key exists");
            empty.setDisable(true);
            items.add(empty);
        }

        return items;
    }
}
