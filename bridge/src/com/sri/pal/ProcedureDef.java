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

// $Id: ProcedureDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * Represents the definition of a procedure. Note that this is different from a
 * procedure invocation, {@link ProcedureInvocation}. A procedure is a
 * specialized sort of action which must be loaded from some source, typically a
 * script in a scripting language. Procedures also differ in their ability to be
 * copied and have metadata associated with them.
 *
 * @author chris
 */
public abstract class ProcedureDef
        extends ActionDef {
    protected ProcedureDef(ATRActionDeclaration atr,
                           Bridge bridge)
            throws PALException {
        super(atr, bridge);
    }

    @Override
    public abstract String getXml();

    @Override
    protected abstract Set<ActionModelDef> getRequiredDefs()
            throws PALException;

    @Override
    protected ProcedureInvocation newInstance(ActionInvocation parent,
                                              long serial)
            throws PALException {
        TransactionUID uid = getSpine().getNextUid();
        return new ProcedureInvocation(this, parent, serial, uid);
    }

    @Override
    public ProcedureInvocation invoke(ActionInvocation parent,
                                      Object... args)
            throws PALException {
        return invoke(parent, Arrays.asList(args));
    }

    @Override
    public ProcedureInvocation invoke(ActionInvocation parent,
                                      List<? extends Object> args)
            throws PALException {
        return (ProcedureInvocation) super.invoke(parent, args);
    }

    @Override
    ActionInvocation invoked(ActionStreamEvent caller,
                             TransactionUID uid,
                             long serialNumber)
            throws PALException {
        return new ProcedureInvocation(this, caller, serialNumber, uid);
    }

    /**
     * Copy the procedure and give the copy a new name.
     *
     * @param newName
     *            the new name for the copied procedure
     * @return a new procedure with the given name
     * @throws PALException
     *             if an error occurs
     */
    public abstract ProcedureDef copyAndRename(SimpleTypeName newName)
            throws PALException;

    /**
     * Provides the source used to create this procedure. This string should be
     * considered opaque, as it is implementation-dependent.
     *
     * @return the source for this procedure
     */
    public String getSource() {
        return getXml();
    }

    @Override
    public ProcedureExecutor getExecutor() {
        return (ProcedureExecutor) super.getExecutor();
    }

    @Override
    public ProcedureDef newDefaultValue(int pos,
                                        Object value) {
        return (ProcedureDef) super.newDefaultValue(pos, value);
    }
}
