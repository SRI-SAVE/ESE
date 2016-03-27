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

// $Id: RegexRenamer.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.upgrader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.ATRStruct;
import com.sri.ai.lumen.atr.ATRTraverser;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.ATRVariable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RegexRenamer
        extends ATRTraverser {
    private static final Logger log = LoggerFactory
            .getLogger(RegexRenamer.class);

    private final Pattern pattern;
    private final String replacement;
    private final CTRConstructor ctrBuilder;

    public RegexRenamer(String regex,
                        String replacement) {
        pattern = Pattern.compile(regex);
        this.replacement = replacement;
        ctrBuilder = new CTRConstructor();
    }

    public void run(ATRActionDeclaration proc) {
        traverse(proc);
    }

    private String rename(String oldStr) {
        Matcher matcher = pattern.matcher(oldStr);
        String result = matcher.replaceAll(replacement);
        if (!oldStr.equals(result)) {
            log.debug("Renaming {} to {}", oldStr, result);
        }
        return result;
    }

    @Override
    protected boolean process(ATR atr,
                              int preorderIndex) {
        if (atr instanceof ATRStruct) {
            /*
             * Rename this thing, which could be a procedure declaration or an
             * action call.
             */
            ATRStruct.Mutable<?> mut = (ATRStruct.Mutable<?>) atr;
            String functor = mut.getFunctor();
            functor = rename(functor);
            mut.setFunctor(functor);
        }

        if (atr instanceof ATRSig) {
            /*
             * This is also an ATRStruct, so it's already getting renamed. But
             * we have to rename its parameter types also.
             */
            ATRSig atrSig = (ATRSig) atr;
            for (ATRParameter param : atrSig.getElements()) {
                ATRLiteral type = param.getType();
                String typeStr = type.getString();
                typeStr = rename(typeStr);
                type = ctrBuilder.createLiteral(typeStr, null);
                @SuppressWarnings("unchecked")
                ATRParameter.Mutable<ATRVariable, ATRTerm, ATRLiteral> mutParam = (ATRParameter.Mutable<ATRVariable, ATRTerm, ATRLiteral>) param;
                mutParam.setType(type);
            }
        }

        return true;
    }
}
