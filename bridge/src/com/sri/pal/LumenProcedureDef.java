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

// $Id: LumenProcedureDef.java 7750 2016-07-26 16:53:01Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import com.sri.ai.lumen.syntax.FormatUtil;
import com.sri.ai.lumen.atr.ATRConstructor;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.ActionRenamer;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRMap.Mutable;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.atr.term.impl.CTRLiteral;
import com.sri.ai.lumen.core.IStructure;
import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.RequestCanceler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.ObjectFactory;
import com.sri.pal.jaxb.TaskType;
import com.sri.pal.upgrader.ProcedureUpgrader;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.spine.util.SubTasksFinder;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LumenProcedureDef
        extends ProcedureDef {
    private static final Logger log = LoggerFactory
            .getLogger(LumenProcedureDef.class);

    public static final String SERIALIZATION_FORMAT_VERSION = "0.4";

    private String xmlSource = null;

    /**
     * Don't delete this, even if it's unused. It keeps references to dependent
     * actions. If those actions are transient, they'll get garbage collected
     * unless we maintain a reference to them.
     */
    private final Set<AbstractActionDef> nestedActions;

    static LumenProcedureDef newInstance(String xmlSource,
                                         boolean isTransient,
                                         Bridge bridge)
            throws PALException {
        String ctrSource = unwrapXml(xmlSource);
        ATRActionDeclaration proc = sourceToProc(ctrSource);
        return newInstance(proc, isTransient, bridge);
    }

    static RequestCanceler newInstance(CallbackHandler<LumenProcedureDef> callbackHandler,
                                       String xmlSource,
                                       boolean isTransient,
                                       Bridge bridge) {
        ATRActionDeclaration proc;
        try {
            String ctrSource = unwrapXml(xmlSource);
            proc = sourceToProc(ctrSource);
        } catch (PALException e) {
            log.warn("Unable to build proc from " + xmlSource, e);
            ErrorInfo error = ErrorFactory.error(bridge.getSpine()
                    .getClientId(), ErrorType.INTERNAL_PARSE, xmlSource);
            callbackHandler.error(error);
            return new RequestCanceler() {
                @Override
                public void cancel() {
                }
            };
        }
        return newInstance(callbackHandler, proc, isTransient,
                bridge);
    }

    static LumenProcedureDef newInstance(ATRActionDeclaration sparklProc,
                                         boolean isTransient,
                                         Bridge bridge)
            throws PALException {
        SynchronousCallbackHandler<LumenProcedureDef> sch = new SynchronousCallbackHandler<LumenProcedureDef>();
        newInstance(sch, sparklProc, isTransient, bridge);
        return sch.waitForResult();
    }

    static RequestCanceler newInstance(final CallbackHandler<LumenProcedureDef> callbackHandler,
                                       ATRActionDeclaration sparklProc,
                                       final boolean isTransient,
                                       Bridge bridge) {
        try {
            return innerNewInstance(callbackHandler, sparklProc, isTransient,
                    bridge);
        } catch (Exception e) {
            log.warn("Unable to build proc from " + procToSource(sparklProc), e);
            ErrorInfo error = ErrorFactory.error(bridge.getSpine()
                    .getClientId(), ErrorType.INTERNAL_PARSE, sparklProc);
            callbackHandler.error(error);
            return new RequestCanceler() {
                @Override
                public void cancel() {
                }
            };
        }
    }

    private static RequestCanceler innerNewInstance(final CallbackHandler<LumenProcedureDef> callbackHandler,
                                                    final ATRActionDeclaration sparklProc,
                                                    boolean isTransient,
                                                    final Bridge bridge)
            throws PALException {
        if (log.isDebugEnabled()) {
            log.debug("working on Lumen proc: {}",
                    ATRSyntax.toSource(sparklProc));
        }
        ATRSig sig = sparklProc.getSignature();
        String name = sig.getFunctor();
        final SimpleTypeName typeName = (SimpleTypeName) TypeNameFactory
                .makeName(name);
        String namespace = LumenProcedureExecutor.getNamespace();
        String version = LumenProcedureExecutor.getVersion();
        if (!namespace.equals(typeName.getNamespace())
                || !version.equals(typeName.getVersion())) {
            TypeName goodName = TypeNameFactory.makeName(
                    typeName.getSimpleName(), version, namespace);
            log.warn("Procedure " + name
                    + " will never run -- should be named "
                    + goodName.getFullName() + " instead.");
            throw new PALException("Refusing to load un-runnable procedure "
                    + name);
        }
        boolean allowPartial = false;
        if (TypeUtil.needsPreload(sparklProc)) {
            allowPartial = true;
        }

        /*
         * Overwrite the transient property on the ATR proc. We might have
         * loaded what was once a transient proc as non-transient this time, or
         * vice versa.
         */
        try {
            @SuppressWarnings("rawtypes")
            ATRMap.Mutable props = (Mutable) sparklProc.getProperties();
            @SuppressWarnings("unchecked")
            Map<String, ATRTerm> propMap = props.getMap();
            ATRLiteral transientTerm = new CTRLiteral(Boolean.toString(isTransient));
            propMap.put(TypeUtil.TRANSIENT, transientTerm);
        } catch (Exception e) {
            log.debug("Couldn't reset transient param on {}", typeName);
        }

        AsyncChain<Set<ActionModelDef>, LumenProcedureDef> chain = new AsyncChain<Set<ActionModelDef>, LumenProcedureDef>(
                callbackHandler) {
            @Override
            public void results(Set<ActionModelDef> result) {
                Set<AbstractActionDef> nestedActions = new HashSet<AbstractActionDef>();
                for (ActionModelDef type : result) {
                    if (type instanceof AbstractActionDef) {
                        nestedActions.add((AbstractActionDef) type);
                    } else {
                        log.debug("Nested non-action: {}", type);
                        ErrorInfo error = ErrorFactory.error(bridge
                                .getSpine().getClientId(),
                                ErrorType.ACTION_MODEL, type);
                        error(error);
                        return;
                    }
                }
                try {
                    LumenProcedureDef sparkProc = new LumenProcedureDef(
                            sparklProc, bridge, nestedActions);

                    // Make sure default values are accessible.
                    for (int i = 0; i < sparkProc.numInputParams(); i++) {
                        sparkProc.getDefaultValue(i);
                    }

                    subCH.result(sparkProc);
                } catch (Exception e) {
                    log.warn("Cannot load procedure: "
                            + procToSource(sparklProc), e);
                    ErrorInfo error = ErrorFactory.error(bridge.getSpine()
                            .getClientId(), ErrorType.INTERNAL_PARSE,
                            procToSource(sparklProc));
                    subCH.error(error);
                }
            }
        };
        Set<SimpleTypeName> calledActionNames = SubTasksFinder
                .findSubTasks(sparklProc);
        RequestCanceler rc = bridge.getActionModel().getTypes(chain,
                calledActionNames, allowPartial);
        chain.addCanceler(rc);
        return chain;
    }

    /**
     * Put XML wrappers on a CTR source string.
     *
     * @param source
     *            a CTR-formatted string containing the source of a Lumen
     *            procedure
     * @return XML-wrapped CTR source, suitable for putting into long term
     *         storage
     * @throws JAXBException
     *             if an XML marshalling error occurs
     */
    public static String wrapXml(String source)
            throws JAXBException {
        TaskType taskXml = new TaskType();

        taskXml.setVersion(SERIALIZATION_FORMAT_VERSION);
        taskXml.setBodySource(source);

        initJaxb();

        ObjectFactory objFact = new ObjectFactory();
        JAXBElement<TaskType> taskModel = objFact.createTaskModel(taskXml);
        return marshal(taskModel).trim();
    }

    /**
     * Remove XML wrappers from a CTR source string.
     *
     * @param xmlSource
     *            an XML-formatted string containing source for a Lumen
     *            procedure
     * @return CTR-formatted source for the procedure
     * @throws PALException
     *             if the XML cannot be parsed, or if it comes from an
     *             incompatible version
     * @throws PALSerializationVersionException
     *             if the XML can be parsed, but is using an incompatible
     *             version of the PAL serialization format. If this occurs,
     *             upgrading is required using {@link ProcedureUpgrader}.
     */
    public static String unwrapXml(String xmlSource)
            throws PALException {
        return unwrapXml(xmlSource, true);
    }

    /**
     * Remove XML wrappers from a CTR source string.
     *
     * @param xmlSource
     *            an XML-formatted string containing source for a Lumen
     *            procedure
     * @param checkVersion
     *            {@code true} to check the version embedded in the procedure
     *            source and throw an error if it's incompatible with this
     *            version of PAL
     * @return CTR-formatted source for the procedure
     * @throws PALException
     *             if the XML cannot be parsed, or if it comes from an
     *             incompatible version
     * @throws PALSerializationVersionException
     *             if the XML can be parsed, but is using an incompatible
     *             version of the PAL serialization format. If this occurs,
     *             upgrading is required using {@link ProcedureUpgrader}.
     */
    public static String unwrapXml(String xmlSource,
                                   boolean checkVersion)
            throws PALException {
        JAXBElement<?> ele;
        try {
            ele = unmarshal(xmlSource);
        } catch (JAXBException e) {
            String msg = "Error parsing XML string " + xmlSource;
            log.error(msg, e);
            throw new PALException(msg, e);
        }

        TaskType taskXml = (TaskType) ele.getValue();

        if (checkVersion) {
            String version = taskXml.getVersion();
            if (!SERIALIZATION_FORMAT_VERSION.equals(version)) {
                String msg = "Wrong task format version: expected "
                        + SERIALIZATION_FORMAT_VERSION + ", got " + version;
                log.error(msg);
                throw new PALSerializationVersionException(msg);
            }
        }

        String bodySource = taskXml.getBodySource();

        return bodySource;
    }

    /**
     * Converts from a CTR-S string representing a procedure to the
     * corresponding ATR structure.
     *
     * @param ctrSource
     *            the source string
     * @return an ATR procedure declaration
     * @throws PALException
     *             if the string cannot be parsed.
     */
    public static ATRActionDeclaration sourceToProc(String ctrSource)
            throws PALException {
        CTRConstructor factory = new CTRConstructor();
        return sourceToProc(ctrSource, factory);
    }

    /**
     * Converts a CTR-S source string into an ATRActionDeclaration.
     */
    public static <E, ETask extends E, ETerm extends E, EDecl extends E, ELogical extends E, ESig extends E, EParameter extends E, EVariable extends ETerm, EMap extends ETerm, EPredicate extends ELogical, ESymbol extends ETerm> ATRActionDeclaration sourceToProc(String ctrSource,
                                                                                                                                                                                                                                                                      ATRConstructor<E, ETask, ETerm, EDecl, ELogical, ESig, EParameter, EVariable, EMap, EPredicate, ESymbol> factory)
            throws PALException {
        ATRActionDeclaration result;
        try {
            IStructure<?> obj = (IStructure<?>) FormatUtil
                    .parseLumenStatement(ctrSource);
            ATRSyntax<E, ETask, ETerm, EDecl, ELogical, ESig, EParameter, EVariable, EMap, EPredicate, ESymbol> atrUtil = new ATRSyntax<E, ETask, ETerm, EDecl, ELogical, ESig, EParameter, EVariable, EMap, EPredicate, ESymbol>(
                    factory);
            result = (ATRActionDeclaration) atrUtil.toActionDeclaration(obj);
        } catch (Exception e) {
            String msg = "Failed to parse as CTR-S source: " + ctrSource;
            log.error(msg, e);
            throw new PALException(msg, e);
        }
        return result;
    }

    /**
     * Converts an ATRActionDeclaration into a CTR-S source string.
     */
    public static String procToSource(ATRActionDeclaration proc) {
        return ATRSyntax.toSource(proc);
    }

    private LumenProcedureDef(ATRActionDeclaration atr,
                              Bridge bridge,
                              Set<AbstractActionDef> nestedActions)
            throws PALException {
        super(atr, bridge);

        this.nestedActions = new HashSet<AbstractActionDef>();
        this.nestedActions.addAll(nestedActions);
    }

    @Override
    public Set<ActionModelDef> getRequiredDefs()
            throws PALException {
        Set<ActionModelDef> result = new HashSet<ActionModelDef>();

        // Add parent if applicable.
        if (getParentDef() != null) {
            result.add(getParentDef());
            result.addAll(getParentDef().getRequiredDefs());
        }

        // Add param types; possibly redundant, but it won't hurt.
        for (int i = 0; i < size(); i++) {
            TypeDef type = getParamType(i);
            result.add(type);
            result.addAll(type.getRequiredDefs());
        }

        // Add nested actions.
        for (AbstractActionDef nested : nestedActions) {
            result.add(nested);
            result.addAll(nested.getRequiredDefs());
        }

        return result;
    }

    @Override
    public String getXml() {
        return getSource();
    }

    @Override
    public LumenProcedureExecutor getExecutor() {
        return (LumenProcedureExecutor) super.getExecutor();
    }

    @Override
    public LumenProcedureDef copyAndRename(SimpleTypeName newName)
            throws PALException {
        CTRConstructor factory = new CTRConstructor();
        return copyAndRename(newName, factory);
    }

    public <E, ETask extends E, ETerm extends E, EDecl extends E, ELogical extends E, ESig extends E, EParameter extends E, EVariable extends ETerm, EMap extends ETerm, EPredicate extends ELogical, ESymbol extends ETerm> LumenProcedureDef copyAndRename(SimpleTypeName newName,
                                                                                                                                                                                                                                                             ATRConstructor<E, ETask, ETerm, EDecl, ELogical, ESig, EParameter, EVariable, EMap, EPredicate, ESymbol> factory)
            throws PALException {
        ActionRenamer<E, ETask, ETerm, EDecl, ELogical, ESig, EParameter, EVariable, EMap, EPredicate, ESymbol> renamer = new ActionRenamer<E, ETask, ETerm, EDecl, ELogical, ESig, EParameter, EVariable, EMap, EPredicate, ESymbol>(
                factory);
        ATRActionDeclaration newProc = (ATRActionDeclaration) renamer
                .renameActionDeclaration(getAtr(), newName.getFullName());
        return newInstance(newProc, isTransient(), getBridge());
    }

    @Override
    public String getSource() {
        if (xmlSource == null) {
            try {
                xmlSource = wrapXml(procToSource(getAtr()));
            } catch (Exception e) {
                String msg = "Unable to generate XML source for " + getName();
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }

        return xmlSource;
    }

    /**
     * Provides access to all of this procedure's nested actions. The nested
     * actions are the actions which this procedure directly calls or depends
     * on. Transitive dependencies are not included in this set.
     *
     * @return all directly called actions
     */
    public Set<AbstractActionDef> getNestedActions() {
        return Collections.unmodifiableSet(nestedActions);
    }

    ATRActionDeclaration getSparklProcedure() {
        return getAtr();
    }

    /**
     * Gets the CTR-S procedure body source.
     */
    String getProcedureSource() {
        return procToSource(getAtr());
    }

    @Override
    public LumenProcedureDef newDefaultValue(int pos,
                                             Object value) {
        return (LumenProcedureDef) super.newDefaultValue(pos, value);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + getSource().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        LumenProcedureDef other = (LumenProcedureDef) obj;
        if (!getSource().equals(other.getSource()))
            return false;
        return true;
    }
}
