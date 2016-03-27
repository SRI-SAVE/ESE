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

package com.sri.tasklearning.ui.core.validation;

import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;

import com.sri.ai.lumen.core.Sym;
import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.util.repair.RepairOperation;
import com.sri.ai.lumen.util.repair.RepairOperationChangeParameter;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.ActionStepModel;
import com.sri.tasklearning.ui.core.step.LoopModel;
import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.VariableView;

/**
 * A repair operation that knows how to change a parameter.
 */
public class EditorRepairOperationChangeParameter extends EditorRepairOperation {
    private static final long serialVersionUID = 1L;
    
    private ATR parent;

    /** The unbound argument, if any. */
    private ParameterModel unboundParam;

    /** The candiate variables to which to change this argument. */
    private List<VariableModel> candidates;

    /** The currently selected candidate. */
    private VariableModel selectedCandidate;

    public EditorRepairOperationChangeParameter(
            final RepairOperation op,
            final ProcedureModel proc) {
        super(op, proc);
        
        parent = BackendFacade.locate(getPreorderIndex(), procModel, 1);
        
        Sym[] newCandidates = ((RepairOperationChangeParameter) getOperation())
                .getNewCandidates();
        List<VariableModel> allCandidates = new ArrayList<VariableModel>(newCandidates.length);
        for (int i = 0; i < newCandidates.length; i++) {
            String name = newCandidates[i].toString().replaceAll("\\$", "");
            allCandidates.add(varMgr.getVariableByName(name));
        }

        if ((parent instanceof ActionStepModel) && (getLocation() instanceof VariableModel)) {
            for (ParameterModel curParam : ((ActionStepModel) parent)
                    .getInputs()) {
                if (curParam.getTerm().equals(getLocation())) {
                    unboundParam = curParam;
                    break;
                }
            }
            List<VariableModel> locations = new ArrayList<VariableModel>();
            locations.add((VariableModel)getLocation());
            candidates = varMgr.filterValidInputs(allCandidates, unboundParam, locations);
        }
        else if (parent instanceof LoopModel) {
            unboundParam = ((LoopModel)parent).getInputCollection();
            candidates = allCandidates;
        }
    }

    /**
     * Changes the parameter as described by the operation.
     * 
     * @return whether the EditController successfully changed the argument or not
     */
    @Override
    public boolean applyOperation() {
        return controller.changeArgument(
                unboundParam, selectedCandidate, getStep(), false);
    }

    // build the UI
    @Override
    public EditorRepairUI getSuggestionUI(EditorIssue issue) {

        if (candidates != null && candidates.size() > 0) {
            EditorRepairUI ui = new EditorRepairUI(this, "Replace ");

            // In case the broken variable reference is nested within a 
            // function call, which we need to be robust to but don't fully
            // support yet. 
            if (!(getLocation() instanceof VariableModel))
                    return null;
            
            VariableView vv = new VariableView(
                    (VariableModel) getLocation(), unboundParam, null, null);
            vv.setDisable(true);
            vv.setInline(true);
            ui.addContent(vv.getNode());
            ui.addText(" with ");

            // make the drop-down list of candidates
            selectedCandidate = candidates.get(0);
            ReplacementVariableView rvv = new ReplacementVariableView(selectedCandidate, ui);
            ui.addContent(rvv.getNode());

            return ui;
        } else
            return null;
    }
    
    private class ReplacementVariableView extends VariableView {
        private final EditorRepairUI owner;
        
        public ReplacementVariableView(VariableModel vm, EditorRepairUI owner) {
            super(vm, null, null, null);
 
            this.owner = owner;
        }
        
        @Override
        public List<MenuItem> buildTermOptionsMenu() {
            List<MenuItem> nodes = new ArrayList<MenuItem>();
            for (VariableModel v : candidates) {
                final VariableModel vm = v;
                CheckMenuItem cmi = new CheckMenuItem(v.getVariableName());
                cmi.setSelected(v.equals(selectedCandidate));
                cmi.setOnAction(new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent e) {
                        selectedCandidate = vm;
                        owner.addContent( new ReplacementVariableView(selectedCandidate, owner).getNode());
                        owner.removeContent(ReplacementVariableView.this.getNode());
                    }
                });
                nodes.add(cmi);
            }
            return nodes;
        }
    }
}
