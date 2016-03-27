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

package com.sri.tasklearning.ui.core.step;

import java.util.List;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.ATRStruct;
import com.sri.ai.lumen.atr.ATRStructure;

/**
 * @author Will Haines (haines@ai.sri.com)
 * 
 *         Wraps structures where our representation does not match ATR
 *         one-to-one.
 */
public class Wrapper implements ATRStructure {
    private ATRStruct wrapped;
    
    public Wrapper(ATRStruct argWrapped) {
        wrapped = argWrapped; 
    }

    @Override
    public ATRCat getCategory() {
        return ATRCat.OTHER_STRUCTURE;
    }

    @Override
    public String getFunctor() {
        return wrapped.getFunctor();
    }

    @Override
    public List<? extends ATR> getElements() {
        return wrapped.getElements();
    }

    @Override
    public ATR getInternalSub() {
        return null;
    }
    
    public ATRStruct getWrapped() {
        return wrapped;     
    }
}