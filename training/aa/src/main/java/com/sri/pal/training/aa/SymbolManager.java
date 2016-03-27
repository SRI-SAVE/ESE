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
package com.sri.pal.training.aa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.impl.CTRActionDeclaration;
import com.sri.ai.lumen.atr.impl.CTRParameter;
import com.sri.ai.lumen.atr.impl.CTRSig;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.impl.CTRVariable;
import com.sri.ai.lumen.editorsupport.ProcedureInfo;
import com.sri.pal.Bridge;
import com.sri.pal.ExpressionEvaluator;
import com.sri.pal.Validator;
import com.sri.pal.training.core.exercise.Value;
import com.sri.pal.training.core.util.ValueUtil;

public class SymbolManager {
    private static final String PREFIX = "expr:";
    private static final Logger log = LoggerFactory.getLogger(SymbolManager.class);    
    
    private final Bridge bridge;
    private final Map<String, Value> valueMap = new HashMap<String, Value>();    
    private final CTRActionDeclaration fakeProc = new CTRActionDeclaration(new CTRSig("fake")); 
    private final ExpressionEvaluator eval;
        
    public SymbolManager(Bridge bridge) {        
        this.bridge = bridge;        
        eval = new ExpressionEvaluator(bridge); 
    }
    
    public Value lookupReference(String ref) {
        if (valueMap.containsKey(ref))
            return valueMap.get(ref);
        
        if (ref.startsWith(PREFIX)) {
            // Evaluate the expression. If successful, add it to the map. 
            try {
                String expr = ref.substring(PREFIX.length());
                ATRTerm term = ATRSyntax.CTR.termFromSource(expr);
                List<ATRTerm> terms = new ArrayList<ATRTerm>();
                terms.add(term);
                List<Object> evals = eval.evaluateExpressions(terms);
                if (evals == null) {
                    valueMap.put(ref, null);
                    return null; 
                }
                Object obj = evals.get(0);
                Validator val = new Validator(bridge);
                ProcedureInfo info = val.makeProcedureInfo(fakeProc);
                String type = info.getType(term);
                Value v = new Value();
                v.setId(ref);
                v.setType(type);
                ValueUtil.setObject(v, obj, bridge);
                valueMap.put(ref, v);
                return v; 
            } catch (Exception e) {
                log.error("Failed to evaluate symbol expression: " + ref, e);
                return null;
            }            
        } else {
            return null; 
        }
    }
    
    @SuppressWarnings("unchecked")
    public void put(String key, Value value) {
        valueMap.put(key, value);
        
        if (value != null) {
            eval.addBinding(key, ValueUtil.getTypeDef(value, bridge),
                    ValueUtil.getObject(value, bridge));
        
            CTRVariable v = new CTRVariable(key);
            CTRParameter p = new CTRParameter(v, Modality.INPUT, value.getType()); 
            ((List<CTRParameter>)fakeProc.getSignature().getElements()).add(p);
        }
    }
}
