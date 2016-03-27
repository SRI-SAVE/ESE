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
import com.sri.ai.lumen.atr.term.ATRNoEvalTerm;

/**
 * Implementation of ATRNoEvalTerm, which is a special term that tells
 * Lumen not to evaluate the contents of the inner term. These are currently 
 * used in constraints which as of yet have not been utilized in Adept or 
 * WebTAS. We include an implementation of ATRNoEvalTerm so that the PAL UI can 
 * load procedures that contain NoEvalTerms, but there's currently no support
 * for viewing or modifying them. 
 */
public class NoEvalTermModel extends TermModel implements ATRNoEvalTerm {

    private TermModel inner;

    public NoEvalTermModel(TermModel term) {
        this.inner = term;
    }

    @Override
    public ATRCat getCategory() {
        return ATRCat.getATRCat(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof NoEvalTermModel) {
            NoEvalTermModel oth = (NoEvalTermModel) other;
            return inner.equals(oth.inner);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return inner.hashCode();
    }

    public TermModel getInnerTerm() {
        return inner;
    }

    @Override
    public String getDisplayString() {
        return inner.toString();
    }
    
    @Override
    public NoEvalTermModel deepCopy() {
        return new NoEvalTermModel(inner.deepCopy());
    }
}
