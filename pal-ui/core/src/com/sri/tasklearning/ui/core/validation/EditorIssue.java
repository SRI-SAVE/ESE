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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.core.Issue;
import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.errors.ErrShouldBeInputs;
import com.sri.ai.lumen.errors.ErrShouldBeInputs.InputParametersNotEvaluableError;
import com.sri.ai.lumen.errors.ErrUndeclaredFunctor;
import com.sri.ai.lumen.errors.ProcedureInputParameterUnused;
import com.sri.ai.lumen.util.repair.RepairOperation;
import com.sri.ai.lumen.util.repair.RepairOperationDeleteStep;
import com.sri.ai.lumen.util.repair.RepairSuggestion;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.BackendInterface;
import com.sri.tasklearning.ui.core.ProcedureEditController;
import com.sri.tasklearning.ui.core.PalUiException;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * Wraps a validation issue detected by Lumen. Note that this class has several 
 * specializations but is concrete and is used as the default class for 
 * representing a validation issue. 
 */

public class EditorIssue {
    private static final Logger log = LoggerFactory.getLogger(EditorIssue.class);

    protected final BackendInterface backend = BackendFacade.getInstance();
    protected final ProcedureEditController control;
    
    private final ProcedureModel procModel;
    private StepModel step;   
    private final Issue issue;
    private ATR issueLocation;
    private final List<RepairSuggestion> suggestions;
    
    protected boolean nested = false; // is the issue nested inside of something (function calls) 
        
    /**
     * A factory method for creating EditorIssues.
     * 
     * @param issue
     *            the model to visualize
     * @return an EditorIssue that visualizes this issue
     */
    public static EditorIssue create(Issue issue, List<RepairSuggestion> suggestions, ProcedureModel procedure) {
        if (issue instanceof InputParametersNotEvaluableError)
            return new EditorIssueUnboundInputsError(issue, suggestions, procedure);
        else if (issue instanceof ErrShouldBeInputs.InputParametersNotEvaluableError)
            return new EditorIssuePreviousUnboundWarning(issue, suggestions, procedure);
        else if (issue instanceof ErrShouldBeInputs.LoopInputParametersEvaluableError)
            return new EditorIssueUnboundLoopError(issue, suggestions, procedure);
        else if (issue instanceof ErrUndeclaredFunctor.UndeclaredFunctorError)
            return new EditorIssueUndeclaredFunctor(issue, suggestions, procedure);
        else if (issue instanceof ProcedureInputParameterUnused)
            return null; // We detect/handle this warning ourselves through variable management
        else
            return new EditorIssue(issue, suggestions, procedure);        
    }

    protected EditorIssue(
            final Issue issue, 
            final List<RepairSuggestion> lumenSuggestions, 
            final ProcedureModel procedure) {
        
        this.issue = issue;
        procModel = procedure;
        control = procedure.getController();
        
        this.suggestions = new ArrayList<RepairSuggestion>();
        
        issueLocation = BackendFacade.locate(issue.getPreorderIndex(), procModel);
        
        if (issueLocation == null)
            log.error("Could not locate issue {}", issue);
        else {
            if (issueLocation instanceof StepModel) {
                step = (StepModel) issueLocation;
            } else if (issueLocation instanceof TermModel) {
                ATR found = null;
                int i = 1;
                do {
                    found = BackendFacade.locate(issue.getPreorderIndex(), procModel, i++);
                } while (!(found instanceof StepModel) && i < 1000);
                if (found instanceof StepModel)
                    step = (StepModel) found;
                else
                    step = null;
                nested = true;
            } else {
                log.error("Unsupported error location {} for error {}", issueLocation.getClass(), issue);
                step = null;
            }
        }
               
        // If there were previously no errors/warnings, but now there is (given
        // that we're in this constructor) we know that the last edit caused 
        // all errors/warnings reported by lumen, so we should offer 'undo' as 
        // a fix. If the user makes subsequent edits that do not resolve the 
        // errors/warnings, undo will not be an explicit repair operation 
        // because It would be cumbersome to track which errors/warnings 
        // were triggered by that edit as opposed to previous edits. Plus, they 
        // can always undo from the toolbar.
        if (control.getUndoManager().canUndo() &&
            control.numStepsWithErrorsProperty().getValue() == 0 &&
            control.numStepsWithWarningsProperty().getValue() == 0) {
            RepairSuggestion undoSug = new RepairSuggestion(EditorRepairOperationUndo.MESSAGE);
            undoSug.addOperation( new EditorRepairOperationUndo(procModel));
            suggestions.add(undoSug);
        }
        
        for (RepairSuggestion sug : lumenSuggestions) {
            if (nested && !(sug.getOperations().get(0) instanceof RepairOperationDeleteStep))
                continue;            
            
            RepairSuggestion newSug = new RepairSuggestion(sug.getDescription());
            for (RepairOperation op : sug.getOperations()) {                
                try {
                    RepairOperation newOp = EditorRepairOperation.create(op,
                            procModel);
                    newSug.addOperation(newOp);
                } catch (PalUiException e) {
                    log.error("Unable to generate UI repair operation", e);
                }
            }
            if (newSug.getOperations().size() > 0)
                suggestions.add(newSug);
        }        
    }

    public ProcedureModel getProcedureModel() {
        return procModel; 
    }
    
    public StepModel getStep() {
        return step;
    }   
    
    public Issue getIssue() {
        return issue;
    }
    
    public ATR getIssueLocation() {
        return issueLocation; 
    }
    
    public List<RepairSuggestion> getSuggestions() {
        return suggestions;
    }

    public Boolean isError() {
        return (issue.getSeverity()
                .equals(com.sri.ai.lumen.core.Issue.Severity.ERROR));
    }
    
    public List<Object> getFancyMessage(boolean asString) {
        List<Object> fancyName = new ArrayList<Object>();
        fancyName.add(issue.getMessage());
        return fancyName; 
    }

    @Override
    public String toString() {
        return issue.toString();
    }
}
