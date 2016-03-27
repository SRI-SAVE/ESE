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

import java.util.HashMap;
import java.util.Map;

import com.sri.ai.lumen.core.LumenConstant;
import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.term.ATRFunction;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRSymbol;

/**
 * Implementation of an ATRMap that uses HashMap. Currently these are only used
 * for property maps as Lapdog uses positionalTupleGen/nth for creating and
 * accessing structs. 
 */
public class MapModel extends TermModel implements ATRMap {

    private HashMap<String, TermModel> map = new HashMap<String, TermModel>();
    
    public MapModel(Map<String, ? extends TermModel> p_map) {
        for (Map.Entry<String, ? extends TermModel> entry : p_map.entrySet()) 
            map.put(entry.getKey(), entry.getValue());
    }
    
    public MapModel() {
        
    }

    @Override
    public HashMap<String, TermModel> getMap() {
        return map;
    }

    @Override
    public String getDisplayString() {
        return map.toString();
    }

    @Override
    public boolean containsKey (String key) {
        return map.containsKey(key);
    }

    @Override
    public boolean  contains(ATRSymbol symbol) {
        return map.containsKey(symbol.getNameString());
    }

    @Override
    public boolean contains(String symbol) {
        return map.containsKey(symbol);
    }
    
    @Override
    public Integer getInteger(String key) throws ClassCastException {
        ATRLiteral lit = (ATRLiteral) map.get(key);
        return lit == null ? null : (Integer) lit.getValue();
    }

    @Override
    public String getString(String key) throws ClassCastException {
        ATRLiteral lit = (ATRLiteral) map.get(key);
        return lit == null ? null : (String) lit.getValue();
    }

    @Override
    public Double getDouble(String key) throws ClassCastException {
        ATRLiteral lit = (ATRLiteral) map.get(key);
        return lit == null ? null : (Double) lit.getValue();
    }

    @Override
    public Boolean getBoolean(String key) throws ClassCastException {
        ATRFunction f = (ATRFunction) map.get(key);

        if (f != null && !f.getElements().isEmpty()) {
            String functor = f.getFunctor();
            if (functor.equals(LumenConstant.TRUE))
                return Boolean.TRUE;
            else if (functor.equals(LumenConstant.FALSE))
                return Boolean.FALSE;            
        }
        
        throw new ClassCastException("attempting to interpret value other than true or false as a Boolean");
    }

    public void set(String key, TermModel value) {
        map.put(key, value);
    }

    @Override
    public TermModel get(ATRSymbol symbol) {
        return map.get(symbol.getNameString());
    }
    
    @Override
    public TermModel get(String symbol) {
        return map.get(symbol);
    }

    public void remove(String s) {
        map.remove(s);
    }

    @Override
    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }

    @Override
    public ATR getInternalSub() {
        return null;
    }
    
    @Override
    public MapModel deepCopy() {
        MapModel copy = new MapModel();        
        for (String key : map.keySet()) 
            copy.map.put(key, ((TermModel)map.get(key)).deepCopy());
        copy.setTypeDef(getTypeDef());
        return copy;
    }
}