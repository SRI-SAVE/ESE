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

package com.sri.tasklearning.ui.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRConstructor;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.logical.ATRLogical;
import com.sri.ai.lumen.atr.logical.ATRPredicate;
import com.sri.ai.lumen.atr.task.ATRIf;
import com.sri.ai.lumen.atr.task.ATRSequence;
import com.sri.ai.lumen.atr.task.ATRTask;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRSymbol;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.ATRVariable;
import com.sri.ai.lumen.core.LumenConstant;
import com.sri.ai.lumen.runtime.StructureGenFunOp;
import com.sri.ai.lumen.runtime.StructureGetFunOp;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.ui.core.procedure.ProcedureModel;
import com.sri.tasklearning.ui.core.procedure.SignatureModel;
import com.sri.tasklearning.ui.core.procedure.StepSequence;
import com.sri.tasklearning.ui.core.step.ActionStepModel;
import com.sri.tasklearning.ui.core.step.IdiomStepModel;
import com.sri.tasklearning.ui.core.step.LoopModel;
import com.sri.tasklearning.ui.core.step.ProcedureStepModel;
import com.sri.tasklearning.ui.core.step.StepModel;
import com.sri.tasklearning.ui.core.term.ActionDeclarationParameterModel;
import com.sri.tasklearning.ui.core.term.ConstantValueModel;
import com.sri.tasklearning.ui.core.term.ListModel;
import com.sri.tasklearning.ui.core.term.MapModel;
import com.sri.tasklearning.ui.core.term.NoEvalTermModel;
import com.sri.tasklearning.ui.core.term.NullValueModel;
import com.sri.tasklearning.ui.core.term.TermModel;
import com.sri.tasklearning.ui.core.term.VariableModel;
import com.sri.tasklearning.ui.core.term.function.*;

/**
 * ATR factory for the PAL UI's ATR implementation (PUTR). Uses classes from 
 * {@code com.sri.tasklearning.ui.core.procedure},
 * {@code com.sri.tasklearning.ui.core.step},
 * {@code com.sri.tasklearning.ui.core.term} and
 * {@code com.sri.tasklearning.ui.core.term.function} packages to construct
 * PUTR procedure representations. 
 */

public class CoreUIModelFactory implements ATRConstructor<ATR, ATRTask, ATRTerm, ATRDecl, ATRLogical, ATRSig,
        ATRParameter, ATRVariable, ATRMap, ATRPredicate, ATRSymbol> {
    
    private final static String MESSAGE = "Editor does not currently support ";

    @Override
    public StepModel createAction(
            final String functor,
            final Collection<? extends ATRTerm> arguments,
            final ATRTask body) {
        // Although ATR doesn't distinguish between a procedure step and a 
        // normal action step, we do so we make that distinction here based on
        // the action's namespace        
        SimpleTypeName name = (functor == null || functor.length() == 0) ? 
                null : (SimpleTypeName)TypeNameFactory.makeName(functor);
        if (name != null && name.getNamespace().equals(BackendInterface.PROC_NAMESPACE))
            return new ProcedureStepModel(functor, arguments);
        else {
            if (body != null) {
                // It's an idiom, so create an IdiomStepModel
                return new IdiomStepModel(functor, arguments, body);
            } else {
                return new ActionStepModel(functor, arguments);
            }
        }
    }


	@Override
	public ATRDecl createActionDeclaration(ATRSig signature, ATRTask taskExpression,
			ATRMap properties) {
		 return new ProcedureModel((SignatureModel)signature, taskExpression, (MapModel)properties);
    } 

    @Override
    public ActionDeclarationParameterModel createParameter(ATRVariable variable, ATRParameter.Modality modality, String type, ATRTerm defaultValue) {
        return new ActionDeclarationParameterModel((VariableModel)variable, modality, type, (TermModel)defaultValue, true);
    }

    @Override
    public FunctionModel createFunction(String functor, Collection<? extends ATRTerm> arguments) {
        @SuppressWarnings("unchecked")
        Collection<TermModel> args = (Collection<TermModel>)arguments; 
        FunctionModel fm;
        if (functor.equals(StructureGenFunOp.NAME_STRING))
            fm = new StructureModel(functor, args);
        else if (functor.equals(StructureGetFunOp.NAME.getFunctor().toString()))
            fm = new StructureGetModel(functor, args);
        else if (functor.equals(LumenConstant.SET_GEN))
            fm = new SetModel(functor, args);
        else if (functor.equals(LumenConstant.BAG_GEN))
            fm = new BagModel(functor, args);
        else if (functor.equals("first") || functor.equals("last") || functor.equals("only"))
            fm = new FirstLastModel(functor, args);
        else if (functor.equals(LumenConstant.SYM_zip.getFunctor().toString()))
            fm = new ZipModel(functor, args);
        else if (functor.equals("positionalTupleGen"))
            fm = new TupleModel(functor, args);
        else if (functor.equals("nth"))
            fm = new TupleGetModel(functor, args);
        else
            fm = new FunctionModel(functor, args);

        return fm;
    }   
    
    @SuppressWarnings("unchecked")
    @Override
    public ListModel createList(Collection<? extends ATRTerm> elements) {        
        return new ListModel((Collection<TermModel>)elements);
    }

    @Override
    public ConstantValueModel createLiteral(Object lit, String type) {;
        return new ConstantValueModel(lit, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapModel createMap(Map<String, ? extends ATRTerm> map) {
        return new MapModel((Map<String, TermModel>)map);
    }
    
    @Override
    public NoEvalTermModel createNoEval(ATRTerm term) {
        return new NoEvalTermModel((TermModel)term);
    }

    @Override
    public NullValueModel createNull() {
        return NullValueModel.NULL;
    }
    
    @Override
    public SignatureModel createSignature(String functor, Collection<ATRParameter> arguments) {
        List<ActionDeclarationParameterModel> parms = new ArrayList<ActionDeclarationParameterModel>();
        for (ATRParameter arg : arguments)
            parms.add((ActionDeclarationParameterModel)arg);
        return new SignatureModel(functor, parms);
    }
    
    @Override
    public VariableModel createVariable(String var) {
        return VariableModel.create(var, VariableManager.getNextVariableManager()); 
    }       
    
    @Override
    public LoopModel createForin(ATRTerm variable, ATRTerm list, ATRTask task, ATRTerm collect, ATRTerm into) {
        ArrayList<StepModel> steps = new ArrayList<StepModel>();
        if ( task instanceof ATRSequence ) {
            for (ATRTask t: ((ATRSequence)task).getTasks())
                steps.add((StepModel)t);
        } else {
            steps.add((StepModel)task);
        }
        return new LoopModel((TermModel)variable, (TermModel)list, steps, (TermModel)collect, (TermModel)into);
    }

    // Begin of unsupported create methods    
    @Override
    public ATRIf createIf(ATRLogical condition, ATRTask ifTask, ATRTask elseTask) {
        throw new UnsupportedOperationException(MESSAGE + " this If");
    }    
    
    @Override
    public ATRActionDeclaration createActionDeclaration(ATRSig signature, String callMethod, ATRMap properties) {
        throw new UnsupportedOperationException(MESSAGE + " this ActionDeclaration");
    }

    @Override
    public ATRLogical createAnd(Collection<? extends ATRLogical> logicalExpressions) {
        throw new UnsupportedOperationException(MESSAGE + "And");
    }

    @Override
    public ATRTask createConclude(ATRPredicate predicate) {
        throw new UnsupportedOperationException(MESSAGE + "Conclude");
    }

    @Override
    public ATRDecl createConstantDeclaration(String name, ATRTerm value) {
        throw new UnsupportedOperationException(MESSAGE + "ConstantDeclaration");
    }

    @Override
    public ATRLogical createExists(Collection<ATRVariable> variables, ATRLogical logicalExpression) {
        throw new UnsupportedOperationException(MESSAGE + "Exists");
    }

    @Override
    public ATRTask createFail(String message) {
        throw new UnsupportedOperationException(MESSAGE + "Fail");
    }

    @Override
    public ATRTask createForall(ATRLogical condition, ATRTask task, ATRTerm collect, ATRTerm into) {
        throw new UnsupportedOperationException(MESSAGE + "Forall");
    }

    @Override
    public ATRDecl createFunctionDeclaration(ATRSig signature, Object eval, Object match, ATRMap properties) {
        throw new UnsupportedOperationException(MESSAGE + "FunctionDeclaration");
    }

    @Override
    public ATRLogical createImplies(ATRLogical left, ATRLogical right) {
        throw new UnsupportedOperationException(MESSAGE + "Implies");
    }

    @Override
    public ATRLogical createGenericLogical(String functor,
            Collection<? extends String> keys, Collection<? extends ATR> arguments) {
        throw new UnsupportedOperationException(MESSAGE + "GenericPredicate");
    }

    @Override
    public ATRDecl createGenericDecl(String functor,
                Collection<? extends String> keys, Collection<? extends ATR> arguments) {
        throw new UnsupportedOperationException(MESSAGE + "GenericDecl");
    }

    @Override
    public ATRTask createGenericTask(String functor,
            Collection<? extends String> keys, Collection<? extends ATR> arguments) {
        throw new UnsupportedOperationException(MESSAGE + "GenericTask");
    }

    @Override
    public ATRTerm createGenericTerm(String functor,
            Collection<? extends String> keys, Collection<? extends ATR> arguments) {
        throw new UnsupportedOperationException(MESSAGE + "GenericLogical");
    }

    @Override
    public ATRLogical createNot(ATRLogical logicalExpression) {
        throw new UnsupportedOperationException(MESSAGE + "Not");
    }

    @Override
    public ATRLogical createOr(Collection<? extends ATRLogical> logicalExpressions) {
        throw new UnsupportedOperationException(MESSAGE + "Or");
    }

    @Override
    public ATRTask createPass() {
        throw new UnsupportedOperationException(MESSAGE + "Pass");
    }

    @Override
    public ATRPredicate createPredicate(String functor, Collection<? extends ATRTerm> arguments) {
        throw new UnsupportedOperationException(MESSAGE + "Predicate");
    }

    @Override
    public ATRDecl createPredicateDeclaration(ATRSig signature, Object solve, Object conclude,
            Object retractall, ATRMap properties) {
        throw new UnsupportedOperationException(MESSAGE + "PredicateDeclaration");
    }

    @Override
    public ATRDecl createProcedureDeclaration(ATRSig cuCATRSignature, ATRTask body, String name, ATRLogical precondition, ATRMap properties) {
        throw new UnsupportedOperationException(MESSAGE + "ProcedureDeclaration");
    }

    @Override
    public ATRTask createRetractAll(ATRPredicate predicate) {
        throw new UnsupportedOperationException(MESSAGE + "RetractAll");
    }

    @Override
    public ATRTask createReturn() {
        throw new UnsupportedOperationException(MESSAGE + "Return");
    }

    @Override
    public ATRTask createSelect(Collection<? extends ATRLogical> logicalExpression,
            Collection<? extends ATRTask> taskExpression) {
        throw new UnsupportedOperationException(MESSAGE + "Select");
    }

    @Override
    public ATRSequence createSequence(Collection<? extends ATRTask> taskExpressions) {
        return new StepSequence(taskExpressions);
    }

    @Override
    public ATRTask createSetValue(ATRVariable variable, ATRTerm value) {
        throw new UnsupportedOperationException(MESSAGE + "SetValue");
    }  

    @Override
    public ATRTerm createSpecial(String functor, Collection<?> initArguments) {
        throw new UnsupportedOperationException(MESSAGE + "Special");
    }

    @Override
    public ATR createStructure(String functor, Collection<? extends ATR> arguments) {
        throw new UnsupportedOperationException(MESSAGE + "Structure");
    }

    @Override
    public ATRSymbol createSymbol(String name) {
        throw new UnsupportedOperationException(MESSAGE + "Symbol");
    }

    @Override
    public ATRTask createTestCondition(ATRLogical logicalExpression) {
        throw new UnsupportedOperationException(MESSAGE + "TestCondition");
    }

    @Override
    public ATRDecl createTriggerDeclaration(ATRSig triggerSignature, ATRTask assertBody, ATRTask retractBody, String name, ATRLogical precondition, ATRMap properties) {
        throw new UnsupportedOperationException(MESSAGE + "TriggerDeclaration");
    }

    @Override
    public ATRTask createTry(ATRTask mainTask, ATRTask onSucceedTask, ATRTask onFailTask, ATRTask finallyTask, ATRVariable failurCVariableModel) {
        throw new UnsupportedOperationException(MESSAGE + "Try");
    }

    @Override
    public ATRTask createWait(Collection<? extends ATRLogical> logicalExpression,
            Collection<? extends ATRTask> taskExpression) {
        throw new UnsupportedOperationException(MESSAGE + "Wait");
    }

    @Override
    public ATRTask createWhile(ATRLogical logicalExpression, ATRTask taskExpression, ATRTerm collect, ATRTerm into) {
        throw new UnsupportedOperationException(MESSAGE + "While");
    }

    @Override
    public ActionDeclarationParameterModel getShortParameter(ATRParameter argument) {
        throw new UnsupportedOperationException(MESSAGE + "ShortParameter");
    }

    @Override
    public ATRDecl createTypeDeclaration(String name, List<String> optEquivalentTypes, ATRMap optProperties,
            List<String> optValues, String optRepresentationType, String optParentType,
            List<String> optFieldNames, List<String> optFieldTypes) {
        throw new UnsupportedOperationException(MESSAGE + "TypeDeclaration");
    }


}
