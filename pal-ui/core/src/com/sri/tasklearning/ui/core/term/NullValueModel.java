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

import com.sri.ai.lumen.atr.ATRCat;
import com.sri.ai.lumen.atr.term.ATRNull;
import com.sri.pal.TypeDef;

/**
 * Represents a NULL value in PUTR. 
 */
public final class NullValueModel extends AtomicTermModel implements ATRNull {
    public static final NullValueModel NULL = new NullValueModel();
    
    private NullValueModel() {};
    
    @Override
    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }

    @Override
    public String getDisplayString() {
        return "?";
    }
    
    @Override
    public void setTypeDef(TypeDef def) {
        // Intentionally empty...null shouldn't have a type
    }

    public boolean equals(Object other) {
        return (other instanceof NullValueModel);
    }
    
    public int hashCode() {
        return 83669461;
    }
    
    @Override
    public NullValueModel deepCopy() {
        return NULL; 
    }
}
