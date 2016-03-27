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

// $Id: LumenTypeAdder.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.type.Type;
import com.sri.ai.lumen.mediator.MediatorException;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.mediators.MediatorsException;
import com.sri.tasklearning.mediators.TypeAdder;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the details of adding types to Lumen (and removing them). Among other
 * things, it keeps Lumen's type registry up to date.
 */
public class LumenTypeAdder
        implements TypeAdder {
    private static final Logger log = LoggerFactory
            .getLogger(LumenTypeAdder.class);

    private final LumenFacade lumen;
    private final Type.Registry registry;

    public LumenTypeAdder(LumenFacade lumenFacade){
        lumen = lumenFacade;
        registry = lumen.getTypeRegistry();
    }

    @Override
    public void add(ATRDecl type)
            throws MediatorsException {
        if (TypeUtil.isProcedure(type)) {
            // TODO Lumen should be able to take an ATRActionDeclaration.
            String source = ATRSyntax.toSource(type);
            try {
                lumen.addTaskDefinition(source);
            } catch (MediatorException e) {
                throw new MediatorsException(source, e);
            }
        } else if (TypeUtil.isAction(type)) {
            ATRActionDeclaration lumenAction = (ATRActionDeclaration) type;
            lumen.addIfPrimitiveAction(lumenAction);
        } else if (TypeUtil.isType(type)) {
            if (registry.optDeclaration(TypeUtil.getName(type).getFullName()) == null) {
                registry.addDeclaration((ATRTypeDeclaration) type);
            }
        }
    }

    @Override
    public boolean remove(ATRDecl decl)
            throws MediatorsException {
        /*
         * Don't remove non-action types, because we're not sure if that will
         * work, and they're pretty static, and they don't take up much memory
         * anyway.
         */
        if (!TypeUtil.isAction(decl)) {
            log.debug("Not removing non-action type {}",
                    ATRSyntax.toSource(decl));
            return false;
        }

        SimpleTypeName typeName = TypeUtil.getName(decl);
        /*
         * Don't remove actions that aren't backed by Lumen procedures. These
         * are pretty static, so no big memory leak here.
         */
        if (!ExecutionHandler.NAMESPACE.equals(typeName.getNamespace())) {
            log.debug("Not removing non-Lumen action {}", decl);
            return false;
        }
        try {
            lumen.removeTaskDefinition(typeName);
            return true;
        } catch (MediatorException e) {
            throw new MediatorsException(typeName.getFullName(), e);
        }
    }
}
