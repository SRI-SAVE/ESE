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

import java.util.Collection;

import com.sri.tasklearning.ui.core.term.ParameterModel;
import com.sri.tasklearning.ui.core.term.TermModel;

/**
 * Represents a zip function call, which somehow interleaves the elements of
 * 2 (multiple?).  
 */
public class ZipModel extends FunctionModel {
    public ZipModel(String f, Collection<? extends TermModel> elts) {

        super(f, elts);

        // TODO if we ever support editing zip, we'll need to calculate
        // type information for it
    }
    
    @Override
    public String getDisplayString() {
        final String sep = " and ";
        StringBuffer disp = new StringBuffer();
        
        for (ParameterModel pm : getInputs()) {
            disp.append(pm.getTerm().getDisplayString());
            disp.append(sep);
        }
        
        return disp.substring(0,  disp.length() - sep.length());
    }
}
